/*
 * Created on Feb 25, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.jpc.cache;

import java.net.InetSocketAddress;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;

/**
 * Handles JPC cache server connections.
 */
public interface JPCCacheManager {
  
  
  /**
   * Attempt to locate and establish a connection with the local download cache server if one exists.
   * @param adapter for handling cache connection
   */
  public void connectLocalDownloadCache( JPCCacheDownloaderAdapter adapter );
  

  
  public void registerWithRemoteUploadCache( final InetSocketAddress address, Download download, Peer download_peer, int downloader_id );
  

  public void cancelRemoteUploadCacheRegistration( int downloader_id );
  
}
