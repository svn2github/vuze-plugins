/**
 * Created on Sep 28, 2012
 *
 * Copyright 2011 Vuze, LLC.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.aelitis.plugins.rcmplugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.search.*;

import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author TuxPaper
 * @created Sep 28, 2012
 *
 */
public class RCM_SearchProvider
	implements SearchProvider
{
	private RCMPlugin	plugin;
	
	private Map<Integer,Object>	properties = new HashMap<Integer, Object>();
	
	protected
	RCM_SearchProvider(
		RCMPlugin	_plugin )
	{
		plugin	= _plugin;
		
		properties.put( PR_NAME, MessageText.getString( "rcm.search.provider" ));
		
		try{
			URL url = 
				MagnetURIHandler.getSingleton().registerResource(
					new MagnetURIHandler.ResourceProvider()
					{
						public String
						getUID()
						{
							return( RelatedContentManager.class.getName() + ".1" );
						}
						
						public String
						getFileType()
						{
							return( "png" );
						}
								
						public byte[]
						getData()
						{
							InputStream is = getClass().getClassLoader().getResourceAsStream( "org/gudy/azureus2/ui/icons/rcm.png" );
							
							if ( is == null ){
								
								return( null );
							}
							
							try{
								ByteArrayOutputStream	baos = new ByteArrayOutputStream();
								
								try{
									byte[]	buffer = new byte[8192];
									
									while( true ){
			
										int	len = is.read( buffer );
						
										if ( len <= 0 ){
											
											break;
										}
				
										baos.write( buffer, 0, len );
									}
								}finally{
									
									is.close();
								}
								
								return( baos.toByteArray());
								
							}catch( Throwable e ){
								
								return( null );
							}
						}
					});
													
			properties.put( PR_ICON_URL, url.toExternalForm());
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public SearchInstance
	search(
		Map<String,Object>	search_parameters,
		SearchObserver		observer )
	
		throws SearchException
	{		
		try{
			RelatedContentManager manager = RelatedContentManager.getSingleton();
			
			return( manager.searchRCM( search_parameters, new SearchObserverFilter( manager, observer )));
			
		}catch( Throwable e ){
			
			throw( new SearchException( "Search failed", e ));
		}
	}
	
	public Object
	getProperty(
		int			property )
	{
		return( properties.get( property ));
	}
	
	public void
	setProperty(
		int			property,
		Object		value )
	{
		properties.put( property, value );
	}
	
	private class
	SearchObserverFilter
		implements SearchObserver
	{
		private RelatedContentManager				manager;
		private ByteArrayHashMap<RelatedContent>	hash_map;
		
		private SearchObserver				observer;
		
		private int	min_rank = plugin.getMinuumSearchRank();
		
		private
		SearchObserverFilter(
			RelatedContentManager	_manager,
			SearchObserver			_observer )
		{
			manager		= _manager;
			observer 	= _observer;
			
			if ( min_rank > 0 ){
				
				hash_map = new ByteArrayHashMap<RelatedContent>();
				
				RelatedContent[]	rc = manager.getRelatedContent();
				
				for ( RelatedContent r: rc ){
					
					byte[] hash = r.getHash();
					
					if ( hash != null ){
						
						hash_map.put( hash, r );
					}
				}
			}
		}
		
		public void
		resultReceived(
			SearchInstance		search,
			SearchResult		result )
		{
			if ( hash_map == null){
				
				long	cnet = (Long)result.getProperty( 50000 );
				
				if ( !plugin.isVisible( cnet )){
					
					byte[]	tracker_keys = (byte[])result.getProperty( 50001 );
					
					if ( !plugin.isVisible( tracker_keys )){
						
						byte[]	ws_keys = (byte[])result.getProperty( 50002 );
	
						if ( !plugin.isVisible( ws_keys )){
							
							return;
						}
					}
				}
				
				observer.resultReceived( search, result );
				
			}else{
				
				byte[] hash = (byte[])result.getProperty( SearchResult.PR_HASH );
				
				if ( hash != null ){
					
					RelatedContent rc = hash_map.get( hash );
					
					if ( rc.getRank() >= min_rank ){
						
						observer.resultReceived( search, result );
					}
				}
			}
		}
		
		public void
		complete()
		{
			observer.complete();
		}
		
		public void
		cancelled()
		{
			observer.cancelled();
		}
		
		public Object
		getProperty(
			int		property )
		{
			return( observer.getProperty(property));
		}
	}
}
