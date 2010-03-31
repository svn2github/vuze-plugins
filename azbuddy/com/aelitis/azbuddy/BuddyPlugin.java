package com.aelitis.azbuddy;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azbuddy.buddy.*;
import com.aelitis.azbuddy.config.ConfigManager;
import com.aelitis.azbuddy.dht.DHTManager;
import com.aelitis.azbuddy.ui.UILogger;
import com.aelitis.azbuddy.ui.ViewControl;
import com.aelitis.azbuddy.utils.TaskScheduler;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;

public class BuddyPlugin implements UnloadablePlugin {

	private static BuddyPlugin singleton;

	private PluginInterface pI;

	private static final String VIEWID = "azbuddy.Plugin";
	private UISWTInstance swtInstance;
	private ViewControl view;

	private BuddyManager buddyMan;
	private DHTManager dhtMan;
	private ConfigManager configMan;
	private SESecurityManager secuMan;
	private SEPublicKey myIdent;

	public void initialize(PluginInterface pluginInterface)
	throws PluginException
	{
		if(singleton != null)
			throw new PluginException("Plugin already running");
		singleton = this;

		TaskScheduler.init();

		pI = pluginInterface;

		// Get notified when the UI is attached
		pluginInterface.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void UIAttached(UIInstance instance) {
						if (instance instanceof UISWTInstance) {
							UILogger.log("attaching to SWT");
							UISWTInstance swtInstance = ((UISWTInstance) instance);
							view = new ViewControl();
							if (view != null) {
								// Add it to the menu
								swtInstance.addView(UISWTInstance.VIEW_MAIN, VIEWID, view);
								// Open it immediately
								//swtInstance.openMainView(VIEWID, viewListener, null);
							}
						}
					}

					public void UIDetached(UIInstance instance) {
					}
				});

		//UI listener is waiting, creating featureitis
		
		//order is imporant
		secuMan = pI.getUtilities().getSecurityManager();
		configMan = new ConfigManager();
		dhtMan = new DHTManager(pI);
		buddyMan = new BuddyManager(); // requires dht, config & crypto

		pI.addListener(new PluginListener() {
			public void closedownComplete()	{ }
			public void closedownInitiated() { shutdown(); }
			public void initializationComplete()
			{
				new AEThread2("AsyncBuddyPluginInit",true) {
					public void run()
					{ // do async loading; order is important
						
						// TODO remove password hack
						AzureusCoreFactory.getSingleton().getCryptoManager().addPasswordHandler(
								new CryptoManagerPasswordHandler()
								{
									public int
									getHandlerType()
									{
										return( HANDLER_TYPE_USER );
									}
									
									public passwordDetails
						        	getPassword(
						        		int			handler_type,
						        		int			action_type,
						        		boolean		last_pw_incorrect,
						        		String		reason )
									{
										System.out.println( "CryptoPassword (" + reason + ")");
										
										return( 
											new passwordDetails()
											{
												public char[]
												getPassword()
												{
													return( "changeit".toCharArray());
												}
												
												public int 
												getPersistForSeconds() 
												{
													return( 0 );
												}
											});
									}
									
									public void 
									passwordOK(
										int 				handler_type,
										passwordDetails 	details) 
									{
									}
								});
						
						
						try
						{
							myIdent = secuMan.getPublicKey(SEPublicKey.KEY_TYPE_ECC_192, "Buddy Plugin Init");
						} catch (Exception e)
						{
							throw new RuntimeException("unable to retrieve public key, buddyPlugin initialisation failed",e);
						}
						dhtMan.start();
						buddyMan.start();
 					}
				}.start();
			}
		});

	}

	public void unload() throws PluginException
	{
		if(singleton == null) // unload already in progress?
			return;
		singleton = null;

		shutdown();

		TaskScheduler.destroy();
		dhtMan.stop();
		if (swtInstance != null)
			swtInstance.removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
	}

	/**
	 * exiting with a clean state
	 */
	private void shutdown()
	{
		buddyMan.stop();
		configMan.save();
	}

	public static BuddyPlugin getSingleton()
	{
		return singleton;
	}

	public BuddyManager getBuddyMan()
	{
		return buddyMan;
	}

	public static DHTManager getDHTMan()
	{
		return singleton.dhtMan;
	}

	public static PluginInterface getPI()
	{
		return singleton.pI;
	}

	public static ConfigManager getConfigMan()
	{
		return singleton.configMan;
	}
	
	public static SESecurityManager getSecurityManager()
	{
		return singleton.secuMan;
	}
	
	public static SEPublicKey getIdentity()
	{
		return singleton.myIdent;
	}
}
