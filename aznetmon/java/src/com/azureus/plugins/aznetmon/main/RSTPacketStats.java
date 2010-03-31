/**
 * Created on Dec 17, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.azureus.plugins.aznetmon.main;

import com.azureus.plugins.aznetmon.util.CdnMonitorTimeUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.gudy.azureus2.core3.config.COConfigurationManager;

public class RSTPacketStats
{

    public static final long INTERVAL = 1000 * 60 * 10; //show a stat every 10 min.

    //Some constants for other packages to use.
    public static final String PLUGIN_MAP_NAME = "plugin.versionserver.data";
    public static final String RESET_RECORDS = "reset_records";
    public static final String RESET_DEBUG = "reset_debug";

    //reset_records data items.
    public static final String TIME = "time";
    public static final String N_RST = "nRST";
    public static final String N_ACTIVE = "nActive";
    public static final String N_PASSIVE = "nPassive";
    public static final String N_FAILED = "nFailed";
    public static final String C_OPEN = "cOpen";


    
    private static RSTPacketStats instance = new RSTPacketStats();

    //data
    private List rstPacketDataList; //<RSTPacketData>
    private List dataListeners; //<DataListener>

    private String status;


    public static RSTPacketStats getInstance() {
        return instance;
    }

    private RSTPacketStats()
    {
        rstPacketDataList = new ArrayList(); //<RSTPacketData>
        dataListeners = new ArrayList(); //<DataListener>
        status = "Initial data is zero. Collection is 10 min interval.";
    }

    /**
     * What is the status of the application.
     * @param _status -
     */
    public void setStatus(String _status){
        status = _status;
    }

    /**
     *
     * @return -
     */
    public String getStatus(){
        return status;
    }

    
    public void add(RSTPacketData d){
        rstPacketDataList.add(0,d);

        //inform any listeners of this new data.
        update();

        boolean isShared = COConfigurationManager.getBooleanParameter("aznetmon.share");
        String ending = ".";
        if(isShared){
            ending = "!";
        }

        //update the status
        setStatus( "Last measurement at "+CdnMonitorTimeUtils.getReadableTime(d.timestamp) + ". Next update in 10 minutes"+ending );

    }//add


    public RSTPacketData getMostRecent(){

        if( rstPacketDataList.size()>0 ){
            return (RSTPacketData) rstPacketDataList.get(0);
        }else{
            return null;
        }
    }

    /**
     * Get upto n most recent results.
     * @param n -
     * @return -
     */
    public RSTPacketData[] getMostRecent(int n){

        int size = rstPacketDataList.size();
        if( n<size ) size = n;

        if(size==0){
            RSTPacketData[] def = new RSTPacketData[1];
            def[0] = new RSTPacketData();
            return def;
        }

        RSTPacketData[] retVal = new RSTPacketData[size];
        for(int i=0; i<size; i++){

            retVal[i] = (RSTPacketData) rstPacketDataList.get(i);

        }//for
        
        return retVal;
    }


    /**
     * Gather the results that haven't been sent to version server yet and
     * create one result. Then mark all these results as sent!!
     * @return - RSTPacketData - summary of un-sent results!!
     */
    public RSTPacketData gatherUnstoredResults()
    {

        List unstoredResults = new ArrayList(); //<RSTPacketData>

        int size = rstPacketDataList.size();

        //Put the unsorted values into a list.
        for(int i=0;i<size;i++){

            RSTPacketData curr = (RSTPacketData) rstPacketDataList.get(i);

            //Don't count stored data.
            if( !curr.isStored ){
                curr.isStored=true;
                //now just incase will store this back into the list.
                rstPacketDataList.set(i,curr);

                unstoredResults.add(curr);
            }

        }//for

        RSTPacketData retVal = new RSTPacketData();
        return retVal.accumulateDeltas(unstoredResults);

    }//gatherUnstoredResults

    /**
     * Check the last result and the interval. If the next result will cross the one hour
     * boundary then return true. Otherwise return false.
     * @return - boolean
     */
    public boolean isTimeToStoreResults()
    {

        RSTPacketData data = getMostRecent();

        //Current Time
        Calendar curr = Calendar.getInstance();
        curr.setTime( new Date(data.timestamp) );

        //Next Interval
        Calendar next = Calendar.getInstance();
        next.setTime( new Date(data.timestamp + RSTPacketStats.INTERVAL) );


        //If the hour setting is different then return true.
        int hCurr = curr.get( Calendar.HOUR_OF_DAY );
        int hNext = next.get( Calendar.HOUR_OF_DAY );

        return (hCurr != hNext);
        
    }//isTimeToStoreResults

    public synchronized void addListener(DataListener dl){

        dataListeners.add(dl);
        
    }//addListener

    public synchronized void removeListener(DataListener dl){

        dataListeners.remove(dl);

    }//removeListener

    public synchronized void update(){

        int size = dataListeners.size();
        for(int i=0; i<size; i++ ){
            DataListener dl = (DataListener) dataListeners.get(i);
            dl.update();
        }

    }//update


    /**
     * data-type.
     */
    public static class RSTPacketData{

        public boolean isStored=false; //true when included with version server.
        public long timestamp=0;
        public String msg = "";

        public int nConnReset=0;
        public int nActiveOpens=0;
        public int nPassiveOpens=0;
        public int nFailedConnAttempt=0;
        public int nCurrentOpen=0;

        public long deltaTime=0;
        public int deltaConnReset=0;
        public int deltaActiveOpens=0;
        public int deltaPassiveOpens=0;
        public int deltaFailedConnAttempt=0;

        //% RST =  Connection Resets / ( OpenActive + PassiveOpen - Failed Connection )
        //% incoming connections =  passive opens / (active open + passive open)
        public float percentRSTConn = 0.0f;
        public float percentIncomingConn = 0.0f;


        //Note: This is used for debugging only.
        public boolean isDebugMode = false;
        public String[] debugLines = null;
        


        public RSTPacketData(){
            timestamp = System.currentTimeMillis();
        }

        public RSTPacketData(String _msg){
            msg = _msg;
            percentIncomingConn = -1.0f;
        }


        /**
         * Call before sending results to version server.
         * @param l - <RSTPacketData> -list of unstored results.
         * @return - RSTPacket, with the delta values accululted.
         */
        public RSTPacketData accumulateDeltas(List l){

            RSTPacketData retVal = new RSTPacketData();

            if( l==null || l.size()==0 ){
                return retVal;
            }

            boolean allDebugFlagsSet=true;

            int size = l.size();
            for(int i=0; i<size; i++){

                RSTPacketData curr = (RSTPacketData) l.get(i);

                retVal.deltaConnReset += curr.deltaConnReset;
                retVal.deltaActiveOpens += curr.deltaActiveOpens;
                retVal.deltaPassiveOpens += curr.deltaPassiveOpens;
                retVal.deltaFailedConnAttempt += curr.deltaFailedConnAttempt;

                //keep the timestamp of the oldest item.
                if( curr.timestamp > retVal.timestamp ){
                    retVal.timestamp = curr.timestamp;
                }

                if( !curr.isDebugMode ){
                    allDebugFlagsSet=false;
                }

            }//for

            calculatePercentValues();

            //Send debug information. - remove before release. 
            if(allDebugFlagsSet){
                RSTPacketData curr = (RSTPacketData) l.get(0);
                retVal.isDebugMode = curr.isDebugMode;
                retVal.debugLines = curr.debugLines;
            }

            return retVal;
        }//accumulateDeltas

        /**
         * Calculate percentages and deltas based on the raw data.
         * @param prev -
         */
        public void calculate(RSTPacketData prev){

            deltaTime = timestamp - prev.timestamp;
            deltaConnReset = nConnReset - prev.nConnReset;
            deltaActiveOpens = nActiveOpens - prev.nActiveOpens;
            deltaPassiveOpens = nPassiveOpens - prev.nPassiveOpens;
            deltaFailedConnAttempt = nFailedConnAttempt - prev.nFailedConnAttempt;

            calculatePercentValues();
            
        }

        private void calculatePercentValues() {
            if( deltaActiveOpens+deltaPassiveOpens-deltaFailedConnAttempt>0 ){

                percentRSTConn = (float)deltaConnReset / (float)(deltaActiveOpens+deltaPassiveOpens-deltaFailedConnAttempt);

            }

            if( deltaActiveOpens+deltaPassiveOpens>0 ){

                percentIncomingConn = (float)deltaPassiveOpens / (float)(deltaActiveOpens+deltaPassiveOpens);

            }
        }


        /**
         * Return any debug information available.
         * @return - String of debug output.
         */
        public String getDebugOutput(){
            StringBuffer sb = new StringBuffer("");

            for(int i=0; i<debugLines.length;i++){
                String line = debugLines[i];
                //don't let the total size exceed 1k-bytes.
                int currLen = sb.length();
                int futureLen = currLen+line.length();
                if(futureLen > 1024){
                    break;
                }

                sb.append("\n").append(line);
            }
            return sb.toString();
        }//getDebugOutput

    }//static class RSTPacketData


}//class
