package com.azureus.plugins.aznetmon.ui;

import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import com.azureus.plugins.aznetmon.main.PluginComposite;
import com.azureus.plugins.aznetmon.util.AzNetMonLogger;

/**
 * Created on Jan 17, 2008
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

public class RSTUnloadableView implements UISWTViewEventListener
{
    boolean isCreated = false;

	PluginComposite basicUI=null;
	PluginComposite advancedUI=null;
	PluginComposite resultsUI=null;
	PluginComposite faqUI =null;

	PluginInterface pluginInterface;

	private final LoggerChannel logger = AzNetMonLogger.getLogger();


	public RSTUnloadableView(PluginInterface _pi){
        pluginInterface=_pi;

    }



    public boolean eventOccurred(UISWTViewEvent event) {

        switch( event.getType() ){

            case UISWTViewEvent.TYPE_CREATE:
                if(isCreated){
                    return false;
                }
                isCreated = true;
                break;

            case UISWTViewEvent.TYPE_INITIALIZE:
                initialize( (Composite) event.getData() );
                break;

            case UISWTViewEvent.TYPE_CLOSE:
            case UISWTViewEvent.TYPE_DESTROY:
                delete();
                break;
        }
        return true;
    }

    private void delete(){

		disposePluginComposite(basicUI);
		disposePluginComposite(advancedUI);
		disposePluginComposite(resultsUI);
		disposePluginComposite(faqUI);
		
		isCreated = false;
    }

	private void disposePluginComposite(PluginComposite pluginComposite){

		try{

			if(pluginComposite!=null){
				pluginComposite.delete();
				pluginComposite=null;
			}

		}catch(Exception e){
			logger.log("PluginComposite had: "+ e );
		}

	}

	/**
	 * Add a tab. "original", "advanced", "results", "acknowledgements"
	 * @param parent -
	 */
	private void initialize(Composite parent){


		TabFolder tf = new TabFolder(parent, SWT.NONE);

		TabItem ti1 = new TabItem(tf, SWT.BORDER);
		ti1.setText("Original");
		basicUI = new BasicUI(tf, SWT.NONE);
		ti1.setControl( basicUI );

		TabItem ti2 = new TabItem(tf, SWT.BORDER);
		ti2.setText("Advanced");
		advancedUI = new AdvancedUI(tf, SWT.BORDER );
		ti2.setControl( advancedUI );


		TabItem ti3 = new TabItem(tf, SWT.BORDER);
		ti3.setText("Results");
		resultsUI = new ResultsUI(tf, SWT.NONE);
		ti3.setControl( resultsUI );

		
		TabItem ti4 = new TabItem(tf, SWT.BORDER);
		ti4.setText("FAQ");
		faqUI = new FaqUI(tf, SWT.BORDER);
		ti4.setControl( faqUI );
		
	}

}
