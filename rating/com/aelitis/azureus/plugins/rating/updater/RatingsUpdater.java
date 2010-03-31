/*
 * Created on 11 mars 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.plugins.rating.updater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.utils.AggregatedList;
import org.gudy.azureus2.plugins.utils.AggregatedListAcceptor;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import com.aelitis.azureus.plugins.rating.RatingPlugin;

public class RatingsUpdater implements DownloadManagerListener,AggregatedListAcceptor {
  
  //1 min time out on ratings read
  private static final int READ_TIMEOUT = 60000;
  
  
  //1 hour update for ratings
  private static final int UPDATE_TIME  = 60 * 60 * 1000;
  UTTimer timer;
  
  
  List downloads;
  Map torrentRatings;
  
  RatingPlugin plugin;
  DistributedDatabase database;  
  
  TorrentAttribute attributeRating;
  TorrentAttribute attributeComment;
  TorrentAttribute attributeGlobalRating;
  
  private AggregatedList listToHandle;
  
  
  
  public RatingsUpdater(RatingPlugin plugin) {    
    this.plugin = plugin;
    this.downloads = new ArrayList();
    this.torrentRatings = new HashMap();
    
    listToHandle = plugin.getPluginInterface().getUtilities().createAggregatedList(this,20 * 1000,100);
    
    
    attributeRating  = this.plugin.getPluginInterface().getTorrentManager().getPluginAttribute("rating");
    attributeComment = this.plugin.getPluginInterface().getTorrentManager().getPluginAttribute("comment");    
    attributeGlobalRating  = this.plugin.getPluginInterface().getTorrentManager().getPluginAttribute("globalRating");
  }
  
  public void initialize() {
    this.plugin.getPluginInterface().getUtilities().createThread("Initializer", new Runnable() {
      public void run() {
       pInitialize(); 
      }
    });  
  }
  
  
  private void pInitialize() {
    PluginInterface pluginInterface = plugin.getPluginInterface();
        
    
    database = pluginInterface.getDistributedDatabase();
    if(database.isAvailable()) {
      //No need to add a listener if the DHT isn't avail
      pluginInterface.getDownloadManager().addListener(this);
         
      //Program the Timer update for updates every 1 hour
      timer = pluginInterface.getUtilities().createTimer("Ratings Update");
      timer.addPeriodicEvent(UPDATE_TIME,new UTTimerEventPerformer() {
        public void perform(UTTimerEvent event) {
          updateRatings();
        }
      });
    }
    
    
  }
  
  public void downloadAdded(Download download) {    
    if(download.getTorrent() != null) {
      final String torrentName = download.getTorrent().getName();
      plugin.logInfo(torrentName + " : downloaded added");
      downloads.add(download);
      listToHandle.add(download);
    }
  }
  
  public void downloadRemoved(Download download) {
    final String torrentName = download.getTorrent().getName();
    plugin.logInfo(torrentName + " : downloaded removed");
    downloads.remove(download);
    torrentRatings.remove(download);
  }
  
  public RatingResults getRatingsForDownload(Download d) {
    
    RatingResults result = null;
    synchronized (torrentRatings) {
      result = (RatingResults) torrentRatings.get(d);       
    }
    
    if(result != null) return result;
    result = new RatingResults();
    try {
      String str = d.getAttribute(attributeGlobalRating);
      if(str != null) {
        float f = Float.parseFloat(str);
        result.setAverage(f);
        return result;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return new RatingResults();
  }
  
  public void accept(final List l) {
    plugin.getPluginInterface().
    getUtilities().
    createThread("Ratings Updater", new Runnable() {
      public void run() {
        //Use a queue not to spam the DHT too fast,
        //ie process all updates one by one :
        List downloads = new LinkedList(l);
        updateRatings(downloads);
        
        downloads = new LinkedList(l);
        publishAllRatings(downloads);
      }        
    }); 
  }
  
  public void updateRatings() {       
    plugin.getPluginInterface().
      getUtilities().
      createThread("Ratings Updater", new Runnable() {
        public void run() {
          //Use a queue not to spam the DHT too fast,
          //ie process all updates one by one :
          List downloads = new LinkedList(RatingsUpdater.this.downloads);
          updateRatings(downloads);              
        }        
      });    
  }
  
  private void updateRatings(final List downloads) {
    if(downloads.size() == 0) return;
    final Download download = (Download) downloads.remove(0);   
    final String torrentName = download.getTorrent().getName();
    try {
      plugin.logInfo(torrentName + " : getting rating");
      DistributedDatabaseKey ddKey = database.createKey(KeyGenUtils.buildRatingKey(download),"Looking-up ratings for " + torrentName);
      database.read(new DistributedDatabaseListener() {
       
        List results = new ArrayList();
       
        public void event(DistributedDatabaseEvent event) {
          
          if(event.getType() == DistributedDatabaseEvent.ET_VALUE_READ) {
            results.add(event.getValue());
          }
         
          if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE 
               || event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT) {
            plugin.logInfo(torrentName + " : Rating received");
            RatingResults ratings = new RatingResults();
            
            for(int i = 0 ; i < results.size() ; i++) {
              
              DistributedDatabaseValue value = (DistributedDatabaseValue) results.get(i);
              
              try {
                
                byte[] bValue = (byte[]) value.getValue(byte[].class);
                
                RatingData data = new RatingData(bValue);
                
                plugin.logInfo("        " + data.getScore() + ", " + value.getContact().getName() + ", " +  data.getNick() + " : " + data.getComment());
                
                ratings.addRating(data);
                
              } catch(Exception e) {
                
                e.printStackTrace();
                
              }                            
            }
            
            synchronized (torrentRatings) {
              torrentRatings.put(download,ratings);
              download.setAttribute(attributeGlobalRating,"" + ratings.getRealAverageScore());
            }
            
            updateRatings(downloads);
          }
        }
      },ddKey,READ_TIMEOUT);      
    } catch(Exception e) {
      e.printStackTrace();
      updateRatings(downloads);
    }
  }
  
  public RatingData loadRatingsFromDownload(Download download) {
   String strRating = download.getAttribute(attributeRating);
   String comment   = download.getAttribute(attributeComment);
   int rating = 0;
   if(strRating != null) {
     try {
       rating = Integer.parseInt(strRating);
     } catch(Exception e) {
       e.printStackTrace();
     }
   }
   if(comment == null) comment = "";
   String nick = plugin.getNick();
   return new RatingData(rating,nick,comment);   
  }
  
  /*
   * Stores and updates the DB if needed
   */
  public void storeRatingsToDownload(Download download,RatingData data) {
    RatingData oldData = loadRatingsFromDownload(download);
    boolean updateNeeded = false;
    updateNeeded |= oldData.getScore() != data.getScore();
    updateNeeded |= ! oldData.getComment().equals(data.getComment());
    

    // without considering the nick, if we need to update, then
    // it's locally
    if(updateNeeded) {
      download.setAttribute(attributeRating,"" + data.getScore());
      download.setAttribute(attributeComment, data.getComment());
    }
    
    updateNeeded |= ! oldData.getNick().equals(data.getNick());
    //Now, with the nick, it's remotely
    if(updateNeeded && database != null && database.isAvailable()) {
      publishDownloadRating(download,data);
    }
  }
  
  private void publishDownloadRating(Download download) {
    publishDownloadRating(download,loadRatingsFromDownload(download));
  }
  
  private void publishDownloadRating(Download download,CompletionListener listener) {
    publishDownloadRating(download,loadRatingsFromDownload(download),listener);
  }

  private void publishDownloadRating(Download download,RatingData data) {
    publishDownloadRating(download,data,new CompletionAdapter());
  }
  
  private void publishDownloadRating(      
      final Download download,
      final RatingData data,
      final CompletionListener listener) {
    
    int score = data.getScore();
    //Non scored torrent aren't published,
    //Comments require score
    if(score == 0) return;
    
    final String torrentName = download.getTorrent().getName();
    
    byte[] value = data.encodes();
    try {
      plugin.logInfo(torrentName + " : publishing rating");
      DistributedDatabaseKey ddKey = database.createKey(KeyGenUtils.buildRatingKey(download),"Registering ratings for " + torrentName);
      DistributedDatabaseValue ddValue = database.createValue(value);
      database.write(new DistributedDatabaseListener() {
        public void event(DistributedDatabaseEvent event) {
          if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
            plugin.logInfo(torrentName + " : rating registeration successfull");
            listener.operationComplete();
            //In case of success, update the rating for that download
            //(as our rating might impact the global rating)
            List dls = new ArrayList();
            dls.add(download);
            updateRatings(dls);
          }
          if(event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT) {
            plugin.logInfo(torrentName + " : rating registeration failed");
            listener.operationComplete();
          }
        }
      },ddKey,ddValue);
    } catch(Exception e) {
      e.printStackTrace();
    }
  } 
  
  private void publishAllRatings() {
    //  Use a queue not to spam the DHT too fast,
    //ie process all updates one by one :
    List downloads = new LinkedList(this.downloads);
    publishAllRatings(downloads);  
  }
  
  private void publishAllRatings(final List downloads) {
    if(downloads.size() == 0) return;
    final Download download = (Download) downloads.remove(0);    
    publishDownloadRating(download,new CompletionListener() {
      public void operationComplete() {
        publishAllRatings(downloads);
      }
    });
  }

}
