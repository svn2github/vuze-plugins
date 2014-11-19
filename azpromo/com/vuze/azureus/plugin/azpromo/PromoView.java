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

import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectSash;
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
				log("TYPE_LANGUAGEUPDATE Called "
						+ Locale.getDefault().toString());
				break;
		}
		return true;
	}

	private void initialize(Composite parent, final UISWTView view) {

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

		final Label lbl = new Label(ourParent, SWT.NONE);
		lbl.setText("x");
		lbl.setCursor(lbl.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		lbl.setLayoutData(fd);
		lbl.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				view.closeView();
				PromoPlugin.pluginInterface.getPluginconfig().setPluginParameter(
						"enabled", false);
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		adControl = new AdControlSWT(ourParent, SWT.NO_SCROLL);
		fd = Utils.getFilledFormData();
		fd.height = 254;
		fd.top = new FormAttachment(lbl, 2);
		adControl.setLayoutData(fd);
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
		IAdControlOptions options = adControl.getOptions();
		options.setPlayerOption(IAdControlOptions.Player.AUTO_MUTE, true);
		options.setPubID("mawra2ag1");
		options.setPageName("vuze");
		options.setPubConfigURL("http://vuze-pubcfg.desktopadx.com/service/pubcfg/get.php?id=");   
		options.setRequestDomain("btpr.vuze.com");

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

	protected void log(String string) {
		System.out.println("PromoView: " + string);
	}

	private void refresh(final UISWTView view) {
	}

}
