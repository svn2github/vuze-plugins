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

package com.vuze.plugin.azVPN_Helper;

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

	public static final String VIEW_ID = "VPNHelper_View";

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
				MenuManager.MENU_MENUBAR,
				"ConfigView.section." + PluginConstants.CONFIG_SECTION_ID);
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
		MdiEntry entry = mdi.createEntryFromSkinRef(null, VIEW_ID, "vpnhelperview",
				"VPNHelper", null, null, true, null);
		entry.setTitleID("ConfigView.section." + PluginConstants.CONFIG_SECTION_ID);
		
		final ViewTitleInfo viewTitleInfo = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT) {
					int statusID = PluginVPNHelper.instance.checker.getCurrentStatusID();
					
					LocaleUtilities texts = UI.this.pi.getUtilities().getLocaleUtilities();

					if (statusID == CheckerCommon.STATUS_ID_OK) {
						return texts.getLocalisedMessageText("vpnhelper.indicator.ok");
					}
					if (statusID == CheckerCommon.STATUS_ID_BAD) {
						return texts.getLocalisedMessageText("vpnhelper.indicator.bad");
					}
					if (statusID == CheckerCommon.STATUS_ID_WARN) {
						return texts.getLocalisedMessageText("vpnhelper.indicator.warn");
					}
					return null;
				}
				if (propertyID == ViewTitleInfo.TITLE_INDICATOR_COLOR) {
					int statusID = PluginVPNHelper.instance.checker.getCurrentStatusID();

					if (statusID == CheckerCommon.STATUS_ID_OK) {
						return new int[] { 0, 80, 0 };
					}
					if (statusID == CheckerCommon.STATUS_ID_BAD) {
						return new int[] { 128, 30, 30 };
					}
					if (statusID == CheckerCommon.STATUS_ID_WARN) {
						return new int[] { 255, 140, 0 };
					}
					return null;
				}
				return null;
			}
		};
		
		entry.setViewTitleInfo(viewTitleInfo);

		final CheckerListener checkerListener = new CheckerListener() {

			public void protocolAddressesStatusChanged(String status) {
			}

			public void portCheckStatusChanged(String status) {
				ViewTitleInfoManager.refreshTitleInfo(viewTitleInfo);
			}

			public void portCheckStart() {
			}
		};
		PluginVPNHelper.instance.checker.addListener(checkerListener);

		entry.addListener(new MdiCloseListener() {
			public void mdiEntryClosed(MdiEntry entry, boolean userClosed) {
				if (PluginVPNHelper.instance.checker != null) {
					PluginVPNHelper.instance.checker.removeListener(checkerListener);
				}
			}
		});
		return entry;
	}


	private void addSkinPaths() {
		String path = "com/vuze/plugin/azVPN_Helper/skins/";

		String sFile = path + "skin3_" + PluginConstants.CONFIG_SECTION_ID;

		ClassLoader loader = PluginVPNHelper.class.getClassLoader();

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
