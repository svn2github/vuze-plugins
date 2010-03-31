package com.aelitis.azbuddy.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.ui.UILogger;

public class ConfigManager {
	
	final static InheritableConfigTree defaultSettings = InheritableConfigTree.getInstance(null, "defaults");
	
	static {
		// example
		new InheritableBoolean(defaultSettings,"PublishPrivateTorrents",false,false);
		new InheritableString(defaultSettings,"Buddy.permaNick",false,"no nickname set");
	}
	
	
	
	public final static String configFile = "buddy.config";
	final File configPath;
	
	final InheritableConfigTree root = InheritableConfigTree.getInstance(defaultSettings, "ConfigRoot");
	
	public ConfigManager()
	{
		// TODO replace core class that's used to fetch the config dir
		//configPath = new File(BuddyPlugin.getSingleton().getPI().getPl);
		configPath = new File(SystemProperties.getUserPath());
		final Map serializedConfig = BuddyPlugin.getPI().getUtilities().readResilientBEncodedFile(configPath, configFile, true);
		
		ConfigUtils.deserializeContainer(serializedConfig, root);

		
		try
		{
			UILogger.log(new String(BEncoder.encode(serializedConfig)));
		} catch (IOException e)
		{
			e.printStackTrace();
		} 
	}
	
	public InheritableConfigTree getRoot()
	{
		return root;
	}
	
	public void save()
	{
		BuddyPlugin.getPI().getUtilities().writeResilientBEncodedFile(configPath, configFile, root.getContentForSerialization(), true);
	}

}
