package com.azureus.plugins.aznetmon.pcap;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManagerListener;
import org.gudy.azureus2.plugins.download.*;

/**
 * Created on Mar 14, 2008
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


/**
 * This thread starts the DownloadManager and PeerManager monitors.
 *
 */
public class AllPeersMonitorRunnable implements Runnable
{

	private final PluginInterface pluginInterface;


	public AllPeersMonitorRunnable(PluginInterface _pluginInterface)
	{
		pluginInterface = _pluginInterface;
	}

	public void run(){

		DownloadManager dm = pluginInterface.getDownloadManager();

		dm.addListener( new DownloadManagerListener(){
			public void downloadAdded(Download download) {

				final PeersStateMonitor psm = PeersStateMonitor.getInstance();

				download.addPeerListener( new DownloadPeerListener(){
					public void peerManagerAdded(Download download, PeerManager peerManager) {


						peerManager.addListener( new PeerManagerListener(){

							/**
							 * Peer added.
							 * @param peerManager -
							 * @param peer -
							 */
							public void peerAdded(PeerManager peerManager, Peer peer) {

								psm.peerAddEvent(peer);

							}

							/**
							 * Peer removed.
							 * @param peerManager -
							 * @param peer -
							 */
							public void peerRemoved(PeerManager peerManager, Peer peer) {

								psm.peerCloseEvent(peer);

							}

						} );

					}//peerManagerAdded

					public void peerManagerRemoved(Download download, PeerManager peerManager) {
						
					}//peerManagerRemoved
				} );

			}//downloadAdded

			public void downloadRemoved(Download download) {
				//assumption here is things will get garbage collected when refereces to the disappear.
			}//downloadRemoved

		}  );

	}//run

}//class
