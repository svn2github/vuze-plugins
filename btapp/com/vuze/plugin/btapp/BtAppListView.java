package com.vuze.plugin.btapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.util.JSONUtils;

public class BtAppListView
	implements UISWTViewEventListener
{

	protected static final String TABLE_ID = "BtAppList";

	private UISWTView swtView;

	private PluginInterface pi;

	private Composite parent;

	private Browser browser;

	protected TableViewSWT<BtAppDataSource> tv;

	private static boolean alreadyCreatedStuff = false;
	
	public BtAppListView() {
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		int type = event.getType();
		switch (type) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				pi = swtView.getPluginInterface();
				
				createStuff();
				// not sure if adding a 'torrent' event to an app with PRIV_READALL
				// should send events for all new torrents.
				//pi.getDownloadManager().addListener(new DownloadManagerListener() {
				//	public void downloadAdded(Download download) {
				//	}
				//
				//	public void downloadRemoved(Download download) {
				//	}
				//});
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				parent = (Composite) event.getData();
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				initView(parent);
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				unloadView();
				break;

		}
		return true;
	}

	private void createStuff() {
		if (alreadyCreatedStuff) {
			return;
		}
		
		alreadyCreatedStuff = true;
		TableManager tableManager = pi.getUIManager().getTableManager();

		TableColumnCreationListener colCreateListener = new TableColumnCreationListener() {
			public void tableColumnCreated(TableColumn column) {
				column.setWidth(BtAppDataSource.getDisplayWidth(column.getName()));
				column.addCellAddedListener(new TableCellAddedListener() {
					public void cellAdded(TableCell cell) {
						Object ds = cell.getDataSource();
						if (ds instanceof BtAppDataSource) {
							BtAppDataSource appInfo = (BtAppDataSource) ds;
							cell.setText(appInfo.getProperty(cell.getTableColumn().getName()));
						}
					}
				});
			}
		};

		String[] keys = BtAppDataSource.getKeys();
		for (String key : keys) {
			tableManager.registerColumn(BtAppDataSource.class, key,
					colCreateListener);
		}

		TableColumnCreationListener colActionCreateListener = new TableColumnCreationListener() {
			public void tableColumnCreated(TableColumn column) {
				ColumnAppListAction columnAppListAction = new ColumnAppListAction();
				column.addListeners(columnAppListAction);
				((TableColumnCore) column).addCellOtherListener("SWTPaint", columnAppListAction);
				column.setWidth(250);
			}
		};
		tableManager.registerColumn(BtAppDataSource.class, "action", colActionCreateListener);

		List<String> displayKeys = new ArrayList<String>(Arrays.asList(BtAppDataSource.getDisplaykeys()));
		displayKeys.add("action");

		TableColumnManager tcm = TableColumnManager.getInstance();
		tcm.setDefaultColumnNames(TABLE_ID, displayKeys.toArray(new String[0]));
	}

	private void unloadView() {
		Utils.disposeComposite(parent, false);
	}

	private void initView(Composite parent) {
		//parent.setLayout(new FormLayout());
		browser = new Browser(parent, SWT.BORDER);
		browser.setVisible(false);
		GridData gridData = new GridData();
		gridData.exclude = true;
		browser.setLayoutData(gridData);

		try {
			ResourceDownloader rd = pi.getUtilities().getResourceDownloaderFactory().create(
					new URL("http://apps.bittorrent.com/store/apps.json"));
			rd.addListener(new ResourceDownloaderListener() {

				public void reportPercentComplete(ResourceDownloader downloader,
						int percentage) {
				}

				public void reportAmountComplete(ResourceDownloader downloader,
						long amount) {
				}

				public void reportActivity(ResourceDownloader downloader,
						String activity) {
				}

				public void failed(ResourceDownloader downloader,
						ResourceDownloaderException e) {
					fail();
				}

				public boolean completed(ResourceDownloader downloader, InputStream data) {
					try {
						String s = FileUtil.readInputStreamAsString(data, -1);
						// Lists get put in a Map with key "value"
						Map map = JSONUtils.decodeJSON(s);
						if (map == null || !(map.get("value") instanceof List)) {
							fail();
						} else {
							loadAppList((List) map.get("value"));
						}
					} catch (IOException e) {
					}
					return true;
				}
			});
			rd.asyncDownload();
		} catch (Exception e) {
		}
	}

	protected void loadAppList(final List list) {
		Utils.execSWTThread(new Runnable() {
			public void run() {

				tv = new TableViewSWTImpl<BtAppDataSource>(BtAppDataSource.class,
						TABLE_ID, TABLE_ID, "name");
				tv.setRowDefaultHeight(25);
				tv.setHeaderVisible(true);

				tv.initialize(parent);

				for (Object appObject : list) {
					if (appObject instanceof Map) {
						BtAppDataSource appInfo = new BtAppDataSource((Map) appObject,
								browser);
						tv.addDataSource(appInfo);
					}
				}
				parent.layout();
			}
		});
	}

	protected void fail() {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				browser.setLayoutData(Utils.getFilledFormData());

				browser.setUrl("http://www.utorrent.com/get-started/apps");
				browser.setLayoutData(new GridData(GridData.FILL_BOTH));

				parent.layout();
			}
		});
	}

}
