/**
 * 
 */
package com.aelitis.azureus.plugins.azjython;

import java.io.*;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.*;
import java.util.jar.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.config.*;
//import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * @author allan
 *
 */
public class JythonInstaller extends ResourceDownloaderAdapter {

	// A list of SourceForge mirrors.
	private static String[] mirrors = new String[] {
		"jaist", "nchc", "keihanna", "optusnet", "peterhost", "ovh", "puzzle",
		"switch", "mesh", "kent", "surfnet", "heanet", "citkit", "internap",
		"cogent", "umn", "easynews", "ufpr"
	};
	
	// Download link template.
	private static String SF_LINK = "http://%s.dl.sourceforge.net/sourceforge/jython/jython_installer-2.2.1.jar";
	
	private static ResourceDownloader createDownloader(PluginInterface pi) {
		ResourceDownloaderFactory fact = pi.getUtilities().getResourceDownloaderFactory();
		ResourceDownloader[] downloaders = new ResourceDownloader[mirrors.length];
		for (int i=0; i<mirrors.length; i++) {
			downloaders[i] = fact.create(makeMirrorLink(mirrors[i]));
		}
		return fact.getRandomDownloader(downloaders);
	}
	
	private static URL makeMirrorLink(String mirror_name) {
		try {return new URL(SF_LINK.replace("%s", mirror_name));}
		catch (MalformedURLException e) {throw new RuntimeException(e);}
	}
	
	private static void installFromJythonInstallerPackage(File installer, File install_path) throws Exception {
		JarFile jar_file = new JarFile(installer);
		Enumeration e = jar_file.entries();

		byte[] buf = new byte[65536];
		
		while (e.hasMoreElements()) {
			JarEntry je = (JarEntry)e.nextElement();
			File dest = new File(install_path, je.getName());
			if (je.isDirectory()) {
				dest.mkdir();
				continue;
			}
			
			InputStream is = jar_file.getInputStream(je);
			FileOutputStream fos = new FileOutputStream(dest);
			int read = 0;
			while (true) {
				read = is.read(buf);
				if (read == -1) {break;}
				fos.write(buf, 0, read);
			}
			fos.close();
		}
	}
	
	/**
	 * 
	 * Creation type methods.
	 * 
	 */
	
	private static JythonInstaller instance;
	public static JythonInstaller getSingleton() {
		if (instance == null) {
			throw new RuntimeException("create singleton has to be called first!");
		}
		return instance;
	}
	
	public static void createSingleton(JythonPluginCore core, BasicPluginViewModel log_model) {
		if (instance != null) {throw new RuntimeException("singleton already created!");}
		instance = new JythonInstaller(core, log_model);
	}

	private JythonPluginCore core;
	private BasicPluginViewModel log_model;
	private JythonInstaller(JythonPluginCore core, BasicPluginViewModel log_model) {
		this.core = core;
		this.log_model = log_model;
	}
	
	/**
	 * Public "do something" methods.
	 */
	
	/**
	 * Returns the location of the Jython directory so that installation can work.
	 * Will return null if it's been stopped.
	 */
	private Parameter start_param;
	private Parameter stop_param;
	private ParameterListener installer;
	private ResourceDownloader dl;
	
	public void startInstall(Parameter start, Parameter stop, ParameterListener installer) {
		
		if (dl != null) {throw new RuntimeException("already started!");}
		
		// We will create a resource downloader, which will call us. We will use
		// the callback methods to complete the installation process.
		this.dl = createDownloader(core.plugin_interface);
		this.start_param = start;
		this.stop_param = stop;
		this.installer = installer;
		 
		this.dl.addListener(this);
		this.dl.asyncDownload();

		this.start_param.setEnabled(false);
		this.stop_param.setEnabled(true);
		
		// This is taken from UISWTInstanceImpl - there should be a better
		// way to do this!
		String model_key_name = this.core.locale_utils.getLocalisedMessageText("ConfigView.section.azjython").replaceAll(" ", ".");
		this.core.plugin.ui_manager.ui_instance.openView(UISWTInstance.VIEW_MAIN, model_key_name, null);
		
		this.log_model.getStatus().setText("Downloading...");
	}
	
	public void stopInstall() {
		if (dl == null) {throw new RuntimeException("already stopped!");}
		this.dl.cancel();
	}
	
	public boolean completed(ResourceDownloader downloader, InputStream data) {
		this.log_model.getStatus().setText("Download successful, installing...");
		
		try {
			byte[] buf = new byte[65536];
			int count = 0;
			
			File temp_file = new File(core.plugin_interface.getPluginDirectoryName(), "installer.tmp");
			FileOutputStream fos = new FileOutputStream(temp_file);
			do {
				count = data.read(buf);
				if (count == -1) {fos.close(); break;}
				fos.write(buf, 0, count);
			}
			while (true);
			
			this.log_model.getActivity().setText("Unpacking files from installer...");

			File jython_dest = new File(core.plugin_interface.getPluginDirectoryName(), "jython_dir");
			jython_dest.mkdirs();
			installFromJythonInstallerPackage(temp_file, jython_dest);
			temp_file.delete();
			
			this.log_model.getActivity().setText("Attempting to install...");
			
			core.plugin_interface.getPluginconfig().setPluginParameter("jython.path", jython_dest.getPath());
			installer.parameterChanged(null); // Attempt the install.
			if (JythonPluginInitialiser.successfully_installed) {
				this.log_model.getStatus().setText("Installed successfully.");
				this.log_model.getActivity().setText("");
			}
			else {
				this.log_model.getStatus().setText("Install failed.");
				this.log_model.getActivity().setText("");
			}
		}
		catch (Exception e) {
			core.logger.log(e);
		}
		
		// We must tell the caller that we have finished (even if install has
		// failed), we don't want to try another resource to download from.
		return true;
	}
	
	public void failed(ResourceDownloader downloader, ResourceDownloaderException e) {
		this.log_model.getStatus().setText("Download failed.");
		this.core.logger.log("Download failed.", e);

		this.start_param.setEnabled(true);
		this.stop_param.setEnabled(false);
		
		this.log_model.getProgress().setPercentageComplete(0);
		
		this.start_param = null;
		this.stop_param = null;
		this.installer = null;

	}
	
	public void reportPercentComplete(ResourceDownloader downloader, int percentage) {
		this.log_model.getProgress().setPercentageComplete(percentage);
	}
	
	public void reportActivity(ResourceDownloader downloader, String activity) { 
		this.core.logger.log(activity);
		this.log_model.getActivity().setText(activity);
	}
}
