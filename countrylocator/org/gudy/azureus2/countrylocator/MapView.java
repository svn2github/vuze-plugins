package org.gudy.azureus2.countrylocator;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.countrylocator.statisticsProviders.DownSpeedProvider;
import org.gudy.azureus2.countrylocator.statisticsProviders.DownloadedDataProvider;
import org.gudy.azureus2.countrylocator.statisticsProviders.IStatisticsProvider;
import org.gudy.azureus2.countrylocator.statisticsProviders.NumberOfPeersProvider;
import org.gudy.azureus2.countrylocator.statisticsProviders.UploadSpeedProvider;
import org.gudy.azureus2.countrylocator.statisticsProviders.UploadedDataProvider;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;


/*
 * TODO Overview
 * (done) 1.4 compatibility
 * (done) Integration into cl v1.6 codebase
 * 
 * Fixes:
 * 	1. (done) Prevent flickering of table (-> use double buffering)
 * 	2. (done) Correct layout
 * 	3. (ok atm) Use better units like MB, kb/s etc. (-> display unit in table tooltip)
 * 		-> now displayd in header (tooltip support could be emulated or just wait for swt3.2 ;-)
 *  4. (done) Correct scrollbar behaviour of the table (-> preserver current location)
 *  5. (done) Automatically set column as wide as max(column header, values)
 * 
 * Additions:
 * 	1. (done) Selecting a tablerow highlights country
 * 	2. (done) Tooltip (hover over country circle -> show tooltip with info, mouseMoveListener interface)
 * 		(done) - black circles sometimes overlap tooltip!
 * 	3. (done) Zoomable view
 *  4. Table sorting by clicking on column title
 *  5. (done) Extend config page with our new settings
 *  6. (done) Refactoring: Decouple the mapViewListener from the real view 
 *  	(create MapView class, which contains everything to display the map view)
 *  7. (Done)(after 9) Add suport to show map on multiple torrents
 *  8. Zoom when the user clicks on the map and center to this location
 */
/**
 * The actual world map view which contains the world map widget and the stats
 * table. Each instance of this class is responsible for a torrent. What we need
 * to create the statistics is the Download object from the torrent which can be
 * obtained by calling <code>view.getDataSource()</code> The map shows the
 * currently selected statistics aggregated on a country level as points whose
 * color depend on the stats value. The stats table displays for each country
 * (from which at least 1 peer is connected) the aggregated statistics values.
 * 
 * @author gooogelybear
 * @author free_lancer
 */
public class MapView {

	/** our reference to the CountryLocator instance */
	private CountryLocator countryLocator;
	
	/*
	 * config keys (used in the config page): Denote the names of the
	 * properties in the config to store the settings.
	 */
	public static String CFG_COUNTRY_MIN_COLOR = "CountryMinColor";
	public static String CFG_COUNTRY_MAX_COLOR = "CountryMaxColor";
	public static String CFG_COUNTRY_HIGHLIGHT_COLOR = "CountryHighlightColor";
	
	/**
	 * The registered statistics Providers
	 */
	protected Vector statsProviders;
	
	/**
	 * Contains a map for each 'stats type':
	 * <em>key</em> = country code, <em>value</em> = stats value
	 */
	private Vector collectedStats;
	
	private ClassLoader classLoader = WorldMapViewListener.class.getClassLoader(); 
	
	/* SWT stuff */
	private Table statsTable;
	private static Image mapImage;
	private Combo combo;
	private ZoomedScrolledMap map;
	private final UISWTView view;
	
	
	/**
	 * Creates an instance of the map view.
	 * @param countryLocator
	 * @param view The view is used to fetch the corresponding <code>Download</code> object
	 */
	public MapView(CountryLocator countryLocator, UISWTView view) {
		this.countryLocator = countryLocator;
		this.view = view;
		registerStatsProviders();
	}
	

	private void registerStatsProviders() {
		statsProviders = new Vector(3);
		// Could use reflection to dynamically load the stats providers
		statsProviders.add(UIConstants.NUMBER_OF_PEERS_POSITION,new NumberOfPeersProvider());
		statsProviders.add(UIConstants.DOWNLOADED_DATA_POSITION,new DownloadedDataProvider());
		statsProviders.add(UIConstants.UPLOADED_DATA_POSITION, new UploadedDataProvider());
		statsProviders.add(UIConstants.DOWN_SPEED_POSITION, new DownSpeedProvider());
		statsProviders.add(UIConstants.UP_SPEED_POSITION, new UploadSpeedProvider());
	}

	/** initialises the UI */
	void initialize(Composite parent) {
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		parent.setLayout(gridLayout);
		
		final Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Select property to display on map:");
		
		combo = new Combo(parent, SWT.READ_ONLY);

		String[] statsChoices = new String[statsProviders.size()];
		for (int i = 0; i < statsChoices.length; i++) {
			statsChoices[i] = ((IStatisticsProvider)statsProviders.get(i)).getName();
		}
		combo.setItems(statsChoices);
		combo.select(0);
		combo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		combo.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent e) {
				/* display new stats on map */
				map.redraw();
			}
		});
			
		/* Setup map if not done yet */
		if (mapImage == null) {
			InputStream is = classLoader.getResourceAsStream(UIConstants.MAP_FILE_2048);
			mapImage = new Image(Display.getCurrent(), is);
		}	
			map = new ZoomedScrolledMap(parent, SWT.NONE, this);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
			map.setLayoutData(gridData);
			map.setImage(mapImage);

		/* setup stats table */
		final Label lbl1 = new Label(parent, SWT.NONE);
		lbl1.setText("Collected Statistics:");
		lbl1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		
		/* table is put in its own composite (see
		 * http://dev.eclipse.org/newslists/news.eclipse.platform.swt/msg07894.html
		 */
		Composite tableParent = new Composite(parent, SWT.NONE);
		tableParent.setLayout(new FillLayout());
		tableParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		statsTable = new Table(tableParent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
		statsTable.setLinesVisible(true);
		statsTable.setHeaderVisible(true);
		statsTable.addSelectionListener(new SelectionAdapter() {		
			public void widgetSelected(SelectionEvent e) {
				map.redraw();
			}
		});
		
		/* add the default columns for country & flag */
		addColumnToTable(statsTable, "Country", 100, SWT.NONE);
		addColumnToTable(statsTable, "Flag", 30, SWT.NONE);
		
		/* create a column for each statistics provider */
		for (Iterator it = statsProviders.iterator(); it.hasNext();) {
			IStatisticsProvider provider = (IStatisticsProvider)it.next();
			TableColumn tc;
			if ("".equals(provider.getUnit()))
				tc = addColumnToTable(statsTable, provider.getName(), 70, SWT.NONE);
			else
				tc = addColumnToTable(statsTable, provider.getName(), provider.getUnit(), 70, SWT.NONE);
			tc.pack();
		}
	}
	
	/** 
	 * Convenience method to add new columns to a table 
	 * @return the column which was added
	 */
	private TableColumn addColumnToTable(Table table, String columnName, int width, int style) {
		TableColumn column = new TableColumn(table, style);
		column.setWidth(width);
		column.setText(columnName);
		return column;
	}
	
	/** 
	 * Same as {@link #addColumnToTable(Table, String, int, int)} but additionally displays
	 * a tooltip 
	 * @return the column which was added to the table
	 */
	private TableColumn addColumnToTable(Table table, String columnName, String tooltip, int width, int style) {
		String toolTipText = columnName + " (" + tooltip + ")";
		String realColumnName = toolTipText;
		TableColumn column = addColumnToTable(table, realColumnName, width, style); // temporary to display unit
		/* TODO setTooltip: setToolTipText only exists in 3.2 (could be simulated in 3.1)!
		 * Atm. we add the tooltip to the column header text.
		 */
		
//		column.setToolTipText(toolTipText); // use this when 3.2 is available
		return column;
	}

	/** 
	 * Refreshes both table and map - this is triggered by the Azureus GUI refresh
	 * interval set by the user
	 */
	void refresh() {
		// [1] Get fresh data from the statistics providers
		Download download = (Download) view.getDataSource();
		
		// AMC: Seen this in debug logs, so protect against this.
		if (download == null) {return;}
		
		PeerManager peerManager = download.getPeerManager();
		if (peerManager == null || download.getState() == Download.ST_STOPPED)
			return;
		Peer[] peers = peerManager.getPeers();
		if ((peers == null) || (peers.length <= 0))
			return; // no peers connected yet
		
		collectedStats = new Vector();
		/* Generate values for each stats type */
		for (Iterator iter = statsProviders.iterator(); iter.hasNext();) {
			IStatisticsProvider provider = (IStatisticsProvider) iter.next();
			Map stats = provider.getStatistics(peers);			
			// TODO Use data field: combo.setData(index, stats) -> simplifies getSelStats!
			collectedStats.add(stats);
		}
		
		// [2] Update the table & trigger redrawal of map
		refreshTable();
		map.redraw();
	}
	
	/**
	 * Refreshes the table with the statistics data from <code>{@link #collectedStats}}</code>
	 * Each element in {@link #collectedStats} is mapped to a column in the table.
	 */
	private void refreshTable() {
		if (collectedStats.size() < 1) return;
		/* Delay drawing of the table until its entire content is updated, prevents
		 * flickering
		 */
		statsTable.getParent().setRedraw(false);
		/* get previously selected country */
		String selectedCountry = getSelectedCountryName();
		Set countryCodes = ((Map)collectedStats.get(0)).keySet();
		
		/* remove unknown country entry */
		countryCodes.remove(UIConstants.UNKNOWN_COUNTRY);
		
		/* Insert the existing rows into a hashtable indexed by the country name for fast
		 * access. Existing rows are reused if still peers for this country connected */
		Hashtable rows = new Hashtable();
		TableItem[] tis = statsTable.getItems();
		for(int i = 0; i < tis.length; i++) {
			rows.put(tis[i].getText(0), tis[i]);
		}
		
		/* Each country corresponds to a row in the table */
		TableItem tableRow;
		for (Iterator it = countryCodes.iterator(); it.hasNext();) {
			String cc = (String)it.next();
			/* Get the localized country name */
			// TODO could we reuse code from CountryLocator to convert cc -> name?
			Locale locale = new Locale("", cc);
			String countryName = locale.getDisplayCountry(Locale.getDefault());

			/* check if there is an existing table item for this country */
			if (rows.get(countryName) != null) {
				/* a row for this country already exists, so we can update it */
				tableRow = (TableItem)rows.get(countryName);
				/* we remove the row from the map so we can later check if there are any 
				 * remaining no longer used rows in the hashmap
				 */
				rows.remove(countryName);
			} else {
				/* there is no existing row, so we create a new one and initialize it */
				tableRow = new TableItem(statsTable, SWT.BORDER);
				tableRow.setText(0, countryName);
				/* display flag (if available for this country) */
				Graphic flagGraphic = countryLocator.getImage(CountryLocator.FLAGS_SMALL, cc.toLowerCase());
				if (flagGraphic != null)
					tableRow.setImage(1, GraphicUtils.getImage(flagGraphic));
				
			}
			/* Fetch all stats values for each country */
			
			/* the stats begin at index 2 (0 = Country name, 1 = flag */
			int columnIndex = 2;
			
			/* Iterate over all collected stats and get the corresponding values for the
			 * current contry code.
			 */
			for (Iterator it2 = collectedStats.iterator(); it2.hasNext();) {
				Map providedData = (Map)it2.next();
				long statsValue = ((Long)providedData.get(cc)).longValue();
				tableRow.setText(columnIndex, String.valueOf(statsValue));
				columnIndex++;
			}
			
			/* select item again if it was selected previously */
			if (countryName == selectedCountry) {
				// TODO Use setSelection(tableItem) when 3.2 is available ;/
//				statsTable.setSelection(tableRow);
				statsTable.select(statsTable.indexOf(tableRow));
			}
		}
		/* check if there are any rows left over in the map which are no longer needed 
		 * and remove them from the table (no connections for this country) */
		if (!rows.isEmpty()) {
			for (Iterator it = rows.values().iterator(); it.hasNext();) {
				TableItem ti = (TableItem)it.next();
				statsTable.remove(statsTable.indexOf(ti));
			}
		}
		
		/* table update complete -> draw it */
		statsTable.getParent().setRedraw(true);
	}
	
	/**
	 * @return The localised name of the country currently selected in the table
	 *         or <code>null</code> if none is selected.
	 */
	String getSelectedCountryName() {
		TableItem[] ti = statsTable.getSelection();
		String selectedCountry = null;
		if (ti.length == 1) {
			selectedCountry = ti[0].getText(0);
		}
		return selectedCountry;
	}
	
	/**
	 * Access to the stats which are currently selected in the combo box.
	 * 
	 * @return currently selected stats as map (key = cc, value = stats value)
	 */
	protected Map getSelectedStats() {
		if (collectedStats == null) return null;
		
		int selection = combo.getSelectionIndex();
		switch (selection) {
			case UIConstants.NUMBER_OF_PEERS_POSITION:
				return (Map)collectedStats.get(UIConstants.NUMBER_OF_PEERS_POSITION);
			case UIConstants.DOWNLOADED_DATA_POSITION:
				return (Map)collectedStats.get(UIConstants.DOWNLOADED_DATA_POSITION);
			case UIConstants.UPLOADED_DATA_POSITION:
				return (Map)collectedStats.get(UIConstants.UPLOADED_DATA_POSITION);
			case UIConstants.DOWN_SPEED_POSITION:
				return (Map)collectedStats.get(UIConstants.DOWN_SPEED_POSITION);
			case UIConstants.UP_SPEED_POSITION:
				return (Map)collectedStats.get(UIConstants.UP_SPEED_POSITION);
			default:
				/* invalid selection: Return default */
				System.err.println("invalid stats selection");
				return (Map)collectedStats.get(UIConstants.NUMBER_OF_PEERS_POSITION);
		}
//		Could use the data field of the combo to store references to the stats...
//		Map<String, Long> stats = (Map<String, Long>) combo.getData(String.valueOf(combo.getSelectionIndex()));
//		return stats;
	}

}
