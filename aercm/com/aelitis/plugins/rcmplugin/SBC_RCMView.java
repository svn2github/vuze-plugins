/**
 * Created on Feb 24, 2009
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

package com.aelitis.plugins.rcmplugin;


import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.core.content.RelatedContentManagerListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.InfoBarUtil;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.plugins.rcmplugin.RelatedContentUI.RCMItemContent;
import com.aelitis.plugins.rcmplugin.RelatedContentUI.RCMItemSubscriptions;
import com.aelitis.plugins.rcmplugin.columns.*;


public class 
SBC_RCMView
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<RelatedContent>
{
	public static final String TABLE_RCM = "RCM";

	private static boolean columnsAdded = false;

	private static RelatedContentManager	manager;
	
	static{
		try{
			manager = RelatedContentManager.getSingleton();
			
		}catch( Throwable e ){
			
			Debug.out(e);
		}
	}
	
	private TableViewSWT<RelatedContent> tv_related_content;

	private MdiEntry 	mdi_entry;
	private Composite			table_parent;
	private boolean				space_reserved;
	
	
	private Text txtFilter;

	private RelatedContentManagerListener current_rcm_listener;

	protected int minSeeds;

	private boolean showUnknownSeeds = true;

	private long createdMsAgo;

	private int minRank;

	private boolean showIndirect = true;

	private Object ds;
	
	public
	SBC_RCMView()
	{
	}
	
	public Object 
	skinObjectInitialShow(
		SWTSkinObject skinObject, Object params ) 
	{
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initColumns( core );
				}
			});

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		
		if ( mdi != null ){

			if ( ds instanceof RCMItemContent ){
				mdi_entry = ((RCMItemContent) ds).getSideBarEntry();
				manager.reserveTemporarySpace();
				
				space_reserved = true;
				
			}else if ( ds instanceof RCMItemSubscriptions ){
				
				mdi_entry = ((RCMItemSubscriptions) ds).getSideBarEntry();
				manager.reserveTemporarySpace();
				
				space_reserved = true;
			} else {
				mdi_entry = mdi.getEntry( RelatedContentUI.SIDEBAR_SECTION_RELATED_CONTENT );
			}
			
			mdi_entry.addToolbarEnabler(this);

		}

		SWTSkinObjectTextbox soFilterBox = (SWTSkinObjectTextbox) getSkinObject("filterbox");
		if (soFilterBox != null) {
			txtFilter = soFilterBox.getTextControl();
		}

		final SWTSkinObject soFilterArea = getSkinObject("filterarea");
		if (soFilterArea != null) {
			
			SWTSkinObjectToggle soFilterButton = (SWTSkinObjectToggle) getSkinObject("filter-button");
			if (soFilterButton != null) {
				soFilterButton.addSelectionListener(new SWTSkinToggleListener() {
					public void toggleChanged(SWTSkinObjectToggle so, boolean toggled) {
						soFilterArea.setVisible(toggled);
						Utils.relayout(soFilterArea.getControl().getParent());
					}
				});
			}
			
			SWTSkinObjectButton soSourcesButton = (SWTSkinObjectButton)getSkinObject("sources-button");
			if (soSourcesButton != null) {
				soSourcesButton.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				@Override
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) 
					{
						final String param_name = "Plugin.aercm.sources.setlist";
						
						List<String> list = RelatedContentUI.getSingleton().getPlugin().getSourcesList();
						
						SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
								"!Swarm Source Selection!", "!Enter the names of enabled swarm sources, or to enable all enter '*'!" );
						
						String 	text = "";
						
						for ( String s: list ){
							
							text += s + "\r\n";
						}
													
						entryWindow.setPreenteredText( text, false );
						
						entryWindow.selectPreenteredText( false );
						
						entryWindow.setMultiLine( true );
						
						entryWindow.setLineHeight( list.size() + 3 );
						
						entryWindow.prompt(new UIInputReceiverListener() {
							public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
								if (!entryWindow.hasSubmittedInput()) {
									return;
								}
								
								String input = entryWindow.getSubmittedInput();
								
								if ( input == null ){
									
									input = "";
									
								}else{
									
									input = input.trim();
								}
								
								String[] lines = input.split( "\n" );
								
								List<String> list = new ArrayList<String>();
								
								for ( String line: lines ){
									
									line = line.trim();
									
									if ( line.length() > 0  ){
										
										list.add( line );
									}
								}
								
								COConfigurationManager.setParameter( param_name, list );
								
								refilter();
							}
						});

					}
				});
			}
			Composite parent = (Composite) soFilterArea.getControl();
	
			Label label;
			FormData fd;
			GridLayout layout;
			int sepHeight = 20;
			
			Composite cRow = new Composite(parent, SWT.NONE);
			fd = Utils.getFilledFormData();
			cRow.setLayoutData(fd);
			RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
			rowLayout.spacing = 5;
			rowLayout.marginBottom = rowLayout.marginTop = rowLayout.marginLeft = rowLayout.marginRight = 0; 
			rowLayout.center = true;
			cRow.setLayout(rowLayout);
			
			

			/////
			
			
			Composite cMinSeeds = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinSeeds.setLayout(layout);
			
			Label lblMinSeeds = new Label(cMinSeeds, SWT.NONE);
			lblMinSeeds.setText(MessageText.getString("rcmview.filter.minSeeds"));
			Spinner spinMinSeeds = new Spinner(cMinSeeds, SWT.BORDER);
			spinMinSeeds.setMinimum(0);
			spinMinSeeds.setSelection(minSeeds);
			spinMinSeeds.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					minSeeds = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});
			
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cCreatedAgo = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cCreatedAgo.setLayout(layout);
			Label lblCreatedAgo = new Label(cCreatedAgo, SWT.NONE);
			lblCreatedAgo.setText(MessageText.getString("rcmview.filter.createdAgo"));
			Spinner spinCreatedAgo = new Spinner(cCreatedAgo, SWT.BORDER);
			spinCreatedAgo.setMinimum(0);
			spinCreatedAgo.setMaximum(999);
			spinCreatedAgo.setSelection((int) (createdMsAgo / 86400000L));
			spinCreatedAgo.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					((Spinner) event.widget).setMaximum(999);
					createdMsAgo = ((Spinner) event.widget).getSelection() * 86400L*1000L;
					refilter();
				}
			});
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cMinRank = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinRank.setLayout(layout);
			Label lblMinRank = new Label(cMinRank, SWT.NONE);
			lblMinRank.setText(MessageText.getString("rcmview.filter.minRank"));;
			Spinner spinMinRank = new Spinner(cMinRank, SWT.BORDER);
			spinMinRank.setMinimum(0);
			spinMinRank.setSelection(minRank);
			spinMinRank.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					minRank = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});

			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Button chkShowPrivate = new Button(cRow, SWT.CHECK);
			chkShowPrivate.setText( MessageText.getString( "rcm.header.show_indirect" ));
			chkShowPrivate.setSelection(showIndirect );
			chkShowPrivate.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showIndirect = ((Button) event.widget).getSelection();
					refilter();
				}
			});
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Button chkShowUnknownSeeds = new Button(cRow, SWT.CHECK);
			chkShowUnknownSeeds.setText(MessageText.getString("rcmview.filter.showUnknown"));
			chkShowUnknownSeeds.setSelection(showUnknownSeeds);
			chkShowUnknownSeeds.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showUnknownSeeds = ((Button) event.widget).getSelection();
					refilter();
				}
			});
			

			parent.layout(true);
		}

		new InfoBarUtil(skinObject, "rcmview.infobar", false,
				"rcm.infobar", "rcm.view.infobar") {
			public boolean allowShow() {
				return true;
			}
		};

		return null;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#dataSourceChanged(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object dataSourceChanged(SWTSkinObject skinObject, Object params) {
		ds = params;
		return super.dataSourceChanged(skinObject, params);
	}
	
	private boolean isOurContent(RelatedContent c) {
		boolean show = ((c.getSeeds() >= minSeeds) || (showUnknownSeeds && c.getSeeds() < 0)) 
			&& (createdMsAgo == 0 || (SystemTime.getCurrentTime() - c.getPublishDate() < createdMsAgo))
			&& ((c.getRank() >= minRank))
			&& (showIndirect || c.getHash() != null);
		
		if ( show ){
			
			show = RelatedContentUI.getSingleton().getPlugin().isVisible( c );
		}
		
		return( show );
	}


	protected void refilter() {
		tv_related_content.refilter();
	}


	private void 
	initColumns(
		AzureusCore core ) 
	{
		synchronized( SBC_RCMView.class ){
			
			if ( columnsAdded ){
			
				return;
			}
		
			columnsAdded = true;
		}
		
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		
		TableManager tableManager = uiManager.getTableManager();
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_New.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_New(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Rank.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Rank(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Level.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Level(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Title.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Title(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Actions.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Actions(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Hash.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Hash(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Tracker.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Tracker(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Size.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Size(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Created.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Created(column);
						}
					});
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Seeds.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Seeds(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Peers.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Peers(column);
						}
					});

		
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_LastSeen.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_LastSeen(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_RelatedTo.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_RelatedTo(column);
						}
					});
	}

	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		RelatedContentUI ui = RelatedContentUI.getSingleton();
		if (ui != null && !ui.getPlugin().hasFTUXBeenShown()) {
			 RelatedContentUI.showFTUX(getSkinObject("rcm-list"));
		} else {
			SWTSkinObject so_list = getSkinObject("rcm-list");

			if ( so_list != null ){
				so_list.setVisible(true);
			}
		}


		SWTSkinObject so_list = getSkinObject("rcm-list");
		
		if ( so_list != null ){
			
			initTable((Composite) so_list.getControl());
		}
		
		return null;
	}

	public Object 
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_related_content != null ){
				
				tv_related_content.delete();
				
				tv_related_content = null;
			}
			
			if (manager != null && current_rcm_listener != null) {
				
				manager.removeListener( current_rcm_listener );
				
				current_rcm_listener = null;
			}
		}

		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		return( super.skinObjectHidden(skinObject, params));
	}

	public Object
	skinObjectDestroyed(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_related_content != null ){
				
				tv_related_content.delete();
				
				tv_related_content = null;
			}

			if (manager != null && current_rcm_listener != null) {
				
				manager.removeListener( current_rcm_listener );
				
				current_rcm_listener = null;
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		if ( space_reserved ){
		
			space_reserved = false;
			
			manager.releaseTemporarySpace();
		}
		
		return( super.skinObjectDestroyed(skinObject, params));
	}
	
	private void 
	initTable(
		Composite control ) 
	{
		tv_related_content = TableViewFactory.createTableViewSWT(
				RelatedContent.class, 
				TABLE_RCM,
				TABLE_RCM, 
				new TableColumnCore[0], 
				ColumnRC_Rank.COLUMN_ID, 
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );
		if (txtFilter != null) {
			tv_related_content.enableFilterCheck(txtFilter, this);
		}
		tv_related_content.setRowDefaultHeight(16);
		SWTSkinObject soSizeSlider = getSkinObject("table-size-slider");
		if (soSizeSlider instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer so = (SWTSkinObjectContainer) soSizeSlider;
			if (!tv_related_content.enableSizeSlider(so.getComposite(), 16, 100)) {
				so.setVisible(false);
			}
		}
		
		TableColumnManager.getInstance().setDefaultColumnNames(TABLE_RCM, new String[] {
					ColumnRC_New.COLUMN_ID,
					ColumnRC_Rank.COLUMN_ID,
					ColumnRC_Title.COLUMN_ID,
					ColumnRC_Actions.COLUMN_ID,
					ColumnRC_Size.COLUMN_ID,
					ColumnRC_Created.COLUMN_ID,
					ColumnRC_Seeds.COLUMN_ID,
					ColumnRC_Peers.COLUMN_ID,
		});
		table_parent = new Composite(control, SWT.NONE);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv_related_content.addSelectionListener(new TableSelectionListener() {

			public void 
			selected(
				TableRowCore[] rows) 
			{
				ArrayList<ISelectedContent>	valid = new ArrayList<ISelectedContent>();

				for (int i=0;i<rows.length;i++){
					
					final RelatedContent rc = (RelatedContent)rows[i].getDataSource();
					
					if ( rc.getHash() != null ){
						SelectedContent sc = new SelectedContent(Base32.encode(rc.getHash()), rc.getTitle());
						sc.setDownloadInfo(new DownloadUrlInfo(
								UrlUtils.getMagnetURI(rc.getHash()) + "&dn=" + rc.getTitle()));
						valid.add(sc);
					}
				}
				
				ISelectedContent[] sels = valid.toArray( new ISelectedContent[valid.size()] );
				
				SelectedContentManager.changeCurrentlySelectedContent( "IconBarEnabler", sels, null );
				
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void mouseExit(TableRowCore row) {
			}

			public void mouseEnter(TableRowCore row) {
			}

			public void focusChanged(TableRowCore focus) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void deselected(TableRowCore[] rows) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
		}, false);

		tv_related_content.addLifeCycleListener(
			new TableLifeCycleListener() 
			{
				private Set<RelatedContent>	content_set = new HashSet<RelatedContent>();
				
				private int liveness_marker;
				
				public void 
				tableViewInitialized() 
				{
					final int current_liveness_marker = ++liveness_marker;
					
					current_rcm_listener = 
						new RelatedContentManagerListener()
						{
							public void
							contentFound(
								RelatedContent[]	content )
							{							
							}

							public void
							contentChanged(
								RelatedContent[]	content )
							{
								if (tv_related_content == null) {
									return;
								}

								final java.util.List<RelatedContent> hits = new ArrayList<RelatedContent>( content.length );

								synchronized( content_set ){
									
									if ( liveness_marker != current_liveness_marker ){
										
										return;
									}							
								
									for ( RelatedContent c: content ){

										if ( content_set.contains( c )){
									
											hits.add( c );
										}
									}
								}
								
								if ( hits.size() > 0 ){
									
									for (RelatedContent rc : hits) {
										TableRowCore row = tv_related_content.getRow(rc);
										if (row != null) {
											row.refresh(true);
										}
									}
								}
							}
							
							public void 
							contentRemoved(
								final RelatedContent[] content )
							{
								final java.util.List<RelatedContent> hits = new ArrayList<RelatedContent>( content.length );
								
								synchronized( content_set ){
									
									if ( liveness_marker != current_liveness_marker ){
										
										return;
									}
								
									for ( RelatedContent c: content ){
									
										if ( content_set.remove( c )){
									
											hits.add( c );
										}
									}
								}
								
								if ( hits.size() > 0 ){
									
									Utils.execSWTThread(
											new Runnable()
											{
												public void
												run()
												{
													if ( tv_related_content != null ){
														
														tv_related_content.removeDataSources( hits.toArray( new RelatedContent[ hits.size()] ));
													}
												}
											});
								}
							}
							
							public void
							contentChanged()
							{
								if ( tv_related_content != null ){
									
									tv_related_content.refreshTable( false );
								}
							}
							
							public void
							contentReset()
							{
								if ( tv_related_content != null ){
									
									tv_related_content.removeAllTableRows();
								}
							}
						};
						
					manager.addListener( current_rcm_listener );
				
					Object data_source = mdi_entry.getDatasource();
					
					if ( data_source instanceof RelatedContentEnumerator ){
						
						final TableViewSWT<RelatedContent> f_table = tv_related_content;
						
						((RelatedContentEnumerator)data_source).enumerate(
							new RelatedContentEnumerator.RelatedContentEnumeratorListener()
							{
								public void
								contentFound(
									RelatedContent[]	content )
								{
									ArrayList<RelatedContent> new_content = null;
									
									synchronized( content_set ){
										
										if ( liveness_marker != current_liveness_marker ){
											
											return;
										}
										
										for ( RelatedContent c: content ){
											
											if ( content_set.contains( c )){
												
												if ( new_content == null ){
													
													new_content = new ArrayList<RelatedContent>( content.length );
													
													for ( RelatedContent c2: content ){
														
														if ( c == c2 ){
															
															break;
														}
														
														if (isOurContent(c2)) {
															new_content.add( c2 );
														}
													}
												}									
											}else{
												
												if ( new_content != null ){
													if (isOurContent(c)) {
  													
  													new_content.add( c );
													}
												}
											}
										}
					
										if ( new_content != null ){
											
											content = new_content.toArray( new RelatedContent[ new_content.size()]);
										}
										
										content_set.addAll( Arrays.asList( content ));
									}
									
									if ( content.length > 0 ){
										
										final RelatedContent[] f_content = content; 
										
										Utils.execSWTThread(
											new Runnable()
											{
												public void
												run()
												{
													if ( tv_related_content == f_table ){
													
														synchronized( content_set ){
															
															if ( liveness_marker != current_liveness_marker ){
																
																return;
															}
														}
														
														f_table.addDataSources( f_content );
													}
												}
											});
									}
								}

							});
					}
				}

				public void 
				tableViewDestroyed() 
				{
					manager.removeListener( current_rcm_listener );
					
					synchronized( content_set ){
						
						liveness_marker++;
					
						content_set.clear();
					}
				}
			});

		RelatedContentUI ui = RelatedContentUI.getSingleton();
		
		final Image	swarm_image = ui==null?null:ui.getSwarmImage();
		
		tv_related_content.addMenuFillListener(
			new TableViewSWTMenuFillListener() 
			{
				public void 
				fillMenu(String sColumnName, Menu menu)
				{
					Object[] _related_content = tv_related_content.getSelectedDataSources().toArray();

					final RelatedContent[] related_content = new RelatedContent[_related_content.length];

					System.arraycopy(_related_content, 0, related_content, 0, related_content.length);

					final MenuItem assoc_item = new MenuItem(menu, SWT.PUSH);

					if ( swarm_image != null && !swarm_image.isDisposed()){
					
						assoc_item.setImage( swarm_image );
					}
					
					assoc_item.setText(MessageText.getString("rcm.menu.discovermore"));

					final ArrayList<RelatedContent> assoc_ok = new ArrayList<RelatedContent>();
					
					for ( RelatedContent c: related_content ){
						
						if ( c.getHash() != null ){
							
							assoc_ok.add( c );
						}
					}
					
					assoc_item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e ){
							
							int	 i = 0;
							
							RelatedContentUI ui = RelatedContentUI.getSingleton();
							
							if ( ui != null ){
								
								for ( RelatedContent c: assoc_ok ){
								
									ui.addSearch( c.getHash(), c.getTitle());
									
									i++;
									
									if ( i > 8 ){
										
										break;
									}
								}
							}
						};
					});
					
					if ( assoc_ok.size() == 0 ){
						
						assoc_item.setEnabled( false );
					}

					MenuItem item;
					item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("rcm.menu.google.hash"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							String s = ByteFormatter.encodeString(related_content[0].getHash());
							String URL = "https://google.com/search?q=" + UrlUtils.encode(s);
							launchURL(URL);
						};
					});

					item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("rcm.menu.gis"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							String s = related_content[0].getTitle();
							s = s.replaceAll("[-_]", " ");
							String URL = "http://images.google.com/images?q=" + UrlUtils.encode(s);
							launchURL(URL);
						}

					});

					item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("rcm.menu.google"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							String s = related_content[0].getTitle();
							s = s.replaceAll("[-_]", " ");
							String URL = "https://google.com/search?q=" + UrlUtils.encode(s);
							launchURL(URL);
						};
					});

					
					new MenuItem(menu, SWT.SEPARATOR );

					final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

					remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

					Utils.setMenuItemImage( remove_item, "delete" );

					remove_item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							manager.delete( related_content );
						};
					});
				}

				public void 
				addThisColumnSubMenu(
					String columnName, Menu menuThisColumn) 
				{
				}
			});

		tv_related_content.addKeyListener(
				new KeyListener()
				{
					public void 
					keyPressed(
						KeyEvent e )
					{
						if ( e.stateMask == 0 && e.keyCode == SWT.DEL ){
							
							Object[] selected;
							
							synchronized (this) {
								
								if ( tv_related_content == null ){
									
									selected = new Object[0];
									
								}else{
								
									selected = tv_related_content.getSelectedDataSources().toArray();
								}
							}
							
							RelatedContent[] content = new RelatedContent[ selected.length ];
							
							for ( int i=0;i<content.length;i++){
								
								content[i] = (RelatedContent)selected[i];
							}
							
							manager.delete( content );
							
							e.doit = false;
						}
					}
					
					public void 
					keyReleased(
						KeyEvent arg0 ) 
					{
					}
				});
		
		tv_related_content.initialize( table_parent );

		control.layout(true);
	}
	

	public String 
	getUpdateUIName() 
	{
		return( "RCMView" );
	}

	public void 
	updateUI() 
	{
		if ( tv_related_content != null ){
			
			tv_related_content.refreshTable( false );
		}
	}


	// @see org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck#filterCheck(java.lang.Object, java.lang.String, boolean)
	public boolean filterCheck(RelatedContent ds, String filter, boolean regex) {
		if (!isOurContent(ds)) {
			return false;
		}

		if ( filter == null || filter.length() == 0 ){
			
			return( true );
		}

		try {
			String name = ds.getTitle();
			String s = regex ? filter : "\\Q" + filter.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
  		Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
  
  		return pattern.matcher(name).find();
		} catch (Exception e) {
			return true;
		}
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck#filterSet(java.lang.String)
	public void filterSet(String filter) {
	}

	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if (item.getID().equals("remove")) {
			Object[] _related_content = tv_related_content.getSelectedDataSources().toArray();
			
			if ( _related_content.length > 0 ){
				
				RelatedContent[] related_content = new RelatedContent[_related_content.length];
				
				System.arraycopy( _related_content, 0, related_content, 0, related_content.length );
				
				manager.delete( related_content );
			}
			return true;
		}
		return false;
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		if (tv_related_content == null) {
			return;
		}
		list.put("remove", tv_related_content.getSelectedDataSources().size() > 0 ? UIToolBarItem.STATE_ENABLED : 0);
	}

	private void launchURL(String s) {
		Program program = Program.findProgram(".html");
		if (program != null && program.getName().contains("Chrome")) {
			try {
				Field field = Program.class.getDeclaredField("command");
				field.setAccessible(true);
				String command = (String) field.get(program);
				command = command.replaceAll("%[1lL]", s);
				command = command.replace(" --", "");
				System.out.println(command + " -incognito");
				PluginInitializer.getDefaultInterface().getUtilities().createProcess(command + " -incognito");
			} catch (Exception e1) {
				e1.printStackTrace();
				Utils.launch(s);
			}
		} else {
			Utils.launch(s);
		}
	};


}
