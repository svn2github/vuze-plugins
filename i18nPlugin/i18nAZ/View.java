/*
 * Util.java
 *
 * Created on February 24, 2004, 12:00 PM
 */
package i18nAZ;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.ToolTip;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.IMenuConstants;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;

import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText2;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;

/**
 * View.java
 * 
 * @author Repris d'injustice
 */
class View implements UISWTViewCoreEventListener
{
    static final String VIEWID = "i18nAZ";

    static final String DATAKEY_LOCALE = "locale";
    static final String DATAKEY_LANGUAGE_TAG = "languageTag";
    static final String DATAKEY_FILE = "file";
    static final String DATAKEY_SELECTED_ROW = "selectedRow";
    static final String DATAKEY_SELECTED_COLUMN = "selectedColumn";
    static final String DATAKEY_DEFAULT_STYLES = "styles";
    static final String DATAKEY_TEXT_ID = "textId";
    static final String DATAKEY_TOOLTIP = "toolTip";
    static final String DATAKEY_TOOLTIP_HAND = "toolTipHand";

    static final String AZUREUS_LANG_FILE = "org.gudy.azureus2.internat.MessagesBundle";

    static String AZUREUS_PLUGIN_NAME = "(core)";

    private boolean isCreated = false;

    private ImageLoader imageLoader = SWTSkinFactory.getInstance().getImageLoader(SWTSkinFactory.getInstance().getSkinProperties());

    private Display display = null;

    private PluginInterface pluginInterface = null;
    private PluginInterface[] pluginInterfaces = null;

    private LoggerChannel loggerChannel = null;

    private SWTSkinButtonUtility addLanguageButton = null;
    private SWTSkinButtonUtility exportLanguageButton = null;
    public SWTSkinButtonUtility removeLanguageButton = null;

    private SWTSkinButtonUtility emptyFilterButton = null;
    private SWTSkinButtonUtility unchangedFilterButton = null;
    private SWTSkinButtonUtility extraFilterButton = null;
    private SWTSkinButtonUtility multilineEditorButton = null;
    private SWTSkinButtonUtility treeModeButton = null;
    private SWTSkinButtonUtility urlsFilterButton = null;
    private SWTSkinButtonUtility redirectKeysFilterButton = null;
    private SWTSkinButtonUtility helpButton = null;

    private SWTSkinObjectText2 infoText = null;
    private SWTSkinObjectTextbox searchTextbox = null;

    private Combo pluginsCombo = null;

    private ToolItem undoToolItem = null;
    private ToolItem redoToolItem = null;
    private ToolItem cutToolItem = null;
    private ToolItem copyToolItem = null;
    private ToolItem pasteToolItem = null;
    private ToolItem selectAllToolItem = null;
    private ToolItem upperCaseToolItem = null;
    private ToolItem lowerCaseToolItem = null;
    private ToolItem firstCaseToolItem = null;
    private ToolItem trademarkToolItem = null;
    private ToolItem validateToolItem = null;
    private ToolItem cancelToolItem = null;

    private ToolBar toolBar = null;

    private StyledText infoStyledText = null;
    private StyledText editorStyledText = null;

    private Label statusLabel = null;

    private BundleObject currentBundleObject;
    private String defaultPath;

    boolean emptyFilter = false;
    boolean unchangedFilter = false;
    boolean extraFilter = false;

    boolean redirectKeysFilter = false;
    int urlsFilter = 0;

    private boolean multilineEditor = false;

    private ArrayList<String> keys = new ArrayList<String>();

    List<LocalesProperties> localesProperties = null;

    Map<Pattern, Object> searchPatterns = null;
    private HashSet<String> searchPrefixes = null;
    private boolean regexSearch = false;

    Map<String, Item> deletableRows = new HashMap<String, Item>();

    private PluginInterface selectedPluginInterface = null;

    private Clipboard clipboard = null;

    private List<int[]> infoParams = null;
    private List<Object[]> infoReferences = null;
    private List<Object[]> infoUrls = null;

    private List<int[]> editorParams = null;
    private List<Object[]> editorReferences = null;
    private List<Object[]> editorUrls = null;

    HashSet<String> emptyFilterExcludedKey = new HashSet<String>();
    HashSet<String> unchangedFilterExcludedKey = new HashSet<String>();
    HashSet<String> extraFilterExcludedKey = new HashSet<String>();

    HashSet<String> hideRedirectKeysFilterExcludedKey = new HashSet<String>();
    Map<String, Integer> urlsFilterOverriddenStates = new HashMap<String, Integer>();

    private Thread saveThread = null;

    private List<SaveObject> saveObjects = new ArrayList<SaveObject>();

    private MenuItem editMenu = null;
    private Menu dropDownMenu = null;

    UndoRedo undoRedo = null;

    class LocalesProperties
    {
        Locale locale = null;
        CommentedProperties commentedProperties = null;

        LocalesProperties(Locale locale)
        {
            this.locale = locale;
            this.commentedProperties = null;
        }
    }

    class State
    {
        static final int NONE = 0;
        static final int EMPTY = 1;
        static final int UNCHANGED = 2;
        static final int EXTRA = 4;
        static final int URL = 8;
        static final int REDIRECT_KEY = 16;
    }

    class MenuOptions
    {
        static final int NONE = 0;
        static final int ROW_COPY = 1;
        static final int REMOVE_COLUMN = 2;
        static final int EDITOR = 4;
        static final int FILTERS = 8;
        static final int TOPFILTERS = 16;
        static final int SEARCH = 32;
        static final int OPEN_URL = 64;
    }

    class SaveObject
    {
        private CommentedProperties commentedProperties = null;
        private File file = null;
        private String currentKey = null;

        public SaveObject(CommentedProperties commentedProperties, File file, String currentKey)
        {
            this.commentedProperties = (CommentedProperties) commentedProperties.clone();
            this.file = new File(file.getAbsolutePath());
            this.currentKey = currentKey;
        }

        public CommentedProperties getCommentedProperties()
        {
            return this.commentedProperties;
        }

        public File getFile()
        {
            return this.file;
        }

        public String getCurrentKey()
        {
            return this.currentKey;
        }

        public void dispose()
        {
            this.commentedProperties.clear();
            this.commentedProperties = null;
            this.file = null;
        }
    }

    // constructor
    View(PluginInterface pluginInterface)
    {
        this.pluginInterface = pluginInterface;

        View.AZUREUS_PLUGIN_NAME = this.getLocalisedMessageText("i18nAZ.Labels.DefaultPlugin");

        this.loggerChannel = pluginInterface.getLogger().getChannel("i18nEditor");
        this.loggerChannel.log(1, "i18nEditor View Startup");
        try
        {
            this.defaultPath = pluginInterface.getUtilities().getAzureusProgramDir();
        }
        catch (Exception e)
        {
            this.defaultPath = "";
        }

        this.localesProperties = new ArrayList<LocalesProperties>();
        this.localesProperties.add(new LocalesProperties(new Locale("", "")));

        List<?> original_list = COConfigurationManager.getListParameter("i18nAZ.LocalesSelected", new ArrayList<String>());
        List<?> list = BDecoder.decodeStrings(BEncoder.cloneList(original_list));

        Locale[] AvailableLocales = Locale.getAvailableLocales();
        for (int i = 0; i < list.size(); i++)
        {
            for (int j = 0; j < AvailableLocales.length; j++)
            {
                if (list.get(i).equals(AvailableLocales[j].toLanguageTag()))
                {
                    this.localesProperties.add(new LocalesProperties(AvailableLocales[j]));
                    break;
                }
            }
        }

        // HEADER
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.attach.bottom", "");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.attach.template", "template.fill");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.color", "{color.library.header}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.height", "26");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.propogate", "1");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.type", "container");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.widgets", "mdientry.toolbar.full");

        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.align", "left");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.attach.bottom", "100,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.attach.left", "5,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.attach.right", "i18nAZ.main.header.search,-5,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.attach.template", "template.fill");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.text.shadow", "#FFFFFF80");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.text.style", "bold,shadow");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.type", "text");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.view", "i18nAZ-info");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.info.v-align", "center");

        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.attach.left", "");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.attach.right", "100,-10");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.attach.top", "0,center");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.message", "{i18nAZ.Labels.Filter}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.style", "search");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.type", "textbox");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.view", "i18nAZ-search");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.main.header.search.width", "150");

        // TOOLBAR
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.all.attach.top", "0,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.all.cursor", "hand");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.all.propogate", "1");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.all.type", "container");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.all.widgets", "toolbar.area.sitem.a.imagearea");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.attach.bottom", "100,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.attach.left", "0,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.attach.right", "100,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.background", "{image.toolbar.2nd.lr-bg}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.background.drawmode", "tile-x");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.background-down", "{image.toolbar.2nd.lr-bg-down}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.background-over", "{image.toolbar.2nd.lr-bg}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.height", "{button.height}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.minwidth", "{button.width}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.type", "container");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.sitem.a.imagearea.widgets", "toolbar.area.sitem.image");

        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.attach.top", "0,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.cursor", "hand");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.propogate", "1");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.type", "container");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.widgets", "toolbar.area.vitem.imagearea");

        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.attach.bottom", "100,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.attach.left", "0,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.attach.right", "100,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.background", "{i18nAZ.image.toolbar.2nd-view.m-bg}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.background.drawmode", "tile-x");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.background-over", "{i18nAZ.image.toolbar.2nd-view.m-bg}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.background-over-selected", "{i18nAZ.image.toolbar.2nd-view.m-bg-down}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.background-selected", "{i18nAZ.image.toolbar.2nd-view.m-bg-down}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.height", "{button.height}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.minwidth", "32");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.type", "container");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.imagearea.widgets", "toolbar.area.sitem.image");

        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.all.attach.top", "0,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.all.cursor", "hand");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.all.propogate", "1");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.all.type", "container");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.all.widgets", "toolbar.area.vitem.a.imagearea");

        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.attach.bottom", "100,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.attach.left", "0,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.attach.right", "100,0");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.background", "{i18nAZ.image.toolbar.2nd-view.lr-bg}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.background.drawmode", "tile-x");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.background-over", "{i18nAZ.image.toolbar.2nd-view.lr-bg}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.background-over-selected", "{i18nAZ.image.toolbar.2nd-view.lr-bg-down}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.background-selected", "{i18nAZ.image.toolbar.2nd-view.lr-bg-down}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.height", "{button.height}");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.minwidth", "32");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.type", "container");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("toolbar.area.vitem.a.imagearea.widgets", "toolbar.area.sitem.image");

        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.image.toolbar.2nd-view.m-bg", "{template.imagedir}/tb/view_r_l.png,{template.imagedir}/tb/view_r_m.png,{template.imagedir}/tb/view_l_r.png");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.image.toolbar.2nd-view.m-bg-down", "{template.imagedir}/tb/view_r_l_down.png,{template.imagedir}/tb/view_r_m_down.png,{template.imagedir}/tb/view_l_r_down.png");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.image.toolbar.2nd-view.lr-bg", "{template.imagedir}/tb/view_l_l.png,{template.imagedir}/tb/view_r_m.png,{template.imagedir}/tb/view_r_r.png");
        SWTSkinFactory.getInstance().getSkinProperties().addProperty("i18nAZ.image.toolbar.2nd-view.lr-bg-down", "{template.imagedir}/tb/view_l_l_down.png,{template.imagedir}/tb/view_r_m_down.png,{template.imagedir}/tb/view_r_r_down.png");

        this.imageLoader.addSkinProperties(new SkinPropertiesImpl(pluginInterface.getPluginClassLoader(), "images", "images.properties"));

    }

    private SWTSkinButtonUtility addButton(SWTSkinObjectContainer ToolBarContainer, String Id, boolean checked, String Align, String ImageId, final String TextID, int Offset, Control LastControl)
    {
        return this.addButton(ToolBarContainer, Id, checked == true ? 1 : 0, Align, ImageId, TextID, Offset, LastControl);
    }

    private SWTSkinButtonUtility addButton(SWTSkinObjectContainer ToolBarContainer, String Id, String Align, String ImageId, final String TextID, int Offset, Control LastControl)
    {
        return this.addButton(ToolBarContainer, Id, -1, Align, ImageId, TextID, Offset, LastControl);
    }

    private SWTSkinButtonUtility addButton(SWTSkinObjectContainer toolBarContainer, String id, int checked, String align, String imageId, final String textID, int offset, Control lastControl)
    {
        final SWTSkinButtonUtility button = new SWTSkinButtonUtility(SWTSkinFactory.getInstance().createSkinObject("toolbar:" + id, "toolbar.area." + ((checked != -1) ? "vitem" : "sitem") + ((align.equals("") == true) ? "" : "." + align), toolBarContainer), "toolbar-item-image");
        button.setImage(imageId);
        button.setTextID(textID);
        button.getSkinObject().getControl().setData(View.DATAKEY_TEXT_ID, textID);
        if (checked == -1)
        {
            ToolTipText.set(button.getSkinObject().getControl(), textID);
        }
        else
        {
            this.checkButton(button, checked == 1);
            button.addSelectionListener(new ButtonListenerAdapter()
            {
                @Override
                public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
                {
                    View.this.checkButton(button);
                }
            });
        }
        FormData formData = (FormData) button.getSkinObject().getControl().getLayoutData();
        if (lastControl == null)
        {
            formData.left = new FormAttachment(offset);
        }
        {
            formData.left = new FormAttachment(lastControl, offset);
        }

        lastControl = button.getSkinObject().getControl();
        return button;
    }

    private void addLanguage()
    {
        TreeTableManager.getCurrent().setFocus();

        AddLanguageDialog addLanguageDialog = new AddLanguageDialog(SWTSkinFactory.getInstance().getShell().getShell());
        if (addLanguageDialog.localesSelected != null)
        {
            for (int i = 0; i < addLanguageDialog.localesSelected.length; i++)
            {                
                this.localesProperties.add(new LocalesProperties(addLanguageDialog.localesSelected[i]));
                try
                {
                    this.addLocaleColumn(addLanguageDialog.localesSelected[i], addLanguageDialog.getLocalBundleObject());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            List<String> Locales = new ArrayList<String>();
            for (int i = 0; i < this.localesProperties.size(); i++)
            {
                Locale locale = this.localesProperties.get(i).locale;
                if ((locale.toLanguageTag() != null) && (!locale.toLanguageTag().equals("")) && (!locale.toLanguageTag().equals("und")))
                {
                    Locales.add(locale.toLanguageTag());
                }
            }
            COConfigurationManager.setParameter("i18nAZ.LocalesSelected", Locales);
            COConfigurationManager.save();

            this.updateTreeTable();

            TreeTableManager.getCurrent().setFocus();
        }
    }

    private void addLinkManager(final StyledText styledText, boolean hand)
    {
        styledText.setData(View.DATAKEY_TOOLTIP_HAND, hand);
        if (styledText.getData(View.DATAKEY_TOOLTIP) == null)
        {
            styledText.setData(View.DATAKEY_TOOLTIP, new ToolTip(SWTSkinFactory.getInstance().getShell().getShell(), SWT.NULL));
            ((ToolTip) styledText.getData(View.DATAKEY_TOOLTIP)).addListener(SWT.MouseExit, new Listener()
            {
                @Override
                public void handleEvent(Event event)
                {
                    ((ToolTip) styledText.getData(View.DATAKEY_TOOLTIP)).setVisible(false);
                }

            });
        }

        Listener mouselistener = new Listener()
        {
            @Override
            public void handleEvent(Event e)
            {

                boolean hand = (boolean) styledText.getData(View.DATAKEY_TOOLTIP_HAND);
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
                    styledText.setCursor(new Cursor(View.this.display, SWT.CURSOR_IBEAM));
                }
                if (styleRange != null && styleRange.data != null)
                {
                    if (hand == false)
                    {
                        if (styledText.isFocusControl() == false)
                        {
                            styledText.setFocus();
                            styledText.setSelection(offset);
                        }
                    }
                    if ((e.stateMask & SWT.MOD1) != 0 || e.keyCode == SWT.MOD1)
                    {
                        if (e.type == SWT.MouseUp)
                        {
                            Utils.launch((String) ((Object[]) styleRange.data)[2]);
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
                                styledText.setCursor(new Cursor(View.this.display, SWT.CURSOR_HAND));
                            }
                        }
                    }
                    else
                    {

                        Rectangle bounds = (Rectangle) Util.invoke(styledText, "getBoundsAtOffset", new Object[] { (int) ((Object[]) styleRange.data)[0] });
                        Point point = styledText.toDisplay(bounds.x, bounds.y + bounds.height);
                        toolTip.setText(View.this.getLocalisedMessageText("i18nAZ.ToolTips.FollowLink"));
                        toolTip.setMessage((String) ((Object[]) styleRange.data)[2]);
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

    private void addLocaleColumn(Locale locale, BundleObject bundleObject)
    {
        String localizedBundleName = this.currentBundleObject.getName();
        if (TreeTableManager.getColumnCount() > 1)
        {
            localizedBundleName = localizedBundleName + "_" + locale.toLanguageTag().replace('-', '_');
        }
        File localFile = new File(this.getPluginInterface().getPluginDirectoryName().toString() + "\\internat\\" + this.currentBundleObject.getPluginName() + "\\" + localizedBundleName + BundleObject.EXTENSION);
        File tempFile = new File(localFile.getAbsolutePath() + ".temp");

        CommentedProperties localeProperties = Util.getLocaleProperties(localFile);
        CommentedProperties tempProperties = Util.getLocaleProperties(tempFile);

        if (localeProperties == null && tempProperties == null)
        {
            String OriginalLocalizedBundleName = bundleObject.getName();
            if (TreeTableManager.getColumnCount() > 1)
            {
                OriginalLocalizedBundleName = OriginalLocalizedBundleName + "_" + locale.toLanguageTag().replace('-', '_');
            }
            URL OriginalBundleURL = null;
            try
            {
                OriginalBundleURL = new URL(bundleObject.getUrl().toString() + OriginalLocalizedBundleName + BundleObject.EXTENSION);
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            if (OriginalBundleURL != null && Util.jarEntryExists(OriginalBundleURL))
            {
                localeProperties = Util.getLocaleProperties(OriginalBundleURL);
                if (localeProperties != null && localeProperties.IsLoaded() == true)
                {
                    Util.saveLocaleProperties(localeProperties, localFile);
                }
            }
        }
        if (localeProperties == null && tempProperties != null)
        {
            if (Util.saveLocaleProperties(tempProperties, localFile) == null)
            {
                localeProperties = tempProperties;
            }
            else
            {
                tempProperties.clear();
                tempProperties = null;
            }
        }
        if (localeProperties != null && tempProperties != null)
        {
            tempFile.delete();
            tempProperties = null;
        }

        if (localeProperties != null && localeProperties.IsLoaded() == true)
        {
            for (Iterator<String> iterator = localeProperties.stringPropertyNames().iterator(); iterator.hasNext();)
            {
                String key = iterator.next();
                if (this.keys.contains(key) == false)
                {
                    this.keys.add(key);
                }
            }
        }
        if (localeProperties == null)
        {
            localeProperties = new CommentedProperties();
        }
        this.getLocalesProperties(locale).commentedProperties = localeProperties;
        String headerText = "";
        String headerLanguageTag = "";
        int width = COConfigurationManager.getIntParameter("i18nAZ.columnWidth." + TreeTableManager.getColumnCount(), 200);
        if (TreeTableManager.getColumnCount() == 1)
        {
            headerText = this.getLocalisedMessageText("i18nAZ.Columns.Reference");
        }
        else
        {
            headerText = Util.getLocaleDisplay(locale, false);
            headerLanguageTag = locale.toLanguageTag().replace("-", "");
        }
        Item column = TreeTableManager.addColumn(headerText, width);
        column.setData(View.DATAKEY_LOCALE, locale);
        column.setData(View.DATAKEY_FILE, localFile);
        column.setData(View.DATAKEY_LANGUAGE_TAG, headerLanguageTag);
    }

    private SWTSkinObject addSeparator(SWTSkinObjectContainer ToolBarContainer, Control LastControl)
    {
        SWTSkinObject so = SWTSkinFactory.getInstance().createSkinObject("toolbar_sep" + Math.random(), "toolbar.area.sitem.sep", ToolBarContainer);
        FormData formData = (FormData) so.getControl().getLayoutData();
        formData.left = new FormAttachment(LastControl);
        return so;
    }

    private void cancel()
    {
        View.this.updateStyledTexts();
        TreeTableManager.Cursor.cancelfocusedRow();
        this.selectEditor();
    }

    private void checkButton(SWTSkinButtonUtility button)
    {
        boolean checked = (boolean) button.getSkinObject().getData("checked") == false;
        View.this.checkButton(button, checked);
    }

    private void checkButton(SWTSkinButtonUtility button, boolean checked)
    {
        String textID = (String) button.getSkinObject().getControl().getData(View.DATAKEY_TEXT_ID);
        button.getSkinObject().switchSuffix(checked ? "-selected" : "", 4, false);
        button.getSkinObject().setData("checked", checked);
        ToolTipText.set(button.getSkinObject().getControl(), textID + ((checked) ? ".Pressed" : ""));
    }

    private void createTopLevelMenuitem()
    {
        if (MenuFactory.findMenuItem(SWTSkinFactory.getInstance().getShell().getShell().getMenuBar(), "i18nAZ.Menus.Edit") == null)
        {
            this.dropDownMenu = new Menu(SWTSkinFactory.getInstance().getShell().getShell(), SWT.DROP_DOWN);
            this.dropDownMenu.setData(IMenuConstants.KEY_MENU_ID, "i18nAZ.Menus.Edit");

            this.editMenu = new MenuItem(SWTSkinFactory.getInstance().getShell().getShell().getMenuBar(), SWT.CASCADE, 1);
            this.editMenu.setData(IMenuConstants.KEY_MENU_ID, this.dropDownMenu.getData(IMenuConstants.KEY_MENU_ID));
            Messages.setLanguageText(this.editMenu, (String) this.dropDownMenu.getData(IMenuConstants.KEY_MENU_ID));
            this.editMenu.setMenu(this.dropDownMenu);

            this.dropDownMenu.addMenuListener(new MenuListener()
            {

                @Override
                public void menuHidden(MenuEvent e)
                {
                }

                @Override
                public void menuShown(MenuEvent e)
                {
                    if (e.widget != null && e.widget.equals(View.this.dropDownMenu) == true)
                    {

                        int visible = MenuOptions.SEARCH | MenuOptions.TOPFILTERS | MenuOptions.EDITOR | MenuOptions.ROW_COPY | MenuOptions.OPEN_URL;
                        int enabled = MenuOptions.SEARCH | MenuOptions.TOPFILTERS;

                        if (TreeTableManager.Cursor.isFocusControl() == true && (boolean) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_EXIST) == true)
                        {
                            enabled |= MenuOptions.ROW_COPY;
                        }

                        if (View.this.editorStyledText.isFocusControl() == true)
                        {
                            enabled |= MenuOptions.EDITOR;
                            int[] states = (int[]) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_STATES);
                            if ((states[1] & State.URL) != 0)
                            {
                                try
                                {
                                    new URL(TreeTableManager.getText(TreeTableManager.Cursor.getRow(), TreeTableManager.Cursor.getColumn()));
                                    enabled |= MenuOptions.OPEN_URL;
                                }
                                catch (MalformedURLException me)
                                {
                                }
                            }
                        }

                        View.this.populateMenu(View.this.dropDownMenu, visible, enabled);

                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[0].setSelection((boolean) View.this.emptyFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[1].setSelection((boolean) View.this.unchangedFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[2].setSelection((boolean) View.this.extraFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[4].setSelection((boolean) View.this.redirectKeysFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[6].setSelection(View.this.urlsFilter == 1);
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[7].setSelection(View.this.urlsFilter == 2);
                    }
                }
            });

        }
    }

    @Override
    public boolean eventOccurred(UISWTViewEvent e)
    {
        switch (e.getType())
        {
            case UISWTViewEvent.TYPE_CREATE:
                if (this.isCreated)
                {
                    return false;
                }
                ((UISWTViewImpl) e.getView()).setTitle(this.getLocalisedMessageText("i18nAZ.SideBar.Title"));
                if (SideBar.instance != null)
                {
                    MdiEntry mdiEntry = SideBar.instance.getEntry(VIEWID);
                    mdiEntry.setImageLeftID("i18nAZ.image.sidebar");
                }
                this.isCreated = true;

                break;

            case UISWTViewEvent.TYPE_INITIALIZE:
                this.saveObjects.clear();
                this.keys.clear();

                this.initialize((Composite) e.getData(), (SWTSkinObjectContainer) ((UISWTViewImpl) e.getView()).getSkinObject());

                this.startSaveThread();

                break;
            case UISWTViewEvent.TYPE_FOCUSGAINED:
                this.createTopLevelMenuitem();
                break;

            case UISWTViewEvent.TYPE_FOCUSLOST:
                if (this.editMenu != null)
                {
                    this.editMenu.dispose();
                    this.editMenu = null;
                    this.dropDownMenu = null;
                }
                break;

            case UISWTViewEvent.TYPE_REFRESH:
                break;

            case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
                break;

            case UISWTViewEvent.TYPE_DESTROY:
                if (this.isCreated == true)
                {
                    this.isCreated = false;

                    this.pluginInterfaces = null;

                    this.display = null;

                    if (this.infoStyledText.getData(View.DATAKEY_TOOLTIP) != null)
                    {
                        ((ToolTip) this.infoStyledText.getData(View.DATAKEY_TOOLTIP)).dispose();
                        this.infoStyledText.setData(View.DATAKEY_TOOLTIP, null);
                    }
                    if (this.editorStyledText.getData(View.DATAKEY_TOOLTIP) != null)
                    {
                        ((ToolTip) this.editorStyledText.getData(View.DATAKEY_TOOLTIP)).dispose();
                        this.editorStyledText.setData(View.DATAKEY_TOOLTIP, null);
                    }

                    this.addLanguageButton = null;
                    this.exportLanguageButton = null;
                    this.removeLanguageButton = null;

                    this.emptyFilterButton = null;
                    this.unchangedFilterButton = null;
                    this.extraFilterButton = null;
                    this.multilineEditorButton = null;
                    this.treeModeButton = null;
                    this.urlsFilterButton = null;
                    this.redirectKeysFilterButton = null;
                    this.helpButton = null;
                    this.infoText = null;
                    this.searchTextbox = null;
                    this.pluginsCombo = null;
                    this.undoToolItem = null;
                    this.redoToolItem = null;
                    this.cutToolItem = null;
                    this.copyToolItem = null;
                    this.pasteToolItem = null;
                    this.selectAllToolItem = null;
                    this.upperCaseToolItem = null;
                    this.lowerCaseToolItem = null;
                    this.firstCaseToolItem = null;
                    this.trademarkToolItem = null;
                    this.validateToolItem = null;
                    this.cancelToolItem = null;
                    this.toolBar = null;
                    this.infoStyledText = null;
                    this.editorStyledText = null;
                    this.statusLabel = null;

                    if (this.undoRedo != null)
                    {
                        this.undoRedo.dispose();
                        this.undoRedo = null;
                    }
                    if (this.clipboard != null)
                    {
                        this.clipboard.dispose();
                        this.clipboard = null;
                    }

                    if (this.editMenu != null)
                    {
                        this.editMenu.dispose();
                        this.editMenu = null;
                        this.dropDownMenu = null;
                    }

                    TreeTableManager.dispose();
                    if (this.saveThread != null && this.saveThread.isAlive())
                    {
                        try
                        {
                            this.saveThread.join();
                        }
                        catch (InterruptedException ie)
                        {
                            ie.printStackTrace();
                        }
                        this.saveThread = null;
                    }
                }

                break;
        }
        return true;
    }

    private void exportLanguage()
    {
        TreeTableManager.getCurrent().setFocus();

        DirectoryDialog directoryDialog = new DirectoryDialog(SWTSkinFactory.getInstance().getShell().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        directoryDialog.setFilterPath(this.defaultPath);
        directoryDialog.setText(this.getLocalisedMessageText("i18nAZ.Titles.Export") + this.currentBundleObject.getName() + "_*");
        directoryDialog.setMessage(this.getLocalisedMessageText("i18nAZ.Labels.Export"));
        String path = directoryDialog.open();
        if (path != null)
        {
            this.defaultPath = path;
            File pathFile = new File(path);
            for (int i = 1; i < this.localesProperties.size(); i++)
            {
                Locale locale = this.localesProperties.get(i).locale;
                String sFileName = this.currentBundleObject.getName() + "_" + locale.toLanguageTag().replace('-', '_') + BundleObject.EXTENSION;
                Util.saveLocaleProperties(this.localesProperties.get(i).commentedProperties, new File(pathFile + File.separator + sFileName));
            }
        }
    }

    private void formatStyledText(StyledText styledText, List<int[]> params, List<Object[]> references, List<Object[]> urls, boolean hand)
    {
        // found default styles
        StyleRange styleRange = null;
        if (styledText.getData(View.DATAKEY_DEFAULT_STYLES) == null)
        {
            // set default style
            styleRange = new StyleRange(0, styledText.getText().length(), styledText.getForeground(), styledText.getBackground());
            styleRange.font = styledText.getFont();
            styledText.setStyleRange(styleRange);

            String languageTag = (String) TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()).getData(View.DATAKEY_LANGUAGE_TAG);

            if (this.searchPatterns != null && (this.searchPrefixes == null || this.searchPrefixes.contains(languageTag) == true))
            {
                for (Iterator<Entry<Pattern, Object>> iterator = this.searchPatterns.entrySet().iterator(); iterator.hasNext();)
                {
                    Entry<Pattern, Object> entry = iterator.next();
                    Pattern searchPattern = entry.getKey();
                    boolean searchResult = (boolean) entry.getValue();
                    Matcher matcher = searchPattern.matcher(styledText.getText());
                    matcher.reset();
                    while (matcher.find() == searchResult)
                    {
                        styleRange = new StyleRange(matcher.start(), matcher.end() - matcher.start(), styledText.getForeground(), Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
                        styledText.setStyleRange(styleRange);
                    }
                }
            }

        }
        else
        {
            styledText.setStyleRanges((StyleRange[]) styledText.getData(View.DATAKEY_DEFAULT_STYLES));
        }

        // set styles params
        for (int i = 0; i < params.size(); i++)
        {
            styleRange = new StyleRange(params.get(i)[0], params.get(i)[1], new Color(Display.getCurrent(), 163, 21, 21), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
            styledText.setStyleRange(styleRange);
        }

        // set styles references
        for (int i = 0; i < references.size(); i++)
        {
            styleRange = new StyleRange((int) references.get(i)[0], (int) references.get(i)[1], Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styleRange.underline = true;
            styledText.setStyleRange(styleRange);

            styleRange = new StyleRange((int) references.get(i)[0], 1, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styledText.setStyleRange(styleRange);

            styleRange = new StyleRange((int) references.get(i)[0] + (int) references.get(i)[1] - 1, 1, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styledText.setStyleRange(styleRange);
        }

        // set styles urls
        for (int i = 0; i < urls.size(); i++)
        {
            styleRange = new StyleRange((int) urls.get(i)[0], (int) urls.get(i)[1], new Color(Display.getCurrent(), 0, 0, 0), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styleRange.underline = true;
            if (hand == true)
            {
                styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            }
            styleRange.data = urls.get(i);
            styledText.setStyleRange(styleRange);
        }
    }

    boolean find(int columnIndex, String value)
    {

        if (this.searchPrefixes != null)
        {
            String prefix = "";
            switch (columnIndex)
            {
                case -1:
                    prefix = "c";
                    break;

                case 0:
                    prefix = "k";
                    break;

                case 1:
                    prefix = "r";
                    break;

                default:
                    prefix = (String) TreeTableManager.getColumn(columnIndex).getData(View.DATAKEY_LANGUAGE_TAG);
                    break;

            }
            if (this.searchPrefixes.contains(prefix) == false)
            {
                return false;
            }

        }
        for (Iterator<Entry<Pattern, Object>> iterator = this.searchPatterns.entrySet().iterator(); iterator.hasNext();)
        {
            Entry<Pattern, Object> entry = iterator.next();
            Pattern searchPattern = entry.getKey();
            boolean searchResult = (boolean) entry.getValue();
            if (searchPattern.matcher(value).find() == searchResult)
            {
                return true;
            }
        }
        return false;
    }

    int[] getCounts(String topKey, int[] parentCounts, int columnIndex)
    {
        if (topKey.equals("") == true)
        {
            topKey = null;
        }
        List<Map<String, Object>> prebuildItems = this.getPrebuildItems(topKey, false);
        return this.getCounts(prebuildItems, parentCounts, columnIndex);
    }

    int[] getCounts(List<Map<String, Object>> prebuildItems, int[] parentCounts, int columnIndex)
    {
        int[] counts = new int[6];
        parentCounts = (parentCounts == null) ? new int[6] : parentCounts;
        int entryCount = 0;
        int emptyCount = 0;
        int unchangedCount = 0;
        int extraCount = 0;
        int redirectKeyCount = 0;
        int urlsCount = 0;
        for (int i = 0; i < prebuildItems.size(); i++)
        {
            Map<String, Object> prebuildItem = prebuildItems.get(i);

            if (prebuildItem.containsKey(TreeTableManager.DATAKEY_CHILDS) == true)
            {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> childPrebuildItems = (List<Map<String, Object>>) prebuildItem.get(TreeTableManager.DATAKEY_CHILDS);
                counts = this.getCounts(childPrebuildItems, counts, columnIndex);
            }

            boolean Exist = (boolean) prebuildItem.get(TreeTableManager.DATAKEY_EXIST);
            if (Exist == false)
            {
                continue;
            }

            int[] states = (int[]) prebuildItem.get(TreeTableManager.DATAKEY_STATES);

            entryCount++;

            if ((states[1] & State.REDIRECT_KEY) != 0)
            {
                redirectKeyCount++;
                continue;
            }
            if ((states[1] & State.URL) != 0)
            {
                urlsCount++;
                continue;
            }

            boolean rowContainEmpty = false;
            boolean rowContainUnchanged = false;
            boolean rowContainExtra = false;

            for (int j = 2; j < this.localesProperties.size() + 1; j++)
            {
                if (columnIndex == -1 || columnIndex == j)
                {
                    switch (states[j])
                    {
                        case State.EMPTY:
                            rowContainEmpty = true;
                            break;

                        case State.UNCHANGED:
                            rowContainUnchanged = true;
                            break;

                        case State.EXTRA:
                            rowContainExtra = true;
                            break;
                    }
                }
            }
            if (rowContainEmpty == true)
            {
                emptyCount += 1;
            }
            if (rowContainUnchanged == true)
            {
                unchangedCount += 1;
            }
            if (rowContainExtra == true)
            {
                extraCount += 1;
            }
        }
        counts[0] += parentCounts[0] + entryCount;
        counts[1] += parentCounts[1] + emptyCount;
        counts[2] += parentCounts[2] + unchangedCount;
        counts[3] += parentCounts[3] + extraCount;
        counts[4] += parentCounts[4] + redirectKeyCount;
        counts[5] += parentCounts[5] + urlsCount;
        return counts;
    }

    BundleObject getCurrentBundleObject()
    {
        return this.currentBundleObject;
    }

    String getDefaultPath()
    {
        return this.defaultPath;
    }

    Display getDisplay()
    {
        return this.display;
    }

    ImageLoader getImageLoader()
    {
        return this.imageLoader;
    }

    LocalesProperties getLocalesProperties(Locale locale)
    {
        for (int i = 0; i < this.localesProperties.size(); i++)
        {
            if (this.localesProperties.get(i).locale == locale)
            {
                return this.localesProperties.get(i);
            }
        }
        return null;
    }

    String getLocalisedMessageText(String key)
    {
        return this.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(key);
    }

    String getLocalisedMessageText(String key, String param)
    {
        return this.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(key, new String[] { param });
    }

    String getLocalisedMessageText(String key, String[] params)
    {
        return this.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(key, params);
    }

    PluginInterface getPluginInterface()
    {
        return this.pluginInterface;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getPrebuildItems(String topKey, boolean sort)
    {
        List<Map<String, Object>> prebuildItems = new ArrayList<Map<String, Object>>();
        if (this.localesProperties.size() > 0)
        {
            if (sort == true)
            {
                Collections.sort(this.keys, new Comparator<String>()
                {
                    @Override
                    public int compare(String key1, String key2)
                    {
                        String lowerkey1 = key1.toLowerCase(Locale.US);
                        String lowerkey2 = key2.toLowerCase(Locale.US);

                        int prefix_length = 0;
                        for (int i = 0; i < Math.min(lowerkey1.length(), lowerkey2.length()); i++)
                        {
                            char lowerChar1 = lowerkey1.charAt(i);
                            char lowerChar2 = lowerkey2.charAt(i);
                            if (lowerChar1 != lowerChar2)
                            {
                                break;
                            }
                            if (lowerChar1 == '.')
                            {
                                prefix_length = i;

                                break;
                            }
                        }
                        if (prefix_length > 0)
                        {
                            int result = key2.substring(0, prefix_length).compareTo(key1.substring(0, prefix_length));
                            if (result != 0)
                            {
                                return result;
                            }
                            return key1.substring(prefix_length).compareToIgnoreCase(key2.substring(prefix_length));
                        }
                        return key1.toString().compareToIgnoreCase(key2);
                    }
                });
            }

            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < this.keys.size(); i++)
            {

                boolean show = true;

                boolean matchesSearch = this.searchPatterns == null;

                // key
                String key = this.keys.get(i);

                if (topKey != null && key.startsWith(topKey) == false)
                {
                    continue;
                }

                String[] values = new String[this.localesProperties.size() + 1];
                int[] states = new int[this.localesProperties.size() + 1];
                values[0] = (TreeTableManager.isTreeMode() ? key.substring(key.lastIndexOf('.') + 1) : key);
                if (matchesSearch == false && key != null)
                {
                    matchesSearch = this.find(0, key);
                }

                // define show row values
                boolean showRowEmpty = this.emptyFilter;
                boolean showRowUnchanged = this.unchangedFilter;
                boolean showRowExtra = this.extraFilter;
                boolean hideRedirectKeysFilter = this.redirectKeysFilter;
                int urlsFilterState = this.urlsFilter;

                for (Iterator<String> iterator = this.emptyFilterExcludedKey.iterator(); iterator.hasNext();)
                {
                    if (key.startsWith(iterator.next() + "."))
                    {
                        showRowEmpty = !showRowEmpty;
                        break;
                    }
                }
                for (Iterator<String> iterator = this.unchangedFilterExcludedKey.iterator(); iterator.hasNext();)
                {
                    if (key.startsWith(iterator.next() + "."))
                    {
                        showRowUnchanged = !showRowUnchanged;
                        break;
                    }
                }
                for (Iterator<String> iterator = this.extraFilterExcludedKey.iterator(); iterator.hasNext();)
                {
                    if (key.startsWith(iterator.next() + "."))
                    {
                        showRowExtra = !showRowExtra;
                        break;
                    }
                }
                for (Iterator<String> iterator = this.hideRedirectKeysFilterExcludedKey.iterator(); iterator.hasNext();)
                {
                    if (key.startsWith(iterator.next() + "."))
                    {
                        hideRedirectKeysFilter = !hideRedirectKeysFilter;
                        break;
                    }
                }
                for (Iterator<Entry<String, Integer>> iterator = this.urlsFilterOverriddenStates.entrySet().iterator(); iterator.hasNext();)
                {
                    Entry<String, Integer> entry = iterator.next();
                    if (key.startsWith(entry.getKey() + "."))
                    {
                        urlsFilterState = entry.getValue();
                        break;
                    }
                }

                // reference
                CommentedProperties localeProperties = this.localesProperties.get(0).commentedProperties;
                values[1] = Util.escape(localeProperties.getProperty(key), false);
                states[1] = Util.getStateOfReference(values[1]);
                if (matchesSearch == false && values[1] != null)
                {
                    matchesSearch = this.find(1, values[1]);
                }

                // comments
                String[] commentsLines = new String[] {};

                commentsLines = localeProperties.getComment(key);
                if (matchesSearch == false && commentsLines != null)
                {
                    String comments = "";
                    for (int k = 0; k < commentsLines.length; k++)
                    {
                        comments += commentsLines[k].replaceAll("\\n", "\\\\n") + "\n";
                    }
                    matchesSearch = this.find(-1, comments);
                }

                // show not redirect key
                if (hideRedirectKeysFilter == true && (states[1] & State.REDIRECT_KEY) != 0)
                {
                    show = false;
                }

                // show/hide url
                switch (urlsFilterState)
                {
                    case 1:
                        if ((states[1] & State.URL) != 0)
                        {
                            show = false;
                        }
                        break;

                    case 2:
                        if ((states[1] & State.URL) == 0)
                        {
                            show = false;
                        }
                        break;
                }

                // values
                String rowText = values[1];
                boolean rowContainEmpty = false;
                boolean rowContainUnchanged = false;
                boolean rowContainExtra = false;

                for (int j = 1; j < this.localesProperties.size(); j++)
                {
                    localeProperties = this.localesProperties.get(j).commentedProperties;
                    values[j + 1] = Util.escape(localeProperties.getProperty(key), false);
                    states[j + 1] = Util.getStateOfValue(values[1], values[j + 1]);
                    if (matchesSearch == false)
                    {
                        matchesSearch = this.find(j + 1, values[j + 1]);
                    }
                    switch (states[j + 1])
                    {
                        case State.EMPTY:
                            rowContainEmpty = true;
                            break;

                        case State.UNCHANGED:
                            rowContainUnchanged = true;
                            break;

                        case State.EXTRA:
                            rowContainExtra = true;
                            break;
                    }
                    rowText += values[j + 1];
                }
                if (matchesSearch == false)
                {
                    show = false;
                }

                if (show == true && (showRowEmpty || showRowUnchanged || showRowExtra))
                {
                    show = (rowContainEmpty && showRowEmpty) || (rowContainUnchanged && showRowUnchanged) || (rowContainExtra && showRowExtra);
                }
                if (topKey != null)
                {
                    show = true;
                }
                if (rowText.equals("") == true)
                {
                    show = false;
                }
                if (show == true)
                {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put(TreeTableManager.DATAKEY_KEY, key);
                    item.put(TreeTableManager.DATAKEY_COMMENTS, commentsLines);
                    item.put(TreeTableManager.DATAKEY_VALUES, values);
                    item.put(TreeTableManager.DATAKEY_STATES, states);
                    item.put(TreeTableManager.DATAKEY_EXIST, true);
                    items.add(item);
                }
            }
            for (int i = 0; i < items.size(); i++)
            {
                String key = (String) items.get(i).get(TreeTableManager.DATAKEY_KEY);
                if (TreeTableManager.isTreeMode() == true)
                {
                    int nextDotIndex = -1;
                    do
                    {
                        Map<String, Object> item = null;
                        String currentKey = key;
                        nextDotIndex = key.indexOf('.', nextDotIndex + 1);
                        if (nextDotIndex == -1)
                        {
                            item = items.get(i);
                        }
                        else
                        {
                            currentKey = key.substring(0, nextDotIndex);
                            item = new HashMap<String, Object>();
                            int parentLastDotIndex = currentKey.lastIndexOf('.');

                            String[] values = new String[this.localesProperties.size() + 1];
                            values[0] = currentKey.substring(parentLastDotIndex + 1);

                            item.put(TreeTableManager.DATAKEY_KEY, currentKey);
                            item.put(TreeTableManager.DATAKEY_COMMENTS, new String[] {});
                            item.put(TreeTableManager.DATAKEY_VALUES, values);
                            item.put(TreeTableManager.DATAKEY_STATES, new int[this.localesProperties.size() + 1]);
                            item.put(TreeTableManager.DATAKEY_EXIST, false);
                        }

                        List<Map<String, Object>> foundedPrebuildItems = prebuildItems;
                        List<Map<String, Object>> tempPrebuildItems = prebuildItems;
                        do
                        {
                            boolean loop = false;
                            for (int j = 0; j < tempPrebuildItems.size(); j++)
                            {
                                String parentKey = tempPrebuildItems.get(j).get(TreeTableManager.DATAKEY_KEY) + ".";
                                if (currentKey.startsWith(parentKey) == true)
                                {
                                    if (tempPrebuildItems.get(j).containsKey(TreeTableManager.DATAKEY_CHILDS) == true)
                                    {
                                        foundedPrebuildItems = (List<Map<String, Object>>) tempPrebuildItems.get(j).get(TreeTableManager.DATAKEY_CHILDS);
                                        tempPrebuildItems = (List<Map<String, Object>>) tempPrebuildItems.get(j).get(TreeTableManager.DATAKEY_CHILDS);
                                        loop = true;
                                    }
                                    else
                                    {
                                        foundedPrebuildItems = new ArrayList<Map<String, Object>>();
                                        tempPrebuildItems.get(j).put(TreeTableManager.DATAKEY_CHILDS, foundedPrebuildItems);
                                    }
                                    break;
                                }
                            }
                            if (loop == false)
                            {
                                break;
                            }
                        }
                        while (true);
                        boolean found = false;
                        for (int j = 0; j < foundedPrebuildItems.size(); j++)
                        {
                            if (currentKey.equals((foundedPrebuildItems.get(j).get(TreeTableManager.DATAKEY_KEY))) == true)
                            {
                                if ((boolean) item.get(TreeTableManager.DATAKEY_EXIST) == true && (boolean) foundedPrebuildItems.get(j).get(TreeTableManager.DATAKEY_EXIST) == false)
                                {
                                    if (foundedPrebuildItems.get(j).containsKey(TreeTableManager.DATAKEY_CHILDS) == true)
                                    {
                                        tempPrebuildItems.get(j).put(TreeTableManager.DATAKEY_CHILDS, foundedPrebuildItems.get(j).get(TreeTableManager.DATAKEY_CHILDS));
                                    }
                                    foundedPrebuildItems.set(j, item);
                                }
                                found = true;
                                break;
                            }
                        }
                        if (found == false)
                        {
                            foundedPrebuildItems.add(item);
                        }
                        if (nextDotIndex == -1)
                        {
                            break;
                        }
                    }
                    while (true);
                }
                else
                {
                    prebuildItems.add(items.get(i));
                }
            }
        }
        return prebuildItems;
    }

    private void initialize(Composite composite, SWTSkinObjectContainer skinObjectContainer)
    {
        this.loggerChannel.log(1, "Initialize");

        this.clipboard = new Clipboard(Display.getCurrent());

        this.display = composite.getDisplay();

        SWTSkinObjectContainer MainChildContainer = new SWTSkinObjectContainer(SWTSkinFactory.getInstance(), SWTSkinFactory.getInstance().getSkinProperties(), "i18nAZ.main", "i18nAZ.main", skinObjectContainer);
        MainChildContainer.setControl(composite);
        MainChildContainer.getComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        SWTSkinObjectContainer HeaderContainer = new SWTSkinObjectContainer(SWTSkinFactory.getInstance(), SWTSkinFactory.getInstance().getSkinProperties(), "i18nAZ.main.header", "i18nAZ.main.header", MainChildContainer);
        HeaderContainer.getComposite().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        SWTSkinObjectContainer ToolBarContainer = (SWTSkinObjectContainer) SWTSkinFactory.getInstance().createSkinObject("mdientry.toolbar.full", "mdientry.toolbar.full", HeaderContainer);

        Control lastControl = null;

        // ADD LANGUAGE BUTTON
        this.addLanguageButton = this.addButton(ToolBarContainer, "addLanguage", "left", "i18nAZ.image.toolbar.add", "i18nAZ.ToolTips.AddLanguage", 10, lastControl);
        this.addLanguageButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.addLanguage();
            }
        });

        lastControl = this.addLanguageButton.getSkinObject().getControl();
        lastControl = this.addSeparator(ToolBarContainer, lastControl).getControl();

        // EXPORT BUTTON
        this.exportLanguageButton = this.addButton(ToolBarContainer, "exportLanguage", "", "i18nAZ.image.toolbar.export", "i18nAZ.ToolTips.ExportLanguage", 0, lastControl);
        this.exportLanguageButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.exportLanguage();
            }
        });
        lastControl = this.exportLanguageButton.getSkinObject().getControl();
        lastControl = this.addSeparator(ToolBarContainer, lastControl).getControl();

        // REMOVE BUTTON
        this.removeLanguageButton = this.addButton(ToolBarContainer, "removeLanguage", "right", "image.toolbar.remove", "i18nAZ.ToolTips.RemoveLanguage", 0, lastControl);
        this.removeLanguageButton.setDisabled(true);
        this.removeLanguageButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.removeLanguage();
            }
        });
        lastControl = this.removeLanguageButton.getSkinObject().getControl();

        // TREE MODE BUTTON
        TreeTableManager.setMode(COConfigurationManager.getBooleanParameter("i18nAZ.treeMode"));
        this.treeModeButton = this.addButton(ToolBarContainer, "treeMode", TreeTableManager.isTreeMode(), "left", "i18nAZ.image.toolbar.treeMode", "i18nAZ.ToolTips.TreeMode", 10, lastControl);
        this.treeModeButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();

                TreeTableManager.setMode((boolean) buttonUtility.getSkinObject().getData("checked"));
                View.this.updateTreeTable(false);
                COConfigurationManager.setParameter("i18nAZ.treeMode", TreeTableManager.isTreeMode());
                COConfigurationManager.save();
            }
        });
        lastControl = this.treeModeButton.getSkinObject().getControl();
        lastControl = this.addSeparator(ToolBarContainer, lastControl).getControl();

        // SHOW REF BUTTON
        this.redirectKeysFilter = COConfigurationManager.getBooleanParameter("i18nAZ.redirectKeysFilter");
        this.redirectKeysFilterButton = this.addButton(ToolBarContainer, "redirectKeysFilter", this.redirectKeysFilter, "", "i18nAZ.image.toolbar.redirectKeysFilter", "i18nAZ.ToolTips.RedirectKeysFilter", 0, lastControl);
        this.redirectKeysFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();
                View.this.extraFilterExcludedKey.clear();
                View.this.redirectKeysFilter = (boolean) buttonUtility.getSkinObject().getData("checked");
                View.this.updateTreeTable();
                COConfigurationManager.setParameter("i18nAZ.redirectKeysFilter", View.this.redirectKeysFilter);
                COConfigurationManager.save();
            }
        });
        lastControl = this.redirectKeysFilterButton.getSkinObject().getControl();
        lastControl = this.addSeparator(ToolBarContainer, lastControl).getControl();

        // SHOW URL BUTTON
        this.urlsFilter = COConfigurationManager.getIntParameter("i18nAZ.urlsFilter", 0);
        this.urlsFilterButton = this.addButton(ToolBarContainer, "urlsFilter", this.urlsFilter != 0, "right", "i18nAZ.image.toolbar.urlsFilter" + (this.urlsFilter == 2 ? "On" : (this.urlsFilter == 1 ? "Off" : "")), "i18nAZ.ToolTips.UrlsFilter", 0, lastControl);
        View.this.setUrlsFilterState(this.urlsFilter);
        this.urlsFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.setUrlsFilterState();
            }
        });
        lastControl = this.urlsFilterButton.getSkinObject().getControl();

        // EMPTY FILTER BUTTTON
        this.emptyFilter = COConfigurationManager.getBooleanParameter("i18nAZ.emptyFilter");
        this.emptyFilterButton = this.addButton(ToolBarContainer, "emptyFilter", this.emptyFilter, "left", "i18nAZ.image.toolbar.emptyFilter", "i18nAZ.ToolTips.EmptyFilter", 10, lastControl);
        this.emptyFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();

                View.this.setEmptyFilter((boolean) buttonUtility.getSkinObject().getData("checked"));
                View.this.updateTreeTable();
            }
        });
        lastControl = this.emptyFilterButton.getSkinObject().getControl();
        lastControl = this.addSeparator(ToolBarContainer, lastControl).getControl();

        // UNCHANGED FILTER BUTTTON
        this.unchangedFilter = COConfigurationManager.getBooleanParameter("i18nAZ.unchangedFilter");
        this.unchangedFilterButton = this.addButton(ToolBarContainer, "unchangedFilter", this.unchangedFilter, "", "i18nAZ.image.toolbar.unchangedFilter", "i18nAZ.ToolTips.UnchangedFilter", 0, lastControl);
        this.unchangedFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();

                View.this.setUnchangedFilter((boolean) buttonUtility.getSkinObject().getData("checked"));
                View.this.updateTreeTable();
            }
        });
        lastControl = this.unchangedFilterButton.getSkinObject().getControl();
        lastControl = this.addSeparator(ToolBarContainer, lastControl).getControl();

        // EXTRA FILTER BUTTTON
        this.extraFilter = COConfigurationManager.getBooleanParameter("i18nAZ.extraFilter");
        this.extraFilterButton = this.addButton(ToolBarContainer, "extraFilter", this.extraFilter, "right", "i18nAZ.image.toolbar.extraFilter", "i18nAZ.ToolTips.ExtraFilter", 0, lastControl);
        this.extraFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();

                View.this.setExtraFilter((boolean) buttonUtility.getSkinObject().getData("checked"));
                View.this.updateTreeTable();
            }
        });
        lastControl = this.extraFilterButton.getSkinObject().getControl();

        // MULTILINE EDITOR BUTTON
        this.multilineEditor = COConfigurationManager.getBooleanParameter("i18nAZ.multilineEditor");
        this.multilineEditorButton = this.addButton(ToolBarContainer, "multilineEditor", this.multilineEditor, "all", "i18nAZ.image.toolbar.multilineEditor", "i18nAZ.ToolTips.MultilineEditor", 10, lastControl);
        this.multilineEditorButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                String odlValue = null;
                if (View.this.editorStyledText.getVisible() == true)
                {
                    odlValue = View.this.editorStyledText.getText();
                    if (View.this.multilineEditor == true)
                    {
                        odlValue = Util.escape(odlValue, false);
                    }
                }
                View.this.multilineEditor = (boolean) buttonUtility.getSkinObject().getData("checked");
                COConfigurationManager.setParameter("i18nAZ.multilineEditor", View.this.multilineEditor);
                COConfigurationManager.save();

                ToolTipText.set(View.this.validateToolItem, "i18nAZ.ToolTips.Validate" + ((View.this.multilineEditor == true) ? "" : ".Shortcut"));

                View.this.updateStyledTexts();
                if (odlValue != null)
                {
                    if (View.this.multilineEditor == true)
                    {
                        odlValue = Util.unescape(odlValue);
                    }
                    View.this.editorStyledText.setText(odlValue);
                }
            }
        });
        lastControl = this.multilineEditorButton.getSkinObject().getControl();

        // HELP BUTTON
        this.helpButton = this.addButton(ToolBarContainer, "help", "all", "i18nAZ.image.toolbar.help", "i18nAZ.ToolTips.Help", 10, lastControl);
        this.helpButton.addSelectionListener(new ButtonListenerAdapter()
        {
            @Override
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                String helpText = "";
                String helpFullPath = "readme/readme_" + View.this.getPluginInterface().getUtilities().getLocaleUtilities().getCurrentLocale().toLanguageTag().replace('-', '_') + ".txt";
                InputStream stream = View.this.getPluginInterface().getPluginClassLoader().getResourceAsStream(helpFullPath);
                if (stream == null)
                {
                    helpFullPath = "readme/readme.txt";
                    stream = View.this.getPluginInterface().getPluginClassLoader().getResourceAsStream(helpFullPath);
                }
                if (stream == null)
                {
                    helpText = "Error loading resource: " + helpFullPath;
                }
                else
                {
                    try
                    {
                        helpText = Util.readInputStreamAsString(stream, 65535, "utf8");
                        stream.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                // create shell
                Shell shell = new Shell(SWTSkinFactory.getInstance().getShell().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
                shell.setImages(SWTSkinFactory.getInstance().getShell().getShell().getImages());
                shell.setLayout(new FillLayout());
                shell.setText(View.this.getLocalisedMessageText("i18nAZ.Titles.Help"));

                // add help text
                Label bannerLabel = new Label(shell, SWT.NULL);
                bannerLabel.setImage(View.this.imageLoader.getImage("i18nAZ.image.banner"));

                // add help text
                Text helpLabel = new Text(shell, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
                helpLabel.setText(helpText);
                helpLabel.setEditable(false);

                // set shell size
                shell.setSize(640, 480);

                // open shell and center it to parent
                Util.openShell(SWTSkinFactory.getInstance().getShell().getShell(), shell);
            }
        });
        lastControl = this.helpButton.getSkinObject().getControl();

        // SEARCH TEXTBOX
        this.regexSearch = COConfigurationManager.getBooleanParameter("i18nAZ.RegexSearch");
        this.searchTextbox = (SWTSkinObjectTextbox) SWTSkinFactory.getInstance().createSkinObject("i18nAZ.main.header.search", "i18nAZ.main.header.search", ToolBarContainer);
        ToolTipText.set(this.searchTextbox.getTextControl(), "i18nAZ.ToolTips.Search");
        this.searchTextbox.getTextControl().setBackground(Display.getCurrent().getSystemColor(this.regexSearch == true ? SWT.COLOR_INFO_BACKGROUND : SWT.COLOR_WIDGET_BACKGROUND));
        this.searchTextbox.getTextControl().addModifyListener(new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (View.this.searchTextbox.getText().equals("") == true)
                {
                    View.this.setSearch(null);
                }
            }
        });
        this.searchTextbox.getTextControl().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
            }

            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.stateMask == SWT.CTRL && e.keyCode == 'x')
                {
                    View.this.regexSearch = View.this.regexSearch == false;
                    COConfigurationManager.setParameter("i18nAZ.RegexSearch", View.this.regexSearch);
                    COConfigurationManager.save();
                    View.this.searchTextbox.getTextControl().setBackground(Display.getCurrent().getSystemColor(View.this.regexSearch == true ? SWT.COLOR_INFO_BACKGROUND : SWT.COLOR_WHITE));
                }
            }
        });
        this.searchTextbox.getTextControl().addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                if ((e.detail & SWT.CANCEL) != 0)
                {
                    View.this.setSearch(null);
                }
                else
                {
                    View.this.setSearch(View.this.searchTextbox.getText());
                }
            }

            @Override
            public void widgetSelected(SelectionEvent e)
            {

            }
        });

        // INFO TEXT
        this.infoText = (SWTSkinObjectText2) SWTSkinFactory.getInstance().createSkinObject("i18nAZ.main.header.info", "i18nAZ.main.header.info", ToolBarContainer);
        FormData formData = (FormData) this.infoText.getControl().getLayoutData();
        formData.left = new FormAttachment(lastControl, 10);

        SWTSkinObjectContainer AreaContainer = new SWTSkinObjectContainer(SWTSkinFactory.getInstance(), SWTSkinFactory.getInstance().getSkinProperties(), "i18nAZ.main.area", "i18nAZ.main.area", MainChildContainer);
        AreaContainer.getComposite().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        GridLayout gridLayout = null;

        AreaContainer.getComposite().setLayout(new GridLayout(1, false));

        Composite pluginComposite = new Composite(AreaContainer.getComposite(), SWT.NULL);
        gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        pluginComposite.setLayout(gridLayout);
        pluginComposite.setLayoutData(new GridData(SWT.FILL, SWT.NULL, true, false));

        Label pluginsLabel = new Label(pluginComposite, SWT.NULL);
        pluginsLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        pluginsLabel.setText(this.getLocalisedMessageText("i18nAZ.Labels.Plugins"));

        this.pluginsCombo = new Combo(pluginComposite, SWT.READ_ONLY);
        GridData gridData = new GridData(SWT.FILL, SWT.NULL, false, false);
        gridData.widthHint = 200;
        this.pluginsCombo.setLayoutData(gridData);
        this.pluginsCombo.setCursor(new org.eclipse.swt.graphics.Cursor(Display.getCurrent(), SWT.CURSOR_HAND));
        String PluginSelected = "";
        if (COConfigurationManager.hasParameter("i18nAZ.PluginSelected", true))
        {
            PluginSelected = COConfigurationManager.getStringParameter("i18nAZ.PluginSelected");
        }
        int PluginSelectedIndex = 0;
        this.selectedPluginInterface = null;
        this.pluginInterfaces = Util.getPluginInterfaces();
        for (int i = 0; i < this.pluginInterfaces.length; i++)
        {
            String PluginKey = "";
            String PluginName = AZUREUS_PLUGIN_NAME;
            PluginInterface pluginInterface = this.pluginInterfaces[i];
            if (pluginInterface != null)
            {
                Properties PluginProperties = pluginInterface.getPluginProperties();
                PluginKey = PluginProperties.getProperty("plugin.id") + "_" + PluginProperties.getProperty("plugin.version");
                String LocalizedKey = "Views.plugins." + PluginProperties.getProperty("plugin.id") + ".title";
                if (this.getPluginInterface().getUtilities().getLocaleUtilities().hasLocalisedMessageText(LocalizedKey) == true)
                {
                    PluginName = this.getLocalisedMessageText(LocalizedKey);
                }
                else
                {
                    PluginName = PluginProperties.getProperty("plugin.name");
                }
            }
            this.pluginsCombo.add(PluginName);
            if (PluginKey.equals(PluginSelected))
            {
                PluginSelectedIndex = i;
                this.selectedPluginInterface = pluginInterface;
            }
        }
        this.pluginsCombo.select(PluginSelectedIndex);
        this.pluginsCombo.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                PluginInterface pluginInterface = View.this.pluginInterfaces[View.this.pluginsCombo.getSelectionIndex()];
                String pluginKey = "";
                if (pluginInterface != null)
                {
                    Properties pluginProperties = pluginInterface.getPluginProperties();
                    pluginKey = pluginProperties.getProperty("plugin.id") + "_" + pluginProperties.getProperty("plugin.version");
                }
                COConfigurationManager.setParameter("i18nAZ.PluginSelected", pluginKey);
                COConfigurationManager.save();
                View.this.selectedPluginInterface = pluginInterface;
                View.this.updateTreeTable(true, true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                this.widgetSelected(e);
            }
        });

        TreeTableManager.initTreeTable(AreaContainer);

        // Create info composite
        Composite infoComposite = new Composite(AreaContainer.getComposite(), SWT.NULL);
        gridLayout = new GridLayout(1, false);
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        infoComposite.setLayout(gridLayout);

        gridData = new GridData(SWT.FILL, SWT.NULL, true, false);
        // gridData.heightHint = 0;
        infoComposite.setLayoutData(gridData);

        // Create info styled text composite
        Composite infoStyledTextComposite = new Composite(infoComposite, SWT.BORDER);
        infoStyledTextComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        infoStyledTextComposite.setLayout(new GridLayout(1, false));
        infoStyledTextComposite.setLayoutData(gridData);

        // Create info styled text
        this.infoStyledText = new StyledText(infoStyledTextComposite, SWT.WRAP | SWT.MULTI);
        this.infoStyledText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        this.infoStyledText.setFont(new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 8, SWT.NORMAL));
        this.infoStyledText.setEditable(false);
        this.infoStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.addLinkManager(this.infoStyledText, true);

        Composite toolBarComposite = new Composite(infoComposite, SWT.NULL);
        RowLayout rowLayout = new RowLayout(SWT.NULL);
        toolBarComposite.setLayout(rowLayout);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.heightHint = 0;
        toolBarComposite.setLayoutData(gridData);
        toolBarComposite.setVisible(false);

        this.toolBar = new ToolBar(toolBarComposite, SWT.FLAT);
        this.toolBar.setFont(new Font(null, "Times New Roman", 11, SWT.NORMAL));
        ToolTipText.config(this.toolBar);

        this.undoToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.undoToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.undo"));
        ToolTipText.set(this.undoToolItem, "i18nAZ.ToolTips.Undo");
        this.undoToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                View.this.undoRedo.undo();
                View.this.editorStyledText.setFocus();
            }
        });

        this.redoToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.redoToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.redo"));
        ToolTipText.set(this.redoToolItem, "i18nAZ.ToolTips.Redo");
        this.redoToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                View.this.undoRedo.redo();
                View.this.editorStyledText.setFocus();
            }
        });

        new ToolItem(this.toolBar, SWT.SEPARATOR);

        this.cutToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.cutToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.cut"));
        ToolTipText.set(this.cutToolItem, "i18nAZ.ToolTips.Cut");
        this.cutToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                View.this.copyToolItem.notifyListeners(SWT.Selection, null);
                Point selection = View.this.editorStyledText.getSelection();
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    View.this.editorStyledText.setText("");
                }
                else
                {
                    View.this.editorStyledText.setText(View.this.editorStyledText.getText().substring(0, selection.x) + View.this.editorStyledText.getText().substring(selection.y));
                }
                selection.y = selection.x;
                View.this.editorStyledText.setSelection(selection);
                View.this.editorStyledText.setFocus();
            }
        });

        this.copyToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.copyToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.copy"));
        ToolTipText.set(this.copyToolItem, "i18nAZ.ToolTips.Copy");
        this.copyToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String textData = null;
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    textData = View.this.editorStyledText.getText();
                }
                else
                {
                    textData = View.this.editorStyledText.getSelectionText();
                }
                if (View.this.multilineEditor == false)
                {
                    textData = Util.unescape(textData);
                }
                View.this.clipboard.setContents(new Object[] { textData }, new Transfer[] { TextTransfer.getInstance() });
                View.this.pasteToolItem.setEnabled(true);
                View.this.editorStyledText.setFocus();
            }
        });

        this.pasteToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.pasteToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.paste"));
        ToolTipText.set(this.pasteToolItem, "i18nAZ.ToolTips.Paste");
        this.pasteToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Point selection = View.this.editorStyledText.getSelection();
                TextTransfer transfer = TextTransfer.getInstance();
                String data = (String) View.this.clipboard.getContents(transfer);
                if (data != null)
                {
                    data = data.replaceAll("\\r\\n", "\n");
                    if (View.this.multilineEditor == false)
                    {
                        data = Util.escape(data, false);
                    }
                    View.this.editorStyledText.insert(data);
                    selection.x = selection.x + data.length();
                    selection.y = selection.x;
                    View.this.editorStyledText.setSelection(selection);
                }
                View.this.editorStyledText.setFocus();
            }
        });

        new ToolItem(this.toolBar, SWT.SEPARATOR);

        this.selectAllToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.selectAllToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.selectall"));
        ToolTipText.set(this.selectAllToolItem, "i18nAZ.ToolTips.SelectAll");
        this.selectAllToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                View.this.selectEditor();
            }
        });

        new ToolItem(this.toolBar, SWT.SEPARATOR);

        this.upperCaseToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.upperCaseToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.ucase"));
        ToolTipText.set(this.upperCaseToolItem, "i18nAZ.ToolTips.Uppercase");
        this.upperCaseToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                // get selected column
                int selectedColumn = (int) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get selected locale
                Locale selectedLocale = (Locale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_LOCALE);

                Point selection = View.this.editorStyledText.getSelection();
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    View.this.editorStyledText.setText(View.this.editorStyledText.getText().toUpperCase(selectedLocale));
                }
                else
                {
                    View.this.editorStyledText.insert(View.this.editorStyledText.getSelectionText().toUpperCase(selectedLocale));
                }
                View.this.editorStyledText.setSelection(selection);
                View.this.editorStyledText.setFocus();
            }
        });

        this.lowerCaseToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.lowerCaseToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.lcase"));
        ToolTipText.set(this.lowerCaseToolItem, "i18nAZ.ToolTips.Lowercase");
        this.lowerCaseToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                // get selected column
                int selectedColumn = (int) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get selected locale
                Locale selectedLocale = (Locale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_LOCALE);

                Point selection = View.this.editorStyledText.getSelection();
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    View.this.editorStyledText.setText(View.this.editorStyledText.getText().toLowerCase(selectedLocale));
                }
                else
                {
                    View.this.editorStyledText.insert(View.this.editorStyledText.getSelectionText().toLowerCase(selectedLocale));
                }
                View.this.editorStyledText.setSelection(selection);
                View.this.editorStyledText.setFocus();
            }
        });

        this.firstCaseToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.firstCaseToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.fcase"));
        ToolTipText.set(this.firstCaseToolItem, "i18nAZ.ToolTips.Firstcase");
        this.firstCaseToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                // get selected column
                int selectedColumn = (int) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get selected locale
                Locale selectedLocale = (Locale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_LOCALE);

                Point selection = View.this.editorStyledText.getSelection();
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    View.this.editorStyledText.setText(Character.toTitleCase(View.this.editorStyledText.getText().charAt(0)) + View.this.editorStyledText.getText().substring(1).toLowerCase(selectedLocale));
                }
                else
                {
                    View.this.editorStyledText.insert(Character.toTitleCase(View.this.editorStyledText.getSelectionText().charAt(0)) + View.this.editorStyledText.getSelectionText().substring(1).toLowerCase(selectedLocale));
                }
                View.this.editorStyledText.setSelection(selection);
                View.this.editorStyledText.setFocus();
            }
        });

        new ToolItem(this.toolBar, SWT.SEPARATOR);
        this.trademarkToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.trademarkToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.trademark"));
        ToolTipText.set(this.trademarkToolItem, "i18nAZ.ToolTips.Trademark");
        this.trademarkToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Point selection = View.this.editorStyledText.getSelection();
                View.this.editorStyledText.insert("™");
                selection.x = selection.x + 1;
                selection.y = selection.x;
                View.this.editorStyledText.setSelection(selection);
                View.this.editorStyledText.setFocus();
            }
        });
        new ToolItem(this.toolBar, SWT.SEPARATOR);

        this.validateToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.validateToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.validate"));
        ToolTipText.set(this.validateToolItem, "i18nAZ.ToolTips.Validate" + ((this.multilineEditor == true) ? "" : ".Shortcut"));
        this.validateToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                View.this.valid(true);
            }
        });

        this.cancelToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.cancelToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.cancel"));
        ToolTipText.set(this.cancelToolItem, "i18nAZ.ToolTips.Cancel");
        this.cancelToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                View.this.cancel();
            }
        });

        this.editorStyledText = new StyledText(infoComposite, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        this.editorStyledText.setKeyBinding(SWT.MOD1 | 'X', ST.VerifyKey);
        this.editorStyledText.setKeyBinding(SWT.MOD1 | 'C', ST.VerifyKey);
        this.editorStyledText.setKeyBinding(SWT.MOD1 | 'V', ST.VerifyKey);
        this.addLinkManager(this.editorStyledText, false);

        this.undoRedo = new UndoRedo(this.editorStyledText);
        this.undoRedo.addListener(SWT.CHANGED, new Listener()
        {
            @Override
            public void handleEvent(Event e)
            {
                View.this.undoToolItem.setEnabled(View.this.undoRedo.canUndo());
                View.this.redoToolItem.setEnabled(View.this.undoRedo.canRedo());
            }
        });
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.editorStyledText.setVisible(false);
        this.editorStyledText.setLayoutData(gridData);
        this.editorStyledText.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (View.this.editorStyledText.isFocusControl() == false)
                {
                    View.this.editorStyledText.setFocus();
                }
                View.this.cutToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0 && e.y - e.x > 0);
                View.this.copyToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0 && e.y - e.x > 0);
                View.this.selectAllToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0 && e.y - e.x != View.this.editorStyledText.getText().length());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });

        final Menu menu = new Menu(this.editorStyledText);
        this.editorStyledText.setMenu(menu);
        this.editorStyledText.addMenuDetectListener(new MenuDetectListener()
        {
            @Override
            public void menuDetected(MenuDetectEvent e)
            {
                View.this.populateMenu(menu, MenuOptions.EDITOR);
            }
        });

        this.createTopLevelMenuitem();

        this.editorStyledText.addExtendedModifyListener(new ExtendedModifyListener()
        {
            @Override
            public void modifyText(ExtendedModifyEvent e)
            {
                View.this.selectAllToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0);
                View.this.upperCaseToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0);
                View.this.lowerCaseToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0);
                View.this.firstCaseToolItem.setEnabled(View.this.editorStyledText.getText().length() > 1);

                // get selected row
                Item selectedRow = (Item) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);

                // get selected column
                int selectedColumn = (int) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get reference
                String reference = TreeTableManager.getText(selectedRow, 1);
                if (View.this.multilineEditor == true)
                {
                    reference = Util.unescape(reference);
                }

                // get old value
                String oldValue = TreeTableManager.getText(selectedRow, selectedColumn);
                if (View.this.multilineEditor == true)
                {
                    oldValue = Util.unescape(oldValue);
                }

                View.this.validateToolItem.setEnabled(View.this.deletableRows.size() > 0 || oldValue.equals(View.this.editorStyledText.getText()) == false);
                View.this.cancelToolItem.setEnabled(View.this.editorStyledText.getText().equals("") == true || (oldValue.equals(View.this.editorStyledText.getText()) == false && !(oldValue.equals("") == true && View.this.editorStyledText.getText().equals(reference) == true)));

                // found params & references for editor
                View.this.editorParams = Util.getParams(0, View.this.editorStyledText.getText());
                View.this.editorReferences = Util.getReferences(0, View.this.editorStyledText.getText());
                View.this.editorUrls = Util.getUrls(0, View.this.editorStyledText.getText());

                // apply styles for editorStyledText
                View.this.formatStyledText(View.this.editorStyledText, View.this.editorParams, View.this.editorReferences, View.this.editorUrls, false);

                // search unknown params
                for (int i = 0; i < View.this.editorParams.size(); i++)
                {
                    boolean found = false;
                    for (int j = 0; j < View.this.infoParams.size(); j++)
                    {
                        if (View.this.editorParams.get(i)[2] == View.this.infoParams.get(j)[2])
                        {
                            found = true;
                            break;
                        }
                    }

                    if (found == false)
                    {
                        // set not found param style
                        StyleRange styleRange = new StyleRange(View.this.editorParams.get(i)[0], View.this.editorParams.get(i)[1], new Color(Display.getCurrent(), 163, 21, 21), null);
                        styleRange.font = new Font(null, View.this.editorStyledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
                        styleRange.underline = true;
                        styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                        styleRange.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
                        View.this.editorStyledText.setStyleRange(styleRange);
                    }
                }

                // Search unknown references
                for (int i = 0; i < View.this.editorReferences.size(); i++)
                {
                    boolean found = false;
                    for (int j = 0; j < View.this.infoReferences.size(); j++)
                    {
                        if (View.this.editorReferences.get(i)[2].equals(View.this.infoReferences.get(j)[2]) == true)
                        {
                            found = true;
                            break;
                        }
                    }

                    if (found == false)
                    {
                        // set not found references style
                        StyleRange styleRange = new StyleRange((int) View.this.editorReferences.get(i)[0] + 1, (int) View.this.editorReferences.get(i)[1] - 2, Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
                        styleRange.font = new Font(null, View.this.editorStyledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
                        styleRange.underline = true;
                        styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                        styleRange.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
                        View.this.editorStyledText.setStyleRange(styleRange);
                    }
                }

            }
        });
        this.editorStyledText.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                View.this.pasteToolItem.setEnabled(View.this.clipboard.getContents(TextTransfer.getInstance()) != null);
                View.this.selectEditor();
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                View.this.valid(false);
            }
        });
        this.editorStyledText.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.character == SWT.ESC)
                {
                    e.doit = false;
                    if (View.this.cancelToolItem.getEnabled() == true)
                    {
                        View.this.cancel();
                    }
                }
            }
        });
        this.editorStyledText.addVerifyKeyListener(new VerifyKeyListener()
        {
            @Override
            public void verifyKey(VerifyEvent e)
            {
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.character == SWT.CR)
                {
                    if (View.this.multilineEditor == false)
                    {
                        e.doit = false;
                        if (View.this.validateToolItem.getEnabled() == true)
                        {
                            View.this.valid(true);
                        }
                        TreeTableManager.Cursor.notifyListeners(SWT.KeyDown, Util.createKeyEvent(TreeTableManager.getCurrent(), SWT.ARROW_DOWN));
                        View.this.selectEditor();
                    }
                    else
                    {
                        Point selection = View.this.editorStyledText.getSelection();
                        View.this.editorStyledText.insert(Character.toString(SWT.LF));
                        selection.x = selection.x + 1;
                        selection.y = selection.x;
                        View.this.editorStyledText.setSelection(selection);
                        e.doit = false;
                    }
                }
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.PAGE_UP)
                {
                    e.doit = false;
                    if (View.this.validateToolItem.getEnabled() == true)
                    {
                        View.this.valid(false);
                    }
                    Item selectedRow = null;
                    TreeTableManager.setRedraw(false, false);
                    do
                    {
                        selectedRow = TreeTableManager.Cursor.getRow();
                        TreeTableManager.Cursor.notifyListeners(SWT.KeyDown, Util.createKeyEvent(TreeTableManager.getCurrent(), SWT.ARROW_UP));
                    }
                    while (selectedRow != TreeTableManager.Cursor.getRow() && (boolean) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_EXIST) == false);
                    TreeTableManager.setRedraw(true, false);
                    View.this.selectEditor();
                }
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.PAGE_DOWN)
                {
                    e.doit = false;
                    if (View.this.validateToolItem.getEnabled() == true)
                    {
                        View.this.valid(false);
                    }
                    Item selectedRow = null;
                    TreeTableManager.setRedraw(false, false);
                    do
                    {
                        selectedRow = TreeTableManager.Cursor.getRow();
                        TreeTableManager.Cursor.notifyListeners(SWT.KeyDown, Util.createKeyEvent(TreeTableManager.getCurrent(), SWT.ARROW_DOWN));
                    }
                    while (selectedRow != TreeTableManager.Cursor.getRow() && (boolean) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_EXIST) == false);
                    TreeTableManager.setRedraw(true, false);
                    View.this.selectEditor();
                }
                if (e.character == SWT.TAB && View.this.multilineEditor == false)
                {
                    e.doit = false;

                    if (View.this.validateToolItem.getEnabled() == true)
                    {
                        View.this.valid(false);
                    }
                    Item selectedRow = null;
                    TreeTableManager.setRedraw(false, false);
                    do
                    {
                        selectedRow = TreeTableManager.Cursor.getRow();
                        TreeTableManager.Cursor.notifyListeners(SWT.KeyDown, Util.createKeyEvent(TreeTableManager.getCurrent(), SWT.ARROW_DOWN));
                    }
                    while (selectedRow != TreeTableManager.Cursor.getRow() && (boolean) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_EXIST) == false);
                    TreeTableManager.setRedraw(true, false);
                    View.this.selectEditor();
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'z')
                {
                    View.this.undoToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'y')
                {
                    View.this.redoToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'x')
                {
                    View.this.cutToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'c')
                {
                    View.this.copyToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'v')
                {
                    View.this.pasteToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'a')
                {
                    View.this.selectAllToolItem.notifyListeners(SWT.Selection, null);
                }
            }
        });

        this.updateTreeTable(true, true);

        this.statusLabel = new Label(AreaContainer.getComposite(), SWT.NULL);
    }

    void itemEnterEventOccurred(Item item, int columnIndex)
    {
        String key = (String) item.getData(TreeTableManager.DATAKEY_KEY);
        String message = "";
        if (columnIndex == 0)
        {
            if (TreeTableManager.Cursor.getColumn() < 2 || TreeTableManager.getChildItemCount(item) == 0)
            {
                return;
            }
            int[] counts = this.getCounts(key, null, TreeTableManager.Cursor.getColumn());
            message += this.getLocalisedMessageText("i18nAZ.ToolTips.Items.ExpandMessage", new String[] { String.valueOf(counts[0]), String.valueOf(counts[1]), String.valueOf(counts[2]), String.valueOf(counts[3]) });
        }
        else
        {
            if ((Boolean) item.getData(TreeTableManager.DATAKEY_EXIST) == false)
            {
                return;
            }
            message = TreeTableManager.getText(item, columnIndex);
            if (this.multilineEditor == false)
            {
                message = Util.unescape(message);
            }
        }
        ToolTipText.set(item, columnIndex, null, new String[] { key }, new String[] { message });
    }

    void populateMenu(final Menu menu, final int visible)
    {
        this.populateMenu(menu, visible, visible);
    }

    void populateMenu(final Menu menu, final int visible, int enabled)
    {
        while (menu.getItemCount() > 0)
        {
            menu.getItem(0).dispose();
        }
        MenuItem menuItem = null;

        if ((visible & MenuOptions.REMOVE_COLUMN) != 0)
        {
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.RemoveLanguage", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.removeLanguage();
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.removeLanguageButton.isDisabled() == false && ((enabled & MenuOptions.REMOVE_COLUMN) != 0));
        }

        // EDITOR
        if ((visible & MenuOptions.EDITOR) != 0)
        {
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Undo", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.undoToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.undoToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Redo", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.redoToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.redoToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            new MenuItem(menu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Cut", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.cutToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.cutToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Copy", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.copyToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.copyToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));
        }

        // ROW_COPY
        if ((visible & MenuOptions.ROW_COPY) != 0)
        {
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.CopyKey", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    String textData = (String) item.getData(TreeTableManager.DATAKEY_KEY);
                    View.this.clipboard.setContents(new Object[] { textData }, new Transfer[] { TextTransfer.getInstance() });
                }
            }, SWT.PUSH);
            menuItem.setEnabled(true && ((enabled & MenuOptions.ROW_COPY) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.CopyReference", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    String textData = TreeTableManager.getText(item, 1);
                    View.this.clipboard.setContents(new Object[] { textData }, new Transfer[] { TextTransfer.getInstance() });
                }
            }, SWT.PUSH);
            menuItem.setEnabled(true && ((enabled & MenuOptions.ROW_COPY) != 0));
        }

        // EDITOR
        if ((visible & MenuOptions.EDITOR) != 0)
        {
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Paste", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.pasteToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.pasteToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            new MenuItem(menu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.SelectAll", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.selectAllToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.selectAllToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            new MenuItem(menu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Uppercase", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.upperCaseToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.upperCaseToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Lowercase", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.lowerCaseToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.lowerCaseToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Firstcase", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.firstCaseToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.firstCaseToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));
        }

        // OPEN URL
        if ((visible & MenuOptions.OPEN_URL) != 0)
        {
            if (menu.getItemCount() > 0)
            {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.OpenUrl", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    int columnIndex = (int) menu.getData(TreeTableManager.DATAKEY_COLUMN_INDEX);
                    Utils.launch(TreeTableManager.getText(item, columnIndex));
                }
            }, SWT.PUSH);
            menuItem.setEnabled(true && ((enabled & MenuOptions.OPEN_URL) != 0));
        }

        // SEARCH
        if ((visible & MenuOptions.SEARCH) != 0)
        {
            if (menu.getItemCount() > 0)
            {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Find", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    View.this.searchTextbox.getTextControl().setFocus();
                    View.this.searchTextbox.getTextControl().selectAll();
                }
            }, SWT.PUSH);
            menuItem.setEnabled(true && ((enabled & MenuOptions.SEARCH) != 0));
        }

        // FILTERS & TOPFILTERS
        if ((visible & MenuOptions.FILTERS) != 0 || (visible & MenuOptions.TOPFILTERS) != 0)
        {

            if ((visible & MenuOptions.ROW_COPY) != 0 && (visible & MenuOptions.SEARCH) == 0)
            {
                new MenuItem(menu, SWT.SEPARATOR);
            }

            Menu topMenu = menu;
            if ((visible & MenuOptions.TOPFILTERS) != 0)
            {
                menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Filters", new Listener()
                {
                    @Override
                    public void handleEvent(Event e)
                    {

                    }
                }, SWT.CASCADE);
                menuItem.setEnabled(((enabled & MenuOptions.TOPFILTERS) != 0));

                topMenu = new Menu(menu);
                menuItem.setMenu(topMenu);

            }

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.EmptyFilter", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.emptyFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.emptyFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);

                    if (((MenuItem) e.widget).getSelection() == View.this.emptyFilter)
                    {
                        View.this.emptyFilterExcludedKey.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    else
                    {
                        View.this.emptyFilterExcludedKey.add((String) item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.emptyFilterButton.isDisabled() == false));

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.UnchangedFilter", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.unchangedFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.unchangedFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);

                    if (((MenuItem) e.widget).getSelection() == View.this.unchangedFilter)
                    {
                        View.this.unchangedFilterExcludedKey.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    else
                    {
                        View.this.unchangedFilterExcludedKey.add((String) item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.unchangedFilterButton.isDisabled() == false));

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.ExtraFilter", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.extraFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.extraFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);

                    if (((MenuItem) e.widget).getSelection() == View.this.extraFilter)
                    {
                        View.this.extraFilterExcludedKey.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    else
                    {
                        View.this.extraFilterExcludedKey.add((String) item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.extraFilterButton.isDisabled() == false));

            new MenuItem(topMenu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.RedirectKeysFilter", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.redirectKeysFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.redirectKeysFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);

                    if (((MenuItem) e.widget).getSelection() == View.this.redirectKeysFilter)
                    {
                        View.this.hideRedirectKeysFilterExcludedKey.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    else
                    {
                        View.this.hideRedirectKeysFilterExcludedKey.add((String) item.getData(TreeTableManager.DATAKEY_KEY));
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.redirectKeysFilterButton.isDisabled() == false));

            new MenuItem(topMenu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.HideUrlsFilter", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        if (View.this.urlsFilter != 1)
                        {
                            View.this.setUrlsFilterState(1);
                        }
                        else
                        {
                            View.this.setUrlsFilterState(0);
                        }
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    if (View.this.urlsFilter == 1)
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            View.this.urlsFilterOverriddenStates.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                        }
                        else
                        {
                            View.this.urlsFilterOverriddenStates.put((String) item.getData(TreeTableManager.DATAKEY_KEY), 0);
                            TreeTableManager.setExpanded(item, true);
                        }
                    }
                    else
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            View.this.urlsFilterOverriddenStates.put((String) item.getData(TreeTableManager.DATAKEY_KEY), 1);
                        }
                        else
                        {
                            View.this.urlsFilterOverriddenStates.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                            TreeTableManager.setExpanded(item, true);
                        }
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.urlsFilterButton.isDisabled() == false));

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.ShowUrlsFilter", new Listener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        if (View.this.urlsFilter != 2)
                        {
                            View.this.setUrlsFilterState(2);
                        }
                        else
                        {
                            View.this.setUrlsFilterState(0);
                        }
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    if (View.this.urlsFilter == 2)
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            View.this.urlsFilterOverriddenStates.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                        }
                        else
                        {
                            View.this.urlsFilterOverriddenStates.put((String) item.getData(TreeTableManager.DATAKEY_KEY), 0);
                        }
                    }
                    else
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            View.this.urlsFilterOverriddenStates.put((String) item.getData(TreeTableManager.DATAKEY_KEY), 2);
                        }
                        else
                        {
                            View.this.urlsFilterOverriddenStates.remove(item.getData(TreeTableManager.DATAKEY_KEY));
                        }
                    }
                    TreeTableManager.setExpanded(item, true);
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.urlsFilterButton.isDisabled() == false));

        }
    }

    void removeLanguage()
    {
        TreeTableManager.getCurrent().setFocus();

        final int columnIndex = TreeTableManager.Cursor.getColumn();
        if (columnIndex != -1)
        {
            // Item column = TreeTableManager.getColumn(columnIndex);
            final Locale selectedLocale = (Locale) TreeTableManager.getColumn(columnIndex).getData(View.DATAKEY_LOCALE);

            MessageBox messageBox = new MessageBox(SWTSkinFactory.getInstance().getShell().getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            messageBox.setMessage(View.this.getLocalisedMessageText("i18nAZ.Messages.Remove"));

            int result = messageBox.open();
            if (result == SWT.YES)
            {
                File vuzeDirectory = new File(View.this.getPluginInterface().getUtilities().getAzureusProgramDir());
                for (int j = 0; j < this.pluginInterfaces.length; j++)
                {
                    BundleObject bundleObject = new BundleObject(this.pluginInterfaces[j]);
                    if (bundleObject.isValid() == true)
                    {
                        String localizedBundleName = bundleObject.getName() + "_" + selectedLocale.toLanguageTag().replace('-', '_');
                        File localfile = new File(View.this.getPluginInterface().getPluginDirectoryName().toString() + "\\internat\\" + bundleObject.getPluginName() + "\\" + localizedBundleName + BundleObject.EXTENSION);
                        if ((localfile.isFile()) && (localfile.exists()))
                        {
                            localfile.delete();
                        }
                    }

                }
                String fileName = BundleObject.DEFAULT_NAME + "_" + selectedLocale.toLanguageTag().replace('-', '_');
                File mergedLocalfile = new File(vuzeDirectory + File.separator + fileName + BundleObject.EXTENSION);
                if ((mergedLocalfile.isFile()) && (mergedLocalfile.exists()))
                {
                    mergedLocalfile.delete();
                }
            }
            // remove column
            TreeTableManager.removeColumns(columnIndex);
            for (int i = 0; i < this.localesProperties.size(); i++)
            {
                if (this.localesProperties.get(i).locale == selectedLocale)
                {
                    this.localesProperties.remove(i);
                    break;
                }
            }
            List<String> locales = new ArrayList<String>();
            for (int i = 0; i < this.localesProperties.size(); i++)
            {
                Locale locale = this.localesProperties.get(i).locale;
                if ((locale.toLanguageTag() != null) && (!locale.toLanguageTag().equals("")) && (!locale.toLanguageTag().equals("und")))
                {
                    locales.add(locale.toLanguageTag());
                }
            }
            COConfigurationManager.setParameter("i18nAZ.LocalesSelected", locales);
            COConfigurationManager.save();
            View.this.updateTreeTable(false, false);

        }
    }

    void selectEditor()
    {
        this.editorStyledText.selectAll();
        if (this.editorStyledText.isFocusControl() == false)
        {
            this.editorStyledText.setFocus();
        }
        this.cutToolItem.setEnabled(true);
        this.copyToolItem.setEnabled(true);
        this.selectAllToolItem.setEnabled(false);
    }

    private void setEmptyFilter(boolean checked)
    {
        this.emptyFilterExcludedKey.clear();
        View.this.emptyFilter = checked;
        COConfigurationManager.setParameter("i18nAZ.emptyFilter", View.this.emptyFilter);
        COConfigurationManager.save();
        View.this.emptyFilterButton.getSkinObject().switchSuffix(checked ? "-selected" : "", 4, true);
    }

    private void setExtraFilter(boolean checked)
    {
        this.extraFilterExcludedKey.clear();
        View.this.extraFilter = checked;
        COConfigurationManager.setParameter("i18nAZ.extraFilter", View.this.extraFilter);
        COConfigurationManager.save();
        View.this.extraFilterButton.getSkinObject().switchSuffix(checked ? "-selected" : "", 4, true);
    }

    private void setUrlsFilterState()
    {
        this.urlsFilterOverriddenStates.clear();
        switch (View.this.urlsFilter)
        {
            case 0:
                View.this.setUrlsFilterState(1);
                break;
            case 1:
                View.this.setUrlsFilterState(2);
                break;
            case 2:
                View.this.setUrlsFilterState(0);
                break;
        }
    }

    private void setUrlsFilterState(int state)
    {
        if (TreeTableManager.getCurrent() != null)
        {
            TreeTableManager.getCurrent().setFocus();
        }
        View.this.urlsFilter = state;
        switch (state)
        {
            case 0:
                View.this.urlsFilterButton.setImage("i18nAZ.image.toolbar.urlsFilter");
                View.this.checkButton(View.this.urlsFilterButton, false);
                ToolTipText.set(View.this.urlsFilterButton.getSkinObject().getControl(), "i18nAZ.ToolTips.UrlsFilter.State1");
                break;
            case 1:
                View.this.urlsFilterButton.setImage("i18nAZ.image.toolbar.urlsFilterOff");
                View.this.checkButton(View.this.urlsFilterButton, true);
                ToolTipText.set(View.this.urlsFilterButton.getSkinObject().getControl(), "i18nAZ.ToolTips.UrlsFilter.State2");
                break;
            case 2:
                View.this.urlsFilterButton.setImage("i18nAZ.image.toolbar.urlsFilterOn");
                View.this.checkButton(View.this.urlsFilterButton, true);
                ToolTipText.set(View.this.urlsFilterButton.getSkinObject().getControl(), "i18nAZ.ToolTips.UrlsFilter.State3");
                break;
        }
        if (TreeTableManager.getCurrent() != null)
        {
            View.this.updateTreeTable();
        }
        COConfigurationManager.setParameter("i18nAZ.urlsFilter", View.this.urlsFilter);
        COConfigurationManager.save();
    }

    private void setSearch(String text)
    {
        this.searchPrefixes = new HashSet<String>();

        text = (text == null) ? "" : text;
        String prefix = "";
        while (text.indexOf(':') != -1)
        {
            prefix = text.split(":", 2)[0];
            boolean valid = true;
            for (int i = 0; i < prefix.length(); i++)
            {
                char c = prefix.charAt(i);
                if (c != 'k' && c != 'r' && c != 'c')
                {
                    valid = false;
                    break;
                }
            }
            if (valid == true)
            {
                for (int i = 0; i < prefix.length(); i++)
                {
                    this.searchPrefixes.add(Character.toString(prefix.charAt(i)));
                }
                text = text.split(":", 2)[1];
                continue;
            }
            boolean found = true;
            Locale[] AvailableLocales = Locale.getAvailableLocales();
            for (int i = 0; i < AvailableLocales.length; i++)
            {
                if (AvailableLocales[i].toLanguageTag().equals("") == false && AvailableLocales[i].toLanguageTag().equals("und") == false && AvailableLocales[i].toLanguageTag().equals(prefix) == true)
                {
                    found = true;
                    break;
                }
            }
            if (found == true)
            {
                this.searchPrefixes.add(prefix);
                text = text.split(":", 2)[1];
                continue;
            }
            break;
        }
        if (this.searchPrefixes.size() == 0)
        {
            this.searchPrefixes = null;
        }

        text = text.replace("\\|", Character.toString((char) 0));
        text = text.replace("\\!", Character.toString((char) 1));

        this.searchPatterns = new HashMap<Pattern, Object>();

        String[] phrases = text.split("\\|");
        for (int i = 0; i < phrases.length; i++)
        {
            if (phrases[i].equals("") == false)
            {
                boolean searchResult = true;
                if (phrases[i].startsWith("!"))
                {
                    searchResult = false;
                    phrases[i] = phrases[i].substring(1);
                }
                phrases[i] = phrases[i].replace(Character.toString((char) 0), "\\|");
                phrases[i] = phrases[i].replace(Character.toString((char) 1), "!");

                String search = (this.regexSearch == true) ? phrases[i] : ("\\Q" + phrases[i].replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E");

                this.searchPatterns.put(Pattern.compile(search, 2), searchResult);
            }
        }
        if (this.searchPatterns.size() == 0)
        {
            this.searchPatterns = null;
            this.searchPrefixes = null;
        }
        this.updateTreeTable();
    }

    void setUnchangedFilter(boolean checked)
    {
        this.unchangedFilterExcludedKey.clear();
        View.this.unchangedFilter = checked;
        COConfigurationManager.setParameter("i18nAZ.unchangedFilter", View.this.unchangedFilter);
        COConfigurationManager.save();
        View.this.unchangedFilterButton.getSkinObject().switchSuffix(checked ? "-selected" : "", 4, true);
    }

    void startSaveThread()
    {
        this.saveThread = new Thread(new Runnable()
        {
            public void setInfo(final String info)
            {
                View.this.display.asyncExec(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        View.this.statusLabel.setText(info);
                        View.this.statusLabel.getParent().layout();
                    }
                });
            }

            @Override
            public void run()
            {
                try
                {
                    while (View.this.isCreated == true)
                    {
                        this.setInfo(View.this.getLocalisedMessageText("i18nAZ.Messages.Status.Ready"));

                        Thread.sleep(1000);

                        SaveObject saveObject = null;
                        synchronized (View.this.saveObjects)
                        {
                            if (View.this.saveObjects.size() == 0)
                            {
                                continue;
                            }
                            saveObject = View.this.saveObjects.get(0);
                            View.this.saveObjects.remove(0);
                        }

                        String currentSaveName = saveObject.getFile().getParentFile().getName() + File.pathSeparator + saveObject.getFile().getName();
                        this.setInfo(View.this.getLocalisedMessageText("i18nAZ.Messages.Status.Saving"));

                        // clean empty reference
                        while (true)
                        {
                            boolean loop = false;
                            for (Iterator<String> iterator = saveObject.getCommentedProperties().stringPropertyNames().iterator(); iterator.hasNext();)
                            {
                                String key = iterator.next();
                                if (saveObject.getCommentedProperties().getProperty(key).equals("") == true)
                                {
                                    saveObject.getCommentedProperties().remove(key);
                                    loop = true;
                                    break;
                                }

                            }
                            if (loop == false)
                            {
                                break;
                            }
                        }

                        String result = Util.saveLocaleProperties(saveObject.getCommentedProperties(), saveObject.getFile());

                        // dispose
                        saveObject.dispose();
                        saveObject = null;

                        if (result != null)
                        {
                            this.setInfo(View.this.getLocalisedMessageText("i18nAZ.Messages.Status.ErrorSave", new String[] { currentSaveName, result }));
                            Thread.sleep(10000);
                            continue;
                        }

                        // merge and store all bundle files for viewing in Vuze
                        synchronized (View.this.saveObjects)
                        {
                            if (View.this.saveObjects.size() > 0)
                            {
                                continue;
                            }
                        }

                        // get Buze directory
                        File vuzeDirectory = new File(i18nAZ.viewInstance.getPluginInterface().getUtilities().getAzureusProgramDir());
                        for (int i = 0; i < View.this.localesProperties.size(); i++)
                        {
                            // merge plugins properties
                            CommentedProperties mergedlocaleProperties = new CommentedProperties();
                            for (int j = 0; j < i18nAZ.viewInstance.pluginInterfaces.length; j++)
                            {
                                BundleObject bundleObject = new BundleObject(i18nAZ.viewInstance.pluginInterfaces[j]);
                                if (bundleObject.isValid() == true)
                                {
                                    String localizedBundleName = bundleObject.getName();
                                    if (i > 0)
                                    {
                                        localizedBundleName = localizedBundleName + "_" + View.this.localesProperties.get(i).locale.toLanguageTag().replace('-', '_');
                                    }
                                    File localfile = new File(i18nAZ.viewInstance.getPluginInterface().getPluginDirectoryName().toString() + "\\internat\\" + bundleObject.getPluginName() + "\\" + localizedBundleName + BundleObject.EXTENSION);
                                    if ((localfile.isFile()) && (localfile.exists()))
                                    {
                                        CommentedProperties localeProperties = Util.getLocaleProperties(localfile);
                                        if (localeProperties != null && localeProperties.IsLoaded() == true)
                                        {
                                            for (Iterator<String> iterator = localeProperties.stringPropertyNames().iterator(); iterator.hasNext();)
                                            {
                                                String key = iterator.next();
                                                mergedlocaleProperties.put(key, localeProperties.getProperty(key));
                                            }
                                        }
                                    }
                                }
                            }

                            // set merge file
                            String fileName = BundleObject.DEFAULT_NAME;
                            if (i > 0)
                            {
                                fileName = fileName + "_" + View.this.localesProperties.get(i).locale.toLanguageTag().replace('-', '_');
                            }

                            File localFile = new File(vuzeDirectory + File.separator + fileName + BundleObject.EXTENSION);

                            // save
                            result = Util.saveLocaleProperties(mergedlocaleProperties, localFile);
                            if (result != null)
                            {
                                this.setInfo(View.this.getLocalisedMessageText("i18nAZ.Messages.Status.ErrorGlobalSave", new String[] { fileName, result }));
                                Thread.sleep(10000);
                                break;
                            }
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }

            }
        }, "i18nAZ.saveThread");
        this.saveThread.setDaemon(true);
        this.saveThread.start();
    }

    private void updateInfoText()
    {

        String localisedMessageText = "";
        if (TreeTableManager.getItemCount() == 0)
        {
            if (View.this.searchPatterns == null)
            {
                localisedMessageText = View.this.getLocalisedMessageText("i18nAZ.Labels.Noentry");
            }
            else
            {
                localisedMessageText = View.this.getLocalisedMessageText("i18nAZ.Labels.UnsuccessfulSearch");
            }
        }
        else
        {            
            final int columnIndex = TreeTableManager.Cursor.getColumn();
            if (columnIndex >= 2)
            {
                
                Thread infoThread = new Thread(new Runnable()
                {
                    @Override
                    synchronized public void run()
                    {
                        String localisedMessageText = "";
                        int[] counts = View.this.getCounts("", null, columnIndex);
                        if (View.this.searchPatterns == null)
                        {
                            localisedMessageText = View.this.getLocalisedMessageText("i18nAZ.Labels.Informations.Prefix");
                        }
                        else
                        {
                            localisedMessageText = View.this.getLocalisedMessageText("i18nAZ.Labels.SearchResult.Prefix");
                        }
                        localisedMessageText += View.this.getLocalisedMessageText("i18nAZ.Labels.Informations", new String[] { String.valueOf(counts[0]), String.valueOf(counts[1]), String.valueOf(counts[2]), String.valueOf(counts[3]) });

                        
                        final String finalLocalisedMessageText = localisedMessageText;
                        View.this.display.asyncExec(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                View.this.updateToolTipColumnHeader(TreeTableManager.getColumn(columnIndex));
                                View.this.infoText.setText(finalLocalisedMessageText);
                            }
                        });
                        
                    }
                }, "i18nAZ.infoText");
                infoThread.setDaemon(true);
                infoThread.start();
                return;
            }
            else
            {
                localisedMessageText = View.this.getLocalisedMessageText("i18nAZ.Labels.Nolanguage");
            }
        }
        View.this.infoText.setText(localisedMessageText);      
    }

    void updateStyledTexts()
    {
        // set enabled tool item
        this.cutToolItem.setEnabled(false);
        this.copyToolItem.setEnabled(false);
        this.pasteToolItem.setEnabled(View.this.clipboard.getContents(TextTransfer.getInstance()) != null);
        this.selectAllToolItem.setEnabled(true);
        this.upperCaseToolItem.setEnabled(false);
        this.lowerCaseToolItem.setEnabled(false);
        this.firstCaseToolItem.setEnabled(false);
        this.validateToolItem.setEnabled(false);
        this.cancelToolItem.setEnabled(false);

        // get layout datas
        GridData parentgridData = (GridData) this.editorStyledText.getParent().getLayoutData();
        GridData infogridData = (GridData) this.infoStyledText.getParent().getLayoutData();
        GridData toolBargridData = (GridData) this.toolBar.getParent().getLayoutData();
        GridData valuegridData = (GridData) this.editorStyledText.getLayoutData();

        // show/hide label & editor
        if (TreeTableManager.getSelection().length == 0 || TreeTableManager.getColumnCount() == 0)
        {
            // set datas
            this.editorStyledText.setData(View.DATAKEY_SELECTED_ROW, null);
            this.editorStyledText.setData(View.DATAKEY_SELECTED_COLUMN, 0);

            // set heights
            parentgridData.heightHint = 0;
            infogridData.heightHint = 0;
            toolBargridData.heightHint = 0;
            valuegridData.heightHint = 0;

            // set visible
            this.editorStyledText.getParent().setVisible(false);

            // layout all
            this.editorStyledText.getParent().layout();
            this.editorStyledText.getParent().getParent().layout();
            return;
        }

        // set heights
        parentgridData.heightHint = SWT.DEFAULT;
        infogridData.heightHint = SWT.DEFAULT;

        // set visible
        this.editorStyledText.getParent().setVisible(true);

        // get selected row
        Item selectedRow = TreeTableManager.Cursor.getRow();

        // get selected column
        int selectedColumn = TreeTableManager.Cursor.getColumn();

        // get locales
        Locale DefaultLocale = (Locale) TreeTableManager.getColumn(1).getData(View.DATAKEY_LOCALE);
        Locale selectedLocale = (Locale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_LOCALE);

        // get exist
        boolean exist = (boolean) selectedRow.getData(TreeTableManager.DATAKEY_EXIST);

        // get key
        String key = (String) selectedRow.getData(TreeTableManager.DATAKEY_KEY);

        // get referenve
        String reference = TreeTableManager.getText(selectedRow, 1);
        if (View.this.multilineEditor == true)
        {
            reference = Util.unescape(reference);
        }

        // get current value
        String currentValue = TreeTableManager.getText(selectedRow, selectedColumn);
        if (View.this.multilineEditor == true)
        {
            currentValue = Util.unescape(currentValue);
        }

        // get key info
        String keyInfo = key;
        if (selectedRow != null && TreeTableManager.getChildItemCount(selectedRow) > 0 && selectedColumn >= 2)
        {
            int[] counts = this.getCounts(key, null, TreeTableManager.Cursor.getColumn());
            keyInfo += " - " + this.getLocalisedMessageText("i18nAZ.Labels.Comments", new String[] { String.valueOf(counts[0]), String.valueOf(counts[1]), String.valueOf(counts[2]), String.valueOf(counts[3]) });;
        }
        keyInfo += "\n";

        // get reference info
        String currentReferenceInfo = (reference == "") ? "" : "\n" + reference;

        // get comments info
        String[] commentsLines = (String[]) selectedRow.getData(TreeTableManager.DATAKEY_COMMENTS);
        String commentsInfo = "";
        if (commentsLines != null)
        {
            for (int i = 0; i < commentsLines.length; i++)
            {
                commentsInfo += commentsLines[i].replaceAll("\\n", "\\\\n") + "\n";
            }
            commentsInfo = Util.trimNewLine(commentsInfo);
            commentsInfo = (commentsInfo == "") ? "" : "\n\n" + commentsInfo;
        }

        // get references info
        String referencesInfo = "";
        if (selectedColumn >= 2)
        {
            String[] refs = Util.getReferences((String) this.getLocalesProperties(DefaultLocale).commentedProperties.get(key));
            while (true)
            {
                if (refs.length > 0)
                {
                    List<String> values = new ArrayList<String>();
                    for (int i = 0; i < refs.length; i++)
                    {
                        referencesInfo += refs[i] + " => ";
                        String ref = refs[i].substring(1, refs[i].length() - 1);;
                        String value = "";
                        if (this.getLocalesProperties(selectedLocale).commentedProperties.containsKey(ref))
                        {
                            value = (String) this.getLocalesProperties(selectedLocale).commentedProperties.get(ref);
                        }
                        else if (this.getLocalesProperties(DefaultLocale).commentedProperties.containsKey(ref))
                        {
                            value = (String) this.getLocalesProperties(DefaultLocale).commentedProperties.get(ref);
                        }
                        else if (this.getPluginInterface().getUtilities().getLocaleUtilities().hasLocalisedMessageText(ref))
                        {
                            value = this.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(ref);
                        }
                        else
                        {
                            value = "???";
                        }
                        if (View.this.multilineEditor == false)
                        {
                            value = Util.escape(value, false);
                        }
                        referencesInfo += value + "\n";
                        values.add(value);
                    }
                    refs = Util.getReferences(values.toArray(new String[values.size()]));
                    continue;
                }
                break;
            }
            referencesInfo = Util.trimNewLine(referencesInfo);
            referencesInfo = (referencesInfo == "") ? "" : ((commentsInfo == "") ? "\n\n" : "\n") + referencesInfo;
        }

        // set data for info
        this.infoStyledText.setText(keyInfo + currentReferenceInfo + commentsInfo + referencesInfo);

        // set default style for info
        StyleRange styleRange = new StyleRange(0, this.infoStyledText.getText().length(), this.infoStyledText.getForeground(), null);
        styleRange.font = this.infoStyledText.getFont();
        this.infoStyledText.setStyleRange(styleRange);

        // set styles for key
        styleRange = new StyleRange(0, key.length(), this.infoStyledText.getForeground(), null);
        styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
        this.infoStyledText.setStyleRange(styleRange);

        // set styles for info key
        styleRange = new StyleRange(key.length(), keyInfo.length() - key.length(), this.infoStyledText.getForeground(), null);
        styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 9, SWT.ITALIC);
        this.infoStyledText.setStyleRange(styleRange);

        // set styles for reference
        styleRange = new StyleRange(keyInfo.length(), currentReferenceInfo.length(), this.infoStyledText.getForeground(), null);
        styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
        this.infoStyledText.setStyleRange(styleRange);

        // set styles for comment
        styleRange = new StyleRange(keyInfo.length() + currentReferenceInfo.length(), commentsInfo.length(), this.infoStyledText.getForeground(), null);
        styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 8, SWT.ITALIC);
        this.infoStyledText.setStyleRange(styleRange);

        // set styles for info reference
        styleRange = new StyleRange(keyInfo.length() + currentReferenceInfo.length() + commentsInfo.length(), referencesInfo.length(), this.infoStyledText.getForeground(), null);
        styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 8, SWT.ITALIC);
        this.infoStyledText.setStyleRange(styleRange);

        if (this.searchPatterns != null && (this.searchPrefixes == null || this.searchPrefixes.contains("r") == true))
        {
            for (Iterator<Entry<Pattern, Object>> iterator = this.searchPatterns.entrySet().iterator(); iterator.hasNext();)
            {
                Entry<Pattern, Object> entry = iterator.next();
                Pattern searchPattern = entry.getKey();
                boolean searchResult = (boolean) entry.getValue();
                Matcher matcher = searchPattern.matcher(currentReferenceInfo);
                matcher.reset();
                while (matcher.find() == searchResult)
                {
                    styleRange = new StyleRange(keyInfo.length() + matcher.start(), matcher.end() - matcher.start(), this.infoStyledText.getForeground(), Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
                    this.infoStyledText.setStyleRange(styleRange);
                }
            }
        }
        if (this.searchPatterns != null && (this.searchPrefixes == null || this.searchPrefixes.contains("c") == true))
        {
            for (Iterator<Entry<Pattern, Object>> iterator = this.searchPatterns.entrySet().iterator(); iterator.hasNext();)
            {
                Entry<Pattern, Object> entry = iterator.next();
                Pattern searchPattern = entry.getKey();
                boolean searchResult = (boolean) entry.getValue();
                Matcher matcher = searchPattern.matcher(commentsInfo);
                matcher.reset();
                while (matcher.find() == searchResult)
                {
                    styleRange = new StyleRange(keyInfo.length() + currentReferenceInfo.length() + matcher.start(), matcher.end() - matcher.start(), this.infoStyledText.getForeground(), Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
                    this.infoStyledText.setStyleRange(styleRange);
                }
            }
        }
        // set data for info
        this.infoStyledText.setData(View.DATAKEY_DEFAULT_STYLES, this.infoStyledText.getStyleRanges(true));

        // found params & references for info
        this.infoParams = Util.getParams(keyInfo.length(), currentReferenceInfo);
        this.infoReferences = Util.getReferences(keyInfo.length(), currentReferenceInfo);
        this.infoUrls = Util.getUrls(keyInfo.length(), currentReferenceInfo);

        // apply styles for infoStyledText
        this.formatStyledText(this.infoStyledText, this.infoParams, this.infoReferences, this.infoUrls, true);

        // set data for editor
        this.editorStyledText.setData(View.DATAKEY_SELECTED_ROW, selectedRow);
        this.editorStyledText.setData(View.DATAKEY_SELECTED_COLUMN, selectedColumn);

        // set editor value
        this.editorStyledText.setText(currentValue.equals("") == true ? reference : currentValue);

        // show/hide editor
        if (exist == true && selectedColumn >= 2 && !(reference.equals("") == true && currentValue.equals("") == true))
        {
            // set heights
            toolBargridData.heightHint = SWT.DEFAULT;
            valuegridData.heightHint = SWT.DEFAULT;
            valuegridData.minimumHeight = 100;

            // set undo redo
            PluginInterface pluginInterface = View.this.pluginInterfaces[View.this.pluginsCombo.getSelectionIndex()];
            String pluginKey = "(core)";
            if (pluginInterface != null)
            {
                Properties pluginProperties = pluginInterface.getPluginProperties();
                pluginKey = pluginProperties.getProperty("plugin.id") + "_" + pluginProperties.getProperty("plugin.version");
            }
            this.undoRedo.set(pluginKey, key);

            // set visibles
            this.toolBar.getParent().setVisible(true);
            this.editorStyledText.setVisible(true);

        }
        else
        {
            // set heights
            toolBargridData.heightHint = 0;
            valuegridData.heightHint = 0;
            valuegridData.minimumHeight = 0;

            // set visibles
            this.toolBar.getParent().setVisible(false);
            this.editorStyledText.setVisible(false);
        }

        // layout all
        this.editorStyledText.getParent().layout();
        this.editorStyledText.getParent().getParent().layout();
    }

    private void updateToolTipColumnHeader(Item column)
    {
        CommentedProperties localeProperties = null;
        int emptyCount = 0;
        int entryCount = 0;
        for (int i = 0; i < this.keys.size(); i++)
        {
            int state = State.NONE;
            String key = this.keys.get(i);

            localeProperties = this.getLocalesProperties((Locale) TreeTableManager.getColumn(1).getData(View.DATAKEY_LOCALE)).commentedProperties;
            String reference = localeProperties.getProperty(key);

            state = Util.getStateOfReference(reference);
            if ((state & State.URL) != 0 || (state & State.REDIRECT_KEY) != 0)
            {
                continue;
            }
            entryCount++;

            localeProperties = this.getLocalesProperties((Locale) column.getData(View.DATAKEY_LOCALE)).commentedProperties;
            String value = localeProperties.getProperty(key);

            state = Util.getStateOfValue(reference, value);
            if ((state & State.EMPTY) != 0)
            {
                emptyCount++;
            }
        }

        double percent = (1 - ((double) emptyCount) / ((double) entryCount));
        if (percent > 1)
        {
            percent = 1;
        }
        DecimalFormat decimalFormat = new DecimalFormat("00.00 %");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);

        Locale locale = (Locale) column.getData(View.DATAKEY_LOCALE);
        String headerTooltTipText = this.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Title") + "\n\n";
        NumberFormat.getPercentInstance(locale).setMaximumFractionDigits(2);
        NumberFormat.getPercentInstance(locale).setMinimumFractionDigits(2);

        headerTooltTipText += this.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.TranslationProgress", new String[] { decimalFormat.format(percent) }) + "\n";

        if (locale.getDisplayLanguage() != null && locale.getDisplayLanguage().equals("") == false)
        {
            String DisplayLanguage = locale.getDisplayLanguage();
            DisplayLanguage = Character.toTitleCase(DisplayLanguage.charAt(0)) + DisplayLanguage.substring(1).toLowerCase(locale);
            String LocalizedDisplayLanguage = locale.getDisplayLanguage(locale);
            LocalizedDisplayLanguage = Character.toTitleCase(LocalizedDisplayLanguage.charAt(0)) + LocalizedDisplayLanguage.substring(1).toLowerCase(locale);
            headerTooltTipText += this.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Language", new String[] { DisplayLanguage, LocalizedDisplayLanguage }) + "\n";
        }
        if (locale.getDisplayCountry() != null && locale.getDisplayCountry().equals("") == false)
        {
            String DisplayCountry = locale.getDisplayCountry();
            DisplayCountry = Character.toTitleCase(DisplayCountry.charAt(0)) + DisplayCountry.substring(1).toLowerCase(locale);
            String LocalizedDisplayCountry = locale.getDisplayCountry(locale);
            LocalizedDisplayCountry = Character.toTitleCase(LocalizedDisplayCountry.charAt(0)) + LocalizedDisplayCountry.substring(1).toLowerCase(locale);
            headerTooltTipText += this.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Country", new String[] { DisplayCountry, LocalizedDisplayCountry }) + "\n";
        }
        if (locale.getDisplayScript() != null && locale.getDisplayScript().equals("") == false)
        {
            String DisplayScript = locale.getDisplayScript();
            DisplayScript = Character.toTitleCase(DisplayScript.charAt(0)) + DisplayScript.substring(1).toLowerCase(locale);
            String LocalizedDisplayScript = locale.getDisplayScript(locale);
            LocalizedDisplayScript = Character.toTitleCase(LocalizedDisplayScript.charAt(0)) + LocalizedDisplayScript.substring(1).toLowerCase(locale);
            headerTooltTipText += this.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Script", new String[] { DisplayScript, LocalizedDisplayScript }) + "\n";
        }
        if (locale.getDisplayVariant() != null && locale.getDisplayVariant().equals("") == false)
        {
            String DisplayVariant = locale.getDisplayVariant();
            DisplayVariant = Character.toTitleCase(DisplayVariant.charAt(0)) + DisplayVariant.substring(1).toLowerCase(locale);
            String LocalizedDisplayVariant = locale.getDisplayVariant(locale);
            LocalizedDisplayVariant = Character.toTitleCase(LocalizedDisplayVariant.charAt(0)) + LocalizedDisplayVariant.substring(1).toLowerCase(locale);
            headerTooltTipText += this.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Variant", new String[] { DisplayVariant, LocalizedDisplayVariant }) + "\n";
        }
        headerTooltTipText += this.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.LanguageTag", new String[] { locale.toLanguageTag().replace("-", "") });
        TreeTableManager.setToolTipColumnHeader(column, headerTooltTipText);
    }

    private void updateTreeTable()
    {
        this.updateTreeTable(false, true);
    }

    private void updateTreeTable(boolean savePosition)
    {
        this.updateTreeTable(true, savePosition);
    }

    private void updateTreeTable(boolean refreshColumn, boolean savePosition)
    {
        this.addLanguageButton.setDisabled(true);
        this.exportLanguageButton.setDisabled(true);
        this.removeLanguageButton.setDisabled(true);
        this.emptyFilterButton.setDisabled(true);
        this.unchangedFilterButton.setDisabled(true);
        this.extraFilterButton.setDisabled(true);
        this.treeModeButton.setDisabled(true);
        this.redirectKeysFilterButton.setDisabled(true);
        this.urlsFilterButton.setDisabled(true);

        this.deletableRows.clear();
        TreeTableManager.removeAll(savePosition);

        if (refreshColumn == true)
        {
            this.emptyFilterExcludedKey.clear();
            this.unchangedFilterExcludedKey.clear();
            this.extraFilterExcludedKey.clear();
            this.hideRedirectKeysFilterExcludedKey.clear();
            this.urlsFilterOverriddenStates.clear();
            TreeTableManager.removeAllColumns();
            if ((this.currentBundleObject == null || this.currentBundleObject.getPluginInterface() != this.selectedPluginInterface))
            {
                this.keys.clear();
                this.currentBundleObject = new BundleObject(this.selectedPluginInterface);
            }

            if (this.currentBundleObject.isValid() == true)
            {
                int width = COConfigurationManager.getIntParameter("i18nAZ.columnWidth.0", 200);
                TreeTableManager.addColumn(this.getLocalisedMessageText("i18nAZ.Columns.Key"), width);
                for (int i = 0; i < this.localesProperties.size(); i++)
                {
                    try
                    {
                        this.addLocaleColumn(this.localesProperties.get(i).locale, this.currentBundleObject);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (TreeTableManager.getColumnCount() <= 2)
        {
            this.setEmptyFilter(false);
            this.setUnchangedFilter(false);
            this.setExtraFilter(false);
        }

        TreeTableManager.buildItems(this.getPrebuildItems(null, true));

        this.updateInfoText();

        if (TreeTableManager.getColumnCount() > 0)
        {
            this.addLanguageButton.setDisabled(false);
            this.exportLanguageButton.setDisabled(false);
            this.treeModeButton.setDisabled(false);
            this.redirectKeysFilterButton.setDisabled(false);
            this.urlsFilterButton.setDisabled(false);
        }
        if (TreeTableManager.getColumnCount() > 2)
        {
            this.emptyFilterButton.setDisabled(false);
            this.unchangedFilterButton.setDisabled(false);
            this.extraFilterButton.setDisabled(false);
        }
    }

    private void valid(boolean force)
    {
        // show/hide value editor
        if (this.editorStyledText.getVisible() == false)
        {
            return;
        }

        // init
        String errorMessage = null;

        // get selected row
        Item selectedRow = (Item) this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);
        if (selectedRow == null || selectedRow.isDisposed() == true)
        {
            updateStyledTexts();
            return;
        }

        // get selected column
        int selectedColumn = (int) this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

        // get reference (the search process is in for)
        String reference = TreeTableManager.getText(selectedRow, 1);

        // get old value
        String oldValue = TreeTableManager.getText(selectedRow, selectedColumn);

        // get new value
        String newValue = this.editorStyledText.getText();
        if (this.multilineEditor == true)
        {
            newValue = Util.escape(newValue, false);
        }

        // restaure styles for infoStyledText
        this.formatStyledText(this.infoStyledText, this.infoParams, this.infoReferences, this.infoUrls, true);

        // search missing params
        if (errorMessage == null)
        {
            for (int i = 0; i < this.infoParams.size(); i++)
            {
                boolean found = false;
                for (int j = 0; j < this.editorParams.size(); j++)
                {
                    if (this.infoParams.get(i)[2] == this.editorParams.get(j)[2])
                    {
                        found = true;
                        break;
                    }
                }

                if (found == false)
                {
                    // set not found param style
                    StyleRange styleRange = new StyleRange(this.infoParams.get(i)[0], this.infoParams.get(i)[1], new Color(Display.getCurrent(), 163, 21, 21), null);
                    styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
                    styleRange.underline = true;
                    styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                    styleRange.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
                    this.infoStyledText.setStyleRange(styleRange);

                    // show error message box
                    errorMessage = "";
                    if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                    {
                        errorMessage = this.getLocalisedMessageText("i18nAZ.Messages.MissingParam", new String[] { "%" + String.valueOf(this.infoParams.get(i)[2]) });
                    }
                    break;
                }
            }
        }

        // search unknown params
        if (errorMessage == null)
        {
            for (int i = 0; i < this.editorParams.size(); i++)
            {
                boolean found = false;
                for (int j = 0; j < this.infoParams.size(); j++)
                {
                    if (this.editorParams.get(i)[2] == this.infoParams.get(j)[2])
                    {
                        found = true;
                        break;
                    }
                }

                if (found == false)
                {
                    // show error message box
                    errorMessage = "";
                    if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                    {
                        errorMessage = this.getLocalisedMessageText("i18nAZ.Messages.UnknownParam", new String[] { "%" + String.valueOf(this.editorParams.get(i)[2]) });
                    }
                    break;
                }
            }
        }

        // search missing references
        if (errorMessage == null)
        {
            for (int i = 0; i < this.infoReferences.size(); i++)
            {
                boolean found = false;
                for (int j = 0; j < this.editorReferences.size(); j++)
                {
                    if (this.infoReferences.get(i)[2].equals(this.editorReferences.get(j)[2]) == true)
                    {
                        found = true;
                        break;
                    }
                }

                if (found == false)
                {
                    // set not found references style
                    StyleRange styleRange = new StyleRange((int) this.infoReferences.get(i)[0] + 1, (int) this.infoReferences.get(i)[1] - 2, Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
                    styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
                    styleRange.underline = true;
                    styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                    styleRange.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
                    this.infoStyledText.setStyleRange(styleRange);

                    // show error message box
                    errorMessage = "";
                    if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                    {
                        if (Util.isRedirectKeys(oldValue) == true)
                        {
                            if (this.editorStyledText.getText().equals("") == false)
                            {
                                errorMessage = this.getLocalisedMessageText("i18nAZ.Messages.InvalidValue", new String[] { reference });
                            }
                        }
                        else
                        {
                            errorMessage = this.getLocalisedMessageText("i18nAZ.Messages.MissingReference", new String[] { (String) this.infoReferences.get(i)[2] });
                        }
                    }
                    break;
                }
            }
        }

        // Search unknown references
        if (errorMessage == null)
        {
            for (int i = 0; i < this.editorReferences.size(); i++)
            {
                boolean found = false;
                for (int j = 0; j < this.infoReferences.size(); j++)
                {
                    if (this.editorReferences.get(i)[2].equals(this.infoReferences.get(j)[2]) == true)
                    {
                        found = true;
                        break;
                    }
                }

                if (found == false)
                {
                    // show error message box
                    errorMessage = "";
                    if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                    {
                        errorMessage = this.getLocalisedMessageText("i18nAZ.Messages.UnknownReference", new String[] { (String) this.editorReferences.get(i)[2] });
                    }
                    break;
                }
            }
        }

        // search url error
        if (errorMessage == null)
        {
            int referenceState = ((int[]) selectedRow.getData(TreeTableManager.DATAKEY_STATES))[1];
            if ((referenceState & State.URL) != 0)
            {
                try
                {
                    new URL(newValue);
                }
                catch (MalformedURLException e)
                {
                    // show error message box
                    errorMessage = "";
                    if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                    {
                        errorMessage = this.getLocalisedMessageText("i18nAZ.Messages.MalformedURL", new String[] { newValue });
                    }
                }

            }
        }

        if (errorMessage != null)
        {
            TreeTableManager.Cursor.setfocusedRow(selectedRow, selectedColumn);
            if (errorMessage.equals("") == false)
            {
                MessageBox messageBox = new MessageBox((Shell) SWTSkinFactory.getInstance().getShell(), SWT.ICON_ERROR);
                messageBox.setMessage(errorMessage);
                messageBox.open();
            }

            return;
        }
        else
        {
            TreeTableManager.Cursor.cancelfocusedRow();
        }

        // get key
        String currentKey = (String) selectedRow.getData(TreeTableManager.DATAKEY_KEY);

        // get old state
        int oldState = ((int[]) selectedRow.getData(TreeTableManager.DATAKEY_STATES))[selectedColumn];

        // get new state
        int newState = Util.getStateOfValue(reference, newValue);

        // check opportunity
        if ((!(oldValue.equals(newValue) == true && oldState == newState) && (!(oldState == 1 && newState == 2) || force == true)) || (force == true && this.deletableRows.size() > 0))
        {
            // update cell
            TreeTableManager.setText(selectedRow, selectedColumn, newValue);
            TreeTableManager.setRedraw(true);

            // get locale properties for save
            CommentedProperties localeProperties = this.getLocalesProperties((Locale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_LOCALE)).commentedProperties;

            // update resource bundle
            localeProperties.put(currentKey, Util.unescape(newValue));

            // get local file
            File Localfile = (File) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_FILE);

            // add to save objects
            synchronized (this.saveObjects)
            {
                this.saveObjects.add(new SaveObject(localeProperties, Localfile, currentKey));
            }

            // init values for check
            boolean showable = true;
            boolean matchesSearch = this.searchPatterns == null;

            // search in key
            if (matchesSearch == false)
            {
                matchesSearch = this.find(0, currentKey);
            }

            // define show row values
            boolean showRowEmpty = this.emptyFilter;
            boolean showRowUnchanged = this.unchangedFilter;
            boolean showRowExtra = this.extraFilter;

            for (Iterator<String> iterator = this.emptyFilterExcludedKey.iterator(); iterator.hasNext();)
            {
                if (currentKey.startsWith(iterator.next() + "."))
                {
                    showRowEmpty = !showRowEmpty;
                    break;
                }
            }
            for (Iterator<String> iterator = this.unchangedFilterExcludedKey.iterator(); iterator.hasNext();)
            {
                if (currentKey.startsWith(iterator.next() + "."))
                {
                    showRowUnchanged = !showRowUnchanged;
                    break;
                }
            }
            for (Iterator<String> iterator = this.extraFilterExcludedKey.iterator(); iterator.hasNext();)
            {
                if (currentKey.startsWith(iterator.next() + "."))
                {
                    showRowExtra = !showRowExtra;
                    break;
                }
            }

            // reference
            if (matchesSearch == false && reference != null)
            {
                matchesSearch = this.find(1, reference);
            }

            // search in comments
            if (matchesSearch == false)
            {
                String[] CommentsLines = (String[]) selectedRow.getData(TreeTableManager.DATAKEY_COMMENTS);
                if (CommentsLines != null)
                {
                    String comments = "";
                    for (int k = 0; k < CommentsLines.length; k++)
                    {
                        comments += CommentsLines[k].replaceAll("\\n", "\\\\n") + "\n";
                    }
                    matchesSearch = this.find(-1, comments);
                }
            }

            // values
            String rowText = reference;
            boolean rowContainEmpty = false;
            boolean rowContainUnchanged = false;
            boolean rowContainExtra = false;

            for (int j = 2; j < TreeTableManager.getColumnCount(); j++)
            {
                localeProperties = this.getLocalesProperties((Locale) TreeTableManager.getColumn(j).getData(View.DATAKEY_LOCALE)).commentedProperties;
                if (localeProperties == null)
                {
                    continue;
                }
                String value = localeProperties.getProperty(currentKey);
                value = (value == null) ? "" : value;
                if (matchesSearch == false)
                {
                    matchesSearch = this.find(j, value);
                }
                int state = Util.getStateOfValue(reference, value);
                switch (state)
                {
                    case State.EMPTY:
                        rowContainEmpty = true;
                        break;

                    case State.UNCHANGED:
                        rowContainUnchanged = true;
                        break;

                    case State.EXTRA:
                        rowContainExtra = true;
                        break;
                }
                if (j == selectedColumn)
                {
                    TreeTableManager.setState(selectedRow, selectedColumn, state);
                }
                rowText += value;
            }

            if (matchesSearch == false)
            {
                showable = false;
            }

            if (showable == true && (showRowEmpty || showRowUnchanged || showRowExtra))
            {
                showable = (rowContainEmpty && showRowEmpty) || (rowContainUnchanged && showRowUnchanged) || (rowContainExtra && showRowExtra);
            }
            if (rowText.equals("") == true)
            {
                showable = false;
            }

            if (showable == false && TreeTableManager.getChildItemCount(selectedRow) == 0)
            {
                this.deletableRows.put(currentKey, selectedRow);
            }
            else if (this.deletableRows.containsKey(currentKey))
            {
                this.deletableRows.remove(currentKey);
            }
            if (force == true)
            {
                for (Iterator<Entry<String, Item>> iterator = this.deletableRows.entrySet().iterator(); iterator.hasNext();)
                {
                    HashSet<Item> removableItems = new HashSet<Item>();
                    Entry<String, Item> entry = iterator.next();
                    Item Parent = entry.getValue();
                    removableItems.add(Parent);
                    while (true)
                    {
                        Parent = TreeTableManager.getParentItem(Parent);
                        if (Parent == null)
                        {
                            break;
                        }
                        if (TreeTableManager.getChildItemCount(Parent) == 1)
                        {
                            removableItems.add(Parent);
                        }
                        else
                        {
                            break;
                        }
                    }
                    for (Iterator<Item> iterator2 = removableItems.iterator(); iterator2.hasNext();)
                    {
                        Item RemovableItem = iterator2.next();
                        RemovableItem.dispose();
                        RemovableItem = null;
                    }
                }

                this.deletableRows.clear();
            }

            // update info text
            this.updateInfoText();
        }

        // set focus
        if(TreeTableManager.getItemCount() > 0)
        {
            TreeTableManager.Cursor.setFocus();
        }
        else
        {
            updateStyledTexts();
        }
    }
}
