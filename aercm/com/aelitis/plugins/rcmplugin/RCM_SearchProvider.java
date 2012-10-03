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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.search.*;

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

	private Map<Integer,Object>	properties = new HashMap<Integer, Object>();
	
	protected
	RCM_SearchProvider()
	{
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
			return( RelatedContentManager.getSingleton().searchRCM( search_parameters, observer ));
			
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
}
