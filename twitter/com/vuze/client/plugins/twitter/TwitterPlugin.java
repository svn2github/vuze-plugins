/*
 * Created on Jun 29, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */



package com.vuze.client.plugins.twitter;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterGroup;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;


public class 
TwitterPlugin
	implements UnloadablePlugin, DownloadManagerListener, UTTimerEventPerformer
{
	public static final String CONSUMER_KEY 	= "2Dln8erj8B7tzObS4Og";
	public static final String CONSUMER_SECRET	= "gj7kB6GBxTdr66o7Xph1fEeQDqOA4tLZff7p30EujX4";
	
	static{
		System.setProperty( "twitter4j.oauth.consumerKey", CONSUMER_KEY );
		System.setProperty( "twitter4j.oauth.consumerSecret", CONSUMER_SECRET );
	}
	
	public static final String 	CONFIG_ENABLE			= "enable";
	public static final boolean CONFIG_ENABLE_DEFAULT	= true;
	
	public static final String	CONFIG_TWEET_ADDED			= "twitter.tweet_added";
	public static final String	CONFIG_TWEET_ADDED_DEFAULT	= "twitter.tweet_added.msg";
		
	public static final String 	CONFIG_TWITTER_USER		= "twitter_user";
	public static final String 	CONFIG_TWITTER_PASSWORD	= "twitter_password";

	private static final String	TWEET_DOWNLOAD_ADDED = "added";
	
	private static final int	MAX_RESEND_DELAY	= 1*60*60*1000;
	
	private PluginInterface		plugin_interface;
	
	private TorrentAttribute	ta_tweets;
	
	private LoggerChannel			log;
	private UTTimer					timer;
	private UTTimerEvent			timer_event;
	
	private BasicPluginViewModel	view_model;
	private BasicPluginConfigModel 	config_model;
	
	private BooleanParameter 		enable;
	
	private StringParameter			tweet_text;

	// private StringParameter			twitter_user;
	// private PasswordParameter		twitter_password;
	
	private AccessToken	access_token;
	
	private List<Download>	active_tweets = new ArrayList<Download>();
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher();
	
	
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
			
		String ac_str 	= plugin_interface.getPluginconfig().getPluginStringParameter( "twitter.access.token", "" );
		String acs_str 	= plugin_interface.getPluginconfig().getPluginStringParameter( "twitter.access.token.secret", "" );
		
		if ( ac_str.length() > 0 && acs_str.length() > 0 ){
			
			access_token = new AccessToken( ac_str, acs_str );
		}
		
		ta_tweets = plugin_interface.getTorrentManager().getPluginAttribute( "tweets" );
		
		final LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		log	= plugin_interface.getLogger().getChannel( "Twitter");
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		config_model = ui_manager.createBasicPluginConfigModel( "plugins", "twitter.name");
						
		enable 		= config_model.addBooleanParameter2( CONFIG_ENABLE, "twitter.enable", CONFIG_ENABLE_DEFAULT );

		LabelParameter	tweet_info = config_model.addLabelParameter2( "twitter.tweet.info" );
		
		tweet_text = config_model.addStringParameter2( CONFIG_TWEET_ADDED, CONFIG_TWEET_ADDED, loc_utils.getLocalisedMessageText( CONFIG_TWEET_ADDED_DEFAULT ));

		ParameterGroup tweet_group = config_model.createGroup( "twitter.tweet.group", new Parameter[]{ tweet_info, tweet_text });

		// twitter_user 		= config_model.addStringParameter2( CONFIG_TWITTER_USER, "twitter.user", "" );
		// twitter_password 	= config_model.addPasswordParameter2( CONFIG_TWITTER_PASSWORD, "twitter.password", PasswordParameter.ET_PLAIN, new byte[0] );

		
		LabelParameter oauth_info = config_model.addLabelParameter2( "twitter.oauth.info" );
		
		final ActionParameter oauth_setup = config_model.addActionParameter2( "twitter.oauth.start.label", "twitter.oauth.start.button" );
		
		final RequestToken[] request_token = { null };
		
		final StringParameter oauth_pin = config_model.addStringParameter2( "twitter.oauth.pin", "twitter.oauth.pin", "" );
		
		oauth_pin.setValue( "" );
		
		final ActionParameter oauth_done = config_model.addActionParameter2( "twitter.oauth.done.label", "twitter.oauth.done.button" );

		oauth_setup.addConfigParameterListener(
				new ConfigParameterListener()
				{
					public void
					configParameterChanged(
						ConfigParameter	param )
					{
						oauth_setup.setEnabled( false );
						
						plugin_interface.getUtilities().createThread(
							"Twitter Setup",
							new Runnable()
							{
								public void
								run()
								{
						            Twitter twitter = new TwitterFactory().getInstance();
						            
						            try{
							            RequestToken requestToken = twitter.getOAuthRequestToken();
							            
							            request_token[0] = requestToken;
							            
							            oauth_done.setEnabled( true );
							            							            							            
							            log.log( "OAuth URL: " + requestToken.getAuthorizationURL());
							            
							            plugin_interface.getUIManager().openURL( new URL( requestToken.getAuthorizationURL()));
							            
							            Thread.sleep( 5000 );
							            
						            }catch( Throwable e ){
						            	
						            	log.log( "OAuth setup failed", e);
						            							            	
										plugin_interface.getUIManager().showMessageBox(
												"twitter.oauth.error.title",
												"twitter.oauth.error.details",
												UIManagerEvent.MT_OK );
						            }finally{
						            	
						            	oauth_setup.setEnabled( true );
						            }
								}
							});
					}
				});
			
		
		oauth_done.addConfigParameterListener(
				new ConfigParameterListener()
				{
					public void
					configParameterChanged(
						ConfigParameter	param )
					{
						plugin_interface.getUtilities().createThread(
								"Twitter Setup",
								new Runnable()
								{
									public void
									run()
									{
										try{
											Twitter twitter = new TwitterFactory().getInstance();

											AccessToken at = twitter.getOAuthAccessToken( request_token[0], oauth_pin.getValue());

											access_token = at;

											String token 			= at.getToken();
											String token_secret 	= at.getTokenSecret();

											plugin_interface.getPluginconfig().setPluginParameter( "twitter.access.token", token );
											plugin_interface.getPluginconfig().setPluginParameter( "twitter.access.token.secret", token_secret );

											plugin_interface.getPluginconfig().save();

											log.log( "OAuth setup successful - token saved" );
											
											plugin_interface.getUIManager().showMessageBox(
													"twitter.oauth.ok.title",
													"twitter.oauth.ok.details",
													UIManagerEvent.MT_OK );
												
										}catch( Throwable e ){
											
							            	log.log( "OAuth setup failed", e);
							            	
											plugin_interface.getUIManager().showMessageBox(
													"twitter.oauth.error.title",
													"twitter.oauth.error.details",
													UIManagerEvent.MT_OK );
										}
									}
								});
					}
				});

		oauth_done.setEnabled( false );
		
		ParameterGroup auth_group = 
			config_model.createGroup( 
					"twitter.oauth.group", 
					new Parameter[]{ oauth_info, oauth_setup, oauth_pin, oauth_done });

		ActionParameter action = config_model.addActionParameter2( "twitter.tweet_test", "twitter.send" );

		action.addConfigParameterListener(
			new ConfigParameterListener()
			{
				public void
				configParameterChanged(
					ConfigParameter	param )
				{
					plugin_interface.getUtilities().createThread(
						"Twitter Test",
						new Runnable()
						{
							public void
							run()
							{
								Map<String,String> params = new HashMap<String,String>();
								
								params.put( "%t", "<test_torrent_name>" );
								params.put( "%m", "<magnet_uri>" );
								
								TwitterResult result = sendTweet( params );
								
								if ( result.isOK()){
									
									plugin_interface.getUIManager().showMessageBox(
											"twitter.testok.title",
											"twitter.testok.details",
											UIManagerEvent.MT_OK );
								}else{
									
									plugin_interface.getUIManager().showMessageBox(
											"twitter.testfail.title",
											"!" + loc_utils.getLocalisedMessageText( "twitter.testfail.details", new String[]{ result.getError() } ) + "!",
											UIManagerEvent.MT_OK );
								}
							}
						});
				}
			});
		
		view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "twitter.name" ));

		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	content )
					{
						view_model.getLogArea().appendText( content + "\n" );
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						view_model.getLogArea().appendText( str + "\n" );
						view_model.getLogArea().appendText( error.toString() + "\n" );
					}
				});
		
		
		
		view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
				

		enable.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter		p )
				{					
					view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
					
					checkEnabled();
				}
			});	
		

		
		enable.addEnabledOnSelection( tweet_info );
		enable.addEnabledOnSelection( tweet_text );
		enable.addEnabledOnSelection( oauth_info );
		enable.addEnabledOnSelection( oauth_setup );
		enable.addEnabledOnSelection( oauth_pin );
		enable.addEnabledOnSelection( oauth_done );
		
		enable.addEnabledOnSelection( action );

		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						checkEnabled();
					}
				}
				
				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
		
		timer = plugin_interface.getUtilities().createTimer( "refresher" );
		
		timer_event = timer.addPeriodicEvent( 60*1000, this );

		plugin_interface.getDownloadManager().addListener( this );
	}
	
	private void
	checkEnabled()
	{
		if ( enable.getValue()){
			
			if ( access_token == null ){
				
				plugin_interface.getUIManager().showMessageBox(
						"twitter.init.title",
						"twitter.init.details",
						UIManagerEvent.MT_OK );
			}
		}
	}
	
	public void
	unload()
	{
		if ( plugin_interface != null ){
			
			plugin_interface.getDownloadManager().removeListener( this );
		}
		
		if ( timer_event != null ){
			
			timer_event.cancel();
		}
		
		if ( timer != null ){
			
			timer.destroy();
		}
		
		if ( config_model != null ){
			
			config_model.destroy();
		}
		
		if ( view_model != null ){
			
			view_model.destroy();
		}
	}
	
	public void
	perform(
		UTTimerEvent		event )
	{
		if ( !enable.getValue()){
			
			return;
		}
		
		checkStuff();
	}
	
	private void
	checkStuff()
	{
		dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					checkStuffSupport();
				}
			});
	}
	
	private void
	checkStuffSupport()
	{
		List<Download>	list;
		
		synchronized( this ){
		
			list = new ArrayList<Download>( active_tweets );
		}
		
		if ( list.size() == 0 ){
			
			return;
		}
		
		long	now = SystemTime.getCurrentTime();
		
		for ( Download download: list ){
			
			Map<String,Map> tweets;
			
			synchronized( this ){

				tweets = download.getMapAttribute( ta_tweets );
			
				if ( tweets == null || tweets.size() == 0 || download.getTorrent() == null ){
				
					active_tweets.remove( download );
					
					continue;
				}
				
				tweets = new HashMap( tweets );
			}
			
			for ( Map.Entry entry: tweets.entrySet()){
				
				String	type 	= (String)entry.getKey();
				Map		details	= (Map)entry.getValue();
			
				Long	fail_time = (Long)details.get( "fail_time" );
				
				long	send_after = 0;
				
				if ( fail_time != null ){
					
					Long	fail_count = (Long)details.get( "fail_count" );
					
					long	fails = fail_count==null?1:fail_count;
					
					long	delay = 60*1000;
					
					for (int i=1;i<fails;i++){
						
						delay *= 2;
						
						if ( delay > MAX_RESEND_DELAY ){
							
							delay = MAX_RESEND_DELAY;
							
							break;
						}
					}
					
					send_after = fail_time + delay;
				}
				
				if ( send_after <= now ){
								
					Map<String,String>	params = new HashMap<String, String>();
					
					params.put( "%t", download.getName());
					params.put( "%m", UrlUtils.getMagnetURI( download.getTorrent().getHash()));
					
					TwitterResult result = sendTweet( params );
					
					if ( result.isOK() || !result.canRetry()){
							
						removeTweetRequest( download, TWEET_DOWNLOAD_ADDED );
						
					}else{
						
						failedTweetRequest( download, TWEET_DOWNLOAD_ADDED );
					}
				}
			}
		}
	}
	
	public void
	downloadAdded(
		final Download	download )
	{			
		recoverTweetRequests( download );
		
		if ( !enable.getValue()){
			
			return;
		}
		
		if ( download.getFlag( Download.FLAG_LOW_NOISE ) || download.getFlag( Download.FLAG_ONLY_EVER_SEEDED )){
			
			return;
		}
		
		if ( !download.isPersistent()){
			
			return;
		}
		
		long create_time = download.getCreationTime();
		
		long now = System.currentTimeMillis();
		
		if ( now - create_time >= 20*1000 ){
			
			return;
		}
		
		log.log( "Download added:" + download.getName());
		
		addTweetRequest( download, TWEET_DOWNLOAD_ADDED );
	
		checkStuff();
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		synchronized( this ){

			active_tweets.remove( download );
		}
	}
	
	private void
	addTweetRequest(
		Download		download,
		String			type )
	{
		synchronized( this ){
			
			Map tweets = download.getMapAttribute( ta_tweets );
	
			if ( tweets == null ){
				
				tweets = new HashMap();
				
			}else{
				
				tweets = new HashMap( tweets );
			}
			
			if ( tweets.containsKey( type )){
				
				return;
			}
			
			Map	details = new HashMap();
			
			details.put( "create_time", new Long( SystemTime.getCurrentTime()));
			
			if ( tweets.size() == 0 ){
				
				active_tweets.add( download );
			}
			
			tweets.put( type, details );
			
			download.setMapAttribute( ta_tweets, tweets );
		}
	}
	
	private void
	removeTweetRequest(
		Download		download,
		String			type )
	{
		synchronized( this ){
			
			Map tweets = download.getMapAttribute( ta_tweets );
	
			if ( tweets == null ){
				
				return;	
			}
				
			tweets = new HashMap( tweets );
		
			tweets.remove( type );
			
			if ( tweets.size() == 0 ){
				
				active_tweets.remove( download );
			}
			
			download.setMapAttribute( ta_tweets, tweets );	
		}
	}
	
	private void
	failedTweetRequest(
		Download		download,
		String			type )		
	{
		synchronized( this ){

			Map tweets = download.getMapAttribute( ta_tweets );

			if ( tweets != null ){
				
				tweets = new HashMap( tweets );
				
				Map details = (Map)tweets.get( type );
		
				if ( details != null ){
					
					details = new HashMap( details );
					
					tweets.put( type, details );
					
					if ( !details.containsKey( "fail_time" )){
						
						details.put( "fail_time", SystemTime.getCurrentTime());
					}
					
					Long fail_count = (Long)details.get( "fail_count" );
					
					if ( fail_count == null ){
						
						fail_count = 1L;
						
					}else{
						
						fail_count++;
					}
					
					details.put( "fail_count", fail_count );
				}
			}
			
			download.setMapAttribute( ta_tweets, tweets );
		}
	}
	
	private void
	recoverTweetRequests(
		Download	download )
	{
		synchronized( this ){

			Map<String,Map> tweets = (Map<String,Map>)download.getMapAttribute( ta_tweets );
			
			if ( tweets != null && tweets.size() > 0 ){
				
				tweets = new HashMap<String,Map>( tweets );
				
				for ( Map.Entry entry: tweets.entrySet()){
					
					Map details = new HashMap((Map)entry.getValue());
										
					details.remove( "fail_count" );
					details.remove( "fail_time" );
					
					entry.setValue( details );
				}
				
				active_tweets.add( download );
				
				download.setMapAttribute( ta_tweets, tweets );	
			}
		}
	}
	
	private TwitterResult
	sendTweet(
		Map<String,String>	params )
	{
		String	text = tweet_text.getValue().trim();
		
		if ( text.length() > 0 ){
		
			return( sendTweet( expandMessage( text, params )));
		}
		
		return( new TwitterResult());
	}
	
	private TwitterResult
	sendTweet(
		String		status )
	{
		TwitterResult	twitter_result;
		
		log.log( "Updating status: " + status );
		
		try{
			ConfigurationBuilder cb = new ConfigurationBuilder();
			
			//cb.setSource( "Vuze" );
			
			// Twitter twitter = new TwitterFactory( cb.build()).getInstance( twitter_user.getValue(), new String( twitter_password.getValue()));
			
			Status result;
				
			Twitter twitter = null;
			
			try{
				if ( access_token == null ){
					
					throw( new Exception( "Please configure your account settings in the plugin options" ));
				}
				
				cb.setOAuthAccessToken(access_token.getToken())
				.setOAuthAccessTokenSecret(access_token.getTokenSecret())
				.setOAuthConsumerKey(CONSUMER_KEY)
				.setOAuthConsumerSecret(CONSUMER_SECRET);

				twitter = new TwitterFactory(cb.build()).getInstance();

				result = twitter.updateStatus( status );
				
			}catch( Throwable e ){
					
					// hack for old clients that don't have correct trust store
				
				if ( 	twitter != null && 
						(e instanceof TwitterException) && ((TwitterException)e).getStatusCode() == -1 ){
				
					String truststore_name 	= FileUtil.getUserFile(SESecurityManager.SSL_CERTS).getAbsolutePath();
					
					File	target = new File( truststore_name );

					if ( !target.exists() || target.length() < 2*1024 ){
										
						File cacerts = new File( new File( new File( System.getProperty( "java.home" ), "lib" ), "security" ), "cacerts" );
						
						if ( cacerts.exists()){
						
							FileUtil.copyFile( cacerts, target );
						}
					}
					
						// this merely acts to trigger a load of the keystore
					
					plugin_interface.getUtilities().getSecurityManager().installServerCertificate( new URL( "https://twitter.com/" ));
					
					result = twitter.updateStatus( status );
					
				}else{
					
					throw( e );
				}
			}
			
			log.log( "Status updated to '" + result.getText() + "'" );
		
			twitter_result = new TwitterResult();
			
		}catch( TwitterException e ){
			
			int	status_code = e.getStatusCode();
			
			if ( status_code == 401 ){
				
				log.logAlert( LoggerChannel.LT_ERROR, "Twitter status update failed: id or password incorrect" );
				
				twitter_result = new TwitterResult( "Authentication failed: ID or password incorrect", false );
				
			}else if ( status_code == 403 ){
					
				log.logAlert( LoggerChannel.LT_ERROR, "Twitter status update failed: duplicate tweet rejected by the server" );
					
				twitter_result = new TwitterResult( "Tweet has already been sent!", false );
				
			}else if ( status_code >= 500 && status_code < 600 ){
				
				log.logAlert( LoggerChannel.LT_ERROR, "Twitter status update failed: Twitter is down or being upgraded" );
				
				twitter_result = new TwitterResult( "Tweet servers unavailable - try again later", true );
				
			}else{
				
				log.logAlert( "Twitter status update failed", e );
				
				twitter_result = new TwitterResult( Debug.getNestedExceptionMessage( e ), false );
			}
		}catch( Throwable e ){
			
			log.logAlert( "Twitter status update failed", e );
			
			twitter_result = new TwitterResult( Debug.getNestedExceptionMessage( e ), false );
		}
		
		return( twitter_result );
	}

	private String
	expandMessage(
		String					unexpanded,
		Map<String,String>		params )
	{
		String expanded = expandMessageSupport( unexpanded, params );
		
		if ( expanded.length() > 140 ){

			String title = params.get( "%t" );
			
			if ( title != null ){
				
				int	to_trim = expanded.length() - 137;
				
				if ( to_trim > 0 ){
					
					title = title.substring( 0, title.length() - to_trim ) + "...";
					
					params.put( "%t", title );
					
					expanded = expandMessageSupport( unexpanded, params );
				}
			}
		}
		
		if ( expanded.length() > 140 ){
			
			expanded = expanded.substring( 0, 140 );
		}
		
		return( expanded );
	}
	
	private String
	expandMessageSupport(
		String					str,
		Map<String,String>		params )
	{
		Iterator<String>	it = params.keySet().iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			String	val = (String)params.get(key);
			
			str = str.replaceAll( key, val );
		}
		
		return( str );
	}
	
	private class
	TwitterResult
	{
		private boolean ok;
		private String	error;
		private boolean	retry;
		
		protected
		TwitterResult()
		{
			ok	= true;
		}
		
		protected
		TwitterResult(
			String		_error,
			boolean		_retry )
		{
			error	= _error;
			retry	= _retry;
		}
		
		protected boolean
		isOK()
		{
			return( ok );
		}
		
		protected boolean
		canRetry()
		{
			return( retry );
		}
		
		protected String
		getError()
		{
			return( error );
		}
	}
}
