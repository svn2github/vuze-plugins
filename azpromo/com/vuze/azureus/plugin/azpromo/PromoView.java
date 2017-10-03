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
import java.util.*;
import java.util.concurrent.TimeUnit;
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
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
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

/**
 * @created Sep 29, 2014
 *
 */
public class PromoView
	implements UISWTViewEventListener
{
	private static final String URL_JSON = "http://client.vuze.com/donation/sidebar_promo.php?ver=2.5";

	private static final String DEFAULT_INHOUSE_HTML = "<html><body style=\"overflow:hidden; margin:100px 10px;\"><p>Please <a target=\"_BLANK\" href=\"http://www.vuze.com/donation/donate.php?sourceRef=sidebarpromo\">Donate</a></BODY></html>";

	private PromoPlugin		plugin;
	
	private Browser adBrowser;

	private UISWTView view;

	private boolean showingInHouse = false;

	private Map mapJSON;

	private static TimerEvent timeEvent_inHouse;

	private static Map<String, Map.Entry<Date, String>> urlToLastResult = new HashMap<>();

	public PromoView() {
		plugin	= PromoPlugin.getPlugin();
	}

	// @see org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener#eventOccurred(org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent)
	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				plugin.addViewInViews( this );
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData(), event.getView());
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh(event.getView());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				plugin.removeViewInViews( this );
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				break;
		}
		return true;
	}

	private void initialize(Composite parent, final UISWTView view) {

		this.view = view;
		configureInitialPluginSize();

		final Composite ourParent = parent;

		ourParent.setVisible(false);

		FormData fd;
		fd = Utils.getFilledFormData();
		fd.bottom = null;
		fd.left = null;
		fd.right.offset = -3;

		final Label lblClose = createCloseLabel(ourParent, fd);

		final Label lblText = createPlusLabel(ourParent);

		configureLogger();

		Browser theirBrowser = findBrowser( ourParent );
		listenOnLocationChangeFor(theirBrowser);


		adBrowser = new Browser(ourParent, SWT.NO_SCROLL);
		listenBrowserOnWindowOpenFor(ourParent);

		fd = Utils.getFilledFormData();
		fd.height = 254;
		fd.top = new FormAttachment(lblClose, 2);
		adBrowser.setLayoutData(fd);

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(adBrowser, -1);
		fd.top = null;
		fd.right = null;
		fd.left.offset = 3;
		lblText.setLayoutData(fd);

		ourParent.getShell().layout(true, true);

		plugin.getPluginInterface().getUtilities().createThread("pv",
				new Runnable() {
					public void run() {
						loadInHouse();
					}
				});

	}

	private void listenBrowserOnWindowOpenFor(Composite ourParent) {
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
	}

	private void listenOnLocationChangeFor(Browser theirBrowser) {
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
	}

	private void configureLogger() {
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
	}

	private Label createPlusLabel(Composite ourParent) {
		final Label lblText = new Label(ourParent, SWT.NONE);
		lblText.setText("Plus Users Don't See Ads");
		lblText.setFont(FontUtils.getFontWithHeight(lblText.getFont(), null, 9));
		lblText.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				if (plugin.getPluginInterface() == null) {
					return;
				}

				plugin.getPluginInterface().getUtilities().createThread("LoadPromo",
						new Runnable() {

					public void run() {
						try {
							log("loadclick");
							loadInHouse();
						} catch (Throwable t) {
						}
					}
				});
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		return lblText;
	}

	private Label createCloseLabel(Composite ourParent, FormData fd) {
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
		return lblClose;
	}

	private void configureInitialPluginSize() {
		try {
			PluginConfig config = plugin.getPluginInterface().getPluginconfig();
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
	}

	protected void loadInHouse() {
		if (plugin.getPluginInterface() == null) {
			return;
		}

		String json = readStringFromUrlOrCache(URL_JSON).trim();

		log("Inhouse load");
	
		mapJSON = JSONUtils.decodeJSON(json);

		boolean isFirstShowKeyPresent = mapJSON == null;

		if (isFirstShowKeyPresent){
			
			int firstShowInMS = MapUtils.getMapInt(mapJSON, "first-show-ms", 0);
			flipTest(firstShowInMS);
			
		} else {
			
			flipTest(0);
		}

		int showEvery = isFirstShowKeyPresent ? 1000 * 60 * 15
				: MapUtils.getMapInt(mapJSON, "show-every-ms", 1000 * 60 * 5);

		if (timeEvent_inHouse != null) {
			timeEvent_inHouse.cancel();
		}

		timeEvent_inHouse = SimpleTimer.addEvent("pv", SystemTime.getOffsetTime(showEvery),
				new TimerEventPerformer() {

			public void perform(TimerEvent event) {
				loadInHouse();
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
				if (plugin.getPluginInterface() == null) {
					return;
				}

				boolean popupOnShowEvery = MapUtils.getMapBoolean(mapJSON, "popup-on-show-every", false);
				if (popupOnShowEvery) {
					plugin.addViewInSidebar();
				} else if (adBrowser == null || adBrowser.isDisposed()) {
					return;
				}
				if (adBrowser != null && ! adBrowser.isDisposed()) {
					setInHouse(true);
				}
			}
		});
	}

	protected void temporaryClose() {
		if (view == null) {
			return;
		}

		PromoPlugin.logEvent("clickx");

		UISWTInstance swtInstance = plugin.getSWTInstance();
		if (swtInstance == null) {
			return;
		}
		int result = swtInstance.promptUser("Get Vuze Plus",
				"Upgrading to Vuze Plus will remove ads from the client.",
				new String[] {
					"Not Now",
					"Upgrade"
		}, 1);

		if (result == 1) {
			PromoPlugin.logEvent("clickUpgrade");

			SBC_PlusFTUX.setSourceRef("dlg-promo");

			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
		} else {
			PromoPlugin.logEvent("clickNotNow");

			boolean canCloseOnX = MapUtils.getMapBoolean(mapJSON, "can-close-on-x", false);
			if (canCloseOnX) {
				plugin.removeViewInSidebar();
				boolean popupOnShowEvery = MapUtils.getMapBoolean(mapJSON, "popup-on-show-every", false);
				if (!popupOnShowEvery) {
					if( timeEvent_inHouse != null ) {
						timeEvent_inHouse.cancel();
					}
				}
			}
		}
	}

	protected void log(String string) {

		plugin.log(string);
	}

	private void refresh(final UISWTView view) {
	}

	private void setInHouse(boolean on) {
		long showUntil = MapUtils.getMapLong(mapJSON, "show-until", 0);
		if (showUntil > 0 && System.currentTimeMillis() > showUntil) {
			showingInHouse = false;
		} else {
			showingInHouse = on;
		}
		adBrowser.setText("");
		String html = showingInHouse ? MapUtils.getMapString(mapJSON, "html", DEFAULT_INHOUSE_HTML) : DEFAULT_INHOUSE_HTML;
		adBrowser.setText(html);
	}
	
	protected void
	destroy()
	{
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					if ( view != null ){
				}
					view.closeView();
				}
			});
	}


	public String readStringFromUrlOrCache(String url) {
		Map.Entry<Date, String> lastResult = urlToLastResult.get(url);
		Date currentTime = new Date();
		if (lastResult != null && (TimeUnit.MILLISECONDS.toSeconds(currentTime.getTime() - lastResult.getKey().getTime()))<10) {
			return lastResult.getValue();
		} else {

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

			urlToLastResult.put(url, new AbstractMap.SimpleEntry<Date, String>(new Date(), sb.toString()));
			return sb.toString();
		}
	}

}
