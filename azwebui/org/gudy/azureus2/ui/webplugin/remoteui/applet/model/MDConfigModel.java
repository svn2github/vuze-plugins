/*
 * File    : MDConfigModel.java
 * Created : 17-Feb-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet.model;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.pluginsimpl.remote.RPException;

public class 
MDConfigModel 
{
	protected PluginInterface	pi;
	
	protected PluginConfig		plugin_config;
	
	protected int		refresh_period;
	protected int		max_upload;
	protected int		max_download;
	protected int		max_connections_per_torrent;
	protected int		max_connections_global;
	
	protected List		listeners = new ArrayList();
	
	public
	MDConfigModel(
		PluginInterface	_pi )
	{
		pi		= _pi;
		
		plugin_config = pi.getPluginconfig();
		
		refresh_period = plugin_config.getPluginIntParameter( "MDConfigModel:refresh_period", 30 );
		
		max_upload = 
			plugin_config.getIntParameter( 
				PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC,
				COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED );
		
		max_download = 
			plugin_config.getIntParameter( 
				PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC,
				COConfigurationManager.CONFIG_DEFAULT_MAX_DOWNLOAD_SPEED );

		max_connections_per_torrent = 
			plugin_config.getIntParameter( 
				PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT,
				COConfigurationManager.CONFIG_DEFAULT_MAX_CONNECTIONS_PER_TORRENT );

		max_connections_global = 
			plugin_config.getIntParameter( 
				PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL,
				COConfigurationManager.CONFIG_DEFAULT_MAX_CONNECTIONS_GLOBAL );
	}
	
	public int
	getRefreshPeriod()
	{
		return( refresh_period );
	}
	
	public void
	setRefreshPeriod(
		int	v )
	{
		refresh_period = v;
			
		plugin_config.setPluginParameter( "MDConfigModel:refresh_period", refresh_period );
		
		try{
			plugin_config.save();
			
		}catch( PluginException e ){
			
			throw( new RPException("setRefreshPeriod Fails", e ));
		}
		
		fireEvent( MDConfigModelPropertyChangeEvent.PT_REFRESH_PERIOD, new Integer( refresh_period ));
	}
	
	public int
	getMaxUploadSpeed()
	{
		return( max_upload );
	}
	
	public void
	setMaxUploadSpeed(
		int		v )
	{
		if ( v > 0 && v < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED ){
			
			throw( new RPException( "Maximum upload speed must be at least " + COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED ));
		}
				
		plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, v );
		
		max_upload = v;
		
		try{
			plugin_config.save();
			
		}catch( PluginException e ){
			
			throw( new RPException("setMaxUploadSpeed Fails", e ));
		}
	
	}
	
	public int
	getMaxDownloadSpeed()
	{
		return( max_download );
	}
	
	public void
	setMaxDownloadSpeed(
		int		v )
	{	
		plugin_config.setIntParameter( PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, v );
		
		max_download = v;
		
		try{
			plugin_config.save();
			
		}catch( PluginException e ){
			
			throw( new RPException("setMaxDownloadSpeed Fails", e ));
		}
	
	}
	
	public int
	getMaxConnectionsPerTorrent()
	{
		return( max_connections_per_torrent );
	}
	
	public void
	setMaxConnectionsPerTorrent(
		int		v )
	{		
		plugin_config.setIntParameter( 
				PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT, v );
		
		max_connections_per_torrent = v;
		
		try{
			plugin_config.save();
			
		}catch( PluginException e ){
			
			throw( new RPException("setMaxConnectionsPerTorrent Fails", e ));
		}
	
	}
	
	public int
	getMaxConnectionsGlobal()
	{
		return( max_connections_global );
	}
	
	public void
	setMaxConnectionsGlobal(
		int		v )
	{		
		plugin_config.setIntParameter( 
				PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL, v );
		
		max_connections_global = v;
		
		try{
			plugin_config.save();
			
		}catch( PluginException e ){
			
			throw( new RPException("setMaxConnectionsGlobal Fails", e ));
		}
	}
	
	protected void
	fireEvent(
		int		type,
		Object	value )
	{
		MDConfigModelPropertyChangeEvent	ev = new MDConfigModelPropertyChangeEvent( type, value );
		
		for (int i=0;i<listeners.size();i++){
			
			((MDConfigModelListener)listeners.get(i)).propertyChanged(ev);
		}
	}
	
	public void
	addListener(
			MDConfigModelListener	l )
	{
		listeners.add( l );
	}
}
