/*
 * Util.java
 *
 * Created on February 24, 2004, 12:00 PM
 */

package i18nAZ;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.JarFile;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * Misc Usefull functions.
 * 
 * @author Repris d'injustice
 */
class Util
{
    private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

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
        int state = View.State.NONE;
        if (Util.isRedirectKeys(reference) == true)
        {
            state |= View.State.REDIRECT_KEY;
        }
        try
        {
            new URL(reference);
            state |= View.State.URL;
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
        int state = View.State.NONE;
        if ((!reference.equals("")) && (value.equals("")))
        {
            state = View.State.EMPTY;
        }
        else if ((!reference.equals("")) && (value.equals(reference)) && Util.isRedirectKeys(value) == false)
        {
            state = View.State.UNCHANGED;
        }
        else if ((reference.equals("")) && (!value.equals("")))
        {
            state = View.State.EXTRA;
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
    static File getFileFromUrl(URL Url)
    {
        if (Url == null)
        {
            return null;
        }
        String FilePath = Url.getFile();
        try
        {
            FilePath = URLDecoder.decode(FilePath, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
        }
        FilePath = FilePath.replace('/', '\\');
        FilePath = FilePath.replace('|', ':');
        return new File(FilePath);
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
        String DisplayScript = (ForceToLocale == true ? locale.getDisplayScript(locale) : locale.getDisplayScript());
        if (DisplayScript != null && DisplayScript.equals("") == false)
        {
            DisplayScript = Character.toTitleCase(DisplayScript.charAt(0)) + DisplayScript.substring(1).toLowerCase(locale);
        }
        String DisplayVariant = (ForceToLocale == true ? locale.getDisplayVariant(locale) : locale.getDisplayVariant());
        if (DisplayVariant != null && DisplayVariant.equals("") == false)
        {
            DisplayVariant = Character.toTitleCase(DisplayVariant.charAt(0)) + DisplayVariant.substring(1).toLowerCase(locale);
        }
        return DisplayLanguage + (DisplayCountry == "" ? "" : " (" + (DisplayScript == "" ? "" : DisplayScript + ", ") + DisplayCountry + (DisplayVariant == "" ? "" : ", " + DisplayVariant) + ")");
    }

    static CommentedProperties getLocaleProperties(URL url)
    {
        CommentedProperties commentedProperties = new CommentedProperties();
        try
        {
            commentedProperties.load(url);
        }
        catch (IOException e)
        {
            commentedProperties = null;
        }
        return commentedProperties;
    }

    static CommentedProperties getLocaleProperties(File file)
    {
        CommentedProperties commentedProperties = null;
        if (file.isFile() == true && file.exists() == true)
        {
            URL url = null;
            try
            {
                url = file.toURI().toURL();
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            if (url != null)
            {
                commentedProperties = Util.getLocaleProperties(url);
            }
        }
        return commentedProperties;
    }

    static List<int[]> getParams(int offset, String value)
    {
        value = (value == null) ? "" : value;
        List<int[]> params = new ArrayList<int[]>();
        int indexOfPercent = -1;
        while (true)
        {
            indexOfPercent = value.indexOf('%', indexOfPercent + 1);
            if (indexOfPercent == -1 || indexOfPercent == value.length() - 1)
            {
                break;
            }
            int integer = -1;
            int paramLength = -1;
            for (int i = indexOfPercent + 1; i < value.length(); i++)
            {
                try
                {
                    integer = Integer.parseInt(value.substring(indexOfPercent + 1, i + 1));
                    paramLength = 1 + i - indexOfPercent;
                }
                catch (NumberFormatException nfe)
                {
                    break;
                }
            }
            if (integer != -1)
            {
                params.add(new int[] { offset + indexOfPercent, paramLength, integer });
            }
        }
        return params;
    }

    static PluginInterface[] getPluginInterfaces()
    {        
        ArrayList<PluginInterface> ReturnPluginInterface = new ArrayList<PluginInterface>();
        ReturnPluginInterface.add(null);

        PluginInterface[] pluginInterfaces = i18nAZ.viewInstance.getPluginInterface().getPluginManager().getPluginInterfaces();
        for (int i = 0; i < pluginInterfaces.length; i++)
        {
            BundleObject bundleObject = new BundleObject(pluginInterfaces[i]);
            if (bundleObject.isValid() == true)
            {
                ReturnPluginInterface.add(pluginInterfaces[i]);
            }
        }

        Collections.sort(ReturnPluginInterface, new Comparator<PluginInterface>()
        {
            @Override
            public int compare(PluginInterface pluginInterface1, PluginInterface pluginInterface2)
            {
                String s1 = "";
                String s2 = "";
                if (pluginInterface1 != null)
                {
                    Properties PluginProperties = pluginInterface1.getPluginProperties();
                    String LocalizedKey = "Views.plugins." + PluginProperties.getProperty("plugin.id") + ".title";
                    if (i18nAZ.viewInstance.getPluginInterface().getUtilities().getLocaleUtilities().hasLocalisedMessageText(LocalizedKey) == true)
                    {
                        s1 = i18nAZ.viewInstance.getLocalisedMessageText(LocalizedKey);
                    }
                    else
                    {
                        s1 = PluginProperties.getProperty("plugin.name");
                    }
                }
                if (pluginInterface2 != null)
                {
                    Properties PluginProperties = pluginInterface2.getPluginProperties();
                    String LocalizedKey = "Views.plugins." + PluginProperties.getProperty("plugin.id") + ".title";
                    if (i18nAZ.viewInstance.getPluginInterface().getUtilities().getLocaleUtilities().hasLocalisedMessageText(LocalizedKey) == true)
                    {
                        s2 = i18nAZ.viewInstance.getLocalisedMessageText(LocalizedKey);
                    }
                    else
                    {
                        s2 = PluginProperties.getProperty("plugin.name");
                    }
                }
                return s1.toString().compareToIgnoreCase(s2);
            }
        });

        return ReturnPluginInterface.toArray(new PluginInterface[ReturnPluginInterface.size()]);
    }

    static List<Object[]> getReferences(int offset, String value)
    {
        value = (value == null) ? "" : value;
        List<Object[]> references = new ArrayList<Object[]>();
        int indexOfPercent = -1;
        while (true)
        {
            indexOfPercent = value.indexOf('{', indexOfPercent + 1);
            if (indexOfPercent == -1 || indexOfPercent == value.length() - 1)
            {
                break;
            }
            String reference = null;
            for (int i = indexOfPercent + 1; i < value.length(); i++)
            {
                if (value.charAt(i) == '}')
                {
                    boolean IsNotRef = false;
                    for (int j = indexOfPercent + 1; j < i; j++)
                    {
                        char c = value.charAt(j);
                        if (c == '#' || c == '.' || c == '_' || c == '%' || c == '-' || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || (c >= 48 && c <= 57))
                        {
                            continue;
                        }
                        IsNotRef = true;
                        break;
                    }
                    if (IsNotRef == false)
                    {
                        reference = value.substring(indexOfPercent, i + 1);
                    }
                    break;
                }
            }
            if (reference != null)
            {
                references.add(new Object[] { offset + indexOfPercent, reference.length(), reference });
            }
        }
        return references;
    }

    static String[] getReferences(String value)
    {
        List<Object[]> references = getReferences(0, value);

        List<String> Refs = new ArrayList<String>();

        for (int i = 0; i < references.size(); i++)
        {
            Refs.add((String) references.get(0)[2]);
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

    static Object invoke(Object object, String methodName)
    {
        return invoke(object, methodName, null);
    }

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
        Method method = null;
        try
        {
            method = object.getClass().getMethod(methodName, paramTypes);
        }
        catch (NoSuchMethodException | SecurityException e1)
        {
        }
        Object result = null;
        try
        {
            result = method.invoke(object, params);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1)
        {
        }
        return result;
    }

    static boolean isRedirectKeys(String value)
    {
        List<Object[]> references = getReferences(0, value);
        if (references.size() == 1)
        {
            String reference = (String) references.get(0)[2];
            if (value.equals(reference) == true)
            {
                return true;
            }
        }
        return false;
    }

    static boolean jarEntryExists(URL url)
    {
        URLConnection connection = null;
        try
        {
            connection = url.openConnection();
        }
        catch (IOException e)
        {
        }
        if (connection instanceof JarURLConnection)
        {
            JarFile jarFile = null;
            try
            {
                jarFile = ((JarURLConnection) connection).getJarFile();
            }
            catch (IOException e)
            {
            }
            return jarFile != null;
        }
        else if (url.getProtocol().equals("file") == true)
        {
            File file = Util.getFileFromUrl(url);
            return file != null && file.isFile() && file.exists();
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
    static void removeTypedListenerAndChildren(Widget widget, int eventType)
    {
        List<TypedListener> typedListeners = new  ArrayList<TypedListener>();
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
        for(int i = 0; i < typedListeners.length; i++)
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

    static String saveLocaleProperties(CommentedProperties localeProperties, File localeFile)
    {
        String errorMessage = null;
       // boolean isError = false;

        // get temp filename
        File tempFile = new File(localeFile.getAbsolutePath() + ".temp");

        // store automatically
        try
        {
            localeProperties.store(tempFile);
        }
        catch (IOException e)
        {
            tempFile.delete();
            errorMessage = "Error #1";
        }

        // move file
        if (errorMessage == null)
        {
            if (localeFile.exists() && localeFile.isFile())
            {
                if (localeFile.delete() == false)
                {
                    errorMessage = "Error #2";
                }
            }
            if (errorMessage == null)
            {
                if (tempFile.renameTo(localeFile) == false)
                {
                    errorMessage = "Error #3";
                }
            }
        }

        return errorMessage;
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
}
