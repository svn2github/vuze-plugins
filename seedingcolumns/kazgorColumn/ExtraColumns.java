/**
 * KazgorColumn.java Created on May 15, 2004
 * 
 * Free. Use however you see fit.
 */

package kazgorColumn;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.utils.Formatters;
import org.gudy.azureus2.plugins.utils.LocaleListener;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

/**
 * Simple Column Example
 * 
 * @author TuxPaper
 */
public class ExtraColumns implements Plugin {

	// Plugin parameter keys
	static final String RATIO_TYPE_PARAMETER_KEY = "ratio_type";
	static final String MANUAL_RATIO_PARAMETER_KEY = "manual_ratio";
	static final String MANUAL_RATIO_LIST_STRING_PARAMETER_KEY = "manual_ratio_list_string";
	static final String MANUAL_RATIO_LIST_STRING_DELIMITER = ";";
	static final String COLOR0_25_BLUE_PARAMETER_KEY = "color0_25_BLUE";
	static final String COLOR0_25_GREEN_PARAMETER_KEY = "color0_25_GREEN";
	static final String COLOR0_25_RED_PARAMETER_KEY = "color0_25_RED";
	static final String COLOR25_50_BLUE_PARAMETER_KEY = "color25_50_BLUE";
	static final String COLOR25_50_GREEN_PARAMETER_KEY = "color25_50_GREEN";
	static final String COLOR25_50_RED_PARAMETER_KEY = "color25_50_RED";
	static final String COLOR50_75_BLUE_PARAMETER_KEY = "color50_75_BLUE";
	static final String COLOR50_75_GREEN_PARAMETER_KEY = "color50_75_GREEN";
	static final String COLOR50_75_RED_PARAMETER_KEY = "color50_75_RED";
	static final String COLOR75_100_BLUE_PARAMETER_KEY = "color75_100_BLUE";
	static final String COLOR75_100_GREEN_PARAMETER_KEY = "color75_100_GREEN";
	static final String COLOR75_100_RED_PARAMETER_KEY = "color75_100_RED";

	// Column names
	private static final String COLUMNID_TORRENTNAME = "KazgorColumn1";
	private static final String COLUMNID_UPETA = "KazgorColumn2";
	private static final String COLUMNID_REMAINING = "KazgorColumn3";
	private static final String COLUMNID_REMAINING_PERCENT = "KazgorColumn4";
	private static final String COLUMNID_SDONE = "KazgorColumn5";
	private static final String COLUMNID_TARGETRATIO = "KazgorColumn6";
	private static final String COLUMNID_AVG_DOWNLOAD_SPEED = "KazgorColumn7";
	private static final String COLUMNID_AVG_UPLOAD_SPEED = "KazgorColumn8";

	
	// Localized String IDs & strings & stuff
	private NumberFormat numberFormatter = null;
	private NumberFormat percentFormatter = null;
	static final String PLUGIN_ID = "seedingcolumns1";
	private static final String COLUMN = PLUGIN_ID + ".column";
	private static final String UPLOADETA_COLUMN = COLUMN + ".uploadeta";
	private static final String UPLOADETA_COLUMN_EXCEED = UPLOADETA_COLUMN + ".exceed";
	private String uploadETAcolumnExceed;
	private static final String UPLOAD_TARGET = COLUMN + ".uploadtarget";
	private String uploadTarget;
	private static final String UPLOAD_REMAINING = COLUMN + ".uploadremaining";
	private String uploadRemaining;
	private static final String UPLOAD_RATIO_OF = COLUMN + ".uploadratioof";
	private String uploadRatioOf;
	private static final String FILES_SELECTED_TOTAL_SIZE = COLUMN + ".filesselectedtotalsize";
	private String filesSelectedTotalSize;

	// Copied from Constants - the plugin API should provide access to these
	// constants somewhere.
	private static final String INFINITY_STRING = "\u221E";

	
	// Fields and constants for ratio related stuff
	static final int RATIO_TYPE_DEFAULT = 2;
	private int ratioType = RATIO_TYPE_DEFAULT;

	static final float TARGET_RATIO_DEFAULT = 1.0f;
	private float targetRatio = TARGET_RATIO_DEFAULT;
	static final String TARGET_RATIO_DEFAULT_STRING = "1.0";
	static final float ERROR_TARGET_RATIO = 42.0f;

	static final float[] TARGET_RATIO_LIST_DEFAULT = { TARGET_RATIO_DEFAULT };
	private float[] targetRatioList = TARGET_RATIO_LIST_DEFAULT;
	static final String[] TARGET_RATIO_LIST_DEFAULT_STRING = { TARGET_RATIO_DEFAULT_STRING };

	
	// Fields and constants for color related stuff
	static final String[] COLOR_PARAMETER_KEYS = { COLOR0_25_RED_PARAMETER_KEY, COLOR0_25_GREEN_PARAMETER_KEY, COLOR0_25_BLUE_PARAMETER_KEY,
													COLOR25_50_RED_PARAMETER_KEY, COLOR25_50_GREEN_PARAMETER_KEY, COLOR25_50_BLUE_PARAMETER_KEY, 
													COLOR50_75_RED_PARAMETER_KEY, COLOR50_75_GREEN_PARAMETER_KEY, COLOR50_75_BLUE_PARAMETER_KEY, 
													COLOR75_100_RED_PARAMETER_KEY, COLOR75_100_GREEN_PARAMETER_KEY, COLOR75_100_BLUE_PARAMETER_KEY };
	private int[] color0_25 = new int[3];
	private int[] color25_50 = new int[3];
	private int[] color50_75 = new int[3];
	private int[] color75_100 = new int[3];
	static final int[] COLOR_0_25_DEFAULT = { 64, 0, 128 }, COLOR_25_50_DEFAULT = { 0, 128, 255 };
	static final int[] COLOR_50_75_DEFAULT = { 255, 128, 64 }, COLOR_75_100_DEFAULT = { 200, 0, 0 };
	static final int[] COLOR_DEFAULTS = { COLOR_0_25_DEFAULT[0], COLOR_0_25_DEFAULT[1], COLOR_0_25_DEFAULT[2],
								        COLOR_25_50_DEFAULT[0], COLOR_25_50_DEFAULT[1], COLOR_25_50_DEFAULT[2],
								        COLOR_50_75_DEFAULT[0], COLOR_50_75_DEFAULT[1], COLOR_50_75_DEFAULT[2],
								        COLOR_75_100_DEFAULT[0], COLOR_75_100_DEFAULT[1], COLOR_75_100_DEFAULT[2] };

	private PluginInterface plugin_Interface;
	private PluginConfig pluginConfig;
	private Formatters formatter;
	private LocaleUtilities localizer;

	
	/**
	 * This method is called when the plugin is loaded / initialized
	 * @param pluginInterface access to Azureus' plugin interface
	 */
	public void initialize(final PluginInterface pluginInterface) {

		// set the global plugin_Interface to the local pluginInterface so later
		// classes can use it
		plugin_Interface = pluginInterface;
		pluginConfig = plugin_Interface.getPluginconfig();
		formatter = pluginInterface.getUtilities().getFormatters();

		
		// localize and add a listener for locale-changes
		localizer = pluginInterface.getUtilities().getLocaleUtilities();
		localize(localizer.getCurrentLocale());
		localizer.addListener(new LocaleListener() {
			public void localeChanged(Locale l) {
				localize(l);
			}
		});

		
		// get the ratio-related plugin config values and add a listener for ratio-related plugin config changes
		fetchRatioValues();
		ConfigParameterListener configParameterListener = new ConfigParameterListener() {
			public void configParameterChanged(ConfigParameter p) {
				fetchRatioValues();
			}
		};
		pluginConfig.getPluginParameter(RATIO_TYPE_PARAMETER_KEY).addConfigParameterListener(configParameterListener);
		pluginConfig.getPluginParameter(MANUAL_RATIO_PARAMETER_KEY).addConfigParameterListener(configParameterListener);
		pluginConfig.getPluginParameter(MANUAL_RATIO_LIST_STRING_PARAMETER_KEY).addConfigParameterListener(configParameterListener);

		
		// get the color-related plugin config values and add a listener for color-related plugin config changes
		fetchColorValues();
		configParameterListener = new ConfigParameterListener() {
			public void configParameterChanged(ConfigParameter p) {
				fetchColorValues();
			}
		};
		for (int i = 0; i < COLOR_PARAMETER_KEYS.length; i++) {
			pluginConfig.getPluginParameter(COLOR_PARAMETER_KEYS[i]).addConfigParameterListener(configParameterListener);
		}

		
		// add the columns + listeners
		TableManager tableManager = pluginInterface.getUIManager().getTableManager();
		TableColumn kazgorColumn;

		// Torrent Filename - Add to the Complete(Seeding) column 
		TorrentNameRefreshListener torrentNameRefreshListener = new TorrentNameRefreshListener();
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, COLUMNID_TORRENTNAME);
		kazgorColumn.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 150);
		kazgorColumn.addCellRefreshListener(torrentNameRefreshListener);
		tableManager.addColumn(kazgorColumn);

		// Torrent Filename - Add to the Incomplete(downloading) column 
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, COLUMNID_TORRENTNAME);
		kazgorColumn.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 150);
		kazgorColumn.addCellRefreshListener(torrentNameRefreshListener);
		tableManager.addColumn(kazgorColumn);

		// ETA for Seeding column to get you to a given ratio 
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, COLUMNID_UPETA);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(new UploadEtaListener());
		tableManager.addColumn(kazgorColumn);

		// Remaining for Seeding column to get you to a given ratio 
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, COLUMNID_REMAINING);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(new UploadRemainingListener());
		tableManager.addColumn(kazgorColumn);

		// Remaining % for Seeding column to get you to a given ratio 
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, COLUMNID_REMAINING_PERCENT);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 80, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(new UploadRemainingPercentListener());
		tableManager.addColumn(kazgorColumn);

		// selected files % done column 
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, COLUMNID_SDONE);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 80, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(new SelectedDoneListener());
		tableManager.addColumn(kazgorColumn);

		// Target Ratio that should be reached - Add to complete (seeding) columns
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, COLUMNID_TARGETRATIO);
		kazgorColumn.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_LAST, 80, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(new UploadTargetRatioListener());
		tableManager.addColumn(kazgorColumn);

		// Average download speed - Add to the Complete(seeding) columns 
		AverageDownloadSpeedListener averageDownloadSpeedListener = new AverageDownloadSpeedListener();
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, COLUMNID_AVG_DOWNLOAD_SPEED);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150); // seeding torrents don't download anymore, so no INTERVAL_LIVE needed here
		kazgorColumn.addCellRefreshListener(averageDownloadSpeedListener);
		tableManager.addColumn(kazgorColumn);

		// Average download speed - Add to the Incomplete(downloading) columns 
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, COLUMNID_AVG_DOWNLOAD_SPEED);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(averageDownloadSpeedListener);
		tableManager.addColumn(kazgorColumn);

		// Average upload speed - Add to the Complete(seeding) columns 
		AverageUploadSpeedListener averageUploadSpeedListener = new AverageUploadSpeedListener();
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, COLUMNID_AVG_UPLOAD_SPEED);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(averageUploadSpeedListener);
		tableManager.addColumn(kazgorColumn);

		// Average upload speed - Add to the Incomplete(downloading) columns 
		kazgorColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, COLUMNID_AVG_UPLOAD_SPEED);
		kazgorColumn.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150, TableColumn.INTERVAL_LIVE);
		kazgorColumn.addCellRefreshListener(averageUploadSpeedListener);
		tableManager.addColumn(kazgorColumn);

		
		// The magic to open a plugin GUI view -- see, wasn't that easy!
		// plugin_Interface.getUIManager().getSWTManager().addView((new
		// View(pluginInterface)),false);

		// update opening the GUI to the new interface
		pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					UISWTInstance swt = (UISWTInstance) instance;
					View view = new View(plugin_Interface);
					swt.addView(UISWTInstance.VIEW_MAIN, PLUGIN_ID, view);
				}
			}

			public void UIDetached(UIInstance arg0) {

			}
		});
	}

	
	/* Start of the Listener processes */

	static public class TorrentNameRefreshListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download dm = (Download) cell.getDataSource();
			if (dm == null) return;

			String TorrentName = dm.getTorrentFileName();

			// Now lets just get the filename minus the path
			File fl = new File(TorrentName);
			TorrentName = fl.getName();

			String str = TorrentName;
			int p = str.lastIndexOf(".torrent");

			// if the filename does not end in .torrent it must
			// be autoloaded from azureus itself (like my AZCVSUpdater does)
			// .. therefore it will end in .tmp instead
			if (p == -1) {
				p = str.lastIndexOf(".tmp");
				// if all else fails, just have the full name so you do not get errors
				if (p == -1) {
					p = str.length();
				}
			}

			TorrentName = str.substring(0, p);

			if (!cell.setSortValue(TorrentName) && cell.isValid()) return;

			cell.setText(TorrentName);
		}
	}

	
	public class UploadEtaListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download dm = (Download)cell.getDataSource();
			if (dm == null) return;

			// For all none seeding torrent don't display anything in the column
			int state = dm.getState();
			if (state != Download.ST_SEEDING) {
				if (!cell.setText("") && cell.isValid()) {
					// no cell text to update
				} else {
					cell.setSortValue(-3);
				}
				return;
			}

			// long filesize = dm.getTorrent().getSize();
			long downloaded = getRealDownloaded(dm);
			long uploaded = dm.getStats().getUploaded();
			float ratio = getNextTargetRatio((float) uploaded / downloaded);
			float filesized_ratio = downloaded * ratio;

			if (uploaded >= filesized_ratio) {
				if (!cell.setText(uploadETAcolumnExceed + " " + numberFormatter.format(ratio) + ":1") && cell.isValid()) {
					// no cell text to update
				} else {
					cell.setSortValue(-1);
				}
				return;
			}

			long upspeed = dm.getStats().getUploadAverage();

			if ((upspeed <= 0)) {
				if (!cell.setText(INFINITY_STRING) && cell.isValid()) {
					// no cell text to update
				} else {
					cell.setSortValue(-2);
				}
				return;
			}

			long eta_long = (long) (filesized_ratio - uploaded) / upspeed;

			if (!cell.setSortValue(eta_long) && cell.isValid())	return;

			float remaining = 0;
			if (filesized_ratio >= uploaded) remaining = filesized_ratio - uploaded;

			float perc = ((remaining * 100) / filesized_ratio);
			try {
				if (perc >= 75) cell.setForeground(color75_100);
				else if ((perc >= 50) && (perc < 75)) cell.setForeground(color50_75);
				else if ((perc >= 25) && (perc < 50)) cell.setForeground(color25_50);
				else if (perc != 0.0 && cell.isValid()) cell.setForeground(color0_25);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// --------------
			cell.setText(formatter.formatETAFromSeconds(eta_long));
		}
	}

	
	public class UploadRemainingListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download dm = (Download) cell.getDataSource();
			if (dm == null) return;

			float uploaded = dm.getStats().getUploaded();
			float real_download = getRealDownloaded(dm);

			float ratio = getNextTargetRatio((float) uploaded / real_download);
			float filesized_ratio = real_download * ratio;

			float remaining = 0;
			if (filesized_ratio > uploaded) remaining = filesized_ratio - uploaded;

			float perc = ((remaining * 100) / filesized_ratio);

			try {
				if (perc >= 75) cell.setForeground(color75_100);
				else if ((perc >= 50) && (perc < 75)) cell.setForeground(color50_75);
				else if ((perc >= 25) && (perc < 50)) cell.setForeground(color25_50);
				else if (perc != 0.0 && cell.isValid())	cell.setForeground(color0_25);

				if (!cell.setSortValue((long)remaining) && cell.isValid()) return;

				cell.setText(formatter.formatByteCountToKiBEtc((long) remaining));

				String hovertext = uploadTarget + " (" + formatter.formatByteCountToKiBEtc((long) filesized_ratio)
				        + ")\n";
				hovertext = hovertext + uploadRatioOf + " (" + numberFormatter.format(ratio) + ":1)";

				cell.setToolTip(hovertext);

			} catch (Exception e) {
				// If a null point exception is still thrown (had it happen once!?!), then ignore it
				e.printStackTrace();
			}
		}
	}

	
	public class UploadRemainingPercentListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download dm = (Download) cell.getDataSource();
			if (dm == null) return;

			float uploaded = dm.getStats().getUploaded();
			float real_download = getRealDownloaded(dm);

			float ratio = getNextTargetRatio((float) uploaded / real_download);
			float filesized_ratio = real_download * ratio;

			float remaining = 0;
			if (filesized_ratio >= uploaded) remaining = filesized_ratio - uploaded;

			double perc = remaining /filesized_ratio;
			try {
				if (perc >= 0.75) cell.setForeground(color75_100);
				else if ((perc >= 0.50) && (perc < 0.75)) cell.setForeground(color50_75);
				else if ((perc >= 0.25) && (perc < 0.50)) cell.setForeground(color25_50);
				else if (perc != 0.0 && cell.isValid()) cell.setForeground(color0_25);

				if (!cell.setSortValue((float) perc) && cell.isValid())	return;
				
				cell.setText(percentFormatter.format(perc));

				String hovertext = uploadRemaining + " (" + formatter.formatByteCountToKiBEtc((long) remaining) + ")\n";
				hovertext += uploadTarget + " (" + formatter.formatByteCountToKiBEtc((long) filesized_ratio) + ")\n";
				hovertext += uploadRatioOf + " (" + numberFormatter.format(ratio) + ":1)";
				// hovertext +=
				// "\ndiscarded ("+formatter.formatByteCountToKiBEtc
				// (dm.getStats().getDiscarded())+")";
				// hovertext +=
				// "\nhash fails ("+formatter.formatByteCountToKiBEtc
				// (dm.getStats().getHashFails())+")";

				cell.setToolTip(hovertext);

			} catch (Exception e) {
				// If a null point exception is still thrown (had it happen once!?!), then ignore it
				e.printStackTrace();
			}
		}
	}

	
	public class SelectedDoneListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download dm = (Download) cell.getDataSource();
			if (dm == null)	return;

			long filessize = getDownloadingFilesTotalSize(dm.getDiskManagerFileInfo());
			long downloaded = filessize - dm.getStats().getRemaining();


			double perc = (double)downloaded / filessize;
			if (perc < 0) perc = 0;

			if (!cell.setSortValue((float)perc) && cell.isValid()) return;

			cell.setText(percentFormatter.format(perc));
			String hovertext = filesSelectedTotalSize + ": " + formatter.formatByteCountToKiBEtc(filessize);
			cell.setToolTip(hovertext);
		}
	}

	
	public class UploadTargetRatioListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download dm = (Download)cell.getDataSource();
			if (dm == null) return;

			long uploaded = dm.getStats().getUploaded();
			long realDownload = getRealDownloaded(dm);

			float ratio = getNextTargetRatio((float)uploaded / realDownload);

			if (!cell.setSortValue(ratio) && cell.isValid()) return;

			cell.setText(numberFormatter.format(ratio));
		}
	}


	public class AverageDownloadSpeedListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download download = (Download)cell.getDataSource();
			if(download==null) return;

			DownloadStats downloadStats = download.getStats();
			long downloaded = downloadStats.getDownloaded();
			long secondsDownloading = downloadStats.getSecondsDownloading();

			long bytesPerSec = 0;
			if(secondsDownloading > 0) bytesPerSec = downloaded / secondsDownloading;

			if(!cell.setSortValue(bytesPerSec) && cell.isValid()) return;

			cell.setText(formatter.formatByteCountToKiBEtcPerSec(bytesPerSec));
		}
	}


	public class AverageUploadSpeedListener implements TableCellRefreshListener {
		public void refresh(TableCell cell) {
			Download download = (Download)cell.getDataSource();
			if(download==null) return;
			
			DownloadStats downloadStats = download.getStats();
			long uploaded = downloadStats.getUploaded();
			long secondsDownloading = downloadStats.getSecondsDownloading();
			long secondsOnlySeeding = downloadStats.getSecondsOnlySeeding();
			long secondsUploading = (secondsDownloading == -1 ? 0 : secondsDownloading) + (secondsOnlySeeding == -1 ? 0: secondsOnlySeeding);

			long bytesPerSec = 0;
			if(secondsUploading > 0) bytesPerSec = uploaded / secondsUploading;

			if(!cell.setSortValue(bytesPerSec) && cell.isValid()) return;

			cell.setText(formatter.formatByteCountToKiBEtcPerSec(bytesPerSec));
		}
	}


	/**
	 * Returns the real downloaded amount of a Download
	 * @param download The Download
	 * @return realDownload The real downloaded amount of bytes
	 */
	private long getRealDownloaded(Download download) {
		DownloadStats downloadStats = download.getStats();
		long realDownloaded = downloadStats.getDownloaded() - downloadStats.getDiscarded() - (downloadStats.getHashFails() * download.getTorrent().getPieceSize());
		return realDownloaded < 0 ? 0 : realDownloaded;
	}


	/**
	 * Localizes the strings
	 * TODO: Remove the need to change this method when a localized string gets added
	 */
	private void localize(Locale l) {
		numberFormatter = NumberFormat.getNumberInstance(l);
		percentFormatter = NumberFormat.getPercentInstance(l);
		percentFormatter.setMaximumFractionDigits(2);
		uploadETAcolumnExceed = localizer.getLocalisedMessageText(UPLOADETA_COLUMN_EXCEED);
		uploadTarget = localizer.getLocalisedMessageText(UPLOAD_TARGET);
		uploadRemaining = localizer.getLocalisedMessageText(UPLOAD_REMAINING);
		uploadRatioOf = localizer.getLocalisedMessageText(UPLOAD_RATIO_OF);
		filesSelectedTotalSize = localizer.getLocalisedMessageText(FILES_SELECTED_TOTAL_SIZE);
	}

	
	/**
	 * get byte size of all files marked for downloading of one torrent
	 */
	private long getDownloadingFilesTotalSize(DiskManagerFileInfo[] dmfi) {
		long tsize = 0;
		for (int i = 0; i < dmfi.length; i++)
			if (!dmfi[i].isSkipped() && !dmfi[i].isDeleted())
				tsize += dmfi[i].getLength();
		return tsize;
	}

	
	/**
	 * Returns the nextRatio that should be reached or highest ratio
	 * 
	 * @param currentRatio The current reached ratio
	 * @return nextRatio that should be reached or highest ratio
	 */
	private synchronized float getNextTargetRatio(float currentRatio) {
		if (ratioType < 4) return targetRatio;

		int targetRatioListLength = targetRatioList.length;
		if (targetRatioListLength == 0) return ERROR_TARGET_RATIO;
		for (int i=0; i<targetRatioListLength; i++) {
			if (targetRatioList[i] > currentRatio) return targetRatioList[i];
		}
		return targetRatioList[targetRatioListLength-1];
	}

	
	/**
	 * Fetch the ratio settings from the plugin config parameters and set the class fields.
	 * If plugin config parameters are missing they get created.
	 * This method is called from configParameterChanged() from the ConfigParameterListener for ratio related plugin config parameters
	 */
	private synchronized void fetchRatioValues() {
		if (pluginConfig.hasPluginParameter(RATIO_TYPE_PARAMETER_KEY)) {
			ratioType = pluginConfig.getPluginIntParameter(RATIO_TYPE_PARAMETER_KEY, RATIO_TYPE_DEFAULT);
		} else {
			pluginConfig.setPluginParameter(RATIO_TYPE_PARAMETER_KEY, ratioType);
		}

		try {
			switch (ratioType) {
			case 1:
				targetRatio = pluginConfig.getUnsafeIntParameter("StartStopManager_iFirstPriority_ShareRatio", 1000) / 1000.f;
				break;
			case 2:
				targetRatio = pluginConfig.getUnsafeFloatParameter("Stop Ratio");
				break;
			case 3:
				targetRatio = getManualRatioParameter();
				break;
			case 4:
				targetRatio = ERROR_TARGET_RATIO;
				targetRatioList = getManualRatioListStringParameter();
				break;
			default:
				targetRatio = ERROR_TARGET_RATIO;
			}
		} catch (Exception e) {
			e.printStackTrace();
			targetRatio = ERROR_TARGET_RATIO;
		}
	}

	
	/**
	 * Get the manually set ratio out of the plugin config parameter
	 * 
	 * @return float ratio
	 */
	private float getManualRatioParameter() {
		if (pluginConfig.hasPluginParameter(MANUAL_RATIO_PARAMETER_KEY)) {
			try {
				return Float.valueOf(pluginConfig.getPluginStringParameter(MANUAL_RATIO_PARAMETER_KEY, TARGET_RATIO_DEFAULT_STRING)).floatValue();
			} catch (Exception e) {
				e.printStackTrace();
				return ERROR_TARGET_RATIO;
			}
		} else {
			pluginConfig.setPluginParameter(MANUAL_RATIO_PARAMETER_KEY, TARGET_RATIO_DEFAULT_STRING);
			return TARGET_RATIO_DEFAULT;
		}
	}

	
	/**
	 * Get the ManualRatioListStringParameter from the plugin config into a float array
	 * 
	 * @return the ManualRatioList array
	 */
	private float[] getManualRatioListStringParameter() {
		if (pluginConfig.hasPluginParameter(MANUAL_RATIO_LIST_STRING_PARAMETER_KEY)) {
			String[] manualRatioListStrings = pluginConfig.getPluginStringParameter(MANUAL_RATIO_LIST_STRING_PARAMETER_KEY).split(MANUAL_RATIO_LIST_STRING_DELIMITER);
			float[] rManualRatioList = new float[manualRatioListStrings.length];
			try {
				for (int i = 0; i < manualRatioListStrings.length; i++)
					rManualRatioList[i] = Float.valueOf(manualRatioListStrings[i]).floatValue();
			} catch (Exception e) {
				e.printStackTrace();
				rManualRatioList = new float[1];
				rManualRatioList[0] = ERROR_TARGET_RATIO;
			}
			return rManualRatioList;
		} else {
			pluginConfig.setPluginParameter(MANUAL_RATIO_LIST_STRING_PARAMETER_KEY, TARGET_RATIO_DEFAULT_STRING + MANUAL_RATIO_LIST_STRING_DELIMITER);
			return TARGET_RATIO_LIST_DEFAULT;
		}
	}


	/**
	 * Fetch the color settings from the plugin config parameters and set the class fields.
	 * If plugin config parameters are missing they get created.
	 * This method is called from configParameterChanged() from the ConfigParameterListener for color related plugin config parameters
	 */
	private synchronized void fetchColorValues() {
		int[] colors = new int[COLOR_PARAMETER_KEYS.length];
		for (int i = 0; i < COLOR_PARAMETER_KEYS.length; i++) {
			if (pluginConfig.hasPluginParameter(COLOR_PARAMETER_KEYS[i])) {
				colors[i] = pluginConfig.getPluginIntParameter(COLOR_PARAMETER_KEYS[i], COLOR_DEFAULTS[i]);
			} else {
				colors[i] = COLOR_DEFAULTS[i];
				pluginConfig.setPluginParameter(COLOR_PARAMETER_KEYS[i], colors[i]);
			}
		}
		for (int i = 0; i < 3; i++) {
			color0_25[i] = colors[i];
			color25_50[i] = colors[i+3];
			color50_75[i] = colors[i+6];
			color75_100[i] = colors[i+9];
		}
	}

}