/*
 * Created on Sep 20, 2004
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
package com.aelitis.azureus.plugins.minibrowser;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.SWT.SWTManager;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;

/**
 * 
 */
public class MiniBrowser implements Plugin {
  
 PluginInterface pluginInterface;
 BrowserConfig browserConfig;
 BrowserView browserView;
 
 
  public void initialize(PluginInterface pluginInterface)
      throws PluginException {
    this.pluginInterface = pluginInterface;
    this.browserConfig = new BrowserConfig(pluginInterface.getPluginconfig());    
        
    SWTManager swtManager = pluginInterface.getUIManager().getSWTManager();
    this.browserView = new BrowserView(this.browserConfig,swtManager);
    
    swtManager.addView(browserView);
    
    PluginConfigUIFactory factory = pluginInterface.getPluginConfigUIFactory();
    Parameter params[] = new Parameter[1];
    params[0] = factory.createStringParameter("homepage","plugin.minibrowser.config.homepage",BrowserConfig.DEFAULT_HOME_PAGE);
    pluginInterface.addConfigUIParameters(params,"plugin.minibrowser.config.title");    
  }
  
  
  
  
  

}
