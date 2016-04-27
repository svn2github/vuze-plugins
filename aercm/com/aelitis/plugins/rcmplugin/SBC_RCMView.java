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
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
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
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemContent;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemSubView;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemSubscriptions;
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

	private MdiEntry 			mdi_entry;
	private Composite			table_parent;
	private boolean				space_reserved;
	
	
	private Text txtFilter;

	private RelatedContentManagerListener current_rcm_listener;

	protected int minSeeds;

	private boolean showUnknownSeeds = true;

	private long createdMsAgo;

	private int minRank;

	private int minSize;
	
	private boolean showIndirect = true;
	
	private Object ds;

	private ParameterListener paramSourceListener;
	
	private List<RelatedContent>	last_selected_content = new ArrayList<RelatedContent>();
	
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
		
		boolean	show_info_bar = true;
		
		if ( mdi != null ){

			if ( ds instanceof RCMItemSubView ){
				
				manager.reserveTemporarySpace();
			
				space_reserved = true;
			
				show_info_bar = false;
				
			}else if ( ds instanceof RCMItemContent ){
				
				RCMItemContent	ic = (RCMItemContent)ds;
				
				mdi_entry = ic.getSideBarEntry();
								
				manager.reserveTemporarySpace();
				
				space_reserved = true;
				
			}else if ( ds instanceof RCMItemSubscriptions ){
				
				mdi_entry = ((RCMItemSubscriptions) ds).getSideBarEntry();
				manager.reserveTemporarySpace();
				
				space_reserved = true;
				
			}else{
				
				mdi_entry = mdi.getEntry( RelatedContentUISWT.SIDEBAR_SECTION_RELATED_CONTENT );
			}
			
			if ( mdi_entry != null ){
			
				mdi_entry.addToolbarEnabler(this);
			}
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
			
				// min rank
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cMinRank = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinRank.setLayout(layout);
			Label lblMinRank = new Label(cMinRank, SWT.NONE);
			lblMinRank.setText(MessageText.getString("rcmview.filter.minRank"));
			Spinner spinMinRank = new Spinner(cMinRank, SWT.BORDER);
			spinMinRank.setMinimum(0);
			spinMinRank.setSelection(minRank);
			spinMinRank.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					minRank = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});

				// min size
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cMinSize = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinSize.setLayout(layout);
			Label lblMinSize = new Label(cMinSize, SWT.NONE);
			lblMinSize.setText(MessageText.getString("rcmview.filter.minSize"));
			Spinner spinMinSize = new Spinner(cMinSize, SWT.BORDER);
			spinMinSize.setMinimum(0);
			spinMinSize.setMaximum(100*1024*1024);	// 100 TB should do...
			spinMinSize.setSelection(minSize);
			spinMinSize.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					minSize = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});
			
				// show indirect
			
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

		if ( true || show_info_bar ){
			new InfoBarUtil(skinObject, "rcmview.infobar", false,
					"rcm.infobar", "rcm.view.infobar") {
				public boolean allowShow() {
					return true;
				}
			};
		}
		
		return null;
	}
	
	private boolean 
	isOurContent(
		RelatedContent c) 
	{
		boolean show = 
			((c.getSeeds() >= minSeeds) || (showUnknownSeeds && c.getSeeds() < 0)) && 
			(createdMsAgo == 0 || (SystemTime.getCurrentTime() - c.getPublishDate() < createdMsAgo)) &&
			((c.getRank() >= minRank)) &&
			(c.getSize()==-1||(c.getSize() >= 1024L*1024*minSize)) &&
			(showIndirect || c.getHash() != null);
		
		if ( show ){
			
			show = RelatedContentUISWT.getSingleton().getPlugin().isVisible( c );
		}
		
		return( show );
	}


	protected void refilter() {
		if (tv_related_content != null) {
			tv_related_content.refilter();
		}
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
				ColumnRC_ChangedLocallyAgo.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_ChangedLocallyAgo(column);
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
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Tags.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Tags(column);
						}
					});
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Networks.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Networks(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Rating.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Rating(column);
						}
					});
	}

	public Object 
	dataSourceChanged(
		SWTSkinObject skinObject, Object params) 
	{
		//hideView();
		
		ds = params;
		
		//showView();
		
		return( super.dataSourceChanged(skinObject, params));
	}

	
	private void
	showView()
	{
		RelatedContentUISWT ui = RelatedContentUISWT.getSingleton();
		
		if ( ui != null && !ui.getPlugin().hasFTUXBeenShown()){
			
			ui.showFTUX(getSkinObject("rcm-list"));
			
		}else{
			
			SWTSkinObject so_list = getSkinObject("rcm-list");

			if ( so_list != null ){
				
				so_list.setVisible(true);
			}
		}


		SWTSkinObject so_list = getSkinObject("rcm-list");
		
		if ( so_list != null ){
			
			initTable((Composite) so_list.getControl());
		}
		
		paramSourceListener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				refilter();
			}
		};
		COConfigurationManager.addParameterListener(RCMPlugin.PARAM_SOURCES_LIST, paramSourceListener);
	}
	
	private void
	hideView()
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

		COConfigurationManager.removeParameterListener(RCMPlugin.PARAM_SOURCES_LIST, paramSourceListener);
	}
	
	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		showView();
		
		return null;
	}

	public Object 
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		hideView();
		
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
		TableColumnManager tcManager = TableColumnManager.getInstance();

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
		
		tcManager.setDefaultColumnNames(TABLE_RCM, new String[] {
					ColumnRC_New.COLUMN_ID,
					ColumnRC_Rank.COLUMN_ID,
					ColumnRC_Title.COLUMN_ID,
					ColumnRC_Actions.COLUMN_ID,
					ColumnRC_Size.COLUMN_ID,
					ColumnRC_Created.COLUMN_ID,
					ColumnRC_Seeds.COLUMN_ID,
					ColumnRC_Peers.COLUMN_ID,
					ColumnRC_Tags.COLUMN_ID,
					ColumnRC_Rating.COLUMN_ID,
		});
		
		if ( ds instanceof RCMItemContent ){
			
			if (((RCMItemContent)ds).isPopularity()){
		
					// force view to be sorted by peers, descending
						
				tcManager.setDefaultSortColumnName(TABLE_RCM, ColumnRC_Peers.COLUMN_ID);
				
				TableColumnCore tcc = tcManager.getTableColumnCore( TABLE_RCM, ColumnRC_Peers.COLUMN_ID );
				
				if ( tcc != null ){
					
					tcc.setSortAscending( false);
				}
			}
		}
		
		table_parent = new Composite(control, SWT.NONE);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv_related_content.addSelectionListener(new TableSelectionListener() {

			public void 
			selected(
				TableRowCore[] _rows) 
			{
				updateSelectedContent();
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
				updateSelectedContent();
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
			
			private void
			updateSelectedContent()
			{
				TableRowCore[] rows = tv_related_content.getSelectedRows();
				
				ArrayList<ISelectedContent>	valid = new ArrayList<ISelectedContent>();

				last_selected_content.clear();
				
				for (int i=0;i<rows.length;i++){
					
					final RelatedContent rc = (RelatedContent)rows[i].getDataSource();
					
					last_selected_content.add( rc );
					
					if ( rc.getHash() != null && rc.getHash().length > 0 ){
						
						SelectedContent sc = new SelectedContent(Base32.encode(rc.getHash()), rc.getTitle());
						
						sc.setDownloadInfo(new DownloadUrlInfo(	RCMPlugin.getMagnetURI( rc )));
						
						valid.add(sc);
					}
				}
				
				ISelectedContent[] sels = valid.toArray( new ISelectedContent[valid.size()] );
				
				SelectedContentManager.changeCurrentlySelectedContent("IconBarEnabler",
						sels, tv_related_content);
				
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}
		}, false);

		tv_related_content.addLifeCycleListener(
			new TableLifeCycleListener() 
			{
				private Set<RelatedContent>	content_set = new HashSet<RelatedContent>();
				
				private int liveness_marker;
				
				private boolean	initial_selection_handled = false;
				
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
									
									for ( RelatedContent rc : hits ){
										
										TableRowCore row = tv_related_content.getRow(rc);
										
										if ( row != null ){
											
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
									
										if ( tv_related_content != null ){
											
											tv_related_content.removeDataSources( hits.toArray( new RelatedContent[ hits.size()] ));
										}
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
				
					Object data_source = mdi_entry==null?ds:mdi_entry.getDatasource();
					
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
														
														if ( isOurContent(c2)){
															
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
														
														if ( !initial_selection_handled ){
															
																// get user's last selection back, if any
															
															initial_selection_handled = true;
														
															final List<TableRowCore>	selected_rows = new ArrayList<TableRowCore>();
															
															if ( last_selected_content.size() > 0 ){
																
																f_table.processDataSourceQueueSync();
															}
															
															for ( RelatedContent rc: last_selected_content ){
															
																TableRowCore row = f_table.getRow( rc );
																
																if ( row != null ){
																	
																	selected_rows.add( row );
																}
															}
															
															if ( selected_rows.size() > 0 ){
																	
																Utils.execSWTThreadLater(
																	1,
																	new Runnable()
																	{
																		public void
																		run()
																		{
																				// selection visible logic requires viewport to be initialised before it can scroll
																				// properly so we need to defer this action
																			
																			f_table.setSelectedRows( selected_rows.toArray( new TableRowCore[selected_rows.size()] ));
																		}
																	});
															}
														}
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

		RelatedContentUISWT ui = RelatedContentUISWT.getSingleton();
		
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
							
							RelatedContentUISWT ui = RelatedContentUISWT.getSingleton();
							
							if ( ui != null ){
								
								for ( RelatedContent c: assoc_ok ){
								
									ui.addSearch( c.getHash(), c.getNetworks(), c.getTitle());
									
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

					
					new MenuItem(menu, SWT.SEPARATOR );

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

					item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("rcm.menu.bis"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							String s = related_content[0].getTitle();
							s = s.replaceAll("[-_]", " ");
							String URL = "http://www.bing.com/images/search?q=" + UrlUtils.encode(s);
							launchURL(URL);
						};
					});

					new MenuItem(menu, SWT.SEPARATOR );
					
					item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("rcm.menu.uri"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							
							ClipboardCopy.copyToClipBoard( RCMPlugin.getMagnetURI(related_content[0]));
						};
					});
					
					if ( related_content.length==1 ){
						byte[] hash = related_content[0].getHash();
						item.setEnabled(hash!=null&&hash.length > 0 );
					}else{
						item.setEnabled(false);
					}
					
					item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("rcm.menu.uri.i2p"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							
							String[] magnet_uri = { RCMPlugin.getMagnetURI(related_content[0]) };
							
							UrlUtils.extractNetworks( magnet_uri );
							
							String i2p_only_uri = magnet_uri[0] + "&net=" + UrlUtils.encode( AENetworkClassifier.AT_I2P );
							
							ClipboardCopy.copyToClipBoard( i2p_only_uri );
						};
					});
					
					if ( related_content.length==1 ){
						byte[] hash = related_content[0].getHash();
						item.setEnabled(hash!=null&&hash.length > 0 );
					}else{
						item.setEnabled(false);
					}
					
					new MenuItem(menu, SWT.SEPARATOR );

					final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

					remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

					Utils.setMenuItemImage( remove_item, "delete" );

					remove_item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							userDelete(related_content);
						}

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
							
							userDelete( content );
							
							e.doit = false;
						}
					}
					
					public void 
					keyReleased(
						KeyEvent arg0 ) 
					{
					}
				});
		
		if (ds instanceof RCMItemSubView) {
  		tv_related_content.addCountChangeListener(new TableCountChangeListener() {
  			
  			public void rowRemoved(TableRowCore row) {
  				updateCount();
  			}
  			
  			public void rowAdded(TableRowCore row) {
  				updateCount();
  			}

				private void updateCount() {
					int size = tv_related_content == null ? 0 : tv_related_content.size(false);
					((RCMItemSubView) ds).setCount(size);
				}
  		});
  		((RCMItemSubView) ds).setCount(0);
		}
		
		tv_related_content.initialize( table_parent );

		control.layout(true);
	}
	
	private void userDelete(RelatedContent[] related_content) {
		TableRowCore focusedRow = tv_related_content.getFocusedRow();
		TableRowCore focusRow = null;
		if (focusedRow != null) {
			int i = tv_related_content.indexOf(focusedRow);
			int size = tv_related_content.size(false);
			if (i < size - 1) {
				focusRow = tv_related_content.getRow(i + 1);
			} else if (i > 0) {
				focusRow = tv_related_content.getRow(i - 1);
			}
		}
		manager.delete(related_content);
		
		if (focusRow != null) {
  		tv_related_content.setSelectedRows(new TableRowCore[] {
  			focusRow
  		});
		}
	};

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

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	public boolean 
	toolBarItemActivated(
		ToolBarItem item, 
		long activationType,
		Object datasource ) 
	{
		if ( tv_related_content == null || !tv_related_content.isVisible()){
			
			return( false );
		}
		if (item.getID().equals("remove")) {
			
			Object[] _related_content = tv_related_content.getSelectedDataSources().toArray();
			
			if ( _related_content.length > 0 ){
				
				RelatedContent[] related_content = new RelatedContent[_related_content.length];
				
				System.arraycopy( _related_content, 0, related_content, 0, related_content.length );
				
				userDelete(related_content);
			
				return true;
			}
		}
		
		return false;
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		if (tv_related_content == null || !tv_related_content.isVisible()) {
			return;
		}
		
			// make sure we're operating on a selection we understand...
		
		ISelectedContent[] content = SelectedContentManager.getCurrentlySelectedContent();
		
		for ( ISelectedContent c: content ){
			
			if ( c.getDownloadManager() != null ){
				
				return;
			}
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
