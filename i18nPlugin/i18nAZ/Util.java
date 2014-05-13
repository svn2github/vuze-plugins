/*
 * Util.java
 *
 * Created on February 24, 2004, 12:00 PM
 */

package i18nAZ;

import i18nAZ.FilterManager.State;
import i18nAZ.SpellChecker.SpellObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;

/**
 * Misc Usefull functions.
 * 
 * @author Repris d'injustice
 */
class Util
{
    private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    static final Locale EMPTY_LOCALE = new Locale("", "");
    static final KeyComparator KEY_COMPARATOR = new KeyComparator();
    static final StringComparator STRING_COMPARATOR = new StringComparator();

    static class KeyComparator implements Comparator<String>
    {
        Collator collator = null;

        KeyComparator()
        {
            this.collator = Collator.getInstance(Locale.US);
            this.collator.setStrength(Collator.IDENTICAL);
        }

        
        public int compare(String key1, String key2)
        {
            String[] splittedKeys1 = key1.split("\\.");
            String[] splittedKeys2 = key2.split("\\.");

            int result = 0;
            for (int i = 0; i < Math.max(splittedKeys1.length, splittedKeys2.length); i++)
            {
                String splittedKey1 = "";
                String splittedKey2 = "";
                if (i < splittedKeys1.length)
                {
                    splittedKey1 = splittedKeys1[i];
                }
                if (i < splittedKeys2.length)
                {
                    splittedKey2 = splittedKeys2[i];
                }
                result = this.collator.compare(splittedKey1, splittedKey2);
                if (result != 0)
                {
                    break;
                }
            }
            return result;
        }
    }

    static class StringComparator implements Comparator<String>
    {
        Collator collator = Collator.getInstance(Locale.US);

        
        public int compare(String key1, String key2)
        {
            return this.collator.compare(key1, key2);
        }
    }

    static class LocaleComparator implements Comparator<String>
    {
        Collator collator = null;

        LocaleComparator(Locale locale)
        {
            this.collator = Collator.getInstance(locale);
        }

        
        public int compare(String key1, String key2)
        {
            return this.collator.compare(key1, key2);
        }
    }

    static void addListener(Widget widget, int eventType, Listener listener)
    {
        Listener[] listeners = widget.getListeners(eventType);
        for (int i = 0; i < listeners.length; i++)
        {
            Listener currentListener = null;

            if (!(listeners[i] instanceof TypedListener))
            {
                currentListener = listeners[i];
            }
            else
            {
                TypedListener typedListener = (TypedListener) listeners[i];
                currentListener = (Listener) typedListener.getEventListener();
            }
            if (currentListener.equals(listener))
            {
                return;
            }
        }
        widget.addListener(eventType, listener);
    }

    static void addTypedListenerAndChildren(Widget widget, int eventType, TypedListener typedListener)
    {
        widget.addListener(eventType, typedListener);
        if (widget instanceof Composite)
        {
            Control[] children = ((Composite) widget).getChildren();
            for (int i = 0; i < children.length; i++)
            {
                Util.addTypedListenerAndChildren(children[i], eventType, typedListener);
            }
        }
    }

    static void addSortManager(final TableColumn tableColumn, final Comparator<String> comparator)
    {
        Listener sortListener = new Listener()
        {
            
            public void handleEvent(Event e)
            {
                if (tableColumn.isDisposed() == true)
                {
                    return;
                }
                tableColumn.getParent().setRedraw(false);
                tableColumn.getParent().setVisible(false);
                final int index = tableColumn.getParent().indexOf(tableColumn);
                if (index == -1)
                {
                    return;
                }

                tableColumn.getParent().setSortColumn(tableColumn);

                String[] fieldNames = new String[] { "strings", "images", "font", "cellFont", "checked", "grayed", "cached", "imageIndent", "background", "foreground", "cellBackground", "cellForeground", "text", "image", "state", "data", "eventTable" };
                Field[] fields = new Field[fieldNames.length];
                for (int i = 0; i < fieldNames.length; i++)
                {
                    fields[i] = Util.getField(TableItem.class, fieldNames[i]);
                }

                List<Object[]> virtualItems = new ArrayList<Object[]>(tableColumn.getParent().getItemCount());
                for (int i = 0; i < tableColumn.getParent().getItemCount(); i++)
                {
                    Object[] values = new Object[fields.length];
                    for (int j = 0; j < fields.length; j++)
                    {
                        values[j] = Util.getValue(tableColumn.getParent().getItem(i), fields[j]);
                    }
                    virtualItems.add(values);
                }
                while (tableColumn.getParent().getItemCount() > 0)
                {
                    tableColumn.getParent().getItem(0).dispose();
                }
                Collections.sort(virtualItems, new Comparator<Object[]>()
                {
                    
                    public int compare(Object[] object1, Object[] object2)
                    {
                        int result = comparator.compare(((String[]) object1[0])[index], ((String[]) object2[0])[index]);
                        if (tableColumn.getParent().getSortDirection() != SWT.DOWN)
                        {
                            if (result == -1)
                            {
                                result = 1;
                            }
                            else if (result == 1)
                            {
                                result = -1;
                            }
                        }

                        return result;
                    }
                });

                for (int i = 0; i < virtualItems.size(); i++)
                {
                    TableItem item = new TableItem(tableColumn.getParent(), SWT.NONE);
                    for (int j = 0; j < fields.length; j++)
                    {
                        Util.setValue(item, fields[j], virtualItems.get(i)[j]);
                    }

                }

                tableColumn.getParent().setSortDirection(tableColumn.getParent().getSortDirection() != SWT.DOWN ? SWT.DOWN : SWT.UP);
                tableColumn.getParent().setRedraw(true);
                tableColumn.getParent().setVisible(true);
            }
        };
        tableColumn.addListener(SWT.Selection, sortListener);

    }

    static Event createKeyEvent(Widget widget, int key)
    {
        return createKeyEvent(widget, key, (char) 0);
    }

    static Event createKeyEvent(Widget widget, int key, char c)
    {
        Event event = new Event();
        event.widget = widget;
        event.stateMask = 0;
        event.keyCode = key;
        event.character = c;
        return event;
    }

    static int getStateOfReference(String reference)
    {
        reference = (reference == null) ? "" : reference;
        int state = State.NONE;
        if (Util.isRedirectKeys(reference) == true)
        {
            state |= State.REDIRECT_KEY;
        }
        try
        {
            new URL(reference);
            state |= State.URL;
        }
        catch (MalformedURLException e)
        {
        }
        return state;
    }

    static int getStateOfValue(String reference, String value)
    {
        reference = (reference == null) ? "" : reference;
        value = (value == null) ? "" : value;
        int state = State.NONE;
        if ((!reference.equals("")) && (value.equals("")))
        {
            state = State.EMPTY;
        }
        else if ((!reference.equals("")) && (value.equals(reference)) && Util.isRedirectKeys(value) == false)
        {
            state = State.UNCHANGED;
        }
        else if ((reference.equals("")) && (!value.equals("")))
        {
            state = State.EXTRA;
        }
        return state;
    }   
    
    static String escape(String value, boolean escapeUnicode)
    {
        value = (value == null) ? "" : value;

        String resultvalue = "";
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            switch (c)
            {
                case '\n':
                    resultvalue += "\\n";
                    continue;

                case '\r':
                    resultvalue += "\\r";
                    continue;

                case '\f':
                    resultvalue += "\\f";
                    continue;

                case '\t':
                    resultvalue += "\\t";
                    continue;

                case '\\':
                    resultvalue += "\\\\";
                    continue;
            }

            if (((c < 0x0020) || (c > 0x007e)) && escapeUnicode == true)
            {
                resultvalue += '\\';
                resultvalue += 'u';
                resultvalue += Util.toHex((c >> 12) & 0xF);
                resultvalue += Util.toHex((c >> 8) & 0xF);
                resultvalue += Util.toHex((c >> 4) & 0xF);
                resultvalue += Util.toHex(c & 0xF);
            }
            else
            {
                resultvalue += c;
            }
        }
        return resultvalue;
    }

    static List<Item> getAllItems(Widget parent, List<Item> items)
    {
        List<Item> itemList = new ArrayList<Item>();
        if (items != null)
        {
            itemList.addAll(items);
        }

        Item[] childItems = null;

        if (parent instanceof ToolBar)
        {
            childItems = ((ToolBar) parent).getItems();
        }
        else if (parent instanceof Table)
        {
            childItems = ((Table) parent).getItems();
        }
        else if (parent instanceof Tree)
        {
            childItems = ((Tree) parent).getItems();
        }
        else if (parent instanceof TreeItem)
        {
            childItems = ((TreeItem) parent).getItems();
        }
        else
        {
            childItems = new Item[0];
        }

        for (int i = 0; i < childItems.length; i++)
        {
            itemList.add(childItems[i]);

            itemList = Util.getAllItems(childItems[i], itemList);
        }
        return itemList;
    }

    static Item getItem(Widget parent, Point location)
    {
        if (parent == null)
        {
            return null;
        }

        if (parent instanceof ToolBar)
        {
            return ((ToolBar) parent).getItem(location);
        }
        else if (parent instanceof Table)
        {
            return ((Table) parent).getItem(location);
        }
        else if (parent instanceof Tree)
        {
            return ((Tree) parent).getItem(location);
        }
        return null;
    }

    static List<Object[]> getTags(int offset, String value, String tag)
    {
        value = (value == null) ? "" : value;
        List<Object[]> bolds = new ArrayList<Object[]>();
        int indexOfBeginBold = -1;
        while (true)
        {
            indexOfBeginBold = value.indexOf("<" + tag + ">", indexOfBeginBold + 1);
            if (indexOfBeginBold == -1)
            {
                break;
            }
            int indexOfEndBold = value.indexOf("</" + tag + ">", indexOfBeginBold + 1);
            if (indexOfEndBold == -1)
            {
                break;
            }
            String bold = value.substring(indexOfBeginBold + 2 + tag.length(), indexOfEndBold);
            if (bold != null)
            {
                value = value.substring(0, indexOfBeginBold) + bold + value.substring(indexOfEndBold + 3 + tag.length());
                bolds.add(new Object[] { offset + indexOfBeginBold, bold.length(), bold });
            }
            indexOfBeginBold = indexOfEndBold;
        }
        bolds.add(0, new Object[] { value });
        return bolds;
    }
    static String getBundleFileName(Locale locale)
    {
        return getBundleFileName(LocalizablePluginManager.DEFAULT_NAME, locale, LocalizablePluginManager.EXTENSION);
    }

    static String getBundleFileName(String baseName, Locale locale)
    {
        return getBundleFileName(baseName, locale, LocalizablePluginManager.EXTENSION);
    }

    static String getBundleFileName(String baseName, Locale locale, String extension)
    {
        String fileName = baseName;
        String languageTag = Util.getLanguageTag(locale);
        if (languageTag.equals("und") == false)
        {
            fileName = fileName + "_" + languageTag.replace("-", "_");

        }
        return fileName + extension;
    }

    static String getLanguageTag(Locale locale)
    {
        if (locale != null && (locale.equals(Util.EMPTY_LOCALE) == false))
        {
            String languageTag = "";
            if (locale.getLanguage().equals("") == false)
            {
                languageTag = languageTag + locale.getLanguage();
                if (locale.getCountry().equals("") == false)
                {
                    languageTag = languageTag + "-" + locale.getCountry();
                    if (locale.getVariant().equals("") == false)
                    {
                        languageTag = languageTag + "-" + locale.getVariant();
                    }
                }
            }
            return languageTag;
        }
        else
        {
            return "und";
        }
    }
    static Locale getLocaleFromFilename(String filename)
    {
        if(filename.indexOf("_") == -1)
        {
            return null;
        }
        
        // detect languageTag
        String languageTag = filename.substring(filename.indexOf("_") + 1).replace('_', '-');
        Locale[] availableLocales = Locale.getAvailableLocales();
        for (int j = 0; j < availableLocales.length; j++)
        {
            if (availableLocales[j].getCountry() != "")
            {
                if (Util.getLanguageTag(availableLocales[j]).equals(languageTag) == true)
                {
                    return availableLocales[j];
                }
            }
        }
        String[] languageTagBlock = languageTag.split("-");
        String language = languageTagBlock[0];
        String country = null;
        String variant = null;
        if(languageTagBlock.length > 1)
        {
            country = languageTagBlock[1];
        }
        if(languageTagBlock.length > 2)
        {
            variant = languageTagBlock[2];
        }
        if(country == null)
        {
            return new Locale(language);            
        }
        else if( variant == null)
        {
            return new Locale(language, country);            
        }
        return new Locale(language, country, variant);  
    }

    static String getLocaleDisplay(Locale locale, boolean ForceToLocale)
    {
        String DisplayLanguage = (ForceToLocale == true ? locale.getDisplayLanguage(locale) : locale.getDisplayLanguage());
        if (DisplayLanguage != null && DisplayLanguage.equals("") == false)
        {
            DisplayLanguage = Character.toTitleCase(DisplayLanguage.charAt(0)) + DisplayLanguage.substring(1).toLowerCase(locale);
        }
        String DisplayCountry = (ForceToLocale == true ? locale.getDisplayCountry(locale) : locale.getDisplayCountry());
        if (DisplayCountry != null && DisplayCountry.equals("") == false)
        {
            DisplayCountry = Character.toTitleCase(DisplayCountry.charAt(0)) + DisplayCountry.substring(1).toLowerCase(locale);
        }
        String DisplayVariant = (ForceToLocale == true ? locale.getDisplayVariant(locale) : locale.getDisplayVariant());
        if (DisplayVariant != null && DisplayVariant.equals("") == false)
        {
            DisplayVariant = Character.toTitleCase(DisplayVariant.charAt(0)) + DisplayVariant.substring(1).toLowerCase(locale);
        }
        return DisplayLanguage + (DisplayCountry == "" ? "" : " (" + DisplayCountry + (DisplayVariant == "" ? "" : ", " + DisplayVariant) + ")");
    }

    static Image getLocaleImage(Locale locale)
    {
        Image[] images = null;
        if (locale.getCountry().equals("") == true)
        {
            images = i18nAZ.viewInstance.getImageLoader().getImages("i18nAZ.image.langs." + locale.getLanguage());
        }
        else
        {
            images = i18nAZ.viewInstance.getImageLoader().getImages("i18nAZ.image.flags." + locale.getCountry());
        }
        if (images.length == 0)
        {
            images = i18nAZ.viewInstance.getImageLoader().getImages("i18nAZ.image.noneFlag");
        }
        return images[0];
    }

    static LocaleProperties getLocaleProperties(Locale locale, URL url)
    {
        LocaleProperties LocaleProperties = new LocaleProperties(locale);
        try
        {
            LocaleProperties.load(url);
        }
        catch (IOException e)
        {
            LocaleProperties = null;
        }
        return LocaleProperties;
    }

    static Properties getProperties(URL url)
    {
        Properties properties = new Properties();
        try
        {
            InputStream inputStream = Path.openInputStream(url);           
            if (inputStream != null)
            {
                properties.load(inputStream);
                inputStream.close();
            }
        }
        catch (IOException e)
        {
            properties = null;
        }
        return properties;
    }

    static LocaleProperties getLocaleProperties(Locale locale, File file)
    {
        return Util.getLocaleProperties(locale, Path.getUrl(file));
    }

    static Properties getProperties(File file)
    {  
        return Util.getProperties(Path.getUrl(file));
    }

    static Field getField(Class<? extends Object> currentClass, String fieldName)
    {
        Field field = null;
        do
        {
            try
            {
                field = currentClass.getDeclaredField(fieldName);
                break;
            }
            catch (SecurityException e1)
            {
                currentClass = currentClass.getSuperclass();
            }
            catch (NoSuchFieldException e1)
            {
                currentClass = currentClass.getSuperclass();
            }
        }
        while (currentClass != null);
        if (field.isAccessible() == false)
        {
            field.setAccessible(true);
        }
        return field;
    }

    static Object getValue(Object object, Field field)
    {
        Object result = null;
        try
        {
            result = field.get(object);
        }
        catch (IllegalAccessException e1)
        {
        }
        catch (IllegalArgumentException e1)
        {
        }
        return result;
    }

    static String[] getReferences(String value)
    {
        List<SpellObject> references = SpellChecker.getReferences(0, value);

        List<String> Refs = new ArrayList<String>();

        for (int i = 0; i < references.size(); i++)
        {
            Refs.add((String) references.get(0).getValue());
        }
        return Refs.toArray(new String[Refs.size()]);
    }

    static String[] getReferences(String[] values)
    {
        List<String> Refs = new ArrayList<String>();
        for (int i = 0; i < values.length; i++)
        {
            Refs.addAll(Arrays.asList(Util.getReferences(values[i])));
        }
        return Refs.toArray(new String[Refs.size()]);
    }

    static Composite getNewComposite(Composite parent, int style)
    {
        return getNewComposite(parent, style, 1, SWT.BEGINNING, SWT.CENTER, false, false, SWT.DEFAULT);
    }

    static Composite getNewComposite(Composite parent, int style, int columnIndex)
    {
        return getNewComposite(parent, style, columnIndex, SWT.BEGINNING, SWT.CENTER, false, false, SWT.DEFAULT);
    }

    static Composite getNewComposite(Composite parent, int style, int horizontalAlignment, int verticalAlignment)
    {
        return getNewComposite(parent, style, 1, horizontalAlignment, verticalAlignment, false, false, SWT.DEFAULT);
    }

    static Composite getNewComposite(Composite parent, int style, int columnIndex, int horizontalAlignment, int verticalAlignment)
    {
        return getNewComposite(parent, style, columnIndex, horizontalAlignment, verticalAlignment, false, false, SWT.DEFAULT);
    }

    static Composite getNewComposite(Composite parent, int style, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace)
    {
        return getNewComposite(parent, style, 1, horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, SWT.DEFAULT);
    }

    static Composite getNewComposite(Composite parent, int style, int columnIndex, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace)
    {
        return getNewComposite(parent, style, columnIndex, horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, SWT.DEFAULT);
    }

    static Composite getNewComposite(Composite parent, int style, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int heightHint)
    {
        return getNewComposite(parent, style, 1, horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, heightHint);
    }

    static Composite getNewComposite(Composite parent, int style, int columnIndex, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int heightHint)
    {
        Composite composite = new Composite(parent, style);
        GridLayout gridLayout = new GridLayout(columnIndex, false);
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        composite.setLayout(gridLayout);

        GridData gridData = new GridData(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace);
        gridData.heightHint = heightHint;
        composite.setLayoutData(gridData);
        return composite;
    }

    static Object invoke(Object object, String methodName)
    {
        return invoke(object, methodName, null);
    }

    @SuppressWarnings("unchecked")
    static Object invoke(Object object, String methodName, Object[] params)
    {
        Class<?>[] paramTypes = null;
        if (params != null)
        {
            paramTypes = new Class[params.length];
            for (int i = 0; i < params.length; ++i)
            {
                if (params[i].getClass().getName().equals("java.lang.Byte"))
                {
                    paramTypes[i] = byte.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Short"))
                {
                    paramTypes[i] = short.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Integer"))
                {
                    paramTypes[i] = int.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Long"))
                {
                    paramTypes[i] = long.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Character"))
                {
                    paramTypes[i] = char.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Float"))
                {
                    paramTypes[i] = float.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Double"))
                {
                    paramTypes[i] = double.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Boolean"))
                {
                    paramTypes[i] = boolean.class;
                }
                else if (params[i].getClass().getName().equals("java.lang.Void"))
                {
                    paramTypes[i] = void.class;
                }
                else
                {
                    paramTypes[i] = params[i].getClass();                    
                }
            }
        }
        Class<? extends Object> currentClass = null;
        if (object instanceof Class)
        {
            currentClass = (Class<? extends Object>) object;
        }
        else
        {
            currentClass = object.getClass();
        }
        Object result = null;
        if (methodName == null)
        {
            Constructor<? extends Object> constructor = null;
            for (int i=0; i < currentClass.getDeclaredConstructors().length; i++)
            {
                if(currentClass.getDeclaredConstructors()[i].getParameterTypes().length == paramTypes.length)
                {
                    boolean correct = true;
                    
                    for (int j = 0; j < currentClass.getDeclaredConstructors()[i].getParameterTypes().length; j++)
                    {
                        if(currentClass.getDeclaredConstructors()[i].getParameterTypes()[j].isInstance(params[j]) == false)
                        {
                            correct = false;
                            break;
                        }
                    }
                    if(correct == true)
                    {
                        constructor = currentClass.getDeclaredConstructors()[i];
                        break;
                    }
                }
            }                    
            if (constructor.isAccessible() == false)
            {
                constructor.setAccessible(true);
            }
            try
            {
                result = constructor.newInstance(params);
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Method method = null;
            do
            {
                try
                {
                    method = currentClass.getDeclaredMethod(methodName, paramTypes);
                    break;
                }
                catch (SecurityException e)
                {
                    currentClass = currentClass.getSuperclass();
                }
                catch (NoSuchMethodException e)
                {
                    currentClass = currentClass.getSuperclass();
                }
            }
            while (currentClass != null);
            if (method.isAccessible() == false)
            {
                method.setAccessible(true);
            }

            try
            {
                result = method.invoke(object, params);
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
        }

        return result;
    }

    static boolean isRedirectKeys(String value)
    {
        List<SpellObject> references = SpellChecker.getReferences(0, value);
        if (references.size() == 1)
        {
            if (value.equals(references.get(0).getValue()) == true)
            {
                return true;
            }
        }
        return false;
    }
    static void openShell(Shell parent, Shell shell)
    {
        Rectangle bounds = parent.getBounds();
        Rectangle rect = shell.getBounds();

        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;

        shell.setLocation(x, y);

        shell.open();
        while (!shell.isDisposed())
        {
            if (!Display.getCurrent().readAndDispatch())
            {
                Display.getCurrent().sleep();
            }
        }
    }

    static String readInputStreamAsString(InputStream is, int size_limit, String charSet) throws IOException
    {
        StringBuffer result = new StringBuffer(1024);

        byte[] buffer = new byte[1024];

        while (true)
        {

            int len = is.read(buffer);

            if (len <= 0)
            {

                break;
            }

            result.append(new String(buffer, 0, len, charSet));

            if (size_limit >= 0 && result.length() > size_limit)
            {

                result.setLength(size_limit);

                break;
            }
        }

        return (result.toString());
    }

    static void redraw(Composite composite)
    {
        composite.setRedraw(true);
        composite.getParent().layout(true, true);
        composite.redraw();
        composite.update();
    }

    static void removeTypedListenerAndChildren(Widget widget, int eventType)
    {
        List<TypedListener> typedListeners = new ArrayList<TypedListener>();
        Listener[] listeners = widget.getListeners(eventType);
        for (int i = 0; i < listeners.length; i++)
        {
            if (!(listeners[i] instanceof TypedListener))
            {
                continue;
            }
            typedListeners.add((TypedListener) listeners[i]);
        }
        Util.removeTypedListenerAndChildren(widget, eventType, typedListeners.toArray(new TypedListener[typedListeners.size()]));
    }

    static void removeTypedListenerAndChildren(Widget widget, int eventType, TypedListener[] typedListeners)
    {
        for (int i = 0; i < typedListeners.length; i++)
        {
            widget.removeListener(eventType, typedListeners[i]);
        }
        if (widget instanceof Composite)
        {
            Control[] children = ((Composite) widget).getChildren();
            for (int i = 0; i < children.length; i++)
            {
                Util.removeTypedListenerAndChildren(children[i], eventType, typedListeners);
            }
        }
    }

    static void addLinkManager(final StyledText styledText, boolean hand, final boolean noCtrl)
    {
        styledText.setData(View.DATAKEY_TOOLTIP_HAND, hand);
        if (styledText.getData(View.DATAKEY_TOOLTIP) == null)
        {
            styledText.setData(View.DATAKEY_TOOLTIP, new ToolTip(SWTSkinFactory.getInstance().getShell().getShell(), SWT.NULL));
            ((ToolTip) styledText.getData(View.DATAKEY_TOOLTIP)).addListener(SWT.MouseExit, new Listener()
            {
                
                public void handleEvent(Event event)
                {
                    ((ToolTip) styledText.getData(View.DATAKEY_TOOLTIP)).setVisible(false);
                }

            });
        }

        Listener mouselistener = new Listener()
        {
            
            public void handleEvent(Event e)
            {

                boolean hand = (Boolean) styledText.getData(View.DATAKEY_TOOLTIP_HAND);
                ToolTip toolTip = (ToolTip) styledText.getData(View.DATAKEY_TOOLTIP);
                StyleRange styleRange = null;
                int offset = -1;
                if (e.type != SWT.KeyUp && e.type != SWT.KeyDown)
                {
                    try
                    {
                        offset = styledText.getOffsetAtLocation(new Point(e.x, e.y));
                        styleRange = styledText.getStyleRangeAtOffset(offset);
                    }
                    catch (IllegalArgumentException ie)
                    {
                    }
                }
                if (hand == false)
                {
                    styledText.setCursor(new Cursor(styledText.getDisplay(), SWT.CURSOR_IBEAM));
                }
                if (styleRange != null && styleRange.data != null)
                {
                    SpellObject spellObject = (SpellObject) styleRange.data;
                    
                    if (hand == false)
                    {
                        if (styledText.isFocusControl() == false)
                        {
                            styledText.setFocus();
                            styledText.setSelection(offset);
                        }
                    }
                    if ((e.stateMask & SWT.MOD1) != 0 || e.keyCode == SWT.MOD1 || noCtrl == true)
                    {
                        if (e.type == SWT.MouseUp)
                        {
                            Utils.launch(spellObject.getValue());
                            e.doit = false;
                        }
                        else
                        {
                            if (e.type == SWT.MouseDown)
                            {
                                e.doit = false;
                            }
                            if (hand == false)
                            {
                                styledText.setCursor(new Cursor(styledText.getDisplay(), SWT.CURSOR_HAND));
                            }
                        }
                    }
                    if ((e.stateMask & SWT.MOD1) == 0 && e.keyCode != SWT.MOD1)
                    {
                        Rectangle bounds = (Rectangle) Util.invoke(styledText, "getBoundsAtOffset", new Object[] { spellObject.getOffset() });
                        Point point = styledText.toDisplay(bounds.x, bounds.y + bounds.height);
                        if (noCtrl == true)
                        {
                            toolTip.setText(i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.FollowLink"));
                        }
                        else
                        {
                            toolTip.setText(i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.CtrlFollowLink"));
                        }
                        toolTip.setMessage(spellObject.getValue());
                        toolTip.setLocation(point);
                        toolTip.setVisible(true);
                        return;
                    }
                }

                toolTip.setVisible(false);
            }
        };
        styledText.addListener(SWT.MouseExit, mouselistener);
        styledText.addListener(SWT.MouseEnter, mouselistener);
        styledText.addListener(SWT.MouseMove, mouselistener);
        styledText.addListener(SWT.MouseUp, mouselistener);
        styledText.addListener(SWT.MouseDown, mouselistener);
        styledText.addListener(SWT.KeyUp, mouselistener);
        styledText.addListener(SWT.KeyDown, mouselistener);

    }

    static String addLocalePropertiesToZipOutputStream(ZipOutputStream zipOutputStream, LocaleProperties localeProperties, String zipPath)
    {
        String errorMessage = null;
        try
        {
            zipOutputStream.putNextEntry(new ZipEntry(zipPath));
        }
        catch (IOException e)
        {
            errorMessage = "Error Zip Entry #1";
        }

        // store automatically
        if (errorMessage == null)
        {
            try
            {
                localeProperties.store(zipOutputStream, "");
            }
            catch (IOException e)
            {
                errorMessage = "Error Zip Entry #2";
            }
        }
        return errorMessage;
    }

    static LocaleProperties loadLocaleProperties(Locale locale, URL internalFile)
    {
        URL tempFile =  Path.getUrl(Path.getPath(internalFile) + ".temp");
        LocaleProperties properties = Util.getLocaleProperties(locale, internalFile);
        LocaleProperties tempProperties = Util.getLocaleProperties(locale, tempFile);
        if (properties == null && tempProperties != null)
        {
            if (Util.saveLocaleProperties(tempProperties, internalFile) == null)
            {
                properties = tempProperties;
            }
            else
            {
                tempProperties.clear();
                tempProperties = null;
            }
        }
        if (properties != null && tempProperties != null)
        {
            Path.getFile(tempFile).delete();
            tempProperties = null;
        }

        return properties;
    }

    static String saveProperties(Properties properties, URL internalFile)
    {
        String errorMessage = null;

        // get temp filename
        URL tempFile =  Path.getUrl(Path.getPath(internalFile) + ".temp");

        // store automatically
        Path.getFile(tempFile).getParentFile().mkdirs();
        FileOutputStream output = null;
        try
        {
            output = new FileOutputStream(Path.getFile(tempFile));
        }
        catch (FileNotFoundException e1)
        {
            Path.getFile(tempFile).delete();
            errorMessage = "Error #1";
        }       
        if (output != null)
        {
            try
            {
                properties.store(output, null);
            }
            catch (IOException e)
            {
                Path.getFile(tempFile).delete();
                errorMessage = "Error #1";
            }
            try
            {
                output.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }        

        // move file
        if (errorMessage == null)
        {
            if (Path.exists(internalFile) == true)
            {
                if (Path.getFile(internalFile).delete() == false)
                {
                    errorMessage = "Error #2";
                }
            }
            if (errorMessage == null)
            {
                if (Path.getFile(tempFile).renameTo(Path.getFile(internalFile)) == false)
                {
                    errorMessage = "Error #3";
                }
            }
        }

        return errorMessage;
    }
    static String saveLocaleProperties(LocaleProperties localeProperties, URL internalFile)
    {
        String errorMessage = null;

        // get temp filename
        URL tempFile =  Path.getUrl(Path.getPath(internalFile) + ".temp");

        // store automatically
        try
        {
            localeProperties.store(tempFile);
        }
        catch (IOException e)
        {
            Path.getFile(tempFile).delete();
            errorMessage = "Error #1";
        }

        // move file
        if (errorMessage == null)
        {
            if (Path.exists(internalFile) == true)
            {
                if (Path.getFile(internalFile).delete() == false)
                {
                    errorMessage = "Error #2";
                }
            }
            if (errorMessage == null)
            {
                if (Path.getFile(tempFile).renameTo(Path.getFile(internalFile)) == false)
                {
                    errorMessage = "Error #3";
                }
            }
        }

        return errorMessage;
    }
    static void setCustomHotColor(Table table)
    {
        table.addListener(SWT.EraseItem, new Listener()
        {
            private Item itemHot = null;

            
            public void handleEvent(Event e)
            {
                if (e.widget.isDisposed() == true)
                {
                    return;
                }
                switch (e.type)
                {
                    case SWT.EraseItem:

                        // INIT
                        Rectangle originalClipping = e.gc.getClipping();

                        e.detail &= ~SWT.FOCUSED;
                        // NO HOT NO SELECTED
                        if (this.itemHot != null && this.itemHot.equals(e.item) == true && (e.detail & SWT.HOT) == 0 && e.index > 0)
                        {
                            // e.detail &= ~SWT.BACKGROUND;
                            // e.detail &= ~SWT.SELECTED;
                            e.detail |= SWT.HOT;

                        }
                        if ((e.detail & SWT.HOT) == 0 && e.index == 0)
                        {
                            this.itemHot = null;
                        }
                        if ((e.detail & SWT.HOT) == 0 && (e.detail & SWT.SELECTED) == 0)
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
                            rowClipping.width = ((Table) e.widget).getClientArea().width;
                            e.gc.setClipping(rowClipping);
                            e.gc.fillRectangle(e.x, e.y, rowClipping.width, e.height);
                        }

                        // SELECTED
                        if ((e.detail & SWT.SELECTED) != 0)
                        {
                            e.detail &= ~SWT.SELECTED;
                            e.detail &= ~SWT.BACKGROUND;
                        }

                        e.gc.setClipping(originalClipping);
                        e.detail &= ~SWT.HOT;
                        break;
                }
            }
        });
    }

    static void setWidthtHint(Control control, int widthHint)
    {
        setHeightHint(control, widthHint, 0);
    }

    static void setWidthtHint(Control control, int widthHint, int minimumWidth)
    {
        GridData gridData = null;
        if (control.getLayoutData() instanceof GridData)
        {
            gridData = (GridData) control.getLayoutData();
        }
        else
        {
            gridData = new GridData();
        }

        gridData.minimumWidth = minimumWidth;
        gridData.widthHint = widthHint;
        control.setLayoutData(gridData);
    }

    static void setHeightHint(Control control, int heightHint)
    {
        setHeightHint(control, heightHint, 0);
    }

    static void setHeightHint(Control control, int heightHint, int minimumHeight)
    {
        GridData gridData = null;
        if (control.getLayoutData() instanceof GridData)
        {
            gridData = (GridData) control.getLayoutData();
        }
        else
        {
            gridData = new GridData();
        }

        gridData.minimumHeight = minimumHeight;
        gridData.heightHint = heightHint;
        control.setLayoutData(gridData);
    }

    static void setGridData(Control control, int horizontalAlignment, int verticalAlignment)
    {
        setGridData(control, horizontalAlignment, verticalAlignment, false, false, SWT.DEFAULT, 0, SWT.DEFAULT, 0, 1, 1);
    }

    static void setGridData(Control control, int horizontalAlignment, int verticalAlignment, int horizontalSpan, int verticalSpan)
    {
        setGridData(control, horizontalAlignment, verticalAlignment, false, false, SWT.DEFAULT, 0, SWT.DEFAULT, 0, horizontalSpan, verticalSpan);
    }

    static void setGridData(Control control, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace)
    {
        setGridData(control, horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, SWT.DEFAULT, 0, SWT.DEFAULT, 0, 1, 1);
    }

    static void setGridData(Control control, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int heightHint)
    {
        setGridData(control, horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, SWT.DEFAULT, 0, heightHint, 0, 1, 1);
    }

    static void setGridData(Control control, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int widthHint, int heightHint)
    {
        setGridData(control, horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, widthHint, 0, heightHint, 0, 1, 1);
    }

    static void setGridData(Control control, int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int widthHint, int minimumWidth, int heightHint, int minimumHeight, int horizontalSpan, int verticalSpan)
    {
        GridData gridData = null;
        if (control.getLayoutData() instanceof GridData)
        {
            gridData = (GridData) control.getLayoutData();
        }
        else
        {
            gridData = new GridData();
        }
        gridData.horizontalAlignment = horizontalAlignment;
        gridData.verticalAlignment = verticalAlignment;
        gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
        gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
        gridData.horizontalSpan = horizontalSpan;
        gridData.verticalSpan = verticalSpan;
        gridData.minimumWidth = minimumWidth;
        gridData.widthHint = widthHint;
        gridData.minimumHeight = minimumHeight;
        gridData.heightHint = heightHint;
        control.setLayoutData(gridData);
    }

    static void setValue(Object object, Field field, Object value)
    {
        try
        {
            field.set(object, value);
        }
        catch (IllegalAccessException e1)
        {
        }
        catch (IllegalArgumentException e1)
        {
        }
    }

    static char toHex(int nibble)
    {
        return hexDigit[(nibble & 0xF)];
    }

    static String trimNewLine(String text)
    {
        text = (text == null) ? "" : text;

        while (text.length() > 0 && text.charAt(0) == '\n')
        {
            text = text.substring(1);
        }
        while (text.length() > 0 && text.charAt(text.length() - 1) == '\n')
        {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    static String unescape(String value)
    {
        value = (value == null) ? "" : value;

        String resultvalue = "";
        for (int i = 0; i < value.length(); i++)
        {
            char c1 = value.charAt(i);
            if (c1 == '\\')
            {
                i++;
                if (i >= value.length())
                {
                    break;
                }

                char c2 = value.charAt(i);

                switch (c2)
                {
                    case 'n':
                        resultvalue += "\n";
                        continue;

                    case 'r':
                        resultvalue += "\r";
                        continue;

                    case 'f':
                        resultvalue += "\f";
                        continue;

                    case 't':
                        resultvalue += "\t";
                        continue;

                    case '\\':
                        resultvalue += "\\";
                        continue;

                    case 'u':
                        int unicode = 0;
                        boolean isMalformed = false;
                        for (int j = 0; j < 4 && i < value.length(); j++)
                        {
                            i++;
                            char c3 = value.charAt(i);
                            switch (c3)
                            {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    unicode = (unicode << 4) + c3 - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    unicode = (unicode << 4) + 10 + c3 - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    unicode = (unicode << 4) + 10 + c3 - 'A';
                                    break;
                                default:
                                    isMalformed = true;
                            }
                        }
                        if (isMalformed == false)
                        {
                            resultvalue += (char) unicode;
                        }
                        continue;
                }
                resultvalue += c1 + c2;
                continue;
            }
            resultvalue += c1;
        }
        return resultvalue;
    }
    static int findEscape(String value, int offset, String[] result)
    {
        value = (value == null) ? "" : value;

        for (int i = offset; i < value.length(); i++)
        {
            char c1 = value.charAt(i);
            if (c1 == '\\')
            {
                i++;
                if (i >= value.length())
                {
                    break;
                }

                char c2 = value.charAt(i);
                switch (c2)
                {
                    case 'n':
                        result[0] = "\n";
                        result[1] = "\\n";
                        return i;

                    case 'r':
                        result[0] = "\r";
                        result[1] = "\\r";
                        return i;

                    case 'f':
                        result[0] = "\f";
                        result[1] = "\\f";
                        return i;

                    case 't':
                        result[0] = "\t";
                        result[1] = "\\t";
                        return i;

                    case '\\':
                        result[0] = "\\";
                        result[1] = "\\\\";
                        return i;

                    case 'u':
                        int unicode = 0;
                        String sunicode = "u";
                        for (int j = 0; j < 4 && i < value.length(); j++)
                        {
                            i++;
                            char c3 = value.charAt(i);
                            sunicode += c3; 
                            switch (c3)
                            {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    unicode = (unicode << 4) + c3 - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    unicode = (unicode << 4) + 10 + c3 - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    unicode = (unicode << 4) + 10 + c3 - 'A';
                                    break;
                                default:
                            }
                        }
                        result[0] = Character.toString((char) unicode);
                        result[1] = sunicode;
                        return i;
                }
            }            
        }
        return -1;
    }
    public static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
        }        
    }
}
