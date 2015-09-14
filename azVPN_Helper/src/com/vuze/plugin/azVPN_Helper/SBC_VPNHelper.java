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

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectButton;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;

public class SBC_VPNHelper
	extends SkinView
	implements CheckerListener
{

	private SWTSkinObjectText soAddresses;

	private SWTSkinObjectButton soAddressesButton;

	private SWTSkinObjectText soPFStatus;

	private SWTSkinObjectButton soPFButton;

	private SWTSkinButtonUtility btnPFCheck;

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	@Override
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		soAddresses = (SWTSkinObjectText) getSkinObject("addresses");
		Control control = soAddresses.getControl();
		Menu clipMenuA = new Menu(control);
		MenuItem miClipA = new MenuItem(clipMenuA, SWT.PUSH);
		miClipA.setText(
				MessageText.getString("MyTorrentsView.menu.thisColumn.toClipboard"));
		miClipA.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				new Clipboard(e.display).setContents(new Object[] {
					soPFStatus.getText()
				}, new Transfer[] {
					TextTransfer.getInstance()
				});
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		control.setMenu(clipMenuA);

		soAddressesButton = (SWTSkinObjectButton) getSkinObject("addresses-button");
		final SWTSkinButtonUtility btnAddresses = new SWTSkinButtonUtility(
				soAddressesButton);
		soAddressesButton.addSelectionListener(new ButtonListenerAdapter() {
			@Override
			public void pressed(SWTSkinButtonUtility buttonUtility,
					SWTSkinObject skinObject, int stateMask) {
				btnAddresses.setDisabled(true);
				soAddresses.switchSuffix("-disabled");
				Utils.getOffOfSWTThread(new AERunnable() {
					@Override
					public void runSupport() {
						PluginVPNHelper.instance.checker.calcProtocolAddresses();
						btnAddresses.setDisabled(false);
						soAddresses.switchSuffix(null);
					}
				});
			}
		});

		soPFStatus = (SWTSkinObjectText) getSkinObject("port-forwarding-status");
		Control controlStatus = soPFStatus.getControl();
		Menu clipMenuPF = new Menu(controlStatus);
		MenuItem miClipPF = new MenuItem(clipMenuPF, SWT.PUSH);
		miClipPF.setText(
				MessageText.getString("MyTorrentsView.menu.thisColumn.toClipboard"));
		miClipPF.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				new Clipboard(e.display).setContents(new Object[] {
					soPFStatus.getText()
				}, new Transfer[] {
					TextTransfer.getInstance()
				});
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		controlStatus.setMenu(clipMenuPF);

		soPFButton = (SWTSkinObjectButton) getSkinObject("port-forwarding-button");
		btnPFCheck = new SWTSkinButtonUtility(soPFButton);
		soPFButton.addSelectionListener(new ButtonListenerAdapter() {
			@Override
			public void pressed(SWTSkinButtonUtility buttonUtility,
					SWTSkinObject skinObject, int stateMask) {
				Utils.getOffOfSWTThread(new AERunnable() {
					@Override
					public void runSupport() {
						if (PluginVPNHelper.instance.checker == null) {
							return;
						}
						PluginVPNHelper.instance.checker.portBindingCheck();
					}
				});
			}
		});

		SWTSkinObjectButton soConfigButton = (SWTSkinObjectButton) getSkinObject(
				"config-button");
		soConfigButton.addSelectionListener(new ButtonListenerAdapter() {
			/* (non-Javadoc)
			 * @see com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter#pressed(com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility, com.aelitis.azureus.ui.swt.skin.SWTSkinObject, int)
			 */
			@Override
			public void pressed(SWTSkinButtonUtility buttonUtility,
					SWTSkinObject skinObject, int stateMask) {
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG,
						PluginConstants.CONFIG_SECTION_ID);
			}
		});

		PluginVPNHelper.instance.addListener(this);
		
		checkerChanged(PluginVPNHelper.instance.checker);

		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	@Override
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		PluginVPNHelper.instance.removeListener(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see com.vuze.plugin.azVPN_Helper.CheckerListener#portCheckStatusChanged(java.lang.String)
	 */
	public void portCheckStatusChanged(String status) {
		soPFStatus.setText(status);
		if (btnPFCheck != null) {
			btnPFCheck.setDisabled(false);
		}
	}

	/* (non-Javadoc)
	 * @see com.vuze.plugin.azVPN_Helper.CheckerListener#protocolAddressesStatusChanged(java.lang.String)
	 */
	public void protocolAddressesStatusChanged(String status) {
		soAddresses.setText(status);
	}

	/* (non-Javadoc)
	 * @see com.vuze.plugin.azVPN_Helper.CheckerListener#portCheckStart()
	 */
	public void portCheckStart() {
		if (btnPFCheck != null) {
			btnPFCheck.setDisabled(true);
		}
	}

	/* (non-Javadoc)
	 * @see com.vuze.plugin.azVPN_Helper.CheckerListener#checkerChanged(com.vuze.plugin.azVPN_Helper.CheckerCommon)
	 */
	public void checkerChanged(CheckerCommon checker) {
		soPFStatus.setTextID(checker == null ? "vpnhelper.select.vpn" : null); 
		btnPFCheck.setDisabled(checker == null);
		soAddresses.setText("");
	}

}
