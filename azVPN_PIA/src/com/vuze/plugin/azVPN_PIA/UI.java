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

package com.vuze.plugin.azVPN_PIA;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiCloseListener;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryCreationListener;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;

public class UI implements MdiEntryCreationListener
{

	public static final String VIEW_ID = "PIA_View";

	private PluginInterface pi;

	private MenuItem menuItemShowView;

	public UI(PluginInterface pi, UISWTInstance swtInstance) {
		this.pi = pi;

		addSkinPaths();

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

		mdi.registerEntry(VIEW_ID, this);

		// Requires 4700
		mdi.loadEntryByID(VIEW_ID, false, true, null);

		UIManager uiManager = pi.getUIManager();
		menuItemShowView = uiManager.getMenuManager().addMenuItem(
				MenuManager.MENU_MENUBAR, "ConfigView.section.vpn_pia");
		menuItemShowView.addListener(new MenuItemListener() {

			public void selected(MenuItem menu, Object target) {
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				mdi.showEntryByID(UI.VIEW_ID);
			}
		});

		//swtInstance.addView(UISWTInstance.VIEW_MAIN, VIEW_ID, view.class, swtInstance);
	}
	
	public void destroy() {
		if (menuItemShowView != null) {
			menuItemShowView.remove();
			menuItemShowView = null;
		}
		
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		mdi.closeEntry(VIEW_ID);
		// Since we don't have a way to close an entry without removing the 
		// "open again" flag, manually reset the "open once" flag so the view
		// opens again when plugin is reloaded/upgraded
		COConfigurationManager.removeParameter("sb.once." + VIEW_ID);
		COConfigurationManager.removeParameter("tab.once." + VIEW_ID);

		// Requires 5610
		mdi.deregisterEntry(VIEW_ID, this);
	}
	

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntryCreationListener#createMDiEntry(java.lang.String)
	 */
	public MdiEntry createMDiEntry(String id) {
		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		MdiEntry entry = mdi.createEntryFromSkinRef(null, VIEW_ID, "piaview",
				"PIA", null, null, true, null);
		entry.setTitleID("ConfigView.section.vpn_pia");
		
		final ViewTitleInfo viewTitleInfo = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT) {
					int statusID = PluginPIA.instance.checkerPIA.getCurrentStatusID();
					
					LocaleUtilities texts = UI.this.pi.getUtilities().getLocaleUtilities();

					if (statusID == CheckerPIA.STATUS_ID_OK) {
						return texts.getLocalisedMessageText("pia.indicator.ok");
					}
					if (statusID == CheckerPIA.STATUS_ID_BAD) {
						return texts.getLocalisedMessageText("pia.indicator.bad");
					}
					if (statusID == CheckerPIA.STATUS_ID_WARN) {
						return texts.getLocalisedMessageText("pia.indicator.warn");
					}
					return null;
				}
				if (propertyID == ViewTitleInfo.TITLE_INDICATOR_COLOR) {
					int statusID = PluginPIA.instance.checkerPIA.getCurrentStatusID();

					if (statusID == CheckerPIA.STATUS_ID_OK) {
						return new int[] { 0, 80, 0 };
					}
					if (statusID == CheckerPIA.STATUS_ID_BAD) {
						return new int[] { 128, 30, 30 };
					}
					if (statusID == CheckerPIA.STATUS_ID_WARN) {
						return new int[] { 255, 140, 0 };
					}
					return null;
				}
				return null;
			}
		};
		
		entry.setViewTitleInfo(viewTitleInfo);

		final CheckerPIAListener checkerListener = new CheckerPIAListener() {

			public void protocolAddressesStatusChanged(String status) {
			}

			public void portCheckStatusChanged(String status) {
				ViewTitleInfoManager.refreshTitleInfo(viewTitleInfo);
			}

			public void portCheckStart() {
			}
		};
		PluginPIA.instance.checkerPIA.addListener(checkerListener);

		entry.addListener(new MdiCloseListener() {
			public void mdiEntryClosed(MdiEntry entry, boolean userClosed) {
				if (PluginPIA.instance.checkerPIA != null) {
					PluginPIA.instance.checkerPIA.removeListener(checkerListener);
				}
			}
		});
		return entry;
	}


	private void addSkinPaths() {
		String path = "com/vuze/plugin/azVPN_PIA/skins/";

		String sFile = path + "skin3_vpn_pia";

		ClassLoader loader = PluginPIA.class.getClassLoader();

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();

		try {
			ResourceBundle subBundle = ResourceBundle.getBundle(sFile,
					Locale.getDefault(), loader);

			skinProperties.addResourceBundle(subBundle, path, loader);

		} catch (MissingResourceException mre) {

			mre.printStackTrace();
		}
	}
}
