/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 
package com.aelitis.azureus.plugins.xmwebui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.*;

/**
 * @author TuxPaper
 * @created Feb 18, 2016
 *
 */
public class TorrentBlank
	implements Torrent
{

	private Download d;

	/**
	 * 
	 */
	public TorrentBlank(Download d) {
		this.d = d;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getName()
	public String getName() {
		return d.getName();
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getAnnounceURL()
	public URL getAnnounceURL() {
		try {
			return new URL("http://invalid.torrent");
		} catch (MalformedURLException e) {
		}
		return null;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setAnnounceURL(java.net.URL)
	public void setAnnounceURL(URL url) {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getAnnounceURLList()
	public TorrentAnnounceURLList getAnnounceURLList() {
		return null;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getHash()
	public byte[] getHash() {
		byte[] b = new byte[32];
		return b;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getSize()
	public long getSize() {
		return 0;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getComment()
	public String getComment() {
		return "";
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setComment(java.lang.String)
	public void setComment(String comment) {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getCreationDate()
	public long getCreationDate() {
		return 0;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getCreatedBy()
	public String getCreatedBy() {
		return "";
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getPieceSize()
	public long getPieceSize() {
		return 0;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getPieceCount()
	public long getPieceCount() {
		return 0;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getPieces()
	public byte[][] getPieces() {
		return new byte[0][0];
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getFiles()
	public TorrentFile[] getFiles() {
		return new TorrentFile[0];
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getEncoding()
	public String getEncoding() {
		return "utf8";
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setEncoding(java.lang.String)
	public void setEncoding(String encoding)
			throws TorrentEncodingException {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setDefaultEncoding()
	public void setDefaultEncoding()
			throws TorrentEncodingException {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getAdditionalProperty(java.lang.String)
	public Object getAdditionalProperty(String name) {
		return null;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#removeAdditionalProperties()
	public Torrent removeAdditionalProperties() {
		return this;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setPluginStringProperty(java.lang.String, java.lang.String)
	public void setPluginStringProperty(String name, String value) {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getPluginStringProperty(java.lang.String)
	public String getPluginStringProperty(String name) {
		return "";
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setMapProperty(java.lang.String, java.util.Map)
	public void setMapProperty(String name, Map value) {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getMapProperty(java.lang.String)
	public Map getMapProperty(String name) {
		return new HashMap();
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#isDecentralised()
	public boolean isDecentralised() {
		return false;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#isDecentralisedBackupEnabled()
	public boolean isDecentralisedBackupEnabled() {
		return false;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setDecentralisedBackupRequested(boolean)
	public void setDecentralisedBackupRequested(boolean requested) {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#isDecentralisedBackupRequested()
	public boolean isDecentralisedBackupRequested() {
		return false;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#isPrivate()
	public boolean isPrivate() {
		return false;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setPrivate(boolean)
	public void setPrivate(boolean priv) {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#wasCreatedByUs()
	public boolean wasCreatedByUs() {
		return false;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getMagnetURI()
	public URL getMagnetURI()
			throws TorrentException {
		try {
			return new URL("http://invalid.torrent");
		} catch (MalformedURLException e) {
			throw new TorrentException(e);
		}
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#writeToMap()
	public Map writeToMap()
			throws TorrentException {
		return new HashMap();
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#writeToFile(java.io.File)
	public void writeToFile(File file)
			throws TorrentException {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#writeToBEncodedData()
	public byte[] writeToBEncodedData()
			throws TorrentException {
		return new byte[0];
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#save()
	public void save()
			throws TorrentException {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#setComplete(java.io.File)
	public void setComplete(File data_dir)
			throws TorrentException {
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#isComplete()
	public boolean isComplete() {
		return false;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#isSimpleTorrent()
	public boolean isSimpleTorrent() {
		return false;
	}

	// @see org.gudy.azureus2.plugins.torrent.Torrent#getClone()
	public Torrent getClone()
			throws TorrentException {
		return this;
	}

}
