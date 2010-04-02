/*
 * Created on 16-Jun-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.ext.ietoolbar;

import java.io.File;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

public class 
IEToolBarPlugin
	implements Plugin
{
	public void 
	initialize(
		PluginInterface plugin_interface )
	 
		throws PluginException
	{
		LocaleUtilities	loc_utils = plugin_interface.getUtilities().getLocaleUtilities();
		
		String	name = loc_utils.getLocalisedMessageText( "ietorrentbar.name" );
		
		String	info = 
			loc_utils.getLocalisedMessageText( 
				"ietorrentbar.info",
				new String[]{ 
						// plugin_interface.getPluginconfig().getPluginUserFile( "Torrent" + File.separator + "TorrentBar" ).toString()});
						new File( plugin_interface.getPluginDirectoryName(), "readme.txt" ).toString()});
		
		final BasicPluginViewModel	view_model = plugin_interface.getUIManager().createBasicPluginViewModel(name); 
				
		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );

		view_model.getLogArea().setText( info );
		
	}
}
