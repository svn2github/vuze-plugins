/*
 * TreeTableManager.java
 *
 * Created on March 21, 2014, 4:27 PM
 */

package i18nAZ;

import i18nAZ.View.MenuOptions;
import i18nAZ.View.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.custom.TreeCursor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;

/**
 * TreeTableManager.java
 * 
 * @author Repris d'injustice
 */
class TreeTableManager
{
    static final String DATAKEY_KEY = "key";
    static final String DATAKEY_COMMENTS = "comments";
    static final String DATAKEY_VALUES = "values";
    static final String DATAKEY_EXIST = "exist";
    static final String DATAKEY_CHILDS = "childs";
    static final String DATAKEY_STATES = "states";
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

    private static HashSet<String> expandedkeys = new HashSet<String>();
    private static String selectedKey = "";
    private static int relativeTopIndex = -1;

    private static Image EmptyImage = null;
    private static Image UnchangedImage = null;
    private static Image ExtraImage = null;

    private static Item focusedRow = null;
    private static int focusedColumn = 0;

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
            if(i18nAZ.viewInstance.getDisplay().getThread().equals(Thread.currentThread()) == false)            
            {
                final List<Integer> value = new ArrayList<Integer>();
                i18nAZ.viewInstance.getDisplay().syncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        value.add(TreeTableManager.Cursor.getColumn());           
                   }                
                });
                return value.get(0);
            }
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
            i18nAZ.viewInstance.undoRedo.unsetKey();
            
            if (TreeTableManager.focusedRow != null)
            {
                TreeTableManager.Cursor.setSelection(TreeTableManager.focusedRow, TreeTableManager.focusedColumn);
                TreeTableManager.setSelection(TreeTableManager.Cursor.getRow());
                i18nAZ.viewInstance.selectEditor();
                return;
            }
            if (TreeTableManager.getCurrent().getVisible() == true && ((TreeTableManager.isTreeMode() == false && TreeTableManager.treeTable instanceof Table) || (TreeTableManager.isTreeMode() == true && TreeTableManager.treeTable instanceof Tree)))
            {
                try
                {
                    if (TreeTableManager.getItemCount() > 0)
                    {
                        if (TreeTableManager.getColumnCount() >= 2)
                        {
                            if (TreeTableManager.Cursor.getColumn() < 2)
                            {
                                TreeTableManager.Cursor.setSelection(TreeTableManager.Cursor.getRow(), 2);
                                return;
                            }
                        }
                        Item row = TreeTableManager.Cursor.getRow();
                        boolean exist = false;
                        if (row != null)
                        {
                            TreeTableManager.setSelection(TreeTableManager.Cursor.getRow());
                            exist = (boolean) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_EXIST);
                        }
                        TreeTableManager.setBackgroundColumn();
                        TreeTableManager.Cursor.setVisible(exist);
                    }
                    i18nAZ.viewInstance.updateStyledTexts();
                    TreeTableManager.Cursor.setFocus();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        static void setDefaultSelection()
        {
            int selectedRowIndex = -1;
            Item selectedItem = null;
            if (TreeTableManager.selectedKey.equals("") == false)
            {
                Object[] datas = TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, TreeTableManager.selectedKey, -1);
                selectedItem = (Item) datas[2];
                selectedRowIndex = (selectedItem == null) ? -1 : (int) datas[0];

            }
            if (selectedRowIndex != -1)
            {
                TreeTableManager.Cursor.setSelection(selectedItem, 0);

                Object[] datas = TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, selectedItem, -1);
                selectedRowIndex = (int) datas[0];

                if (TreeTableManager.relativeTopIndex != -1)
                {
                    switch (TreeTableManager.CurrentMode)
                    {
                        case TreeTableManager.MODE_TABLE:
                            int topIndex = selectedRowIndex - TreeTableManager.relativeTopIndex;
                            ((Table) TreeTableManager.treeTable).setTopIndex(topIndex);
                            break;

                        default:
                            Item topItem = (Item) TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, selectedRowIndex - TreeTableManager.relativeTopIndex, -1)[2];
                            if (topItem != null)
                            {
                                ((Tree) TreeTableManager.treeTable).setTopItem((TreeItem) topItem);
                            }
                            break;
                    }
                }
                TreeTableManager.Cursor.setSelection(selectedItem, 2);
            }
            else if (TreeTableManager.getItemCount() > 0)
            {
                TreeTableManager.Cursor.setSelection(TreeTableManager.getItem(0), 2);
            }
            else
            {
                TreeTableManager.Cursor.onSelect();
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

        private static void setSelection(Item row, int columnIndex)
        {
            try
            {
                if (columnIndex >= 0 && columnIndex < TreeTableManager.getColumnCount())
                {
                    switch (TreeTableManager.CurrentMode)
                    {
                        case TreeTableManager.MODE_TABLE:
                            if (row != null && row.equals(((TableCursor) TreeTableManager.cursor).getRow()) && columnIndex == ((TableCursor) TreeTableManager.cursor).getColumn())
                            {
                                return;
                            }
                            ((TableCursor) TreeTableManager.cursor).setSelection((TableItem) row, columnIndex);
                            break;

                        default:
                            if (row != null && row.equals(((TreeCursor) TreeTableManager.cursor).getRow()) && columnIndex == ((TreeCursor) TreeTableManager.cursor).getColumn())
                            {
                                return;
                            }
                            ((TreeCursor) TreeTableManager.cursor).setSelection((TreeItem) row, columnIndex);
                            break;
                    }
                    TreeTableManager.Cursor.onSelect();
                }
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
        ControlAdapter controlAdapter = new ControlAdapter () 
        {
            public void controlResized(ControlEvent e)
            {
                int columnIndex = (int) e.widget.getData(TreeTableManager.DATAKEY_COLUMN_INDEX); 
                int width = (int) Util.invoke(e.widget, "getWidth");
                COConfigurationManager.setParameter("i18nAZ.columnWidth." + columnIndex, width);                          
            }            
        };
                
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                final Table table = (Table) TreeTableManager.treeTable;
                TableColumn tableColumn = new TableColumn((Table) TreeTableManager.treeTable, 16384);
                tableColumn.setText(text);
                tableColumn.setWidth(width);
                Listener sortListener = new Listener()
                {
                    @Override
                    public void handleEvent(Event e)
                    {
                        TableColumn column = (TableColumn) e.widget;
                        TableItem[] items = table.getItems();
                        int index = -1;
                        TableColumn[] columns = table.getColumns();
                        for (int i = 0; i < columns.length; i++)
                        {
                            if (column.equals(columns[i]) == true)
                            {
                                index = i;
                                break;
                            }
                        }
                        if (index == -1)
                        {
                            return;
                        }
                        TreeTableManager.setRedraw(false);
                        for (int i = 1; i < items.length; i++)
                        {
                            String value1 = items[i].getText(index);
                            for (int j = 0; j < i; j++)
                            {
                                String value2 = items[j].getText(index);
                                if ((value1.compareTo(value2) < 0 && table.getSortDirection() != SWT.UP) || (value2.compareTo(value1) < 0 && table.getSortDirection() == SWT.UP))
                                {
                                    String[] values = new String[table.getColumnCount()];
                                    for (int k = 0; k < values.length; k++)
                                    {
                                        values[k] = items[i].getText(k);
                                    }

                                    Map<String, Object> prebuildItem = new HashMap<String, Object>();
                                    prebuildItem.put(TreeTableManager.DATAKEY_KEY, items[i].getData(TreeTableManager.DATAKEY_KEY));
                                    prebuildItem.put(TreeTableManager.DATAKEY_COMMENTS, items[i].getData(TreeTableManager.DATAKEY_COMMENTS));
                                    prebuildItem.put(TreeTableManager.DATAKEY_VALUES, values);
                                    prebuildItem.put(TreeTableManager.DATAKEY_STATES, items[i].getData(TreeTableManager.DATAKEY_STATES));
                                    prebuildItem.put(TreeTableManager.DATAKEY_EXIST, items[i].getData(TreeTableManager.DATAKEY_EXIST));

                                    items[i].dispose();

                                    TableItem item = new TableItem(table, SWT.NONE, j);
                                    TreeTableManager.addItem(item, prebuildItem);

                                    items = table.getItems();
                                    break;
                                }
                            }
                        }
                        table.setSortColumn(column);
                        table.setSortDirection(table.getSortDirection() != SWT.UP ? SWT.UP : SWT.DOWN);
                        TreeTableManager.setRedraw(true);
                    }
                };
                tableColumn.addListener(SWT.Selection, sortListener);
                tableColumn.addControlListener(controlAdapter);  
                tableColumn.setData(TreeTableManager.DATAKEY_COLUMN_INDEX, TreeTableManager.getColumnCount() - 1);   
                return tableColumn;

            default:
                TreeColumn treeColumn = new TreeColumn((Tree) TreeTableManager.treeTable, 16384);
                treeColumn.setText(text);
                treeColumn.setWidth(width);
                treeColumn.addControlListener(controlAdapter);
                treeColumn.setData(TreeTableManager.DATAKEY_COLUMN_INDEX, TreeTableManager.getColumnCount() - 1);                
                return treeColumn;
        }
    }

    private static void addItem(final Item item, Map<String, Object> prebuildItem)
    {        
        Util.addTypedListenerAndChildren((Widget)item, SWT.MouseEnter, new TypedListener(new MouseTrackAdapter()
        {
            @Override
            public void mouseEnter(MouseEvent e)
            {
                i18nAZ.viewInstance.itemEnterEventOccurred(item, (int) e.data);
            }
        }));
        
        item.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                Util.removeTypedListenerAndChildren((Widget)item, SWT.MouseExit);
                Control parent = (Control) Util.invoke(item, "getParent");
                int columnCount = (int) Util.invoke(parent, "getColumnCount");                
                for(int i = 0; i < columnCount; i++)
                {
                    ToolTipTrackListener.removeToolTipAndListeners(item, SWT.MouseExit, i);
                    ToolTipTrackListener.removeToolTipAndListeners(item, SWT.MouseHover, i);    
                }
            }            
        });
        
        String key = (String) prebuildItem.get(TreeTableManager.DATAKEY_KEY);
        String[] commentsLines = (String[]) prebuildItem.get(TreeTableManager.DATAKEY_COMMENTS);
        String[] values = (String[]) prebuildItem.get(TreeTableManager.DATAKEY_VALUES);
        int[] states = (int[]) prebuildItem.get(TreeTableManager.DATAKEY_STATES);
        boolean Exist = (boolean) prebuildItem.get(TreeTableManager.DATAKEY_EXIST);

        item.setData(TreeTableManager.DATAKEY_KEY, key);
        item.setData(TreeTableManager.DATAKEY_COMMENTS, commentsLines);
        item.setData(TreeTableManager.DATAKEY_STATES, new int[values.length]);

        for (int i = 0; i < values.length; i++)
        {
            TreeTableManager.setText(item, i, values[i]);
            TreeTableManager.setState(item, i, states[i]);
        }
        item.setData(TreeTableManager.DATAKEY_EXIST, Exist);
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                break;

            default:
                if (prebuildItem.containsKey(TreeTableManager.DATAKEY_CHILDS) == true)
                {
                    ((TreeItem) item).setBackground(TreeTableManager.COLOR_ROW_NODE_BACKGROUND);
                   
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> childPrebuildItems = (List<Map<String, Object>>) prebuildItem.get(TreeTableManager.DATAKEY_CHILDS);
                    TreeItem[] childItems = ((TreeItem) item).getItems();
                    TreeTableManager.buildNotVirtualItems(item, childItems, childPrebuildItems);
                }
                else
                {
                    ((TreeItem) item).setBackground(TreeTableManager.COLOR_ROW_DEFAULT_BACKGROUND);
                }
                if (TreeTableManager.expandedkeys.contains(key) == true)
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

    static void buildItems(List<Map<String, Object>> prebuildItems)
    {
        Item[] items = TreeTableManager.getItems();
        TreeTableManager.buildNotVirtualItems(TreeTableManager.treeTable, items, prebuildItems);

        TreeTableManager.expandedkeys.clear();

        TreeTableManager.setRedraw(true, true);

        TreeTableManager.Cursor.setDefaultSelection();
    }

    private static void buildNotVirtualItems(Object parent, Item[] items, List<Map<String, Object>> prebuildItems)
    {
        for (int i = 0; i < prebuildItems.size(); i++)
        {
            Map<String, Object> prebuildItem = prebuildItems.get(i);
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
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                Item item = TreeTableManager.Cursor.getRow();
                if (item != null)
                {
                    TreeTableManager.setSelection(TreeTableManager.Cursor.getRow());
                    if(((TreeItem) item).getExpanded() == false)
                    {
                        TreeTableManager.setExpanded((TreeItem) item, true);
                    }
                    else
                    {
                        TreeTableManager.setExpanded((TreeItem) item, false);
                    }
                }               
            }
        });
        
        
        final TreeCursor treeCursor = new TreeCursor(tree, SWT.NULL);
     
        treeCursor.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.character == '+')
                {
                    tree.getSelection()[0].setExpanded(true);
                }
                else if (e.character == '*')
                {
                    TreeTableManager.setExpanded(tree.getSelection()[0], true);
                }
                else if (e.character == '-')
                {
                    tree.getSelection()[0].setExpanded(false);
                }
                else if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN)
                {
                    Item item = TreeTableManager.Cursor.getRow();
                    if (item != null)
                    {
                        TreeTableManager.setSelection(TreeTableManager.Cursor.getRow());
                    }
                    i18nAZ.viewInstance.updateStyledTexts();
                }
            }
        });
        return treeCursor;
    }

    private static void collectExpandedKeys(TreeItem[] items)
    {
        for (int i = 0; i < items.length; i++)
        {
            if (items[i].getExpanded() == true)
            {
                expandedkeys.add((String) items[i].getData(TreeTableManager.DATAKEY_KEY));
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
            TreeTableManager.setRedraw(false, true);            
            TreeTableManager.removeAll(false);
            if (!TreeTableManager.treeTable.isDisposed())
            {
                TreeTableManager.treeTable.dispose();
            }
            TreeTableManager.treeTable = null;
        }
        if (TreeTableManager.cursor != null)
        {
            if (!TreeTableManager.cursor.isDisposed())
            {
                TreeTableManager.cursor.dispose();
            }
            TreeTableManager.cursor = null;
        }
        if (TreeTableManager.EmptyImage != null)
        {
            if (!TreeTableManager.EmptyImage.isDisposed())
            {
                TreeTableManager.EmptyImage.dispose();
            }
            TreeTableManager.EmptyImage = null;
        }
        if (TreeTableManager.UnchangedImage != null)
        {
            if (!TreeTableManager.UnchangedImage.isDisposed())
            {
                TreeTableManager.UnchangedImage.dispose();
            }
            TreeTableManager.UnchangedImage = null;
        }
        if (TreeTableManager.ExtraImage != null)
        {
            if (!TreeTableManager.ExtraImage.isDisposed())
            {
                TreeTableManager.ExtraImage.dispose();
            }
            TreeTableManager.ExtraImage = null;
        }
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
        if(i18nAZ.viewInstance.getDisplay().getThread().equals(Thread.currentThread()) == false)            
        {
            final List<Integer> value = new ArrayList<Integer>();
            i18nAZ.viewInstance.getDisplay().syncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    value.add(TreeTableManager.getColumnCount());           
               }                
            });
            return value.get(0);
        }
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
    static Object getDataColumn(final int columnIndex, final String key)
    {
        if(i18nAZ.viewInstance.getDisplay().getThread().equals(Thread.currentThread()) == false)            
        {
            final List<Object> value = new ArrayList<Object>();
            i18nAZ.viewInstance.getDisplay().syncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    value.add(TreeTableManager.getColumn(columnIndex).getData(key));           
               }                
            });
            return value.get(0);
        }
        return TreeTableManager.getColumn(columnIndex).getData(key);
    }
    private static Item getItem(int index)
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
        if(i18nAZ.viewInstance.getDisplay().getThread().equals(Thread.currentThread()) == false)            
        {
            final List<Integer> value = new ArrayList<Integer>();
            i18nAZ.viewInstance.getDisplay().syncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    value.add(TreeTableManager.getItemCount());           
               }                
            });
            return value.get(0);
        }
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

    private static Object[] getSelectedRowIndex(Object parent, Object search, int parentRowIndex)
    {
        int found = 1;
        int rowIndex = parentRowIndex;
        Item foundedItem = null;

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
        if (items != null)
        {
            for (int i = 0; i < items.length; i++)
            {
                rowIndex++;
                if (search instanceof Item && items[i].equals(search) == true)
                {
                    found = 0;
                    foundedItem = items[i];
                    break;
                }
                else if (search instanceof String && ((String) items[i].getData(TreeTableManager.DATAKEY_KEY)).equals(search) == true)
                {
                    found = 0;
                    foundedItem = items[i];
                    break;
                }
                else if ((search instanceof Item) == false && (search instanceof String) == false && rowIndex == (int) search)
                {
                    found = 0;
                    foundedItem = items[i];
                    break;
                }
                if (items[i] instanceof TreeItem && (((TreeItem) items[i]).getExpanded() == true || (search instanceof String) == true))
                {
                    Object[] datas = TreeTableManager.getSelectedRowIndex(items[i], search, rowIndex);
                    rowIndex = (int) datas[0];
                    found = (int) datas[1];
                    foundedItem = (Item) datas[2];
                    if (found == 0)
                    {
                        break;
                    }
                }
            }
        }
        return new Object[] { rowIndex, found, foundedItem };
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
            int style = SWT.FLAT | SWT.FULL_SELECTION | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE;
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

            newTreeTable.setLayoutData(new GridData(4, 4, true, true));

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
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    TreeTableManager.Cursor.setSelection(TreeTableManager.getSelection()[0], TreeTableManager.Cursor.getColumn());
                }
            });            
            TreeTableManager.Cursor.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    TreeTableManager.Cursor.onSelect();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e)
                {
                    i18nAZ.viewInstance.selectEditor();
                }
            });
     
            Listener mouseTrackListener = new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    e.data =  TreeTableManager.Cursor.getColumn();
                    Listener[] listeners = TreeTableManager.Cursor.getRow().getListeners(e.type);
                    for (int k = 0; k < listeners.length; k++)
                    {
                        TypedListener typedListener = (TypedListener) listeners[k];
                        typedListener.handleEvent(e);
                    }                    
                }                
            };
            cursor.addListener(SWT.MouseExit, mouseTrackListener);
            cursor.addListener(SWT.MouseEnter, mouseTrackListener);
            cursor.addListener(SWT.MouseHover, mouseTrackListener);
            
            Listener listener = new Listener()
            { 
                int getTotalWidth()
                {
                    int width = 0;
                    for(int i = 0; i < TreeTableManager.getColumnCount(); i++)
                    {  
                        switch (TreeTableManager.CurrentMode)
                        {
                            case TreeTableManager.MODE_TABLE:
                                width +=((TableColumn) TreeTableManager.getColumn(i)).getWidth();
                                break;
                            default:
                                width +=((TreeColumn) TreeTableManager.getColumn(i)).getWidth();
                                break;
                        }
                        
                    }
                    return width;
                }
                String getText(GC gc, Item item, int column)
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
                
                Item itemHot = null;
                   
                @Override
                public void handleEvent(Event e)
                {
                    switch (e.type)
                    {
                        case SWT.Selection:
                            
                            
                        case SWT.EraseItem:
                            
                            // INIT
                            Rectangle originalClipping = e.gc.getClipping();                            
                           
                            // FOREGROUND
                            if (e.index == 0 && TreeTableManager.CurrentMode == TreeTableManager.MODE_TABLE)
                            {                                
                                e.detail &= ~SWT.FOREGROUND;
                            }                            
                          
                            // NO HOT NO SELECTED
                            if(this.itemHot != null && this.itemHot.equals(e.item) == true && (e.detail & SWT.HOT) == 0 && e.index > 0)
                            {
                                //e.detail &= ~SWT.BACKGROUND;
                                //e.detail &= ~SWT.SELECTED;                                
                                e.detail |= SWT.HOT;
                                
                            }
                            if ((e.detail & SWT.HOT) == 0  && e.index == 0)
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
                                //e.detail &= ~SWT.SELECTED;
                                if(e.index == 0)
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
                               // e.gc.drawRectangle(e.x, e.y, rowClipping.width, e.height);                                   
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
            TreeTableManager.treeTable.addListener(SWT.EraseItem, listener);
            TreeTableManager.treeTable.addListener(SWT.MeasureItem, listener);
            TreeTableManager.treeTable.addListener(SWT.PaintItem, listener);            
            
            ToolTipText.config(TreeTableManager.treeTable);
              
            MenuDetectListener menuDetectListener = new MenuDetectListener()
            {
                @Override
                public void menuDetected(MenuDetectEvent e)
                {

                    if (TreeTableManager.getSelection().length == 0)
                    {
                        e.doit = false;
                        return;
                    }
                    Item item = TreeTableManager.getSelection()[0];
                    Menu menu = ((Composite) e.widget).getMenu();
                    
                    int visible = MenuOptions.NONE;
                    int enabled = MenuOptions.NONE;
                    
                    if (TreeTableManager.Cursor.getColumn() >= 2 && TreeTableManager.getBounds(item, TreeTableManager.Cursor.getColumn()).contains(TreeTableManager.getCurrent().toControl(e.x, e.y)) == true)
                    {
                        visible |= MenuOptions.REMOVE_COLUMN;
                        enabled |= MenuOptions.REMOVE_COLUMN;
                    }
                    if ((TreeTableManager.getBounds(item, 0).contains(TreeTableManager.getCurrent().toControl(e.x, e.y)) == true ||
                            TreeTableManager.getBounds(item, 1).contains(TreeTableManager.getCurrent().toControl(e.x, e.y)) == true))
                    {
                        visible |= MenuOptions.ROW_COPY;
                        if (((boolean) item.getData(TreeTableManager.DATAKEY_EXIST)) == true)
                        { 
                            enabled |= MenuOptions.ROW_COPY;
                        }
                    }                     
                    if (TreeTableManager.getChildItemCount(item) > 0 && TreeTableManager.getBounds(item, 0).contains(TreeTableManager.getCurrent().toControl(e.x, e.y)) == true)
                    {
                        visible |= MenuOptions.FILTERS;
                        enabled |= MenuOptions.FILTERS;
                    }
                    i18nAZ.viewInstance.populateMenu(menu, visible, enabled);
                    
                    if((visible & MenuOptions.FILTERS) != 0)
                    {
                        String key = (String) item.getData(TreeTableManager.DATAKEY_KEY);

                        int[] counts = i18nAZ.viewInstance.getCounts(key, null, -1);
                        menu.getItems()[3].setEnabled(counts[1] > 0);
                        menu.getItems()[4].setEnabled(counts[2] > 0);
                        menu.getItems()[5].setEnabled(counts[3] > 0);
                        menu.getItems()[7].setEnabled(counts[4] > 0);
                        menu.getItems()[8].setEnabled(counts[5] > 0);

                        menu.getItems()[3].setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Menus.EmptyFilter") + (counts[1] == 0 ? "" : " (" + counts[1] + ")"));
                        menu.getItems()[4].setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Menus.UnchangedFilter") + (counts[2] == 0 ? "" : " (" + counts[2] + ")"));
                        menu.getItems()[5].setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Menus.ExtraFilter") + (counts[3] == 0 ? "" : " (" + counts[3] + ")"));
                        menu.getItems()[7].setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Menus.RedirectKeysFilter") + (counts[4] == 0 ? "" : " (" + counts[4] + ")"));
                        menu.getItems()[8].setText(i18nAZ.viewInstance.getLocalisedMessageText("i18nAZ.Menus.UrlsFilter") + (counts[5] == 0 ? "" : " (" + counts[5] + ")"));

                        menu.setData(TreeTableManager.DATAKEY_ITEM, item);
                        menu.getItems()[3].setSelection(i18nAZ.viewInstance.emptyFilter == i18nAZ.viewInstance.emptyFilterExcludedKey.contains(key) == false);
                        menu.getItems()[4].setSelection(i18nAZ.viewInstance.unchangedFilter == i18nAZ.viewInstance.unchangedFilterExcludedKey.contains(key) == false);
                        menu.getItems()[5].setSelection(i18nAZ.viewInstance.extraFilter == i18nAZ.viewInstance.extraFilterExcludedKey.contains(key) == false);
                        menu.getItems()[7].setSelection(i18nAZ.viewInstance.redirectKeysFilter == i18nAZ.viewInstance.redirectKeysFilterExcludedKey.contains(key) == false);
                        menu.getItems()[8].setSelection(i18nAZ.viewInstance.urlsFilter == i18nAZ.viewInstance.urlsFilterExcludedKey.contains(key) == false);
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
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                while (((Table) TreeTableManager.treeTable).getColumnCount() > 0)
                {
                    ((Table) TreeTableManager.treeTable).getColumn(0).dispose();
                }
                break;

            default:
                while (((Tree) TreeTableManager.treeTable).getColumnCount() > 0)
                {
                    ((Tree) TreeTableManager.treeTable).getColumn(0).dispose();
                }
                break;
        }
    }

    private static void savePosition()
    {
        if (TreeTableManager.treeTable == null)
        {
            return;
        }

        if (TreeTableManager.getSelection().length > 0)
        {
            TreeTableManager.relativeTopIndex = -1;
            int selectedRowIndex = (int) TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, TreeTableManager.getSelection()[0], -1)[0];
            switch (TreeTableManager.CurrentMode)
            {
                case TreeTableManager.MODE_TABLE:
                    TreeTableManager.relativeTopIndex = selectedRowIndex - ((Table) TreeTableManager.treeTable).getTopIndex();
                    break;

                default:
                    int topIndex = (int) TreeTableManager.getSelectedRowIndex(TreeTableManager.treeTable, ((Tree) TreeTableManager.treeTable).getTopItem(), -1)[0];

                    TreeTableManager.relativeTopIndex = selectedRowIndex - topIndex;
                    break;
            }
            TreeTableManager.selectedKey = (String) TreeTableManager.getSelection()[0].getData(TreeTableManager.DATAKEY_KEY);
        }
        else
        {
            TreeTableManager.relativeTopIndex = -1;
            TreeTableManager.selectedKey = "";
        }
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
        String columnText = TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()).getText();
        if(columnText.equals("") == false)
        {
            columnText = "'" + columnText + "'";
        }
        i18nAZ.viewInstance.removeLanguageButton.setDisabled(TreeTableManager.Cursor.getColumn() < 2);
        ToolTipText.set(i18nAZ.viewInstance.removeLanguageButton.getSkinObject().getControl(), "i18nAZ.ToolTips.RemoveLanguage", new String[]{columnText});

    }

    private static void setExpanded(TreeItem item, boolean expanded)
    {
        if (item.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        TreeItem[] items = item.getItems();
        item.setExpanded(expanded);
        for (int i = 0; i < items.length; i++)
        {
            TreeTableManager.setExpanded(items[i], expanded);
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
    static void setRedraw(boolean redraw, boolean setVisible)
    {
        switch (TreeTableManager.CurrentMode)
        {
            case TreeTableManager.MODE_TABLE:
                if(setVisible == true)
                {
                    ((Table) TreeTableManager.treeTable).setVisible(redraw);                    
                }
                ((Table) TreeTableManager.treeTable).setRedraw(redraw);
                break;

            default:
                if(setVisible == true)
                {
                    ((Tree) TreeTableManager.treeTable).setVisible(redraw);                    
                }
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

    static void setState(Item item, int column, int state)
    {
        if (item.isDisposed() == true || TreeTableManager.treeTable.isDisposed() == true)
        {
            return;
        }
        int[] states = (int[]) item.getData(TreeTableManager.DATAKEY_STATES);
        states[column] = state;
        item.setData(TreeTableManager.DATAKEY_STATES, states);
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

    static void setText(Item item, int column, String value)
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
}