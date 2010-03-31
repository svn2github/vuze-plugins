/*
 * File    : TrackerWebDefaultStaticPlugin.java
 * Created : 09-Dec-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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

package org.gudy.azureus2.ui.tracker;


import java.io.*;
import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.tracker.web.*;


public class 
TrackerWebDefaultStaticPlugin
	extends TrackerWeb
{
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		super.initialize( _plugin_interface );
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		if ( !plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE, CONFIG_TRACKER_PUBLISH_ENABLE_DEFAULT )){
			
			return( false );
		}
				
		String requestURL = request.getAbsoluteURL().toString();
		int trackerURL_end = requestURL.lastIndexOf("/")+1;
		String trackerURL = requestURL.substring(0,trackerURL_end);

		String	url = request.getURL();

		Hashtable	params = null;
		
		url = mapHomePage( url );
		
		int	p_pos = url.indexOf( '?' );
		
		if ( p_pos != -1 ){
			
			params = decodeParams( url.substring( p_pos+1 ));
			
			url = url.substring(0,p_pos);
		}
		
		OutputStream	os = response.getOutputStream();
		
		String	target = file_root + url.replace('/',File.separatorChar);
		File canonical_file = new File(target).getCanonicalFile();
		//System.out.println( "static request: " + canonical_file.toString());
		
			// make sure some fool isn't trying to use ../../ to escape from web dir
		
		if ( !canonical_file.toString().startsWith( file_root )){
			
			return( false );
		}
		
		if ( canonical_file.isDirectory()){
			
			return( false );
		}

		if ( canonical_file.canRead()){
			
			String str = canonical_file.toString().toLowerCase();
			
			int	pos = str.lastIndexOf( "." );
			
			if ( pos == -1 ){
				
				return( false );
			}
			
			String	file_type = str.substring(pos+1);
			
			if ( file_type.equals("php") || file_type.equals("tmpl")){
			
				Hashtable	args = new Hashtable();
			
				args.put( "filename", canonical_file.toString());
				
				return( handleTemplate( trackerURL, url, params, args, os ));
				
			}else{ 
				
				FileInputStream	fis = null;
				
				try{
					fis = new FileInputStream(canonical_file);
					
					response.useStream( file_type, fis );
					
					return( true );
					
				}finally{
					
					if ( fis != null ){
						
						fis.close();
					}
				}
			}
		}
		
		return( false );
	}
}	