/**
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

package com.azureus.plugins.azemp.ui.swt.emp;

import java.util.ArrayList;
import java.util.HashMap;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 * Stats and State info while EMP is running
 * 
 * @author TuxPaper
 * @created Oct 25, 2007
 *
 */
public class EmpStats
{
	private long rebufferMS;

	long initialBufferReadyOn;

	long startedOn;

	long dmStartedOn;

	private int numHardRebuffers;

	boolean pp;

	long got1stPeerOn;

	long got1stConnectedPeerOn;

	long got1stHandshakedPeerOn;

	long got1stDataOn;

	long minDLRateSinceRebuffer = -1;

	long maxDLRateSinceRebuffer = -1;

	long dlRateAtPlay = -1;
	
	long forcedWaitTime = -1;

	long lastForcedWaitTime = -1;

	ArrayList rebufferList = new ArrayList();

	HashMap lastInfo = null;

	long metStreamSpeedAt = -1;

	long met50StreamSpeedAt = -1;

	long met25StreamSpeedAt = -1;

	long met75StreamSpeedAt = -1;
	
	long streamableUnbufferedOn = -1;
	
	long startedTrackerAnnounceOn = -1;

	long completedTrackerAnnounceOn = -1;

	public boolean didWait;
	
	boolean isDownloadStreaming = false;
	
	boolean useWMP = false;
	
	boolean useExternalPlayer = false;
	
	String externalPlayer;
	
	int playIndex;
	
	long streamSpeed = 0;
	
	boolean usedFullScreen = false;

	/** 
	 * Used to prevent seeking beyond the max position the user has viewed so far
	 */
	float maxSeekAheadSecs = -1;
	
	float totalSecs = 0;

	public long numOtherTorrentsActive = -1;

	public long playBeforePageLoadedOn = -1;

	public long forceWaitClearedOn = -1;

	public long startCalledOn = -1;
	
	/**
	 * Flag to indicate we are required to wait before playing.  This means
	 * a pre playback page will load (usually with an ETA on it).  If
	 * we timeout or get an incorrect page, this flag will be flipped off.
	 * The webpage can also turn this flag off (when the ETA is 0)
	 */
	public boolean playWait;

	/** 
	 * Indicates whether the playWait change was from due to a bad thing, such
	 */
	public String playWaitAbortedReason;


	public EmpStats() {
		startedOn = SystemTime.getCurrentTime();
	}

	public void increaseRebufferCount(DownloadManager dm, boolean hardRebuffer,
			float positionSecs, long streamPos, long availPos, int bufferSize,
			long minDlRate, long maxDlRate) {

		DownloadManagerStats dmStats = dm.getStats();

		if (hardRebuffer) {
			numHardRebuffers++;
		}
		HashMap info = new HashMap();
		if (positionSecs > 0) {
			info.put("position-secs", new Double(positionSecs));
		}
		if (streamPos > 0) {
			info.put("streampos-bytes", new Long(streamPos));
		}
		if (availPos > 0) {
			info.put("availpos-bytes", new Long(availPos));
		}
		info.put("buffer-size-bytes", new Long(bufferSize));
		info.put("is-hardrebuffer", new Boolean(hardRebuffer));

		long when = SystemTime.getCurrentTime();
		long dlBytes = dmStats.getTotalGoodDataBytesReceived();

		info.put("when", new Long(when));
		info.put("dl-bytes", new Long(dlBytes));

		long deltaMS;
		long deltaStreamBytes;
		long deltaAvailBytes;
		long deltaDlBytes;
		if (lastInfo != null) {
			deltaMS = when - ((Long) lastInfo.get("when")).longValue();

			Long lastStreamPos = (Long) lastInfo.get("streampos-bytes");
			deltaStreamBytes = lastStreamPos == null ? streamPos : streamPos
					- lastStreamPos.longValue();

			Long lastAvailPos = (Long) lastInfo.get("availpos-bytes");
			deltaAvailBytes = lastAvailPos == null ? availPos : availPos
					- lastAvailPos.longValue();

			deltaDlBytes = dlBytes - ((Long) lastInfo.get("dl-bytes")).longValue();
		} else {
			deltaMS = when - dm.getStats().getTimeStarted();
			deltaAvailBytes = availPos;
			deltaDlBytes = dlBytes;
			deltaStreamBytes = streamPos;
		}

		info.put("diff-ms", new Long(deltaMS));
		info.put("diff-stream-bytes", new Long(deltaStreamBytes));
		info.put("diff-avail-bytes", new Long(deltaAvailBytes));
		info.put("diff-dl-bytes", new Long(deltaDlBytes));

		if (minDlRate >= 0) {
			info.put("min-dl-Bps", new Long(minDlRate));
		}
		if (maxDlRate >= 0) {
			info.put("max-dl-Bps", new Long(maxDlRate));
		}

		info.put("dl-Bps", new Long(dmStats.getDataReceiveRate()));
		info.put("ul-Bps", new Long(dmStats.getDataSendRate()));

		lastInfo = info;
		rebufferList.add(info);
	}

	public void increaseRebufferMS(long ms) {
		rebufferMS += ms;
		if (lastInfo != null) {
			lastInfo.put("wait-time-ms", new Long(ms));
		}
	}

	public long getNumRebuffers() {
		return rebufferList.size();
	}

	public long getNumHardRebuffers() {
		return numHardRebuffers;
	}

	public long getRebufferMS() {
		return rebufferMS;
	}
}
