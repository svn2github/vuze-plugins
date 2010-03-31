package com.azureus.plugins.aznetmon.util;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;


/**
 * Created on May 24, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class CdnMonitorTimeUtils {

    public static final String TIME_FORMAT_YYYYMMDD = "yyyyMMdd'T'HHmmss";
    public static final String TIME_FORMAT_READABLE = "yyyy-MM-dd HH:mm:ss";

    private CdnMonitorTimeUtils(){}

    /**
     * Format June 06, 2007, 6:30:24 PM
     *          as
     *     20070606T183024
     * 
     * @return Current Time in formatted YYYYMMDD'T'HHmmss
     */
    public static String getFormattedTime(){
        return getFormattedTime( System.currentTimeMillis() );
    }

    /**
     * Format June 06, 2007, 6:30:24 PM
     *          as
     *     20070606T183024
     *
     * @param javaTimestamp - time
     * @return Current Time in formatted YYYYMMDD'T'HHmmss
     */
    public static String getFormattedTime(long javaTimestamp){
        SimpleDateFormat fmt = new SimpleDateFormat(TIME_FORMAT_YYYYMMDD);
        return fmt.format( new Date(javaTimestamp) );
    }

    /**
     * Fomat June 06, 2007, 6:30:24 PM
     *         as
     *   2007-06-06 18:30:24
     * @param javaTimestamp -
     * @return -
     */
    public static String getReadableTime(long javaTimestamp){
        SimpleDateFormat fmt = new SimpleDateFormat(TIME_FORMAT_READABLE);
        return fmt.format( new Date(javaTimestamp) );
    }

    /**
     * Convert the Java time-stamp in milliseconds into Unix time in seconds.
     * @param javaTimeInMilliSec - from System.currentTimeMilliSec()
     * @return - UNIX time is in seconds since epoch, Java time is milliseconds.
     */
    private static long convertJavaTime2UnixTime(long javaTimeInMilliSec){
        return ( javaTimeInMilliSec/1000 );
    }

    /**
     * From: [0606 18:30:24]
     * To:   20070606T183024
     * @param azureusTimeFormat -
     * @return -
     */
    public static String convertAzuresutLogTimeFormat2FormattedTime(String azureusTimeFormat){
        long unixTime = convertAzureusLogTimeFormat2UnixTime(azureusTimeFormat);
        return getFormattedTime( unixTime*1000 );
    }

    /**
     * The Azureus time format is [2005 22:48:20] - This is May 20 at 10:48:20 PM.
     *
     * @param azureusTimeFormat -
     * @return -
     * @throws IllegalArgumentException - if the input is not in the format.
     */
    public static long convertAzureusLogTimeFormat2UnixTime(String azureusTimeFormat)
    {
        String original = azureusTimeFormat;

        //remove the [ ] from the items.
        azureusTimeFormat = azureusTimeFormat.replace('[',' ');
        azureusTimeFormat = azureusTimeFormat.replace(']',' ');
        azureusTimeFormat = azureusTimeFormat.trim();

        String[] times = azureusTimeFormat.split(" ");

        //Check the format.
        if(times==null || times.length!=2){
            throw new IllegalArgumentException("Incorrect format - Not two parts: [DDMM hh:mm:ss]. Was="+original);
        }

        //is the first part a number.
        try{
            Integer.parseInt( times[0] );
        }catch(Throwable t){
            throw new IllegalArgumentException("Incorrect format - DDMM part not a number: [DDMM hh:mm:ss]. Was="+original);
        }

        //Does the second part contain the colon ":" character.
        String[] hms = times[1].split(":");
        if( hms==null || hms.length!=3 ){
            throw new IllegalArgumentException("Incorrect format - hh:mm:ss part missing colons : [DDMM hh:mm:ss]. Was="+original);
        }

        try{

            String dd = times[0].substring(2);
            int day = Integer.parseInt(dd);
            String mm = times[0].substring(1,2);
            int month = Integer.parseInt(mm);

            int hour = Integer.parseInt( hms[0] );
            int min = Integer.parseInt( hms[1] );
            int sec = Integer.parseInt( hms[2] );

            Calendar c = Calendar.getInstance();
            c.set( Calendar.YEAR, 2007 );//ToDo: get the current year.
            c.set( Calendar.MONTH, month-1 ); //Not the -1 is because Java in zero indexed.
            c.set( Calendar.DATE, day );
            c.set( Calendar.HOUR, hour );
            c.set( Calendar.MINUTE, min );
            c.set( Calendar.SECOND, sec );

            long logTime = c.getTimeInMillis();

            return convertJavaTime2UnixTime( logTime );

        }catch(Throwable t){
            throw new IllegalArgumentException("Incorrect format - Should be : [DDMM hh:mm:ss]. Got error"+t);
        }
    }

    /**
     * Makes a name from the type and ip
     * @param type -
     * @param ip -
     * @return -
     */
    public static String createRrdNameBase(String type, String ip){
        ip = ip.trim().replace('.','-');
        ip = ip.replace(':','_');

        return (type.trim()+"_"+ip).toLowerCase();
    }//createRrdNameBase


    public static Date getYesterday()
    {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis( (System.currentTimeMillis() - (1000*60*60*24L)) );

        c.set(Calendar.HOUR,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);

        return c.getTime();
    }

}//class
