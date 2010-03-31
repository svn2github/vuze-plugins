/**
 * File: StatusIconPlugin.java
 * Library: Status Icon Plugin for Azureus
 * Date: 22 May 2007
 *
 * Author: Allan Crooks
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.aelitis.azureus.plugins.statusicon;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringListParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

public class StatusIconPlugin implements Plugin, DownloadListener, TableCellRefreshListener, UIManagerListener {
	
	private TableColumn[] columns = null;
	private SkinBar skinbar = null;
	private UISWTInstance swt_ui = null;
	private PluginConfig config = null;
	private LoggerChannel channel = null;
	private LocaleUtilities lu = null;
	private Properties bundled_skins = null;
	private PluginInterface plugin_interface = null;
	
	public void initialize(PluginInterface plugin_interface) {
		this.plugin_interface = plugin_interface;
		
		// This will notify us of all download state changes.
		plugin_interface.getDownloadManager().getGlobalDownloadEventNotifier().addListener(this);
	
		// Create the columns we will use.
		TableManager tm = plugin_interface.getUIManager().getTableManager();
		this.columns = new TableColumn[] {
			tm.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "dl_status_icon"),
			tm.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE,   "dl_status_icon")
		};
		
		// Now set up the columns.
		for (int i=0; i<this.columns.length; i++) {
			TableColumn tc = this.columns[i];
			tc.setType(TableColumn.TYPE_GRAPHIC);
			tc.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
			tc.setPosition(0);
			tc.setWidth(20);
			tc.addCellRefreshListener(this);
			tm.addColumn(tc);
		}
		
		// Before we create the skin bar, we have to wait for the UIInstance to arrive.
		this.config = plugin_interface.getPluginconfig();
		this.channel = plugin_interface.getLogger().getChannel("statusicon");
		this.lu = plugin_interface.getUtilities().getLocaleUtilities();
		
		// Create the config page.
		BasicPluginConfigModel model = plugin_interface.getUIManager().createBasicPluginConfigModel("statusicon");

		// Do you want to load from file, or choose a bundled skin?
		String[] skin_source_type_values = new String[] {"none", "bundled", "file"};
		String[] skin_source_type_labels = new String[skin_source_type_values.length];
		for (int i=0; i<skin_source_type_labels.length; i++) {
			skin_source_type_labels[i] = lu.getLocalisedMessageText(
					"statusicon.config.skin_source_type." + skin_source_type_values[i]);
		}

		Parameter skin_source_type = model.addStringListParameter2(
			"skin_source_type", "statusicon.config.skin_source_type",
			skin_source_type_values, skin_source_type_labels, "none"
		);
		
		// What bundled skin would the user like to use?
		bundled_skins = new Properties();
		try {bundled_skins.load(getInternalResource("icons/skins.properties"));}
		catch (IOException ioe) {channel.log("Error loading skins.properties", ioe);}
		
		ArrayList skin_file_names = new ArrayList();
		ArrayList skin_titles = new ArrayList();
		
		int i = 1;
		while (true) {
			String skin_title = bundled_skins.getProperty("skin" + i + ".title");
			if (skin_title == null) {break;}
			skin_titles.add(skin_title);
			skin_file_names.add(bundled_skins.getProperty("skin" + i + ".file"));
			i++;
		}
		
		final Parameter skin_bundled_file = model.addStringListParameter2("skin_bundled_file", "statusicon.config.skin_bundled_file",
				(String[])skin_file_names.toArray(new String[0]),
				(String[])skin_titles.toArray(new String[0]),
				(String)skin_file_names.get(0));
		
		final Parameter skin_external_file = model.addFileParameter2("skinfile", "statusicon.config.skinfile", "");
		
		// What types of skin do we support?
		String[] skin_values = new String[] {"ut"};
		String[] skin_labels = new String[skin_values.length];
		for (i=0; i<skin_labels.length; i++) {
			skin_labels[i] = lu.getLocalisedMessageText(
					"statusicon.skintype." + skin_values[i]);
		}
		
		final Parameter skin_external_type = model.addStringListParameter2("skintype", "statusicon.config.skintype", skin_values, skin_labels, "ut");
		
		Parameter update_param = model.addActionParameter2("statusicon.config.install_skin_bar", "statusicon.config.install_skin_bar.action");
		model.createGroup("statusicon.config.group.main", new Parameter[] {skin_source_type, skin_bundled_file, skin_external_file, skin_external_type, update_param});
		
		// Settings for the uTorrent skin type.
		Parameter[] ut_group_params = new Parameter[1];
		ut_group_params[0] = model.addBooleanParameter2("skintype.ut.use_finished_icon", "statusicon.config.skintype.ut.use_finished_icon", true);
		model.createGroup("statusicon.config.group.ut", ut_group_params);
		
		// This button will create the skinbar (and remove any existing one).
		update_param.addListener(
			new ParameterListener() {
				public void parameterChanged(Parameter p) {
					installSkinBar();
				}
			}
		);
		
		// Web links.
		Parameter link1 = model.addHyperlinkParameter2("statusicon.webpage.main.title", lu.getLocalisedMessageText("statusicon.webpage.main.location"));
		Parameter link2 = model.addHyperlinkParameter2("statusicon.webpage.screens.title", lu.getLocalisedMessageText("statusicon.webpage.screens.location"));
		model.createGroup("statusicon.config.group.webpage", new Parameter[] {link1, link2});
		
		// Thanks section.
		ArrayList thanks_params = new ArrayList();
		for (i=1; i<=3; i++) {
			String thank_link = lu.getLocalisedMessageText("statusicon.thanks." + i + ".link");
			if (thank_link == null || thank_link.trim().length() == 0) {
				thanks_params.add(model.addLabelParameter2("statusicon.thanks." + i));
			}
			else {
				thanks_params.add(model.addHyperlinkParameter2("statusicon.thanks." + i, thank_link));
			}
		}
		thanks_params.add(model.addLabelParameter2("statusicon.thanks.translation"));
		thanks_params.add(model.addLabelParameter2("statusicon.thanks.translation.text"));
		model.createGroup("statusicon.config.group.thanks", (Parameter[])thanks_params.toArray(new Parameter[0]));
		
		
		ParameterListener skin_source_type_listener = new ParameterListener() {
			public void parameterChanged(Parameter p) {
				String setting = ((StringListParameter)p).getValue();
				boolean file_enabled = setting.equals("file");
				boolean bundled_enabled = setting.equals("bundled");
				skin_bundled_file.setEnabled(bundled_enabled);
				skin_external_file.setEnabled(file_enabled);
				skin_external_type.setEnabled(file_enabled);
			}
		};
		skin_source_type.addListener(skin_source_type_listener);
		skin_source_type_listener.parameterChanged(skin_source_type); // Sorts out the enabled status of the params.
		
		plugin_interface.getUIManager().addUIListener(this);
	}
	
	public void UIAttached(UIInstance ui) {
		if (!(ui instanceof UISWTInstance)) {return;}
		this.swt_ui = (UISWTInstance)ui;
		installSkinBar();
	}
	
	public void UIDetached(UIInstance ui) {}
	
    public void positionChanged(Download download, int old_pos, int new_pos) {}
    
    /**
     * This will force the value of this cell for this download to be recalculated.
     */
    public void stateChanged(Download download, int old_state, int new_state) {
        if (this.columns == null) return;
        for (int i=0; i<columns.length; i++) {
            this.columns[i].invalidateCell(download);
        }
    }
    
    /**
     * This will recalculate what value a cell should hold.
     */
    public void refresh(TableCell cell) {

        // Set tooltip.
        Download download = (Download)cell.getDataSource();
        cell.setToolTip(download.getStats().getStatus(true));
        
        // Set status icon, if we can.
        if (this.skinbar == null) return;
        Graphic graphic = this.skinbar.getStatusIcon(download);
        if (graphic == null) return;
        cell.setGraphic(graphic);
        cell.setMarginHeight(0);
        
        cell.setSortValue(this.skinbar.getSortValue(download));
    }
    
    private InputStream getSkinBarInputStream() throws IOException {
    	String skin_source_type = this.config.getPluginStringParameter("skin_source_type");
    	if (skin_source_type.equals("file")) {
    		String skinfile = this.config.getPluginStringParameter("skinfile");
    		String skintype = this.config.getPluginStringParameter("skintype");
    	
    		// No skin file means that we don't do anything.
    		if (skinfile.trim().equals("")) {
    			this.channel.logAlertRepeatable(LoggerChannel.LT_INFORMATION, lu.getLocalisedMessageText("statusicon.alert.no_skin_file"));
    			return null;
    		}
    	
    		// We only support the uTorrent skin file type at the moment.
    		// We don't expect this to happen.
    		if (!skintype.equals("ut")) {
    			return null;
    		}
    		
    		return new BufferedInputStream(new FileInputStream(skinfile));
    	}
    	
    	else if (skin_source_type.equals("bundled")) {
    		String skinfile = this.config.getPluginStringParameter("skin_bundled_file");

    		// Figure out what skin definition the associated file links to.
    		int i = 1;
    		while (true) {
    			String skinfilename = bundled_skins.getProperty("skin" + i + ".file");
    			if (skinfilename == null) {
    				// Not found, so you can't find the skin.
    				return null;
    			}
    			if (skinfilename.equals(skinfile)) {break;}
    			i++;
    		}
    		
    		// What type is it?
    		String skintype = bundled_skins.getProperty("skin" + i + ".type");

    		// We only support the uTorrent skin file type at the moment.
    		// We don't expect this to happen.
    		if (!skintype.equals("ut")) {
    			return null;
    		}
    		
    		// Construct a stream to the resource.
    		return getInternalResource("icons/" + skintype + "/" + skinfile);
    	}
    	
    	// We don't support any other setting.
    	return null;
    }
    
    private void installSkinBar() {

    	// We shouldn't be called if we don't have a reference to the UI, but just in case...
    	if (this.swt_ui == null) {return;}
    	
    	// External file source.
    	InputStream instream = null;
    	try {instream = getSkinBarInputStream();}
		catch (Exception e) {
			channel.logAlertRepeatable(lu.getLocalisedMessageText("statusicon.alert.skin_install_error"), e);
			return;
		}
		
		if (instream == null) {return;}
    	
    	boolean use_finished_icon = this.config.getPluginBooleanParameter("skintype.ut.use_finished_icon");
    	final SkinBar new_skinbar = new uTorrentSkinBar(instream, use_finished_icon);

    	// Try and construct the skinbar.
    	this.swt_ui.getDisplay().asyncExec(new Runnable() {
    		public void run() {
    			try {new_skinbar.initialise(swt_ui);}
    			catch (Exception e) {
    				channel.logAlertRepeatable(lu.getLocalisedMessageText("statusicon.alert.skin_install_error"), e);
    				return;
    			}
    			
    			SkinBar old_skinbar = StatusIconPlugin.this.skinbar;

    			// Start using the skinbar, invalidate all cells to force recalculation..
    			StatusIconPlugin.this.skinbar = new_skinbar;
    			for (int i=0; i<columns.length; i++) {
    				columns[i].invalidateCells();
    			}

    			// ... and dispose of the old skinbar (if there was one).
    			if (old_skinbar != null) old_skinbar.dispose();
    		}
    	});
    }
    
    interface SkinBar {
    	public Graphic getStatusIcon(Download download);
    	public void initialise(UISWTInstance instance) throws Exception;
    	public void dispose();
    	public int getSortValue(Download download);
    }
    
    private class uTorrentSkinBar implements SkinBar {
    	
    	private InputStream instream;
    	private boolean use_finished_icon;
    	private Graphic[] status_icons;
    	
    	public uTorrentSkinBar(InputStream instream, boolean use_finished_icon) {
    		if (instream == null) {throw new NullPointerException("instream cannot be null");}
    		this.instream = instream;
    		this.use_finished_icon = use_finished_icon;
    		this.status_icons = new Graphic[10];
    	}

    	/**
    	 * This should be called on the SWT thread.
    	 */
    	public void initialise(UISWTInstance swt_ui) throws Exception {
    		Image main_image = new Image(swt_ui.getDisplay(), this.instream);
    		ImageData main_image_data = main_image.getImageData();
    		
    		byte[] buf = new byte[16];
    		int[] pbuf = new int[16];
    		
    		Image sub_image = null;
    		for (int i=0; i<10; i++) {
    			ImageData sub_image_data = main_image_data.scaledTo(16, 16);
        		for (int y=0; y<16; y++) {
        			main_image_data.getPixels(i * 16, y, 16, pbuf, 0);
        			sub_image_data.setPixels(0, y, 16, pbuf, 0);
        			main_image_data.getAlphas(i * 16, y, 16, buf, 0);
        			sub_image_data.setAlphas(0, y, 16, buf, 0);
        		}
        		
        		/**
        		 * If the image is not transparent (according to SWT), but the image is
        		 * a 32-bit bitmap, then the alpha channel is probably not being picked
        		 * up, so we've got to create it manually.
        		 */
        		if (main_image_data.depth == 32 && main_image_data.getTransparencyType() == SWT.TRANSPARENCY_NONE) {
        			sub_image_data.alphaData = new byte[256];
        			for (int x=0; x<16; x++) {
        				for (int y=0; y<16; y++) {
        					sub_image_data.setAlpha(x, y, sub_image_data.getPixel(x, y) & 0x000000FF);
        				}
        			}
        		}
        		
        		/**
        		 * I've seen non 32-bit bitmaps with no transparency information, where purple
        		 * is the transparency colour.
        		 */
        		else if (main_image_data.depth < 32 && main_image_data.getTransparencyType() == SWT.TRANSPARENCY_NONE) {
        			sub_image_data.transparentPixel = main_image_data.palette.getPixel(new RGB(255, 0, 255));
        		}

        		sub_image = new Image(swt_ui.getDisplay(), sub_image_data);
    		    this.status_icons[i] = swt_ui.createGraphic(sub_image);
    		}
    		main_image.dispose();
    		instream.close();
    	}
    	
    	public Graphic getStatusIcon(Download download) {
    		int state = download.getState();
    	    if (state == Download.ST_STOPPING)
    	        state = download.getSubState();
    	    
    		if (state == Download.ST_DOWNLOADING)
    	        return this.status_icons[0]; // Downloading
    	    
    	    if (state == Download.ST_SEEDING)
    	    	return this.status_icons[1]; // Seeding

    	    if (state == Download.ST_ERROR) {
    	        return this.status_icons[6]; // Error
    	    }
    	    
    	    if (state == Download.ST_STOPPED) {
    	        if (download.isPaused()) {
    	            return this.status_icons[3]; // Paused
    	        }
    	        else if (this.use_finished_icon && download.isComplete(false)) {
    	        	return this.status_icons[7]; // Finished
    	        }
    	        else {
    	            return this.status_icons[2]; // Stopped
    	        }
    	    }
    	    
    	    if (state == Download.ST_QUEUED) {
    	        if (download.isComplete(false)) {
    	            return this.status_icons[9]; // Seeding queued.
    	        }
    	        else {
    	            return this.status_icons[8]; // Download queued.
    	        }
    	    }
    	    
    	    // We don't know how to handle anything else.
    	    return null;
    	}
    	
    	public int getSortValue(Download download) {
    		int state = download.getState();
    	    if (state == Download.ST_STOPPING)
    	        state = download.getSubState();
    	    return state;
    	}

    	
    	/**
    	 * This should be called on the SWT thread.
    	 */
    	public void dispose() {
    		UISWTGraphic graphic = null;
    		Image img = null;
    		for (int i=0; i<10; i++) {
    			graphic = (UISWTGraphic)this.status_icons[i];
    			if (graphic != null) {
    				img = graphic.getImage();
    				if (img != null && !img.isDisposed()) {
    					img.dispose();
    				}
    			}
    		}
    	}
    	
    }
    
    private InputStream getInternalResource(String path) {
    	path = "com/aelitis/azureus/plugins/statusicon/" + path;
    	InputStream in = this.plugin_interface.getPluginClassLoader().getResourceAsStream(path);
    	if (in != null) {return in;}
    	return getClass().getClassLoader().getResourceAsStream(path);
    }
	
}