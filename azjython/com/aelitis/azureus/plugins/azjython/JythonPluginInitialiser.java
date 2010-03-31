/**
 * File: JythonPluginInitialiser.java
 * Library: Jython Plugin for Azureus
 * Date: 29th November 2006
 *
 * Author: Allan Crooks
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package com.aelitis.azureus.plugins.azjython;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;

import org.gudy.azureus2.plugins.update.UpdateInstaller;
import org.gudy.azureus2.plugins.update.UpdateManager;

/**
 * @author Allan Crooks
 *
 */
public class JythonPluginInitialiser {
	
	private JythonPluginCore core;
	private static final String CLASS_NAME_TO_CHECK = "org.python.util.jython";
	
	// Versions we've tested against, so we don't warn the user about...
	private static final String[] TESTED_VERSIONS = new String[] {
		"2.1", "2.2.1",
	};

	public JythonPluginInitialiser(JythonPluginCore core) {
		this.core = core;
	}
	
	public boolean initialiseJythonPath(boolean inform_user) {
		
		// Step 1: Check for an explicit python.home setting.
		String python_home = System.getProperty("python.home");
		String err_key_suffix = null;
		if (python_home == null) {
			core.logger.log("python.home setting does not exist.");
		}
		else {
			
			core.logger.log("python.home setting does exist, verifying...");
			
			// Check the setting is valid.
			err_key_suffix = this.validateJythonPath(python_home);
			if (err_key_suffix == null) {
				
				core.logger.log("python.home setting was valid.");
				
				// python.home is valid, so we can stop here.
				return true;
				
			}
			
			core.logger.log("python.home setting wasn't valid - " + err_key_suffix);
			
			// Doesn't appear to be valid - we will always warn the user if
			// inform_user is true.
			if (inform_user) {
				this.core.warnUser("init.pythonhome.warn." + err_key_suffix, python_home);
			}
		}
		
		core.logger.log("Checking user.home setting...");
		
		// Step 2: Check the user's home directory.
		String user_home = System.getProperty("user.home");
		if (user_home != null) {
			core.logger.log("user.home does exist, testing for a .jython subdirectory.");
			File f_user_home = new File(user_home, ".jython");
			err_key_suffix = this.validateJythonPath(f_user_home.getPath());
			
			// We aren't necessarily expecting a user to have this set, so we don't
			// mind if it isn't there.
			if (err_key_suffix == null) {
				core.logger.log(".jython directory did exist.");
				return true;
			}
			else {
				core.logger.log("Couldn't find valid .jython directory.");
			}
		}
		else {
			core.logger.log("No user.home setting.");
		}
		
		// Step 3: Check the config directory.
		String config_dir = this.core.getJythonPath();
		if (config_dir == null || config_dir.trim().length() == 0) {
			core.logger.log("No configuration setting of Jython path.");
			
			// Not an error - we'll just inform the user about it.
			if (inform_user) {
				core.informUser("init.jythonpath.info.not_set");
			}
		}
		else {
			String err_key = this.validateJythonPath(config_dir);
			if (err_key == null) {
				core.logger.log("Valid Jython path stored in configuration setting, setting system property.");
				System.setProperty("python.home", config_dir);
				return true;
			}
			else {
				core.logger.log("Problem with Jython path in configuration setting - " + err_key);
				if (inform_user) {
					core.warnUser("init.jythonpath.warn." + err_key, config_dir);
				}
			}
		}

		// Failed.
		return false;
	}
	
	// Returns a message key indicating the reason why the path is not
	// valid (if it isn't valid, of course).
	private String validateJythonPath(String path) {
		File pathdir = new File(path);
		if (!pathdir.isDirectory()) {return "path_invalid";}
		File registry_file = new File(path, "registry");
		if (!registry_file.isFile()) {return "no_registry";}
		return null;
	}
	
	public boolean initialiseJython(boolean inform_user) {
		
		core.logger.log("Testing to see if Jython is available...");
		
		// Is Jython available?
		boolean available = true;
		try {Class.forName(CLASS_NAME_TO_CHECK);}
		catch (ClassNotFoundException cnfe) {available = false;}

		boolean welcome = core.isFirstRun();
		boolean last_init_ok = core.initSuccessful(available);
		
		// If it is...
		if (available) {
			
			// Figure out what the Jython version is.
			core.jython_version = this.getInstalledJythonVersion();
			core.logger.log("Jython is available, seeing if it is setup correctly...");
			
			// We only inform the user about the setup of Jython if the setting is
			// enabled.
			boolean warn_on_init_errors = core.plugin_interface.getPluginconfig().getPluginBooleanParameter("warn_on_jython_misconfiguration", true); 
			this.initialiseJythonPath(inform_user && warn_on_init_errors);
			
			if (inform_user) {
				if (welcome) {core.informUser("init.welcome.info.working");}
				else if (!last_init_ok) {core.informUser("init.start.info.working");}
				else {/* Do nothing. */}
			}
		}
		else {
			
			// Let's check to see if Jython is available by the class loader.
			boolean pc_available = true;
			try {Class.forName(CLASS_NAME_TO_CHECK, false, core.plugin_interface.getPluginClassLoader());}
			catch (ClassNotFoundException cnfe) {pc_available = false;}
			
			// If it is available in the plugin class loader, this doesn't suggest a problem with Jython - it's
			// a problem with this plugin! Usually, this happens when the plugin is being loaded as core code,
			// rather than as a plugin. This usually only happens to someone who's developing this plugin.
			if (pc_available) {
				core.logger.log("Jython is not available via plugin class-loader - this code is not being run as plugin!");
				
				if (inform_user) {
					this.core.warnUser("init.plugin_only.warning");
				}
				return false;
			}
			 
			// We've failed to load Jython. Report to user.
			if (inform_user) {
				
				// First time user - be gentle with them. :)
				if (welcome) {
					this.core.informUser("init.welcome.info.not_working"); // xxx: test
				}
				
				// Worked previously, warn them.
				else if (last_init_ok) {
					this.core.warnUser("init.start.warn.failed"); //xxx: test
				}
				
			}
		}
		
		return available;
	}
	
	public boolean installJython(boolean inform_user) {
		core.logger.log("Attempting to install Jython.");
		String jython_path = core.getJythonPath();
		if (jython_path.length() == 0) {
			core.logger.log("No Jython path setup.");
			if (inform_user) {core.showError("install.error.no_path");}
			return false;
		}
		
		File f_jython_path = new File(jython_path);
		if (!f_jython_path.isDirectory()) {
			core.logger.log("Config path is not a directory - " + f_jython_path.getPath());
			if (inform_user) {core.showError("install.error.invalid_dir", f_jython_path.getPath());}
			return false;
		}
			
		File jar_path = new File(f_jython_path, "jython.jar");
		if (!jar_path.isFile()) {
			core.logger.log("No jython.jar exists here: " + jar_path.getPath());
			if (inform_user) {core.showError("install.error.no_jython_jar", jar_path.getPath());}
			return false;
		}
			
		String version_number = determineJythonVersion(jar_path);
		String target_file_name = "jython" + (version_number == null ? "" : ("_" + version_number) + ".jar");
		File target_jar = new File(core.plugin_interface.getPluginDirectoryName(), target_file_name);
			
		if (target_jar.exists()) {
			core.logger.log("Target JAR file already exists - " + target_jar.getPath());
			if (inform_user) {core.showError("install.error.target_jar_exists", target_jar.getPath());}
			return false;
		}
		
		// Attempt to register an installation of the JAR file.
		UpdateManager um = core.plugin_interface.getUpdateManager();		
		try {
			UpdateInstaller ui = um.createInstaller();
			ui.addResource("azjython.core.jar.tmp", new FileInputStream(jar_path), true);
			ui.addMoveAction("azjython.core.jar.tmp", target_jar.getPath());
		}
		catch (Exception e) {
			core.logger.log("Error trying to register JAR file installation from " + jar_path.getPath(), e);
			if (inform_user) {core.showError("install.error.problem_registering_install", e);}
			return false;
		}
		
		// Only if install is going to succeed do we warn the user if we've got a version that
		// may be incompatible.
		if (inform_user) {
			if (version_number != null) {
				if (!java.util.Arrays.asList(TESTED_VERSIONS).contains(version_number)) {
					// Only warn them if we haven't warned this user before...
					String warn_param_name = "jython_not_tested_warning_" + version_number;
					if (!core.getFlag(warn_param_name, false)) {
						core.warnUser("install.warn.untested_jython_version", version_number);
						core.setFlag(warn_param_name, true);
					}
				}
			}
		}
		
		// Installation appears to have succeeded! Let's tell the user.
		if (inform_user) {
			if (version_number == null) {
				core.informUser("install.info.done_no_version");
			}
			else {
				core.informUser("install.info.done_with_version", version_number);
			}
		}
		
		successfully_installed = true;
		return true;

	}
	
	static boolean successfully_installed = false;
	
	public String determineJythonVersion(File jar_file_location) {
		core.logger.log("Attempt to determine Jython version in: " + jar_file_location.getPath());
		String result = null;
		try {
			URLClassLoader cl = URLClassLoader.newInstance(new URL[] {jar_file_location.toURL()}, ClassLoader.getSystemClassLoader());
			Class j_class = cl.loadClass("org.python.core.PySystemState"); 
			Field j_field = j_class.getField("version");
			result = (String)j_field.get(null); 
		}
		catch (Exception e) {
			core.logger.log("Error determining Jython version", e);
			return null;
		}
		core.logger.log("Determined Jython version: \"" + result + "\".");
		return result;
	}
	
	public String getInstalledJythonVersion() {
		try {
			Class j_class = Class.forName("org.python.core.PySystemState"); 
			Field j_field = j_class.getField("version");
			return (String)j_field.get(null);
		}
		catch (Exception e) {
			return null;
		}
	}
	
}
