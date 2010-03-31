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

import java.io.File;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadWillBeRemovedListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.tracker.server.impl.dht.TRTrackerServerDHT;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThreadAlreadyInstanciatedException;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManager;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.messenger.config.PlatformTorrentMessenger;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.FakeTableCell;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.PlayUtils;
import com.azureus.plugins.azemp.*;

/**
 * @author TuxPaper
 * @created Aug 6, 2007
 *
 *	TODO: Put all on new thread, call SWT thread when needed
 */
public class EmbeddedPlayerWindowSWT
	implements EmbeddedPlayerWindow,
	EmpListenerFileChanged, EmpListenerAudio, EmpListenerTimeout,
	EmpListenerFullScreen, GlobalManagerDownloadWillBeRemovedListener,
	DownloadManagerTrackerListener
{
	protected static final boolean DEBUG = true;

	protected static final boolean DEBUG_TO_STDOUT = true;

	private static final String PATH_SKIN_DEFS = "com/azureus/plugins/azemp/ui/skin/";

	private static final String FILE_SKIN_DEFS = "skin3_emp_window.properties";

	private static final int MIN_PLAYING_AUTO_WIDTH = 420;

	private static final int MIN_PLAYING_AUTO_HEIGHT = 300;

	private static final boolean ONLY_ONE = true;

	private static EmbeddedPlayerWindowSWT alreadyRunningWin = null;

	private static AEMonitor alreadyRunningWin_mon = new AEMonitor(
			"alreadyRunningWin");

	protected static final int STATE_PLAYING = 0;

	protected static final int STATE_STOPPED = 1;

	protected static final int STATE_PAUSED = 2;

	protected static final int STATE_BUFFERING = 3;

	protected static final int STATE_UNINITED = 4;

	private static final String[] STATE_STRINGS = {
		"Play",
		"Stop",
		"Pause",
		"Buffer",
		"UNINITIALIZED"
	};

	private static final boolean RESIZE_ON_PLAY = false;

	private static final long TIMEOUT_PREPLAYBACK_LOADING = 15 * 1000;

	private static final long TIMEOUT_PREPLAYBACK_FULLYLOADED = 75 * 1000;

	private static final Long LOG_VERSION = new Long(13);

	public String URL_PLAYAFTER;

	public String URL_PLAYBEFORE;

	private int MIN_BUFFER_SIZE = 1024 * 128; // 128k

	private int MIN_REBUFFER_SIZE = 1024 * 256; // 256k

	private int bufferSize = MIN_BUFFER_SIZE;

	private int rebufferSize = MIN_REBUFFER_SIZE;

	private int currentBufferSize;

	private static long ETA_SOON_UNTIL = 15000;

	private TOTorrent torrent;

	private String runFile;

	protected SWTSkin skin;

	protected Shell shell;

	protected EmbeddedMediaPlayer emp = null;

	protected SWTSkinButtonUtility btnPause;

	private SWTSkinObjectText soTimeText;

	private boolean fullscreen = false;

	private SWTSkinObjectBrowser soPlayAfterBrowser;

	private BrowserHandlerSWT bhAfter;

	private SWTSkinObjectBrowser soPlayBeforeBrowser;

	private BrowserHandlerSWT bhBefore;

	private int state;

	private TimerEventPeriodic timePollerTimer;

	private String platformTorrentHash;

	private SWTSkinObjectSlider soTimeSlider;

	private FakeTableCell rateCell;

	private EnhancedDownloadManager edm;

	private String contentTitle = "";

	private ArrayList buttonsToDisable = new ArrayList();

	private ArrayList adbuttonsToEnable = new ArrayList();

	private DiskManagerFileInfo primaryFileInfo = null;

	private long primaryFileLength;

	private int currentButtonState;

	private SWTSkinObjectSlider soVolumeSlider;

	private String nowPlayingFile;

	/** 
	 * Used to store the ETA/wait for time before we can initially play.
	 * 0 when we aren't waiting 
	 */
	private long waitFor;

	private SWTSkinObject soMute;

	private SWTSkinButtonUtility btnMute;

	static LoggerChannel logger;

	private DownloadManager dm;

	protected SWTSkinButtonUtility btnFullScreen;

	private boolean isCurrentIncompleteStream;

	private File primaryFile;

	private long rebufferStartTime;

	long lastETA;

	boolean needsToBuffer;

	boolean nowPaused;

	float positionSecs;

	protected Composite cPlayer;

	protected boolean fastRolling;

	private DownloadManagerListener dmListener;

	private ArrayList dlAvgList;

	EmpStats stats;

	private AEMonitor mon_stats = new AEMonitor("empstats");

	private SWTSkinButtonUtility btnFlipToolbar;

	private TimerEvent playEnablerTimer;

	protected SWTSkinObjectText soWaitText;

	private boolean updatingDLStats;

	private int lastCursorPos = SWT.DEFAULT;

	private Point lastCursorPt = new Point(0, 0);

	private SWTSkinObject soTop;

	private SWTSkinObject soBottom;

	private Composite cRate;

	private boolean USE_SWT_FULLSCREEN;

	private Listener keyListener;

	private int[] videoResolution;

	private boolean initSizeSet;

	private SWTSkinButtonUtility btnNext;

	private SWTSkinButtonUtility btnPrev;

	private boolean lastMute;

	private boolean isOurContent;

	private byte[] hash;

	private boolean isCVS;

	private ContentNetwork contentNetwork;

	/////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Display d = new Display();
		AzureusCore core = AzureusCoreFactory.create();

		MessageText.integratePluginMessages(
				"com/azureus/plugins/azemp/ui/internat/Messages",
				EmbeddedPlayerWindowSWT.class.getClassLoader());
		try {
			SWTThread.createInstance(null);
		} catch (SWTThreadAlreadyInstanciatedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ShellFactory.createMainShell(SWT.SHELL_TRIM);
		//DownloadManagerEnhancer.initialise(core);

		TOTorrent torrent = null;
		try {
			torrent = TorrentUtils.readFromFile(new File("C:\\h264\\1k.torrent"),
					false);
		} catch (final TOTorrentException e) {

		}

		try {
			String file = "C:\\h264\\main.mkv";
			//String file = "test.asx";

			EmbeddedPlayerWindowSWT window = EmbeddedPlayerWindowSWT.openWindow(null,
					torrent, file);
			window.waitUntilClosed(d);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/////////////////////////////////////////////////////////////////////////

	public static EmbeddedPlayerWindowSWT openWindow(DownloadManager dm)
			throws Throwable {
		return openWindow(dm, dm.getTorrent(), null);
	}

	public static EmbeddedPlayerWindowSWT openWindow(DownloadManager dm,
			TOTorrent torrent, String runfile) throws Throwable {
		if (!Constants.isWindows && !Constants.isOSX) {
			throw new Exception("EMP Windows and OSX Only");
		}

		PluginInterface pi = null;

		try {
			pi = EmbeddedPlayerWindowManager.getPluginInterface();
			if (pi != null) {
				if (!Constants.isOSX) {
					pi.getPluginProperties().setProperty("plugin.unload.disabled", "true");
					pi.getPluginProperties().setProperty("plugin.silentrestart.disabled",
							"true");
				}
			}
			alreadyRunningWin_mon.enter();
			try {
				if (ONLY_ONE && alreadyRunningWin != null) {

					debug("same window for " + dm + ";" + runfile);
					alreadyRunningWin.semiClose();
				} else {
					debug("new window for " + dm + ";" + runfile);
					alreadyRunningWin = new EmbeddedPlayerWindowSWT();
				}

			} finally {
				alreadyRunningWin_mon.exit();
			}

			alreadyRunningWin.init(dm, torrent, runfile);
			return alreadyRunningWin;

		} catch (Throwable t) {
			if (pi != null && !Constants.isOSX) {
				pi.getPluginProperties().setProperty("plugin.unload.disabled", "false");
				pi.getPluginProperties().setProperty("plugin.silentrestart.disabled",
						"false");
			}
			throw t;
		}
	}

	public void init(final DownloadManager dm, TOTorrent torrent,
			String initialRunfile) throws Throwable {
		this.dm = dm;
		this.torrent = torrent;
		this.runFile = initialRunfile;

		isOurContent = false;
		isCurrentIncompleteStream = false;
		waitFor = 1;
		lastETA = -1;
		needsToBuffer = false;
		nowPaused = false;
		positionSecs = 0;
		rebufferStartTime = 0;
		currentBufferSize = rebufferSize;
		state = STATE_UNINITED;
		updatingDLStats = false;
		videoResolution = null;
		initSizeSet = false;

		if (dm != null) {
			dm.getGlobalManager().addDownloadWillBeRemovedListener(this);
		}

		if (bhAfter != null) {
			bhAfter.reset();
		}
		if (bhBefore != null) {
			bhBefore.reset();
		}

		hash = null;
		try {
			platformTorrentHash = torrent.getHashWrapper().toBase32String();
			hash = torrent.getHashWrapper().getBytes();
			if (!isOurContent) {
				platformTorrentHash = "nonvuze";
			}
		} catch (Exception e) {
			debug("No Hash");
			//throw new Exception("EMP: No Hash", e);
		}
		
		ContentNetworkManager cnm = ContentNetworkManagerFactory.getSingleton();
		long contentNetworkID = PlatformTorrentUtils.getContentNetworkID(torrent);
		contentNetwork = cnm.getContentNetwork(contentNetworkID);
		if (contentNetwork == null) {
			contentNetwork = ConstantsVuze.getDefaultContentNetwork();
		}
		
		URL_PLAYBEFORE = contentNetwork.getServiceURL(
				ContentNetwork.SERVICE_PREPLAYBACK, new Object[] {
					platformTorrentHash
				});
		URL_PLAYAFTER = contentNetwork.getServiceURL(
				ContentNetwork.SERVICE_POSTPLAYBACK, new Object[] {
					platformTorrentHash
				});

		if (torrent != null) {
			DownloadManagerEnhancer dme = DownloadManagerEnhancer.getSingleton();
			if (dme != null) {
				edm = dme.getEnhancedDownload(torrent.getHash());
			}
		}

		// do our own primary file calculation
		calculatePrimaryRunFile();

		if (!primaryFile.exists() && dm.getAssumedComplete()) {
			UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (functionsSWT == null) {
				return;
			}
			ManagerUtils.start(dm);

			String sPrefix = "v3.mb.PlayFileNotFound.";
			MessageBoxShell mb = new MessageBoxShell(functionsSWT.getMainShell(),
					MessageText.getString(sPrefix + "title"), MessageText.getString(
							sPrefix + "text", new String[] {
								dm.getDisplayName(),
							}), new String[] {
						MessageText.getString(sPrefix + "button.remove"),
						MessageText.getString(sPrefix + "button.redownload"),
						MessageText.getString("Button.cancel"),
					}, 2);
			mb.setRelatedObject(dm);
			int i = mb.open();

			if (i == 0) {
				ManagerUtils.remove(dm, functionsSWT.getMainShell(), true, false);
			} else if (i == 1) {
				dm.forceRecheck(new ForceRecheckListener() {
					public void forceRecheckComplete(DownloadManager dm) {
						ManagerUtils.start(dm);
					}
				});
			}
			close();
			return;
		}

		try {
			videoResolution = PlatformTorrentUtils.getContentVideoResolution(torrent);
		} catch (Throwable t) {
			debug("WARNING: getContentVideoResolution NA; " + t.toString());
		}

		try {
			mon_stats.enter();
			stats = new EmpStats();
		} finally {
			mon_stats.exit();
		}

		long streamSpeed = PlatformTorrentUtils.getContentStreamSpeedBps(torrent);

		stats.streamSpeed = streamSpeed;

		stats.useWMP = primaryFile.getName().endsWith(".wmv");

		if (stats.useWMP && !Constants.isWindows) {
			close();
			throw new Exception("embedded wmv playback for windows only");
		}

		final Download download = PluginCoreUtils.wrap(dm);

		final EmbeddedMediaPlayerPlugin plugin = EmbeddedMediaPlayerPlugin.instance;
		if (plugin != null) {
			if (!EmbeddedMediaPlayerPlugin.isExternalPlayerInstalled()) {

				UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (functionsSWT != null) {
					final Shell mainShell = functionsSWT.getMainShell();
					if (mainShell != null) {
						final Display display = mainShell.getDisplay();
						if (display != null && !display.isDisposed()) {
							display.asyncExec(new Runnable() {
								public void run() {
									final Shell installationShell = new Shell(mainShell,
											SWT.APPLICATION_MODAL);
									installationShell.setLayout(new FormLayout());

									Label label = new Label(installationShell, SWT.WRAP);
									label.setText(MessageText.getString("ump.install"));

									final ProgressBar progress = new ProgressBar(
											installationShell, SWT.SMOOTH);
									progress.setMinimum(0);
									progress.setMaximum(100);
									progress.setSelection(0);

									final Button cancel = new Button(installationShell, SWT.NONE);
									cancel.addListener(SWT.Selection, new Listener() {
										public void handleEvent(Event arg0) {
											plugin.cancelInstallExternalPlayer();
											installationShell.dispose();
										}
									});
									cancel.setText(MessageText.getString("Button.cancel"));

									FormData data;

									data = new FormData();
									data.left = new FormAttachment(0, 5);
									data.right = new FormAttachment(100, -5);
									data.top = new FormAttachment(0, 5);
									label.setLayoutData(data);

									data = new FormData();
									data.left = new FormAttachment(0, 5);
									data.right = new FormAttachment(100, -5);
									data.top = new FormAttachment(label, 5);
									progress.setLayoutData(data);

									data = new FormData();
									data.width = 70;
									data.right = new FormAttachment(100, -5);
									data.top = new FormAttachment(progress, 5);
									cancel.setLayoutData(data);

									installationShell.setSize(400, 100);
									Utils.centerWindowRelativeTo(installationShell, mainShell);
									installationShell.open();

									AEThread2 installer = new AEThread2("ump installer", false) {
										public void run() {
											try {
												plugin.installExternalPlayer(new InstallationListener() {

													public void reportComplete() {
														if (display != null && !display.isDisposed()) {
															display.asyncExec(new Runnable() {
																public void run() {
																	if (installationShell != null
																			&& !installationShell.isDisposed()) {
																		installationShell.dispose();
																	}
																}
															});
														}
														plugin.updateDownloadInfo(download);
														EmbeddedPlayerWindowSWT epw = EmbeddedPlayerWindowSWT.this;
														try {
															epw.init(epw.dm, epw.torrent, epw.runFile);
														} catch (Throwable t) {
															t.printStackTrace();
														}
													}

													public void reportError() {
														if (display != null && !display.isDisposed()) {
															display.asyncExec(new Runnable() {
																public void run() {
																	if (installationShell != null
																			&& !installationShell.isDisposed()) {
																		installationShell.dispose();
																	}
																}
															});
														}
													}

													public void reportPercentDone(final int percent) {
														if (display != null && !display.isDisposed()) {
															display.asyncExec(new Runnable() {
																public void run() {
																	if (progress != null
																			&& !progress.isDisposed()) {
																		progress.setSelection(percent);
																	}
																}
															});
														}

													}
												});

											} catch (Throwable t) {
												t.printStackTrace();
											}
										}
									};
									installer.start();
									return;

								}
							});
						}
					}
				}
				close();
				//throw new Exception("downloading universal player");
				return;
			}
		}

		// recalculate primary file here, becase I don't know if the code above
		// affects some attribute
		calculatePrimaryRunFile();

		updateDLStats(false);

		primaryFileLength = primaryFileInfo == null ? 0
				: primaryFileInfo.getLength();

		if (edm != null && edm.getDownloadManager().isDownloadComplete(true)) {
			debug("Download is complete");
			if (stats != null) {
				stats.isDownloadStreaming = false;
			}
			edm = null;
		} else {
			if (streamSpeed <= 0) {
				bufferSize = MIN_BUFFER_SIZE;
				rebufferSize = MIN_REBUFFER_SIZE;
			} else {
				bufferSize = (int) (streamSpeed * 10);
				if (bufferSize < MIN_BUFFER_SIZE) {
					bufferSize = MIN_BUFFER_SIZE;
				}
				rebufferSize = (int) (streamSpeed * 16);
				if (rebufferSize < MIN_REBUFFER_SIZE) {
					rebufferSize = MIN_REBUFFER_SIZE;
				}
			}
			setMinBufferBytes(rebufferSize);

			if (edm != null) {
				debug("Setting Progressive mode.");
				edm.setProgressiveMode(true);
			}
			if (stats != null) {
				stats.isDownloadStreaming = true;
			}

			dlAvgList = new ArrayList();

			dmListener = new DownloadManagerAdapter() {
				public void stateChanged(DownloadManager manager, int state) {
					if (stats != null && state == DownloadManager.STATE_DOWNLOADING) {
						stats.dmStartedOn = manager.getStats().getTimeStarted();
						debug("DM Started On " + stats.dmStartedOn);
					}
				}

				public void downloadComplete(DownloadManager manager) {
					dm.removeListener(dmListener);
					dmListener = null;

					if (currentButtonState != BS_ADS) {
						isCurrentIncompleteStream = false;
						if (stats != null) {
							stats.maxSeekAheadSecs = -1;
						}
						if (emp != null) {
							emp.setAllowSeekAhead(true);
						}

						if (soTimeSlider != null) {
							soTimeSlider.setDisabled(false);
						}
					}
				}
			};
			if (dm != null) {
				dm.addListener(dmListener);
				dm.addTrackerListener(this);
			}
		}

		fastRolling = true; // useWMP || Constants.isOSX;

		contentTitle = PlatformTorrentUtils.getContentTitle2(dm);
		if (contentTitle == null) {
			contentTitle = "Embedded Media Player!";
		}

		open();
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void calculatePrimaryRunFile() {
		int primaryFileIndex = EmbeddedMediaPlayerPlugin.getExternallyPlayableIndex(hash);
		if (primaryFileIndex >= 0) {
			primaryFileInfo = dm.getDiskManagerFileInfo()[0];
			primaryFile = primaryFileInfo.getFile(true);
			runFile = primaryFile.getAbsolutePath();
		} else if (edm != null) {
			primaryFileInfo = edm.getPrimaryFile();
			primaryFile = primaryFileInfo.getFile(true);
			runFile = primaryFile.getAbsolutePath();
		} else if (dm != null) {
			runFile = dm.getDownloadState().getPrimaryFile();
			if (runFile == null) {
				primaryFileInfo = dm.getDiskManagerFileInfo()[0];
				primaryFile = primaryFileInfo.getFile(true);
				runFile = primaryFile.getAbsolutePath();
			} else {
				primaryFile = new File(runFile);
			}
		} else if (runFile != null) {
			primaryFile = new File(runFile);
		}

		if (dm != null) {
			final Download download = PluginCoreUtils.wrap(dm);
			if (download.hasAttribute(EmbeddedMediaPlayerPlugin.playableAttribute)) {
				int playableIndex = download.getIntAttribute(EmbeddedMediaPlayerPlugin.playableAttribute);
				if (playableIndex >= 0) {
					if (stats != null) {
						stats.useExternalPlayer = true;
						stats.externalPlayer = download.getAttribute(EmbeddedMediaPlayerPlugin.playableWithAttribute);
						stats.playIndex = playableIndex;
					}
					try {
						runFile = download.getDiskManagerFileInfo()[playableIndex].getFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace();
					}
					//updateFlipToolbarButton(false,true);
				}
			}
		}
	}

	/**
	 * @param rebufferSize2
	 *
	 * @since 3.0.3.5
	 */
	private void setMinBufferBytes(int newSize) {
		if (edm != null) {
			edm.setMinimumBufferBytes((int) newSize);
			debug("switch buffer size to " + newSize);
		}
		currentBufferSize = newSize;
	}

	public void open() throws Throwable {
		debug("open()");
		updateDLStats(false);

		Object o = null;
		o = Utils.execSWTThreadWithObject("initEMP Shell", new AERunnableObject() {

			public Object runSupport() {
				debug("initEMP Shell");
				try {
					if (shell == null || shell.isDisposed()) {
						initShell();
						openShell();
						initEMP(stats.useExternalPlayer, stats.externalPlayer);
					} else {
						openShell();
						if (stats == null
								|| (stats.useWMP && !(emp instanceof EmbeddedWMP))
								|| (!stats.useWMP && !(emp instanceof EmbeddedMPlayer))
								|| (stats.useExternalPlayer)) {
							initEMP(stats.useExternalPlayer, stats.externalPlayer);
						}
					}
				} catch (Throwable e) {
					try {
						if (shell != null && !shell.isDisposed()) {
							shell.dispose();
						}
					} catch (Throwable e2) {
					}
					return e;
				}
				return null;
			}

		}, 0);

		if (o instanceof Throwable) {
			if (emp != null) {
				emp.delete();
				emp = null;
			}
			throw (Throwable) o;
		}

		playEnablerTimer = SimpleTimer.addEvent("Play Enabler",
				SystemTime.getOffsetTime(TIMEOUT_PREPLAYBACK_LOADING),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (bhBefore == null) {
							return;
						}
						if (stats != null && stats.playWait) {
							if (bhBefore.goodTitleSetOn < 0 && bhBefore.loadingURL) {
								debug("15s timeout hit.. aborting preplaybackwait. loadingURL? "
										+ bhBefore.loadingURL
										+ "; goodTitleSetOn="
										+ bhBefore.goodTitleSetOn);
								if (bhBefore.isOnURL(URL_PLAYBEFORE)) {
									abortPrePlaybackWait("15sTO");
								} else {
									abortPrePlaybackWait("15sATO");
								}
							} else {
								playEnablerTimer = SimpleTimer.addEvent("Play Enabler",
										SystemTime.getOffsetTime(TIMEOUT_PREPLAYBACK_FULLYLOADED),
										new TimerEventPerformer() {
											public void perform(TimerEvent event) {
												if (stats != null && stats.playWait) {
													debug("90s timeout hit.. aborting preplaybackwait.");
													if (bhBefore != null
															&& bhBefore.isOnURL(URL_PLAYBEFORE)) {
														abortPrePlaybackWait("90sTO");
													} else {
														abortPrePlaybackWait("90sATO");
													}
												}
											}
										});
							}
						}
					}
				});

		try {
			start(edm, dm, stats);
		} catch (Throwable e) {
			if (emp != null) {
				emp.delete();
				emp = null;
			}
			throw e;
		}
	}

	/**
	 * Initializes the shell
	 * <p>
	 * Note: Shell may be re-used, so don't set UI things that need resetting
	 *       when a new video is played.  Do it in {@link #openShell()} instead, or
	 *       if it's non UI stuff, set it in init()
	 *
	 * @throws Throwable
	 */
	public void initShell() throws Throwable {
		debug("initShell()");
		updateDLStats(false);

		int style = SWT.SHELL_TRIM | SWT.ON_TOP;

		shell = ShellFactory.createShell(style);

		shell.setMenuBar(null);

		try {
			shell.getFullScreen();
			USE_SWT_FULLSCREEN = true;
		} catch (Throwable t) {
			t.printStackTrace();
			USE_SWT_FULLSCREEN = false;
		}

		if (Constants.isOSX && !USE_SWT_FULLSCREEN
				&& System.getProperty("os.version").startsWith("10.3.")) {
			throw new Exception("embedded player for osx >= 10.4 only");
		}

		shell.setData("class", this);
		setMinSize(720, 580);

		Utils.setShellIcon(shell);
		if (!Utils.linkShellMetricsToConfig(shell, "ep2")) {
			Rectangle clientArea = shell.getMonitor().getClientArea();
			shell.setSize(clientArea.width * 100 / 120, clientArea.height * 100 / 120);
			Utils.centreWindow(shell);
		}
		//shell.setSize(1024, 700);

		skin = SWTSkinFactory.getNonPersistentInstance(
				EmbeddedPlayerWindow.class.getClassLoader(), PATH_SKIN_DEFS,
				FILE_SKIN_DEFS);

		skin.initialize(shell, "ep.shell");

		skin.layout();

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				debug("CLOSE");
				// this sometimes causes a deadlock
				//shell.setVisible(false);
				e.display.removeFilter(SWT.KeyDown, keyListener);
				close();
			}
		});

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE && emp != null && fullscreen) {
					emp.runAction(EmbeddedMediaPlayer.ACTION_FLIP_FULLSCREEN);
				}
			}
		});

		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				adjustBottomBar();
			}
		});

		SWTSkinObject skinObject = skin.getSkinObject("ep-player");
		cPlayer = (Composite) skinObject.getControl();

		//emp.sendCommand("pausing_keep get_file_name");
		//emp.sendCommand("pausing_keep get_time_length");

		KeyListener oldkeyListener = new KeyListener() {
			String cmd = "";

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.character == 0) {
					return;
				}
				if (e.keyCode == 13) {
					System.out.println(cmd);
					if (emp != null) {
						System.out.println(cmd);
						emp.sendCommand(cmd);
					}
					cmd = "";
				} else {
					cmd += e.character;
				}
			}
		};

		//shell.getDisplay().getFocusControl().addKeyListener(keyListener);

		keyListener = new Listener() {
			public void handleEvent(Event e) {
				if (e.display.getActiveShell() != shell) {
					return;
				}

				if (Constants.isOSX && e.stateMask == SWT.COMMAND && e.keyCode == 'w') {
					close();
				}

				if (e.stateMask == SWT.CONTROL && e.keyCode == 'v') {
					MessageBox mb = new MessageBox(shell, SWT.OK);
					mb.setText("EMP");
					mb.setMessage("v" + EmbeddedPlayerWindowManager.getPluginVersion());
					mb.open();
				}

				if (waitFor > 0) {
					return;
				}

				if (e.stateMask == SWT.MOD1 || e.stateMask == SWT.MOD3) {
					if (e.keyCode == '1') {
						System.out.println("50%");
						setShellSizePct(0.5, null);
					} else if (e.keyCode == '2') {
						System.out.println("100%");
						setShellSizePct(1, null);
					} else if (e.keyCode == '3') {
						System.out.println("200%");
						setShellSizePct(2, null);
					}
				}

				Control focusControl = e.display.getFocusControl();
				if (focusControl instanceof Browser) {
					return;
				}

				if (soPlayAfterBrowser != null) {
					Browser browser = soPlayAfterBrowser.getBrowser();
					if (browser != null && !browser.isDisposed()
							&& browser.isFocusControl()) {
						return;
					}
				}

				if (e.keyCode == ' ') {
					emp.runAction(EmbeddedMediaPlayer.ACTION_FLIP_PAUSE);
				} else if (e.keyCode == 'f') {
					emp.runAction(EmbeddedMediaPlayer.ACTION_FLIP_FULLSCREEN);
				} else if (e.keyCode == SWT.ARROW_RIGHT) {
					if (currentButtonState != BS_ADS && !isCurrentIncompleteStream) {
						emp.seekRelative(e.stateMask == SWT.CONTROL ? 1f : 10f);
					}
				} else if (e.keyCode == SWT.ARROW_LEFT) {
					emp.seekRelative(e.stateMask == SWT.CONTROL ? -1f : -10f);
				} else if (e.keyCode == SWT.ARROW_UP) {
					emp.setVolumeRelative(e.stateMask == SWT.CONTROL ? 1 : 10);
				} else if (e.keyCode == SWT.ARROW_DOWN) {
					emp.setVolumeRelative(e.stateMask == SWT.CONTROL ? -1 : -10);
				}
			}
		};

		shell.getDisplay().addFilter(SWT.KeyDown, keyListener);

		soTop = skin.getSkinObject("ep-top");
		soBottom = skin.getSkinObject("ep-bottom");

		SWTSkinObject soPause = skin.getSkinObject("ep-control-pause");
		if (soPause != null) {
			btnPause = new SWTSkinButtonUtility(soPause);
			buttonsToDisable.add(btnPause);
			adbuttonsToEnable.add(btnPause);
			btnPause.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (bhAfter != null) {
						// unpausing reloads playafter screen
						if ((state == STATE_STOPPED || state == STATE_PAUSED)
								&& bhAfter.isBrowserVisible()) {
							bhAfter.setURL(URL_PLAYAFTER, false, true, "");
						}

						bhAfter.setVisible(false, false);
					}

					if (emp == null) {
						return;
					}

					if (state == STATE_STOPPED) {
						debug("LOAD MEDIA from pause");
						emp.loadMedia(primaryFile == null ? runFile
								: primaryFile.getAbsolutePath());
					} else {
						emp.runAction(EmbeddedMediaPlayer.ACTION_FLIP_PAUSE);
					}
					updateUI();
				}
			});
		}

		SWTSkinObject soPrev = skin.getSkinObject("ep-control-prev");
		if (soPrev != null) {
			btnPrev = new SWTSkinButtonUtility(soPrev);
			buttonsToDisable.add(btnPrev);
			adbuttonsToEnable.add(btnPrev);
			btnPrev.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (bhAfter != null) {
						bhAfter.setVisible(false, false);
					}

					if (emp == null) {
						return;
					}

					// Most people with WMP can't seek our WMVs when they are streamed
					// So, reload the media on click
					if (state == STATE_STOPPED
							|| (stats.useWMP && nowPlayingFile != null && nowPlayingFile.indexOf("http://") >= 0)) {
						debug("reset" + runFile);
						emp.loadMedia(nowPlayingFile == null ? runFile : nowPlayingFile);
					} else {
						debug("seek 0");
						emp.seekPercent(0f);
						soTimeSlider.setPercent(0);
						emp.runAction(EmbeddedMediaPlayer.ACTION_PLAY);
					}
				}
			});
		}

		SWTSkinObject soNext = skin.getSkinObject("ep-control-next");
		if (soNext != null) {
			btnNext = new SWTSkinButtonUtility(soNext);
			buttonsToDisable.add(btnNext);
			btnNext.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					debug("next");
					if (bhAfter != null) {
						bhAfter.setURL(URL_PLAYAFTER, true, false, "");
					}
					if (stats != null) {
						stats.pp = true;
					}

					if (bhAfter != null) {
						bhAfter.setVisible(true, true);
						if (bhBefore != null) {
							bhBefore.notifyGeneric("emplog(\"hitPostPlayback\")");
						}
					}
				}
			});
		}

		final SWTSkinObject soFlipTopbar = skin.getSkinObject("ep-control-topbar");
		if (soFlipTopbar != null) {
			btnFlipToolbar = new SWTSkinButtonUtility(soFlipTopbar);
			buttonsToDisable.add(btnFlipToolbar);
			btnFlipToolbar.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					boolean newVisibility = !soTop.isVisible();
					SWTSkinUtils.setVisibility(skin, "ep.topbar", "ep-top",
							newVisibility, true, fastRolling);
					updateFlipToolbarButtonState(newVisibility);
				}
			});
		}

		PluginInterface pi = EmbeddedPlayerWindowManager.getPluginInterface();
		isCVS = pi == null ? true : pi.getAzureusVersion().indexOf("_") > 0;
		SWTSkinObject so = skin.getSkinObject("test4");
		if (so != null) {
			SWTSkinButtonUtility btn = new SWTSkinButtonUtility(so);
			btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
				}
			});
			((SWTSkinObjectText) so).setText("EMP v"
					+ EmbeddedPlayerWindowManager.getPluginVersion()
					+ " (CVS Debug Mode)");
			so.setVisible(isCVS);
		}

		SWTSkinObject soFullScreen = skin.getSkinObject("ep-control-fullscreen");
		if (soFullScreen != null) {
			btnFullScreen = new SWTSkinButtonUtility(soFullScreen);
			buttonsToDisable.add(btnFullScreen);
			adbuttonsToEnable.add(btnFullScreen);
			btnFullScreen.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					debug("fs btn");
					if (emp == null) {
						return;
					}
					emp.runAction(EmbeddedMediaPlayer.ACTION_FLIP_FULLSCREEN);
				}
			});
		}

		soVolumeSlider = (SWTSkinObjectSlider) skin.getSkinObject("ep-slider-volume");
		if (soVolumeSlider != null) {
			soVolumeSlider.addListener(new SWTSkinObjectSlider.SWTSkinListenerSliderSelection() {
				public void selectionChanged(double percent) {
					int iPercent = (int) (percent * 100);

					if (emp == null) {
						return;
					}
					emp.setVolume(iPercent);

					updateMuteButton(false);
					COConfigurationManager.setParameter("ep.volume", iPercent);
				}
			});
			int volume = COConfigurationManager.getIntParameter("ep.volume", 100);
			soVolumeSlider.setPercent(volume / 100.0);
		}

		soMute = skin.getSkinObject("ep-control-mute");
		if (soMute != null) {
			btnMute = new SWTSkinButtonUtility(soMute);
			btnMute.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {
					new AEThread2("switch mute", true) {
						public void run() {
							boolean nowMuted;
							if (emp == null) {
								return;
							}
							nowMuted = !emp.isMuted();
							emp.setMuted(nowMuted);
							updateMuteButton(nowMuted);
							COConfigurationManager.setParameter("ep.mute", nowMuted);
						}
					}.start();
				}
			});

			boolean nowMuted = COConfigurationManager.getBooleanParameter("ep.mute",
					false);
			updateMuteButton(nowMuted);
		}

		soTimeSlider = (SWTSkinObjectSlider) skin.getSkinObject("ep-time-slider");
		if (soTimeSlider != null) {
			soTimeSlider.setMouseMoveAdjusts(false);
			soTimeSlider.addListener(new SWTSkinObjectSlider.SWTSkinListenerSliderSelection() {
				// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectSlider.SWTSkinListenerSliderSelection#selectionChanging(double)
				public Double selectionChanging(double oldPercent, double newPercent) {
					Double newValue = null;

					if (newPercent == 0) {
						return null;
					} else if (newPercent < 0) {
						newValue = new Double(0);
						return newValue;
					} else if (newPercent > 1) {
						newValue = new Double(1);
					}

					if (stats != null) {
						// No seeking ahead on streams or ads
						if (stats.maxSeekAheadSecs >= 0
								&& (isCurrentIncompleteStream || currentButtonState == BS_ADS)) {
							double newSecs = stats.totalSecs * newPercent;
							if (newSecs > stats.maxSeekAheadSecs) {
								newValue = new Double(stats.maxSeekAheadSecs / stats.totalSecs);
							}
						}

						long newSecs = (long) (stats.totalSecs * (newValue == null
								? newPercent : newValue.doubleValue()));

						soTimeText.setText("(" + TimeFormatter.formatColon(newSecs) + ") "
								+ TimeFormatter.formatColon((long) positionSecs) + " / "
								+ TimeFormatter.formatColon((long) stats.totalSecs));
					}

					return newValue;
				}

				public void selectionChanged(double percent) {
					if (emp == null || stats == null) {
						return;
					}
					if (state == STATE_STOPPED) {
						debug("slider " + runFile);
						emp.loadMedia(nowPlayingFile == null ? runFile : nowPlayingFile);
						if (bhAfter != null) {
							bhAfter.setVisible(false, false);
						}
					}
					double posSecs = stats.totalSecs * percent;
					if (posSecs == 0) {
						emp.seekPercent(0f);
					} else {
  					emp.seekAbsolute((float) posSecs);
					}
					if (state == STATE_BUFFERING) {
						setState(0);
					}
				}
			});
		}

		soTimeText = (SWTSkinObjectText) skin.getSkinObject("ep-time-text");
		if (soTimeText != null) {
			timePollerTimer = SimpleTimer.addPeriodicEvent("EMP time poller", 200,
					new TimerEventPerformer() {
						int i = 0;

						public void perform(final TimerEvent event) {
							checkCursorPos();
							if ((i++ % 4) == 0) {
								updateUI();
							}
							if ((i % 2) == 0) {
								updateDLStats((i % 5) == 0);
							}
						}
					});
		}

		soPlayBeforeBrowser = (SWTSkinObjectBrowser) skin.getSkinObject("ep-play-before");
		if (soPlayBeforeBrowser != null) {
			bhBefore = new BrowserHandlerSWT(this, soPlayBeforeBrowser);
		}

		soPlayAfterBrowser = (SWTSkinObjectBrowser) skin.getSkinObject("ep-play-after");
		if (soPlayAfterBrowser != null) {
			bhAfter = new BrowserHandlerSWT(this, soPlayAfterBrowser);
			bhAfter.setSwapsWithPlayer(true);
		}

		soWaitText = (SWTSkinObjectText) skin.getSkinObject("ep-wait-text");

		debug("initShell() done");
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	protected void adjustBottomBar() {
		Rectangle clientArea = shell.getClientArea();
		boolean enableFlipToolbar = true;
		boolean enableTimeText = true;
		boolean enableVolumeSlider = true;
		boolean enableNext = true;
		boolean enablePrev = true;

		if (clientArea.width < 450) {
			enableFlipToolbar = false;
			if (clientArea.width < 415) {
				enableVolumeSlider = false;
			}
			if (clientArea.width < 350) {
				enableTimeText = false;
			}
			if (clientArea.width < 220) {
				enableNext = false;
			}
			if (clientArea.width < 180) {
				enablePrev = false;
			}
		}
		if (btnFlipToolbar != null) {
			Control control = btnFlipToolbar.getSkinObject().getControl();
			setVisibilityAndResize(control, enableFlipToolbar);
		}
		if (soTimeText != null) {
			setVisibilityAndResize(soTimeText.getControl(), enableTimeText);
		}
		if (soVolumeSlider != null) {
			setVisibilityAndResize(soVolumeSlider.getControl(), enableVolumeSlider);
		}
		if (btnNext != null) {
			setVisibilityAndResize(btnNext.getSkinObject().getControl(), enableNext);
		}
		if (btnPrev != null) {
			setVisibilityAndResize(btnPrev.getSkinObject().getControl(), enablePrev);
		}
	}

	private void setVisibilityAndResize(Control control, boolean visible) {
		FormData formData = (FormData) control.getLayoutData();
		int width = visible ? -1 : 0;
		int height = visible ? -1 : 0;
		if (visible) {
			Object data = control.getData("oldVisible.width");
			if (data instanceof Long) {
				width = ((Long) data).intValue();
			} else if (formData.width > 0) {
				width = formData.width;
			}
			data = control.getData("oldVisible.height");
			if (data instanceof Long) {
				height = ((Long) data).intValue();
			} else if (formData.height > 0) {
				height = formData.height;
			}
		} else {
			if (formData.height > 0) {
				control.setData("oldVisible.height", new Long(formData.height));
			}
			if (formData.width > 0) {
				control.setData("oldVisible.width", new Long(formData.width));
			}
		}
		boolean changed = false;
		if (formData.width != width) {
			formData.width = width;
			changed = true;
		}
		if (formData.height != height) {
			formData.height = height;
			changed = true;
		}
		control.setVisible(visible);
		if (changed) {
			control.setLayoutData(formData);
			control.getParent().layout(true);
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	private void openShell() {
		debug("openShell()");
		updateDLStats(false);

		if (bhBefore != null) {
			bhBefore.setToWaitText(contentTitle);
			bhBefore.setVisible(false, false);
		}
		if (bhAfter != null && bhAfter.isBrowserVisible()) {
			bhAfter.setVisible(false, false);
		}
		cPlayer.moveBelow(null);

		changeButtonDisableState(BS_DISABLE);
		SWTSkinUtils.setVisibility(skin, "ep.topbar", "ep-top", false, false, true);
		if (soTimeSlider != null) {
			soTimeSlider.setDisabled(true);
		}
		if (soWaitText != null) {
			soWaitText.setVisible(true);
			soWaitText.getControl().moveAbove(null);
			soWaitText.setText(contentTitle);

			Utils.execSWTThreadLater(0, new AERunnable() {

				public void runSupport() {
					soWaitText.setVisible(true);
					soWaitText.getControl().moveAbove(null);

				}
			});
		}

		shell.setText(contentTitle);
		if (shell != null && !shell.isVisible()) {
			shell.open();
			shell.layout();
		}
		shell.setMinimized(false);
		shell.setActive();
		shell.forceActive();

		Display display = shell.getDisplay();
		int x = 0;
		while (!display.isDisposed() && display.readAndDispatch() && x < 1000) {
			x++;
		}
		if (rateCell != null) {
			rateCell.setDataSource(dm);
			rateCell.invalidate();
			rateCell.refresh();
		}
	}

	private void initEMP(boolean useExternalPlayer, String externalPlayer)
			throws Throwable {
		debug("initEmp()");
		updateDLStats(false);

		changeButtonDisableState(BS_DISABLE);

		try {
			if (emp != null) {
				emp.delete();
			}
			if (stats.useWMP) {
				emp = new EmbeddedWMP();
			} else {
				emp = new EmbeddedMPlayer(useExternalPlayer, externalPlayer);
			}

			emp.addListener((EmpListenerAudio) this);
			emp.addListener((EmpListenerFileChanged) this);
			emp.addListener((EmpListenerTimeout) this);
			emp.addListener((EmpListenerFullScreen) this);

			emp.init(cPlayer, USE_SWT_FULLSCREEN);
		} catch (Throwable e) {
			if (emp != null) {
				emp.delete();
				emp = null;
			}
			if (shell != null && !shell.isDisposed()) {
			}
			throw e;
		}
	}

	// @see org.gudy.azureus2.core3.download.DownloadManagerTrackerListener#announceResult(org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse)
	public void announceResult(TRTrackerAnnouncerResponse response) {
		if (stats == null) {
			return;
		}
		if (stats.completedTrackerAnnounceOn == -1) {
			stats.completedTrackerAnnounceOn = SystemTime.getCurrentTime();

			TRTrackerAnnouncer trackerAnnouncer = dm.getTrackerClient();
			if (trackerAnnouncer != null) {
				int lastAnnounceStartedOnSecs = trackerAnnouncer.getLastUpdateTime();
				if (lastAnnounceStartedOnSecs > 0) {
					long startedTrackerAnnounceOn = lastAnnounceStartedOnSecs * 1000L + 999;
					if (startedTrackerAnnounceOn > stats.completedTrackerAnnounceOn) {
						startedTrackerAnnounceOn = stats.completedTrackerAnnounceOn;
					}
					if (startedTrackerAnnounceOn > stats.startedTrackerAnnounceOn) {
						stats.startedTrackerAnnounceOn = startedTrackerAnnounceOn;
					}

					debug("Started Tracker Announce On " + stats.startedTrackerAnnounceOn
							+ "; real=" + startedTrackerAnnounceOn);
				}
			}

			debug("Received Tracker Announce On " + stats.completedTrackerAnnounceOn
					+ "; took "
					+ (stats.completedTrackerAnnounceOn - stats.startedTrackerAnnounceOn));
		}
	}

	// @see org.gudy.azureus2.core3.download.DownloadManagerTrackerListener#scrapeResult(org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse)
	public void scrapeResult(TRTrackerScraperResponse response) {
		if (stats == null) {
			return;
		}
		if (stats.completedTrackerAnnounceOn == -1) {
			TRTrackerAnnouncer trackerAnnouncer = dm.getTrackerClient();
			if (trackerAnnouncer != null) {
				TRTrackerAnnouncerResponse lastResponse = trackerAnnouncer.getLastResponse();
				int lastAnnounceStartedOnSecs = trackerAnnouncer.getLastUpdateTime();
				if (lastAnnounceStartedOnSecs > 0 && lastResponse.getURL() != null) {
					stats.completedTrackerAnnounceOn = SystemTime.getCurrentTime();

					long startedTrackerAnnounceOn = lastAnnounceStartedOnSecs * 1000L + 999;
					if (startedTrackerAnnounceOn > stats.completedTrackerAnnounceOn) {
						startedTrackerAnnounceOn = stats.completedTrackerAnnounceOn;
					}
					if (startedTrackerAnnounceOn > stats.startedTrackerAnnounceOn) {
						stats.startedTrackerAnnounceOn = startedTrackerAnnounceOn;
					}
					debug("Started Tracker Announce via scrape trigger on "
							+ stats.startedTrackerAnnounceOn + "; real="
							+ startedTrackerAnnounceOn);

					debug("Received Tracker Announce via scrape trigger on "
							+ stats.completedTrackerAnnounceOn
							+ "; took "
							+ (stats.completedTrackerAnnounceOn - stats.startedTrackerAnnounceOn));
				}
			}
		}
	}

	private void updateDLStats(boolean doSnapshot) {
		if (updatingDLStats) {
			return;
		}
		updatingDLStats = true;

		try {
			if (stats == null || !stats.isDownloadStreaming || dm == null
					|| dmListener == null || dm.getAssumedComplete()) {
				return;
			}

			long now = SystemTime.getCurrentTime();

			DLSnapshot snapshot = new DLSnapshot();
			snapshot.dlRate = dm.getStats().getDataReceiveRate();
			if (snapshot.dlRate > stats.maxDLRateSinceRebuffer) {
				stats.maxDLRateSinceRebuffer = snapshot.dlRate;
			}
			if (snapshot.dlRate < stats.minDLRateSinceRebuffer
					|| stats.minDLRateSinceRebuffer == -1) {
				stats.minDLRateSinceRebuffer = snapshot.dlRate;
			}

			if (stats.completedTrackerAnnounceOn == -1) {
				TRTrackerAnnouncer trackerAnnouncer = dm.getTrackerClient();
				if (trackerAnnouncer == null
						|| trackerAnnouncer.getLastUpdateTime() == 0) {
					stats.startedTrackerAnnounceOn = SystemTime.getCurrentTime();

					debug("a=" + stats.dmStartedOn + ";" + stats.startedTrackerAnnounceOn);
				}
			}

			if (stats.metStreamSpeedAt == -1) {
				if (snapshot.dlRate > stats.streamSpeed) {
					stats.metStreamSpeedAt = now;
					debug("met stream speed at " + stats.metStreamSpeedAt);
				} else {
					// only record % speeds if we haven't fully met stream speed yet 
					if (stats.met75StreamSpeedAt == -1
							&& snapshot.dlRate > stats.streamSpeed / 4 * 3) {
						stats.met75StreamSpeedAt = now;
						debug("met 75% of stream speed at " + stats.met75StreamSpeedAt);
					}

					if (stats.met50StreamSpeedAt == -1
							&& snapshot.dlRate > stats.streamSpeed / 2) {
						stats.met50StreamSpeedAt = now;
						debug("met 50% of stream speed at " + stats.met50StreamSpeedAt);
					}

					if (stats.met25StreamSpeedAt == -1
							&& snapshot.dlRate > stats.streamSpeed / 4) {
						stats.met25StreamSpeedAt = now;
						debug("met 25% of stream speed at " + stats.met25StreamSpeedAt);
					}
				}
			}

			if (stats.streamableUnbufferedOn == -1
					&& edm.getProgressivePlayETA(true) <= 0) {
				stats.streamableUnbufferedOn = SystemTime.getCurrentTime();
				debug("streamability met, now buffering");
			}

			PEPeerManager pm = dm.getPeerManager();
			if (pm != null) {
				if (doSnapshot) {
					snapshot.numPeers = pm.getNbPeers();
					snapshot.numSeeds = pm.getNbSeeds();
				}

				if (stats.got1stPeerOn == 0 || stats.got1stDataOn == 0
						|| stats.got1stConnectedPeerOn == 0
						|| stats.got1stHandshakedPeerOn == 0) {
					List peers = pm.getPeers();

					if (stats.got1stPeerOn == 0 && peers.size() > 0) {
						stats.got1stPeerOn = SystemTime.getCurrentTime();
						debug("got 1st peer on " + stats.got1stPeerOn);
					}

					//List peers = pm.getPeers();
					for (Iterator iter = peers.iterator(); iter.hasNext();) {
						PEPeer peer = (PEPeer) iter.next();
						if (stats.got1stDataOn == 0) {
							if (peer.getStats().getTotalDataBytesReceived() > 0) {
								stats.got1stDataOn = SystemTime.getCurrentTime();
								debug("got 1st data on " + stats.got1stDataOn);
							}
						}
						int state = peer.getPeerState();
						if (stats.got1stConnectedPeerOn == 0
								&& (state == PEPeer.HANDSHAKING || state == PEPeer.TRANSFERING)) {
							stats.got1stConnectedPeerOn = SystemTime.getCurrentTime();
							debug("got 1st connected on " + stats.got1stConnectedPeerOn);
						}

						if (stats.got1stHandshakedPeerOn == 0
								&& state == PEPeer.TRANSFERING) {
							stats.got1stHandshakedPeerOn = SystemTime.getCurrentTime();
							debug("got 1st handshaked on " + stats.got1stHandshakedPeerOn);
						}

						if (stats.got1stDataOn > 0 && stats.got1stConnectedPeerOn > 0
								&& stats.got1stHandshakedPeerOn > 0) {
							break;
						}
					}
				}
			}

			if (doSnapshot) {
				dlAvgList.add(snapshot);
			}
		} finally {
			updatingDLStats = false;
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.3.3
	 */
	protected void updateUI() {
		if (shell.isDisposed()) {
			timePollerTimer.cancel();
		}

		EmbeddedMediaPlayer empCopy = emp;
		if (empCopy == null) {
			return;
		}

		//System.out.println(emp.getFrameDropPct());

		if (empCopy.isStopped()) {
			if (waitFor == 0 && state == STATE_STOPPED) {
				return;
			}
			positionSecs = -1;
			nowPaused = false;
			// TODO: Update rate icon!
		} else {
			nowPaused = empCopy.isPaused();

			positionSecs = empCopy.getPositionSecs();
		}

		if (stats != null) {
			if (positionSecs > stats.maxSeekAheadSecs) {
				stats.maxSeekAheadSecs = positionSecs;
			}

			if (positionSecs > stats.totalSecs) {
				stats.totalSecs = Math.max(positionSecs, emp.getLength());
			}
		}

		if (isCurrentIncompleteStream && currentButtonState != BS_ADS
				&& waitFor == 0 && edm != null) {
			long streamPos = empCopy.getStreamPos();
			long availPos = edm.getContiguousAvailableBytes(primaryFileInfo);

			// DM might be null, but the file complete.. quick hack check
			// as this should be done in EnhancedDownloadManager but we want to
			// release a fix earlier..
			if (availPos == -1 && dm.getAssumedComplete()) {
				availPos = primaryFileLength;
			}

			debug("S: " + availPos + " - " + streamPos + "=" + (availPos - streamPos)
					+ ";" + (availPos - streamPos - currentBufferSize) + ";p="
					+ nowPaused + ":state=" + state + ";pos=" + positionSecs + ";buf?"
					+ empCopy.isBuffering());

			if (streamPos >= 0) {
				edm.setViewerPosition(primaryFileInfo, streamPos);

				if (empCopy.isBuffering()
						|| (availPos < primaryFileLength && availPos - streamPos
								- currentBufferSize <= 0)) {
					needsToBuffer = true;
					setState(STATE_BUFFERING);
				} else {
					needsToBuffer = false;
				}
			} else {
				needsToBuffer = false;
				// else streamPos == -1 { keep needsToBuffer state }
			}
		} else {
			needsToBuffer = false;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				updateUISWT();
			}
		});
	}

	private void checkCursorPos() {
		if (!fullscreen || !USE_SWT_FULLSCREEN) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (!fullscreen || shell == null || shell.isDisposed()) {
					return;
				}
				Point cursorLoc = shell.getDisplay().getCursorLocation();

				if (cursorLoc.equals(lastCursorPt)) {
					return;
				}

				Control cursorControl = shell.getDisplay().getCursorControl();

				while (cursorControl != null && cursorControl != soTop.getControl()
						&& cursorControl != soBottom.getControl()) {
					cursorControl = cursorControl.getParent();
				}
				if (cursorControl != null) {
					return;
				}

				Rectangle bounds = shell.getMonitor().getBounds();

				int newCursorPos = SWT.DEFAULT;
				if (cursorLoc.y < bounds.y + 3 && cursorLoc.y >= bounds.y) {
					newCursorPos = SWT.TOP;
				} else if (cursorLoc.y > bounds.y + bounds.height - 3
						&& cursorLoc.y <= bounds.y + bounds.height) {
					newCursorPos = SWT.BOTTOM;
				}

				if (newCursorPos != lastCursorPos) {
					lastCursorPos = newCursorPos;
					switch (newCursorPos) {
						case SWT.TOP:
							SWTSkinUtils.setVisibility(skin, null, "ep-top", true, false,
									true);
							updateFlipToolbarButtonState(true);
							break;

						case SWT.BOTTOM:
							SWTSkinUtils.setVisibility(skin, null, "ep-bottom", true, false,
									true);
							break;

						default:
							SWTSkinUtils.setVisibility(skin, null, "ep-bottom", false, false,
									true);
							SWTSkinUtils.setVisibility(skin, null, "ep-top", false, false,
									true);
							updateFlipToolbarButtonState(false);
							break;
					}
				}

			}
		});
	}

	/**
	 * 
	 *
	 * @return 
	 * @since 3.0.3.3
	 */
	protected boolean updateUISWT() {
		if (shell.isDisposed()) {
			return false;
		}
		
		// grab the reference of emp in case it nulls
		EmbeddedMediaPlayer emp = this.emp;
		if (emp == null) {
			return false;
		}

		if (state == STATE_BUFFERING) {
			long eta = waitFor > 0 || !isCurrentIncompleteStream || edm == null
					? waitFor : edm.getProgressivePlayETA();

			if (eta > 0) {
				needsToBuffer = true;
				boolean moment = stats == null
						|| (SystemTime.getCurrentTime() - stats.startedOn < ETA_SOON_UNTIL);
				String sETA = moment ? " a moment" : TimeFormatter.format(eta);

				if (lastETA != eta) {
					lastETA = eta;
					if (waitFor > 0) {
						soTimeText.setText("Up Next: " + contentTitle);
					} else {
						soTimeText.setText("Buffering.. ETA: " + sETA);
					}
					if (!moment && (stats == null || !stats.playWait)) {
						shell.setText(sETA + " | " + contentTitle);
					}

					if (!moment && soWaitText != null) {
						soWaitText.setText("ETA: " + sETA);
					}

					String script = "try { if (updateETA) { updateETA('" + sETA + "', "
							+ eta + ", '" + contentTitle.replaceAll("'", "`") + " '"
							+ ");} } catch (e) { }";
					soPlayBeforeBrowser.getBrowser().execute(script);
				}
				return true;
			}
		} else if (lastETA >= 0) {
			lastETA = -1;
			shell.setText(contentTitle);
			if (soWaitText != null) {
				soWaitText.setText("");
			}
		}

		if (waitFor > 0) {
			needsToBuffer = true;
		}

		boolean isEmpBuffering;
		isEmpBuffering = emp.isBuffering();

		if (isEmpBuffering) {
			setState(STATE_BUFFERING);
			needsToBuffer = true;
		} else if (state == STATE_BUFFERING && !needsToBuffer) {
			setState(0);
			debug("===========UNpause");
		}

		//if (totalSecs == 0) {
		//	return false;
		//}

		if (soBottom.isVisible()) {
			if (state == STATE_BUFFERING) {
				soTimeText.setText("Buffering..");
				return true;
			}
			if (stats != null) {
				soTimeSlider.setPercent(positionSecs / stats.totalSecs);
			}
		}
		if (positionSecs < 0) {
			if (emp.isStopped()) {
				debug("BUFFING? " + (emp == null ? "null" : "" + emp.isBuffering())
						+ ";" + state + ";ps=" + positionSecs + ";stopped?"
						+ emp.isStopped());
				setState(STATE_STOPPED);
			}
		} else {
			if (bhBefore != null) {
				if (currentButtonState == BS_ADS) {
					bhBefore.notifyAdPlaying((long) positionSecs, (long) stats.totalSecs);
				} else {
					bhBefore.notifyVideoPlaying((long) positionSecs,
							(long) stats.totalSecs);
				}
			}

			if (state != STATE_BUFFERING && nowPaused != (state == STATE_PAUSED)) {
				setState(nowPaused ? STATE_PAUSED : 0);
				return true;
			} else {
				if (state != 0 && !nowPaused) {
					setState(0);
				}

				if (soBottom.isVisible() && stats != null) {
					soTimeText.setText(TimeFormatter.formatColon((long) positionSecs)
							+ " / " + TimeFormatter.formatColon((long) stats.totalSecs));
				}
			}

		}
		return true;
		//debug(positionSecs);
	}

	private synchronized void setState(int i) {
		if (state == i) {
			return;
		}

		debug("State change from " + STATE_STRINGS[state] + " to "
				+ STATE_STRINGS[i] + " via " + Debug.getCompressedStackTrace(3));

		// Note: state is old state, i is new state
		if (state == STATE_BUFFERING && rebufferStartTime > 0) {
			// switching off buffering
			if (stats != null) {
				stats.increaseRebufferMS(SystemTime.getCurrentTime()
						- rebufferStartTime);
			}
			rebufferStartTime = 0;
		}

		if (i == STATE_STOPPED && state != STATE_UNINITED) {
			btnFullScreen.setDisabled(true);

			if (bhAfter != null) {
				bhAfter.setURL(URL_PLAYAFTER, true, false, "");
			}
			if (stats != null) {
				stats.pp = true;
			}
			if (bhAfter != null) {
				bhAfter.setVisible(true, true);
				if (bhBefore != null) {
					bhBefore.notifyGeneric("emplog(\"hitPostPlaybackViaStopped\")");
				}
			}
			soTimeText.setText("Stopped");
			if (btnPause != null) {
				btnPause.setImage("image.ep.button.replay");
			}
		} else if (i == STATE_BUFFERING) {
			if (edm != null) {
				setMinBufferBytes(rebufferSize);
			}

			// switching on buffering
			rebufferStartTime = SystemTime.getCurrentTime();

			soTimeText.setText("Buffering....");

			if (waitFor == 0) {
				final boolean hardRebuffer = emp.isBuffering()
						&& (stats != null && !stats.useWMP);
				if (hardRebuffer) {
					debug("HR1");
				}
				new AEThread2("EMP.sS", true) {
					public void run() {
						long availPos = edm == null ? -1
								: edm.getContiguousAvailableBytes(primaryFileInfo);
						if (stats != null) {
							stats.increaseRebufferCount(dm, hardRebuffer, positionSecs,
									emp.getStreamPos(), availPos, currentBufferSize,
									stats.minDLRateSinceRebuffer, stats.maxDLRateSinceRebuffer);
							stats.maxDLRateSinceRebuffer = -1;
							stats.minDLRateSinceRebuffer = -1;
						}
						emp.runAction(EmbeddedMediaPlayer.ACTION_PAUSE);
					}
				}.start();
			}
		} else if (i == STATE_PLAYING) {
			if (!fullscreen) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						final boolean newVisibility = COConfigurationManager.getBooleanParameter(
								"ep.topbar", true);
						SWTSkinUtils.setVisibility(skin, "ep.topbar", "ep-top",
								newVisibility, true, fastRolling);
						updateFlipToolbarButtonState(newVisibility);
					}
				});
			}
			if (edm != null) {
				setMinBufferBytes(bufferSize);
			}
			btnFullScreen.setDisabled(false);
			disableFlipToolbarButton(false);

			if (bhAfter != null) {
				bhAfter.setVisible(false, true);
			}

			if (bhBefore != null) {
				if (bhBefore.getHeight() <= 0 && bhBefore.getWidth() <= 0) {
					bhBefore.setHeight(0, true);
				}
			}
			emp.runAction(EmbeddedMediaPlayer.ACTION_PLAY);
			if (btnPause != null) {
				btnPause.setImage("image.ep.button.pause");
			}
			nowPaused = false;
		} else if (i == STATE_PAUSED) {
			if (btnPause != null) {
				btnPause.setImage("image.ep.button.play");
			}
		}

		if (soTimeSlider != null) {
			boolean b = i == STATE_BUFFERING
					|| isCurrentIncompleteStream
					|| currentButtonState == BS_ADS
					|| ((i == STATE_PAUSED || i == STATE_STOPPED) && state == STATE_UNINITED);
			soTimeSlider.setDisabled(b);
		}

		state = i;
	}

	protected void updateFlipToolbarButtonState(boolean visible) {
		if (btnFlipToolbar == null) {
			return;
		}
		updateFlipToolbarButton(visible, btnFlipToolbar.isDisabled());
	}

	protected void disableFlipToolbarButton(boolean disable) {
		if (btnFlipToolbar == null) {
			return;
		}

		updateFlipToolbarButton(soTop.isVisible(), disable);
	}

	private void updateFlipToolbarButton(boolean visible, boolean disabled) {
		if (btnFlipToolbar == null) {
			return;
		}

		if (btnFlipToolbar.isDisabled() != disabled) {
			btnFlipToolbar.setDisabled(disabled);
		}

		if (visible) {
			btnFlipToolbar.setImage("image.ep.button.hidetop");
			if (rateCell != null) {
				rateCell.invalidate();
				rateCell.refresh();
			}
		} else {
			btnFlipToolbar.setImage("image.ep.button.showtop");
		}
	}

	/**
	 * @param nowMuted
	 *
	 * @since 3.0.2.3
	 */
	protected void updateMuteButton(boolean nowMuted) {
		String id = nowMuted ? "image.ep.mute" : "image.ep.vol";
		lastMute = nowMuted;
		btnMute.setImage(id);
	}

	/**
	 * @param string
	 *
	 * @since 3.0.2.3
	 */
	protected static void debug(String string) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.emp");
		diag_logger.log("[EMP] " + string);

		if (DEBUG_TO_STDOUT) {
			System.out.println(SystemTime.getCurrentTime() + "] " + string);
		}

		if (DEBUG) {
			if (logger == null) {
				logger = EmbeddedPlayerWindowManager.getLogger();
				if (logger == null) {
					return;
				}
			}
			logger.log(string);
		}
	}

	private static int BS_DISABLE = 1;

	private static int BS_ENABLE = 2;

	private static int BS_ADS = 3;

	private void changeButtonDisableState(int buttonState) {
		currentButtonState = buttonState;
		for (Iterator iter = buttonsToDisable.iterator(); iter.hasNext();) {
			Object next = iter.next();

			boolean disable;
			if (buttonState != BS_ADS) {
				disable = buttonState == BS_DISABLE;
			} else {
				disable = !adbuttonsToEnable.contains(next);
			}

			if (next instanceof SWTSkinButtonUtility) {
				SWTSkinButtonUtility btn = (SWTSkinButtonUtility) next;
				btn.setDisabled(disable);
			} else if (next instanceof SWTSkinObjectBasic) {
				String suffix = disable ? "-disabled" : "";

				((SWTSkinObjectBasic) next).switchSuffix(suffix, 1, true);
			}
		}
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				if (!shell.isDisposed()) {
					shell.update();
				}
			}

		});
	}

	/**
	 * Passes in parameters instead of using class' variable, because the class
	 * is re-used if it's already opened.  Not the best way to handle it, but
	 * it works 
	 * 
	 * @since 3.0.5.3
	 */
	private void start(EnhancedDownloadManager edm, DownloadManager dm,
			EmpStats stats) {
		waitFor = 1;
		debug("start()");
		updateDLStats(false);

		if (stats != null) {
			stats.startCalledOn = SystemTime.getCurrentTime();
		}

		final List dmsToWaitFor = new ArrayList();
		final AEMonitor dmsToWaitFor_mon = new AEMonitor("dmsToWaitFor");

		if (edm != null || (stats != null && stats.playWait)) {
			if (stats != null) {
				stats.didWait = true;
			}

			if (edm != null) {
				dmsToWaitFor.add(edm);
				if (stats != null) {
					stats.dmStartedOn = dm.getStats().getTimeStarted();
				}
			}
			debug("==========================WAITING FOR ETA on "
					+ dmsToWaitFor.size());

			boolean ok = !shell.isDisposed();

			long eta;
			dmsToWaitFor_mon.enter();
			try {
				eta = getLargestETA(dmsToWaitFor);
			} finally {
				dmsToWaitFor_mon.exit();
			}

			boolean first = true;
			long lastUpdate = 0;
			boolean sentSetStreamReady = false;

			while (stats == this.stats
					&& (stats.playWait || eta > 0)
					&& ok) {
				//System.out.println("playWait=" + playWait + ";eta=" + eta);

				// If playWait has been aborted, that means we timed-out (or other good
				// reasons). Assuming the main content is complete, we should stop
				// waiting for dependencies to download
				if (stats.playWaitAbortedReason != null && dm != null
						&& dm.getAssumedComplete()) {
					debug("Playing immediately as wait was aborted "
							+ stats.playWaitAbortedReason + ", and content is complete");
					break;
				}

				if (first) {
					state = STATE_BUFFERING;

					if (bhBefore != null) {
						bhBefore.reset();
						bhBefore.setURL(URL_PLAYBEFORE, false, true, "&streamReady="
								+ (eta <= 0 ? "1" : "0"));
						bhBefore.setVisible(true, true);
					}
					first = false;
				}

				if (eta > 0) {
					if (eta != waitFor) {
						debug("ETA: " + eta);
					}
					waitFor = eta;
				}

				try {
					if (Utils.isThisThreadSWT()) {
						while (shell.getDisplay().readAndDispatch()) {
						}
						Thread.sleep(20);
					} else {
						Thread.sleep(300);
					}
					if (SystemTime.getCurrentTime() >= lastUpdate + 300) {
						updateDLStats(false);
						lastUpdate = SystemTime.getCurrentTime();
					}
				} catch (Throwable e) {
				}
				eta = getLargestETA(dmsToWaitFor);

				if (eta <= 0 && !sentSetStreamReady
						&& (bhBefore == null || bhBefore.pageReady)) {
					sentSetStreamReady = true;
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							try {
								String script = "try { azClient.setStreamReady(true); } catch (e) { }";
								soPlayBeforeBrowser.getBrowser().execute(script);
								debug("sent setStreamReady");
							} catch (Exception e) {
								debug("setStreamReady failed " + e.toString());
							}
						}
					});
				}

				ok = !shell.isDisposed() && emp != null;
			}

			if (stats != this.stats) {
				debug("===============Done waiting for ETA: Stats already processed, thus we should stop waiting.");
				ok = false;
			} else {
				debug("===============Done waiting for ETA: " + eta + ", ok=" + ok);
			}

			if (!ok) {
				return;
			}

			if (stats != null) {
				stats.initialBufferReadyOn = SystemTime.getCurrentTime();
			}
		} else {
			if (bhBefore != null) {
				bhBefore.setToWaitText(contentTitle);
				bhBefore.setHeight(0, true);
			}
		}

		// Calculate file to run
		boolean bComplete = dm == null ? true : dm.isDownloadComplete(false);

		if (bComplete && dm != null) {
			long completedTime = dm.getDownloadState().getLongParameter(
					DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
			if (completedTime > 0 && completedTime > SystemTime.getOffsetTime(-5000)) {
				bComplete = false;
			}
		}

		if (!bComplete) {
			String url = PlayUtils.getMediaServerContentURL(dm);
			if (url != null) {
				runFile = url;
			}
		}

		if (dm != null) {
			if (bComplete) {
				// update runFile in case it's been moved
				calculatePrimaryRunFile();
			}
		}

		if (edm != null) {
			setMinBufferBytes(bufferSize);
			if (stats != null) {
				stats.dlRateAtPlay = dm.getStats().getDataReceiveRate();
			}
		}

		if (bhAfter != null) {
			bhAfter.setVisible(false, false);
			bhAfter.setURL(URL_PLAYAFTER, false, false, "");
		}

		if (bhBefore != null) {
			bhBefore.notifyGeneric("emplog(\"loadMedia\")");
		}
		debug("LM " + runFile);
		emp.loadMedia(runFile);
		waitFor = 0;
		soWaitText.setVisible(false);
	}

	/**
	 * @param semWaitASX
	 *
	 * @since 3.0.4.1
	 */
	private void createASX(final AESemaphore semWaitASX) {
		if (dm == null || primaryFile == null || !primaryFile.exists()) {
			if (semWaitASX != null) {
				semWaitASX.releaseForever();
			}
			return;
		}
	}

	/**
	 * @param dmsToWaitFor
	 * @return
	 *
	 * @since 3.0.2.3
	 */
	protected long getLargestETA(List dmsToWaitFor) {
		long maxETA = 0;

		if (stats != null && dmsToWaitFor.size() >= 1) {
			stats.numOtherTorrentsActive = dmsToWaitFor.size() - 1;
		}

		for (Iterator iter = dmsToWaitFor.iterator(); iter.hasNext();) {
			Object item = (Object) iter.next();
			long eta = 0;
			if (item instanceof DownloadManager) {
				DownloadManager dm = (DownloadManager) item;
				if (!dm.getAssumedComplete()) {
					TRTrackerAnnouncer trackerClient = dm.getTrackerClient();
					if (trackerClient instanceof TRTrackerServerDHT) {
						continue;
					}
					TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
					if (response != null
							&& (response.getStatus() == TRTrackerScraperResponse.ST_ERROR || response.getScrapeStartTime() > 0)) {
						if (response.getSeeds() <= 0) {
							continue;
						}
					}

					eta = dm.getStats().getETA();
					if (eta <= 0) {
						eta = 1;
					}
				}
			} else if (item instanceof EnhancedDownloadManager) {
				EnhancedDownloadManager edm = (EnhancedDownloadManager) item;
				DownloadManager dm = edm.getDownloadManager();
				if (!dm.getAssumedComplete()) {
					eta = edm.getProgressivePlayETA();
				}
			}

			if (eta > maxETA) {
				maxETA = eta;
			}
		}
		return maxETA;
	}

	public void semiClose() {
		EmpStats statsCopy;
		try {
			mon_stats.enter();
			statsCopy = stats;
			stats = null;
		} finally {
			mon_stats.exit();
		}

		if (dm != null) {
			dm.getGlobalManager().removeDownloadWillBeRemovedListener(this);
		}

		if (playEnablerTimer != null) {
			playEnablerTimer.cancel();
		}

		if (Utils.isThisThreadSWT()) {
			final AESemaphore sem = new AESemaphore("waitForSemiClose");
			final EmpStats fstatsCopy = statsCopy;
			new AEThread2("EMP.sPAV", true) {
				public void run() {
					_semiClose(sem, fstatsCopy);
				}
			}.start();
			sem.reserve();
		} else {
			_semiClose(null, statsCopy);
		}
	}

	private void _semiClose(AESemaphore sem, EmpStats stats) {
		debug("semiclose; emp=" + emp);

		if (emp != null) {
			emp.semiClose();
		}

		if (stats != null) {
			storeStats(stats);
		}

		if (dm != null) {

			if (dmListener != null) {
				dm.removeListener(dmListener);
				dmListener = null;
			}

			dm.removeTrackerListener(this);
		}

		try {
			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					torrent.getHash());
			if (edm.getProgressiveMode()) {
				edm.setProgressiveMode(false);
			}
		} catch (Exception e) {
		}

		if (sem != null) {
			sem.releaseForever();
		}

		if (!Constants.isOSX) {
			PluginInterface pi = EmbeddedPlayerWindowManager.getPluginInterface();
			if (pi != null) {
				pi.getPluginProperties().setProperty("plugin.unload.disabled", "false");
				pi.getPluginProperties().setProperty("plugin.silentrestart.disabled",
						"false");
			}
		}
	}

	/**
	 * @param stats
	 *
	 * @since 3.0.5.3
	 */
	private void storeStats(EmpStats stats) {
		if (stats == null || !isOurContent) {
			return;
		}
		long startedAgo = stats.startCalledOn > 0 ? SystemTime.getCurrentTime()
				- stats.startCalledOn : -1;
		if (startedAgo < 800) {
			debug("cancel QOS stats - closed in " + startedAgo + "ms of start call");
			return;
		} else {
			debug("QOS stats - closed in " + startedAgo + "ms of start call");
		}
		Map params = new HashMap();

		boolean quitEarly = (stats.initialBufferReadyOn <= 0 && stats.didWait)
				|| currentButtonState == BS_ADS || stats.maxSeekAheadSecs < 0;

		long waitTimeToEMP = stats.dmStartedOn <= 0
				|| stats.dmStartedOn > stats.startedOn ? 0 : stats.startedOn
				- stats.dmStartedOn;

		if (currentButtonState == BS_ADS && stats.maxSeekAheadSecs > 0) {
			stats.maxSeekAheadSecs *= -1;
		}

		if (quitEarly) {
			// negative for give up time
			long waitTime = stats.startedOn - SystemTime.getCurrentTime();
			params.put("wait-time", new Long(waitTime));
		} else if (stats.didWait) {
			// positive for wait time, 
			long waitTime = stats.initialBufferReadyOn - stats.startedOn;
			params.put("wait-time", new Long(waitTime));
		} else {
			params.put("wait-time", new Long(0));
		}

		long dlCap = 0;
		long ulCap = 0;

		try {
			SpeedManager sm = AzureusCoreFactory.getSingleton().getSpeedManager();
			if (sm != null) {
				SpeedManagerLimitEstimate dlCapO = sm.getEstimatedDownloadCapacityBytesPerSec();
				if (dlCapO != null) {
					dlCap = dlCapO.getBytesPerSec();
				}
				SpeedManagerLimitEstimate ulCapO = sm.getEstimatedUploadCapacityBytesPerSec();
				if (ulCapO != null) {
					ulCap = ulCapO.getBytesPerSec();
				}
			}
		} catch (Exception e) {
		}

		params.put("wait-time-to-emp", new Long(waitTimeToEMP));
		params.put("max-seek", new Long((long) stats.maxSeekAheadSecs));
		params.put("num-rebuffers", new Long(stats.getNumRebuffers()));
		params.put("rebuffer-ms", new Long(stats.getRebufferMS()));
		params.put("num-hard-rebuffers", new Long(stats.getNumHardRebuffers()));
		params.put("pp", new Boolean(stats.pp));
		params.put("video-type", stats.useWMP ? "wmv" : "x264");
		params.put("total-secs", new Long((long) stats.totalSecs));
		if (ulCap > 0) {
			params.put("ul-capacity", new Long(ulCap));
		}
		if (dlCap > 0) {
			params.put("dl-capacity", new Long(dlCap));
		}
		params.put("streamed", new Boolean(stats.isDownloadStreaming));
		if (emp != null && !quitEarly) {
			float frameDropPct = emp.getFrameDropPct();
			if (frameDropPct >= 0) {
				params.put("frame-drop-pct", new Double(frameDropPct));
			}
		}

		if (rebufferStartTime > 0) {
			stats.increaseRebufferMS(SystemTime.getCurrentTime() - rebufferStartTime);
			rebufferStartTime = 0;
		}

		params.put("rebuffer-info", stats.rebufferList);
		params.put("platform", SWT.getPlatform());

		if (stats.forcedWaitTime > 0) {
			params.put("force-wait-time-ms", new Long(stats.forcedWaitTime));
		}
		if (bhBefore != null && bhBefore.goodTitleSetOn > 0
				&& bhBefore.loadURLStartedOn > 0
				&& bhBefore.goodTitleSetOn >= bhBefore.loadURLStartedOn) {
			long diff = bhBefore.goodTitleSetOn - bhBefore.loadURLStartedOn;
			if (diff > 0) {
				params.put("force-page-wait-time-ms", new Long(diff));
			}
		}

		if (stats.playWaitAbortedReason != null) {
			params.put("abort-pre-playback", stats.playWaitAbortedReason);
		}

		if (stats.isDownloadStreaming) {
			if (waitTimeToEMP < 10000) {
				if (stats.got1stPeerOn > 0) {
					params.put("1st-peer-ms", new Long(stats.got1stPeerOn
							- stats.dmStartedOn));
				}
				if (stats.got1stConnectedPeerOn > 0) {
					params.put("1st-connected-peer-ms", new Long(
							stats.got1stConnectedPeerOn - stats.dmStartedOn));
				}
				if (stats.got1stDataOn > 0) {
					params.put("1st-data-ms", new Long(stats.got1stDataOn
							- stats.dmStartedOn));
				}
				if (stats.got1stHandshakedPeerOn > 0) {
					params.put("1st-handshaked-peer-ms", new Long(
							stats.got1stHandshakedPeerOn - stats.dmStartedOn));
				}
				if (stats.metStreamSpeedAt > 0) {
					params.put("met-stream-speed-ms", new Long(stats.metStreamSpeedAt
							- stats.dmStartedOn));
				}
				if (stats.met25StreamSpeedAt > 0) {
					params.put("met-25pct-stream-speed-ms", new Long(
							stats.met25StreamSpeedAt - stats.dmStartedOn));
				}
				if (stats.met50StreamSpeedAt > 0) {
					params.put("met-50pct-stream-speed-ms", new Long(
							stats.met50StreamSpeedAt - stats.dmStartedOn));
				}
				if (stats.met75StreamSpeedAt > 0) {
					params.put("met-75pct-stream-speed-ms", new Long(
							stats.met75StreamSpeedAt - stats.dmStartedOn));
				}

				if (stats.startedTrackerAnnounceOn > 0) {
					long diff = stats.startedTrackerAnnounceOn - stats.dmStartedOn;
					if (diff < 0) {
						diff = 0;
					}
					params.put("tracker-announce-start-ms", new Long(diff));
				}
				if (stats.completedTrackerAnnounceOn > 0) {
					params.put("tracker-announce-completed-ms", new Long(
							stats.completedTrackerAnnounceOn - stats.dmStartedOn));
				}
			}

			long count = dlAvgList.size();
			if (count > 0) {
				// 0.5 for rounding
				double ttlDLRate = 0.5;
				double ttlPeers = 0.5;
				double ttlSeeds = 0.5;
				long countS = 0;
				long countP = 0;
				long[] speedAt10Secs = new long[3];
				int speedAtSecs10Pos = 0;
				for (Iterator iter = dlAvgList.iterator(); iter.hasNext();) {
					DLSnapshot snap = (DLSnapshot) iter.next();
					if (speedAtSecs10Pos < speedAt10Secs.length
							&& snap.timestamp - stats.dmStartedOn >= (speedAtSecs10Pos + 1) * 10000) {
						speedAt10Secs[speedAtSecs10Pos] = snap.dlRate;
						speedAtSecs10Pos++;
					}
					ttlDLRate += snap.dlRate;
					if (snap.numPeers >= 0) {
						ttlPeers += snap.numPeers;
						countP++;
					}
					if (snap.numSeeds >= 0) {
						ttlSeeds += snap.numSeeds;
						countS++;
					}
				}
				params.put("avg-dl-Bps", new Long((long) (ttlDLRate / count)));
				if (countP > 0) {
					params.put("avg-peers", new Long((long) (ttlPeers / countP)));
				}
				if (countS > 0) {
					params.put("avg-seeds", new Long((long) (ttlSeeds / countS)));
				}
				for (int i = 0; i < speedAtSecs10Pos; i++) {
					params.put("speed-at-" + (i + 1) + "0-secs", new Long(
							speedAt10Secs[i]));
				}
			}
			if (stats.dlRateAtPlay > 0) {
				params.put("at-play-dl-Bps", new Long(stats.dlRateAtPlay));
			}

			if (stats.streamableUnbufferedOn > stats.dmStartedOn) {
				params.put("streamable-unbuffered-ms", new Long(
						stats.streamableUnbufferedOn - stats.dmStartedOn));
			}
		}

		if (stats.streamSpeed > 0) {
			params.put("stream-speed-Bps", new Long(stats.streamSpeed));
		}

		params.put("plugin-version", EmbeddedPlayerWindowManager.getPluginVersion());
		params.put("os", org.gudy.azureus2.core3.util.Constants.OSName);

		if (!quitEarly) {
			params.put("used-fullscreen", new Boolean(stats.usedFullScreen));
		}

		params.put("started-ago-ms", new Long(SystemTime.getCurrentTime()
				- stats.startedOn + 3000));

		if (stats.numOtherTorrentsActive >= 0) {
			params.put("other-torrents-active",
					new Long(stats.numOtherTorrentsActive));
		}

		params.put("version", LOG_VERSION);
		if (emp != null) {
			params.put("vo", emp.getVO());
		}
		PlatformTorrentMessenger.streamComplete(torrent, params);
	}

	public void close() {
		alreadyRunningWin = null;

		semiClose();

		try {
			if (timePollerTimer != null) {
				timePollerTimer.cancel();
				timePollerTimer = null;
			}
		} catch (Exception e) {
		}

		try {
			if (emp != null) {
				emp.delete();
			}
			emp = null;
		} catch (Exception e) {
		}

		if (shell != null && !shell.isDisposed()) {
			Utils.execSWTThreadWithBool("close EMP", new AERunnableBoolean() {

				public boolean runSupport() {
					try {
						shell.dispose();
					} catch (SWTException e) {
						Debug.out("warning while disposing", e);
					}
					return true;
				}
			});
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.1.7
	 */
	private void waitUntilClosed(Display d) {
		while (shell == null) {
			if (!d.readAndDispatch()) {
				d.sleep();
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch()) {
				shell.getDisplay().sleep();
			}
		}
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmpListenerFileChanged#EmpFileChanged(java.lang.String)
	public void empFileChanged(String nowPlaying) {
		// don't emp_mon.enter() on a listener trigger, since it may cause a deadlock

		// mplayer will return -1 :(
		if (stats != null) {
			stats.totalSecs = emp == null ? -1 : emp.getLength();
		}

		if (nowPlaying != null) {
			// fix bug:
			nowPlaying = nowPlaying.replaceAll("file:/+", "/");
			isCurrentIncompleteStream = nowPlaying.startsWith("http://")
					&& !dm.getAssumedComplete();
		} else {
			isCurrentIncompleteStream = false;
		}
		nowPlayingFile = nowPlaying;

		debug("Emp File Changed to : " + nowPlaying + ";len="
				+ (stats == null ? -1 : stats.totalSecs) + ";Fullscreen?" + fullscreen);

		if (stats != null) {
			stats.maxSeekAheadSecs = -1;
		}
		changeButtonDisableState(BS_ENABLE);

		if (bhBefore != null && stats != null) {
			bhBefore.notifyVideoPlaying(0, (long) stats.totalSecs);
		}

		boolean noSeekAhead = isCurrentIncompleteStream;
		emp.setAllowSeekAhead(!noSeekAhead);
		if (soTimeSlider != null) {
			soTimeSlider.setDisabled(noSeekAhead);
		}

		int[] realVideoResolution = emp.getVideoResolution();
		if (realVideoResolution != null) {
			videoResolution = realVideoResolution;
		}

		if (emp != null && !emp.isStopped() && !emp.isPaused()) {
			setState(STATE_PLAYING);
		}

		if (videoResolution != null && RESIZE_ON_PLAY && !initSizeSet
				&& !fullscreen) {
			setShellSizePct(1, new Point(MIN_PLAYING_AUTO_WIDTH,
					MIN_PLAYING_AUTO_HEIGHT));
			initSizeSet = true;
		}

		updateUI();
	}

	// @see org.gudy.azureus2.core3.global.GlobalManagerDownloadWillBeRemovedListener#downloadWillBeRemoved(org.gudy.azureus2.core3.download.DownloadManager, boolean, boolean)
	public void downloadWillBeRemoved(DownloadManager manager,
			boolean remove_torrent, boolean remove_data)
			throws GlobalManagerDownloadRemovalVetoException {
		if (dm == null) {
			return;
		}
		if (dm.equals(manager)) {
			try {
				semiClose();

				// delete emp object first in case it's holding a file lock
				if (emp != null) {
					emp.delete();
					emp = null;
				}

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (shell != null && !shell.isDisposed()) {
							shell.close();
						}
					}
				});
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmpListenerAudio#audioInitialized()
	public void audioInitialized() {
		if (emp == null) {
			return;
		}
		int volume = COConfigurationManager.getIntParameter("ep.volume", 100);
		soVolumeSlider.setPercent(volume / 100.0);
		emp.setVolume(volume);

		boolean nowMuted = COConfigurationManager.getBooleanParameter("ep.mute",
				false);
		emp.setMuted(nowMuted);
		emp.sendCommand("pausing_keep osd 0");

		updateMuteButton(nowMuted);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmpListenerAudio#audioChanged(int)
	public void audioChanged(final int volume) {
		if (soVolumeSlider != null) {
			soVolumeSlider.setPercent(volume / 100.0);
		}
		if (volume > 0 && lastMute) {
			muteChanged(true);
		}

		if (bhBefore != null) {
			bhBefore.setVolume(volume);
		}
		if (bhAfter != null) {
			bhAfter.setVolume(volume);
		}
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmpListenerAudio#muteChanged(boolean)
	public void muteChanged(final boolean mute) {
		updateMuteButton(mute);

		if (bhBefore != null) {
			bhBefore.setMute(mute);
		}
		if (bhAfter != null) {
			bhAfter.setMute(mute);
		}
	}

	private void setShellSizePct(final double d, final Point minPlayerSize) {
		if (videoResolution == null || fullscreen) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (shell == null || shell.isDisposed() || videoResolution == null) {
					return;
				}

				int x = (int) (videoResolution[0] * d);
				int y = (int) (videoResolution[1] * d);
				if (soTop != null && soTop.isVisible()) {
					y += soTop.getControl().getSize().y;
				}
				if (soBottom != null && soBottom.isVisible()) {
					y += soBottom.getControl().getSize().y;
				}
				if (bhBefore != null) {
					if (bhBefore.getHeight() > 0) {
						y += bhBefore.getHeight();
					}
					if (bhBefore.getWidth() > 0) {
						x += bhBefore.getWidth();
					}
				}
				Rectangle clientArea = shell.getClientArea();
				Rectangle bounds = shell.getBounds();
				y += (bounds.height - clientArea.height);
				x += (bounds.width - clientArea.width);
				if (minPlayerSize != null) {
					if (x < minPlayerSize.x) {
						x = minPlayerSize.x;
					}
					if (y < minPlayerSize.y) {
						y = minPlayerSize.y;
					}
				}
				shell.setSize(x, y);
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmpListenerTimeout#empTimeout()
	public void empTimeout() {
		setState(STATE_BUFFERING);
	}

	public void abortPrePlaybackWait(String reason) {
		if (stats == null || !stats.playWait) {
			return;
		}
		debug("abort playWait: " + reason);
		setPrePlaybackWait(false);
		stats.playWaitAbortedReason = reason;
		try {
			bhBefore.setToWaitText(contentTitle);
			SWTSkinObject so = skin.getSkinObject("test4");
			if (isCVS && (so instanceof SWTSkinObjectText)) {
				((SWTSkinObjectText) so).setText("EMP v"
						+ EmbeddedPlayerWindowManager.getPluginVersion()
						+ " ABORTED PRE-PLAYBACK: " + reason);
			}
		} catch (Throwable t) {
			Debug.out(t);
		}
	}

	// @see com.azureus.plugins.azemp.EmbeddedPlayerWindow#setPrePlaybackWait(boolean)
	public void setPrePlaybackWait(boolean wait) {
		if (stats == null || stats.playWait == wait) {
			return;
		}
		if (wait) {
			stats.playWaitAbortedReason = null;
		}
		stats.playWait = wait;
		long now = SystemTime.getCurrentTime();
		if (stats != null) {
			if (stats.playWait) {
				stats.lastForcedWaitTime = now;
			} else {
				stats.forceWaitClearedOn = SystemTime.getCurrentTime();
				if (stats.lastForcedWaitTime > 0) {
					stats.forcedWaitTime = SystemTime.getCurrentTime()
							- stats.lastForcedWaitTime;
				}
			}
		}
		debug("playwait set to " + stats.playWait);
	}

	// @see com.azureus.plugins.azemp.EmbeddedPlayerWindow#setMinSize(int, int)
	public void setMinSize(final int width, final int height) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (shell != null && !shell.isDisposed()) {
					shell.setMinimumSize(width, height);
				}
			}
		});
	}

	public void fullScreenChanged(final boolean isFullScreen) {
		debug("FS changed to " + isFullScreen);
		fullscreen = isFullScreen;
		if (isFullScreen) {
			stats.usedFullScreen = true;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (USE_SWT_FULLSCREEN) {
					SWTSkinUtils.setVisibility(skin, null, "ep-bottom", !isFullScreen,
							false, true);
					if (bhAfter == null || !bhAfter.isBrowserVisible()) {
						final boolean newVisibility = isFullScreen ? false
								: COConfigurationManager.getBooleanParameter("ep.topbar", true);
						SWTSkinUtils.setVisibility(skin, null, "ep-top", newVisibility,
								false, fastRolling);
						updateFlipToolbarButton(newVisibility, isFullScreen);
					}
				}
				updateUISWT();
			}
		});
		if (emp != null) {
			if (fullscreen) {
				emp.sendCommand("pausing_keep sub_alignment 0");
				emp.sendCommand("pausing_keep osd_show_text \"Press 'Esc' to exit fullscreen\" 3000 0");
			} else {
				emp.sendCommand("pausing_keep osd_show_text \"\"");
			}
		}
		if (bhBefore != null) {
			bhBefore.notifyFullScreenChange(isFullScreen);
		}
	}

	protected int getState() {
		return state;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}

	/**
	 * @param extraParams 
	 * @return
	 *
	 * @since 3.1.0.1
	 */
	public String getURLSuffix(String extraParams) {
		boolean mute = COConfigurationManager.getBooleanParameter("ep.mute", false);
		int volume = COConfigurationManager.getIntParameter("ep.volume", 100);

		String s = "&streaming="
				+ (stats != null && stats.isDownloadStreaming ? "1" : "0") + "&mute="
				+ (mute || volume < 1 ? "1" : "0") + "&v="
				+ EmbeddedPlayerWindowManager.getPluginVersion() + "&rnd="
				+ Math.random() + extraParams;
		return s;
	}

	public boolean isOnBaseURL(String prefix, String url) {
		if (url == null) {
			return false;
		}
		String s = prefix;

		if (dm != null && PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
			s += platformTorrentHash;
		} else {
			s += "default";
		}
		return url.startsWith(s);
	}

	public boolean isPlayWaitAborted() {
		return stats == null || stats.playWaitAbortedReason != null;
	}

	public int[] getVideoResolution() {
		return videoResolution;
	}

	public void setVideoResolution(int[] videoResolution) {
		this.videoResolution = videoResolution;
	}
}
