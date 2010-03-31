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

package com.azureus.plugins.azemp;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.azureus.plugins.azemp.media.MediaAnalyser;
import com.azureus.plugins.azemp.media.MediaInfo;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadCompletionListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.installer.FilePluginInstaller;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

/**
 * @author TuxPaper
 * @created Sep 4, 2007
 *
 */
public class EmbeddedMediaPlayerPlugin
	implements UnloadablePlugin
{
	public static EmbeddedMediaPlayerPlugin instance = null;
	
	public static final String ID = "azemp";

	public static final String MSG_PREFIX = "v3.emp";

	public static final String CFG_VIDEO_OUT = "video.out";

	// In percent
	private static final int fileSizeThreshold = 90;
	private static final String playableFileExtensions = ".mpg .avi .flv .flc .mp4 .mpeg .divx .h264 .mkv .wmv .mov .mp2 .m2v .m4v";
	
	private LoggerChannel logger;

	private UIManager uiManager;

	private BasicPluginViewModel viewModel;

	private BasicPluginConfigModel configModel;
	
	private static List externalPlayers = new ArrayList();
	
	private final static String playabilityCheckedAttributeName = "playabilityChecked";
	private final static String playableAttributeName = "playableIdx";
	private  final static String playableWithAttributeName = "playableWith";
	
	public static TorrentAttribute playabilityCheckedAttribute;
	public static TorrentAttribute playableAttribute;
	public static TorrentAttribute playableWithAttribute;
	String playabilityCheckSignature = "0";
	
	private static Map mapTorrentPlayers = new HashMap();

	private static PluginInterface pluginInterface;
	

	public void unload()
			throws PluginException {
		EmbeddedPlayerWindowManager.killAll();
		if (viewModel != null) {
			viewModel.destroy();
		}
		if (configModel != null) {
			configModel.destroy();
		}
	}

	public void initialize(PluginInterface pluginInterface)
			throws PluginException {
		instance = this;
		this.pluginInterface = pluginInterface;
		EmbeddedPlayerWindowManager.setPluginInterface(pluginInterface);
		EmbeddedPlayerWindowManager.setPluginDir(pluginInterface.getPluginDirectoryName());

		if (pluginInterface.getUtilities().isOSX()) {
			try {
				String binaryPath = pluginInterface.getPluginDirectoryName();
				String newLibPath = binaryPath + File.pathSeparator
						+ System.getProperty("java.library.path");

				System.out.println(newLibPath);

				System.setProperty("java.library.path", newLibPath);
				Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");

				fieldSysPath.setAccessible(true);

				if (fieldSysPath != null) {
					fieldSysPath.set(System.class.getClassLoader(), null);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			Properties props = pluginInterface.getPluginProperties();
			props.setProperty("plugin.unload.disabled", "true");
		}

		logger = pluginInterface.getLogger().getTimeStampedChannel(ID);

		EmbeddedPlayerWindowManager.setLogger(logger);

		uiManager = pluginInterface.getUIManager();

		viewModel = uiManager.createBasicPluginViewModel("azemp.name");

		viewModel.getActivity().setVisible(false);
		viewModel.getProgress().setVisible(false);

		logger.addListener(new LoggerChannelListener() {
			public void messageLogged(int type, String content) {
				viewModel.getLogArea().appendText(content + "\n");
			}

			public void messageLogged(String str, Throwable error) {
				if (str.length() > 0) {
					viewModel.getLogArea().appendText(str + "\n");
				}

				StringWriter sw = new StringWriter();

				PrintWriter pw = new PrintWriter(sw);

				error.printStackTrace(pw);

				pw.flush();

				viewModel.getLogArea().appendText(sw.toString() + "\n");
			}
		});

		uiManager.addUIListener(new UIManagerListener() {
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {

					uiManager.removeUIListener(this);

					logger.log("SWT user interface bound");

					configModel = uiManager.createBasicPluginConfigModel(ID);
					configModel.addStringListParameter2(CFG_VIDEO_OUT, MSG_PREFIX
							+ ".cfg.video.out", new String[] {
						"default",
						"directx",
						"gl2",
						"swt",
						"direct3d"
					}, "default");
				}
			}

			public void UIDetached(UIInstance instance) {

			}
		});
		
		writeRegistry(pluginInterface);
		
		
		playabilityCheckedAttribute = pluginInterface.getTorrentManager().getPluginAttribute(playabilityCheckedAttributeName);
		playableAttribute = pluginInterface.getTorrentManager().getPluginAttribute(playableAttributeName);
		playableWithAttribute = pluginInterface.getTorrentManager().getPluginAttribute(playableWithAttributeName);
		
		final DownloadCompletionListener completionListener = new DownloadCompletionListener() {
			public void onCompletion(Download d) {
				updateDownloadInfo(d);
			}
		};
		
		final DownloadManagerListener downloadManagerListener =  new DownloadManagerListener() {
			
			public void downloadAdded(Download download) {
				download.addCompletionListener(completionListener);
			}
			
			public void downloadRemoved(Download download) {
				mapTorrentPlayers.remove(download);
			}
			
			
		};
		pluginInterface.getDownloadManager().addListener(downloadManagerListener);
	}
	
	public void registerExternalPlayer(String player) {
		synchronized(externalPlayers) {
			externalPlayers.add(player);
			StringBuffer signature = new StringBuffer();
			for(int i = 0 ; i < externalPlayers.size() ; i++) {
				signature.append(externalPlayers.get(i));
				signature.append("\n");
			}
			playabilityCheckSignature = "" + signature.toString().hashCode();
		}
	}
	
	public void unRegisterExternalPlayer(String player) {
		synchronized(externalPlayers) {
			externalPlayers.remove(player);
		}
	}
	
	public void updateDownloadInfo(Download download) {
		Torrent torrent = download.getTorrent();
		if (torrent == null) {
			return;
		}

		org.gudy.azureus2.core3.download.DownloadManager dm = PluginCoreUtils.unwrap(download);
		if (!dm.getAssumedComplete()) {
			return;
		}
		//Do not check if it's a platform torrent
		TOTorrent totorrent = dm.getTorrent();
//		if(PlatformTorrentUtils.useEMP(totorrent)) {
//			download.setAttribute(playabilityCheckedAttribute, playabilityCheckSignature);
//			download.setIntAttribute(playableAttribute, 0);
//			if (externalPlayers.size() > 0) {
//				download.setAttribute(playableWithAttribute, (String) externalPlayers.get(0) );
//			}
//			return;
//		}
		
		//System.out.println("Download Complete : " + download.getName());
		String playabilityChecked = download.getAttribute(playabilityCheckedAttribute);
		//System.out.println("Playabilitycheck : " + playabilityChecked);
		boolean validCheck = false;
		if(playabilityChecked != null && playabilityChecked.equals(playabilityCheckSignature)) {
			validCheck = true;
		}
		
		//UMPDEBUG : Remove comment to force re-check (slows down the UI quite a bit)
		//validCheck = false;
		
		if (!validCheck && externalPlayers.size() > 0) {
			//System.out.println("Not a valid check, checking");
			//Let's check it
			synchronized (externalPlayers) {
				int playableIndex = -1;
				String playableWith = null;
				for (int i = 0; i < externalPlayers.size(); i++) {
					//System.out.println("Checking with " + (String)externalPlayers.get(i));
					MediaAnalyser analyser = new MediaAnalyser(
							(String) externalPlayers.get(i));
					DiskManagerFileInfo fileInfos[] = download.getDiskManagerFileInfo();

					
					for (int j = 0; j < fileInfos.length; j++) {
						DiskManagerFileInfo fileInfo = fileInfos[j];

						if (fileInfo.getLength() > torrent.getSize() * fileSizeThreshold / 100l) {
							String fileToAnalyse = fileInfos[j].getFile().getAbsolutePath();
							
							int extIndex = fileToAnalyse.lastIndexOf(".");
							if(extIndex > -1) {
								
								
								String ext = fileToAnalyse.substring(extIndex);
								
								if(ext != null) {
									ext  = ext.toLowerCase();
									//Only check safe media files (avoids checking everything in the library)
									if(playableFileExtensions.indexOf(ext) > -1) {
//										MediaInfo info = analyser.analyse(fileToAnalyse, null);
//										if (info != null && info.isPlayable()) {
											playableIndex = j;
											download.setIntAttribute(playableAttribute, playableIndex);
											playableWith = (String) externalPlayers.get(i);
//										}
									}
								}
								
								
							}
							
							
							// Assuming threshold is > 50, there can be no other file
							// that matches the threshold, so it's safe to break
							if (fileSizeThreshold > 50) {
								break;
							}
						}
					}

				}
				
				download.setAttribute(playabilityCheckedAttribute,
						playabilityCheckSignature);
				download.setIntAttribute(playableAttribute, playableIndex);
				if (playableIndex >= 0) {
					download.setAttribute(playableWithAttribute, playableWith);
				}

				mapTorrentPlayers.put(totorrent, download);
			}
		}
	}
	
	public static boolean isExternallyPlayabale(TOTorrent torrent) {
		Download dl = (Download) mapTorrentPlayers.get(torrent);

		if (dl != null) {
			return isExternallyPlayable(dl);
		}
		
		try {
			Download download = pluginInterface.getDownloadManager().getDownload(torrent.getHash());
			if (download != null) {
				mapTorrentPlayers.put(torrent, download);
				return isExternallyPlayable(download);
			}
		} catch (Exception e) {
		}
		
		return false;
	}
	
	public static boolean isExternallyPlayable(Download d) {
		if (!d.isComplete()) {
			return false;
		}
		boolean result = false;
		instance.updateDownloadInfo(d);
		if(isExternalPlayerInstalled()) {
			if (! d.hasAttribute(playableAttribute)) {
				if(instance != null) {
					instance.updateDownloadInfo(d);
				}
			}
			if (d.hasAttribute(playableAttribute)) {
		  		int playableIdx = d.getIntAttribute(playableAttribute);
		  		if(playableIdx >= 0) {
		  			result = true;
		  		}	else if(PlatformTorrentUtils.useEMP(PluginCoreUtils.unwrap(d.getTorrent()))) {
		  			d.setIntAttribute(playableAttribute, 0);
		  			d.setAttribute(playableWithAttribute, (String) externalPlayers.get(0) );
		  			result = true;
		  		}

			}
		} else {
			long size = d.getTorrent().getSize();
			DiskManagerFileInfo[] infos = d.getDiskManagerFileInfo();
			for(int i = 0; i < infos.length ; i++) {
				if(infos[i].getLength() > (long)fileSizeThreshold * size / 100l) {
					String name = infos[i].getFile().getName();
					int extIndex = name.lastIndexOf(".");
					if(extIndex > -1) {
						String ext = name.substring(extIndex);
						if(playableFileExtensions.indexOf(ext) > -1) {
							return true;
						}
					}
					if (fileSizeThreshold > 50) {
						break;
					}
				}
			}
			return false;
		}
		//System.out.println("Checking playability of " + d.getName() + " > " + playable + " : " +  result);
		return result;
	}
	
	public static boolean isExternalPlayerInstalled() {
		if(pluginInterface != null) {
			return pluginInterface.getPluginManager().getPluginInterfaceByID("azump") != null;
		}
		return externalPlayers != null && externalPlayers.size() > 0;
	}
	
	
	public void cancelInstallExternalPlayer() {
		if(rd != null) {
			try {
				rd.cancel();
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public synchronized void installExternalPlayer() {
		installExternalPlayer(null);
	}
	
	
	ResourceDownloader rd;
	public synchronized void installExternalPlayer(final InstallationListener listener) {
		if(!isExternalPlayerInstalled()) {
			
			String pluginUrl = null;
			if(Constants.isOSX) {
				pluginUrl = "http://cache2.vuze.com/plugins/azump_osx.zip";
			}
			if(Constants.isWindows) {
				pluginUrl = "http://cache2.vuze.com/plugins/azump_win32.zip";
			}
			if(pluginUrl != null) {
				try {
					ResourceDownloaderFactory factory = pluginInterface.getUtilities().getResourceDownloaderFactory();
					rd = factory.create(new URL(pluginUrl));
					rd.addListener(new ResourceDownloaderListener() {
						public boolean completed(ResourceDownloader downloader,
								InputStream data) {
							try {
								File dir = AETemporaryFileHandler.createTempDir();
								File f = new File(dir,"azump.zip");  
								FileUtil.copyFile(data, f);
								FilePluginInstaller fpi = pluginInterface.getPluginManager().getPluginInstaller().installFromFile(f);
								fpi.install(false,true,true);
								if(listener != null) {
									listener.reportComplete();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							return true;
						}
						
						public void failed(ResourceDownloader downloader,
								ResourceDownloaderException e) {
							if(listener != null) {
								listener.reportError();
							}
						}
						
						public void reportActivity(
								ResourceDownloader downloader, String activity) {}
						
						public void reportAmountComplete(
								ResourceDownloader downloader, long amount) {}
						
						public void reportPercentComplete(
								ResourceDownloader downloader, int percentage) {
							if(listener != null) {
								listener.reportPercentDone(percentage);
							}
						}
					});
				rd.download();
				} catch (Exception e) {
					//likely canceled
					if(listener != null) {
						listener.reportError();
					}
				}
			}
		}
			
	}
	
	/**
	 * Returns index of item to play externally.  -1 if none or vuze content
	 * 
	 * @param hash
	 * @return
	 *
	 * @since 3.1.0.1
	 */
	public static int getExternallyPlayableIndex(byte[] hash) {
		try {
			Download download = pluginInterface.getDownloadManager().getDownload(hash);
			if (download != null) {
				if (!download.hasAttribute(playableAttribute)) {
					return -1;
				}
				return download.getIntAttribute(playableAttribute);
			}
		} catch (Exception e) {
		}
		return -1;
	}

	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	private void writeRegistry(PluginInterface pi) {
		if (!Constants.isWindows) {
			return;
		}
		try {
			//		AEWin32Access accessor = AEWin32Manager.getAccessor(true);
			Class cWin32 = Class.forName("org.gudy.azureus2.platform.win32.access.AEWin32Manager");
			if (cWin32 == null) {
				return;
			}

			Method mGetAccessor = cWin32.getMethod("getAccessor", new Class[] {
				boolean.class
			});
			if (mGetAccessor == null) {
				return;
			}

			Object cAccessor = mGetAccessor.invoke(null, new Object[] {
				new Boolean(false)
			});
			if (cAccessor == null) {
				return;
			}

			Method mWriteStringValue = cAccessor.getClass().getMethod(
					"writeStringValue", new Class[] {
						int.class,
						String.class,
						String.class,
						String.class
					});
			if (mWriteStringValue == null) {
				return;
			}

			// 4 == HKCU
			mWriteStringValue.invoke(cAccessor, new Object[] {
				new Integer(4),
				"SOFTWARE\\Azureus",
				"azmplay",
				new File(pi.getPluginDirectoryName(), "azmplay.exe").getAbsolutePath()
			});

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
}
