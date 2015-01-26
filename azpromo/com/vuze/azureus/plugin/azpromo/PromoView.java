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

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.plugins.PluginConfig;
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
import com.appadx.adcontrol.*;

/**
 * @created Sep 29, 2014
 *
 */
public class PromoView
	implements UISWTViewEventListener
{
	private AdControlSWT adControl;

	private UISWTView view;

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

			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		adControl = new AdControlSWT(ourParent, SWT.NO_SCROLL);
		fd = Utils.getFilledFormData();
		fd.height = 254;
		fd.top = new FormAttachment(lblClose, 2);
		adControl.setLayoutData(fd);

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
		options.setPubConfigURL("http://vuze-pubcfg.desktopadx.com/service/pubcfg/get.php?id=");
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
		System.out.println("PromoView: " + string);
	}

	private void refresh(final UISWTView view) {
	}

}
