/*
 * Created on Dec 18, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.parg.azureus.plugins.networks.tor.swt;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.parg.azureus.plugins.networks.tor.TorPlugin;
import org.parg.azureus.plugins.networks.tor.TorPluginUI;

public class 
TorPluginUISWT
	implements TorPluginUI
{
	private LocaleUtilities		lu;
	
	private List<Shell>			active_shells = new ArrayList<Shell>();
	
	private volatile boolean		destroyed;
	
	public
	TorPluginUISWT(
		TorPlugin	_plugin )
	{
		lu	= _plugin.getPluginInterface().getUtilities().getLocaleUtilities();
	}
	
	public boolean
	promptForHost(
		String	host )
	{
		if ( Utils.isSWTThread()){
			
			Debug.out( "Invocation on SWT thead not supported" );
			
			return( false );
		}
		
		if ( destroyed ){
			
			return( false );
		}
		
		final AESemaphore	wait_sem 	= new AESemaphore( "wait" );
		final boolean[]		result  	= {false};
		
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					try{
						final Shell shell = ShellFactory.createMainShell( SWT.DIALOG_TRIM );
				
						shell.addDisposeListener(
							new DisposeListener()
							{
								public void 
								widgetDisposed(
									DisposeEvent arg0 ) 
								{
									synchronized( TorPluginUISWT.this ){
										
										active_shells.remove( shell );
									}
									
									wait_sem.release();
								}
							});
						
						synchronized( TorPluginUISWT.this ){
							
							if ( destroyed ){
								
								shell.dispose();
								
								return;
								
							}else{
								
								active_shells.add( shell );
							}
						}
						
						shell.setText( lu.getLocalisedMessageText( "aztorplugin.ask.title" ));
								
						Utils.setShellIcon(shell);
						
						GridLayout layout = new GridLayout();
						layout.numColumns = 2;
						layout.marginHeight = 0;
						layout.marginWidth = 0;
						shell.setLayout(layout);
						GridData grid_data = new GridData(GridData.FILL_BOTH );
						shell.setLayoutData(grid_data);
						
						shell.addTraverseListener(new TraverseListener() {
							public void keyTraversed(TraverseEvent e) {
								if (e.detail == SWT.TRAVERSE_ESCAPE) {
									shell.dispose();
									e.doit = false;
								}
							}
						});
						
						
						
						
						
						
						
						
						Point point = shell.computeSize(400, SWT.DEFAULT);
						shell.setSize(point);
						
					    Utils.centreWindow(shell);
					    shell.open();
					    
					}catch( Throwable e ){
						
						Debug.out( e );
						
						wait_sem.release();
					}
				}
			});
		
		wait_sem.reserve();
		
		return( result[0] );
	}
	
	public void
	destroy()
	{
		synchronized( this ){
			
			destroyed = true;
			
			if ( active_shells.size() > 0 ){
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								List<Shell>	copy;
							
								synchronized( TorPluginUISWT.this ){
								
									copy = new ArrayList<Shell>(active_shells);
								}
								
								for ( Shell shell: copy ){
									
									shell.dispose();
								}
							}
						});
			}
		}
	}
}
