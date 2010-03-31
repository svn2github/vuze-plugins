/*
 * Created on 20-Nov-2004
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

package com.aelitis.azureus.plugins.tracker.simpleauth;

import java.io.IOException;
import java.net.URL;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.web.TrackerAuthenticationListener;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

/**
 * @author parg
 *
 */

public class 
TrackerSimpleAuthPlugin
	implements Plugin
{
	protected PluginInterface	plugin_interface;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		Tracker	tracker = plugin_interface.getTracker();
		
		tracker.addAuthenticationListener(
			new TrackerAuthenticationListener()
			{
				public boolean
				authenticate(
					URL			resource,
					String		user,
					String		password )
				{
					System.out.println( "auth:" + resource + ":" + user + "/" + password );
					
					if ( password.equals( "password" )){
						
						return( true );
					}
					
					return( false );
				}
				
				public byte[]
				authenticate(
					URL			resource,
					String		user )
				{
					System.out.println( "auth:" + resource + ":" + user );
					
					return( null );
				}
			});
		
		tracker.addPageGenerator(
			new TrackerWebPageGenerator()
			{
				public boolean
				generate(
					TrackerWebPageRequest		request,
					TrackerWebPageResponse		response )
				
					throws IOException
				{
					System.out.println( "user:" + request.getUser());
					
					return( false );
				}
			});
	}
}
