package com.azureus.plugins.aznetmon.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.SWT;
import com.azureus.plugins.aznetmon.main.PluginComposite;

/**
 * Created on Apr 7, 2008
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class FaqUI extends PluginComposite {

	private Display rDisplay;
	private Composite pluginComposite;

	Browser browser;


	public FaqUI(Composite composite, int i) {
		super(composite, i);

		FillLayout l = new FillLayout( SWT.VERTICAL );
		this.setLayout(l);

		createUI(this);
	}

	public void delete() {
		
	}

	private void createUI(Composite parent){

		rDisplay = parent.getDisplay();

		pluginComposite = new Composite(parent, SWT.NONE );

		FillLayout layout = new FillLayout(SWT.VERTICAL);
		layout.marginHeight=5;
		layout.marginWidth=5;
		layout.spacing=1;
		pluginComposite.setLayout(layout);

		browser = new Browser(pluginComposite, SWT.NONE);
		//replace with Network Monitor FAQ.
		browser.setUrl("http://www.azureuswiki.com/index.php/Network_Monitor_FAQ");
		browser.setBounds(5,75,800,500);

	}

	// MacOS - Dalmazio Brisinda
	// pcap
	// jpcap
	// rrd4j

}
