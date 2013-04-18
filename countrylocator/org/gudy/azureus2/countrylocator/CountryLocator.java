/*
 * File    : CountryLocator.java
 * Created : 29 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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

package org.gudy.azureus2.countrylocator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.csvtodat.CsvToDat;

/**
 * @author Olivier<br>
 * @author TuxPaper<br> (update to use TableManager, add flag & country full name)
 *    <li>2005/Oct/02 v1.6: Ignore country "--"
 *    <li>2005/Oct/10 v1.6: <ul>
 *            <li>Update to new UI Interface
 *            <li>Redraw flag less
 *            <li>Reformat code to Java Conventions (w/Tabs because I like less 
 *                than 4 indent)
 *        </ul>
 *    <li>2005/Oct/18 v1.6: <ul>
 *            <li>Made backward compatible w/pre 2306 by Moving new UI 
 *                Interface to new classes so they can be trapped.
 *            <li>Download new GeoIP.dat every month
 *        </ul>
 *                      
 */
public class CountryLocator
{
	private static final String ID = "CountryLocator";

	private static final String GEOIP_URL = "http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz";

	private static final String CFG_GETGEOIPON = "GetGeoIPOn";

	private static final String CFG_REGEX = "csvRegex";

	private static final String CFG_UPDATELOCATION = "updateLocation";

	private static final String CFG_AUTOUPDATE = "autoUpdate";

	/** Subfolder where small flag images reside */
	public static final String FLAGS_SMALL = "18x12";

	/** Subfolder where big flag images reside */
	public static final String FLAGS_BIG = "25x15";

	/** Folder where flag images subfolders are */
	public static final String FLAGS_LOC = "org/gudy/azureus2/countrylocator/flags";

	/** Cache images, so we don't have to re-load them each use
	 * Key: Subfolder+CountryCode;
	 * Value: Image 
	 */
	private static HashMap images = new HashMap();

	/** Azureus' SWT CountryLocator Instance */
	public static UISWTInstance swtInstance = null;

	/** Display used by Azureus.  Stored here for speed */
	public static Display display = null;

	/** Last IP looked up */
	private static String lastIP = "";

	/** Last Country looked up */
	private static Country lastCountry = null;

	/** MaxMind's Country lookup service */
	private static LookupService cl;

	/** Our ClassLoader, so we can retrieve files within the JAR */
	private static ClassLoader classLoader = CountryLocator.class.getClassLoader();

	private PluginInterface pluginInterface = null;

	private PluginConfig config = null;

	private PluginListener pluginListener = null;

	private LoggerChannel logger = null;

	private File fGeoIP = null;
	
	/* map view stuff */
	static final String VIEWID = "org.gudy.azureus2.countrylocator.WorldMapView";
	
	static WorldMapViewListener viewListener = null;
	
	/**
	 * Initialize
	 * 
	 * @param pluginInterface  AZ's PluginInterface
	 */
	public void initialize(final PluginInterface pluginInterface) {
		this.pluginInterface = pluginInterface;
		logger = pluginInterface.getLogger().getChannel(ID);
		config = pluginInterface.getPluginconfig();

		boolean bAutoUpdate = config.getPluginBooleanParameter(CFG_AUTOUPDATE,
				false);

		pluginListener = new PluginListener() {
			public void closedownComplete() {
				unloadObjects();
			}

			public void initializationComplete() {
			}

			public void closedownInitiated() {
			}
		};
		pluginInterface.addListener(pluginListener);
		HookUtils.hookUIInstance(pluginInterface);
		
		/* Add the UIManagerListener for the map view */
		pluginInterface.getUIManager().addUIListener(new MapViewUIManagerListener(this));
		
		// Config page
		BasicPluginConfigModel model = pluginInterface.getUIManager().createBasicPluginConfigModel(
				ID);
		model.addBooleanParameter2(CFG_AUTOUPDATE, ID + ".autoUpdate.enabled",
				false);
		final StringParameter sp = model.addStringParameter2(CFG_UPDATELOCATION, ID
				+ ".updateLocation", GEOIP_URL);
		model.addLabelParameter2(ID + ".updateLocation.info");
		model.addStringParameter2(CFG_REGEX, ID + ".csvRegex", CsvToDat.MATCHSTR);
		model.addLabelParameter2(ID + ".csvRegex.info");
		
		/* buttons */
		final ActionParameter ap = model.addActionParameter2(null, ID
				+ ".updateNow");
		
		model.addActionParameter2(null, ID + ".useInternalDB").addListener(
				new ParameterListener() {
					public void parameterChanged(Parameter param) {
						param.setEnabled(false);
						if (cl != null) {
							cl.close();
							cl = null;
						}
						
						if (fGeoIP.exists())
							fGeoIP.delete();
						
						loadDefaultDatabase(false);
						param.setEnabled(true);
					}
				});
		
		/* put some space between the buttons */
		final LabelParameter lp = model.addLabelParameter2(null);
		
		ap.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				boolean bCanSetLabel;
				boolean bCanSetEnabled;
				
				try {
					param.setEnabled(false);
					bCanSetEnabled = true;
				} catch (NoSuchMethodError e) {
					// setEnabled >= 2300
					bCanSetEnabled = false;
				}
				
				try {
					lp.setLabelKey("CountryLocator.updateStatus.updating");
					bCanSetLabel = true;
				} catch (NoSuchMethodError e) {
					// setLabelKey >= 2306
					bCanSetLabel = false;
				}
				getNewGeoIP(fGeoIP, sp.getValue(), true, bCanSetLabel ? lp : null,
						bCanSetEnabled ? ap : null);
			}
		});

		/* geomap options */
				
		/* Add invisible parameters holding the results from the color dialogs */
		// TODO this is a pretty ugly hack, but the plugin API doesn't seem to support color dialogs...
		// Even though the params are invisible they still use space in the pref page
		model.addIntParameter2(MapView.CFG_COUNTRY_MIN_COLOR, null, 
				GraphicUtils.rgb2int(UIConstants.COUNTRY_MIN_DEFAULT_COLOR)).setVisible(false);
		model.addIntParameter2(MapView.CFG_COUNTRY_MAX_COLOR, null,
				GraphicUtils.rgb2int(UIConstants.COUNTRY_MAX_DEFAULT_COLOR)).setVisible(false);
		model.addIntParameter2(MapView.CFG_COUNTRY_HIGHLIGHT_COLOR, null,
				GraphicUtils.rgb2int(UIConstants.COUNTRY_HIGHLIGHT_DEFAULT_COLOR)).setVisible(false);
		
		/* init colors with the current values from the properties stored in the config */
		int currentMinColor = config.getPluginIntParameter(MapView.CFG_COUNTRY_MIN_COLOR);
		int currentMaxColor = config.getPluginIntParameter(MapView.CFG_COUNTRY_MAX_COLOR);
		int currentHighlightColor = config.getPluginIntParameter(MapView.CFG_COUNTRY_HIGHLIGHT_COLOR);
		ZoomedScrolledMap.countryMinColor = new Color(display, 
				GraphicUtils.int2rgb(currentMinColor));
		ZoomedScrolledMap.countryMaxColor = new Color(display, 
				GraphicUtils.int2rgb(currentMaxColor));
		ZoomedScrolledMap.countryHighlightColor = new Color(display, 
				GraphicUtils.int2rgb(currentHighlightColor));

		model.addLabelParameter2(ID + ".geomap.info");
		
		/* color dialogs */
		class colorParameterListener implements ParameterListener {

			/** title of the color dialog */
			private final String title;
			
			/** name (key) of the property in the config */
			private final String paramName;
			
			public colorParameterListener(String title, String paramName) {
				this.title = title;
				this.paramName = paramName;
				
			}
			
			/** Opens a <code>ColorDialog</code> to set a new color for <code>paramName</code> */
			public void parameterChanged(Parameter param) {
				ColorDialog dialog = new ColorDialog(Display.getCurrent().getActiveShell());
				// atm we use the invisible parameter to store the values
				int currentColor = config.getPluginIntParameter(paramName);
				dialog.setText(title);
				dialog.setRGB(GraphicUtils.int2rgb(currentColor));
				RGB selectedColor = dialog.open();
				
				if (selectedColor != null) {
					/* set the new value in the config */
					config.setPluginParameter(paramName, 
							GraphicUtils.rgb2int(selectedColor));
					/* set the new value in the map */
					// TODO Is there a more elegant way to do this?
					Color color = new Color(display, selectedColor);
					if (paramName.equals(MapView.CFG_COUNTRY_MIN_COLOR)) {
						ZoomedScrolledMap.countryMinColor = color;						
					} else if (paramName.equals(MapView.CFG_COUNTRY_MAX_COLOR)) {
						ZoomedScrolledMap.countryMaxColor = color;
					} else
						ZoomedScrolledMap.countryHighlightColor = color;
				}
			}
			
		};

		ActionParameter minColorParam = model.addActionParameter2(
				ID + ".geomap.CountryMinColor", ID + ".geomap.SelectColor");
		minColorParam.addListener(new colorParameterListener(
				"Select Country Min Color", MapView.CFG_COUNTRY_MIN_COLOR));
		
		ActionParameter maxColorParam = model.addActionParameter2(
				ID + ".geomap.CountryMaxColor", ID + ".geomap.SelectColor");
		maxColorParam.addListener(new colorParameterListener(
				"Select Country Max Color", MapView.CFG_COUNTRY_MAX_COLOR));
		
		ActionParameter highlightColorParam = model.addActionParameter2(
				ID + ".geomap.CountryHighlightColor", ID + ".geomap.SelectColor");
		highlightColorParam.addListener(new colorParameterListener(
				"Select Country Highlight Color", MapView.CFG_COUNTRY_HIGHLIGHT_COLOR));
		
		/* reset colors */
		model.addActionParameter2(null, ID + ".geomap.DefaultColor").addListener(
				new ParameterListener() {
					public void parameterChanged(Parameter param) {
						ZoomedScrolledMap.countryMinColor = new Color(display, UIConstants.COUNTRY_MIN_DEFAULT_COLOR);
						ZoomedScrolledMap.countryMaxColor = new Color(display, UIConstants.COUNTRY_MAX_DEFAULT_COLOR);
						ZoomedScrolledMap.countryHighlightColor = new Color(display, UIConstants.COUNTRY_HIGHLIGHT_DEFAULT_COLOR);
						config.setPluginParameter(MapView.CFG_COUNTRY_MIN_COLOR, GraphicUtils.rgb2int(UIConstants.COUNTRY_MIN_DEFAULT_COLOR));
						config.setPluginParameter(MapView.CFG_COUNTRY_MAX_COLOR, GraphicUtils.rgb2int(UIConstants.COUNTRY_MAX_DEFAULT_COLOR));
						config.setPluginParameter(MapView.CFG_COUNTRY_HIGHLIGHT_COLOR, GraphicUtils.rgb2int(UIConstants.COUNTRY_HIGHLIGHT_DEFAULT_COLOR));
					}
				});
		

		// Load Database

		if (!loadDefaultDatabase(!bAutoUpdate))
			return;

		/* Add Columns */
		String [] peer_tables = new String[] {
			TableManager.TABLE_TORRENT_PEERS,
			TableManager.TABLE_ALL_PEERS,
		};
		
		// ISO3166
		for (int i=0; i<peer_tables.length; i++) {
			TableManager tm = pluginInterface.getUIManager().getTableManager();
			TableColumn iso3166Column = tm.createColumn(
					peer_tables[i], "CountryCode");
			iso3166Column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST,
					30, TableColumn.INTERVAL_INVALID_ONLY);
			iso3166Column.addCellRefreshListener(new TableCellRefreshListener() {
				public void refresh(TableCell cell) {
					Peer peer = (Peer) cell.getDataSource();
					Country loc = (peer == null) ? null : getCountry(peer.getIp());
					String s = (loc == null) ? "" : loc.getCode();
	
					if (!cell.setSortValue(s) && cell.isValid())
						return;
					cell.setText(s);
				}
			});
			tm.addColumn(iso3166Column);
	
			// Country Name
			TableColumn countryColumn = tm.createColumn(
					peer_tables[i], "Country");
			countryColumn.initialize(TableColumn.ALIGN_LEAD,
					TableColumn.POSITION_INVISIBLE, 80, TableColumn.INTERVAL_INVALID_ONLY);
			countryColumn.addCellRefreshListener(new TableCellRefreshListener() {
				public void refresh(TableCell cell) {
					Peer peer = (Peer) cell.getDataSource();
					Country loc = (peer == null) ? null : getCountry(peer.getIp());
					String s = "";
					if (loc != null)
						s = getCountryName(loc, Locale.getDefault());
					if (!cell.setSortValue(s) && cell.isValid())
						return;
					cell.setText(s);
				}
			});
			tm.addColumn(countryColumn);
	
			// Small Flags
			TableColumn flagsColumn = tm.createColumn(peer_tables[i],
					"CountryFlagSmall");
			flagsColumn.initialize(TableColumn.ALIGN_LEAD,
					TableColumn.POSITION_INVISIBLE, 25, TableColumn.INTERVAL_INVALID_ONLY);
			flagsColumn.setType(TableColumn.TYPE_GRAPHIC);
			FlagListener flagListener = new FlagListener(FLAGS_SMALL);
			flagsColumn.addCellRefreshListener(flagListener);
			flagsColumn.addCellToolTipListener(flagListener);
			tm.addColumn(flagsColumn);
	
			// Normal Flags
			flagsColumn = tm.createColumn(peer_tables[i],
					"CountryFlag");
			flagsColumn.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST,
					25, TableColumn.INTERVAL_INVALID_ONLY);
			flagsColumn.setType(TableColumn.TYPE_GRAPHIC);
			flagListener = new FlagListener(FLAGS_BIG);
			flagsColumn.addCellRefreshListener(flagListener);
			flagsColumn.addCellToolTipListener(flagListener);
			tm.addColumn(flagsColumn);
		}
	}

	/**
	 * Load GeoIP.dat from our jar, or from local file if there is one.
	 * 
	 * @param bSkipUpdate Whether to skip update 
	 * 
	 * @return success
	 */
	private boolean loadDefaultDatabase(boolean bSkipUpdate) {
		boolean bTempFile = false;
		long lGeoIPTime = -1;

		if (cl != null) {
			cl.close();
			cl = null;
		}

		// Copy GeoIP.dat to the plugin directory for faster access
		try {
			// get the time of the internal GeoIP 
			// 1) Get the URL
			URL url = classLoader.getResource("GeoIP.dat");
			
			long lInternalTime;
			
			if ( url.getProtocol().equalsIgnoreCase( "file" )){
				
				// resolved to a file (most likely we're running some test code here...
				
				try{
					fGeoIP = new File( url.toURI());
					
				}catch( Throwable e ){
					
					throw( new IOException( "Can't handle url: " + url ));
				}
				
				lInternalTime = fGeoIP.lastModified();
				
			}else{
				// 2) Change it to an URI
				String sInternalPath = url.toExternalForm();
				sInternalPath = sInternalPath.replaceAll(" ", "%20");
				if (sInternalPath.startsWith("jar:file:")
						&& sInternalPath.charAt(9) != '/')
					sInternalPath = "jar:file:/" + sInternalPath.substring(9);
				int posPling = sInternalPath.lastIndexOf('!');
				sInternalPath = sInternalPath.substring(4, posPling);
				URI uri = URI.create(sInternalPath);
				// 3) Create File
				File fInternalGeoIP = new File(uri);
				// 4) Open the JarFile
				JarFile jarFile = new JarFile(fInternalGeoIP);
				// 5) get the JarEntry
				JarEntry entry = jarFile.getJarEntry("GeoIP.dat");
				// 6) get the time
				lInternalTime = entry.getTime();
				// 7) gasp for fresh air and wish you knew of an easier way!

				// get the file we want to create
				String sDir = pluginInterface.getPluginDirectoryName();
				if (!sDir.endsWith(File.separator))
					sDir += File.separator;
				fGeoIP = new File(sDir + "GeoIP.dat");
	
				boolean bCopyFromJar = (!fGeoIP.exists() || lInternalTime > fGeoIP.lastModified());
				if (!bCopyFromJar) {
					// make sure it's valid
					try {
						cl = new LookupService(fGeoIP);
	
						// force error by looking up whitehouse.gov
						bCopyFromJar = cl.getCountry("63.161.169.137") == LookupService.UNKNOWN_COUNTRY;
						cl.close();
					} catch (IOException e) {
						bCopyFromJar = true;
					}
	
					if (bCopyFromJar)
						logger.log(LoggerChannel.LT_WARNING,
								"Local GeoIP.dat invalid, using internal");
				}
	
				if (bCopyFromJar) {
					InputStream is = classLoader.getResourceAsStream("GeoIP.dat");
	
					// If we can't write to plugin dir, use a temp file
					if (!copyFile(is, fGeoIP)) {
						logger.log(LoggerChannel.LT_ERROR, "Can't Write to " + sDir);
						fGeoIP = File.createTempFile("Geo", null);
						fGeoIP.deleteOnExit();
						if (!copyFile(is, fGeoIP))
							return false;
						bTempFile = true;
					}
	
					is.close();
					fGeoIP.setLastModified(lInternalTime);
				}
			}
			
			lGeoIPTime = Math.max(lInternalTime, fGeoIP.lastModified());

			cl = new LookupService(fGeoIP);
		} catch (IOException e) {
			logger.log("Error while Initializing GeoIP", e);
			return false;
		}

		if (!bTempFile && !bSkipUpdate) {
			String sGetGeoIPOn = config.getPluginStringParameter(CFG_GETGEOIPON);
			long lGetGeoIPOn = 0;
			try {
				lGetGeoIPOn = Long.parseLong(sGetGeoIPOn);
			} catch (NumberFormatException e) {
				// ignore
			}

			if (lGetGeoIPOn == 0) {
				// Never downloaded before, set next download to one month after
				// current GeoIP date, with a random day offset to spread out server 
				// load
				Calendar c = new GregorianCalendar();
				c.setTimeInMillis(lGeoIPTime);
				c.add(Calendar.MONTH, 1);
				c.add(Calendar.DAY_OF_MONTH, (int) (Math.random() * 15));
				lGetGeoIPOn = c.getTimeInMillis();
				config.setPluginParameter(CFG_GETGEOIPON, String.valueOf(lGetGeoIPOn));
				config.setPluginParameter("FailCount", 0);
			}

			DateFormat df = new SimpleDateFormat();
			logger.log(LoggerChannel.LT_INFORMATION,
					"CountryLocator: Next GeoIP.dat get is "
							+ df.format(new Date(lGetGeoIPOn)));

			if (System.currentTimeMillis() > lGetGeoIPOn)
				getNewGeoIP(fGeoIP, config.getPluginStringParameter(CFG_UPDATELOCATION,
						GEOIP_URL), true, null, null);
		}

		return true;
	}

	/**
	 * Import a new GeoIP.dat and set it as current
	 * 
	 * @param fGeoIP
	 * @param src
	 * @param bAsync
	 * 
	 * @return status message (only if synchronous)
	 */
	private boolean getNewGeoIP(final File fGeoIP, final String src,
			boolean bAsync, LabelParameter status, ActionParameter paramToEnable)
	{

		if (fGeoIP == null || pluginInterface == null) {
			if (paramToEnable != null)
				paramToEnable.setEnabled(true);
			if (status != null)
				status.setLabelText("null");

			return false;
		}

		final Utilities utils = pluginInterface.getUtilities();

		try {
			ResourceDownloaderFactory rdf = utils.getResourceDownloaderFactory();
			ResourceDownloader downloader;
			if (src.startsWith("http://") || src.startsWith("https://")
					|| src.startsWith("ftp://")) {
				downloader = rdf.create(new URL(src));
			} else {
				File file = new File(src);
				downloader = rdf.create(file);
			}

			ResourceDownloaderListener rdl = new GeoIPDownloader(src, paramToEnable,
					status);
			downloader.addListener(rdl);

			logger.log(LoggerChannel.LT_INFORMATION,
					"Retrieving new country information from " + src);
			if (bAsync)
				downloader.asyncDownload();
			else
				try {
					downloader.download();
				} catch (ResourceDownloaderException e1) {
					e1.printStackTrace();
					if (paramToEnable != null)
						paramToEnable.setEnabled(true);
					if (status != null)
						status.setLabelText(e1.getMessage());

					return false;
				}

		} catch (MalformedURLException e) {
			e.printStackTrace();
			if (paramToEnable != null)
				paramToEnable.setEnabled(true);
			if (status != null)
				status.setLabelText(e.getMessage());

			return false;
		}

		return true;
	}

	/** Retrieve an image in cache, or add it to the cache and return it.
	 * 
	 * @param loc location of the image (TODO shouldn't this be called "size"?)
	 * @param code country code of image
	 * @return Graphic for location and code
	 */
	/* note (gooogelybear): Changed to package visibility to be accessible from
	 * MapView */
	synchronized Graphic getImage(String loc, String code) {
		if (display == null || display.isDisposed() || code.equals("--"))
			return null;

		Graphic graphic = (Graphic) images.get(loc + code);
		if (graphic == null) {
			/* image not in cache */
			InputStream is = classLoader.getResourceAsStream(FLAGS_LOC + loc + "/"
					+ code + ".png");
			if (null != is) {
				graphic = GraphicUtils.createGraphic(new Image(display, is));
			} else {
				logger.log(LoggerChannel.LT_INFORMATION,
						"Country Flag Image Not Found: " + code);
			}
			images.put(loc + code, graphic);
		}
		return graphic;
	}

	/** Destroy what we created */
	public void unloadObjects() {
		/* remove the map view */
		if(swtInstance != null) // TODO Ok here?
			swtInstance.removeViews(UISWTInstance.VIEW_TORRENT_PEERS, VIEWID);
		
		if (pluginListener != null) {
			pluginInterface.removeListener(pluginListener);
			pluginListener = null;
		}

		if (cl != null) {
			cl.close();
			cl = null;
		}

		Iterator iter = images.values().iterator();
		while (iter.hasNext()) {
			Graphic graphic = (Graphic) iter.next();
			if (graphic != null) {
				Image im = null; //GetGraphicUtil.getImage(graphic);

				if (im != null && !im.isDisposed())
					im.dispose();
			}
		}
	}

	/** Retrieve a country based on an IP.  Cache the last found country to
	 * save some time (multiple columns refreshing one after another)
	 * 
	 * @param ip IP address to find country for
	 * @return The country, null if not found
	 */
	private Country getCountry(String ip) {
		Country ret = null;
		if (ip == null)
			return null;

		if (ip.equals(lastIP))
			return lastCountry;

		if (cl == null)
			return null;

		try {
			ret = cl.getCountry(ip);
		} catch (Exception e) {
			// ignore
		}

		lastCountry = ret;
		lastIP = ip;
		return ret;
	}

	/**
	 * Copy a InputStream to a File
	 * 
	 * @param is InputStream to be copied
	 * @param outFile File to copy to
	 * @return success level
	 */
	public static boolean copyFile(InputStream is, File outFile) {
		final byte[] bytes = new byte[1024];

		FileOutputStream os = null;
		try {
			if (!(is instanceof BufferedInputStream))
				is = new BufferedInputStream(is);

			os = new FileOutputStream(outFile);
			while (true) {
				int count = is.read(bytes);
				if (count == -1)
					break;
				os.write(bytes, 0, count);
			}
			os.close();
			return true;
		} catch (IOException e) {
			System.err.println(e);
			if (os != null) {
				try {
					os.close();
				} catch (Exception e2) {
					// ignore
				}
			}
		}
		return false;
	}

	/** 
	 * Flag column refresher
	 * 
	 * @author TuxPaper 2005/Oct/10 v1.6 don't set image if not needed
	 */
	private class FlagListener implements TableCellRefreshListener,
			TableCellToolTipListener
	{
		String sFlagSize;

		/**
		 * Initializer
		 * 
		 * @param strLoc Location of images
		 */
		public FlagListener(String strLoc) {
			sFlagSize = strLoc;
		}

		public void refresh(TableCell cell) {
			Peer peer = (Peer) cell.getDataSource();
			Country loc = (peer == null) ? null : getCountry(peer.getIp());
			String s = (loc == null) ? "" : loc.getCode();

			if (!cell.setSortValue(s) && cell.isValid())
				return;

			Graphic graphic = (loc == null) ? null : getImage(sFlagSize,
					s.toLowerCase());
			cell.setGraphic(graphic);
		}

		public void cellHover(TableCell cell) {
			Peer peer = (Peer) cell.getDataSource();
			Country loc = (peer == null) ? null : getCountry(peer.getIp());
			String s = "";
			if (loc != null) {
				s = loc.getCode() + " - ";
				if (loc != null)
					s += getCountryName(loc, Locale.getDefault());
			}
			cell.setToolTip(s);
		}

		public void cellHoverComplete(TableCell cell) {
			cell.setToolTip(null);
		}
	}

	/**
	 * Downloads new IP to Country database
	 */
	private class GeoIPDownloader implements ResourceDownloaderListener
	{
		private String src;

		private Parameter paramToEnable;

		private Parameter status;

		public GeoIPDownloader(String sLocation, Parameter paramToEnable,
				Parameter status) {
			src = sLocation;
			this.paramToEnable = paramToEnable;
			this.status = status;
		}

		public void reportPercentComplete(ResourceDownloader downloader,
				int percentage)
		{
			if (status != null)
				status.setLabelText("" + percentage + "%");
		}
		
		public void reportAmountComplete(ResourceDownloader downloader, long amount) {
			// TODO Auto-generated method stub

		}
		
		public void reportActivity(ResourceDownloader downloader, String activity) {
			if (status != null)
				status.setLabelText(activity);
		}

		public boolean completed(ResourceDownloader downloader, InputStream data) {
			final LocaleUtilities locale = pluginInterface.getUtilities().getLocaleUtilities();

			logger.log(LoggerChannel.LT_INFORMATION, "Recieved file");

			try {
				long lFileTime;
				DateFormat df = new SimpleDateFormat();
				String sFileName;
				String sType;

				if (!(data instanceof BufferedInputStream))
					data = new BufferedInputStream(data);

				byte[] bytes = new byte[2];
				data.mark(3);
				data.read(bytes, 0, 2);
				data.reset();

				if (bytes[1] == (byte) 0x8b && bytes[0] == 0x1f) {
					sType = "gzip";

					DetailedGZIPInputStream gzip = new DetailedGZIPInputStream(data);

					sFileName = gzip.getName();
					if (sFileName == "") {
						if (src.endsWith(".gz"))
							sFileName = src.substring(0, src.lastIndexOf(".gz"));
						else
							sFileName = src;
					}

					lFileTime = gzip.getTime();
					data = gzip;

				} else if (bytes[0] == 0x50 && bytes[1] == 0x4b) {
					sType = "zip";

					ZipInputStream zip = new ZipInputStream(data);

					ZipEntry zipEntry = zip.getNextEntry();
					// Skip small files
					while (zipEntry != null && zipEntry.getSize() < 1024 * 1024) {
						zipEntry = zip.getNextEntry();
					}

					if (zipEntry == null) {
						if (paramToEnable != null)
							paramToEnable.setEnabled(true);
						if (status != null)
							status.setLabelKey("CountryLocator.updateStatus.invalidFormat");

						return true;
					}
					data = zip;
					lFileTime = zipEntry.getTime();
					sFileName = zipEntry.getName();
				} else {
					sType = "uncompressed";

					File file = new File(src);
					if (file.exists())
						lFileTime = file.lastModified();
					else
						lFileTime = System.currentTimeMillis();

					sFileName = src;
				}

				if (cl != null) {
					cl.close();
					cl = null;
				}

				logger.log(LoggerChannel.LT_INFORMATION, "Found " + sFileName + " of "
						+ sType + " type with a time of " + df.format(new Date(lFileTime)));

				if (lFileTime == -1 || lFileTime >= fGeoIP.lastModified()) {
					if (sFileName.endsWith(".dat") || sFileName.endsWith(".dat.gz") || sFileName.endsWith(".dat.zip") ) {
						copyFile(data, fGeoIP);
					} else {
						CsvToDat cvsToDat = new CsvToDat();

						InputStreamReader reader = new InputStreamReader(data);
						OutputStream os = new FileOutputStream(fGeoIP);
						long count = cvsToDat.csvToDat(config.getPluginStringParameter(
								CFG_REGEX, CsvToDat.MATCHSTR), reader, os);

						if (status != null) {
							status.setLabelText(status.getLabelText()
									+ "\n"
									+ locale.getLocalisedMessageText(
											"CountryLocator.updateStatus.numRangesImported",
											new String[] { "" + count }) + "\n");
						}
					}

					if (lFileTime != -1)
						fGeoIP.setLastModified(lFileTime);

				} else {
					if (status != null) {
						status.setLabelText(status.getLabelText()
								+ "\n"
								+ locale.getLocalisedMessageText("CountryLocator.updateStatus.notNewer"));
					}
				}

				cl = new LookupService(fGeoIP);

				// force error by looking up whitehouse.gov
				if (cl.getCountry("63.161.169.137") == LookupService.UNKNOWN_COUNTRY) {
					cl.close();
					cl = null;
					fGeoIP.delete();
					loadDefaultDatabase(true);

					if (status != null) {
						status.setLabelKey("CountryLocator.updateStatus.invalidFormat");
					}
				} else {
					// Make next update one month from now
					Calendar c = new GregorianCalendar();
					c.add(Calendar.MONTH, 1);
					long lNextTime = c.getTimeInMillis();
					config.setPluginParameter(CFG_GETGEOIPON, String.valueOf(lNextTime));
					logger.log(LoggerChannel.LT_INFORMATION,
							"File saved. Next check will be "
									+ df.format(new Date(lNextTime)));

					if (status != null) {
						status.setLabelText(status.getLabelText()
								+ "\n"
								+ locale.getLocalisedMessageText("CountryLocator.updateStatus.ok")
								+ locale.getLocalisedMessageText(
										"CountryLocator.updateStatus.nextUpdate",
										new String[] { df.format(new Date(lNextTime)) }));

						status.setLabelText(status.getLabelText() + "\n" + "Database: "
								+ cl.getDatabaseInfo().toString());
					}

					config.setPluginParameter("FailCount", 0);
				}

				data.close();
			} catch (IOException e) {
				e.printStackTrace();
				if (status != null) {
					status.setLabelText(status.getLabelText() + "\n" + e.getMessage());
				}
				if (cl == null) {
					if (fGeoIP.isFile())
						try {
							cl = new LookupService(fGeoIP);
						} catch (IOException e1) {
							e1.printStackTrace();
							loadDefaultDatabase(true);
						}
					else
						loadDefaultDatabase(true);
				}

			}

			if (paramToEnable != null)
				paramToEnable.setEnabled(true);

			return true;
		}

		public void failed(ResourceDownloader downloader,
				ResourceDownloaderException e)
		{
			logger.log("GeoIP update failed ", e);
			int iFailCount = config.getPluginIntParameter("FailCount");
			// If we've failed 3 times (3 start ups) in a row, delay it for a
			// week
			if (iFailCount > 3) {
				Calendar c = new GregorianCalendar();
				c.add(Calendar.WEEK_OF_MONTH, 1);
				config.setPluginParameter(CFG_GETGEOIPON,
						String.valueOf(c.getTimeInMillis()));
				config.setPluginParameter("FailCount", 0);
			} else {
				config.setPluginParameter("FailCount", iFailCount++);
			}

			if (paramToEnable != null)
				paramToEnable.setEnabled(true);
			if (status != null)
				status.setLabelText(status.getLabelText() + "\n" + e.getMessage());
		}

	}

	public Locale getIPLocale(String sIP) {
		Country loc = getCountry(sIP);
		if (loc != null) {
			// Try to get the localized country name
			return new Locale("", loc.getCode());
		}
		return new Locale("", "", "");
	}

	public String getIPCountry(String sIP, Locale inLocale) {
		Country loc = getCountry(sIP);
		if (loc != null)
			return getCountryName(loc, inLocale);
		return "";
	}

	public String getIPISO3166(String sIP) {
		Country loc = getCountry(sIP);
		if (loc != null)
			return loc.getCode();

		return "";
	}

	private String getCountryName(Country loc, Locale inLocale) {
		Locale locale = new Locale("", loc.getCode());
		try {
			locale.getISO3Country();
			return locale.getDisplayCountry(inLocale);
		} catch (MissingResourceException e) {
			return loc.getName();
		}
	}
	
}
