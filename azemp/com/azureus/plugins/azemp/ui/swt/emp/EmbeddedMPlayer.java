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

import java.io.*;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.azureus.plugins.azemp.EmbeddedMediaPlayerPlugin;
import com.azureus.plugins.azemp.EmbeddedPlayerWindowManager;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

/**
 * @author TuxPaper
 * @created Aug 9, 2007
 *
 */
public class EmbeddedMPlayer
	implements EmbeddedMediaPlayer
{
	private static final boolean DEBUG_TO_LOGGER = false;

	private static final boolean DEBUG_TO_STDOUT = false;

	private static boolean USE_SWT_FULLSCREEN;

	private static boolean DEBUG_COMMANDS = true;

	private static int COMMAND_WAIT_MS = 700;

	private static AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.emp");

	private OutputStream outputStream;

	private InputStream inputStream;

	private InputStream errorStream;

	private Process exec;

	// key = ID; value=replyListener
	private Map mapListeners = new HashMap();

	private AEMonitor mapListeners_mon = new AEMonitor("mapListeners");

	private float lastPositionSecs = -1;

	private float lastPositionPercent = -1;

	private float lastLength = -1;

	private AESemaphore semLoadWait = new AESemaphore("LoadWait");

	private AESemaphore semStdOut = new AESemaphore("StdOut");

	private AESemaphore semWaitFor = null;

	private String waitForString;

	private long lastBuffStart = 0;

	private ArrayList commandList = new ArrayList();

	private AEMonitor commandList_mon = new AEMonitor("commandList");

	private String lastFilePlaying;

	private ArrayList listenersFC = new ArrayList();

	private ArrayList listenersAudio = new ArrayList();

	private ArrayList listenersTimeout = new ArrayList();

	private ArrayList listenersFullScreen = new ArrayList();

	private LoggerChannel logger;

	private boolean stopped = true;

	private long lastGoodStreamPos;

	private Composite control;

	private boolean isFullScreen = false;

	private ArrayList listenersSemiClose = new ArrayList();

	private boolean wasPaused = false;

	private int lastVolume = 100;

	private boolean deleted;

	private boolean useExternalPlayer;
	private String  externalPlayer;
	
	String voID = null;
	
	public EmbeddedMPlayer(boolean useExternalPlayer,String externalPlayer) {
		this.useExternalPlayer = useExternalPlayer;
		this.externalPlayer = externalPlayer;
	}
	
	
	// @see com.aelitis.azureus.ui.swt.emp.EmbeddedMediaPlayer#init(org.eclipse.swt.widgets.Composite)
	public void init(Composite c, boolean useSWTFullScreen) throws Throwable {
		this.USE_SWT_FULLSCREEN = useSWTFullScreen;

		control = new Composite(c, SWT.NONE);
		control.setLayoutData(Utils.getFilledFormData());

		if (Constants.isOSX) {
			voID = EmbeddedMPlayerOSXUtils.initMPlayerOSX(this, control,
					USE_SWT_FULLSCREEN);
		} else {
			Color color = new Color(control.getDisplay(), 0x1, 0x2, 0x3);
			control.setBackground(color);
		}

		final Listener l = new Listener() {
			private boolean mouseDoubleClick = false;

			public void handleEvent(Event event) {
				if (control == null || control.isDisposed() || !control.isVisible()) {
					return;
				}

				// only pause/doubleclick if click was in bounds of control
				Point size = control.getSize();
				Point mousePos = control.toControl(control.getDisplay().getCursorLocation());
				if (mousePos.x < 0 || mousePos.y < 0 || mousePos.x >= size.x
						|| mousePos.y >= size.y) {
					return;
				}

				Shell shell = control.getShell();
				if (shell.isDisposed()) {
					return;
				}
				if (event.widget instanceof Shell && event.widget != shell) {
					return;
				}
				if (event.widget instanceof Control) {
					if (((Control) event.widget).getShell() != shell) {
						return;
					}
				} else {
					return;
				}

				if (event.type == SWT.MouseDoubleClick) {
					mouseDoubleClick = true;
					if (Constants.isOSX && !USE_SWT_FULLSCREEN) {
						// handled already
						triggerFullScreenListeners(!isFullScreen);
						return;
					}
					if (stopped && !isFullScreen) {
						return;
					}
					runAction(EmbeddedMediaPlayer.ACTION_FLIP_FULLSCREEN);
				} else {
					mouseDoubleClick = false;

					event.display.timerExec(event.display.getDoubleClickTime(),
							new AERunnable() {
								public void runSupport() {
									if (mouseDoubleClick) {
										return;
									}

									runAction(EmbeddedMediaPlayer.ACTION_FLIP_PAUSE);
									System.out.println("flipp");
								}
							});
				}
			}
		};
		control.getDisplay().addFilter(SWT.MouseDoubleClick, l);
		control.getDisplay().addFilter(SWT.MouseDown, l);

		control.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				if (e.display == null || e.display.isDisposed()) {
					return;
				}
				e.display.removeFilter(SWT.MouseDoubleClick, l);
				e.display.removeFilter(SWT.MouseDown, l);
			}
		});

		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				delete();
			}
		});

		PluginInterface pi = EmbeddedPlayerWindowManager.getPluginInterface();

		if (voID == null) {
			voID = pi == null ? "default"
					: pi.getPluginconfig().getPluginStringParameter(
							EmbeddedMediaPlayerPlugin.CFG_VIDEO_OUT, "default");
		}
		
		if (voID.equals("default") && Constants.isWindows) {
			voID = "direct3d";
		}

		try {
			if (Constants.isOSX) {
				
				
				File app = new File(EmbeddedPlayerWindowManager.getPluginDir(),
						"azmplay");
				
				if(useExternalPlayer && externalPlayer != null) {
					app = new File(externalPlayer);
				}
								
				if (!app.exists()) {
					throw new Exception("No azmplay in " + app.getAbsolutePath());
				}
				
				debug("About to play with " + app.getAbsolutePath());

				runProcess("killAll", new String[] {
					"/usr/bin/killall",
					"-9",
					"azmplay"
				});

				runProcess("chmod", new String[] {
					"/bin/chmod",
					"+x",
					app.getAbsolutePath().replaceAll(" ", "\\ ")
				});

				exec = Runtime.getRuntime().exec(new String[] {
					app.getAbsolutePath(),
					"-idle",
					"-quiet",
					"-slave",
					"-fixed-vo",
					"-framedrop",
					"-vo",
					//"macosx"
					(voID.equals("default") ? "swt" : voID),
					"-font",
					EmbeddedPlayerWindowManager.getPluginDir() + File.separator + "font.desc",
				});
			} else {
				long handle = (long) control.handle;

				File app = new File(EmbeddedPlayerWindowManager.getPluginDir(),
						"azmplay.exe");
				
				if(useExternalPlayer && externalPlayer != null) {
					app = new File(externalPlayer);
				}
				
				if (!app.exists()) {
					throw new Exception("No azmplay.exe in " + app.getAbsolutePath());
				}

				String[] cmdarray = new String[] {
					app.getAbsolutePath(),
					"-idle",
					"-quiet",
					"-slave",
					"-fixed-vo",
					"-framedrop",
					"-vo",
					voID,
					"-lavdopts",
					"fast",
					"-doubleclick-time",
					"0",
					"-colorkey",
					"0x030201",
					"-wid",
					"" + handle,
					"-font",
					new File(EmbeddedPlayerWindowManager.getPluginDir(), "font.desc").getAbsolutePath(),
				};
				exec = Runtime.getRuntime().exec(cmdarray);
			}

			inputStream = exec.getInputStream();
			outputStream = exec.getOutputStream();
			errorStream = exec.getErrorStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new AEThread("MPlayer in", true) {
			byte[] buffer = new byte[1024];

			String line = "";

			public void runSupport() {
				while (exec != null) {
					if (inputStream != null) {
						int i;
						try {
							int count = inputStream.available();
							while (count > 0) {
								if (count > buffer.length) {
									count = buffer.length;
								}
								i = inputStream.read(buffer, 0, count);
								String newText = new String(buffer, 0, count);

								if (newText.indexOf('\n') >= 0) {
									boolean endInCR = newText.endsWith("\n");

									String[] split = newText.split("\n");
									if (split.length > 0) {
										gotLine(line + split[0]);

										if (split.length > 1) {
											for (int j = 1; j < split.length; j++) {
												if (semLoadWait != null
														&& !semLoadWait.isReleasedForever()
														&& split[j].startsWith("MPlayer")) {
													semLoadWait.releaseForever();
												}
												gotLine(split[j]);
											}
										}
									}
									if (endInCR) {
										line = "";
									} else {
										line = split[split.length - 1];
									}
								}

								count = inputStream.available();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (errorStream != null) {
						int i;
						try {
							String s = "";
							while (errorStream.available() > 0) {
								i = errorStream.read();
								if (i < 32) {
									if (s.length() > 0) {
										debug("ERR: " + s);
										s = "";
									}
									System.err.println("");
								} else {
									System.err.print((char) i);
									s += (char) i;
								}
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					try {
						this.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();

		semLoadWait.reserve(2000);

		new AEThread("stdin", true) {
			String line = "";

			public void runSupport() {
				while (exec != null) {
					int i;
					try {
						while (System.in.available() > 0) {
							i = System.in.read();

							if (i == '\n') {
								sendCommand(line);
								line = "";
							} else if (i == 8) {
								line = line.substring(0, line.length() - 2);
							} else {
								line += (char) i;
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
					try {
						this.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();

		new AEThread("MPlayer out", true) {
			public void runSupport() {
				while (!semStdOut.isReleasedForever()) {
					semStdOut.reserve();

					while (isBuffering()) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
						}
					}
					commandList_mon.enter();
					try {

						if (commandList.size() == 0) {
							continue;
						}

						for (Iterator iter = commandList.iterator(); iter.hasNext();) {
							String s = (String) iter.next();

							try {
								outputStream.write(s.getBytes());
								outputStream.write('\n');
								if (DEBUG_COMMANDS) {
									debug("sending: " + s);
								}
							} catch (IOException e) {
								// we should probably assume mplayer has closed
								System.err.println("SendCommand Error: " + e.toString());
							}

							iter.remove();
						}
					} finally {
						commandList_mon.exit();
					}

					// flush outside of monitor lock..
					try {
						outputStream.flush();
						if (DEBUG_COMMANDS) {
							debug("commands flushed");
						}
					} catch (IOException e) {
						// we should probably assume mplayer has closed
						System.err.println("SendCommand Error: " + e.toString());
					}
				}
			}
		}.start();
	}

	private void runProcess(final String name, String[] params) {
		Process process;
		try {
			process = Runtime.getRuntime().exec(params);
		} catch (IOException e1) {
			debug("running " + name + " Error: " + e1.toString());
			return;
		}

		final InputStream es = process.getErrorStream();
		new AEThread(name + ":es", true) {
			String s = "";

			public void runSupport() {
				while (true) {
					try {
						while (es.available() > 0) {
							s += (char) es.read();
						}
						Thread.sleep(20);
					} catch (Exception e) {
						break;
					}
				}
				if (s.length() > 0) {
					debug(name + " stderr: " + s);
				}
			}
		}.start();
		final InputStream is = process.getInputStream();
		new AEThread(name + ":is", true) {
			String s = "";

			public void runSupport() {
				while (true) {
					try {
						while (is.available() > 0) {
							s += (char) is.read();
						}
						Thread.sleep(20);
					} catch (Exception e) {
						break;
					}
				}
				if (s.length() > 0) {
					debug(name + " stdout: " + s);
				}
			}
		}.start();

		try {
			process.waitFor();
			Thread.sleep(20);
			process.destroy();
		} catch (Exception e) {
			debug(name + " Error: " + e.toString());
			// ignore
		}
	}

	public void delete() {
		if (deleted) {
			return;
		}
		deleted = true;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (control != null && !control.isDisposed()) {
					control.dispose();
				}
			}
		});

		if (exec != null) {
			try {
				sendCommand("quit");
				waitFor("Exit player", 2000);
				// Even though "Exit player is just before the exit(rc) in mplayer,
				// wait 1/10 sec to make sure it closes.  Otherwise, at least on
				// OSX, exec.destroy will fail and leave the process open forever
				Thread.sleep(50);
			} catch (Exception e) {
			}

			try {
				exec.destroy();
			} catch (Exception e) {
			}
			exec = null;
		} else {
			debug("delete: exec already null");
		}

		listenersAudio.clear();
		listenersFC.clear();
		listenersFullScreen.clear();
		listenersSemiClose.clear();
		listenersTimeout.clear();
		commandList_mon.enter();
		try {
			commandList.clear();
		} finally {
			commandList_mon.exit();
		}

		semStdOut.releaseForever();
	}

	public void gotLine(String line) {
		if (exec == null) {
			return;
		}
		line = line.replaceAll("\r", "");

		if (line.indexOf(">>BUF") >= 0) {
			lastBuffStart = System.currentTimeMillis();
			return;
		} else if (line.indexOf("<<BUF") >= 0) {
			lastBuffStart = 0;
			return;
		}

		if (DEBUG_COMMANDS) {
			debug("Got Line," + line.length() + ": " + line);
		}

		if (line.startsWith("!!TIMEOUT")) {
			lastBuffStart = 1;
			triggerTimeoutListeners();
		} else if (line.startsWith(">>Play")) {
			lastFilePlaying = line.substring(7);
			lastBuffStart = 0;
			stopped = false;
			triggerFCListeners(lastFilePlaying);
		} else if (line.startsWith("!!Stopped")) {
			lastBuffStart = 0;
			stopped = true;
		} else if (line.startsWith("!!Audio Init")) {
			triggerAudioListeners();
		} else if (line.startsWith("!!Volume") && line.length() > 10) {
			String s = line.substring(10);
			try {
				int newVolume = Integer.parseInt(s);
				triggerVolumeListeners(newVolume);
			} catch (Exception e) {
				debug("Can't parse volume: " + s);
			}
		} else if (line.startsWith("!!Mute") && line.length() > 8) {
			try {
				boolean mute = line.charAt(8) == '1';
				triggerMuteListeners(mute);
			} catch (Exception e) {
				debug("Can't parse mute");
			}
		} else if (line.startsWith("!!Fullscreen ")) {
			triggerFullScreenListeners(line.charAt(13) == '1');
		}

		if (semWaitFor != null && !semWaitFor.isReleasedForever()
				&& line.startsWith(waitForString)) {
			if (DEBUG_COMMANDS) {
				debug("got what we were waiting for");
			}
			semWaitFor.releaseForever();
		}

		if (line.startsWith("ANS_")) {
			int i = line.indexOf('=');
			if (i > 0) {
				String id = line.substring(0, i);
				String value = line.substring(i + 1);
				if (value.endsWith("\r")) {
					value = value.substring(0, value.length() - 1);
				}

				ArrayList list = (ArrayList) mapListeners.get(id);
				if (list != null) {
					Object[] arrayListeners = list.toArray();
					for (int j = 0; j < arrayListeners.length; j++) {
						resultListener l = (resultListener) arrayListeners[j];
						try {
							l.receivedAnswer(id, value);
						} catch (Exception e) {
							Debug.out(e);
						}
					}
				}
			}
		}
	}

	public float getFrameDropPct() {
		return getFloatProperty("percent_drop_frame");
	}

	/**
	 * 
	 * @param waitForString
	 * @param maxWait
	 * @return  true: got string; false: timeout
	 */
	public boolean waitFor(String waitForString, long maxWait) {
		this.waitForString = waitForString;
		semWaitFor = new AESemaphore("waitFor " + waitForString);
		boolean gotString = semWaitFor.reserve(maxWait);
		if (!gotString) {
			debug("Timed out waiting " + maxWait + " for `" + waitForString + "`");
		}
		return gotString;
	}

	// @see com.aelitis.azureus.ui.swt.emp.EmbeddedMediaPlayer#runAction(int)
	public void runAction(final int action) {
		if (action == ACTION_FLIP_PAUSE) {
			sendCommand("pause");
		} else if (action == ACTION_PAUSE) {
			sendCommand("pause_media");
		} else if (action == ACTION_PLAY) {
			sendCommand("unpause_media");
		} else if (action == ACTION_FLIP_MUTE) {
			sendCommand("pausing_keep mute");
		} else if (action == ACTION_FLIP_FULLSCREEN) {
			flipFullScreen();
		}
	}

	private void flipFullScreen() {
		debug("Flip fullscreen mode to " + !isFullScreen);
		final boolean wasFullScreen = isFullScreen;
		if (USE_SWT_FULLSCREEN) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (control == null || control.isDisposed()) {
						return;
					}
					if (Constants.isOSX) {
						control.setFocus();
					}
					control.getShell().setFullScreen(!wasFullScreen);
					triggerFullScreenListeners(control.getShell().getFullScreen());
				}
			});
		} else {
			//"pausing_keep set_property fullscreen 1" is actually a toggler (bug)
			// use step for clarity
			sendCommand("pausing_keep set_property fullscreen 1");
			// could be on swt thread, so don't bother with getBooleanProperty
			triggerFullScreenListeners(!isFullScreen);
		}
	}

	public void seekAbsolute(float seconds) {
		//sendCommand("pausing_keep seek " + seconds + " 2");
		sendCommand("seek " + seconds + " 2");
	}

	public void seekPercent(float percent) {
		//sendCommand("pausing_keep seek " + percent + " 1");
		sendCommand("seek " + percent + " 1");
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#seekRelative(float)
	public void seekRelative(float relativeSeconds) {
		sendCommand("seek " + relativeSeconds + " 0");
	}

	public int getVolume() {
		return lastVolume;
	}

	public void setVolume(int val) {
		sendCommand("pausing_keep set_property volume " + val);
		// TODO: Handle Failed to set property 'volume' to 'val'.
	}

	public void setVolumeRelative(int rel) {
		sendCommand("pausing_keep step_property volume " + rel);
	}

	public void setMuted(boolean mute) {
		sendCommand("pausing_keep set_property mute " + (mute ? "1" : "0"));
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#setAllowSeekAhead(boolean)
	public void setAllowSeekAhead(boolean b) {
		sendCommand("pausing_keep set_property allow_seek_ahead " + (b ? "1" : "0"));
	}

	public float getLength() {
		lastLength = getFloatProperty("length");

		return lastLength;
	}

	public float getPositionSecs() {
		if (stopped) {
			lastPositionSecs = -1;
		} else {
			float positionSecs = getFloatProperty("time_pos");
			if (positionSecs >= 0) {
				lastPositionSecs = positionSecs;
			}
		}

		return lastPositionSecs;
	}

	public float getPositionPercent() {
		if (stopped) {
			lastPositionPercent = -1;
		} else {
			float positionPct = getFloatProperty("percent_pos");
			if (positionPct >= 0) {
				lastPositionPercent = positionPct;
			}
		}

		return lastPositionPercent;
	}

	public boolean isFullScreen() {
		return isFullScreen;
	}

	public boolean loadMedia(String mediaLocation) {
		if (exec == null) {
			return false;
		}

		String cmd;
		if (mediaLocation.indexOf(".asx") > 0) {
			cmd = "loadlist";
		} else {
			cmd = "loadfile";
			mediaLocation.replaceFirst("file:/+/", "file://");
		}
		String sFile = mediaLocation.replaceAll("\\\\", "\\\\\\\\");
		sendCommand(cmd + " \"" + sFile + "\"");

		waitFor(">>Play", 30000);

		return true;
	}

	public String getFileName() {
		return getStringProperty("filename");
	}

	public long getStreamPos() {
		long streamPos = (long) getFloatProperty("stream_pos");
		if (streamPos >= 0) {
			lastGoodStreamPos = streamPos;
		}
		return lastGoodStreamPos;
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getVideoResolution()
	public int[] getVideoResolution() {
		String val = getStringValue("get_video_resolution", "ANS_VIDEO_RESOLUTION");
		if (val == null || val.indexOf('x') <= 0) {
			return null;
		}
		if (val.charAt(0) == '\'') {
			val = val.substring(1, val.length() - 1);
		}
		String[] split = val.split(" x ");
		try {
			int[] ret = {
				Integer.parseInt(split[0]),
				Integer.parseInt(split[1])
			};
			return ret;
		} catch (Throwable t) {
			return null;
		}
	}

	private String getStringProperty(String property) {
		if (exec == null) {
			return null;
		}

		final AESemaphore sem = new AESemaphore("getStringProperty");
		final String[] result = {
			null
		};
		addListener("ANS_" + property, new resultListener() {
			public void receivedAnswer(String id, String value) {
				result[0] = value;
				sem.releaseForever();
				removeListener(id, this);
			}
		});

		sendCommand("pausing_keep get_property " + property);

		sem.reserve(COMMAND_WAIT_MS);
		return result[0];
	}

	private String getStringValue(String command, String answerPrefix) {
		if (exec == null) {
			return null;
		}

		final AESemaphore sem = new AESemaphore("getStringProperty");
		final String[] result = {
			null
		};
		addListener(answerPrefix, new resultListener() {
			public void receivedAnswer(String id, String value) {
				result[0] = value;
				sem.releaseForever();
				removeListener(id, this);
			}
		});

		sendCommand("pausing_keep " + command);

		sem.reserve(COMMAND_WAIT_MS);
		return result[0];
	}

	private boolean getBooleanProperty(String property) {
		return getBooleanProperty(property, false);
	}

	private boolean getBooleanProperty(String property, boolean def) {
		if (exec == null) {
			return false;
		}

		final AESemaphore sem = new AESemaphore("getBooleanProperty");
		final boolean[] result = {
			def
		};
		addListener("ANS_" + property, new resultListener() {
			public void receivedAnswer(String id, String value) {
				result[0] = value.equals("yes");
				sem.releaseForever();
				removeListener(id, this);
			}
		});

		sendCommand("pausing_keep get_property " + property);

		if (!sem.reserve(COMMAND_WAIT_MS)) {
			debug("took too long to get " + property);
			System.err.println("took too long to get " + property);
		}
		return result[0];
	}

	private float getFloatProperty(String property) {
		if (exec == null) {
			return -1;
		}

		final AESemaphore sem = new AESemaphore("getStringProperty");
		final float[] result = {
			-1
		};
		addListener("ANS_" + property, new resultListener() {
			public void receivedAnswer(String id, String value) {
				try {
					result[0] = Float.parseFloat(value);
				} catch (Exception e) {
				}
				sem.releaseForever();
				removeListener(id, this);
			}
		});

		sendCommand("pausing_keep get_property " + property);

		if (!sem.reserve(COMMAND_WAIT_MS)) {
			debug("took too long to get " + property);
		}
		return result[0];
	}

	public boolean isPaused() {
		boolean paused = getBooleanProperty("paused", wasPaused);
		wasPaused = paused;
		return paused;
	}

	public boolean isMuted() {
		return getBooleanProperty("mute");
	}

	public void sendCommand(String s) {
		if (exec == null) {
			return;
		}

		commandList_mon.enter();
		try {
			commandList.add(s);
		} finally {
			commandList_mon.exit();
		}

		semStdOut.release();
	}

	public boolean isBuffering() {
		return lastBuffStart > 0
				&& System.currentTimeMillis() - lastBuffStart >= 10;
	}

	public void addListener(String id, resultListener listener) {
		if (Constants.isOSX && Utils.isThisThreadSWT()) {
			debug("addListener " + id + " on wrong thread!\n"
					+ Debug.getStackTrace(false, false));
		}
		mapListeners_mon.enter();
		try {
			ArrayList list = (ArrayList) mapListeners.get(id);
			if (list == null) {
				list = new ArrayList();
				list.add(listener);
			} else {
				list.add(listener);
			}
			mapListeners.put(id, list);
		} finally {
			mapListeners_mon.exit();
		}
	}

	public void removeListener(String id, resultListener listener) {
		mapListeners_mon.enter();
		try {
			ArrayList list = (ArrayList) mapListeners.get(id);
			if (list == null) {
				return;
			}
			list.remove(listener);
			if (list.size() == 0) {
				mapListeners.remove(id);
			}
		} finally {
			mapListeners_mon.exit();
		}
	}

	private interface resultListener
	{
		public void receivedAnswer(String id, String value);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#addListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerFileChanged)
	public void addListener(EmpListenerFileChanged l) {
		listenersFC.add(l);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#removeListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerFileChanged)
	public void removeListener(EmpListenerFileChanged l) {
		listenersFC.remove(l);
	}

	private void triggerFCListeners(final String s) {
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("FC", true) {
			public void runSupport() {
				if (exec == null) {
					return;
				}
				setExtraDropFrames(0);
				Object[] array = listenersFC.toArray();
				for (int i = 0; i < array.length; i++) {
					EmpListenerFileChanged l = (EmpListenerFileChanged) array[i];
					try {
						l.empFileChanged(s);
					} catch (Exception e) {
						Debug.out(s, e);
					}
				}
			}
		}.start();
	}

	public void setExtraDropFrames(long x) {
		sendCommand("pausing_keep set_property extra_drop_frames " + x);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#addListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerAudio)
	public void addListener(EmpListenerAudio l) {
		listenersAudio.add(l);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#removeListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerAudio)
	public void removeListener(EmpListenerAudio l) {
		listenersAudio.remove(l);
	}

	private void triggerAudioListeners() {
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("Audio", true) {
			public void runSupport() {
				if (exec == null) {
					return;
				}
				Object[] array = listenersAudio.toArray();
				for (int i = 0; i < array.length; i++) {
					EmpListenerAudio l = (EmpListenerAudio) array[i];
					try {
						l.audioInitialized();
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}
		}.start();
	}

	private void triggerMuteListeners(final boolean mute) {
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("Mute", true) {
			public void runSupport() {
				if (exec == null) {
					return;
				}

				Object[] array = listenersAudio.toArray();
				for (int i = 0; i < array.length; i++) {
					EmpListenerAudio l = (EmpListenerAudio) array[i];
					try {
						l.muteChanged(mute);
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}
		}.start();
	}

	private void triggerVolumeListeners(final int volume) {
		// volume changes often come as groups (ie. holding down the up arrow)
		// So, we wait a bit before triggering
		lastVolume = volume;

		SimpleTimer.addEvent("Volume", SystemTime.getOffsetTime(100),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (lastVolume != volume) {
							return;
						}

						Object[] array = listenersAudio.toArray();
						for (int i = 0; i < array.length; i++) {
							EmpListenerAudio l = (EmpListenerAudio) array[i];
							try {
								l.audioChanged(volume);
							} catch (Exception e) {
								Debug.out(e);
							}
						}
					}
				});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#addListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerTimeout)
	public void addListener(EmpListenerTimeout l) {
		listenersTimeout.add(l);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#removeListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerTimeout)
	public void removeListener(EmpListenerTimeout l) {
		listenersTimeout.remove(l);
	}

	private void triggerTimeoutListeners() {
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("timeout", true) {
			public void runSupport() {
				if (exec == null) {
					return;
				}

				Object[] array = listenersTimeout.toArray();
				for (int i = 0; i < array.length; i++) {
					EmpListenerTimeout l = (EmpListenerTimeout) array[i];
					try {
						l.empTimeout();
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}
		}.start();
	}

	public void addListener(EmpListenerFullScreen l) {
		listenersFullScreen.add(l);
	}

	public void removeListener(EmpListenerFullScreen l) {
		listenersFullScreen.remove(l);
	}

	private void triggerFullScreenListeners(final boolean state) {
		if (state == isFullScreen) {
			return;
		}
		isFullScreen = state;
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("emp fullscreen", true) {
			public void runSupport() {
				if (exec == null) {
					return;
				}

				Object[] array = listenersFullScreen.toArray();
				for (int i = 0; i < array.length; i++) {
					EmpListenerFullScreen l = (EmpListenerFullScreen) array[i];
					try {
						l.fullScreenChanged(state);
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}
		}.start();
	}

	protected void debug(String string) {
		if (diag_logger == null) {
			return;
		}

		diag_logger.log("[MPl] " + SystemTime.getCurrentTime() + "] " + string);

		if (DEBUG_TO_STDOUT) {
			System.out.println(System.currentTimeMillis() + "] " + string);
		}

		if (DEBUG_TO_LOGGER) {
			if (logger == null) {
				logger = EmbeddedPlayerWindowManager.getLogger();
				if (logger == null) {
					return;
				}
			}
			logger.log(string);
		}
	}

	public boolean isStopped() {
		return stopped;
	}

	public void semiClose() {
		debug("semiclose");
		triggerSemiCloseListeners();

		runAction(EmbeddedMediaPlayer.ACTION_PAUSE);
	}

	public void addListener(EmpListenerSemiClose l) {
		listenersSemiClose.add(l);
	}

	public void removeListener(EmpListenerSemiClose l) {
		listenersSemiClose.remove(l);
	}

	private void triggerSemiCloseListeners() {
		Object[] array = listenersSemiClose.toArray();
		for (int i = 0; i < array.length; i++) {
			EmpListenerSemiClose l = (EmpListenerSemiClose) array[i];
			try {
				l.empSemiClose();
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}


	public String getVO() {
		return voID;
	}
}
