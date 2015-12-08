/**
 * Created on Sep 29, 2014
 *
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

package com.vuze.azureus.plugin.azpromo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.ui.swt.BrowserWrapper;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectSash;
import com.aelitis.azureus.ui.swt.utils.FontUtils;
import com.aelitis.azureus.ui.swt.views.skin.SBC_PlusFTUX;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.MapUtils;
import com.appadx.adcontrol.AdControlEventListener;
import com.appadx.adcontrol.AdControlException;
import com.appadx.adcontrol.AdControlSWT;
import com.appadx.adcontrol.IAdControlOptions;

/**
 * @created Sep 29, 2014
 *
 */
public class PromoView
	implements UISWTViewEventListener
{
	private static final String URL_JSON = "http://misc20150831.s3-website-us-east-1.amazonaws.com/test.json";

	private AdControlSWT adControl;

	private Browser adBrowser;

	private UISWTView view;

	private boolean tuxTest = false;

	private Map mapJSON;

	public PromoView() {
	}

	// @see org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener#eventOccurred(org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent)
	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				log("TYPE_CREATE Called");
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				log("TYPE_INITIALIZE Called");
				initialize((Composite) event.getData(), event.getView());
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh(event.getView());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				log("TYPE_DESTROY Called");
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				log("TYPE_DATASOURCE_CHANGED Called");
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				log("TYPE_FOCUSGAINED Called");
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				log("TYPE_FOCUSLOST Called");
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				log("TYPE_LANGUAGEUPDATE Called " + Locale.getDefault().toString());
				break;
		}
		return true;
	}

	private void initialize(Composite parent, final UISWTView view) {

		this.view = view;
		try {
			PluginConfig config = PromoPlugin.pluginInterface.getPluginconfig();
			if (!config.getPluginBooleanParameter("resized.once")) {
				config.setPluginParameter("resized.once", true);
				boolean visible = SideBar.instance.isVisible();
				if (visible) {
					final SWTSkinObjectSash soSash = (SWTSkinObjectSash) SideBar.instance.getSkin().getSkinObject(
							"sidebar-sash");
					if (soSash != null && soSash.getAboveSize() < 300) {
						soSash.setAboveSize(300);
					}
				}
			}
		} catch (Throwable t) {
		}

		final Composite ourParent = parent;

		ourParent.setVisible(false);

		FormData fd;
		fd = Utils.getFilledFormData();
		fd.bottom = null;
		fd.left = null;
		fd.right.offset = -3;

		final Label lblClose = new Label(ourParent, SWT.NONE);
		lblClose.setText("x");
		lblClose.setCursor(lblClose.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		lblClose.setLayoutData(fd);
		lblClose.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				temporaryClose();
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		final Label lblText = new Label(ourParent, SWT.NONE);
		lblText.setText("Plus Users Don't See Ads");
		lblText.setFont(FontUtils.getFontWithHeight(lblText.getFont(), null, 9));
		lblText.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				if (PromoPlugin.pluginInterface == null) {
					return;
				}

				PromoPlugin.pluginInterface.getUtilities().createThread("LoadPromo",
						new Runnable() {

					public void run() {
						try {
							log("loadclick");
							adControl.loadAd();
						} catch (Throwable t) {
						}
					}
				});

				setTuxText(false);
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		adControl = new AdControlSWT(ourParent, SWT.NO_SCROLL);
		
		
		Logger logger = Logger.getLogger( "com.appadx.adcontrol" );
		
		logger.setUseParentHandlers( false );
		
		//Logger.getLogger( "com.appadx.adcontrol" ).setLevel(Level.OFF);
		
		logger.addHandler(
			new Handler() {
				
				@Override
				public void 
				publish( LogRecord record ) 
				{
					String text = new SimpleFormatter().format(record).trim();
					
					text = text.replace( '\r',  ' ' );
					text = text.replace( '\n',  ' ' );
					
					text = text.replaceAll( "  ", " " );
					
					log( text);
				}
				
				@Override
				public void flush() {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void close() {
					// TODO Auto-generated method stub
					
				}
			});

		
		fd = Utils.getFilledFormData();
		fd.height = 254;
		fd.top = new FormAttachment(lblClose, 2);
		adControl.setLayoutData(fd);

		Browser theirBrowser = findBrowser( ourParent );
		
		if ( theirBrowser != null ){
							
			theirBrowser.addLocationListener(new LocationListener() {
				public void changed(LocationEvent arg0) {
				}

				public void changing(LocationEvent event) {
					
					String str = String.valueOf( event );
					
					if ( str.contains( "://mono.vizu.com" )){
						
						event.doit = false;
					}
					
					if ( Constants.getCurrentVersion().endsWith( "_CVS" )){
						
						log( str );
					}
				}
			});
		}
		
		
		adBrowser = new Browser(ourParent, SWT.NO_SCROLL);
		adBrowser.setVisible(false);
		adBrowser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				final BrowserWrapper subBrowser = Utils.createSafeBrowser(ourParent,
						Utils.getInitialBrowserStyle(SWT.NONE));
				subBrowser.addLocationListener(new LocationListener() {
					public void changed(LocationEvent arg0) {
					}

					public void changing(LocationEvent event) {
						if (event.location == null || !event.location.startsWith("http")) {
							return;
						}
						event.doit = false;
						Utils.launch(event.location);

						Utils.execSWTThreadLater(1000, new AERunnable() {
							public void runSupport() {
								subBrowser.dispose();
							}
						});
					}
				});
				subBrowser.setBrowser(event);
			}
		});
		fd = Utils.getFilledFormData();
		fd.height = 254;
		fd.top = new FormAttachment(lblClose, 2);
		adBrowser.setLayoutData(fd);

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(adControl, -1);
		fd.top = null;
		fd.right = null;
		fd.left.offset = 3;
		lblText.setLayoutData(fd);

		adControl.addAdControlEventListener(new AdControlEventListener() {

			public void onAdClicked() {
				log("clicked");
				super.onAdClicked();
			}

			public void onAdCompleted() {
				log("complete");
				super.onAdCompleted();
			}

			public void onAdError(String arg0, int arg1, int arg2) {
				log("" + arg1 + "/" + arg2 + ": " + arg0);
				super.onAdError(arg0, arg1, arg2);
			}

			public void onAdLogMessage(String mesg) {
				log(mesg);
				super.onAdLogMessage(mesg);
			}

			public void onAdStarted() {
				log("started");
				super.onAdStarted();
			}
		});
		
		String pubID = PromoPlugin.pluginInterface.getPluginProperties().getProperty(
				"PubID", "mawra2ag1");

		//int reloadTime = Integer.parseInt(PromoPlugin.pluginInterface.getPluginProperties().getProperty(
		//		"ReloadSecs", "86400"));
		//log("pubID len=" + pubID.length() + ";reload in " + reloadTime);
		log("pubID len=" + pubID.length());

		IAdControlOptions options = adControl.getOptions();
		options.setPlayerOption(IAdControlOptions.Player.AUTO_MUTE, true);
		options.setPubID(pubID);
		options.setPageName("vuze");
		options.setPubConfigURL(
				"http://vuze-pubcfg.desktopadx.com/service/pubcfg/get.php?id=");
		options.setRequestDomain("btpr.vuze.com");

		//options.setPublisherDefaultAdReloadTime(reloadTime);

		PromoPlugin.pluginInterface.getUtilities().createThread("LoadPromo",
				new Runnable() {

					public void run() {
						try {
							log("load");
							adControl.loadAd();
							Utils.execSWTThread(new Runnable() {
								public void run() {
									log("show");
									if (ourParent.isDisposed()) {
										return;
									}
									ourParent.setVisible(true);
								}
							});

							PromoPlugin.logEvent("shown");
						} catch (AdControlException e1) {
							//  Bad or missing Publisher Configuration - Connection timed out: connect
							e1.printStackTrace();
							Utils.execSWTThread(new Runnable() {
								public void run() {
									log("closeErr");
									view.closeView();
								}
							});
						}
					}
				});
		adControl.getShell().layout(true, true);

		PromoPlugin.pluginInterface.getUtilities().createThread("pv",
				new Runnable() {
					public void run() {
						if (PromoPlugin.pluginInterface == null) {
							return;
						}
						boolean first = mapJSON == null;
						
						int showEvery = 1000 * 60 * 15;
								
						String json = readStringFromUrl(URL_JSON).trim();
						
						if ( json.startsWith( "{" )){
							mapJSON = JSONUtils.decodeJSON(json);
	
							if (first){
								
								int firstShowInMS = MapUtils.getMapInt(mapJSON, "first-show-ms",
										1000 * 60 * ((Math.random() > 0.8) ? 0 : 2));
								flipTest(firstShowInMS);
								
							} else {
								
								flipTest(0);
							}
							
							 showEvery = mapJSON == null ? 1000 * 60 * 15
									: MapUtils.getMapInt(mapJSON, "show-every-ms", 1000 * 60 * 5);
						}
						
						
						SimpleTimer.addEvent("pv", SystemTime.getOffsetTime(showEvery),
								new TimerEventPerformer() {
						
							public void perform(TimerEvent event) {
								run();
							}
						});

					}
				});

	}

	private Browser
	findBrowser(
		Control comp )
	{
		if ( comp instanceof Browser ){
			
			return((Browser)comp);
			
		}else if ( comp instanceof Composite ){
			
			Control[] kids = ((Composite)comp).getChildren();
				
			for ( Control c: kids ){
				
				Browser b = findBrowser( c );
				
				if ( b != null ){
					
					return( b );
				}
			}
		}
			
		return( null );
	}
	
	protected void flipTest(int delay) {
		Utils.execSWTThreadLater(delay, new AERunnable() {
			public void runSupport() {
				if (PromoPlugin.pluginInterface == null) {
					return;
				}

				if (adBrowser == null || adBrowser.isDisposed()) {
					return;
				}

				setTuxText(!tuxTest);
			}
		});
	}

	protected void temporaryClose() {
		if (view == null) {
			return;
		}
		view.closeView();
		PromoPlugin.logEvent("clickx");
		if (PromoPlugin.swtInstance == null) {
			return;
		}
		int result = PromoPlugin.swtInstance.promptUser("Get Vuze Plus",
				"Upgrading to Vuze Plus will remove ads from the client.",
				new String[] {
					"Not Now",
					"Upgrade"
		}, 1);

		if (result == 1) {
			SBC_PlusFTUX.setSourceRef("dlg-promo");

			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
		}
	}

	protected void log(String string) {

		PromoPlugin.log(string);
	}

	private void refresh(final UISWTView view) {
	}

	private void setTuxText(boolean on) {
		long showUntil = MapUtils.getMapLong(mapJSON, "show-until", 0);
		if (showUntil > 0 && System.currentTimeMillis() > showUntil) {
			tuxTest = false;
		} else {
			tuxTest = on;
		}
		adBrowser.setVisible(tuxTest);
		adControl.setVisible(!tuxTest);
		if (tuxTest) {
			adBrowser.setText(MapUtils.getMapString(mapJSON, "html", null));
		}
	}

	public static String readStringFromUrl(String url) {
		StringBuffer sb = new StringBuffer();
		try {
			URL _url = new URL(url);
			HttpURLConnection con = (HttpURLConnection) _url.openConnection();

			con.setConnectTimeout(30000);
			con.setReadTimeout(30000);
			InputStream is = con.getInputStream();

			byte[] buffer = new byte[256];

			int read = 0;

			while ((read = is.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, read));
			}
			con.disconnect();

		} catch (Throwable e) {
			//e.printStackTrace();
		}
		return sb.toString();
	}

}
