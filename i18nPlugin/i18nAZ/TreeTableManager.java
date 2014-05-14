/*
 * TreeTableManager.java
 *
 * Created on March 21, 2014, 4:27 PM
 */

package i18nAZ;

import i18nAZ.FilterManager.PrebuildItem;
import i18nAZ.FilterManager.PrebuildItemCollection;
import i18nAZ.FilterManager.State;
import i18nAZ.TargetLocaleManager.TargetLocale;
import i18nAZ.View.MenuOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.custom.TreeCursor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.TypedListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;


/**
 * TreeTableManager.java
 * 
 * @author Repris d'injustice
 */
class TreeTableManager
{
    static final String DATAKEY_PREBUILD_ITEM = "prebuildItem";
    static final String DATAKEY_ITEM = "item";
    static final String DATAKEY_COLUMN_INDEX = "columnIndex";

    static final Color COLOR_ROW_DEFAULT_BACKGROUND = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

    static Color COLOR_ROW_NODE_BACKGROUND = null;
    static Color COLOR_COLUMN_DEFAULT_BACKGROUND = null;
    static Color COLOR_COLUMN_NODE_BACKGROUND = null;
    static Color COLOR_ROW_SELECTED_RECTANGLE_FOREGROUND = null;
    static Color COLOR_ROW_HOT_RECTANGLE_FOREGROUND = null;
    static Color COLOR_ROW_SELECTED_BACKGROUND = null;
    static Color COLOR_ROW_HOT_SELECTED_BACKGROUND = null;
    static Color COLOR_ROW_HOT_BACKGROUND = null;
    static final Color COLOR_CELL_BACKGROUND = new Color(Display.getCurrent(), 174, 178, 181);

    static final Color COLOR_ROW_SELECTED_FOREGROUND = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
    static final Color COLOR_CELL_FOREGROUND = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

    static final int MODE_TABLE = -1;
    static final int MODE_TREE = 1;

    private static int CurrentMode = TreeTableManager.MODE_TABLE;

    private static Composite treeTable;

    private static Canvas cursor;

    private static Composite areaComposite = null;

    private static Map<String, HashSet<String>> expandedkeys = new HashMap<String, HashSet<String>>();
    private static Map<String, String> selectedKeys = new HashMap<String, String>();
    private static Map<String, Integer> relativeTopIndexes = new HashMap<String, Integer>();

    private static Image EmptyImage = null;
    private static Image UnchangedImage = null;
    private static Image ExtraImage = null;

    private static Item focusedRow = null;
    private static int focusedColumn = 0;

    private static int oldColumn = -1;
    private static String oldLangFileId = "";
    private static ItemListener itemListener = new ItemListener();
    private static int selectedColumnsIndex = -1;

    private static class ItemListener implements Listener
    {
        private Item itemHot = null;

        private int getTotalWidth()
        {
            int width = 0;
            for (int i = 0; i < TreeTableManager.getColumnCount(); i++)
            {
                switch (TreeTableManager.CurrentMode)
                {
                    case TreeTableManager.MODE_TABLE:
                        width += ((TableColumn) TreeTableManager.getColumn(i)).getWidth();
                        break;
                    default:
                        width += ((TreeColumn) TreeTableManager.getColumn(i)).getWidth();
                        break;
                }

            }
            return width;
        }

        private String getText(GC gc, Item item, int column)
        {
            int width = ((TableColumn) TreeTableManager.getColumn(column)).getWidth() - 12;
            String text = TreeTableManager.getText(item, column);
            text = (text == null) ? "" : text;
            if (gc.textExtent(text).x > width)
            {
                int ellipseWidth = gc.textExtent("...").x;
                int length = text.length();
                while (length > 0)
                {
                    length--;
                    text = text.substring(0, length);
                    if (gc.textExtent(text).x + ellipseWidth <= width)
                    {
                        text = text + "...";
                        break;
                    }
                }
            }
            return text;
        }

        
        public void handleEvent(Event e)
        {
            if (((Composite) e.widget).getVisible() == false)
            {
                e.doit = false;
                return;
            }
            switch (e.type)
            {
                case SWT.EraseItem:

                    // INIT
                    Rectangle originalClipping = e.gc.getClipping();
                    e.detail &= ~SWT.FOCUSED;
                    
                    // FOREGROUND
                    if (e.index == 0 && TreeTableManager.CurrentMode == TreeTableManager.MODE_TABLE)
                    {
                        e.detail &= ~SWT.FOREGROUND;
                    }

                    // NO HOT NO SELECTED
                    if (this.itemHot != null && this.itemHot.equals(e.item) == true && (e.detail & SWT.HOT) == 0 && e.index > 0)
                    {
                        e.detail |= SWT.HOT;
                    }
                    if ((e.detail & SWT.HOT) == 0 && e.index == 0)
                    {
                        this.itemHot = null;
                    }
                    if ((e.detail & SWT.HOT) == 0 && (e.detail & SWT.SELECTED) == 0 && TreeTableManager.CurrentMode == TreeTableManager.MODE_TABLE)
                    {
                        e.gc.fillRectangle(e.x, e.y, e.width, e.height);
                    }

                    // HOT
                    if ((e.detail & SWT.HOT) != 0)
                    {
                        e.detail &= ~SWT.BACKGROUND;
                        if (e.index == 0)
                        {
                            this.itemHot = (Item) e.item;
                        }
                        e.gc.setBackground(TreeTableManager.COLOR_ROW_HOT_BACKGROUND);
                        Rectangle rowClipping = e.gc.getClipping();
                        rowClipping.width = this.getTotalWidth() - e.x;
                        e.gc.setClipping(rowClipping);
                        e.gc.fillRectangle(e.x, e.y, rowClipping.width, e.height);
                        Color foreground = e.gc.getForeground();
                        e.gc.setForeground(TreeTableManager.COLOR_ROW_HOT_RECTANGLE_FOREGROUND);
                        e.gc.drawRectangle(0, e.y, this.getTotalWidth() - 1, e.height - 1);
                        e.gc.setForeground(foreground);
                    }

                    // SELECTED
                    if ((e.detail & SWT.SELECTED) != 0)
                    {
                        e.detail &= ~SWT.SELECTED;
                        e.detail &= ~SWT.BACKGROUND;
                        Rectangle rowClipping = e.gc.getClipping();
                        rowClipping.width = this.getTotalWidth() - e.x;
                        e.gc.setClipping(rowClipping);
                        if ((e.detail & SWT.HOT) != 0)
                        {
                            e.gc.setBackground(TreeTableManager.COLOR_ROW_HOT_SELECTED_BACKGROUND);
                            e.gc.setForeground(TreeTableManager.COLOR_ROW_HOT_RECTANGLE_FOREGROUND);
                        }
                        else
                        {
                            e.gc.setBackground(TreeTableManager.COLOR_ROW_SELECTED_BACKGROUND);
                            e.gc.setForeground(TreeTableManager.COLOR_ROW_SELECTED_RECTANGLE_FOREGROUND);
                        }
                        e.gc.fillRectangle(e.x, e.y, rowClipping.width, e.height);
                        e.gc.drawRectangle(0, e.y, this.getTotalWidth() - 1, e.height - 1);

                        e.gc.setForeground(TreeTableManager.COLOR_ROW_SELECTED_FOREGROUND);
                    }

                    e.gc.setClipping(originalClipping);
                    e.detail &= ~SWT.HOT;

                    break;
                case SWT.MeasureItem:
                    if (e.index == 0 && TreeTableManager.CurrentMode == TreeTableManager.MODE_TABLE)
                    {
                        String text = this.getText(e.gc, (Item) e.item, e.index);
                        Point size = e.gc.textExtent(text);
                        e.width = size.x;
                        e.height = Math.max(e.height, size.y);
                    }

                    break;

                case SWT.PaintItem:

                    if (e.index == 0 && TreeTableManager.CurrentMode == TreeTableManager.MODE_TABLE)
                    {
                        TableItem item = (TableItem) e.item;
                        String text = this.getText(e.gc, item, e.index);
                        Point size = e.gc.textExtent(text);
                        int offset2 = e.index == 0 ? Math.max(0, (e.height - size.y) / 2) : 0;
                        if ((e.detail & SWT.SELECTED) != 0)
                        {
                            e.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
                        }
                        e.gc.drawText(text, e.x + 6, e.y + offset2, true);
                    }
                    break;
            }
        }
    };

    private static class SearchResult
    {
        int index = -1;
        Item item = null;
        boolean success = false;
    }

    static class Cursor
    {
        static void notifyListeners(int eventType, Event e)
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    ((TableCursor) TreeTableManager.cursor).notifyListeners(eventType, e);
                    break;

                default:
                    ((TreeCursor) TreeTableManager.cursor).notifyListeners(eventType, e);
                    break;
            }
        }

        private static void addSelectionListener(SelectionAdapter listener)
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    ((TableCursor) TreeTableManager.cursor).addSelectionListener(listener);
                    break;

                default:
                    ((TreeCursor) TreeTableManager.cursor).addSelectionListener(listener);
                    break;
            }
        }

        static void cancelfocusedRow()
        {
            TreeTableManager.focusedRow = null;
            TreeTableManager.focusedColumn = 0;
        }

        static int getColumn()
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    return ((TableCursor) TreeTableManager.cursor).getColumn();

                default:
                    return ((TreeCursor) TreeTableManager.cursor).getColumn();
            }
        }

        static Item getRow()
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    return ((TableCursor) TreeTableManager.cursor).getRow();

                default:
                    return ((TreeCursor) TreeTableManager.cursor).getRow();
            }
        }

        static boolean isFocusControl()
        {
            return TreeTableManager.cursor.isFocusControl();
        }

        static boolean isSetFocusedRow()
        {
            return TreeTableManager.focusedRow != null;
        }

        private static void onSelect()
        {
            // set undo redo
            if (i18nAZ.viewInstance.undoRedo != null)
            {
                i18nAZ.viewInstance.undoRedo.unset();
            }
            if (TreeTableManager.focusedRow != null)
            {
                TreeTableManager.Cursor.setSelection(TreeTableManager.focusedRow, TreeTableManager.focusedColumn);
                TreeTableManager.setSelection(TreeTableManager.Cursor.getRow());
                i18nAZ.viewInstance.selectEditor();
                return;
            }
            if (TreeTableManager.getCurrent().getVisible() == true && ((TreeTableManager.isTreeMode() == false && TreeTableManager.treeTable instanceof Table) || (TreeTableManager.isTreeMode() == true && TreeTableManager.treeTable instanceof Tree)))
            {
                if (TreeTableManager.getItemCount() > 0)
                {
                    if (TreeTableManager.getColumnCount() > 2)
                    {
                        int targetIndex = TreeTableManager.Cursor.getColumn();

                        while (targetIndex > 2 && ((TargetLocale) TreeTableManager.getColumn(targetIndex).getData(View.DATAKEY_TARGET_LOCALE)).isVisible() == false)
                        {
                            if(TreeTableManager.Cursor.getColumn() < TreeTableManager.oldColumn)
                            {
                                targetIndex--;
                                if (targetIndex < 2)
                                {
                                    targetIndex = 0;
                                    break;
                                }
                            }
                            if(TreeTableManager.Cursor.getColumn() > TreeTableManager.oldColumn)
                            {
                                targetIndex++;
                                if (targetIndex >= TreeTableManager.getColumnCount())
                                {
                                    targetIndex = 0;
                                    break;
                                }
                            }                          
                        }
                        if (targetIndex < 2)
                        {
                            targetIndex = 2;
                        }
                        if(TreeTableManager.Cursor.getColumn() != targetIndex)
                        {
                            TreeTableManager.Cursor.setSelection(TreeTableManager.Cursor.getRow(), targetIndex);
                            return;
                        }                        
                    }
                    Item row = TreeTableManager.Cursor.getRow();
                    boolean exist = false;
                    if (row != null)
                    {
                        TreeTableManager.setSelection(TreeTableManager.Cursor.getRow());
                        PrebuildItem prebuildItem = (PrebuildItem) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                        exist = prebuildItem.isExist();
                    }
                    TreeTableManager.setBackgroundColumn();
                    TreeTableManager.Cursor.setVisible(exist && TreeTableManager.getColumnCount() > 2);
                }
                i18nAZ.viewInstance.updateStyledTexts();
                if (oldColumn != TreeTableManager.Cursor.getColumn())
                {
                    oldColumn = TreeTableManager.Cursor.getColumn();
                    i18nAZ.viewInstance.updateInfoText();
                }
                TreeTableManager.Cursor.setFocus();
            }
        }

        static void setDefaultSelection()
        {
            if(TreeTableManager.getItemCount() == 0 || TreeTableManager.getColumnCount() == 0)
            {
                TreeTableManager.Cursor.onSelect();
                return;
            }
            int selectedRowIndex = -1;
            Item selectedItem = TreeTableManager.getItem(0);
            if (LocalizablePluginManager.getCurrentLangFile() != null && TreeTableManager.selectedKeys.containsKey(LocalizablePluginManager.getCurrentLangFile().getId()) == true && TreeTableManager.selectedKeys.get(LocalizablePluginManager.getCurrentLangFile().getId()).equals("") == false)
            {
                SearchResult searchResult = TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, TreeTableManager.selectedKeys.get(LocalizablePluginManager.getCurrentLangFile().getId()));
                if (searchResult.success == true)
                {
                    selectedItem = searchResult.item;
                    selectedRowIndex = searchResult.index;
                }
            }
            int columnIndex = TreeTableManager.selectedColumnsIndex;
            if(columnIndex >= TreeTableManager.getColumnCount())
            {
                columnIndex = TreeTableManager.getColumnCount() - 1;
            }
            if(columnIndex < 2)
            {
                columnIndex = 2;
            }
            TreeTableManager.Cursor.setSelection(selectedItem,  columnIndex);            
            if (selectedRowIndex != -1)
            {
                SearchResult searchResult = TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, selectedItem);
                selectedRowIndex = searchResult.index;
                if (TreeTableManager.relativeTopIndexes.containsKey(LocalizablePluginManager.getCurrentLangFile().getId()) == true && TreeTableManager.relativeTopIndexes.get(LocalizablePluginManager.getCurrentLangFile().getId()) != -1)
                {
                    switch (TreeTableManager.CurrentMode)
                    {
                        case TreeTableManager.MODE_TABLE:
                            int topIndex = selectedRowIndex - TreeTableManager.relativeTopIndexes.get(LocalizablePluginManager.getCurrentLangFile().getId());
                            ((Table) TreeTableManager.treeTable).setTopIndex(topIndex);
                            break;

                        default:
                            Item topItem = TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, selectedRowIndex - TreeTableManager.relativeTopIndexes.get(LocalizablePluginManager.getCurrentLangFile().getId())).item;
                            if (topItem != null)
                            {
                               ((Tree) TreeTableManager.treeTable).setTopItem((TreeItem) topItem);
                            }
                            break;
                    }
                }
            }            
        }

        static boolean setFocus()
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    ((TableCursor) TreeTableManager.cursor).setFocus();
                    return ((Table) TreeTableManager.treeTable).setFocus();

                default:
                    ((TreeCursor) TreeTableManager.cursor).setFocus();
                    return ((Tree) TreeTableManager.treeTable).setFocus();

            }
        }

        static void setfocusedRow(Item row, int column)
        {
            TreeTableManager.focusedRow = row;
            TreeTableManager.focusedColumn = column;
        }
        static void setColumn(Item column)
        {
            TreeTableManager.Cursor.setColumn(TreeTableManager.indexOf(column));
        }
        private static void setColumn(int columnIndex)
        {
            try
            {
                if (TreeTableManager.Cursor.getRow() != null && TreeTableManager.Cursor.getRow().isDisposed() == false &&columnIndex >= 0 && columnIndex < TreeTableManager.getColumnCount())
                {
                    switch (TreeTableManager.CurrentMode)
                    {
                        case TreeTableManager.MODE_TABLE:
                            ((TableCursor) TreeTableManager.cursor).setSelection(((TableCursor) TreeTableManager.cursor).getRow(), columnIndex);
                            break;

                        default:
                            ((TreeCursor) TreeTableManager.cursor).setSelection(((TreeCursor) TreeTableManager.cursor).getRow(), columnIndex);
                            break;
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
                return;
            }
        }

        static void setSelection(Item row, int columnIndex)
        {
            try
            {
                if (columnIndex >= 0 && columnIndex < TreeTableManager.getColumnCount())
                {
                    switch (TreeTableManager.CurrentMode)
                    {
                        case TreeTableManager.MODE_TABLE:
                            if (row != null && (row.isDisposed() == true || (row.equals(((TableCursor) TreeTableManager.cursor).getRow()) && columnIndex == ((TableCursor) TreeTableManager.cursor).getColumn())))
                            {
                                return;
                            }
                            ((TableCursor) TreeTableManager.cursor).setSelection((TableItem) row, columnIndex);
                            break;

                        default:
                            if (row != null && (row.isDisposed() == true || (row.equals(((TreeCursor) TreeTableManager.cursor).getRow()) && columnIndex == ((TreeCursor) TreeTableManager.cursor).getColumn())))
                            {
                                return;
                            }
                            ((TreeCursor) TreeTableManager.cursor).setSelection((TreeItem) row, columnIndex);
                            break;
                    }
                }
                TreeTableManager.Cursor.onSelect();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
                return;
            }
        }

        private static void setVisible(boolean visible)
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    ((TableCursor) TreeTableManager.cursor).setVisible(visible);
                    break;

                default:
                    ((TreeCursor) TreeTableManager.cursor).setVisible(visible);
                    break;
            }
        }
    }

    static Item addColumn(String text, int width)
    {
        ControlAdapter controlAdapter = new ControlAdapter()
        {
            
            public void controlResized(ControlEvent e)
            {
                TargetLocale targetLocale  =  (TargetLocale) e.widget.getData(View.DATAKEY_TARGET_LOCALE);
                if(targetLocale.isVisible() == false)
                {
                    return;
                }
                int columnIndex = TreeTableManager.indexOf((Item) e.widget);
                int columnWidth = (Integer) Util.invoke(e.widget, "getWidth");
                COConfigurationManager.setParameter("i18nAZ.columnWidth." + columnIndex, columnWidth);
            }
        };

        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                TableColumn tableColumn = new TableColumn((Table) TreeTableManager.treeTable, 16384);
                tableColumn.setText(text);
                tableColumn.setWidth(width);
                tableColumn.addControlListener(controlAdapter);
                return tableColumn;

            default:
                TreeColumn treeColumn = new TreeColumn((Tree) TreeTableManager.treeTable, 16384);
                treeColumn.setText(text);
                treeColumn.setWidth(width);
                treeColumn.addControlListener(controlAdapter);
                return treeColumn;
        }
    }
    static void refreshItem(Item item)
    {
        PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
        for (int i = 0; i < prebuildItem.getValues().length; i++)
        {
            TreeTableManager.setText(item, i, prebuildItem.getValues()[i]);
            TreeTableManager.setState(item, i, prebuildItem.getStates()[i]);
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                break;

            default:
                if (prebuildItem.getChilds() != null)
                {
                    ((TreeItem) item).setBackground(TreeTableManager.COLOR_ROW_NODE_BACKGROUND);
                }
                else
                {
                    ((TreeItem) item).setBackground(TreeTableManager.COLOR_ROW_DEFAULT_BACKGROUND);
                }
                break;
        }
    }
    private static void addItem(Item item, PrebuildItem prebuildItem)
    {
        Util.addTypedListenerAndChildren(item, SWT.MouseEnter, new TypedListener(new MouseTrackAdapter()
        {
            
            public void mouseEnter(MouseEvent e)
            {
                if (e.widget instanceof TableCursor)
                {
                    i18nAZ.viewInstance.itemEnterEventOccurred(((TableCursor) e.widget).getRow(), (Integer) e.data);
                }
                else if (e.widget instanceof TreeCursor)
                {
                    i18nAZ.viewInstance.itemEnterEventOccurred(((TreeCursor) e.widget).getRow(), (Integer) e.data);
                }
                else
                {
                    i18nAZ.viewInstance.itemEnterEventOccurred((Item) e.widget, (Integer) e.data);
                }
            }
        }));

        item.setData(TreeTableManager.DATAKEY_PREBUILD_ITEM, prebuildItem);
        TreeTableManager.refreshItem(item);
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                break;

            default:
                if (prebuildItem.getChilds() != null)
                {
                    TreeTableManager.buildNotVirtualItems(item, prebuildItem.getChilds());
                }
                if (TreeTableManager.expandedkeys.containsKey(LocalizablePluginManager.getCurrentLangFile().getId()) == true && TreeTableManager.expandedkeys.get(LocalizablePluginManager.getCurrentLangFile().getId()).contains(prebuildItem.getKey()) == true)
                {
                    ((TreeItem) item).setExpanded(true);
                }
                break;
        }
    }

    private static void addSelectionListener(SelectionAdapter listener)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((Table) TreeTableManager.treeTable).addSelectionListener(listener);
                break;

            default:
                ((Tree) TreeTableManager.treeTable).addSelectionListener(listener);
                break;
        }
    }

    static void buildItems()
    {
        if(LocalizablePluginManager.getCurrentLangFile() != null && TreeTableManager.oldLangFileId.equals(LocalizablePluginManager.getCurrentLangFile().getId()) == false)
        {
            TreeTableManager.oldLangFileId = LocalizablePluginManager.getCurrentLangFile().getId();
            TreeTableManager.oldColumn = -1;
        }
        
        PrebuildItemCollection prebuildItems = FilterManager.getPrebuildItems(LocalizablePluginManager.getCurrentLangFile());  
        
        TreeTableManager.buildNotVirtualItems(TreeTableManager.treeTable, prebuildItems);

        if (LocalizablePluginManager.getCurrentLangFile() != null && TreeTableManager.expandedkeys.containsKey(LocalizablePluginManager.getCurrentLangFile().getId()) == true)
        {
            TreeTableManager.expandedkeys.clear();
        }

        if (TreeTableManager.getCurrent().getLayoutData() == null)
        {
            Util.setGridData(TreeTableManager.getCurrent(), SWT.FILL, SWT.FILL, true, true);
        }

        TreeTableManager.setRedraw(true, true);

      
        if (TreeTableManager.getColumnCount() > 0)
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    ((Table) TreeTableManager.treeTable).setSortColumn((TableColumn) TreeTableManager.getColumn(0));
                    ((Table) TreeTableManager.treeTable).setSortDirection(SWT.UP);
                    break;

                default:
                    ((Tree) TreeTableManager.treeTable).setSortColumn((TreeColumn) TreeTableManager.getColumn(0));
                    ((Tree) TreeTableManager.treeTable).setSortDirection(SWT.UP);
                    break;
            }
        }        
        TreeTableManager.Cursor.setDefaultSelection();
    }

    private static void buildNotVirtualItems(Object parent, PrebuildItemCollection prebuildItemCollection)
    {
        for (int i = 0; i < prebuildItemCollection.size(); i++)
        {
            PrebuildItem prebuildItem = prebuildItemCollection.get(i);
            Item item = null;
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    item = new TableItem((Table) parent, SWT.NULL);

                    break;

                default:
                    if (parent instanceof Tree)
                    {
                        item = new TreeItem((Tree) parent, SWT.NULL);
                    }
                    else
                    {
                        item = new TreeItem((TreeItem) parent, SWT.NULL);
                    }
                    break;
            }
            TreeTableManager.addItem(item, prebuildItem);
        }
    }

    private static Canvas buildTable(int style)
    {
        final Table table = new Table(TreeTableManager.areaComposite, style);
        table.setHeaderVisible(true);
        final TableCursor tableCursor = new TableCursor(table, SWT.NULL);
        return tableCursor;
    }

    private static Canvas buildTree(int style)
    {
        final Tree tree = new Tree(TreeTableManager.areaComposite, style);

        tree.setHeaderVisible(true);
        tree.addMouseListener(new MouseAdapter()
        {
            
            public void mouseDoubleClick(MouseEvent e)
            {
                Item item = TreeTableManager.Cursor.getRow();
                if (item != null)
                {
                    if (((TreeItem) item).getExpanded() == false)
                    {
                        TreeTableManager.setExpanded(item, true);
                    }
                    else
                    {
                        TreeTableManager.setExpanded(item, false);
                    }
                }
            }
        });
        tree.addTreeListener(new TreeListener()
        {
            
            public void treeCollapsed(TreeEvent e)
            {
                TreeTableManager.Cursor.setSelection((Item) e.item, TreeTableManager.Cursor.getColumn());
            }

            
            public void treeExpanded(TreeEvent e)
            {
            }
        });

        final TreeCursor treeCursor = new TreeCursor(tree, SWT.NULL);

        return treeCursor;
    }

    private static void collectExpandedKeys(TreeItem[] items)
    {

        if (TreeTableManager.expandedkeys.containsKey(LocalizablePluginManager.getCurrentLangFile().getId()) == false)
        {
            TreeTableManager.expandedkeys.put(LocalizablePluginManager.getCurrentLangFile().getId(), new HashSet<String>());
        }

        for (int i = 0; i < items.length; i++)
        {
            if (items[i].getExpanded() == true)
            {
                PrebuildItem prebuildItem = (PrebuildItem) items[i].getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                TreeTableManager.expandedkeys.get(LocalizablePluginManager.getCurrentLangFile().getId()).add(prebuildItem.getKey());
            }
            TreeTableManager.collectExpandedKeys(items[i].getItems());
        }
    }

    static void dispose()
    {
        TreeTableManager.areaComposite = null;

        if (TreeTableManager.treeTable != null)
        {
            ToolTipText.unconfig(TreeTableManager.treeTable);
        }

        TreeTableManager.treeTable = null;
        TreeTableManager.cursor = null;

        TreeTableManager.EmptyImage = null;
        TreeTableManager.UnchangedImage = null;
        TreeTableManager.ExtraImage = null;
    }

    static Rectangle getBounds(Item item, int column)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((TableItem) item).getBounds(column);

            default:
                return ((TreeItem) item).getBounds(column);
        }
    }

    static int getChildItemCount(Item item)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return 0;

            default:
                return ((TreeItem) item).getItemCount();
        }
    }

    static Item[] getChildItems(Item item)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return new Item[0];

            default:
                return ((TreeItem) item).getItems();
        }
    }

    static Item getColumn(int index)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((Table) TreeTableManager.treeTable).getColumn(index);

            default:
                return ((Tree) TreeTableManager.treeTable).getColumn(index);
        }
    }

    static int getColumnCount()
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((Table) TreeTableManager.treeTable).getColumnCount();

            default:
                return ((Tree) TreeTableManager.treeTable).getColumnCount();
        }
    }
    static Composite getCurrent()
    {
        return TreeTableManager.treeTable;
    }

    static Item getItem(int index)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((Table) TreeTableManager.treeTable).getItem(index);

            default:
                return ((Tree) TreeTableManager.treeTable).getItem(index);
        }
    }

    static int getItemCount()
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((Table) TreeTableManager.treeTable).getItemCount();

            default:
                return ((Tree) TreeTableManager.treeTable).getItemCount();
        }
    }

    static Item[] getItems()
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((Table) TreeTableManager.treeTable).getItems();

            default:
                return ((Tree) TreeTableManager.treeTable).getItems();
        }
    }

    static Item getParentItem(Item item)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return null;

            default:
                return ((TreeItem) item).getParentItem();
        }
    }

    private static SearchResult getSelectedRowIndex(Object parent, Object search)
    {
        return TreeTableManager.getSelectedRowIndex(parent, search, -1);
    }

    private static SearchResult getSelectedRowIndex(Object parent, Object search, Integer parentRowIndex)
    {
        Item[] items = null;
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                if (parent instanceof Table)
                {
                    items = ((Table) parent).getItems();
                }
                break;

            default:
                if (parent instanceof Tree)
                {
                    items = ((Tree) parent).getItems();
                }
                else
                {
                    items = ((TreeItem) parent).getItems();
                }
                break;
        }

        SearchResult searchResult = new SearchResult();
        Integer rowIndex = parentRowIndex;

        if (items != null)
        {
            for (int i = 0; i < items.length; i++)
            {
                rowIndex++;
                if (search instanceof Item)
                {
                    if (items[i].equals(search) == true)
                    {
                        searchResult.success = true;
                        searchResult.item = items[i];
                        searchResult.index = rowIndex;
                        break;
                    }
                }
                else if (search instanceof String)
                {
                    if (((PrebuildItem) items[i].getData(TreeTableManager.DATAKEY_PREBUILD_ITEM)).getKey().equals(search) == true)
                    {
                        searchResult.success = true;
                        searchResult.item = items[i];
                        searchResult.index = rowIndex;
                        break;
                    }
                }
                else if (search instanceof Integer)
                {
                    if (searchResult.index == (Integer) search)
                    {
                        searchResult.success = true;
                        searchResult.item = items[i];
                        searchResult.index = rowIndex;
                        break;
                    }
                }
                if (items[i] instanceof TreeItem && (((TreeItem) items[i]).getExpanded() == true || (search instanceof String) == true))
                {
                    SearchResult childSearchResult = TreeTableManager.getSelectedRowIndex(items[i], search, rowIndex);
                    rowIndex = childSearchResult.index;
                    if (childSearchResult.success == true)
                    {
                        searchResult.success = childSearchResult.success;
                        searchResult.item = childSearchResult.item;
                        searchResult.index = childSearchResult.index;

                        break;
                    }
                }
            }
        }
        if (searchResult.success == false)
        {
            if (parentRowIndex == -1)
            {
                searchResult.index = -1;
            }
            else
            {
                searchResult.index = rowIndex;
            }
        }
        return searchResult;
    }

    static Item[] getSelection()
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((Table) TreeTableManager.treeTable).getSelection();

            default:
                return ((Tree) TreeTableManager.treeTable).getSelection();
        }
    }

    static String getText(Item item, int column)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((TableItem) item).getText(column);

            default:
                return ((TreeItem) item).getText(column);
        }
    }

    static int indexOf(Item column)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                return ((TableColumn) column).getParent().indexOf((TableColumn) column);

            default:
                return ((TreeColumn) column).getParent().indexOf((TreeColumn) column);
        }
    }

    static void initTreeTable(SWTSkinObjectContainer AreaContainer)
    {
        int R = TreeTableManager.COLOR_CELL_BACKGROUND.getRed();
        int G = TreeTableManager.COLOR_CELL_BACKGROUND.getGreen();
        int B = TreeTableManager.COLOR_CELL_BACKGROUND.getBlue();

        TreeTableManager.COLOR_ROW_HOT_BACKGROUND = new Color(Display.getCurrent(), R + 60, G + 60, B + 60);
        TreeTableManager.COLOR_ROW_NODE_BACKGROUND = new Color(Display.getCurrent(), R + 50, G + 50, B + 50);
        TreeTableManager.COLOR_COLUMN_DEFAULT_BACKGROUND = new Color(Display.getCurrent(), R + 40, G + 40, B + 40);
        TreeTableManager.COLOR_COLUMN_NODE_BACKGROUND = new Color(Display.getCurrent(), R + 30, G + 30, B + 30);
        TreeTableManager.COLOR_ROW_SELECTED_BACKGROUND = new Color(Display.getCurrent(), R + 20, G + 20, B + 20);
        TreeTableManager.COLOR_ROW_HOT_SELECTED_BACKGROUND = new Color(Display.getCurrent(), R + 10, G + 10, B + 10);

        TreeTableManager.COLOR_ROW_HOT_RECTANGLE_FOREGROUND = new Color(Display.getCurrent(), R - 40, G - 40, B - 40);
        TreeTableManager.COLOR_ROW_SELECTED_RECTANGLE_FOREGROUND = new Color(Display.getCurrent(), R - 60, G - 60, B - 60);

        TreeTableManager.EmptyImage = i18nAZ.viewInstance.getImageLoader().getImage("i18nAZ.image.empty");
        TreeTableManager.UnchangedImage = i18nAZ.viewInstance.getImageLoader().getImage("i18nAZ.image.unchanged");
        TreeTableManager.ExtraImage = i18nAZ.viewInstance.getImageLoader().getImage("i18nAZ.image.extra");

        TreeTableManager.areaComposite = AreaContainer.getComposite();
        TreeTableManager.rebuild();
    }
    static boolean isTreeMode()
    {
        return TreeTableManager.CurrentMode == TreeTableManager.MODE_TREE;
    }

    private static void rebuild()
    {
        if (TreeTableManager.areaComposite != null)
        {
            Composite newTreeTable = null;
            Canvas newCursor = null;
            int style = SWT.FULL_SELECTION | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE;
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    newCursor = buildTable(style);
                    newTreeTable = newCursor.getParent();
                    break;
                default:
                    newCursor = buildTree(style);
                    newTreeTable = newCursor.getParent();
                    break;
            }

            Util.setGridData(newTreeTable, SWT.FILL, SWT.FILL, true, true);

            if (TreeTableManager.treeTable != null)
            {
                Composite oldTreeTable = TreeTableManager.getCurrent();
                newTreeTable.moveAbove(oldTreeTable);
                ToolTipText.unconfig(oldTreeTable);
                oldTreeTable.dispose();
                TreeTableManager.areaComposite.layout();
            }

            TreeTableManager.cursor = newCursor;
            TreeTableManager.treeTable = newTreeTable;

            TreeTableManager.addSelectionListener(new SelectionAdapter()
            {
                
                public void widgetSelected(SelectionEvent e)
                {
                    TreeTableManager.Cursor.setSelection(TreeTableManager.getSelection()[0], TreeTableManager.Cursor.getColumn());
                }
            });
            TreeTableManager.Cursor.addSelectionListener(new SelectionAdapter()
            {
                
                public void widgetSelected(SelectionEvent e)
                {
                    TreeTableManager.Cursor.onSelect();
                }

                
                public void widgetDefaultSelected(SelectionEvent e)
                {
                    i18nAZ.viewInstance.selectEditor();
                }
            });

            Listener mouseTrackListener = new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    e.data = TreeTableManager.Cursor.getColumn();
                    if(TreeTableManager.Cursor.getRow() != null)
                    {
                        Listener[] listeners = TreeTableManager.Cursor.getRow().getListeners(e.type);
                        for (int k = 0; k < listeners.length; k++)
                        {
                            TypedListener typedListener = (TypedListener) listeners[k];
                            typedListener.handleEvent(e);
                        }
                    }                    
                }
            };
            cursor.addListener(SWT.MouseExit, mouseTrackListener);
            cursor.addListener(SWT.MouseEnter, mouseTrackListener);
            cursor.addListener(SWT.MouseHover, mouseTrackListener);

            TreeTableManager.treeTable.addListener(SWT.Show, new Listener()
            {
                
                public void handleEvent(Event event)
                {
                    Util.addListener(TreeTableManager.treeTable, SWT.PaintItem, TreeTableManager.itemListener);
                    Util.addListener(TreeTableManager.treeTable, SWT.MeasureItem, TreeTableManager.itemListener);
                    Util.addListener(TreeTableManager.treeTable, SWT.EraseItem, TreeTableManager.itemListener);
                }
            });
            TreeTableManager.treeTable.addListener(SWT.Hide, new Listener()
            {

                
                public void handleEvent(Event event)
                {
                    TreeTableManager.treeTable.removeListener(SWT.PaintItem, TreeTableManager.itemListener);
                    TreeTableManager.treeTable.removeListener(SWT.MeasureItem, TreeTableManager.itemListener);
                    TreeTableManager.treeTable.removeListener(SWT.EraseItem, TreeTableManager.itemListener);
                }

            });
            TreeTableManager.cursor.addKeyListener(new KeyListener()
            {
                
                public void keyPressed(KeyEvent e)
                {                 
                }

                
                public void keyReleased(KeyEvent e)
                {
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.DEL &&  i18nAZ.viewInstance.removeLanguageButton.isDisabled() == false)
                    {
                        i18nAZ.viewInstance.removeLanguage();
                        e.doit = false;
                    }   
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.F8 && i18nAZ.viewInstance.spellCheckerButton.isDisabled() == false)
                    {
                        i18nAZ.viewInstance.spellCheck();
                        e.doit = false;
                    }                    
                }

            });
            ToolTipText.config(TreeTableManager.treeTable);

            MenuDetectListener menuDetectListener = new MenuDetectListener()
            {
                
                public void menuDetected(MenuDetectEvent e)
                {

                    if (TreeTableManager.getSelection().length == 0 || TreeTableManager.getColumnCount() < 2)
                    {
                        e.doit = false;
                        return;
                    }
                    Item item = TreeTableManager.getSelection()[0];
                    int columnIndex = -1;
                    for (int i = 0; i < TreeTableManager.getColumnCount(); i++)
                    {
                        if (TreeTableManager.getBounds(item, i).contains(TreeTableManager.getCurrent().toControl(e.x, e.y)) == true)
                        {
                            columnIndex = i;
                            break;
                        }
                    }
                    if (columnIndex == -1)
                    {
                        e.doit = false;
                        return;
                    }
                    Menu menu = ((Composite) e.widget).getMenu();
                    menu.setData(TreeTableManager.DATAKEY_ITEM, item);
                    menu.setData(TreeTableManager.DATAKEY_COLUMN_INDEX, columnIndex);

                    int visible = MenuOptions.COPY_KEY | MenuOptions.COPY_REFERENCE | MenuOptions.COPY_VALUE;
                    int enabled = MenuOptions.NONE;

                    String textData = TreeTableManager.getText(item, 0);
                    if (textData.equals("") == false)
                    {
                        enabled |= MenuOptions.COPY_KEY;
                    }
                    textData = TreeTableManager.getText(item, 1);
                    if (textData.equals("") == false)
                    {
                        enabled |= MenuOptions.COPY_REFERENCE;
                    }
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                    if (columnIndex >= 2)
                    {
                        visible |= MenuOptions.REMOVE_COLUMN;
                        enabled |= MenuOptions.REMOVE_COLUMN;
                        enabled |= MenuOptions.COPY_VALUE;
                    }
                    if (columnIndex == 0 && TreeTableManager.getChildItemCount(item) > 0)
                    {
                        visible |= MenuOptions.FILTERS;
                        enabled |= MenuOptions.FILTERS;
                    }

                    if (columnIndex > 0)
                    {
                        if ((prebuildItem.getStates()[1] & State.URL) != 0)
                        {
                            visible |= MenuOptions.OPEN_URL;
                            try
                            {
                                new URL(TreeTableManager.getText(item, columnIndex));
                                enabled |= MenuOptions.OPEN_URL;
                            }
                            catch (MalformedURLException me)
                            {
                            }
                        }
                    }

                    i18nAZ.viewInstance.populateMenu(menu, visible, enabled);

                    if ((visible & MenuOptions.FILTERS) != 0)
                    {
                        CountObject counts = FilterManager.getCounts(prebuildItem.getKey());
                        menu.getItems()[4].setEnabled(counts.emptyCount > 0);
                        menu.getItems()[5].setEnabled(counts.unchangedCount > 0);
                        menu.getItems()[6].setEnabled(counts.extraCount > 0);
                        menu.getItems()[8].setEnabled(counts.redirectKeyCount > 0);
                        menu.getItems()[10].setEnabled(counts.urlsCount > 0);

                        menu.getItems()[4].setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.EmptyFilter") + (counts.emptyCount == 0 ? "" : " (" + counts.emptyCount + ")"));
                        menu.getItems()[5].setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.UnchangedFilter") + (counts.unchangedCount == 0 ? "" : " (" + counts.unchangedCount + ")"));
                        menu.getItems()[6].setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.ExtraFilter") + (counts.extraCount == 0 ? "" : " (" + counts.extraCount + ")"));
                        menu.getItems()[8].setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.RedirectKeysFilter") + (counts.redirectKeyCount == 0 ? "" : " (" + counts.redirectKeyCount + ")"));
                        menu.getItems()[10].setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.HideUrlsFilter") + (counts.urlsCount == 0 ? "" : " (" + counts.urlsCount + ")"));
                        menu.getItems()[11].setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.ShowUrlsFilter") + (counts.urlsCount == 0 ? "" : " (" + counts.urlsCount + ")"));

                        menu.getItems()[4].setSelection(FilterManager.getCurrentFilter().empty == FilterManager.getCurrentFilter().emptyExcludedKey.contains(prebuildItem.getKey()) == false);
                        menu.getItems()[5].setSelection(FilterManager.getCurrentFilter().unchanged == FilterManager.getCurrentFilter().unchangedExcludedKey.contains(prebuildItem.getKey()) == false);
                        menu.getItems()[6].setSelection(FilterManager.getCurrentFilter().extra == FilterManager.getCurrentFilter().extraExcludedKey.contains(prebuildItem.getKey()) == false);
                        menu.getItems()[8].setSelection(FilterManager.getCurrentFilter().redirectKeys == FilterManager.getCurrentFilter().hideRedirectKeysExcludedKey.contains(prebuildItem.getKey()) == false);
                        int overriddenState = FilterManager.getCurrentFilter().urls;

                        if (FilterManager.getCurrentFilter().urlsOverriddenStates.containsKey(prebuildItem.getKey()))
                        {
                            overriddenState = FilterManager.getCurrentFilter().urlsOverriddenStates.get(prebuildItem.getKey());
                        }
                        menu.getItems()[10].setSelection(overriddenState == 1);
                        menu.getItems()[11].setSelection(overriddenState == 2);
                    }

                    e.doit = visible != MenuOptions.NONE;
                }
            };
            Menu menu = null;
            menu = new Menu(TreeTableManager.treeTable);
            TreeTableManager.treeTable.setMenu(menu);
            TreeTableManager.treeTable.addMenuDetectListener(menuDetectListener);

            menu = new Menu(TreeTableManager.cursor);
            TreeTableManager.cursor.setMenu(menu);
            TreeTableManager.cursor.addMenuDetectListener(menuDetectListener);

            TreeTableManager.cursor.setBackground(TreeTableManager.COLOR_CELL_BACKGROUND);
            TreeTableManager.cursor.setForeground(TreeTableManager.COLOR_CELL_FOREGROUND);
        }
    }

    static void removeAll(boolean savePosition)
    {
        TreeTableManager.Cursor.setVisible(false);
        TreeTableManager.setRedraw(false, true);

        TreeTableManager.Cursor.cancelfocusedRow();

        if (savePosition == true)
        {
            savePosition();
        }

        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((Table) TreeTableManager.treeTable).removeAll();
                break;

            default:
                TreeTableManager.collectExpandedKeys(((Tree) TreeTableManager.treeTable).getItems());
                ((Tree) TreeTableManager.treeTable).removeAll();
                break;
        }
    }

    static void removeColumns(int columnIndex)
    {
        if (columnIndex > 0 && TreeTableManager.Cursor.getColumn() == columnIndex)
        {
            TreeTableManager.Cursor.setColumn(columnIndex - 1);
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((Table) TreeTableManager.treeTable).getColumns()[columnIndex].dispose();
                break;

            default:
                ((Tree) TreeTableManager.treeTable).getColumns()[columnIndex].dispose();
                break;
        }
    }

    static void removeAllColumns()
    {
        while (TreeTableManager.getColumnCount() > 0)
        {
            TreeTableManager.removeColumns(0);
        }
    }

    static void savePosition()
    {
        if (TreeTableManager.treeTable == null || LocalizablePluginManager.getCurrentLangFile() == null)
        {
            return;
        }
        String selectedKey = "";
        int relativeTopIndex = -1;
        if (TreeTableManager.getSelection().length > 0)
        {
            Item selectedRow = TreeTableManager.getSelection()[0];
            int selectedRowIndex = TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, selectedRow).index;
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    relativeTopIndex = selectedRowIndex - ((Table) TreeTableManager.treeTable).getTopIndex();
                    break;

                default:
                    int topIndex = TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, ((Tree) TreeTableManager.treeTable).getTopItem()).index;

                    relativeTopIndex = selectedRowIndex - topIndex;
                    break;
            }
            while (true)
            {
                PrebuildItem prebuildItem = (PrebuildItem) selectedRow.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                if (PrebuildItem.isShowable(prebuildItem))
                {
                    selectedKey = prebuildItem.getKey();
                    break;
                }
                TreeTableManager.Cursor.notifyListeners(SWT.KeyDown, Util.createKeyEvent(TreeTableManager.getCurrent(), SWT.ARROW_DOWN));
                if (TreeTableManager.Cursor.getRow() == null || selectedRow == null)
                {
                    break;
                }
                if (selectedRow.equals(TreeTableManager.Cursor.getRow()) == true)
                {
                    break;
                }
                selectedRow = TreeTableManager.Cursor.getRow();
            }
        }

        TreeTableManager.selectedKeys.put(LocalizablePluginManager.getCurrentLangFile().getId(), selectedKey);
        TreeTableManager.relativeTopIndexes.put(LocalizablePluginManager.getCurrentLangFile().getId(), relativeTopIndex);
        TreeTableManager.selectedColumnsIndex = TreeTableManager.Cursor.getColumn();
    }

    private static void setBackgroundColumn()
    {
        for (int i = 2; i < TreeTableManager.getColumnCount(); i++)
        {
            for (int row = 0; row < TreeTableManager.getItemCount(); row++)
            {
                Item item = TreeTableManager.getItem(row);
                Color defaultColor = null;
                Color nodeColor = null;
                if (i == TreeTableManager.Cursor.getColumn())
                {
                    defaultColor = TreeTableManager.COLOR_COLUMN_DEFAULT_BACKGROUND;
                    nodeColor = TreeTableManager.COLOR_COLUMN_NODE_BACKGROUND;
                }
                else
                {
                    defaultColor = TreeTableManager.COLOR_ROW_DEFAULT_BACKGROUND;
                    nodeColor = TreeTableManager.COLOR_ROW_NODE_BACKGROUND;
                }
                TreeTableManager.setBackgroundItem(item, i, defaultColor, nodeColor);
            }
        }
        i18nAZ.viewInstance.removeLanguageButton.setDisabled(TreeTableManager.Cursor.getColumn() < 2);
        if (TreeTableManager.Cursor.getColumn() >= 2)
        {
            String columnText = TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()).getText();
            if (columnText.equals("") == false)
            {
                columnText = "'" + columnText + "'";
            }
            ToolTipText.set(i18nAZ.viewInstance.removeLanguageButton.getSkinObject().getControl(), "i18nAZ.ToolTips.RemoveLanguage", new String[] { columnText });
        }
        else
        {
            ToolTipText.set(i18nAZ.viewInstance.removeLanguageButton.getSkinObject().getControl(), "i18nAZ.ToolTips.RemoveLanguage", new String[] {""});
        }
        i18nAZ.viewInstance.updateSpellCheckerButton();               
    }

    static void setExpanded(Item item, boolean expanded)
    {
        if (item.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        if (item instanceof TreeItem)
        {
            return;
        }
        TreeItem[] items = ((TreeItem) item).getItems();
        ((TreeItem) item).setExpanded(expanded);
        for (int i = 0; i < items.length; i++)
        {
            TreeTableManager.setExpanded(items[i], expanded);
        }
    }
    static void setEnableColumn(Item column, boolean enable)
    {   
        if (column.isDisposed() == true)
        {
            return;
        }
        if(enable == true)
        {
            int width =  COConfigurationManager.getIntParameter("i18nAZ.columnWidth." + TreeTableManager.indexOf(column), 200);
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    ((TableColumn)column).setResizable(true);
                    ((TableColumn)column).setWidth(width);
                     break;
    
                default:
                    ((TreeColumn)column).setResizable(true);
                    ((TreeColumn)column).setWidth(width);
                    break;
            }
        }
        else
        {
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    ((TableColumn)column).setResizable(false);
                    ((TableColumn)column).setWidth(0);
                     break;
    
                default:
                    ((TreeColumn)column).setResizable(false);  
                    ((TreeColumn)column).setWidth(0); 
                    break;
            }
        }
    }
    
    private static void setBackgroundItem(Item item, int column, Color defaultColor, Color nodeColor)
    {
        if (item.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        Image image = null;
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((TableItem) item).setBackground(column, defaultColor);
                image = ((TableItem) item).getImage(column);
                if (image != null)
                {
                    image.setBackground(((TableItem) item).getBackground(column));
                }
                break;

            default:
                TreeItem[] items = ((TreeItem) item).getItems();
                ((TreeItem) item).setBackground(column, items.length > 0 ? nodeColor : defaultColor);
                image = ((TreeItem) item).getImage();
                if (image != null)
                {
                    image.setBackground(((TreeItem) item).getBackground(column));
                }
                for (int i = 0; i < items.length; i++)
                {
                    TreeTableManager.setBackgroundItem(items[i], column, defaultColor, nodeColor);
                }
                break;
        }
    }

    static void setMode(boolean treeMode)
    {
        if (TreeTableManager.treeTable != null)
        {
            TreeTableManager.setRedraw(false, true);
        }

        savePosition();
        if (treeMode == false)
        {
            TreeTableManager.CurrentMode = TreeTableManager.MODE_TABLE;
        }
        else
        {
            TreeTableManager.CurrentMode = TreeTableManager.MODE_TREE;
        }
        TreeTableManager.rebuild();
    }

    static void setRedraw(boolean redraw)
    {
        setRedraw(redraw, false);
    }

    private static void setRedraw(boolean redraw, boolean setVisible)
    {
        if (setVisible == true)
        {
            TreeTableManager.setVisible(redraw);
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((Table) TreeTableManager.treeTable).setRedraw(redraw);
                break;

            default:
                ((Tree) TreeTableManager.treeTable).setRedraw(redraw);
                break;
        }
    }

    private static void setSelection(Item item)
    {
        if (item.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((Table) TreeTableManager.treeTable).setSelection((TableItem) item);
                break;

            default:
                ((Tree) TreeTableManager.treeTable).setSelection((TreeItem) item);
                break;
        }
    }

    private static void setState(Item item, int column, int state)
    {
        if (item.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
        prebuildItem.getStates()[column] = state;
        Image image = null;
        switch (state)
        {
            case State.EMPTY:
                image = TreeTableManager.EmptyImage;
                break;

            case State.UNCHANGED:
                image = TreeTableManager.UnchangedImage;
                break;

            case State.EXTRA:
                image = TreeTableManager.ExtraImage;
                break;
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((TableItem) item).setImage(column, image);
                break;

            default:
                ((TreeItem) item).setImage(column, image);
                break;
        }
    }

    private static void setText(Item item, int column, String value)
    {
        if (item.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        if (value == null)
        {
            value = "";
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((TableItem) item).setText(column, value);
                break;
            default:
                ((TreeItem) item).setText(column, value);
                break;
        }
    }

    static void setToolTipColumnHeader(Item column, String toolTipText)
    {
        if (column.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((TableColumn) column).setToolTipText(toolTipText);
                break;

            default:
                ((TreeColumn) column).setToolTipText(toolTipText);
                break;
        }
    }

    static void setVisible(boolean visible)
    {
        if (TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                ((Table) TreeTableManager.treeTable).setVisible(visible);
                break;

            default:
                ((Tree) TreeTableManager.treeTable).setVisible(visible);
                break;
        }
    }
}