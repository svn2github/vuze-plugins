/*
 * File    : MDDownloadModel.java
 * Created : 29-Jan-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.ui.webplugin.remoteui.applet.model;

/**
 * @author parg
 *
 */


import javax.swing.table.*;

import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.*;

public class 
MDDownloadFullModel
	extends 	AbstractTableModel
	implements	MDDownloadModel
{
	public static String[]	column_names = { "#", "Name", "Size", "Downloaded", "Done", "State", "Seeds", "Peers", "Uploaded", "Up Ave", "Down Ave", "ETA", "Share Ratio" };
	
	protected DownloadManager	download_manager;
	protected Download[]		downloads;
	
	protected
	MDDownloadFullModel(
		DownloadManager		_download_manager )
	{
		download_manager	= _download_manager;
		
		loadData();
	}
	
	public Download[]
	getDownloads()
	{
		return( downloads );
	}
	
	public Download
	getDownload(
		int		row )
	{
		return( downloads[row]);
	}
	
	public Download[]
	getDownloads(
		int[]		rows )
	{
		Download[]	res = new Download[rows.length];
		
		for ( int i=0;i<res.length;i++){
			
			res[i] = downloads[rows[i]];
		}
		
		return( res );
	}
	
	public void
	refresh()
	{
		loadData();
		
		fireTableDataChanged();
	}
	
	protected void
	loadData()
	{
		downloads = download_manager.getDownloads();
	}
	
	public int 
	getColumnCount() 
	{ 
		return( column_names.length );
	}

	public int 
	getRowCount() 
	{ 
		return( downloads.length );
	}
	
	public Object 
	getValueAt(
		int row, 
		int col ) 
	{
		Download				download	= downloads[row];
		DownloadAnnounceResult	announce	= download.getLastAnnounceResult();
		DownloadScrapeResult	scrape		= download.getLastScrapeResult();
		
		Torrent	torrent = download.getTorrent();
		
		if ( col == 0 ){
			
			return( new Long(download.getPosition()));
			
		}else if ( col == 1 ){
				
			return( torrent==null?"**** Broken torrent ****":torrent.getName());
				
		}else if ( col == 2 ){
			
			return( new Long( torrent==null?-1:torrent.getSize()));
			
		}else if ( col == 3 ){
			
			return( new Long( download.getStats().getDownloaded()));
			
		}else if ( col == 4 ){
			
			return(new Integer( download.getStats().getCompleted()));
			
		}else if ( col == 5 ){
			
			return( (download.isForceStart()?"Forced ":"") + download.getStats().getStatus());
			
		}else if ( col == 6 ){
			
			return( announce.getSeedCount()+"("+(scrape.getSeedCount()==-1?0:scrape.getSeedCount())+")");
			
		}else if ( col == 7 ){
			
			return( announce.getNonSeedCount()+"("+(scrape.getNonSeedCount()==-1?0:scrape.getNonSeedCount())+")");
			
		}else if ( col == 8 ){
			
			return(new Long( download.getStats().getUploaded()));
			
		}else if ( col == 9 ){
			
			return(new Long( download.getStats().getUploadAverage()));
			
		}else if ( col == 10 ){
			
			return(new Long( download.getStats().getDownloadAverage()));
			
		}else if ( col == 11 ){
			
			return(new String( download.getStats().getETA()));
			
		}else if ( col == 12 ){
			
			int	sr = download.getStats().getShareRatio();
			if ( sr == -1 ){
				sr = 0x7fffffff;
			}
			return(new Integer( sr));
		}
		
		return( null );
	}

	public String 
	getColumnName(
		int column ) 
	{
		return(column_names[ column ]);
	}
	
	public Class 
	getColumnClass(
		int col ) 
	{
		return getValueAt(0,col).getClass();
	}
	
	public boolean 
	isCellEditable(
		int row, 
		int col )
	{
		return( false );
	}
	
	public void
	start(
		int[]		rows )
	
		throws DownloadException
	{
		if ( rows.length > 0 ){
			
			for (int i=0;i<rows.length;i++){
				
				downloads[rows[i]].restart();
			}
		}
	}
	
	public void
	forceStart(
		int[]		rows )
	{
		if ( rows.length > 0 ){
			
			for (int i=0;i<rows.length;i++){
				
				boolean	current = downloads[rows[i]].isForceStart();
					
				downloads[rows[i]].setForceStart( !current );
			}
		}
	}
	
	public void
	stop(
		int[]		rows )
	
		throws DownloadException
	{
		if ( rows.length > 0 ){
			
			for (int i=0;i<rows.length;i++){
			
				downloads[rows[i]].stop();
			}
		}
	}
	
	public void
	remove(
		int[]		rows )
	
		throws DownloadException, DownloadRemovalVetoException
	{
		if ( rows.length > 0 ){
			
			for (int i=0;i<rows.length;i++){
			
				downloads[rows[i]].remove();
			}
		}
	}
	
	public void
	moveUp(
		int[]		rows )
	{
		if ( rows.length > 0 ){
			
			int	current = downloads[rows[0]].getPosition();
							
			if ( current > 0 ){
				
				downloads[rows[0]].moveUp();
			}
		}
	}
	
	
	public void
	moveDown(
		int[]		rows )
	{
		if ( rows.length > 0 ){
			
			int	current = downloads[rows[0]].getPosition();
						
			if ( current < downloads.length ){
				
				downloads[rows[0]].moveDown();
			}
		}
	}
}
