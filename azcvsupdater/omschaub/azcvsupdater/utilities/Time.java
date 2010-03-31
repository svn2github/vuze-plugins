/*
 * Created on Feb 23, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import omschaub.azcvsupdater.main.View;

public class Time {
    
    /**returns the current time formatted in 12 hr or 24 hr time
     * based on user preferences
     * @return String dateCurrentTime
     * @param boolean MilitarTime
     */
    
    public static String getCurrentTime(){
    	return View.formatDate(View.getPluginInterface().getUtilities().getCurrentSystemTime());
    }
    
    /**
     * 
     * @param boolean MilitaryTime
     * @return dateCurrentTime for cvs label
     */
    public static String getCVSTime(){
        String dateCurrentTime;
        Date when = new Date();
        when.getTime();
        
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa" );
        
        sdf.setTimeZone(TimeZone.getDefault());
        dateCurrentTime = sdf.format(when);
        return dateCurrentTime; 
    }
    
    
    /**time get for cvs timer
     * 
     * @param long AUTO_UPDATE_CHECK_PERIOD
     * @param boolean MilitaryTime
     * @return String datecurrentTime
     */
    public static String getCVSTimeNext(long AUTO_UPDATE_CHECK_PERIOD){
        String dateCurrentTime;
        Date when = new Date();
        Date when2 = new Date();
        when.getTime();
        when2.setTime(when.getTime() + AUTO_UPDATE_CHECK_PERIOD);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa" );
        sdf.setTimeZone(TimeZone.getDefault());
        //System.out.println(TimeZone.getDefault().toString());
        dateCurrentTime = sdf.format(when2);
        return dateCurrentTime; 
    }
}
