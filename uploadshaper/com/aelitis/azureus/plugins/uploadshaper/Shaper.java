/*
 * Created on Sep 21, 2004
 * Created by Olivier Chalouhi
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
package com.aelitis.azureus.plugins.uploadshaper;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;


/**
 * 
 */
public class Shaper implements Plugin, Runnable{
  
  PluginInterface pluginInterface;

  PluginConfig config;
  
   public void initialize(PluginInterface pluginInterface)
       throws PluginException {
     this.pluginInterface = pluginInterface;
     config = pluginInterface.getPluginconfig();
     
     PluginConfigUIFactory factory = pluginInterface.getPluginConfigUIFactory();
     Parameter params[] = new Parameter[3];
     params[0] = factory.createBooleanParameter("enabled","plugin.uploadshaper.config.enable",false);
     params[1] = factory.createIntParameter("minUp","plugin.uploadshaper.config.minUp",50);
     params[2] = factory.createIntParameter("rate","plugin.uploadshaper.config.rate",5);
     pluginInterface.addConfigUIParameters(params,"plugin.uploadshaper.config.title"); 
     
     pluginInterface.getUtilities().createThread("Upload Speed Shaper", this);
   }
   
   public void run() {
    while(true) {
      boolean enabled = config.getPluginBooleanParameter("enabled",false);
      if(enabled) {
        int rate = config.getPluginIntParameter("rate",5);
        int min = config.getPluginIntParameter("minUp",50);
        
        long downSpeed = 0;
        Download[] downloads = pluginInterface.getDownloadManager().getDownloads();
        for(int i = 0 ; i < downloads.length ; i ++) {
          downSpeed += downloads[i].getStats().getDownloadAverage();
        }
        
        downSpeed /= 1024;
        
        int computed = (int) downSpeed * rate;
        if(computed < min) {
          computed = min;
        }
        config.setIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC,computed);
      }
      try {
        Thread.sleep(2000);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
   }
   
}
