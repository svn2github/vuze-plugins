/**
 * Created on Jun 23, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.azureus.plugins.azemp.ui.swt.emp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBrowser;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.azureus.plugins.azemp.EmbeddedPlayerWindowManager;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class BrowserHandlerSWT
{
	private static final int MIN_BROWSING_WIDTH = 870;

	private static final int MIN_BROWSING_HEIGHT = 520;

	private static final boolean SHOW_BROWSER_ERRORS = System.getProperty(
			"browser.show.errors", "0").equals("1");

	private final SWTSkinObjectBrowser soBrowser;

	/**
	 * Store URL we currently are on instead of using browser.getUrl() because
	 * we sometimes delay (asyncExec) the setUrl.  This causes trouble.
	 */
	private String urlCurrent;

	protected String lastTitle;

	boolean loadingURL;

	long goodTitleSetOn = -1;

	boolean pageReady;

	protected boolean firstURLSet;

	private final EmbeddedPlayerWindowSWT empWin;

	private BrowserContext browserContext;

	private int height = -1;

	protected boolean swapsWithPlayer = false;

	private LocationListener locationListener;

	private TitleListener titleListener;

	private EmpMessageListener empMessageListener;

	private long lastPosSecs;

	protected int width;

	protected long loadURLStartedOn = -1;

	public BrowserHandlerSWT(final EmbeddedPlayerWindowSWT empWin,
			final SWTSkinObjectBrowser soBrowser) {
		this.empWin = empWin;
		this.soBrowser = soBrowser;
		reset();

		final Browser browser = soBrowser.getBrowser();

		browserContext = (BrowserContext) browser.getData("BrowserContext");
		if (browserContext != null) {
			empMessageListener = new EmpMessageListener(this);
			browserContext.addMessageListener(empMessageListener);
			browserContext.setWiggleBrowser(false);
		}

		locationListener = new LocationListener() {

			public void changing(LocationEvent event) {
				debug("changing to URL: " + event.location);

				if (event.location.startsWith("http")) {
					loadingURL = true;
					goodTitleSetOn = -1;
					pageReady = false;
				}
				if (event.location.endsWith("/close")) {
					if (empWin.getState() != EmbeddedPlayerWindowSWT.STATE_STOPPED) {
						setVisible(false, false);
						if (empWin.emp != null) {
							empWin.emp.runAction(EmbeddedMediaPlayer.ACTION_PLAY);
						}
					}
					event.doit = false;
				}
			}

			public void changed(LocationEvent event) {
				debug("changed to URL: " + event.location);

				loadingURL = false;
				if (isOurWebPageTitle(lastTitle)) {
					if (goodTitleSetOn < 0) {
  					goodTitleSetOn = SystemTime.getCurrentTime();
  					debug("got good title via changed on " + goodTitleSetOn);
					}
				} else {
					goodTitleSetOn = -1;
				}

				if (isBadTitle(lastTitle)) {
					empWin.abortPrePlaybackWait("TITLE-L");
					debug("Changed to URL with bad title: " + lastTitle);

					if (!SHOW_BROWSER_ERRORS) {
						// wait until other events are finished because one of them
						// makes the browser visible..
						event.widget.getDisplay().asyncExec(new AERunnable() {
							public void runSupport() {
								soBrowser.setVisible(false);
								if (!browser.isDisposed()) {
									browser.moveBelow(null);
								}
							}
						});
					}
					reportBadTitle(lastTitle);
				} else if (isOnURL(empWin.URL_PLAYBEFORE)) {
					if (empWin != null) {
  					if (empWin.stats != null && empWin.stats.playBeforePageLoadedOn <= 0) {
  						empWin.stats.playBeforePageLoadedOn = SystemTime.getCurrentTime();
  					}
  					if (empWin.soWaitText != null && !empWin.soWaitText.isDisposed()) {
  						empWin.soWaitText.setVisible(false);
  					}
					}
				}
			}

		};

		browser.addLocationListener(locationListener);

		titleListener = new TitleListener() {
			public void changed(TitleEvent event) {
				// Sometimes, the URL is set before we even set it.
				// This was seen in IE and might have something to do with the home
				// page of the user (reproducing it on other machines failed).
				if (!firstURLSet) {
					return;
				}
				lastTitle = event.title.toLowerCase();
				if (isOurWebPageTitle(lastTitle)) {
					if (goodTitleSetOn < 0) {
  					goodTitleSetOn = SystemTime.getCurrentTime();
  					debug("got good title via title on " + goodTitleSetOn);
					}
				} else {
					goodTitleSetOn = -1;
				}

				// Sometimes (Safari), the Title listner comes after the
				// Location changed event.  So, if we are no longer loading the URL
				// (during load the title may change to other stuff like "" or 
				//  "Loading"), check if title is bad and handle appropriately 
				if (!loadingURL && isBadTitle(lastTitle)) {
					empWin.abortPrePlaybackWait("TITLE-T");
					debug("Changed to URL with bad title via TitleListener: " + lastTitle);

					soBrowser.setVisible(false);
					if (!browser.isDisposed()) {
						browser.moveBelow(null);
					}
					reportBadTitle(lastTitle);
				}
			}
		};
		browser.addTitleListener(titleListener);

		soBrowser.setVisible(false);
	}

	/**
	 * @param lastTitle2
	 *
	 * @since 4.0.0.3
	 */
	protected void reportBadTitle(String lastTitle) {
		try {
			String s = "Bad Title: '" + lastTitle + "' on " + urlCurrent;
			if (s.length() > 150) {
				s = s.substring(0, 150);
			}
  		PlatformMessage message = new PlatformMessage("logging", "JS",
  				"warn", new Object[] {
  					"importance",
  					"USUAL",
  					"namespace",
  					"EMP",
  					"message",
  					s
  				}, 0);
  		//PlatformMessenger.pushMessageNow(message, null);
		} catch (Throwable t) {
			// ignore
		}
	}

	public void reset() {
		debug("reset");
		loadingURL = true;
		urlCurrent = null;
		goodTitleSetOn = -1;
		pageReady = false;
		firstURLSet = false;
		lastPosSecs = -1;
	}

	/**
	 * 
	 *
	 * @since 3.1.0.1
	 */
	public void setToWaitText(final String text) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Browser browser = soBrowser.getBrowser();
				String url = browser.getUrl();
				String htmlWait = getWaitHTML(text == null ? "Please Wait" : text);

				browser.setText(htmlWait);
				browser.execute("document.write('" + htmlWait + "');");
				browser.redraw();
				browser.update();

				if (empWin.soWaitText != null) {
					empWin.soWaitText.setText("One Moment..");
				}
			}
		});
	}

	public void setPrePlaybackWait(boolean wait) {
		empWin.setPrePlaybackWait(wait);
	}

	/**
	 * @param width
	 * @param height
	 *
	 * @since 3.1.0.1
	 */
	public void setMinSize(int width, int height) {
		empWin.setMinSize(width, height);
	}

	public void pageReady() {
		debug("Got page-ready");
		pageReady = true;
		loadingURL = false;
		if (goodTitleSetOn < 0) {
			goodTitleSetOn = SystemTime.getCurrentTime();
			debug("got good title via changed on " + goodTitleSetOn);
		}
		
		if (empWin != null && empWin.isPlayWaitAborted()) {
			debug("Got page-ready, but play wait is aborted.  Skipping show");
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (soBrowser == null) {
					return;
				}
				Browser browser = soBrowser.getBrowser();
				if (browser == null || browser.isDisposed()) {
					return;
				}

				// visible, but maybe not above all other controls..
				// XXX Not sure why I set visiblility here.. disable for now as
				// it messes up OSX OGL overlay
				//browser.setVisible(true);

				if (isOnURL(empWin.URL_PLAYAFTER) && isBrowserVisible()) {
					String script = "if (setupWebAd) { setupWebAd();}";
					browserContext.executeInBrowser(script);
					debug("just called setupWebAd via page-ready");
				} else if (isOnURL(empWin.URL_PLAYBEFORE)
						&& !isBrowserVisible()) {
					debug("Force visible");
					setVisible(true, false);
				}
			}
		});
	}

	protected boolean isOnURL(String prefix) {
		if (prefix.indexOf(":80/") > 0) {
			prefix = prefix.replaceAll(":80/", "/");
		}
		if (urlCurrent == null) {
			return false;
		}
		return urlCurrent.startsWith(prefix);
	}

	protected boolean isBrowserVisible() {
		return soBrowser.isVisible();
	}

	/**
	 * @param title
	 * @return
	 *
	 * @since 3.0.3.5
	 */
	protected boolean isBadTitle(String title) {
		return title != null && title.indexOf(" player") < 0
				&& title.indexOf("about:blank") < 0
				&& title.indexOf("http://") < 0;
	}
	
	protected boolean isOurWebPageTitle(String title) {
		return title != null && title.indexOf(" player") >= 0;
	}

	protected void setVisible(final boolean visible, final boolean resize) {
		if (soBrowser != null && soBrowser.isDisposed()) {
			return;
		}
		debug("set browser " + visible + " via " + Debug.getCompressedStackTrace());

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (empWin.cPlayer == null || empWin.cPlayer.isDisposed()) {
					return;
				}

				boolean v = visible;
				if (v && isBadTitle(lastTitle) && !SHOW_BROWSER_ERRORS) {
					v = false;
				}

				Control browser = soBrowser == null ? null : soBrowser.getControl();
				if (browser != null && !browser.isDisposed()) {
					soBrowser.setVisible(v);
					if (v) {
						browser.moveAbove(null);
						browser.getParent().layout(true, true);

						if (isOnURL(empWin.URL_PLAYAFTER) && resize) {
							SWTSkinUtils.setVisibility(empWin.skin, "ep.topbar", "ep-top",
									true, false, empWin.fastRolling);
							empWin.updateFlipToolbarButtonState(true);
							if (empWin.stats != null) {
								empWin.stats.pp = true;
							}

							if (pageReady) {
								String script = "if (setupWebAd) { setupWebAd();}";
								browserContext.executeInBrowser(script);
								debug("just called setupWebAd via setVis");
							}
						}
					} else {
						browser.moveBelow(null);
					}
					if (swapsWithPlayer) {
						empWin.cPlayer.layout(true, true);
					}
				}

				if (swapsWithPlayer) {
					empWin.cPlayer.setVisible(!v);
					if (!v) {
						empWin.cPlayer.moveAbove(null);
						debug("browser was above. Moving player up");
					}
				} else {
					browser.moveAbove(null);
				}

				empWin.btnFullScreen.setDisabled(v);
				empWin.disableFlipToolbarButton(v);

				if (v && empWin.emp != null) {
					if (empWin.getState() == EmbeddedPlayerWindowSWT.STATE_PLAYING) {
						empWin.emp.runAction(EmbeddedMediaPlayer.ACTION_PAUSE);
					}

					if (empWin.isFullscreen()) {
						debug("FLIP THE FULLSCREEN");
						empWin.emp.runAction(EmbeddedMediaPlayer.ACTION_FLIP_FULLSCREEN);
						empWin.setFullscreen(false);
					}
				}

				if (v && resize) {
					Point size = empWin.shell.getSize();
					if (size.x < MIN_BROWSING_WIDTH) {
						size.x = MIN_BROWSING_WIDTH;
					}

					if (size.y < MIN_BROWSING_HEIGHT) {
						size.y = MIN_BROWSING_HEIGHT;
					}

					empWin.shell.setSize(size);
					Utils.verifyShellRect(empWin.shell, true);
				}
				
				if (!v) {
					setHeight(0, true);
				} else {
					if (height == 0) {
						setHeight(-1, true);
					}
				}
			}

		});
	}

	protected void setURL(String prefix, final boolean blankFirst,
			final boolean forceReload, final String extraParams) {
		if (soBrowser != null) {
			if (prefix.indexOf(":80/") > 0) {
				prefix = prefix.replaceAll(":80/", "/");
			}
			urlCurrent = prefix;

			final String fPrefix = prefix;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (soBrowser == null) {
						return;
					}
					Browser browser = soBrowser.getBrowser();
					if (browser == null || browser.isDisposed()) {
						return;
					}

					if (!forceReload && empWin.isOnBaseURL(fPrefix, browser.getUrl())) {
						debug("setURL: already on " + fPrefix);
						return;
					}

					if (blankFirst) {
						String htmlWait = getWaitHTML("Please Wait");
						browser.setText(htmlWait);
						browser.execute("document.write('" + htmlWait + "');");
						browser.redraw();
						browser.update();
					}

					loadingURL = true;
					goodTitleSetOn = -1;
					loadURLStartedOn  = SystemTime.getCurrentTime();

					debug("setURL: " + fPrefix + (firstURLSet ? "" : " (First URL)"));
					firstURLSet = true;

					soBrowser.setURL(fPrefix + empWin.getURLSuffix(extraParams));
				}
			});
		}
	}

	private String getWaitHTML(String waitText) {
		return "<html style=\"overflow:auto;\">"
				+ "<body style=\"overflow:auto;\" bgcolor=#1b1b1b text=#AAAAAA>"
				+ "<center><TABLE HEIGHT=95%><TR VALIGN=CENTER><TD>"
				+ waitText
				+ "</TD></TR></TABLE></center></body></html>";
	}

	protected void debug(String string) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.emp");
		diag_logger.log("[EMP] [" + soBrowser.getViewID() + "] " + string);

		if (EmbeddedPlayerWindowSWT.DEBUG_TO_STDOUT) {
			System.out.println(SystemTime.getCurrentTime() + "] [EMP] ["
					+ soBrowser.getViewID() + "] " + string);
		}

		if (EmbeddedPlayerWindowSWT.DEBUG) {
			if (EmbeddedPlayerWindowSWT.logger == null) {
				EmbeddedPlayerWindowSWT.logger = EmbeddedPlayerWindowManager.getLogger();
				if (EmbeddedPlayerWindowSWT.logger == null) {
					return;
				}
			}
			EmbeddedPlayerWindowSWT.logger.log(string);
		}
	}

	protected void setHeight(final int height, final boolean top) {
		if (swapsWithPlayer) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {

				BrowserHandlerSWT.this.height = height;
				if (soBrowser == null || soBrowser.isDisposed()) {
					return;
				}
				FormData fdPlayer;
				Object ldPlayer = empWin.cPlayer.getLayoutData();
				if (ldPlayer instanceof FormData) {
					fdPlayer = (FormData) ldPlayer;
				} else {
					return;
				}

				Control browser = soBrowser.getControl();
				Object layoutData = browser.getLayoutData();
				FormData fdBrowser;
				if (layoutData instanceof FormData) {
					fdBrowser = (FormData) layoutData;
				} else {
					return;
				}

				resetFormData(fdBrowser, fdPlayer);

				if (height >= 0) {
					if (top) {
						fdBrowser.bottom = null;
						fdPlayer.top = new FormAttachment(browser);
					} else {
						fdBrowser.top = null;
						fdPlayer.bottom = new FormAttachment(browser);
					}
					fdBrowser.height = height;
				}

				browser.setLayoutData(fdBrowser);
				empWin.cPlayer.setLayoutData(fdPlayer);
				browser.getParent().layout(true, true);
				browser.moveAbove(null);
				empWin.cPlayer.moveBelow(null);

				browser.setVisible(height != 0);
				debug("set height " + height + (top ? " top" : "bottom"));
			}
		});
	}

	/**
	 * @param width
	 * @param left
	 *
	 * @since 3.1.1.1
	 */
	public void setWidth(final int width, final boolean left) {
		if (swapsWithPlayer) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {

				BrowserHandlerSWT.this.width = width;
				if (soBrowser == null || soBrowser.isDisposed()) {
					return;
				}

				FormData fdPlayer;
				Object ldPlayer = empWin.cPlayer.getLayoutData();
				if (ldPlayer instanceof FormData) {
					fdPlayer = (FormData) ldPlayer;
				} else {
					return;
				}

				Control browser = soBrowser.getControl();
				Object layoutData = browser.getLayoutData();
				FormData fdBrowser;
				if (layoutData instanceof FormData) {
					fdBrowser = (FormData) layoutData;
				} else {
					return;
				}

				resetFormData(fdBrowser, fdPlayer);
				if (width >= 0) {
					if (left) {
						fdBrowser.right = null;
						fdPlayer.left = new FormAttachment(browser);
					} else {
						fdBrowser.left = null;
						fdPlayer.right = new FormAttachment(browser);
					}
					fdBrowser.width = width;
				}

				browser.setLayoutData(fdBrowser);
				browser.getParent().layout(true, true);
				browser.moveAbove(null);
				empWin.cPlayer.moveBelow(null);

				browser.setVisible(width != 0);
				debug("set width " + height);
			}
		});
	}

	/**
	 * @param fdBrowser
	 *
	 * @since 3.1.1.1
	 */
	protected void resetFormData(FormData fdBrowser, FormData fdPlayer) {
		if (fdBrowser == null) {
			if (soBrowser == null || soBrowser.isDisposed()) {
				return;
			}
			//Browser browser = soBrowser.getBrowser();
			Control browser = soBrowser.getControl();
			Object layoutData = browser.getLayoutData();
			if (layoutData instanceof FormData) {
				fdBrowser = (FormData) layoutData;
			} else {
				return;
			}
		}
		fdBrowser.right = new FormAttachment(100, 0);
		fdBrowser.left = new FormAttachment(0, 0);
		fdBrowser.height = SWT.DEFAULT;
		fdBrowser.width = SWT.DEFAULT;
		SWTSkinObject skinObject = empWin.skin.getSkinObject("ep-top");
		if (skinObject != null) {
			fdBrowser.top = new FormAttachment(skinObject.getControl());
			fdPlayer.top = new FormAttachment(skinObject.getControl());
		}
		skinObject = empWin.skin.getSkinObject("ep-bottom");
		if (skinObject != null) {
			fdBrowser.bottom = new FormAttachment(skinObject.getControl());
			fdPlayer.bottom = new FormAttachment(skinObject.getControl());
		}

		fdPlayer.right = new FormAttachment(100, 0);
		fdPlayer.left = new FormAttachment(0, 0);
		fdPlayer.height = SWT.DEFAULT;
		fdPlayer.width = SWT.DEFAULT;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	public void setSwapsWithPlayer(boolean swapsWithPlayer) {
		this.swapsWithPlayer = swapsWithPlayer;
	}

	/**
	 * 
	 *
	 * @since 3.1.0.1
	 */
	public void dispose() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				browserContext.removeMessageListener(empMessageListener);
				Browser browser = soBrowser.getBrowser();
				if (!browser.isDisposed()) {
					browser.removeTitleListener(titleListener);
					browser.removeLocationListener(locationListener);
				}
			}
		});
	}
	
	public void notifyGeneric(final String javascript) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				browserContext.executeInBrowser(javascript);
			}
		});
	}

	public void notifyVideoPlaying(final long posSecs, final long totalSecs) {
		if (posSecs == lastPosSecs) {
			return;
		}
		lastPosSecs = posSecs;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				String script = "if (videoPlaying) { videoPlaying(" + posSecs + ","
						+ totalSecs + ");}";
				browserContext.executeInBrowser(script);
				//debug("sent videoPlaying(" + posSecs + "," + totalSecs + ")");
			}
		});
	}

	public void notifyAdPlaying(final long posSecs, final long totalSecs) {
		if (posSecs == lastPosSecs) {
			return;
		}
		lastPosSecs = posSecs;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				String script = "if (adPlaying) { adPlaying(" + posSecs + ","
						+ totalSecs + ");}";
				browserContext.executeInBrowser(script);
			}
		});
	}

	public void notifyFullScreenChange(final boolean fs) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Browser browser = soBrowser.getBrowser();
				String script = "if(typeof empFullScreenSwitch == 'function') {empFullScreenSwitch(" + fs
						+ ");}";
				browserContext.executeInBrowser(script);
			}
		});
	}

	/**
	 * @param volume
	 *
	 * @since 3.1.0.1
	 */
	public void setVolume(int volume) {
		String script = "if (setVolume) { setVolume(" + volume + ");}";

		browserContext.executeInBrowser(script);
		debug("sent setVolume(" + volume + ")");
	}

	/**
	 * @param mute
	 *
	 * @since 3.1.0.1
	 */
	public void setMute(boolean mute) {
		String script = "(setMute) { setMute(" + mute + ");}";
		browserContext.executeInBrowser(script);
		debug("sent setMute(" + mute + ")");
	}
}
