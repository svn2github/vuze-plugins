package com.aelitis.azureus.plugins.torrentnameutils;

import java.io.File;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentFile;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;

public class TorrentNameUtilsPlugin implements Plugin {
	
	private TorrentAttribute display_name_attr;
	private TorrentAttribute comment_attr;
	
	public void initialize(PluginInterface pi) {
		TableManager tm = pi.getUIManager().getTableManager();
		TorrentManager tor = pi.getTorrentManager();
		
		display_name_attr = tor.getAttribute(TorrentAttribute.TA_DISPLAY_NAME);
		comment_attr = tor.getAttribute(TorrentAttribute.TA_USER_COMMENT);
		
		String [] tables = new String[] {TableManager.TABLE_MYTORRENTS_INCOMPLETE, TableManager.TABLE_MYTORRENTS_COMPLETE};
		for (int i=0; i<tables.length; i++) {
			final TableContextMenuItem root = tm.addContextMenuItem(tables[i], "torrentnameutils.menu");
			root.setStyle(TableContextMenuItem.STYLE_MENU);
			
			// Place for the torrent name to be displayed.
			final TableContextMenuItem tname = tm.addContextMenuItem(root, "torrentnameutils.menu.torrentname");
			tname.setStyle(TableContextMenuItem.STYLE_PUSH);
			tname.setEnabled(false); // Always greyed out.
			
			final TableContextMenuItem tname_sep = tm.addContextMenuItem(root, "torrentnameutils.menu.separator");
			tname_sep.setStyle(TableContextMenuItem.STYLE_SEPARATOR);
			
			final TableContextMenuItem rename_d = tm.addContextMenuItem(root, "MyTorrentsView.menu.rename.displayed");
			rename_d.setStyle(TableContextMenuItem.STYLE_PUSH);
			
			final TableContextMenuItem rename_s = tm.addContextMenuItem(root, "MyTorrentsView.menu.rename.save_path");
			rename_s.setStyle(TableContextMenuItem.STYLE_PUSH);
			
			final TableContextMenuItem rename_ds = tm.addContextMenuItem(root, "MyTorrentsView.menu.rename.displayed_and_save_path");
			rename_ds.setStyle(TableContextMenuItem.STYLE_PUSH);
			
			tm.addContextMenuItem(root, "torrentnameutils.menu.separator").setStyle(TableContextMenuItem.STYLE_SEPARATOR);
			
			final TableContextMenuItem set_comment = tm.addContextMenuItem(root, "MyTorrentsView.menu.edit_comment");
			set_comment.setStyle(TableContextMenuItem.STYLE_PUSH);
			
			root.addFillListener(new MenuItemFillListener() {
				public void menuWillBeShown(MenuItem mi, Object data) {
					TableRow[] rows = (TableRow[])data;
					boolean enabled = rows.length > 0;
					root.setEnabled(enabled);
					if (!enabled) {return;}
					
					tname.setVisible(rows.length == 1);
					tname_sep.setVisible(rows.length == 1);
					
					if (rows.length == 1) {
						tname.setText(new File(((Download)rows[0].getDataSource()).getTorrentFileName()).getName());
					}
					
					// We can only rename a torrent if it is stopped.
					rename_s.setEnabled(true);
					rename_ds.setEnabled(true);
					for (int i=0; i<rows.length; i++) {
						Download dl = (Download)rows[i].getDataSource();
						if (!dl.canMoveDataFiles()) {
							rename_s.setEnabled(false);
							rename_ds.setEnabled(false);
							break;
						}
					}
				}});
			
			MenuItemListener item_listener = new MenuItemListener() {
				public void selected(MenuItem mi, Object o) {
					Download download = (Download)((TableRow)o).getDataSource();
					String t_name = new File(download.getTorrentFileName()).getName();
					if (t_name.endsWith(".torrent")) {t_name = t_name.substring(0, t_name.length() - 8);}
					
					// Change displayed name.
					if (mi == rename_d || mi == rename_ds) {
						download.setAttribute(display_name_attr, t_name);
					}
					
					// Change comment.
					if (mi == set_comment) {
						download.setAttribute(comment_attr, t_name);
					}
					
					// Rename download - but be aware doing a rename which will
					// end up removing a file extension.
					if (mi == rename_s || mi == rename_ds) {
						if (t_name.indexOf('.') == -1) { // No file extension
							TorrentFile [] tfiles = download.getTorrent().getFiles();
							if (tfiles.length == 1) { // This is a simple torrent...
								int ext_pos = tfiles[0].getName().lastIndexOf('.');
								if (ext_pos != -1) { // And there appears to be an extension there... 
									// Modify the value to incorporate an extension.
									t_name += tfiles[0].getName().substring(ext_pos);
								}
							}
						}
						try {download.renameDownload(t_name);}
						catch (DownloadException e) {
							throw new RuntimeException(e);
						}
					}
				}
			};
			
			rename_d.addListener(item_listener);
			rename_s.addListener(item_listener);
			rename_ds.addListener(item_listener);
			set_comment.addListener(item_listener);	
			
			
		} // end for
	}

}
