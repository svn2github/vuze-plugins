package com.azureus.plugins.aznetmon.ui;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import com.azureus.plugins.aznetmon.main.PluginComposite;
import com.azureus.plugins.aznetmon.main.DataListener;
import com.azureus.plugins.aznetmon.util.CdnMonitorTimeUtils;
import com.azureus.plugins.aznetmon.pcap.AzureusPeerStats;
import com.azureus.plugins.aznetmon.pcap.util.NetMon16Util;
import com.azureus.plugins.aznetmon.util.AzNetMonLogger;
import com.azureus.plugins.aznetmon.util.AzNetMonUtil;

import java.text.DecimalFormat;

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

public class AdvancedUI extends PluginComposite
{

	private Display rDisplay;
	private Composite pluginComposite;

	boolean isJava16 = false;
	boolean hasPcap = false;
	boolean hasJpcap = false;

	DataListener dataListener;

	Label statusString;
	Label statusLabel;

	Table table;

	Browser browser;

	private final LoggerChannel logger = AzNetMonLogger.getLogger();

	public AdvancedUI(Composite composite, int i) {
		super(composite, i);

		//verify pre-conditions for Advanced Checking.
		// Java1.6
		// pcap
		// jpcap

		rDisplay = composite.getDisplay();

		checkForOptionalComponents();

		if( hasOptionalComponents() ){

			GridData d = new GridData(GridData.FILL_BOTH);
			this.setLayoutData( d );
			GridLayout l = new GridLayout(  );
			this.setLayout(l);

			createUI(this);
		}else{

			FillLayout l = new FillLayout( SWT.VERTICAL );
			this.setLayout(l);

			//This needs to be a web-browser pointing to a wiki-page.
			createInstallUI(this);
		}

	}

	/**
	 * Check for Java 1.6, pcap, jpcap
	 *
	 *
	 */
	private void checkForOptionalComponents()
	{
		//Java 1.6.
		isJava16 = AzNetMonUtil.canRunJava16();

		if(!isJava16){
			return;
		}

		//from beyond this point was are Java 1.6

		hasJpcap = NetMon16Util.hasJPcap();

		//check for jpcap.
		try{
			NetMon16Util.hasJPcap();
			hasJpcap=true;
		}catch(NoClassDefFoundError e){
			hasJpcap=false;
			return;
		}

		//check for pcap - (Might need to seperate MAC from PC).

	}

	private boolean hasOptionalComponents()
	{

		if(!isJava16) {
			return false;
//		}else if( !hasPcap ){  //ToDo: implement pcap check.
//			return false;
		}else if( !hasJpcap ){
			return false;
		}
		return true;

	}//hasOptionalComponents


	/**
	 * Create the UI if all the componenets are installed.
	 * @param parent
	 */
	private void createUI(Composite parent)
	{

		pluginComposite = new Composite(parent, SWT.NONE );
		GridData gd = new GridData(GridData.FILL_BOTH);
		pluginComposite.setLayoutData(gd);

		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		pluginComposite.setLayout(layout);

		createTable();

		AzureusPeerStats stats = AzureusPeerStats.getInstance();
		dataListener = new DataListener(){
			public void update()
			{
				rDisplay.asyncExec( new Runnable(){
					public void run(){
						refreshAdvancedView();
					}
				});
			}
		};

		stats.addListener( dataListener );
		refreshAdvancedView();
	}//createUI

	private void refreshAdvancedView() {

		Thread t = new Thread() {
			public void run(){

				try{
					//Refresh the table here. Also have some diagrams in the lower left.
					AzureusPeerStats stats = AzureusPeerStats.getInstance();
					final String status = stats.getStatus();
					if(statusString!=null){

					}//if
					//Refresh that table.
					AzureusPeerStats.RstData[] rData = stats.getMostRecent(100);

					int len = rData.length;
					for(int i=0;i<len;i++){
						addTableElement( rData[i] );
					}//for


				}catch(Throwable thr){
					AzureusPeerStats stats = AzureusPeerStats.getInstance();
					stats.setStatus( thr.toString() );
				}finally{

				}
			}//run
		};

		table.removeAll();
		t.start();
	}


	private void addTableElement( final AzureusPeerStats.RstData rData ){

		//make sure display isn't null or disposed.
		if( rDisplay==null || rDisplay.isDisposed() )
		return;

		//send it to table.
		rDisplay.asyncExec( new Runnable() {
			public void run() {
				//check again that table hasn't disposed.
				if(table==null | table.isDisposed()){
					return;
				}

				TableItem item = new TableItem(table,SWT.NULL);

				//We use a Calendar to format the Date
				String dataTime = CdnMonitorTimeUtils.getReadableTime(rData.timestamp);
				item.setText(0, dataTime);

				DecimalFormat formatter = new DecimalFormat("#0.0 %");

				item.setText(1,formatter.format( rData.percentRSTConn ) );
				item.setText(2,rData.numPeerRst+"");
				item.setText(3,rData.numPeers+"");
				item.setText(4,rData.numFailedConn+"");
				item.setText(5,rData.ips.toString());

				logger.log(dataTime+" "+rData.percentRSTConn);

			}
		});

	}//addTableElement

	/**
	 * Point the plug-in at the 
	 */
	private void createInstallUI(Composite parent)
	{

		pluginComposite = new Composite(parent, SWT.NONE );

		FillLayout layout = new FillLayout(SWT.VERTICAL);
		layout.marginHeight=5;
		layout.marginWidth=5;
		layout.spacing=1;
		pluginComposite.setLayout(layout);


		browser = new Browser(pluginComposite, SWT.NONE);
		//replace with Network Monitor Advanced Install.
		browser.setUrl("http://www.azureuswiki.com/index.php/Network_Monitor_Advanced_Install");
		browser.setBounds(5,75,800,500);
		
	}

	private void createTable(){

		GridData gridData;
        table = new Table(pluginComposite, SWT.SINGLE | SWT.BORDER);
        //Headers visible
        table.setHeaderVisible(true);

        //This table columns :
        String[] columnNames = {"Time", "% RST Peers" ,"# RST Peers","# Closed Peers", "# failed connects", " IP - RST Peers"};
        //and their width
        int[] columnWidths = {125,75,75,75,100,100};

        //Initialise this table
        for(int i = 0 ; i < columnNames.length ; i++) {
            TableColumn column = new TableColumn(table,SWT.NULL);
            column.setText(columnNames[i]);
            column.setWidth(columnWidths[i]);
        }

        //Add a GridData in order to make it grab all the view space
        //And use the 3 columns
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        table.setLayoutData(gridData);
		
	}//createTable

	public void delete(){
		
	}
	
}
