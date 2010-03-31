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

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.ole.win32.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.azureus.plugins.azemp.EmbeddedPlayerWindowManager;

import org.gudy.azureus2.plugins.logging.LoggerChannel;

/**
 * @author TuxPaper
 * @created Sep 20, 2007
 *
 */
public class EmbeddedWMP
	implements EmbeddedMediaPlayer
{

	private static final boolean DEBUG_TO_STDOUT = false;

	private static final boolean DEBUG_TO_LOGGER = false;

	/**
	 * Sent when the control changes OpenState
	 * void OpenStateChange([in] long NewState);
	 */
	final static int WMP_EVENT_OPENSTATECHANGE = 0x00001389;

	/**
	 * Sent when the control changes PlayState
	 * void PlayStateChange([in] long NewState);
	 */
	final static int WMP_EVENT_PLAYSTATECHANGE = 0x000013ed;

	/**
	 * Sent when the current audio language has changed
	 * void AudioLanguageChange([in] long LangID);
	 */
	final static int WMP_EVENT_AUDIOLANGUAGECHANGE = 0x000013ee;

	/**
	 * Sent when the status string changes
	 * void StatusChange();
	 */
	final static int WMP_EVENT_STATUSCHANGE = 0x0000138a;

	/**
	 * Sent when a synchronized command or URL is received
	 * void ScriptCommand([in] BSTR scType, [in] BSTR Param);
	 */
	final static int WMP_EVENT_SCRIPTCOMMAND = 0x000014b5;

	/**
	 * Sent when a new stream is started in a channel
	 * void NewStream();
	 */
	final static int WMP_EVENT_NEWSTREAM = 0x0000151b;

	/**
	 * Sent when the control is disconnected from the server
	 * void Disconnect([in] long Result);
	 */
	final static int WMP_EVENT_DISCONNECT = 0x00001519;

	/**
	 * Sent when the control begins or ends buffering
	 * void Buffering([in] VARIANT_BOOL Start);
	 */
	final static int WMP_EVENT_BUFFERING = 0x0000151a;

	/**
	 * Sent when the control has an error condition
	 * void Error();
	 */
	final static int WMP_EVENT_ERROR = 0x0000157d;

	/**
	 * Sent when the control encounters a problem
	 * void Warning([in] long WarningType, [in] long Param, [in] BSTR Description);
	 */
	final static int WMP_EVENT_WARNING = 0x000015e1;

	/**
	 * Sent when the end of file is reached
	 * void EndOfStream([in] long Result);
	 */
	final static int WMP_EVENT_ENDOFSTREAM = 0x00001451;

	/**
	 * void PositionChange([in] double oldPosition, [in] double newPosition);
	 */
	final static int WMP_EVENT_POSITIONCHANGED = 0x00001452;

	final static int WMP_EVENT_MARKERHIT = 0x00001453;

	final static int WMP_EVENT_DURATIONUNITCHANGE = 0x00001454;

	final static int WMP_EVENT_CDROMMEDIACHANGE = 0x00001645;

	/**
	 * Sent when a playlist changes
	 * void PlaylistChange([in] IDispatch* Playlist, 
	 * [in] WMPPlaylistChangeEventType change);
	 */
	final static int WMP_EVENT_PLAYLISTCHANGE = 0x000016a9;

	final static int WMP_EVENT_CURRENTPLAYLISTCHANGE = 0x000016ac;

	final static int WMP_EVENT_CURRENTPLAYLISTITEMAVAILABLE = 0x000016ad;

	final static int WMP_EVENT_MEDIACHANGE = 0x000016aa;

	final static int WMP_EVENT_CURRENTMEDIAITEMAVAILABLE = 0x000016ab;

	final static int WMP_EVENT_CURRENTITEMCHANGE = 0x000016ae;

	final static int WMP_EVENT_MEDIACOLLECTIONCHANGE = 0x000016af;

	/**
	 * Occurs when a user clicks the mouse
	 * void Click([in] short nButton, [in] short nShiftState,
	 *            [in] long fX, [in] long fY);
	 */
	final static int WMP_EVENT_CLICK = 0x00001969;

	/**
	 * Occurs when a user double-clicks the mouse
	 * void DoubleClick([in] short nButton, [in] short nShiftState,
	 *                  [in] long fX, [in] long fY);
	 */
	final static int WMP_EVENT_DOUBLECLICK = 0x0000196a;

	/**
	 * Occurs when a key is pressed
	 * void KeyDown([in] short nKeyCode, [in] short nShiftState);
	 */
	final static int WMP_EVENT_KEYDOWN = 0x0000196b;

	/**
	 * Occurs when a key is pressed and released
	 * void KeyPress([in] short nKeyAscii);
	 */
	final static int WMP_EVENT_KEYPRESS = 0x0000196c;

	/**
	 * Occurs when a key is released
	 * void KeyUp([in] short nKeyCode, [in] short nShiftState);
	 */
	final static int WMP_EVENT_KEYUP = 0x000016af;

	/**
	 * Undefined 	Windows Media Player is in an undefined state.
	 */
	final static int WMP_PLAYSTATE_UNDEFINED = 0;

	/**
	 * Stopped 	Playback of the current media item is stopped.
	 */
	final static int WMP_PLAYSTATE_STOPPED = 1;

	/**
	 * Paused 	Playback of the current media item is paused. When a media item is paused, resuming playback begins from the same location.
	 */
	final static int WMP_PLAYSTATE_PAUSED = 2;

	/**
	 * Playing 	The current media item is playing.
	 */
	final static int WMP_PLAYSTATE_PLAYING = 3;

	/**
	 * ScanForward 	The current media item is fast forwarding.
	 */
	final static int WMP_PLAYSTATE_SCANFWD = 4;

	/**
	 * ScanReverse 	The current media item is fast rewinding.
	 */
	final static int WMP_PLAYSTATE_SCANREV = 5;

	/** 	
	 * Buffering 	The current media item is getting additional data from the server.
	 */
	final static int WMP_PLAYSTATE_BUFFERING = 6;

	/**	
	 * Waiting 	Connection is established, but the server is not sending data. Waiting for session to begin.
	 */
	final static int WMP_PLAYSTATE_WAITING = 7;

	/**
	 * MediaEnded 	Media item has completed playback.
	 */
	final static int WMP_PLAYSTATE_MEDIA_END = 8;

	/**	
	 * Transitioning 	Preparing new media item.
	 */
	final static int WMP_PLAYSTATE_TRANSITIONING = 9;

	/**	
	 * Ready 	Ready to begin playing.
	 */
	final static int WMP_PLAYSTATE_READY = 10;

	/**
	 * Reconnecting 	Reconnecting to stream.
	 */
	final static int WMP_PLAYSTATE_RECONNECTING = 11;

	private OleFrame frame;

	private OleControlSite controlSite;

	private OleAutomation automation;

	private LoggerChannel logger;

	private ArrayList listenersFC = new ArrayList();

	private String lastErr;

	private ArrayList listenersAudio = new ArrayList();

	protected int state = 0;

	private boolean buffering = false;

	private boolean isFullScreen = false;

	private ArrayList listenersFullScreen = new ArrayList();

	private boolean USE_SWT_FULLSCREEN;

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#addListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerAudio)
	public void addListener(EmpListenerAudio l) {
		listenersAudio.add(l);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#removeListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerAudio)
	public void removeListener(EmpListenerAudio l) {
		listenersAudio.remove(l);
	}

	private void triggerAudioListener() {
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("Audio", true) {
			public void runSupport() {
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
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("Mute", true) {
			public void runSupport() {
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
		}.start();
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#addListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerFileChanged)
	public void addListener(EmpListenerFileChanged l) {
		listenersFC.add(l);
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#removeListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerFileChanged)
	public void removeListener(EmpListenerFileChanged l) {
		listenersFC.remove(l);
	}

	private void triggerFCListener(final String s) {
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("FC", true) {
			public void runSupport() {
				Object[] array = listenersFC.toArray();
				for (int i = 0; i < array.length; i++) {
					EmpListenerFileChanged l = (EmpListenerFileChanged) array[i];
					try {
						l.empFileChanged(s);
					} catch (Exception e) {
						Debug.out(e);
					}
				}
				triggerAudioListener();
			}
		}.start();
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#init(org.eclipse.swt.widgets.Control)
	public void init(final Composite c, boolean useSWTFullScreen)
			throws Exception {
		//String sID = "{6BF52A52-394A-11d3-B153-00C04F79FAA6}";
		//String sID = "WMPlayer.OCX";
		// ID of WMP 6.x
		//String sID = "MediaPlayer.MediaPlayer.1";

		this.USE_SWT_FULLSCREEN = useSWTFullScreen;
		//c.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		frame = new OleFrame(c, SWT.NO_FOCUS);
		frame.setVisible(false);
		frame.setLayoutData(Utils.getFilledFormData());
		frame.setBackground(c.getBackground());

		String sID = "WMPlayer.OCX.7";
		//		String sID = "{6BF52A52-394A-11d3-B153-00C04F79FAA6}";
		// ID of WMP 6.x
		//		String sID = "MediaPlayer.MediaPlayer.1";

		try {
			controlSite = new OleControlSite(frame, SWT.NO_FOCUS, sID);
		} catch (SWTException e) {
			e.printStackTrace();

		}

		controlSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE);
		controlSite.setBackground(c.getBackground());

		automation = new OleAutomation(controlSite);

		frame.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				if (controlSite != null && !controlSite.isDisposed()) {
					OleUtils.invokeOle(automation, "controls.stop");
					OleUtils.invokeOle(automation, "close");

					// Sometimes stops JVM from crash
					OleUtils.setOleProperty(automation, "uiMode", "none");

					controlSite.doVerb(OLE.OLEIVERB_HIDE);
					controlSite.dispose();
				}
			}
		});

		c.setFocus();
		init();

		c.addListener(SWT.Paint, new Listener() {
			public void handleEvent(Event event) {
				if (c.isDisposed() || frame == null) {
					return;
				}
				frame.setVisible(true);

				c.getShell().removeListener(SWT.Paint, this);
			}
		});

		c.getShell().addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
			}

		});
	}

	private void init() {
		OleUtils.setOleProperty(automation, "Settings.autoStart", 1);
		OleUtils.setOleProperty(automation, "stretchToFit", 1);
		OleUtils.setOleProperty(automation, "EnableContextMenu", 0);
		OleUtils.setOleProperty(automation, "uiMode", "none");

		debug("WMP Version Info: "
				+ OleUtils.getOleProperty(automation, "versionInfo"));

		triggerAudioListener();

		OleListener stateListener = new OleListener() {

			public void handleEvent(OleEvent event) {
				String args = "";
				for (int i = 0; i < event.arguments.length; i++) {
					args += i + " : " + event.arguments[i] + ", ";
				}
				if (event.arguments.length > 0) {
					state = event.arguments[0].getInt();
					String filename = getFileName();
					debug("State change to " + state + ";" + filename);
					if (state == WMP_PLAYSTATE_PLAYING) {
						triggerFCListener(filename);

					} else if (state == WMP_PLAYSTATE_MEDIA_END) {
					} else if (state == WMP_PLAYSTATE_READY) {
					}
				}
				checkFullScreenState();
			}
		};

		OleListener errorListener = new OleListener() {

			public void handleEvent(OleEvent event) {
				String args = "";
				for (int i = 0; i < event.arguments.length; i++) {
					args += i + " : " + event.arguments[i] + ", ";
				}
				Variant oleProperty = OleUtils.getOleProperty(automation,
						"error.errorCount");
				if (oleProperty != null) {
					long count = oleProperty.getInt();
					for (int i = 0; i < count; i++) {
						Variant oleError = OleUtils.getOleProperty(automation,
								"error.item", new Variant((short) i));

						if (oleError != null) {
							OleAutomation auto = oleError.getAutomation();
							String errDesc = OleUtils.getOleString(auto, "errorDescription",
									false, "No Desc");
							String errContext = OleUtils.getOleString(auto, "errorContext",
									false, "No Context");
							lastErr = "[" + errContext + "]" + errDesc;
							debug("Error " + lastErr);
						}
						//Settings.enableErrorDialogs
					}

					OleUtils.invokeOle(automation, "error.clearErrorQueue");
				}

			}
		};

		OleListener openStateListener = new OleListener() {

			public void handleEvent(OleEvent event) {
				debug("openstate event : " + Long.toHexString(event.type));
				String args = "";
				for (int i = 0; i < event.arguments.length; i++) {
					args += i + " : " + event.arguments[i] + ", ";
				}
				debug("       " + args);
			}
		};

		OleListener bufferingListener = new OleListener() {
			// @see org.eclipse.swt.ole.win32.OleListener#handleEvent(org.eclipse.swt.ole.win32.OleEvent)
			public void handleEvent(OleEvent event) {
				debug("Buffering event : " + Long.toHexString(event.type) + ";"
						+ event.detail);
				if (event.arguments.length == 1) {
					setBuffering(event.arguments[0].getBoolean());
				}
			}
		};

		OleListener doubleClickListener = new OleListener() {
			public void handleEvent(OleEvent event) {
				checkFullScreenState();
				if (USE_SWT_FULLSCREEN) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							OleUtils.setOleProperty(automation, "fullScreen", new Variant(0));
						}
					});

					flipFullScreen();
				}
			}
		};

		controlSite.addEventListener(WMP_EVENT_PLAYSTATECHANGE, stateListener);
		controlSite.addEventListener(WMP_EVENT_BUFFERING, bufferingListener);
		controlSite.addEventListener(WMP_EVENT_DOUBLECLICK, doubleClickListener);
		//controlSite.addEventListener(WMP_EVENT_OPENSTATECHANGE, openStateListener);

		controlSite.addEventListener(WMP_EVENT_ERROR, errorListener);

		OleUtils.setOleProperty(automation, "settings.enableErrorDialogs", 1);
	}

	/**
	 * @param boolean1
	 *
	 * @since 3.0.3.5
	 */
	protected void setBuffering(boolean b) {
		if (b == buffering) {
			return;
		}

		// future: trigger listener
		buffering = b;
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#delete()
	public void delete() {
		// without close call, it's super slow
		OleUtils.invokeOle(automation, "close");

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame != null && !frame.isDisposed()) {
					frame.dispose();
					frame = null;
				}
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getFileName()
	public String getFileName() {
		if (frame == null) {
			return null;
		}
		return (String) Utils.execSWTThreadWithObject("getFileName",
				new AERunnableObject() {
					public Object runSupport() {
						String sourceURL = OleUtils.getOleString(automation,
								"currentMedia.sourceURL", true, null);
						return sourceURL;
					}
				});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getLength()
	public float getLength() {
		if (frame == null) {
			return -1;
		}
		return ((Double) Utils.execSWTThreadWithObject("getLength",
				new AERunnableObject() {

					public Object runSupport() {
						try {
							Variant oleProperty = OleUtils.getOleProperty(automation,
									"currentMedia.duration");
							if (oleProperty != null) {
								return new Double(oleProperty.getDouble());
							}
						} catch (Exception e) {
						}
						return new Double(-1);
					}
				})).floatValue();
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getPositionPercent()
	public float getPositionPercent() {
		if (frame == null || frame.isDisposed()) {
			return -1;
		}
		return ((Double) Utils.execSWTThreadWithObject("getPosPct",
				new AERunnableObject() {
					public Object runSupport() {
						if (frame == null || frame.isDisposed()) {
							return new Double(-1);
						}

						try {
							Variant oleProperty = OleUtils.getOleProperty(automation,
									"controls.currentPosition");
							if (oleProperty != null) {
								return new Double(oleProperty.getDouble() / getLength() * 100);
							}
						} catch (Exception e) {
						}
						return new Double(-1);
					}
				})).floatValue();
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getPositionSecs()
	public float getPositionSecs() {
		if (frame == null || frame.isDisposed()) {
			return -1;
		}

		return ((Double) Utils.execSWTThreadWithObject("getPosSecs",
				new AERunnableObject() {
					public Object runSupport() {
						if (frame == null || frame.isDisposed()) {
							return new Double(-1);
						}

						checkFullScreenState();

						Variant oleProperty = OleUtils.getOleProperty(automation,
								"controls.currentPosition");
						if (oleProperty != null) {
							return new Double(oleProperty.getDouble());
						}
						return new Double(-1);
					}
				})).floatValue();
	}

	private void checkFullScreenState() {
		if (USE_SWT_FULLSCREEN) {
			if (_isFullScreen()) {
				OleUtils.setOleProperty(automation, "fullScreen", new Variant(0));
			}
		} else {
			boolean wasFullScreen = isFullScreen;

			isFullScreen = _isFullScreen();
			if (isFullScreen != wasFullScreen) {
				triggerFullScreenListeners(isFullScreen);
			}
		}
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getStreamPos()
	public long getStreamPos() {
		// NO STREAM POS
		return -1;
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getVolume()
	public int getVolume() {
		if (frame == null || frame.isDisposed()) {
			return -1;
		}

		return ((Long) Utils.execSWTThreadWithObject("getVolume",
				new AERunnableObject() {
					public Object runSupport() {
						if (frame == null || frame.isDisposed()) {
							return new Long(-1);
						}

						Variant oleProperty = OleUtils.getOleProperty(automation,
								"settings.volume");
						if (oleProperty != null) {
							return new Long(oleProperty.getLong());
						}
						return new Long(-1);
					}
				})).intValue();
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#isBuffering()
	public boolean isBuffering() {
		if (frame == null || frame.isDisposed()) {
			return false;
		}

		return buffering;
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#isFullScreen()
	public boolean isFullScreen() {
		checkFullScreenState();
		return isFullScreen;
	}

	public boolean _isFullScreen() {
		if (frame == null || frame.isDisposed()) {
			return false;
		}

		return Utils.execSWTThreadWithBool("isFullScreen", new AERunnableBoolean() {
			public boolean runSupport() {
				if (frame == null || frame.isDisposed()) {
					return false;
				}
				Variant oleProperty = OleUtils.getOleProperty(automation, "fullscreen");
				if (oleProperty == null || oleProperty.getType() != OLE.VT_BOOL) {
					debug("Bad Type: " + oleProperty.getType());
					return false;
				}

				return oleProperty.getBoolean();
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#isMuted()
	public boolean isMuted() {
		if (frame == null || frame.isDisposed()) {
			return false;
		}

		return Utils.execSWTThreadWithBool("isMuted", new AERunnableBoolean() {
			public boolean runSupport() {
				if (frame == null || frame.isDisposed()) {
					return false;
				}
				Variant oleProperty = OleUtils.getOleProperty(automation,
						"settings.mute");
				if (oleProperty == null || oleProperty.getType() != OLE.VT_BOOL) {
					debug("Bad Type: " + oleProperty.getType());
					return false;
				}

				return oleProperty.getBoolean();
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#isPaused()
	public boolean isPaused() {
		if (frame == null || frame.isDisposed()) {
			return false;
		}

		return Utils.execSWTThreadWithBool("isPaused", new AERunnableBoolean() {
			public boolean runSupport() {
				if (frame == null || frame.isDisposed()) {
					return false;
				}
				Variant oleProperty = OleUtils.getOleProperty(automation, "playState");
				if (oleProperty == null || oleProperty.getType() != OLE.VT_I4) {
					return false;
				}

				int status = oleProperty.getShort();
				return status == WMP_PLAYSTATE_PAUSED;
			}

		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#isStopped()
	public boolean isStopped() {
		if (frame == null || frame.isDisposed()) {
			return false;
		}

		return Utils.execSWTThreadWithBool("isStopped", new AERunnableBoolean() {
			public boolean runSupport() {
				if (frame == null || frame.isDisposed()) {
					return false;
				}

				Variant oleProperty = OleUtils.getOleProperty(automation, "playState");
				if (oleProperty == null || oleProperty.getType() != OLE.VT_I4) {
					return false;
				}

				int status = oleProperty.getShort();
				return status == WMP_PLAYSTATE_STOPPED || status == WMP_PLAYSTATE_READY;
			}

		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#loadMedia(java.lang.String)
	public boolean loadMedia(final String mediaLocation) {
		if (frame == null || frame.isDisposed()) {
			return false;
		}

		return Utils.execSWTThreadWithBool("loadMedia", new AERunnableBoolean() {
			public boolean runSupport() {
				if (frame == null || frame.isDisposed()) {
					return false;
				}

				return OleUtils.setOleProperty(automation, "URL", mediaLocation);
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#runAction(int)
	public void runAction(final int action) {
		if (frame == null || frame.isDisposed()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame == null || frame.isDisposed()) {
					return;
				}

				switch (action) {
					case ACTION_FLIP_MUTE:
						boolean newMuted = !isMuted();
						OleUtils.setOleProperty(automation, "settings.mute", newMuted ? 1
								: 0);
						triggerMuteListeners(newMuted);

						break;

					case ACTION_FLIP_PAUSE:
						if (isPaused()) {
							OleUtils.invokeOle(automation, "controls.play");
						} else {
							OleUtils.invokeOle(automation, "controls.pause");
						}
						break;

					case ACTION_FLIP_FULLSCREEN:
						if (USE_SWT_FULLSCREEN) {
							flipFullScreen();
						} else {
							OleUtils.setOleProperty(automation, "fullScreen", new Variant(true));
							checkFullScreenState();
						}
						break;

					case ACTION_PAUSE:
						OleUtils.invokeOle(automation, "controls.pause");
						break;

					case ACTION_PLAY:
						if (!isSortofPlaying()) {
							OleUtils.invokeOle(automation, "controls.play");
						}
						break;

					case ACTION_STOP:
						OleUtils.invokeOle(automation, "controls.stop");
						break;
				}
			}
		});
	}

	private boolean isSortofPlaying() {
		return state == WMP_PLAYSTATE_PLAYING || state == WMP_PLAYSTATE_SCANFWD
				|| state == WMP_PLAYSTATE_SCANREV || state == WMP_PLAYSTATE_BUFFERING
				|| state == WMP_PLAYSTATE_WAITING
				|| state == WMP_PLAYSTATE_TRANSITIONING
				|| state == WMP_PLAYSTATE_RECONNECTING;
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#seekAbsolute(float)
	public void seekAbsolute(final float seconds) {
		if (frame == null || frame.isDisposed()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame == null || frame.isDisposed()) {
					return;
				}

				OleUtils.setOleProperty(automation, "controls.currentPosition",
						new Variant((double) seconds));
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#seekPercent(float)
	public void seekPercent(final float percent) {
		if (frame == null || frame.isDisposed()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame == null || frame.isDisposed()) {
					return;
				}

				double newPos = getLength() * percent;
				OleUtils.setOleProperty(automation, "controls.currentPosition",
						new Variant((double) newPos));
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#seekRelative(float)
	public void seekRelative(final float f) {
		if (frame == null || frame.isDisposed()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame == null || frame.isDisposed()) {
					return;
				}

				float newPos = getPositionSecs() + f;
				if (newPos < 0) {
					newPos = 0;
				}
				float length = getLength();
				if (newPos > length) {
					newPos = length;
				}
				OleUtils.setOleProperty(automation, "controls.currentPosition",
						new Variant((double) newPos));
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#sendCommand(java.lang.String)
	public void sendCommand(String command) {
		debug(command + " not supported on WMP");
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#setMuted(boolean)
	public void setMuted(final boolean newMuted) {
		if (frame == null || frame.isDisposed()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame == null || frame.isDisposed()) {
					return;
				}

				debug("set mute to " + newMuted);
				OleUtils.setOleProperty(automation, "settings.mute", newMuted ? 1 : 0);
				triggerMuteListeners(newMuted);
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#setVolume(int)
	public void setVolume(final int newVolume) {
		if (frame == null || frame.isDisposed()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame == null || frame.isDisposed()) {
					return;
				}

				OleUtils.setOleProperty(automation, "settings.volume", (int) newVolume);
				triggerVolumeListeners(newVolume);
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#setVolumeRelative(int)
	public void setVolumeRelative(final int rel) {
		if (frame == null || frame.isDisposed()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (frame == null || frame.isDisposed()) {
					return;
				}

				int newVolume = getVolume() + rel;
				if (newVolume < 0) {
					newVolume = 0;
				} else if (newVolume > 100) {
					newVolume = 100;
				}
				OleUtils.setOleProperty(automation, "settings.volume", newVolume);
				triggerVolumeListeners(newVolume);
			}
		});
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getFrameDropCount()
	public float getFrameDropPct() {
		return 0.0f;
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#setAllowSeekAhead(boolean)
	public void setAllowSeekAhead(boolean b) {
	}

	protected void debug(String string) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.emp");
		diag_logger.log("[WMP] " + SystemTime.getCurrentTime() + "] " + string);

		if (DEBUG_TO_STDOUT) {
			System.out.println(string);
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

	public static void main(String args[]) {
		Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		Composite c = new Composite(shell, SWT.BORDER);
		c.setLayout(new FormLayout());

		EmbeddedWMP playerOLE = new EmbeddedWMP();
		try {
			playerOLE.init(c, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		shell.setSize(320, 200);
		System.out.println("Open");
		shell.open();

		System.out.println("layout");

		shell.layout();

		//playerOLE.test();
		playerOLE.loadMedia("c:\\test.wmv");
		playerOLE.runAction(ACTION_PLAY);

		while (!shell.isDisposed()) {
			while (!display.readAndDispatch()) {
				display.sleep();
			}
		}

	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#addListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerTimeout)
	public void addListener(EmpListenerTimeout l) {
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#removeListener(com.azureus.plugins.azemp.ui.swt.emp.EmpListenerTimeout)
	public void removeListener(EmpListenerTimeout l) {
	}

	public void addListener(EmpListenerFullScreen l) {
		listenersFullScreen.add(l);
	}

	public void removeListener(EmpListenerFullScreen l) {
		listenersFullScreen.remove(l);
	}

	private void triggerFullScreenListeners(final boolean state) {
		// we need to trigger listener on a different thread, so we can
		// continue processing any new incoming lines
		new AEThread("timeout", true) {
			public void runSupport() {
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

	public void addListener(EmpListenerSemiClose l) {
	}

	public void removeListener(EmpListenerSemiClose l) {
	}

	public void semiClose() {
		runAction(EmbeddedMediaPlayer.ACTION_PAUSE);
	}

	public void setExtraDropFrames(long x) {
	}

	// @see com.azureus.plugins.azemp.ui.swt.emp.EmbeddedMediaPlayer#getVideoResolution()
	public int[] getVideoResolution() {
		if (frame == null || frame.isDisposed()) {
			return null;
		}

		Point pt = (Point) Utils.execSWTThreadWithObject("getVideoResolution",
				new AERunnableObject() {
					public Object runSupport() {
						if (frame == null || frame.isDisposed()) {
							return null;
						}

						int w, h;
						Variant olePropertyW = OleUtils.getOleProperty(automation,
								"currentMedia.imageSourceWidth");
						if (olePropertyW != null) {
							w = olePropertyW.getInt();
						} else {
							return null;
						}
						Variant olePropertyH = OleUtils.getOleProperty(automation,
								"currentMedia.imageSourceHeight");
						if (olePropertyH != null) {
							h = olePropertyH.getInt();
						} else {
							return null;
						}
						return new Point(w, h);
					}
				}, 100);
		return pt == null ? null : new int[] {
			pt.x,
			pt.y
		};
	}

	private void flipFullScreen() {
		debug("Flip fullscreen mode to " + !isFullScreen);
		final boolean wasFullScreen = isFullScreen;
		if (USE_SWT_FULLSCREEN) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (frame == null || frame.isDisposed()) {
						return;
					}
					isFullScreen = !wasFullScreen;
					frame.getShell().setFullScreen(isFullScreen);
					triggerFullScreenListeners(isFullScreen);
				}
			});
		} else {
			// could be on swt thread, so don't bother with getBooleanProperty
			triggerFullScreenListeners(!isFullScreen);
		}
	}
	
	public String getVO() {
		return "wmp";
	}
}
