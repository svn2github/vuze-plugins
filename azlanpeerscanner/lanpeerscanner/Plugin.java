package lanpeerscanner;


import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


import lanpeerscanner.scan.AddressesQueue;
import lanpeerscanner.scan.AddressesQueueStringTokenizer;
import lanpeerscanner.scan.MultiThreadedScanner;
import lanpeerscanner.scan.Peer;
import lanpeerscanner.scan.PeerHandler;
import lanpeerscanner.scan.RequestListener;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

/**
 * @author Alexandre Larribeau
 *
 */
public class Plugin implements org.gudy.azureus2.plugins.Plugin, PeerHandler, DownloadListener, UTTimerEventPerformer {

	public static final String URL = "http://www.azureuswiki.com/index.php/Lan_Peer_Scanner";
	
	
	public static final String PARAM_CONFIG_TABNAME = "tab_name";
	public static final String PARAM_INTRO = "intro";
	public static final String PARAM_PLUGIN_ENABLED = "plugin_enabled";
	public static final String PARAM_PEER_INJECTION_ENABLED = "peer_injection_enabled";
	public static final String PARAM_DHT_USE_ENABLED = "ddb_use_enabled";
	public static final String PARAM_ALERTS_ENABLED = "alerts_enabled";
	public static final String PARAM_LISTENING_PORT = "listening_port";
	public static final String PARAM_NB_THREADS = "nb_threads";
	public static final String PARAM_IP_RANGES = "ip_ranges";
	public static final String PARAM_LAUNCH_SCAN_BUTTON_NAME = "launch_scan_button_name";
	public static final String PARAM_LAUNCH_SCAN_BUTTON_LABEL = "launch_scan_button_label";
	public static final String PARAM_UDP_REQ_TIMEOUT = "udp_req_timeout";
	public static final String PARAM_SCAN_TIMEOUT = "full_scan_timeout";
	public static final String PARAM_FIRST_SCAN_DELAY = "first_scan_delay";
	public static final String PARAM_SCAN_PERIODICITY = "scan_periodicity";
	public static final String PARAM_LINK = "link";
	
	public static final Boolean DEF_PLUGIN_ENABLED = true;
	public static final Boolean DEF_PEER_INJECTION_ENABLED = true;
	public static final Boolean DEF_DHT_USE_ENABLED = true;
	public static final int		DEF_LISTENING_PORT = 1234;
	public static final int 	DEF_NB_THREADS = 50;
	public static final String 	DEF_IP_RANGES = "192.168.0.0-255";
	public static final int 	DEF_UDP_REQ_TIMEOUT = 200;//ms
	public static final int 	DEF_SCAN_TIMEOUT = 10;//min
	public static final int 	DEF_FIRST_SCAN_DELAY = 5;//s
	public static final int 	DEF_SCAN_PERIODICITY = 1;//h
	public static final Boolean DEF_ALERTS_ENABLED = false;
	
	
	/**
	 * Plugin interface which gives access to the Azureus plugin API
	 */
	private PluginInterface pluginInterface;
	
	static public LoggerChannel logger;
	
	UTTimer scanNetworkPeriodicalTimer;
	UTTimer scanNetworkTimerFirstTime;
	
	//<IP, Peer>
	private Hashtable<String,Peer> knownPeers;
	private MultiThreadedScanner multiThreadedScanner;
	private RequestListener requestListener;
	
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
	 */
	public void unload() throws PluginException {
		
		//removing config sections
		for (ConfigSection configSection : this.pluginInterface.getConfigSections()) {
			this.pluginInterface.removeConfigSection(configSection);
		}
		
		//closing socket
		this.requestListener.closeSocket();
		
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize(PluginInterface pluginIf) throws PluginException {
		
		this.pluginInterface = pluginIf;
		logger = this.pluginInterface.getLogger().getTimeStampedChannel(this.pluginInterface.getPluginID());
		this.knownPeers = new Hashtable<String, Peer>();
		this.multiThreadedScanner = new MultiThreadedScanner(pluginInterface);
		
		// we add the configuration tab
		this.addPluginConfigModel();
		
		// we create a thread which listens on the listening port
		try {
			requestListener = new RequestListener(this.getListeningPort(), this.pluginInterface.getPluginconfig().getIntParameter("TCP.Listen.Port"), this.pluginInterface.getPluginconfig().getIntParameter("UDP.Listen.Port"), this);
			this.pluginInterface.getUtilities().createThread("peer lan scanner listener", requestListener);
		} catch (SocketException e) {
			logger.logAlert("LAN PEER SCANNER : Creation of listener on " + this.getListeningPort() + " port failed!", e);
		}
		
		// we create a periodical task which is going to scan the ip ranges from time to time
	    scanNetworkPeriodicalTimer = pluginInterface.getUtilities().createTimer("periodicalScanNetworkTimer");
	    scanNetworkPeriodicalTimer.addPeriodicEvent(pluginInterface.getPluginconfig().getPluginLongParameter(PARAM_SCAN_PERIODICITY)*1000*60*60, this);

	    //we launch a single shot task to delay the first scan (to let azureus loads itself in peace)
		scanNetworkTimerFirstTime = pluginInterface.getUtilities().createTimer("firstScanNetworkTimer");
	    scanNetworkTimerFirstTime.addEvent(pluginInterface.getPluginconfig().getPluginLongParameter(PARAM_FIRST_SCAN_DELAY)*1000, this);

	  
		
		// we create a listener on starting downloads to inject the known peers into the downloads
        pluginInterface.getDownloadManager().addListener(new DownloadManagerListener() {

            public void downloadAdded(Download download)
            {
                download.addListener(Plugin.this);
            }

            public void downloadRemoved(Download download)
            {
                download.removeListener(Plugin.this);
            }

        }, true);
	}

	private void addPluginConfigModel() {
		
		BasicPluginConfigModel configModel = pluginInterface.getUIManager().createBasicPluginConfigModel("Config." + PARAM_CONFIG_TABNAME);
		
		configModel.addLabelParameter2("Config." + PARAM_INTRO);
		configModel.addBooleanParameter2(PARAM_PLUGIN_ENABLED, "Config."+PARAM_PLUGIN_ENABLED, DEF_PLUGIN_ENABLED);
		configModel.addBooleanParameter2(PARAM_PEER_INJECTION_ENABLED, "Config."+PARAM_PEER_INJECTION_ENABLED, DEF_PEER_INJECTION_ENABLED);
		configModel.addBooleanParameter2(PARAM_DHT_USE_ENABLED, "Config."+PARAM_DHT_USE_ENABLED, DEF_DHT_USE_ENABLED);
		configModel.addIntParameter2(PARAM_LISTENING_PORT, "Config." + PARAM_LISTENING_PORT, DEF_LISTENING_PORT);
		configModel.addIntParameter2(PARAM_NB_THREADS, "Config." + PARAM_NB_THREADS, DEF_NB_THREADS);
		configModel.addStringParameter2(PARAM_IP_RANGES, "Config." + PARAM_IP_RANGES, DEF_IP_RANGES);
		configModel.addIntParameter2(PARAM_UDP_REQ_TIMEOUT, "Config." + PARAM_UDP_REQ_TIMEOUT, DEF_UDP_REQ_TIMEOUT);
		configModel.addIntParameter2(PARAM_SCAN_TIMEOUT, "Config." + PARAM_SCAN_TIMEOUT, DEF_SCAN_TIMEOUT);
		configModel.addIntParameter2(PARAM_FIRST_SCAN_DELAY, "Config." + PARAM_FIRST_SCAN_DELAY, DEF_FIRST_SCAN_DELAY);
		configModel.addIntParameter2(PARAM_SCAN_PERIODICITY, "Config." + PARAM_SCAN_PERIODICITY, DEF_SCAN_PERIODICITY);
		
		ActionParameter scanActionParameter = configModel.addActionParameter2("Config." + PARAM_LAUNCH_SCAN_BUTTON_LABEL, "Config." + PARAM_LAUNCH_SCAN_BUTTON_NAME);
		
		scanActionParameter.addListener(new ParameterListener(){

			public void parameterChanged(Parameter arg0) {
				Plugin.this.pluginInterface.getUtilities().createThread("scanRequestedByBottom", new Runnable () {
					public void run() {
						perform(null);
					}
				});
				
			}
			
		});
		
		configModel.addBooleanParameter2(PARAM_ALERTS_ENABLED, "Config."+PARAM_ALERTS_ENABLED, DEF_ALERTS_ENABLED);
		
		configModel.addHyperlinkParameter2("Config." + PARAM_LINK, URL);
	}

	
    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.utils.UTTimerEventPerformer#perform(org.gudy.azureus2.plugins.utils.UTTimerEvent)
     */
    public void perform(UTTimerEvent timerEvent)
    {
    	
    	if (isPluginEnabled()) {
    	
	    	//We launch the scan
	        logger.logAlertRepeatable(LoggerChannel.LT_INFORMATION, "Starting scan");
	        AddressesQueue addressesToCheck = new AddressesQueueStringTokenizer(pluginInterface.getPluginconfig().getPluginStringParameter(PARAM_IP_RANGES));
	        Map<String, Peer> foundPeers = this.multiThreadedScanner.scan(pluginInterface.getPluginconfig().getPluginIntParameter(PARAM_NB_THREADS), addressesToCheck,  this.getListeningPort(), pluginInterface.getPluginconfig().getPluginIntParameter(PARAM_UDP_REQ_TIMEOUT), pluginInterface.getPluginconfig().getPluginIntParameter(PARAM_SCAN_TIMEOUT)*1000*60, new Integer(pluginInterface.getPluginconfig().getIntParameter("TCP.Listen.Port")), new Integer(pluginInterface.getPluginconfig().getIntParameter("UDP.Listen.Port")));
	        logger.logAlertRepeatable(LoggerChannel.LT_INFORMATION, "Scan finished - " + foundPeers.size() + " peer(s) found");
	
	        //We put the new peers into the map of known peers
	        knownPeers.putAll(foundPeers);
	        
	        //We add the new peers into the ddb and the peer managers
	        injectPeers(foundPeers.values());
        
    	}
        
    }
    
    public void injectPeers(Collection<Peer> peers)
    {
        Download downloads[] = pluginInterface.getDownloadManager().getDownloads();
        for (Peer peer : peers) {
        	
        	//logger.logAlertRepeatable(LoggerChannel.LT_INFORMATION, (new StringBuilder("Peer found : ")).append(peer.toString()).toString());

        	if(!knownPeers.contains(peer))
            {

            	if (isPeerInjectionEnabled()) {
	                //Injecting peer into downloads'peer manager
	                for(int i = 0; i < downloads.length; i++)
	                {
	                    Download download = downloads[i];
	                    PeerManager pm = download.getPeerManager();
	                    if(pm != null)
	                    {
	                        logger.log((new StringBuilder("Injecting ")).append(peer.getIp()).append(" into '").append(download.getName()).append("': ").toString());
	                        pm.addPeer(peer.getIp(), peer.getTcpPort().intValue(), peer.getUdpPort().intValue(), false);
	                    }
	                }
            	}
                
                if (isDdbUseEnabled()) {
	            	//Adding the peer into the DDB
	                try {
	                	if (pluginInterface.getDistributedDatabase().isAvailable()) {
	                		pluginInterface.getDistributedDatabase().importContact(new InetSocketAddress(peer.getInetAdress(), peer.getUdpPort().intValue()));
	                	}
	                }
	                catch(DistributedDatabaseException e)
	                {
	                    logger.logAlert("Error importing peer into the Distributed Database", e);
	                }  
                }

            }
        }

    }
    
    /* (non-Javadoc)
     * @see lanpeerscanner.scan.PeerHandler#handleFoundPeer(lanpeerscanner.scan.Peer)
     */
    public void handleFoundPeer(Peer peer) {
    	
    	if (isPluginEnabled()) {
    		
    		//we log it
	        logger.log(LoggerChannel.LT_INFORMATION, (new StringBuilder("Peer found : ")).append(peer.toString()).toString());

	        //alertFoundPeer(peer)
	        this.alertFoundPeer(peer);
	        
	        if(!knownPeers.contains(peer))
	        {
	        	//we add the peer into the list of known peers
	            knownPeers.put(peer.getIp(), peer);
	            
	            //we add the peer into the peer managers and the DDB
	            List<Peer> peers = new ArrayList<Peer>();
	            peers.add(peer);
	            injectPeers(peers);
	        }
    	}
    }

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.download.DownloadListener#positionChanged(org.gudy.azureus2.plugins.download.Download, int, int)
	 */
	public void positionChanged(Download download, int old_position, int new_position) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.download.DownloadListener#stateChanged(org.gudy.azureus2.plugins.download.Download, int, int)
	 */
	public void stateChanged(Download download, int old_state, int new_state) {
		if (isPluginEnabled() && isPeerInjectionEnabled()) {
	        if(new_state == Download.ST_DOWNLOADING || new_state == Download.ST_SEEDING) {
	            for(Peer peer : this.knownPeers.values()) {
	                PeerManager pm = download.getPeerManager();
	                if(pm != null) {
	                    logger.log((new StringBuilder("Injecting ")).append(peer.getIp()).append(" into '").append(download.getName()).append("': ").toString());
	                    pm.addPeer(peer.getIp(), peer.getTcpPort().intValue(), peer.getUdpPort().intValue(), false);
	                }
	            }
	        }	
		}
	}
	
	public Boolean isPluginEnabled() {
		return pluginInterface.getPluginconfig().getPluginBooleanParameter(PARAM_PLUGIN_ENABLED, DEF_PLUGIN_ENABLED);
	}
	
	public Boolean isPeerInjectionEnabled() {
		return pluginInterface.getPluginconfig().getPluginBooleanParameter(PARAM_PEER_INJECTION_ENABLED, DEF_PEER_INJECTION_ENABLED);
	}
	
	public Boolean isDdbUseEnabled() {
		return pluginInterface.getPluginconfig().getPluginBooleanParameter(PARAM_DHT_USE_ENABLED, DEF_DHT_USE_ENABLED);
	}
	
	public Boolean areAlertsEnabled() {
		return pluginInterface.getPluginconfig().getPluginBooleanParameter(PARAM_ALERTS_ENABLED,DEF_ALERTS_ENABLED);
	}
	
	public int getListeningPort() {
		return pluginInterface.getPluginconfig().getPluginIntParameter(PARAM_LISTENING_PORT,DEF_LISTENING_PORT);
	}
	
	public void alertFoundPeer(Peer peer) {
		if (areAlertsEnabled()) {
			logger.logAlertRepeatable(LoggerChannel.LT_INFORMATION, this.pluginInterface.getUtilities().getLocaleUtilities().getLocalisedMessageText("Info.peer_found") + peer);
		}
	}
}
