/*
 * File    : VWStatusArea.java
 * Created : 14-Mar-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet.view;

/**
 * @author parg
 *
 */

import java.awt.*;

import javax.swing.*;

import org.gudy.azureus2.ui.webplugin.remoteui.applet.model.*;
import org.gudy.azureus2.core3.util.*;

public class 
VWStatusAreaView 
{
	protected JComponent			component;
	protected MDStatusAreaModel		model;
	
	protected VWLabel				upload_label;
	protected VWLabel				download_label;
	protected VWLabel				ipfilter_label;
	
	public
	VWStatusAreaView(
		MDStatusAreaModel	_model )
	{
		model		= _model;
		
		component = new JPanel( new GridBagLayout());
	
		int	column = 0;
		
		VWLabel version  = new VWLabel( " " + model.getAzureusVersion() + " ");
					
		version.setBorder( new VWStatusEntryBorder());
			
		component.add( 
				version,
					new VWGridBagConstraints(
							column++, 0, 1, 1, 0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.NONE, 
							new Insets(0, 0, 0, 0), 0, 0 ));
		
		JPanel	pad = new JPanel();
		
		component.add( 
				pad,
				new VWGridBagConstraints(
						column++, 0, 1, 1, 1.0, 1.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, 
						new Insets(0, 0, 0, 0), 0, 0 ));

		pad.setBorder( new VWStatusEntryBorder());
	
		ipfilter_label  = new VWLabel( "" );
		
		ipfilter_label.setMinimumWidth(175);
			
		ipfilter_label.setBorder( new VWStatusEntryBorder());
			 
		component.add( 
				ipfilter_label,
				new VWGridBagConstraints(
						column++, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE, 
						new Insets(0, 0, 0, 0), 0, 0 ));
			
		download_label  = new VWLabel( "" );
	
		download_label.setMinimumWidth(100);
		
		download_label.setBorder( new VWStatusEntryBorder());
		 
		component.add( 
				download_label,
				new VWGridBagConstraints(
						column++, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE, 
						new Insets(0, 0, 0, 0), 0, 0 ));
		
		upload_label  = new VWLabel( "" );
		
		upload_label.setMinimumWidth(100);
		
		upload_label.setBorder( new VWStatusEntryBorder());
		
		component.add( 
				upload_label,
				new VWGridBagConstraints(
						column++, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE, 
						new Insets(0, 0, 0, 0), 0, 0 ));
		

		refresh();
	}
	
	public void
	refresh()
	{
		download_label.setText( "D: " + DisplayFormatters.formatByteCountToKiBEtcPerSec(model.getDownloadSpeed()));
		
		upload_label.setText( "U: " + DisplayFormatters.formatByteCountToKiBEtcPerSec(model.getUploadSpeed()));
		
		String	ip_text;
		
		ip_text = "{"+DisplayFormatters.formatDateShort(model.getIPFilterUpdateTime()) + "} IPs: " + 
					model.getIPFilterNumberOfRanges() + " - " + 
					model.getIPFilterNumberOfBlockedIPs();
	
		ipfilter_label.setText( ip_text );
	}
	
	public JComponent
	getComponent()
	{
		return( component );
	}

}
