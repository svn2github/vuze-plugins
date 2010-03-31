package com.azureus.plugins.aznetmon.ui;

import com.azureus.plugins.aznetmon.main.PluginComposite;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminASN;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.Utils;

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

public class ResultsUI extends PluginComposite
{

	private Display rDisplay;
	private Composite pluginComposite;

	Group resultWikiGroup;

	Browser browser;

	public ResultsUI(Composite composite, int i) {
		super(composite, i);

		GridData d = new GridData( GridData.FILL_BOTH );
		this.setLayoutData( d );
		GridLayout l = new GridLayout();
		this.setLayout(l);

		createUI(this);

	}


	private void createUI(Composite parent){


		createResultsGroup(parent);

		createBrowser(parent);

	}


	private void createBrowser(Composite parent){
		

	}

	private void createResultsGroup(Composite parent){

		resultWikiGroup = new Group(parent, SWT.WRAP);
		GridData wikiGridData;
        wikiGridData = new GridData();
        wikiGridData.widthHint = 350;
        resultWikiGroup.setLayoutData(wikiGridData);
        GridLayout wikiLayout = new GridLayout();
        wikiLayout.numColumns = 1;
        wikiLayout.marginHeight = 1;
        resultWikiGroup.setLayout(wikiLayout);

        resultWikiGroup.setText("Network Monitor Results");

        final Label linkLabel = new Label(resultWikiGroup, SWT.NULL);
        linkLabel.setText( "Network Status Monitor Results" );
        linkLabel.setData("http://wiki.vuze.com/w/ISP_Network_Monitor");
        linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        linkLabel.setForeground(Colors.blue);
        wikiGridData = new GridData();
        wikiGridData.horizontalIndent = 10;
        linkLabel.setLayoutData( wikiGridData );
        linkLabel.addMouseListener(new MouseAdapter() {
          public void mouseDoubleClick(MouseEvent arg0) {
              Utils.launch((String) (arg0.widget).getData());
          }
          public void mouseUp(MouseEvent arg0) {
              Utils.launch((String) (arg0.widget).getData());
          }
        });


		//get the ASN.
		final Label asnLabel = new Label(parent, SWT.NULL);
		String asn = getAsn();
		asnLabel.setText("ASN: "+asn);

	}//createResultsGroup

	private String getAsn(){

		NetworkAdminASN naa = NetworkAdmin.getSingleton().getCurrentASN();

		String as = naa.getAS();
		String asName = naa.getASName();

		return as+" : "+asName;
	}

	public void delete() {

	}
}
