/** ColumnExample1.java
 * Created on May 15, 2004
 *
 * Free.  Use however you see fit.
 */
package AzureusPluginExamples;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.utils.Formatters;

/** Simple Column Example
 *
 * @author TuxPaper
 */
public class ColumnExample1 implements Plugin {
  static final String COLUMNID_TORRENTDATE = "SampleColumn1";
  static final String COLUMNID_PERCENTREMAINING = "SampleColumn2";
  Formatters formatter;

	/** This method is called when the plugin is loaded / initialized
   *
   * @param pluginInterface access to Azureus' plugin interface
   */
	public void initialize(PluginInterface pluginInterface) {
    // Get the Formatters object to be used when setting the cell's text
    formatter = pluginInterface.getUtilities().getFormatters();
    // Of course, we'll need the TableManager object in order to add a column!
    TableManager tableManager = pluginInterface.getUIManager().getTableManager();
    TableColumn sampleColumn;
    
    /* Part 1: Create the Table Column in the "My Torrents", Complete Torrents 
     * section, with a ID of COLUMNID_TORRENTDATE */
    sampleColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, 
                                             COLUMNID_TORRENTDATE);
    
    // Initialize column using one function
    sampleColumn.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 150);
    /* The above initialize call could be replaced with:
     *
    sampleColumn.setAlignment(TableColumn.ALIGN_LEAD);
    sampleColumn.setPosition(TableColumn.POSITION_LAST);
    sampleColumn.setWidth(100);
     */
    
    // We need to be notified of cell refreshes so we can set the text
    CreationDateRefreshListener creationDateRefreshListener = new CreationDateRefreshListener();
    sampleColumn.addCellRefreshListener(creationDateRefreshListener);
    /* Alternatively, we could have done this in one line:
     *
    sampleColumn.addCellRefreshListener(new CreationDateRefreshListener());
     */
    
    // Now that the column is set up, add it to the TableManager
    tableManager.addColumn(sampleColumn);
    /* We are done Part 1! */
    

    /* Part 2: Add another column, this time to the Incomplete "My Torrents"
     * table, and have it act exactly the same */
    sampleColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, 
                                             COLUMNID_TORRENTDATE);
    sampleColumn.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 150);
    // Note: The same listener is being used!
    sampleColumn.addCellRefreshListener(creationDateRefreshListener);
    tableManager.addColumn(sampleColumn);
    /** Done Part 2! */

    /* Part 3: Add a column to the peer table that needs constant refreshing
     */
    sampleColumn = tableManager.createColumn(TableManager.TABLE_TORRENT_PEERS,
                                             COLUMNID_PERCENTREMAINING);
    sampleColumn.initialize(TableColumn.ALIGN_TRAIL,
                                 TableColumn.POSITION_LAST, 150, 
                                 TableColumn.INTERVAL_LIVE);
    sampleColumn.addCellRefreshListener(new PercentRemainingRefreshListener());
    tableManager.addColumn(sampleColumn);
    /** Done Part 3! */
	}
  
  /** Handle our Creation Date cells refreshes.
   *
   * Normally, this class would be in a file of it's own 
   * (CellRefreshListener.java).  However, for the sake keeping this example
   * easy to follow, it's been added as a class inside ColumnExmple1
   */
  public class CreationDateRefreshListener implements TableCellRefreshListener {
    /** Refresh gets called whenever a cell needs refreshing.  This happens
     * at least once for each row in the table.
     */
    public void refresh(TableCell cell) {
      Download download = (Download)cell.getDataSource();
      if (download == null)
        return;
      long torrentDate = download.getTorrent().getCreationDate();
      /* You don't have to set the sort value if your text is the order you
       * want users to sort on.  In this case, however, the text is a date and
       * it's better to sort date by it's value and not it's text (otherwise
       * April would come before March, etc) */
      cell.setSortValue(torrentDate);

      cell.setText(formatter.formatDate(torrentDate * 1000));
      /** If we wanted to have different settings for Incomplete and Complete
       * tables, and dodn't want to create a seperate TableCellRefreshListener
       * class for each, we can check the cell.getTableID(), such as: */
      if (cell.getTableID().equals(TableManager.TABLE_MYTORRENTS_COMPLETE))
        cell.setForeground(127, 0, 0);
      else
        cell.setForeground(0, 0, 127);
    }
  }

  public class PercentRemainingRefreshListener implements TableCellRefreshListener {
    /** Since we set the column to be REFRESH_LIVE, a refresh will be triggered
     * every x milliseconds, where x is set by the user ("Update GUI Every")
     */
    public void refresh(TableCell cell) {
      Peer peer = (Peer)cell.getDataSource();
      if (peer == null)
        return;
      int iPercentRemaining = 1000 - peer.getPercentDone();

      /* For this cell, the text is based solely on the sort value
       * So, if the sort value was already set to our newly calculated value,
       * and the cell is valid, then we can exit without setting the text.
       * 
       * For sort that is not directly represending the text, these 2 lines 
       * should be removed.  An example would be if you wanted to prefix the 
       * text with a star if the peer was downloading from you.
       */
      if (!cell.setSortValue(iPercentRemaining) && cell.isValid())
        return;

      cell.setText(formatter.formatPercentFromThousands(iPercentRemaining));
    }
  }
}
