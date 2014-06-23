
/**********************************************************************

  Class name  [PluginVuze.java]

  Abstract    [Main class of the TorrentGuard plugin]

  Description [In charge of initializing the plugin, of dealing with the installation/
  			  uninstallation/changing events in the state of the downloads.
  			  It gets the user's downloads, and sends it to the server via a socket 
  			  communication, then alerts the user with the pertinent information]

*************************************************************************************/
package torrent_guard;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadCompletionListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;

public class PluginVuze implements UnloadablePlugin {

	private DownloadManager download_manager;
	private BasicPluginConfigModel config_page;
	private static Download[] downloads;
	private static Download[] new_downloads;
	private byte[][] id;
	private Torrent[] torrents;
	private String[]  nombre_arch;
    private static LoggerChannel channel;
    private DataInputStream buffer2;
    
    //By default values:
    private int puerto_a = 35555;
	private String b = "163.117.166.253"; // server IP

	//Objects required for the communication between the plugin and Vuze
	PluginListener plugin_listener1;
	PluginInterface plugin_interface1;
	DownloadManagerListener download_listener1;
	DownloadCompletionListener DLC;
	
	//Variables changed in the user pane. Used by the user to select which messages he wants to see
	private static boolean trusted = true;
	private static boolean fake = true;
	private static boolean unknown = true;
	
	private AsyncDispatcher async_dispatcher = new AsyncDispatcher();
	private TorrentAttribute  ta_fake_status;
	private TorrentAttribute ta_reminder; //Attribute used to tell the user a torrent is trusted or not recognized just once
	
	//The alert system depends on the number of torrents the user keeps.
	private static int max_dl= 15 ; 
	private int n_alerts;
	private PluginConfig pconfig;
	
	/* initialize Method ********************************************************************
    Descripction     [Executed every time Vuze is initialized. In charge of running  
    				the plugin and communication with the server. It also attends all the
    				running events during the execution]
    				
    Parameter   	[None]
    *****************************************************************************************/
	public void initialize(PluginInterface plugin_interface) throws PluginException {
		 //Initialize max_dl parameter
		pconfig = plugin_interface.getPluginconfig();
	    max_dl = pconfig.getPluginIntParameter("max dl");
		pconfig.setPluginParameter("max dl", max_dl);
		
		// Creation of the DownloadManager and the plugin configuration section:
		download_manager = plugin_interface.getDownloadManager();
        config_page = plugin_interface.getUIManager().createBasicPluginConfigModel("plugin_vuze");
        crear_pag_config();
        
        //Creation of fake_status TorrentAttribute
        ta_fake_status  = plugin_interface.getTorrentManager().getPluginAttribute("fake_status");
        
        //Creation of reminder TorrentAttribute
        ta_reminder = plugin_interface.getTorrentManager().getPluginAttribute("reminder");
                       
        // Creation of the alert channel::
        channel = plugin_interface.getLogger().getChannel("plugin_vuze");

		// Creation of a listener. The plugin in installed/uninstalled:
		plugin_interface.addListener((plugin_listener1=new PluginListener(){
			public void closedownComplete(){
				;
			}
			public void closedownInitiated() {
				;
			}
			public void initializationComplete() {
				System.out.println("PLUGIN INSTALLED");
			}
		}));

		// New listener: changes in the states of the downloads.
		download_manager.addListener((download_listener1=new DownloadManagerListener() {
		    public void downloadAdded(final Download download) {
		    //a new download being added in the middle if a session
            System.out.println("NEW DOWNLOAD ADDED TO THE LIST");
            async_dispatcher.dispatch(new AERunnable(){
            	public void runSupport(){
            		actualizar(download, 0);
            		}
            	}); 
		    }
			public void downloadRemoved(Download download) {
				;
			}
		}),    false);		
		// If this is the first time the user runs the plugin, he is given a new id.

			//Checks if the client has an ID assigned
			if(file_handler.comprobacion_fichero_cliente() == false){				
				// A new random id is generated for the new client:
				file_handler.generar_id();
				file_handler.escribe_fichero(file_handler.ident);
			}
   	    	// We send the server the generated data:
			async_dispatcher.dispatch(new AERunnable(){
				public void runSupport(){
					comunic_servidor();
					// All the new user's data has been sent to the server. 					
				}
			});

   	    plugin_interface1 = plugin_interface;
	}

	/* arranque_descargas Method ***************************************************************
    Description     [Gets and stores in arrays the downloads and associated
     				 parameters found in the user's library]
    Parameters  	[-]
    *****************************************************************************************/
	public void arranque_descargas(){
		try{
			// Creation of a Download Objects array from the DownloadManager:
			downloads = download_manager.getDownloads();
			//We calculate the number of new downloads
			int num_new_dl = calculate_new_downloads(downloads);
			if(num_new_dl>0){
				//Initialization of another Download array to store the new downloads
				new_downloads = new Download[num_new_dl];
				//Creation of an array for the parameters we need: 
				torrents = new Torrent[num_new_dl];
				nombre_arch = new String[num_new_dl];
				id = new byte[num_new_dl][20];
			}
			
			
			int count=0;
			// Goes over the download array getting each torrent parameters:
			if(downloads != null){
				//We are sending information of the torrents we do not know yet 
				for(int i=0;i<downloads.length;i++){
					//We store the downloads we do not know
					if(downloads[i].getIntAttribute( ta_fake_status )==0){
						new_downloads[count] = downloads[i];						
						//TorrentAttribute: reminder
						if(downloads[i].getBooleanAttribute(ta_reminder)!=true)
							downloads[i].setBooleanAttribute(ta_reminder, false);
						// -- FILE NAME --
						nombre_arch[count] = downloads[i].getName();
						// -- TORRENT FILE NAME --
						torrents[count] = downloads[i].getTorrent();
						// -- ID --
						// Storing of each torrent's infohash in the id array:
						id[count] = torrents[count].getHash();
						count++;
					}else{
						if(n_alerts<max_dl)
							//We inform the user about the torrents we know
							comprobacion_falso(null,false,downloads[i]);
					}										
				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.out.println("Exception raised. Could not get downloads");
		}
}

	/* comunic_servidor Method **************************************************************
    Description     [Opens a socket communication with the server and sends the downloads 
    				and user's information parameters]
    Parameters  	[-]
    *****************************************************************************************/
	public void comunic_servidor(){
		Socket socket = null;  
		try{
        	
             // Creation of InetAddress with the server's IP address:
            InetAddress addr = InetAddress.getByName(b);

            // Client's socket and output data stream creation:
            socket = new Socket();
            socket.connect( new InetSocketAddress(addr, puerto_a ),30*1000); 
            socket.setSoTimeout( 30*100 );
            DataOutputStream buffer = new DataOutputStream (socket.getOutputStream());
            
            // The client's ID and downloads are sent to the server:
            buffer.writeUTF(file_handler.ident);            
            buffer.writeBoolean(true);
            envio_descargas(buffer,socket); 
            
            buffer.close();
            socket.close();
            
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception raised. Could not connect to server");
		}finally{
			if ( socket != null ){
				try{
					socket.close();
				}catch(Throwable e){
					e.printStackTrace();
				}
			}
		}
	}
	
	/* actualizar Method ********************************************************************
    Description     [We inform the client about the state of a torrent through a Download object
    				and an integer. This method updates the server sending the pertinent information
 					in each case]
    Parameters  	[Download Object and the int that notifies the event]
    *****************************************************************************************/
	public void actualizar(Download download,int cambio){
		Torrent torrent;
		Socket socket = null;
		try{
			if(download.getIntAttribute(ta_fake_status)==0){
				download.setBooleanAttribute(ta_reminder, false);
				// Creation of InetAddress with the server's IP address:
				InetAddress addr = InetAddress.getByName(b);

            	// Client's socket and output data stream creation:
            	socket = new Socket();
            	socket.connect( new InetSocketAddress(addr, puerto_a ),30*1000); 
            	socket.setSoTimeout( 30*100 );
            	DataOutputStream buffer = new DataOutputStream (socket.getOutputStream());
            	// Sends the client ID:
            	buffer.writeUTF(file_handler.ident);            
            	buffer.writeBoolean(false);

            	// --Different parameters are sent depending on the value of "cambio"--
            	// New download: all parameters are sent
            	if (cambio == 0){ 
                	buffer.writeUTF (download.getName());
                	torrent = download.getTorrent();
                	buffer.writeUTF(conversion_hexadecimal(torrent.getHash()));
                	comprobacion_falso(socket,true,download);
            	}
            	socket.close();
            	buffer.close();
            	
				}else
					comprobacion_falso(null,false,download);
			
			} catch (Exception e){
				System.out.println("Exception raised. Could not update a download");
			}finally{
				if ( socket != null ){
					try{
						socket.close();
					}catch( Throwable e){
						e.printStackTrace();
				}
			}
		}
	}

	/* comprobacion_falso Method ************************************************************
    Description     [Executed after sending the server the parameters of a download. Informs 
    				the user about the authenticity of that download. By default the user will
    				be alerted if the content is either trusted or fake. These parameters may be
    				changed through the configuration pane]
    *****************************************************************************************/
    public void comprobacion_falso(Socket cliente, boolean isNew, Download download){
    	String id_torrent = null;
    	int var_falso=-1;
    	try{
    		//if the torrent does not have a value for the variable fake_status it is new and its
    		//information is sent to the server
    		if(isNew){
    			// Client accepts a socket connection and creates  an input data stream reader:
    			cliente.setSoLinger (true, 10);
    			buffer2 = new DataInputStream (cliente.getInputStream());		
    			// Gets the id torrent and the integer that provides the authenticity info:
    			id_torrent = buffer2.readUTF();
    			var_falso = buffer2.readInt();
    			
    			//The value fake_status of a given torrent is updated
    			download.setIntAttribute( ta_fake_status, var_falso);
    			
    		//if the torrent is known we check its authenticity via the fake_status variable 
    		}else{
    			var_falso = download.getIntAttribute( ta_fake_status );
    			id_torrent = download.getName();
    		}
    		// We alert the user. He gets to choose via the configuration pannel which messages he wants
    		// to get:
    		System.out.println(id_torrent+"       "+var_falso+"    "+unknown);
    		
  	 		if(var_falso == 1 && fake){ 	 			
  	 			channel.logAlertRepeatable(LoggerChannel.LT_INFORMATION, "FAKE CONTENT:\n"+id_torrent);
  	 		}
  	 		if(var_falso == 2 && trusted && download.getBooleanAttribute(ta_reminder)==false){
  	 			channel.logAlertRepeatable(LoggerChannel.LT_INFORMATION, "TRUSTED CONTENT:\n"+id_torrent);
  	 			download.setBooleanAttribute(ta_reminder, true);
  	 		}
  	 		if(var_falso==0 && unknown && download.getBooleanAttribute(ta_reminder)==false){
  	 			channel.logAlertRepeatable(LoggerChannel.LT_INFORMATION,"NO SUSPICION REPORTED:\n"+id_torrent);
  	 			download.setBooleanAttribute(ta_reminder, true);
  	 		}
    	}catch (Exception e){
    		System.out.println("Issues found with the fake authentication");
    	}
    }
    
    /* set_fake_status***********************************************************************
     Description:	[called when the number of alerts exceeds the parameter "max-dl". This method
     				just sets the variable fake_status of the new torrents]
     *************************************************************************************/
    public void set_fake_status(Socket cliente, Download download){
    	int var_falso=-1;
    	String idtorrent = null;
    	try{
    		//if the torrent does not have a value for the variable fake_status it is new and its
    		//information is sent to the server

    		// Client accepts a socket connection and creates  an input data stream reader:
    		cliente.setSoLinger (true, 10);
    		buffer2 = new DataInputStream (cliente.getInputStream());		
    		// Gets the id torrent and the integer that provides the authenticity info:
    		idtorrent = buffer2.readUTF();
    		var_falso = buffer2.readInt();
    		
    		//The value fake_status of a given torrent is updated
    		download.setIntAttribute( ta_fake_status, var_falso);
    		
 	 		if(var_falso == 1 && fake) 	 			
  	 			channel.logAlertRepeatable(LoggerChannel.LT_INFORMATION, "FAKE CONTENT:\n"+idtorrent);
    		
    	}catch (Exception e){
    		System.out.println("Issues found with the fake authentication");
    	}
    }
    
    /*count_no_suspicious*************************************************************************
     Description	[Given a list of torrents, this method counts the number of "not suspicious"
     				alerts the plugin is going to show]
     Parameters		[List of torrents]
     ******************************************************************************************/
    private int count_no_suspicious(Download[]dl_list){
    	int no_suspicious=0;
    	for(int i=0;i<dl_list.length;i++){
    		if((dl_list[i].getIntAttribute(ta_fake_status))==0 && unknown && dl_list[i].getBooleanAttribute(ta_reminder)==false){
    			dl_list[i].setBooleanAttribute(ta_reminder, true);
    			no_suspicious++;
    		}
    	}
    	System.out.println("A number of "+no_suspicious+" no suspicious torrents reported");
    	return no_suspicious;
    }
    
    /*count_trusted********************************************************************************
     Description	[Given a list of torrents, this method counts the number of "trusted"
     				alerts the plugin is going to show]
     Parameters		[List of torrents]
     *********************************************************************************************/
    private int count_trusted(Download[]dl_list){
    	int n_trusted=0;
    	for(int i=0;i<dl_list.length;i++){
    		if((dl_list[i].getIntAttribute(ta_fake_status))==2 && trusted && dl_list[i].getBooleanAttribute(ta_reminder)==false){
    			dl_list[i].setBooleanAttribute(ta_reminder, true);
    			n_trusted++;
    		}
    	}
    	System.out.println("A number of "+n_trusted+" no suspicious torrents reported");
    	return n_trusted;	
    }
    
    /*show_alerts********************************************************************************
     Descrption		[Show the alerts when the number of alerts exceeds max_dl]
     Parameters		[]
     *******************************************************************************************/
    public void show_alerts(){
    	int no_suspicious = count_no_suspicious(downloads);
    	int n_trusted = count_trusted(downloads);
    	if(n_trusted>0)
    		channel.logAlertRepeatable(LoggerChannel.LT_INFORMATION, n_trusted+" TRUSTED TORRENTS\n");
    	if(no_suspicious>0)
    		channel.logAlertRepeatable(LoggerChannel.LT_INFORMATION, no_suspicious+" TORRENTS WITH NO SUSPICION REPORTED\n");
    }
    
	/* envio_descargas Method ***************************************************************
    Description     [Writes in a buffer the information concerning the existent downloads
    				in the downloads so that the server may read it]
    Parameters  	[Output data stream where the downloads parameters shall be written]
    *****************************************************************************************/   
    public void envio_descargas(DataOutputStream buffer, Socket socket){
        try {
        	arranque_descargas();
        	//We calculate the nº of alerts the plugin may print
        	n_alerts = calculate_alerts(downloads);
        	// Sends the number of downloads we are updating
        	if(new_downloads!=null)
        		buffer.writeInt(new_downloads.length);
        	else
        		buffer.writeInt(0);
		    // Sends all the parameters of the downloads currently in the library:
        	if ( new_downloads != null ){
		        for(int i=0;i<new_downloads.length;i++){
		            buffer.writeUTF (nombre_arch[i]);
		            buffer.writeUTF(conversion_hexadecimal(id[i]));
		            
		            if(n_alerts < max_dl)
		            	comprobacion_falso(socket,true, new_downloads[i]);
		            else
		            	set_fake_status(socket, new_downloads[i]);
		        }
        	}
	        if(n_alerts> max_dl)
	        	show_alerts();
	        	
	        System.out.println ("A number of "+downloads.length+" downloads sent");
		} catch (IOException e) {
			System.out.println("Exception raised. Could not send downloads to the server");
		}
    }     

	/* conversion_hexadecimal Method********************************************************
    Description     [Converts the bytes array parameter into its String equivalent]
    Parameters  	[Infohash byte array]
    Returns 		[String that results from conversion]  
    *****************************************************************************************/
    public static String conversion_hexadecimal(byte[] datos)
    {
    	String resultado="";
        ByteArrayInputStream input = new ByteArrayInputStream(datos);
        String cadena_aux;
        int leido = input.read();
        while(leido != -1)
        {
        	cadena_aux = Integer.toHexString(leido);
            if(cadena_aux.length() < 2)
            	resultado += "0";
                resultado += cadena_aux;
                leido = input.read();
            }
        return resultado;
    }

	/* crear_pag_config Method ***************************************************************
    Description     [Creates the configuration page of the plugin that allows the user to
    				modify the by default values]
    Parameters  	[-]  
    *****************************************************************************************/
	public void crear_pag_config(){
		config_page.addLabelParameter2("plugin_vuze.config.vacio");
		config_page.addLabelParameter2("plugin_vuze.config.titulo");

		config_page.addLabelParameter2("plugin_vuze.config.vacio");
		final BooleanParameter fake_param = config_page.addBooleanParameter2("fake warnings enabled", "plugin_vuze.config.fake", fake);
		final BooleanParameter trusted_param = config_page.addBooleanParameter2("trusted warnings enabled", "plugin_vuze.config.trusted", trusted);
		final BooleanParameter unknown_param = config_page.addBooleanParameter2("unknown warnings disabled", "plugin_vuze.config.unknown", unknown);
		
		config_page.addLabelParameter2("plugin_vuze.config.vacio");
		final BooleanParameter enabled_param = config_page.addBooleanParameter2("max nº of DL", "plugin_vuze.config.enable", true);
		final IntParameter max_dl_param = config_page.addIntParameter2("max nº of DL","plugin_vuze.config.max_dl", pconfig.getPluginIntParameter("max dl"));
		enabled_param.addEnabledOnSelection(max_dl_param);
		
		config_page.addLabelParameter2("plugin_vuze.config.vacio");
		ActionParameter change = config_page.addActionParameter2("plugin_vuze.config.vacio", "plugin_vuze.config.cambiar");
		change.addListener(new ParameterListener(){
			public void parameterChanged(Parameter p){
				fake = fake_param.getValue();
				trusted = trusted_param.getValue();
				unknown = unknown_param.getValue();	
				if(enabled_param.getValue())
					pconfig.setPluginParameter("max dl", max_dl_param.getValue());
			}
		});

		config_page.addLabelParameter2("plugin_vuze.config.vacio");
		config_page.addLabelParameter2("plugin_vuze.config.titulo2");
		config_page.addHyperlinkParameter2("plugin_vuze.config.wiki_link","http://wiki.vuze.com/w/User_Guide");
	}
	
	/*calculate_new_downloads************************************************************************
	 Description	[returns the number of downloads with its fake_status variable set to 0. If the 
	 				authenticity of a torrent is set to unknown, then it is also considered to be new]
	 Parameters		[list of torrents stored by the client]
	 **************************************************************************************************/
	private int calculate_new_downloads(Download [] dl_list){
		int n_downloads=0;
		for(int i=0;i<dl_list.length;i++){
			if(dl_list[i].getIntAttribute( ta_fake_status )==0)
				n_downloads++;
		}
		return n_downloads;
	}
	
	/*calculate_alerts*******************************************************************************
	 Description	[returns the number of alarms the plugin would alert about given a list of torrents]
	 Parameters		[list of torrents stored by the client]
	 **************************************************************************************************/
	private int calculate_alerts(Download [] dl_list){
		int n_alerts=0;
		for(int i=0;i<dl_list.length;i++){
			int var_falso = dl_list[i].getIntAttribute( ta_fake_status );
			if(var_falso == 1 && fake) 	 			
  	 			n_alerts++;
  	 		if(var_falso == 2 && trusted && dl_list[i].getBooleanAttribute(ta_reminder)==false)
  	 			n_alerts++;
  	 		if(var_falso==0 && unknown && dl_list[i].getBooleanAttribute(ta_reminder)==false)
  	 			n_alerts++;
  	 		
		}
		System.out.println("Reporting about "+n_alerts+" downloads");
		return n_alerts;
	}
	

	
	/* unload Method*****************************************
	Description		[Makes the plugin unloadable]
	Parameters 		[None]				
	*************************************************************/
	public void unload() throws PluginException{
		plugin_interface1.removeListener(plugin_listener1);
		plugin_interface1.getDownloadManager().removeListener(download_listener1);
		plugin_interface1.getDownloadManager().getGlobalDownloadEventNotifier().removeCompletionListener(DLC);
		config_page.destroy();
	}
}

