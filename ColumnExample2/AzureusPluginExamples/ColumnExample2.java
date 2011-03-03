/** ColumnExample2.java
 * Created on 2004/May/24
 *
 * Free.  Use however you see fit.
 */
package AzureusPluginExamples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.SWT.GraphicSWT;
import org.gudy.azureus2.plugins.ui.SWT.SWTManager;
import org.gudy.azureus2.plugins.ui.tables.*;


/** Graphic Column Example
 *
 * @author TuxPaper
 */
public class ColumnExample2 implements Plugin {
  static final String COLUMNID_GRAPHIC = "GraphicSampleColumn";
  static final int MARKER_TO_TEXT_PADDING = 3;
  static SWTManager swtManager;
  static PluginConfig pluginConfig;
  
	/** This method is called when the plugin is loaded / initialized
   *
   * @param pluginInterface access to Azureus' plugin interface
   */
	public void initialize(PluginInterface pluginInterface) {
	  swtManager = pluginInterface.getUIManager().getSWTManager();
    pluginConfig = pluginInterface.getPluginconfig();

    TableManager tableManager = pluginInterface.getUIManager().getTableManager();
    TableColumn sampleColumn;
    
    sampleColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_COMPLETE, 
                                             COLUMNID_GRAPHIC);
  
    
    // Initialize column using one function
    sampleColumn.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 
                            80, TableColumn.INTERVAL_GRAPHIC);
    sampleColumn.setType(TableColumn.TYPE_GRAPHIC);
    
    // We need to be notified of cell refreshes so we can set the text
    SampleGraphicCellListener sampleGraphicCellListener = new SampleGraphicCellListener();
    sampleColumn.addCellAddedListener(sampleGraphicCellListener);
    sampleColumn.addCellRefreshListener(sampleGraphicCellListener);
    sampleColumn.addCellDisposeListener(sampleGraphicCellListener);
    
    // Now that the column is set up, add it to the TableManager
    tableManager.addColumn(sampleColumn);
    
    /** Do the same for incomplete */
    sampleColumn = tableManager.createColumn(TableManager.TABLE_MYTORRENTS_INCOMPLETE, 
                                             COLUMNID_GRAPHIC);
  
    
    // Initialize column using one function
    sampleColumn.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 
                            80, TableColumn.INTERVAL_GRAPHIC);
    sampleColumn.setType(TableColumn.TYPE_GRAPHIC);

    // We need to be notified of cell refreshes so we can set the text
    sampleColumn.addCellAddedListener(sampleGraphicCellListener);
    sampleColumn.addCellRefreshListener(sampleGraphicCellListener);
    sampleColumn.addCellDisposeListener(sampleGraphicCellListener);
    
    // Now that the column is set up, add it to the TableManager
    tableManager.addColumn(sampleColumn);
	}
  
  /** Handle our cell's refreshes.
   *
   * Normally, this class would be in a file of it's own 
   * (SampleGraphicCellListener.java).  However, for the sake keeping this
   * example easy to follow, it's been added as a class inside the main one
   */
  public class SampleGraphicCellListener 
         implements TableCellRefreshListener,
                    TableCellDisposeListener,
                    TableCellAddedListener
  {
    /** Set each cell to fill whole cell area */
    public void cellAdded(TableCell cell) {
      cell.setFillCell(true);
    }

    /** Refresh gets called whenever a cell needs refreshing.  This happens
     * at least once for each row in the table.
     */
    public void refresh(TableCell cell) {
      Download download = (Download)cell.getDataSource();
      if (download == null)
        return;
      
      long ratio = download.getStats().getShareRatio();
      if (!cell.setSortValue(ratio) && cell.isValid())
        return;

      // return now and the image will never wil be drawn 
      // (Ratio never changes to -1 so we don't have to worry about blanking the old image)
      if (ratio == -1)
        return;

      int iWidth = cell.getWidth();
      int iHeight = cell.getHeight();
      
      // getWidth and getHeight can return 0 or negatives.  Safeguard against that
      // as well as width or heights less than 3 (or we can't even draw a border)
      if (iWidth <= 2 || iHeight <= 2)
        return;

      // Create a new Image.  We could just get the existing image and draw on
      // it, but that causes flickering
      Image img = new Image(swtManager.getDisplay(), iWidth, iHeight);
      GC gc = new GC(img);

      // Clear the image and draw a border
      gc.setForeground(swtManager.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
      gc.setBackground(swtManager.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
      gc.fillRectangle(1, 1,  iWidth - 2, iHeight - 2);
      gc.drawRectangle(0, 0, iWidth - 1, iHeight - 1);

      int iMarkerPosition;
      int x2 = iWidth - 3;

      // Draw some reference lines as a dotted line
      gc.setLineStyle(SWT.LINE_DOT);

      // Draw a dotted gray line at every 1.0 ratio
      int x = 2;
      int iOldMarkerPosition;
      iMarkerPosition = calculateXPos(1000, x2);
      gc.setForeground(swtManager.getDisplay().getSystemColor(SWT.COLOR_GRAY));
      do {
        gc.drawLine(iMarkerPosition + 1, 1, iMarkerPosition + 1, iHeight - 2);

        iOldMarkerPosition = iMarkerPosition;
        iMarkerPosition = calculateXPos(x++ * 1000, x2);
      } while (iMarkerPosition - iOldMarkerPosition > 1);
      
      // Draw a dotted red line at FP Share Ratio
      int iFPShareRatio = pluginConfig.getIntParameter("StartStopManager_iFirstPriority_ShareRatio");
      gc.setForeground(swtManager.getDisplay().getSystemColor(SWT.COLOR_RED));
      iMarkerPosition = calculateXPos(iFPShareRatio, x2);
      gc.drawLine(iMarkerPosition + 1, 1, iMarkerPosition + 1, iHeight - 2);

      // Draw a dotted blue line at Ignore Share Ratio (or 2.0)
      int iIgnoreShareRatio = (int)(1000 * pluginConfig.getFloatParameter("Stop Ratio"));
      if (iIgnoreShareRatio == 0)
        iIgnoreShareRatio = (iFPShareRatio > 2000) ? iFPShareRatio * 2 : 2000;
      else {
        gc.setForeground(swtManager.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        iMarkerPosition = calculateXPos(iIgnoreShareRatio, x2);
        gc.drawLine(iMarkerPosition + 1, 1, iMarkerPosition + 1, iHeight - 2);
      }

      // Calculate color to use for our ratio line
      int color;
      iMarkerPosition = calculateXPos(ratio, x2);
      if (ratio < iFPShareRatio)
        color = SWT.COLOR_RED;
      else if (ratio < iIgnoreShareRatio)
        color = SWT.COLOR_LIST_FOREGROUND;
      else
        color = SWT.COLOR_BLUE;

      // Draw the ratio line!
      gc.setForeground(swtManager.getDisplay().getSystemColor(color));
      gc.setLineStyle(SWT.LINE_SOLID);
      gc.drawLine(iMarkerPosition + 1, 2, iMarkerPosition + 1, iHeight - 3);
      
      // Draw ratio text
      String sRatio = String.valueOf(ratio / 1000.0);
      Point pt = gc.stringExtent(sRatio);
      if (iMarkerPosition + 6 + pt.x >= iWidth)
        iMarkerPosition -= pt.x + MARKER_TO_TEXT_PADDING;
      else
        iMarkerPosition += MARKER_TO_TEXT_PADDING;
      int y = (iHeight / 2 - pt.y / 2);
      if (y > 0) {
        gc.drawString(sRatio, iMarkerPosition + 1, y, true);
      }
      
      /****** Always dispose of your GC!! ******/
      gc.dispose();

      // Tell Azureus that we have a new graphic

      // First, check to see if there is one
      Graphic g = cell.getGraphic();
      GraphicSWT graphicSWT;

      if (g == null || !(g instanceof GraphicSWT)) {
        // There isn't one, so make a new one!
        graphicSWT = swtManager.createGraphic(img);
      } else {
        // There's one, so we have to dispose of our last SWT Image
        // (Azureus DOES NOT dispose of it for us, in case we are using it
        //  somewhere else)
        graphicSWT = (GraphicSWT)g;
        Image oldImage = graphicSWT.getImage();
        graphicSWT.setImage(img);
        if (oldImage != null && !oldImage.isDisposed())
          oldImage.dispose();
      }

      // Set the graphic.  Even if graphicSWT is the same, we need to call 
      // getGraphic to tell Azureus to redraw it.
      cell.setGraphic(graphicSWT);
    }
    
    /** Always remember to dispose of what you've created */
    public void dispose(TableCell cell) {
      Graphic g = cell.getGraphic();
      GraphicSWT graphicSWT;

      if (g != null && g instanceof GraphicSWT) {
        graphicSWT = (GraphicSWT)g;
        Image oldImage = graphicSWT.getImage();
        if (oldImage != null && !oldImage.isDisposed())
          oldImage.dispose();
      }
    }
    
    /** Calculate where the line should go based on the ratio and the maximum 
     * position
     **/
    private int calculateXPos(long ratio, int x2) {
      if (ratio == 0)
        return 0;

      if (ratio <= 1000)
        return (int)(x2 * ratio / 1000 / 2);

      int half = x2 / 2;
      return (int)(Math.exp(-800.0/(ratio - 1000)) * half) + half;
    }
    
  }
}

/** Notes:
 * If we had a PluginConfigListener, we could listen to the two share ratio
 * parameters.  When changed we would be able to fire off a
 *
 * sampleColumn.invalidateCells();
 *
 * which would force an update on all the column's cells, thus changing the 
 * dotted vertical lines.
 */