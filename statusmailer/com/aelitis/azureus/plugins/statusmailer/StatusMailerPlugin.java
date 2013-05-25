/*
 * Created on 08-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.statusmailer;

import java.util.*;
import java.io.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.*;

import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;
import com.sun.mail.smtp.SMTPTransport;


/**
 * @author parg
 *
 */

public class 
StatusMailerPlugin
	implements Plugin, UTTimerEventPerformer, DownloadManagerListener, DownloadListener
{
	private static final int MAIL_DELAY	= 5*1000;
	
	public static final String 	CONFIG_ENABLE			= "enable";
	public static final boolean CONFIG_ENABLE_DEFAULT	= true;
	
	public static final String 	CONFIG_SMTP_SERVER			= "smtp_server";
	public static final String 	CONFIG_SMTP_PORT			= "smtp_port";
	public static final int	 	CONFIG_SMTP_PORT_DEFAULT	= 25;
	public static final String 	CONFIG_SMTP_SSL				= "smtp_ssl";
	public static final boolean	CONFIG_SMTP_SSL_DEFAULT		= false;
	
	public static final String 	CONFIG_SMTP_USER		= "smtp_user";
	public static final String 	CONFIG_SMTP_PASSWORD	= "smtp_password";

	public static final String 	CONFIG_TO_ADDRESS		= "to_address";
	public static final String 	CONFIG_FROM_ADDRESS		= "from_address";
	
	public static final String 	CONFIG_LOCAL_HOST			= "local_host";

	public static final String 	CONFIG_USE_HTML			= "use_html";
	public static final boolean	CONFIG_USE_HTML_DEFAULT	= true;
	
	public static final String 	CONFIG_TRANS_SEEDING			= "trans_to_seeding";
	public static final boolean	CONFIG_TRANS_SEEDING_DEFAULT	= true;
	
	public static final String 	CONFIG_TRANS_DOWNLOADING			= "trans_to_downloading";
	public static final boolean	CONFIG_TRANS_DOWNLOADING_DEFAULT	= false;

	public static final String	CONFIG_DOWNLOAD_ADDED				= "download_added";
	public static final boolean	CONFIG_DOWNLOAD_ADDED_DEFAULT		= false;

	public static final String	CONFIG_DOWNLOAD_REMOVED				= "download_removed";
	public static final boolean	CONFIG_DOWNLOAD_REMOVED_DEFAULT		= false;

	public static final String 	CONFIG_MSG_SUBJECT					= "msg_subject";
	public static final String	CONFIG_MSG_SUBJECT_DEFAULT			= "Status change for %t";
	
	public static final String 	CONFIG_MSG_CONTENT					= "msg_content";
	public static final String	CONFIG_MSG_CONTENT_DEFAULT			= "Download %t has changed state from %o to %n";

	public static final String	CONFIG_MSG_SUBJECT_ADD_REMOVE			= "msg_subject_add_remove";
	public static final String	CONFIG_MSG_SUBJECT_ADD_REMOVE_DEFAULT	= "Download %a: %t";

	public static final String	CONFIG_MSG_CONTENT_ADD_REMOVE			= "msg_content_add_remove";
	public static final String	CONFIG_MSG_CONTENT_ADD_REMOVE_DEFAULT	= "Download %t was %a";

	public static final String	CONFIG_RATIOS						= "ratios";

	
	public static final String	CONFIG_ATTACH_TORRENT				= "attach_torrent";
	public static final boolean	CONFIG_ATTACH_TORRENT_DEFAULT		= false;
	
	public static final String	CONFIG_INCLUDE_FILES				= "include_files";
	public static final boolean	CONFIG_INCLUDE_FILES_DEFAULT		= false;
	
	public static final String 	CONFIG_NO_PROXY						= "no_proxy";
	public static final boolean	CONFIG_NO_PROXY_DEFAULT				= false;

	
	public static final String 	CONFIG_DEBUG_ON				= "debug_on";
	public static final boolean	CONFIG_DEBUG_ON_DEFAULT		= false;
	
	public static final long	TIMER_PERIOD			= 60*1000;
	
	protected PluginInterface		plugin_interface;
	
	protected LoggerChannel			log;
	protected UTTimer				timer;
	protected UTTimerEvent			timer_event;
	
	protected UTTimer               mail_timer;
	
	protected BooleanParameter 		enable;
	protected StringParameter 		from_address;
	protected StringParameter 		to_address;
	protected BooleanParameter		use_html;
	
	protected StringParameter 		smtp_server;
	protected IntParameter			smtp_port;
	protected BooleanParameter 		smtp_ssl;

	
	protected StringParameter		smtp_user;
	protected PasswordParameter		smtp_password;
	
	protected StringParameter		local_host;
	
	protected BooleanParameter 		trans_to_seeding;
	protected BooleanParameter 		trans_to_downloading;

	protected BooleanParameter		download_added;
	protected BooleanParameter		download_removed;

	protected BooleanParameter		attach_torrent;
	protected BooleanParameter		include_file_details;

	protected StringParameter		msg_subject;
	protected StringParameter		msg_content;

	protected StringParameter		msg_subject_add_remove;
	protected StringParameter		msg_content_add_remove;

	protected StringParameter		ratios;
	
	protected BooleanParameter		force_no_proxy;
	
	protected BooleanParameter		debug_on;

	protected String				mailer_name	= "Azureus";
		
	protected Map					download_ratios	= new HashMap(); 
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		mailer_name	= plugin_interface.getAzureusName() + "_" + plugin_interface.getAzureusVersion();
		
		LocaleUtilities loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		log	= plugin_interface.getLogger().getChannel( "Status Mailer");
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "statusmailer.name");
						
		enable 		= config_model.addBooleanParameter2( CONFIG_ENABLE, "statusmailer.enable", CONFIG_ENABLE_DEFAULT );

		trans_to_seeding 		= config_model.addBooleanParameter2( CONFIG_TRANS_SEEDING, 		"statusmailer.trans_to_seeding", 	CONFIG_TRANS_SEEDING_DEFAULT );
		trans_to_downloading	= config_model.addBooleanParameter2( CONFIG_TRANS_DOWNLOADING, 	"statusmailer.trans_to_downloading", CONFIG_TRANS_DOWNLOADING_DEFAULT );

		download_added		= config_model.addBooleanParameter2( CONFIG_DOWNLOAD_ADDED,		"statusmailer.download_added",		CONFIG_DOWNLOAD_ADDED_DEFAULT );
		download_removed	= config_model.addBooleanParameter2( CONFIG_DOWNLOAD_REMOVED,	"statusmailer.download_removed",	CONFIG_DOWNLOAD_REMOVED_DEFAULT );

		LabelParameter msg_info = config_model.addLabelParameter2( "statusmailer.msg_info" );
		
		msg_subject = config_model.addStringParameter2( CONFIG_MSG_SUBJECT, "statusmailer.msg_subject", CONFIG_MSG_SUBJECT_DEFAULT );
		
		msg_content = config_model.addStringParameter2( CONFIG_MSG_CONTENT, "statusmailer.msg_content", CONFIG_MSG_CONTENT_DEFAULT );
		
		msg_subject_add_remove = config_model.addStringParameter2( CONFIG_MSG_SUBJECT_ADD_REMOVE, "statusmailer.msg_subject_add_remove", CONFIG_MSG_SUBJECT_ADD_REMOVE_DEFAULT );

		msg_content_add_remove = config_model.addStringParameter2( CONFIG_MSG_CONTENT_ADD_REMOVE, "statusmailer.msg_content_add_remove", CONFIG_MSG_CONTENT_ADD_REMOVE_DEFAULT );

		ratios = config_model.addStringParameter2( CONFIG_RATIOS, "statusmailer.ratios", "" );

		attach_torrent 			= config_model.addBooleanParameter2( CONFIG_ATTACH_TORRENT, "statusmailer.attach_torrent",  CONFIG_ATTACH_TORRENT_DEFAULT );
		include_file_details 	= config_model.addBooleanParameter2( CONFIG_INCLUDE_FILES, "statusmailer.include_files",  CONFIG_INCLUDE_FILES_DEFAULT );

		
		
		
		
		to_address = config_model.addStringParameter2( CONFIG_TO_ADDRESS, "statusmailer.to_address", "" );

		from_address = config_model.addStringParameter2( CONFIG_FROM_ADDRESS, "statusmailer.from_address", "" );
		
		use_html	 = config_model.addBooleanParameter2( CONFIG_USE_HTML, "statusmailer.use_html", CONFIG_USE_HTML_DEFAULT );
		
		smtp_server	 	= config_model.addStringParameter2( CONFIG_SMTP_SERVER, "statusmailer.smtp_server", "" );
		smtp_port 		= config_model.addIntParameter2( CONFIG_SMTP_PORT, "statusmailer.smtp_port",  CONFIG_SMTP_PORT_DEFAULT );
		smtp_ssl 		= config_model.addBooleanParameter2( CONFIG_SMTP_SSL, "statusmailer.smtp_ssl",  CONFIG_SMTP_SSL_DEFAULT );
		
		LabelParameter ssl_info = config_model.addLabelParameter2( "statusmailer.ssl_info" );

		LabelParameter smtp_auth = config_model.addLabelParameter2( "statusmailer.smtp_auth" );
		
		smtp_user 		= config_model.addStringParameter2( CONFIG_SMTP_USER, "statusmailer.smtp_user", "" );
		smtp_password 	= config_model.addPasswordParameter2( CONFIG_SMTP_PASSWORD, "statusmailer.smtp_password", PasswordParameter.ET_PLAIN, new byte[0] );
		
		local_host	 	= config_model.addStringParameter2( CONFIG_LOCAL_HOST, "statusmailer.local_host", "" );

		force_no_proxy	= config_model.addBooleanParameter2( CONFIG_NO_PROXY, "statusmailer.force_no_proxy", CONFIG_NO_PROXY_DEFAULT );

		ActionParameter action = config_model.addActionParameter2( "statusmailer.send_test_email", "statusmailer.send" );
		
		debug_on	 = config_model.addBooleanParameter2( CONFIG_DEBUG_ON, "statusmailer.debug_on", CONFIG_DEBUG_ON_DEFAULT );

		action.addConfigParameterListener(
			new ConfigParameterListener()
			{
				public void
				configParameterChanged(
					ConfigParameter	param )
				{
					plugin_interface.getUtilities().createThread(
						"Mail Test",
						new Runnable()
						{
							public void
							run()
							{
								sendMail( null, getTestParams(), 0 );
							}
						});
				}
			});
		
		final BasicPluginViewModel	view_model = ui_manager.createBasicPluginViewModel( loc_utils.getLocalisedMessageText( "statusmailer.name" ));

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
		
		
		log.log( "From address = '" + from_address.getValue() + "'" );
		log.log( "To Address = '" + to_address.getValue() + "'" );
		log.log( "SMTP Server = '" + smtp_server.getValue() + "'" );
		log.log( "SMTP Port = '" + smtp_port.getValue() + "'" );
		log.log( "SMTP User = '" + smtp_user.getValue() + "'" );
		log.log( "Local Host = '" + local_host.getValue() + "'" );
		
		view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
		
		timer = plugin_interface.getUtilities().createTimer("refresher");
		mail_timer = plugin_interface.getUtilities().createTimer("mailtimer", false); // non-lightweight
		
		timer_event = timer.addPeriodicEvent( TIMER_PERIOD, this );
		

		enable.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter		p )
				{					
					view_model.getStatus().setText( enable.getValue()?"Enabled":"Disabled");
				}
			});	
		

		
		enable.addEnabledOnSelection( smtp_server );
		enable.addEnabledOnSelection( smtp_port );
		enable.addEnabledOnSelection( smtp_ssl );
		enable.addEnabledOnSelection( ssl_info );
		enable.addEnabledOnSelection( smtp_user );
		enable.addEnabledOnSelection( smtp_password );
		enable.addEnabledOnSelection( trans_to_seeding );
		enable.addEnabledOnSelection( trans_to_downloading );
		enable.addEnabledOnSelection( download_added );
		enable.addEnabledOnSelection( download_removed );
		enable.addEnabledOnSelection( msg_info );
		enable.addEnabledOnSelection( msg_subject );
		enable.addEnabledOnSelection( msg_content );
		enable.addEnabledOnSelection( msg_subject_add_remove );
		enable.addEnabledOnSelection( msg_content_add_remove );
		enable.addEnabledOnSelection( attach_torrent );
		enable.addEnabledOnSelection( include_file_details );
		enable.addEnabledOnSelection( from_address );
		enable.addEnabledOnSelection( to_address );
		enable.addEnabledOnSelection( smtp_auth );
		
		plugin_interface.getDownloadManager().addListener( this );
		
			// add handlers for main MIME types
		
		MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
		
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
		
		CommandMap.setDefaultCommandMap(mc);
	}
	
	public void
	perform(
		UTTimerEvent		event )
	{
		String	rat = ratios.getValue().trim();
		
		if ( rat.length() == 0 ){
			
			return;
		}
		
		StringTokenizer	tok = new StringTokenizer(rat, ";" );
		
		int[]	rats = new int[tok.countTokens()];
		
		int	pos = 0;
		
		while( tok.hasMoreTokens()){
			
			String	r = tok.nextToken().trim();
			
			try{
				rats[pos++] = (int)(Double.parseDouble(r)*1000);
				
			}catch( Throwable e ){
				
				log.log( "invalid ratio '" + r + "'" );
			}
		}
		
		Arrays.sort( rats );
		
		Iterator	it = download_ratios.keySet().iterator();
		
		while( it.hasNext()){
			
			Download	dl = (Download)it.next();
			
			int	last_ratio 		= ((Integer)download_ratios.get(dl)).intValue();
			int	current_ratio	= dl.getStats().getShareRatio();
			
			if ( current_ratio < 0 ){
				
				current_ratio	= 0;
			}
			
			// log.log( "Checking ratio for '" + dl.getName() + "': last = " + last_ratio + ", current = " + current_ratio );
			
			for (int i=rats.length-1;i>=0;i--){
				
				if ( last_ratio < rats[i] && current_ratio >= rats[i] ){
					
					download_ratios.put( dl, new Integer( current_ratio ));
					
					stateChanged( dl, dl.getState(), dl.getState());
					
					break;
				}
			}
		}
	}
	
	
	public void
	downloadAdded(
		Download	download )
	{
		if ( download.getFlag( Download.FLAG_LOW_NOISE )){
			
			return;
		}
		
		log.log( "Download added:" + download.getName());
		
		int	share_ratio = download.getStats().getShareRatio();
		
		if ( share_ratio < 0 ){
			
			share_ratio	= 0;
		}
		
		download_ratios.put( download, new Integer(share_ratio));
		
		if ( !download.isComplete()){
			
			download.addListener( this );

			if ( download_added.getValue()){

				// Check that the creation time is within the last 20 seconds
				// to avoid a duplicate notification when Azureus is restarted.
				long creationTime = download.getCreationTime();
				long now = System.currentTimeMillis();
				
				if ( creationTime <= now && creationTime > now - 20000 )
				{

					sendMail( download, true );
				}
			}
		}
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		if ( download.getFlag( Download.FLAG_LOW_NOISE )){
			
			return;
		}
		
		log.log( "Download removed:" + download.getName());
		
		download_ratios.remove( download );
		
		download.removeListener( this );

		if ( download_removed.getValue()) {

			sendMail( download, false );
		}
	}
	
	public void
	stateChanged(
		Download		download,
		int				old_state,
		int				new_state )
	{
		if ( download.getFlag( Download.FLAG_LOW_NOISE )){
			
			return;
		}
		
		log.log( 	"Download state changed:" + 
					download.getName() + ": " + Download.ST_NAMES[old_state] + " -> " + Download.ST_NAMES[new_state] );

		if ( old_state == Download.ST_DOWNLOADING ){
			
			if ( 	new_state == Download.ST_SEEDING ||
					new_state == Download.ST_ERROR ){
				
				download.removeListener( this );
				
				if ( trans_to_seeding.getValue()){
					
					sendMail( download, old_state, new_state, MAIL_DELAY );
				}
			}
		}else if ( new_state == Download.ST_DOWNLOADING ){
			
			if ( trans_to_downloading.getValue()){
				
				sendMail( download, old_state, new_state, MAIL_DELAY );
			}
		}
	}
  
	public void
	positionChanged(
		Download	download, 
		int 		oldPosition,
		int 		newPosition )
	{
		
	}
	
	protected Map
	getTestParams()
	{
		Map	params = new HashMap();
		
		params.put( "%t",	"'<torrent name>'" );
		params.put( "%o",	"<old state>" );
		params.put( "%n", 	"<new state>" );
		params.put( "%r",	"<ratio>" );
		params.put( "%a",	"<added/removed>" );
		params.put( "%s",	"<size>" );
		params.put( "%m",	"<magnet URI>" );
	
		return( params );
	}
	
	protected void
	sendMail(
		Download		download,
		int				old_state,
		int				new_state,
		int				delay )
	{
		if ( enable.getValue()){
			
			Map	params = new HashMap();
			
			Torrent	torrent = download.getTorrent();
						
			params.put( "%t",	"'" + download.getName() + "'" );
			params.put( "%o",	"" + Download.ST_NAMES[old_state] );
			params.put( "%n", 	"" + Download.ST_NAMES[new_state]);
			params.put( "%r",	"" + (((float)download.getStats().getShareRatio())/1000));
			params.put( "%s",	"" + (torrent==null?-1:torrent.getSize()));
			params.put( "%m",	(torrent==null?"":UrlUtils.getMagnetURI( torrent.getHash())));
					
			sendMail( download, params, delay );

		}else{
			
			log.log( "Notification ignored as plugin not enabled" );
		}
	}
	
	protected void
	sendMail(
		Download		download,
		boolean			added )
	{
		if ( enable.getValue()){

			Map params = new HashMap();

			Torrent	torrent = download.getTorrent();
			
			params.put( "%t",	"'" + download.getName() + "'" );
			params.put( "%o",	Download.ST_NAMES[download.getState()] );
			params.put( "%n",	Download.ST_NAMES[download.getState()] );
			params.put( "%r",	Float.toString(download.getStats().getShareRatio()/1000));
			params.put( "%a",	added ? "added" : "removed" );
			params.put( "%s",	"" + (torrent==null?-1:torrent.getSize()));
			params.put( "%m",	(torrent==null?"":UrlUtils.getMagnetURI( torrent.getHash())));

			sendAddRemoveMail( download, params, MAIL_DELAY );

		}else{

			log.log( "Notification ignored as plugin not enabled" );
		}
	}

	protected String
	expandMessage(
		String	str,
		Map		params )
	{
		Iterator	it = params.keySet().iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			String	val = (String)params.get(key);
			
			str = str.replaceAll( key, val );
		}
		
		return( str );
	}
	
	protected void
	sendMail(
		Download	download,
		Map			params,
		int			delay )
	{
		sendMessage( download, msg_subject.getValue(), msg_content.getValue(), params, delay );

	}
	
	protected void
	sendAddRemoveMail(
		Download	download,
		Map			params,
		int			delay )
	{
		sendMessage( download, msg_subject_add_remove.getValue(), msg_content_add_remove.getValue(), params, delay );
	}

	protected void
	sendMessage(
		final Download	download,
		final String	subject,
		final String	content,
		final Map		params, 
		final int		delay )
	{
		new DelayedEvent(
			"SMP:delay",
			delay,
			new AERunnable()
			{
				public void
				runSupport()
				{
						// the point of the delay is to allow the download's category to be set before
						// we perform the expansion - not ideal I know but this is significant on
						// an 'add-download' event where programatically the download is added
						// and then has its category set (it isn't possible to set the category before
						// the added event is received)
					
					if ( download != null ){
						
						String cat = download.getCategoryName();
						
						if ( cat == null || cat.length() == 0 || cat.equals( "Categories.uncategorized" )){
				
							cat = "Uncategorized";
						}
				
						params.put( "%c",	cat );
						
					}else{
						
						params.put( "%c",	"Uncategorized" );
					}
			
					File[]	files = null;
					
					if ( attach_torrent.getValue() && download != null ){
						
						files = new File[]{ new File( download.getTorrentFileName())};
					}
					
					String	expanded_content =  expandMessage( content, params );
					
					String[] content_lines;
					
					if ( include_file_details.getValue() && download != null ){
						
						DiskManagerFileInfo[] d_files = download.getDiskManagerFileInfo();
						
						content_lines = new String[d_files.length+1];
						
						content_lines[0] = expanded_content;
						
						for ( int i=0; i<d_files.length;i++ ){
							
							DiskManagerFileInfo f = d_files[i];
						
							long	perthou;
							
							long	size 	= f.getLength();
							long	done	= f.getDownloaded();
							
							if ( size == done || size == 0 ){
								
								perthou = 1000;
								
							}else{
								
								perthou = (1000 * done ) / size;
							}
							
							String f_str = "    " + f.getFile().getName() + ", " + DisplayFormatters.formatByteCountToKiBEtc( f.getLength()) + ":  " + DisplayFormatters.formatPercentFromThousands((int) perthou );
								
							content_lines[i+1] = f_str;
						}
					}else{
						
						content_lines = new String[]{ expanded_content };
					}
					
					sendMessage(	smtp_server.getValue(),
									smtp_port.getValue(),
									smtp_ssl.getValue(),
									smtp_user.getValue().trim(),
									new String( smtp_password.getValue()).trim(),
									use_html.getValue(),
									expandMessage( subject, params ),
									expandMessage( to_address.getValue(),params ),
									from_address.getValue(),
									content_lines,
									files );		
				}
			});
	}
	
		/**
		 * Added for IPC based dispatch
		 */
	
	public void
	sendMessage(
		String subject,
		String to,
		String content )
	{
		sendMessage( 
				smtp_server.getValue(),
				smtp_port.getValue(),
				smtp_ssl.getValue(),
				smtp_user.getValue().trim(),
				new String(smtp_password.getValue()).trim(),
				use_html.getValue(),
				subject,
				to,
				from_address.getValue(),
				new String[]{ content },
				null );
	}
	
	/**
	 * The below methods below are preferred for IPC - the other methods pay
	 * attention to the "use HTML" setting, and if that is true, then it will
	 * add "Azureus download status notification" in the HTML. We should
	 * probably rework it so that such a hacky thing doesn't take place, but
	 * this will do for now.
	 */
	
	// 
	public void ipcSendMessage(String subject, String to, String content) {
		sendMessage( 
				smtp_server.getValue(),
				smtp_port.getValue(),
				smtp_ssl.getValue(),
				smtp_user.getValue().trim(),
				new String(smtp_password.getValue()).trim(),
				false,
				subject,
				to,
				from_address.getValue(),
				new String[]{ content },
				null);		
	}
	
	public void ipcSendMessage(String subject, String to, String content, File[] attachments) {
		sendMessage( 
				smtp_server.getValue(),
				smtp_port.getValue(),
				smtp_ssl.getValue(),
				smtp_user.getValue().trim(),
				new String(smtp_password.getValue()).trim(),
				false,
				subject,
				to,
				from_address.getValue(),
				new String[]{ content },
				attachments);		
	}
    
    public void
    sendMessage(
		final String		server,
		final int			port,
		final boolean		ssl,
		final String		user_name,
		final String		password,
		final boolean		html_message,
		final String		subject,
		final String		to,
		final String		from,
		final String[]	    lines,
		final File[]        attachments)
    {

    	// Perform this in a different thread.
    	timer.addEvent(plugin_interface.getUtilities().getCurrentSystemTime(), new UTTimerEventPerformer() {
    		public void perform(UTTimerEvent event) {
    			sendMessage0(server, port, ssl, user_name, password, html_message, subject, to, from, lines, attachments);
    		}
    	});
    }
    
    private void
    sendMessage0(
		String		server,
		int			port,
		boolean		ssl,
		String		user_name,
		String		password,
		boolean		html_message,
		String		subject,
		String		to,
		String		from,
		String[]	lines,
		File[]      attachments)
    {
    	ClassLoader original = Thread.currentThread().getContextClassLoader();
    	
    	boolean no_proxy = force_no_proxy.getValue();
    	
    	try{
    		if ( no_proxy ){
    			
    			AEProxySelectorFactory.getSelector().startNoProxy();
    		}
    		
    		Thread.currentThread().setContextClassLoader( StatusMailerPlugin.class.getClassLoader());
    		
    	    Properties props = System.getProperties();
    	     
    		props.put( "mail.smtp.host", server);
    		props.put( "mail.smtp.port", "" + port );
    		
    		if( user_name.length() > 0 ){
    			
    			props.put( "mail.smtp.auth", "true" );
    		}
    		

       		if ( ssl ){
 
       			String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    		
       			props.setProperty( "mail.smtp.socketFactory.class", SSL_FACTORY);
       			props.setProperty( "mail.smtp.socketFactory.fallback", "false");
       		}
    		
    	    Session session = Session.getInstance( props );
    	    	
       		session.setDebug( debug_on.getValue());
      			
   	
    	    SMTPTransport transport = (SMTPTransport)session.getTransport("smtp");

    	    String	lh = local_host.getValue().trim();
    	    
    	    if ( lh.length() > 0 ){
    	    	
    	    	transport.setLocalHost( lh );
    	    }
    	    
    	    transport.connect( server, port, user_name, password );
    	    
    	    boolean has_attachments = attachments != null && attachments.length > 0;
    	    
    	    MimeMessage msg = new MimeMessage(session);
    	    MimePart text_part = (has_attachments) ? (MimePart)new MimeBodyPart() : msg;
    	    
    		msg.setFrom(new InternetAddress(from));
 	    
    	    msg.setRecipients(
    	    	Message.RecipientType.TO,
    			InternetAddress.parse(to, false));
  
    	    msg.setSubject(subject);

        	StringBuffer sb = new StringBuffer();
        	
        	if ( html_message ){
	        	sb.append("<HTML>\n");
	        	sb.append("<HEAD>\n");
	        	sb.append("<TITLE>\n");
	        	
	        	sb.append( "Azureus download status notification" + "\n");
	        	
	        	sb.append("</TITLE>\n");
	        	sb.append("</HEAD>\n");
	
	        	sb.append("<BODY>\n");
	        	sb.append("<H1>" + "Azureus download status notification" + "</H1>" + "\n");
	
	        	for (int i=0;i<lines.length;i++){
	       
	        	    sb.append(lines[i].replaceAll( " ", "&nbsp;" ));
	        	    sb.append("<P>");
	        	}
	
	        	sb.append("</BODY>\n");
	        	sb.append("</HTML>\n");

        	}else{
        		
	        	for (int i=0;i<lines.length;i++){
	     	       
	     	        sb.append(lines[i]);
	     	        sb.append("\n");
	     	    }       	
	       	}
        	
        	text_part.setDataHandler(
        		new DataHandler(
        				new ByteArrayDataSource(sb.toString(), html_message?"text/html":"text/plain")));

        	if (has_attachments) {
        		Multipart multipart = new MimeMultipart();
        		multipart.addBodyPart((BodyPart)text_part);
        		for (int i=0; i<attachments.length; i++) {
        			MimeBodyPart message_body_part = new MimeBodyPart();
        			DataSource source = new FileDataSource(attachments[i]);
        			message_body_part.setDataHandler(new DataHandler(source));
        			message_body_part.setFileName(attachments[i].getName());
        			multipart.addBodyPart(message_body_part);
        		}
        		msg.setContent(multipart);
        	}
        	
    	    msg.setHeader("X-Mailer", mailer_name );
    	    
    	    msg.setSentDate(new Date());

    	    msg.saveChanges();
    	    
    	    transport.sendMessage( msg, msg.getAllRecipients());
  
    	    transport.close();
    	    
    	}catch (Throwable e ){
    	   
    		e.printStackTrace();
    		
    		if ( log != null ){
    			
    			log.log(e);
    		}
    	}finally{
    		
       		Thread.currentThread().setContextClassLoader( original );
       		
       		if ( no_proxy ){
    			
    			AEProxySelectorFactory.getSelector().endNoProxy();
    		}
    	}
    }

	



	protected class 
	ByteArrayDataSource 
		implements DataSource 
	{
	    private byte[] 	data;
	    private String 	type;

	    public 
		ByteArrayDataSource(
			InputStream is, 
			String 		_type ) 
	    {
	        type = _type;
	        
	        try {
	            ByteArrayOutputStream os = new ByteArrayOutputStream();
	            
	            int ch;

	            while ((ch = is.read()) != -1){
	            
	            	os.write(ch);
	            }
	            
	            data = os.toByteArray();

	        } catch (IOException e){
	        	
	        	e.printStackTrace();
	        }
	    }

	    public 
		ByteArrayDataSource(
			byte[] _data, 
			String _type) 
	    {
	        data = _data;
	        type = _type;
	    }

	    public 
		ByteArrayDataSource(
			String _data, 
			String _type) 
	    {
	        try {
	            data = _data.getBytes("iso-8859-1");
	            
	        } catch (UnsupportedEncodingException e){
	        
	        	e.printStackTrace();
	        }
	        
	        type = _type;
	    }

	    public InputStream 
		getInputStream() 
	    	throws IOException 
		{
	        if (data == null){
	        
	        	throw new IOException("No input data available");
	        }
	        
	        return( new ByteArrayInputStream(data));
	    }

	    public OutputStream 
		getOutputStream() 
	    	throws IOException 
		{
	        throw( new IOException("Not Supported"));
	    }

	    public String 
		getContentType() 
	    {
	        return( type );
	    }

	    public String 
		getName() 
	    {
	        return( "Azureus data source" );
	    }
	}

	public static void
	main(
		String[]		args )
	{
		new StatusMailerPlugin().sendMessage( 
				"relay.plus.net", 
				25,
				false,
				"",
				"",
				false,
				"hello", 
				"parg@users.sourceforge.net", 
				"parg@users.sourceforge.net",
				new String[]{ "parp parp parp" },
				null );
	}
}
