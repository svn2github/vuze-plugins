/*
 * AMP - Azureus Monitoring Plugin
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

package org.pmm.monitoringplugin;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;

/**
 * @author kustos
 *
 * Manages the configuration of the AMP plugin. 
 */
public class AMPConfig {
    private static final String PREFIX = "MonitoringPlugin";
    
    public static final String MEMORY_SECTION = PREFIX +".ExpandMemory";
    
    public static final String MEMORY_POOLS_SECTION = PREFIX +".ExpandMemoryPools";
    
    public static final String GARBAGE_COLLECTION_SECTION = PREFIX +".ExpandGarbageCollection";
    
    public static final String THREADS_SECTION = PREFIX +".ExpandThreads";
    
    public static final String CLASSES_SECTION = PREFIX +".ExpandClasses";
    
    public static final String COMPILER_SECTION = PREFIX +".ExpandCompiler";
    
    public static final String OS_SECTION = PREFIX +".ExpandOS";
    
    public static final String RUNTIME_SECTION = PREFIX +".ExpandRuntime";
    
    public static final String MEMORY_MANAGERS_SECTION = PREFIX +".ExpandMemoryManagers";
    
    private PluginConfig config;
    
    
    
    public AMPConfig(PluginConfig config) {
        this.config = config;
    }
    
    public void setExpand(String key, boolean expand) {
        this.config.setPluginParameter(key, expand);
        this.save();
    }
    
    public boolean getExpand(String  key, boolean defaultValue) {
//        System.out.println("expand old "+key+": "+this.config.getBooleanParameter(key, true));
//        System.out.println("expand new"+key+": "+this.config.getBooleanParameter("Plugin.monitoringplugin."+key, true));
        //return this.config.getBooleanParameter(key, defaultValue);
        return this.config.getPluginBooleanParameter(key, defaultValue);
    }
    
    public void save() {
        try {            
            this.config.save();
        } catch (PluginException e) {            
            // oops couldn't save
        }
    }
}
