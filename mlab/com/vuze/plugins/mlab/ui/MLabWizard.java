/*
 * Created on May 24, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.vuze.plugins.mlab.ui;

import java.io.InputStream;
import java.util.*;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.vuze.plugins.mlab.MLabPlugin;

public class 
MLabWizard
	extends Wizard
{
	private MLabPlugin		plugin;
	private IPCInterface	callback;
	
	private List<Image>	images = new ArrayList<Image>();
	
	private long		up_rate;
	private long		down_rate;
	
	private boolean		downloads_paused;
	private boolean		finished;
	
	public
	MLabWizard(
		MLabPlugin		_plugin,
		IPCInterface	_callback )
	{
		super( "mlab.wizard.title" );
	
		plugin		= _plugin;
		callback	= _callback;
		
		MLabWizardStart panel = new MLabWizardStart( this );
		
		setFirstPanel( panel );
	}

	protected MLabPlugin
	getPlugin()
	{
		return( plugin );
	}
	
	protected Image
	getImage(
		String		res )
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream( res);
		
		if ( is != null ){
		        
			ImageData imageData = new ImageData( is );
		    
			Image img = new Image( getDisplay(), imageData );
			
			images.add( img );
			
			return( img );
		}
		
		return( null );
	}
	
	protected boolean
	pauseDownloads()
	{		
		if ( downloads_paused ){
			
			return( false );
		}
	
		DownloadManager download_manager = plugin.getPluginInterface().getDownloadManager();

		Download[] downloads = download_manager.getDownloads();
		
		boolean	active = false;

		for ( Download d: downloads ){
			
			int state = d.getState();
			
			if ( state != Download.ST_ERROR && state != Download.ST_STOPPED ){
				
				active = true;
				
				break;
			}
		}
		
		if ( active ){
			
			downloads_paused = true;
			
			download_manager.pauseDownloads();
			
			return( true );
		}else{
			
			return( false );
		}
	}
	
	protected void
	setRates(
		long		_up,
		long		_down )
	{
		up_rate		= _up;
		down_rate	= _down;
	}
	
	public void 
	onClose()
	{
		super.onClose();
		
		if ( downloads_paused ){
			
			plugin.getPluginInterface().getDownloadManager().resumeDownloads();
		}
		
		try{
			if ( finished ){
				
				Map<String,Object> args = new HashMap<String,Object>();
				
				args.put( "up", up_rate );
				args.put( "down", down_rate );
				
				callback.invoke( "results", new Object[]{ args });
				
			}else{
				
				callback.invoke( "cancelled", new Object[]{});

			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		for ( Image img: images ){
			
			img.dispose();
		}
	}
	
	public void
	finish()
	{
		finished	= true;
		
		close();
	}
}
