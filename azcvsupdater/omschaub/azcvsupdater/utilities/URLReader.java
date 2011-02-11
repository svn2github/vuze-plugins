/*
 * Created on Feb 26, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import java.io.BufferedReader;
//import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import omschaub.azcvsupdater.main.StatusBox;
import omschaub.azcvsupdater.main.StatusBoxUtils;
import omschaub.azcvsupdater.main.Tab1;
import omschaub.azcvsupdater.main.Tab1_Subtab_1;
import omschaub.azcvsupdater.main.View;
import omschaub.azcvsupdater.main.Constants;

import org.json.simple.*;

public class URLReader {

    /** Holds the torrent url which was parsed out of the JSON file */
    private static String cvsurl;
    
    /** Holds the HTTP JAR download url which was parsed out of the JSON file */
    private static String jarurl;

    /** Holds the timestamp from the JSON file */
    private static String serverTimeStampVersion;
    
    /** Holds the long of time from the JSON file */
    private static long whenLastModified;
    
    /** Holds the string for the time when last Modified from the JSON file */
    private static String whenLastModifiedString;
    
    /**Holds the old version */
    private static String old_version;
     
     
    private static String parseDate(){
        Locale locale = Locale.ENGLISH;
        DateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss aa" ,locale);
                    
            sdf.setTimeZone(TimeZone.getDefault());
            try {
               //System.out.println(whenLastModifiedString);
                if(whenLastModifiedString == null){
                    whenLastModifiedString = "0";
                }else{
                    if(whenLastModifiedString.endsWith("CET")){
                        whenLastModifiedString = whenLastModifiedString.substring(0,whenLastModifiedString.length()-4);
                    }else if(whenLastModifiedString.endsWith("CEST")){
                        whenLastModifiedString = whenLastModifiedString.substring(0,whenLastModifiedString.length()-5);
                    }
                }
                
                //System.out.println(whenLastModifiedString );
                
                Date new_when = sdf.parse(whenLastModifiedString);
                
                if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("MilitaryTime"))
                {
                    sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss" );
                }
                else
                {
                    sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss aa" );
                }
                whenLastModified = new_when.getTime();
                whenLastModifiedString = sdf.format(new_when);
                
               // System.out.println(whenLastModified);
            } catch (ParseException e1) {
                System.out.println("Could Not Parse: " + whenLastModifiedString);
                StatusBoxUtils.mainStatusAdd(" Error: Could Not Parse Date Correctly: " + whenLastModifiedString,2);
                e1.printStackTrace();
            }
            return whenLastModifiedString;
    }
    
    
    /**
     * Gets the serverTimeStampVersion string and returns it
     * @return String serverTimeStampVersion
     */
    public static String get_serverTimeStampVersion(){
        return serverTimeStampVersion;
    }
    
    /**
     * Gets the cvsurl that was parsed from the main getURL function
     * @return String cvsurl
     */
    public static String get_cvsurl(){
        return cvsurl;
    }
    
    /**
     * Gets the jarurl that was parsed from the main getURL function
     * @return String jarurl
     */
    public static String get_jarurl(){
        return jarurl;
    }
    
    /** 
     * Gets the long value parsed from the main getURL function
     * @return long whenLastModified
     */
    public static long get_whenLastModified(){
        return whenLastModified;
    }
    
    /**
     * New URL parser for Paul's new php specific stuff
     */
    public static void newGetURL(){
        Thread getURL_thread = new Thread () {
            public void run () {
                try {
                        URL azureuscvs = new URL(Constants.VUZE_DEV_JSON_URL);
                        BufferedReader in = new BufferedReader(new InputStreamReader(azureuscvs.openStream()));
                        old_version = Tab1_Subtab_1.version;
                        
                        
                        Tab1_Subtab_1.version = "Checking....";
                        serverTimeStampVersion = "Checking....";

                        String inputLine;
                        String AzVersion;
                        String AzBuild;
                        
                        StringBuilder builder = new StringBuilder();
                        for (String line = null; (line = in.readLine()) != null;) {
                            builder.append(line).append("\n");
                        }
                        in.close();
                        //System.out.println(builder.toString()); 
                        
                        JSONObject obj = (JSONObject) JSONValue.parse(builder.toString());
                        JSONObject versions = (JSONObject) obj.get("versions");
                        JSONObject clientdev = null;
                        if (versions != null) { 
                            clientdev = (JSONObject) versions.get("client-dev");
                        }
                        // avoid possible NPE by a crude trick, assumes obj != null
                        if (clientdev == null) { 
                            clientdev =  obj;
                        }
                        
                        //first line is date
                        if((inputLine = (String) clientdev.get("timestamp")) != null)
                        {
                            whenLastModifiedString = inputLine;
                        }
                        else
                        {
                            whenLastModifiedString = "Checking....";
                        }
                        
                        //second field is version, and is parsed from version & build fields
                        if(((AzVersion = (String) clientdev.get("version")) != null) &&
                           ((AzBuild = (String) clientdev.get("build")) != null))
                        {
                        	Tab1_Subtab_1.version = 
                        		"Azureus" + AzVersion + "-B" + AzBuild + ".jar";
                        }
                        else
                        {
                            Tab1_Subtab_1.version = "Checking....";
                        }
                        
                        //third field is JAR HTTP url
                        if((inputLine = (String) clientdev.get("jar_url")) != null){
                            jarurl = inputLine;
                        }
                        else
                        {
                            jarurl = null;
                        } 

                        //fourth field is torrent url
                        if((inputLine = (String) clientdev.get("torrent_url")) != null){
                            cvsurl = inputLine;
                        }
                        else
                        {
                            cvsurl = null;
                        }
                     
                        View.syncExec(new Runnable (){
                        	public void run () {
                                if(Tab1_Subtab_1.displayVersion !=null && !Tab1_Subtab_1.displayVersion.isDisposed())
                                    Tab1_Subtab_1.version_color.setText(Tab1_Subtab_1.version);
                                    if (Tab1.listTable!= null && !Tab1.listTable.isDisposed())
                                        Tab1.listTable.deselectAll();
                            }
                        });

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e2) {
                           
                            e2.printStackTrace();
                        }
                        if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("TrayAlert"))
                        {
                            if (!Tab1_Subtab_1.version.startsWith("Checking") 
                                    && !Tab1_Subtab_1.version.startsWith("Could") 
                                    && !old_version.startsWith("Checking")
                                    && !old_version.startsWith("Could")
                                    && !old_version.equals(Tab1_Subtab_1.version)) 
                            {
                                TrayAlert.open();
                            }
                        }

                        if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false)){
                            if (!Tab1_Subtab_1.version.startsWith("Checking") 
                                    && !Tab1_Subtab_1.version.startsWith("Could") 
                                    && !old_version.startsWith("Checking")
                                    && !old_version.startsWith("Could")
                                    && !old_version.equals(Tab1_Subtab_1.version)) 
                            {
                                TorrentUtils.removeLastSeed(old_version);
                            }
                        }
                        
                        serverTimeStampVersion = parseDate();
                        if (!View.getDisplay().isDisposed()) {
	                        View.getDisplay().asyncExec(new Runnable (){
	                            public void run () {
	                                if(Tab1_Subtab_1.timestamp_color !=null && !Tab1_Subtab_1.timestamp_color.isDisposed()){
	                                    if (serverTimeStampVersion != null) {
	                                    	Tab1_Subtab_1.timestamp_color.setText(serverTimeStampVersion);
	                                    }
	                                    Tab1_Subtab_1.timestamp_color.redraw();
	                                    Tab1_Subtab_1.timestamp_color.getParent().layout();
	                                    Tab1_Subtab_1.timestamp_color.getParent().getParent().layout();
	                                    Tab1.redrawSubTab();
	                                }
	                            }
	                        });
                        }
                        
                        
                        
                }catch(IOException e) {
                    View.asyncExec( new Runnable() {
                    	public void run() {
                            
                                
                            if(StatusBox.status_group != null && !StatusBox.status_group.isDisposed()){
                                //We should NEVER pack the status_group
                                //StatusBox.status_group.pack();
                                StatusBox.status_group.layout();
                            }
                                
                                StatusBoxUtils.mainStatusAdd(" HTTP Connection ERROR - Either 'Manual Update' or Click blue link to visit site",2);
                                if(Tab1_Subtab_1.timestamp_color != null && !Tab1_Subtab_1.timestamp_color.isDisposed()){
                                    Tab1_Subtab_1.timestamp_color.setText("Could Not Update!");
                                    if (serverTimeStampVersion != null) {
                                    	Tab1_Subtab_1.timestamp_color.setText(serverTimeStampVersion);
                                    }
                                    Tab1_Subtab_1.timestamp_color.redraw();
                                    Tab1_Subtab_1.timestamp_color.getParent().layout();
                                    Tab1_Subtab_1.timestamp_color.getParent().getParent().layout();
                                }
                                if(Tab1_Subtab_1.version_color != null &&  !Tab1_Subtab_1.version_color.isDisposed()){
                                    Tab1_Subtab_1.version_color.setText("Could Not Update!");
                                    
                                }
                        }
                    });
                    StatusBoxUtils.mainStatusAdd(" AZCVSUpdater:  Error Connecting. HTTP ERROR - Socket Timeout likely",2); 
                }
            }
        };
        getURL_thread.setDaemon(true);
        getURL_thread.start();
    }
}
