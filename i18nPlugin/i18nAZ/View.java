/*
 * View.java
 *
 * Created on February 24, 2004, 12:00 PM
 */
package i18nAZ;

import i18nAZ.FilterManager.PrebuildItem;
import i18nAZ.FilterManager.State;
import i18nAZ.LocalizablePluginManager.LocalizablePlugin;
import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;
import i18nAZ.SpellChecker.SpellObject;
import i18nAZ.SpellChecker.SpellObjectManager;
import i18nAZ.SpellChecker.Suggestion;
import i18nAZ.TargetLocaleManager.ExternalPath;
import i18nAZ.TargetLocaleManager.ExternalPathCollection;
import i18nAZ.TargetLocaleManager.TargetLocale;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.ToolTip;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.IMenuConstants;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText2;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

import dk.dren.hunspell.Hunspell.Dictionary;

/**
 * View class
 * 
 * @author Repris d'injustice
 */
class View implements UISWTViewCoreEventListener
{
    static final String VIEWID = "i18nAZ";

    // static final String DATAKEY_LOCALE = "locale";
    static final String DATAKEY_TARGET_LOCALE = "targetLocal";
    static final String DATAKEY_LANGUAGE_TAG = "languageTag";
    static final String DATAKEY_SELECTED_ROW = "selectedRow";
    static final String DATAKEY_SELECTED_COLUMN = "selectedColumn";
    static final String DATAKEY_DEFAULT_STYLES = "styles";
    static final String DATAKEY_TEXT_ID = "textId";
    static final String DATAKEY_TOOLTIP = "toolTip";
    static final String DATAKEY_TOOLTIP_HAND = "toolTipHand";

    static final String AZUREUS_LANG_FILE = "org.gudy.azureus2.internat.MessagesBundle";

    static final int SHOW_LOADING = 1;
    static final int SHOW_SPLASH_SCREEN = 2;
    static final int SHOW_TREETABLE = 3;
    static final int SHOW_WIZARD = 4;

    final private AtomicBoolean viewCreated = new AtomicBoolean(false);
    final private AtomicBoolean viewInitialized = new AtomicBoolean(false);
    final private AtomicBoolean initialized = new AtomicBoolean(false);
    private Boolean wizard = false;

    final private AtomicBoolean onValid = new AtomicBoolean(false);

    private ImageLoader imageLoader = SWTSkinFactory.getInstance().getImageLoader(SWTSkinFactory.getInstance().getSkinProperties());

    private Display display = null;

    private SideBarEntrySWT sideBarEntry = null;

    private SWTSkinObjectContainer mainChildContainer = null; // parent: vuze

    private SWTSkinObjectContainer headerContainer = null; // parent: mainChildContainer
    private SWTSkinObjectContainer toolBarContainer = null; // parent: headerContainer
    private SWTSkinButtonUtility addLanguageButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility exportLanguageButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility importLanguageButton = null; // parent: toolBarContainer
    public SWTSkinButtonUtility removeLanguageButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility emptyFilterButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility unchangedFilterButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility extraFilterButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility multilineEditorButton = null; // parent: toolBarContainer
    SWTSkinButtonUtility spellCheckerButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility treeModeButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility urlsFilterButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility redirectKeysFilterButton = null; // parent: toolBarContainer
    private SWTSkinButtonUtility helpButton = null; // parent: toolBarContainer

    private SWTSkinObjectText2 infoText = null; // parent: toolBarContainer
    private SWTSkinObjectTextbox searchTextbox = null; // parent: toolBarContainer

    private SWTSkinObjectContainer areaContainer = null; // parent: mainChildContainer

    private Composite pluginComposite = null; // parent: areaContainer
    private TableCombo pluginsCombo = null; // parent: pluginComposite
    private TableCombo filesCombo = null; // parent: pluginComposite

    private Composite loadingComposite = null; // parent: areaContainer
    private Composite loadingSubComposite = null; // parent: loadingComposite
    private AnimatedCanvas animatedCanvas = null; // parent: loadingSubComposite
    private Label bannerLabel = null; // parent: loadingSubComposite
    private Label loadingMessageLabel = null; // parent: loadingComposite
    private Label progressLabel = null; // parent: loadingComposite

    private Composite wizardComposite = null; // parent: areaContainer
    private Button wizardButton = null; // parent: areaContainer

    private Composite footerComposite = null; // parent: areaContainer

    private ToolBar toolBar = null; // parent: footerComposite

    private ToolItem undoToolItem = null; // parent: toolBar
    private ToolItem redoToolItem = null; // parent: toolBar
    private ToolItem cutToolItem = null; // parent: toolBar
    private ToolItem copyToolItem = null; // parent: toolBar
    private ToolItem pasteToolItem = null; // parent: toolBar
    private ToolItem selectAllToolItem = null; // parent: toolBar
    private ToolItem upperCaseToolItem = null; // parent: toolBar
    private ToolItem lowerCaseToolItem = null; // parent: toolBar
    private ToolItem firstCaseToolItem = null; // parent: toolBar
    private ToolItem trademarkToolItem = null; // parent: toolBar
    private ToolItem translateToolItem = null; // parent: toolBar
    private ToolItem validateToolItem = null; // parent: toolBar
    private ToolItem cancelToolItem = null; // parent: toolBar

    private StyledText infoStyledText = null; // parent: footerComposite
    private StyledText editorStyledText = null; // parent: footerComposite

    private Label statusLabel = null; // parent: areaContainer

    private boolean multilineEditor = false;

    Map<String, Item> deletableRows = new HashMap<String, Item>();

    private Clipboard clipboard = null;

    private SpellObject[] infoSpellObjects = null;
    private SpellObjectManager editorSpellObjectManager = null;

    private MenuItem editMenu = null;
    private Menu dropDownMenu = null;

    UndoRedo undoRedo = null;

    private String loadingMessage = null;
    private float progressPercent = -1;

    private List<SaveObject> saveObjects = new ArrayList<SaveObject>();
    private Task saveTask = new Task("saveTask", 0, new iTask()
    {
        public void setInfo(final String info)
        {
            Utils.execSWTThread(new AERunnable()
            {
                
                public void runSupport()
                {
                    synchronized (View.this.viewCreated)
                    {
                        if (View.this == null || View.this.viewCreated.get() == false)
                        {
                            return;
                        }
                    }
                    if (View.this.statusLabel == null || View.this.statusLabel.isDisposed() == true)
                    {
                        return;
                    }
                    View.this.statusLabel.setText(info);
                    View.this.areaContainer.getComposite().layout();
                }
            });
        }

        
        public void check()
        {
            this.setInfo(i18nAZ.getLocalisedMessageText("i18nAZ.Messages.Status.Ready"));

            Util.sleep(1000);

            SaveObject saveObject = null;
            if (View.this.saveObjects.size() == 0)
            {
                return;
            }
            saveObject = View.this.saveObjects.get(0);
            View.this.saveObjects.remove(0);

            String currentSaveName = Path.getFile(saveObject.getUrl()).getParentFile().getName() + File.separator + Path.getFile(saveObject.getUrl()).getName();
            this.setInfo(i18nAZ.getLocalisedMessageText("i18nAZ.Messages.Status.Saving"));
            final TargetLocale targetLocale = saveObject.targetLocale;
            AEThread2 refreshThread = new AEThread2("i18nAZ.refreshThread")
            {
                
                public void run()
                {
                    LocalizablePluginManager.getCurrentLangFile().refreshCount(targetLocale);
                }
            };
            refreshThread.start();
        
            // clean empty reference
            while (true)
            {
                boolean loop = false;
                for (Enumeration<?> enumeration = saveObject.getLocaleProperties().propertyNames(); enumeration.hasMoreElements();)
                {
                    String key = (String) enumeration.nextElement();
                    if (saveObject.getLocaleProperties().getProperty(key).equals("") == true)
                    {
                        saveObject.getLocaleProperties().remove(key);
                        loop = true;
                        break;
                    }

                }
                if (loop == false)
                {
                    break;
                }
            }

            String result = Util.saveLocaleProperties(saveObject.getLocaleProperties(), saveObject.getUrl());

            // dispose
            saveObject.dispose();
            saveObject = null;

            if (result != null)
            {
                String errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.Status.ErrorSave", new String[] { currentSaveName, result });
                i18nAZ.logError(errorMessage);
                this.setInfo(errorMessage);
                Util.sleep(10000);
                return;
            }

            // merge and store all bundle files for viewing in
            // client
            if (View.this.saveObjects.size() > 0)
            {
                return;
            }

            // merge file
            result = i18nAZ.mergeBundleFile();
            if (result != null)
            {
                String errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.Status.ErrorGlobalSave", new String[] { Path.getFilename(i18nAZ.mergedInternalFile), result });
                i18nAZ.logError(errorMessage);
                this.setInfo(errorMessage);
                Util.sleep(10000);
            }
        }

        
        public void onStart()
        {
        }

        
        public void onStop(StopEvent e)
        {
        }
    });

    class MenuOptions
    {
        static final int NONE = 0;
        static final int COPY_KEY = 1;
        static final int COPY_REFERENCE = 2;
        static final int COPY_VALUE = 4;
        static final int REMOVE_COLUMN = 8;
        static final int EDITOR = 16;
        static final int FILTERS = 32;
        static final int TOPFILTERS = 64;
        static final int SEARCH = 128;
        static final int OPEN_URL = 256;
    }

    class SaveObject
    {
        private LocaleProperties LocaleProperties = null;
        private URL url = null;
        private TargetLocale targetLocale = null;

        public SaveObject(TargetLocale targetLocale)
        {
            this.targetLocale = (TargetLocale) targetLocale.clone();
            this.LocaleProperties = (LocaleProperties) this.targetLocale.getProperties().clone();
            this.url = Path.clone(this.targetLocale.getInternalPath());
        }

        public LocaleProperties getLocaleProperties()
        {
            return this.LocaleProperties;
        }

        public URL getUrl()
        {
            return this.url;
        }

        public void dispose()
        {
            this.LocaleProperties.clear();
            this.LocaleProperties = null;
            this.url = null;
        }
    }

    // constructor
    View()
    {
        i18nAZ.log("");
        i18nAZ.log("View creating...");

        if (COConfigurationManager.hasParameter("i18nAZ.defaultPath", true) == false)
        {
            try
            {
                COConfigurationManager.setParameter("i18nAZ.defaultPath", i18nAZ.vuzeDirectory.getAbsolutePath());
            }
            catch (Exception e)
            {
                COConfigurationManager.setParameter("i18nAZ.defaultPath", "");
            }
        }

        // Load parameters
        FilterManager.init();

        this.multilineEditor = COConfigurationManager.getBooleanParameter("i18nAZ.multilineEditor");

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

        this.imageLoader.addSkinProperties(new SkinPropertiesImpl(i18nAZ.getPluginInterface().getPluginClassLoader(), "images", "images.properties"));

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
            View.checkButton(button, checked == 1);
            button.addSelectionListener(new ButtonListenerAdapter()
            {
                
                public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
                {
                    View.checkButton(button);
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
        if (TreeTableManager.getCurrent() != null)
        {
            TreeTableManager.getCurrent().setFocus();
        }

        AddLanguageDialog addLanguageDialog = new AddLanguageDialog(SWTSkinFactory.getInstance().getShell().getShell());
        if (addLanguageDialog.localesSelected != null)
        {
            Item column = null;
            for (int i = 0; i < addLanguageDialog.localesSelected.length; i++)
            {
                TargetLocale targetLocale = TargetLocaleManager.add(addLanguageDialog.localesSelected[i]);
                try
                {
                    column = this.addLocaleColumn(targetLocale);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            View.saveLocales();
            if (column != null)
            {
                TreeTableManager.Cursor.setColumn(column);
            }
            this.updateTreeTable();

            TreeTableManager.getCurrent().setFocus();
        }
    }

    private Item addLocaleColumn(TargetLocale targetLocale)
    {
        String headerText = "";
        String headerLanguageTag = "";
        int width = COConfigurationManager.getIntParameter("i18nAZ.columnWidth." + TreeTableManager.getColumnCount(), 200);
        if (TreeTableManager.getColumnCount() == 1)
        {
            headerText = i18nAZ.getLocalisedMessageText("i18nAZ.Columns.Reference");
        }
        else
        {
            headerText = Util.getLocaleDisplay(targetLocale.getLocale(), false);
            headerLanguageTag = Util.getLanguageTag(targetLocale.getLocale()).replace("-", "");
        }

        if (targetLocale.isReadOnly() == true)
        {
            if (targetLocale.getProperties().IsLoaded() == false)
            {
                headerText += " [" + i18nAZ.getLocalisedMessageText("i18nAZ.Labels.NotFound") + "]";
            }
            else
            {
                headerText += " [" + i18nAZ.getLocalisedMessageText("i18nAZ.Labels.ReadOnly") + "]";
            }
        }
        Item column = TreeTableManager.addColumn(headerText, width);
        column.setData(View.DATAKEY_TARGET_LOCALE, targetLocale);
        column.setData(View.DATAKEY_LANGUAGE_TAG, headerLanguageTag);

        if (column instanceof TableColumn)
        {
            if (targetLocale.isReference() == true)
            {
                Util.addSortManager((TableColumn) column, Util.STRING_COMPARATOR);
            }
            else
            {
                Util.addSortManager((TableColumn) column, new Util.LocaleComparator(targetLocale.getLocale()));
            }
        }
        if (targetLocale.isReference() == false)
        {
            column.setImage(Util.getLocaleImage(targetLocale.getLocale()));
        }
        this.updateToolTipColumnHeader(column);
        return column;
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

    private static void checkButton(SWTSkinButtonUtility button)
    {
        boolean checked = (Boolean) button.getSkinObject().getData("checked") == false;
        View.checkButton(button, checked);
    }

    private static void checkButton(SWTSkinButtonUtility button, boolean checked)
    {
        String textID = (String) button.getSkinObject().getControl().getData(View.DATAKEY_TEXT_ID);
        button.getSkinObject().switchSuffix(checked ? "-selected" : "", 4, false);
        button.getSkinObject().setData("checked", checked);
        ToolTipText.set(button.getSkinObject().getControl(), textID + ((checked) ? ".Pressed" : ""));
    }

    private static void checkButton(SWTSkinButtonUtility button, int state, String imageId, String tooltipId)
    {
        switch (state)
        {
            case 0:
                button.setImage(imageId);
                View.checkButton(button, false);
                ToolTipText.set(button.getSkinObject().getControl(), tooltipId + "State1");
                break;
            case 1:
                button.setImage(imageId + "Off");
                View.checkButton(button, true);
                ToolTipText.set(button.getSkinObject().getControl(), tooltipId + "State2");
                break;
            case 2:
                button.setImage(imageId + "On");
                View.checkButton(button, true);
                ToolTipText.set(button.getSkinObject().getControl(), tooltipId + "State3");
                break;
        }
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

                
                public void menuHidden(MenuEvent e)
                {
                }

                
                public void menuShown(MenuEvent e)
                {
                    if (e.widget != null && e.widget.equals(View.this.dropDownMenu) == true)
                    {

                        int visible = MenuOptions.SEARCH | MenuOptions.TOPFILTERS | MenuOptions.EDITOR | MenuOptions.COPY_KEY | MenuOptions.COPY_REFERENCE | MenuOptions.OPEN_URL;
                        int enabled = MenuOptions.SEARCH | MenuOptions.TOPFILTERS;
                        PrebuildItem prebuildItem = null;
                        if (TreeTableManager.Cursor.getRow() != null)
                        {
                            prebuildItem = (PrebuildItem) TreeTableManager.Cursor.getRow().getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                        }
                        if (TreeTableManager.Cursor.isFocusControl() == true && prebuildItem != null && prebuildItem.isExist() == true && TreeTableManager.getColumnCount() > 1)
                        {
                            View.this.dropDownMenu.setData(TreeTableManager.DATAKEY_ITEM, TreeTableManager.Cursor.getRow());
                            View.this.dropDownMenu.setData(TreeTableManager.DATAKEY_COLUMN_INDEX, TreeTableManager.Cursor.getColumn());

                            visible |= MenuOptions.COPY_VALUE;
                            String textData = TreeTableManager.getText(TreeTableManager.Cursor.getRow(), 0);
                            if (textData.equals("") == false)
                            {
                                enabled |= MenuOptions.COPY_KEY;
                            }
                            textData = TreeTableManager.getText(TreeTableManager.Cursor.getRow(), 1);
                            if (textData.equals("") == false)
                            {
                                enabled |= MenuOptions.COPY_REFERENCE;
                            }
                            if (TreeTableManager.Cursor.getColumn() >= 2)
                            {
                                enabled |= MenuOptions.COPY_VALUE;
                            }
                        }

                        if (View.this.editorStyledText.isFocusControl() == true && TreeTableManager.getColumnCount() > 0)
                        {
                            enabled |= MenuOptions.EDITOR;
                            if (prebuildItem != null && (prebuildItem.getStates()[1] & State.URL) != 0)
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

                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[0].setSelection((Boolean) View.this.emptyFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[1].setSelection((Boolean) View.this.unchangedFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[2].setSelection((Boolean) View.this.extraFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[4].setSelection((Boolean) View.this.redirectKeysFilterButton.getSkinObject().getData("checked"));
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[6].setSelection(FilterManager.getCurrentFilter().urls == 1);
                        View.this.dropDownMenu.getItems()[18].getMenu().getItems()[7].setSelection(FilterManager.getCurrentFilter().urls == 2);
                    }
                }
            });

        }
    }

    
    public boolean eventOccurred(UISWTViewEvent e)
    {
        switch (e.getType())
        {
            case UISWTViewEvent.TYPE_CREATE:
                synchronized (View.this.viewCreated)
                {
                    if (this.viewCreated.get() == true)
                    {
                        return false;
                    }
                    this.viewCreated.set(true);
                }
                i18nAZ.log("View created !");

                // set sidebar entry
                this.setSideBar((UISWTViewImpl) e.getView());

                synchronized (this.wizard)
                {
                    if (this.wizard == false)
                    {
                        this.startInitialization();
                    }
                }

                break;

            case UISWTViewEvent.TYPE_INITIALIZE:
                i18nAZ.log("View initializing...");
                this.saveObjects.clear();
                this.initialize((Composite) e.getData(), (SWTSkinObjectContainer) ((UISWTViewImpl) e.getView()).getSkinObject());
                i18nAZ.log("View initialized !");
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
                synchronized (View.this.viewCreated)
                {
                    if (this.viewCreated.get() == false)
                    {
                        return false;
                    }
                    this.viewCreated.set(false);
                }
                i18nAZ.log("");
                i18nAZ.log("View destroying...");
                synchronized (this.viewInitialized)
                {
                    this.viewInitialized.set(false);
                }

                this.display = null;

                if (this.infoStyledText != null && this.infoStyledText.getData(View.DATAKEY_TOOLTIP) != null)
                {
                    ((ToolTip) this.infoStyledText.getData(View.DATAKEY_TOOLTIP)).dispose();
                    this.infoStyledText.setData(View.DATAKEY_TOOLTIP, null);
                }
                if (this.infoStyledText != null && this.editorStyledText.getData(View.DATAKEY_TOOLTIP) != null)
                {
                    ((ToolTip) this.editorStyledText.getData(View.DATAKEY_TOOLTIP)).dispose();
                    this.editorStyledText.setData(View.DATAKEY_TOOLTIP, null);
                }

                this.mainChildContainer = null;

                this.headerContainer = null;
                this.toolBarContainer = null;
                this.areaContainer = null;
                this.addLanguageButton = null;
                this.exportLanguageButton = null;
                this.importLanguageButton = null;
                this.removeLanguageButton = null;
                this.emptyFilterButton = null;
                this.unchangedFilterButton = null;
                this.extraFilterButton = null;
                this.multilineEditorButton = null;
                this.spellCheckerButton = null;
                this.treeModeButton = null;
                this.urlsFilterButton = null;
                this.redirectKeysFilterButton = null;
                this.helpButton = null;
                this.infoText = null;
                this.searchTextbox = null;

                this.pluginComposite = null;
                this.pluginsCombo = null;
                this.filesCombo = null;

                this.loadingComposite = null;
                this.loadingSubComposite = null;
                this.animatedCanvas = null;
                this.bannerLabel = null;
                this.loadingMessageLabel = null;
                this.progressLabel = null;

                this.footerComposite = null;

                this.toolBar = null;
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
                this.translateToolItem = null;
                this.validateToolItem = null;
                this.cancelToolItem = null;
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
                TargetLocaleManager.deleteCountListeners();
                TreeTableManager.dispose();

                if (this.saveTask != null)
                {
                    this.saveTask.stop();
                }

                LocalizablePluginManager.deleteListeners();
                LocalizablePluginManager.stop();

                LocalePropertiesLoader.stop();

                i18nAZ.log("View destroyed !");
                break;
        }
        return true;
    }

    private void setSideBar(UISWTViewImpl uiSWTViewImpl)
    {
        // init count Object
        final CountObject countObject = new CountObject();
        uiSWTViewImpl.setTitle(i18nAZ.getLocalisedMessageText("i18nAZ.SideBar.Title"));
        if (SideBar.instance != null)
        {
            this.sideBarEntry = (SideBarEntrySWT) SideBar.instance.getEntry(VIEWID);
            this.sideBarEntry.setImageLeftID("i18nAZ.image.sidebar");
            // this.sideBarEntry.setCloseable(false);
            ViewTitleInfo viewTitleInfo = new ViewTitleInfo()
            {
                
                public Object getTitleInfoProperty(int pid)
                {
                    if (pid == TITLE_INDICATOR_TEXT)
                    {
                        synchronized (countObject)
                        {
                            if (countObject.emptyCount > 0)
                            {
                                return String.valueOf(countObject.emptyCount);
                            }
                        }
                    }
                    else if (pid == TITLE_INDICATOR_TEXT_TOOLTIP)
                    {
                        return i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Sidebar", new String[] { String.valueOf(countObject.entryCount), String.valueOf(countObject.emptyCount), String.valueOf(countObject.unchangedCount), String.valueOf(countObject.extraCount) });
                    }

                    return null;
                }
            };
            this.sideBarEntry.setViewTitleInfo(viewTitleInfo);
        }
        TargetLocaleManager.addCountListener(new CountListener()
        {
            
            public void countChanged(CountEvent e)
            {
                LocalizablePlugin[] localizablePlugins = LocalizablePluginManager.toArray();
                synchronized (countObject)
                {
                    countObject.clear();
                    for (int i = 0; i < localizablePlugins.length; i++)
                    {
                        CountObject childCountObject = localizablePlugins[i].getCounts();
                        countObject.add(childCountObject);
                    }
                }
                if (View.this.sideBarEntry != null)
                {
                    ViewTitleInfoManager.refreshTitleInfo(View.this.sideBarEntry.getViewTitleInfo());
                }
                if (e != null && e.langFileObject.equals(LocalizablePluginManager.getCurrentLangFile()))
                {
                    Utils.execSWTThread(new AERunnable()
                    {
                        
                        public void runSupport()
                        {
                            View.this.updateInfoText();
                        }
                    });
                }
            }
        });
    }

    private static void exportLanguage()
    {
        TreeTableManager.getCurrent().setFocus();

        String destFilename = LocalizablePluginManager.getCurrentLangFile().getParent().getName() + ".zip";

        FileDialog fileDialog = new FileDialog(SWTSkinFactory.getInstance().getShell().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.SAVE);
        fileDialog.setFilterPath(COConfigurationManager.getStringParameter("i18nAZ.defaultPath"));
        fileDialog.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.Export"));
        fileDialog.setFileName(destFilename);
        fileDialog.setFilterExtensions(new String[] { "*.zip" });
        fileDialog.setFilterNames(new String[] { i18nAZ.getLocalisedMessageText("i18nAZ.Labels.FilterExtensions.Zip") });
        fileDialog.setOverwrite(true);

        String path = fileDialog.open();
        if (path != null)
        {
            COConfigurationManager.setParameter("i18nAZ.defaultPath", new File(path).getParent());
            String errorMessage = null;
            File pathFile = new File(path);

            ZipOutputStream zipOutputStream = null;
            FileOutputStream fileOutputStream = null;
            try
            {
                fileOutputStream = new FileOutputStream(pathFile);
            }
            catch (FileNotFoundException e)
            {
                errorMessage = "Error Zip Output Stream #1:" + e.getLocalizedMessage();
            }
            if (errorMessage == null)
            {
                zipOutputStream = new ZipOutputStream(fileOutputStream);
                TargetLocale[] targetLocales = TargetLocaleManager.toArray();
                LangFileObject[] langFileObjects = LocalizablePluginManager.getCurrentLangFile().getParent().toArray();
                for (int i = 0; i < langFileObjects.length; i++)
                {
                    for (int j = 1; j < targetLocales.length; j++)
                    {
                        if (targetLocales[j].isReadOnly() == true)
                        {
                            continue;
                        }
                        LocaleProperties properties = langFileObjects[i].getProperties(targetLocales[j]);
                        String fileName = langFileObjects[i].getFileName(targetLocales[j]);
                        String folder = langFileObjects[i].getJarFolder().replace('/', '.') + "/";
                        errorMessage = Util.addLocalePropertiesToZipOutputStream(zipOutputStream, properties, folder + fileName);
                        if (errorMessage != null)
                        {
                            break;
                        }
                    }
                }
                try
                {
                    zipOutputStream.close();
                }
                catch (IOException e)
                {
                }
            }
            if (errorMessage != null)
            {
                errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.ExportFailed", errorMessage);
                i18nAZ.logError(errorMessage);
                pathFile.delete();
                MessageBox messageBox = new MessageBox((Shell) SWTSkinFactory.getInstance().getShell(), SWT.ICON_ERROR);
                messageBox.setMessage(errorMessage);
                messageBox.open();
            }
        }
    }

    private static void formatStyledText(StyledText styledText, SpellObject[] spellObjects, boolean hand)
    {
        // found default styles
        if (styledText.getData(View.DATAKEY_DEFAULT_STYLES) == null)
        {
            // set default style
            StyleRange styleRange = new StyleRange(0, styledText.getText().length(), styledText.getForeground(), styledText.getBackground());
            styleRange.font = styledText.getFont();
            styledText.setStyleRange(styleRange);

            String languageTag = (String) TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()).getData(View.DATAKEY_LANGUAGE_TAG);

            if (FilterManager.getCurrentFilter().isTextEnabled(languageTag) == true)
            {
                for (Iterator<Entry<Pattern, Object>> iterator = FilterManager.getCurrentFilter().getPatterns(); iterator.hasNext();)
                {
                    Entry<Pattern, Object> entry = iterator.next();
                    Pattern searchPattern = entry.getKey();
                    boolean searchResult = (Boolean) entry.getValue();
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
        for (int i = 0; i < spellObjects.length; i++)
        {
            if (spellObjects[i].getType() != SpellChecker.TYPE_PARAM)
            {
                continue;
            }
            StyleRange styleRange = new StyleRange(spellObjects[i].getOffset(), spellObjects[i].getLength(), new Color(Display.getCurrent(), 163, 21, 21), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
            styledText.setStyleRange(styleRange);
        }

        // set styles references
        for (int i = 0; i < spellObjects.length; i++)
        {
            if (spellObjects[i].getType() != SpellChecker.TYPE_REFERENCE)
            {
                continue;
            }

            StyleRange styleRange = new StyleRange(spellObjects[i].getOffset(), spellObjects[i].getLength(), Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styleRange.underline = true;
            styledText.setStyleRange(styleRange);

            styleRange = new StyleRange(spellObjects[i].getOffset(), 1, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styledText.setStyleRange(styleRange);

            styleRange = new StyleRange(spellObjects[i].getOffset() + spellObjects[i].getLength() - 1, 1, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styledText.setStyleRange(styleRange);
        }

        // set styles urls
        for (int i = 0; i < spellObjects.length; i++)
        {
            if (spellObjects[i].getType() != SpellChecker.TYPE_URL)
            {
                continue;
            }
            StyleRange styleRange = new StyleRange(spellObjects[i].getOffset(), spellObjects[i].getLength(), new Color(Display.getCurrent(), 0, 0, 0), null);
            styleRange.font = new Font(null, styledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
            styleRange.underline = true;
            if (hand == true)
            {
                styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            }
            styleRange.data = spellObjects[i];
            styledText.setStyleRange(styleRange);
        }

        // set styles misspelling
        for (int i = 0; i < spellObjects.length; i++)
        {
            if ((spellObjects[i].getType() & SpellChecker.TYPE_MISSPELLING) == 0)
            {
                continue;
            }
            StyleRange[] styleRanges = styledText.getStyleRanges(spellObjects[i].getOffset(), spellObjects[i].getLength());
            if (styleRanges == null || styleRanges.length == 0)
            {
                styleRanges = new StyleRange[1];
                styleRanges[0] = new StyleRange(spellObjects[i].getOffset(), spellObjects[i].getLength(), new Color(Display.getCurrent(), 0, 0, 0), null);
            }
            for (int j = 0; j < styleRanges.length; j++)
            {
                styleRanges[j].underline = true;
                styleRanges[j].underlineStyle = SWT.UNDERLINE_SQUIGGLE;

                if (spellObjects[i].getType() == SpellChecker.TYPE_MISSPELLING_ERROR)
                {
                    styleRanges[j].underlineColor = new Color(Display.getCurrent(), 255, 0, 0);
                }
                else
                {
                    styleRanges[j].underlineColor = new Color(Display.getCurrent(), 0, 255, 0);
                }
                styledText.setStyleRange(styleRanges[j]);
            }
        }

        // set styles translated words
        for (int i = 0; i < spellObjects.length; i++)
        {
            if (spellObjects[i].getType() != SpellChecker.TYPE_TRANSLATED_WORDS)
            {
                continue;
            }
            if (spellObjects[i].getOffset() + spellObjects[i].getLength() > styledText.getText().length())
            {
                continue;
            }
            if (styledText.getText().substring(spellObjects[i].getOffset(), spellObjects[i].getOffset() + spellObjects[i].getLength()).equalsIgnoreCase(spellObjects[i].getValue()) == false)
            {
                continue;
            }
            StyleRange[] styleRanges = styledText.getStyleRanges(spellObjects[i].getOffset(), spellObjects[i].getLength());
            if (styleRanges == null || styleRanges.length == 0)
            {
                styleRanges = new StyleRange[1];
                styleRanges[0] = new StyleRange(spellObjects[i].getOffset(), spellObjects[i].getLength(), new Color(Display.getCurrent(), 0, 0, 0), null);
            }
            for (int j = 0; j < styleRanges.length; j++)
            {
                if (styleRanges[j].background != null && styleRanges[j].background.getRGB().equals(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW).getRGB()))
                {
                    continue;
                }
                styleRanges[j].background = new Color(Display.getCurrent(), 178, 214, 253);
                styledText.setStyleRange(styleRanges[j]);
            }
        }
    }

    LangFileObject fillFilesCombo(LocalizablePlugin localizablePlugin)
    {
        LangFileObject langFileObject = null;
        int selectedIndex = 0;
        this.filesCombo.getTable().removeAll();

        LangFileObject[] langFileObjects = localizablePlugin.toArray();
        for (int i = 0; i < langFileObjects.length; i++)
        {
            TableItem tableItem = new TableItem(this.filesCombo.getTable(), SWT.NULL);
            tableItem.setData(langFileObjects[i]);
            tableItem.setText(langFileObjects[i].getLangFile());
            tableItem.setImage(this.imageLoader.getImage("i18nAZ.image.files"));

            if (LocalizablePluginManager.fileSelectedId != null == true)
            {
                if (langFileObjects[i].getId().equals(LocalizablePluginManager.fileSelectedId) == true)
                {
                    selectedIndex = i;
                    LocalizablePluginManager.fileSelectedId = null;
                }
            }
            else
            {
                if ((LocalizablePluginManager.getCurrentLangFile() == null && i == 0) || (LocalizablePluginManager.getCurrentLangFile() != null && langFileObjects[i].getId().equals(LocalizablePluginManager.getCurrentLangFile().getId()) == true))
                {
                    selectedIndex = i;
                }
            }
        }
        if (selectedIndex < this.filesCombo.getItemCount() && this.filesCombo.getSelectionIndex() != selectedIndex)
        {
            this.filesCombo.select(selectedIndex);
            langFileObject = langFileObjects[selectedIndex];
        }
        this.filesCombo.setVisibleItemCount((this.filesCombo.getItemCount() == 0) ? 1 : 20);
        this.updateFilesCombo();
        return langFileObject;
    }

    String translate(String value, Locale locale)
    {
        if (View.this.multilineEditor == false)
        {
            value = Util.unescape(value);
        }

        String[] results = new String[1];
        SpellObject[] translatedObjects = TranslateManager.translate(value, locale, results);
        if (translatedObjects != null)
        {
            if (View.this.multilineEditor == false)
            {
                results[0] = Util.escape(results[0], false);
            }
            View.this.editorSpellObjectManager.setTranslatedObjects(translatedObjects);
            return results[0];
        }
        return null;
    }
    void updateEditor()
    {       
        // get selected row
        Item selectedRow = (Item) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);

        // get selected column
        int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

        // get locale properties for save
        final TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

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
        
        final PrebuildItem prebuildItem = (PrebuildItem) selectedRow.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
        final LangFileObject langFileObject = LocalizablePluginManager.getCurrentLangFile();
        
        //enable/disable toolitems
        View.this.selectAllToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0);
        View.this.upperCaseToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0);
        View.this.lowerCaseToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0);
        View.this.firstCaseToolItem.setEnabled(View.this.editorStyledText.getText().length() > 1);
        View.this.validateToolItem.setEnabled(View.this.deletableRows.size() > 0 || oldValue.equals(View.this.editorStyledText.getText()) == false);
        View.this.cancelToolItem.setEnabled(View.this.editorStyledText.getText().equals("") == true || (oldValue.equals(View.this.editorStyledText.getText()) == false && !(oldValue.equals("") == true && View.this.editorStyledText.getText().equals(reference) == true)));

        // found params & references for editor
        if (selectedTargetLocale != null)
        {
            final String value = View.this.editorStyledText.getText();
            AEThread2 spellCheckThread = new AEThread2("i18nAZ.spellCheck")
            {
                
                public void run()
                {
                    final SpellObject[] editorSpellObjects = View.this.editorSpellObjectManager.getSpellObjects(langFileObject, prebuildItem.getKey(),selectedTargetLocale.getLocale(), value, View.this.multilineEditor);
                    Utils.execSWTThread(new AERunnable()
                    {
                        
                        public void runSupport()
                        {
                            if (value.equals(View.this.editorStyledText.getText()) == false)
                            {
                                return;
                            }

                            // apply styles for editorStyledText
                            View.formatStyledText(View.this.editorStyledText, editorSpellObjects, false);

                            // search unknown params
                            SpellObject[] unknownParams = SpellChecker.foundMissings(editorSpellObjects, View.this.infoSpellObjects, SpellChecker.TYPE_PARAM);
                            for (int i = 0; i < unknownParams.length; i++)
                            {
                                // set not found param style
                                StyleRange styleRange = new StyleRange(unknownParams[i].getOffset(), unknownParams[i].getLength(), new Color(Display.getCurrent(), 163, 21, 21), null);
                                styleRange.font = new Font(null, View.this.editorStyledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
                                styleRange.underline = true;
                                styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                                styleRange.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
                                View.this.editorStyledText.setStyleRange(styleRange);
                            }

                            // Search unknown references
                            SpellObject[] unknownReferences = SpellChecker.foundMissings(editorSpellObjects, View.this.infoSpellObjects, SpellChecker.TYPE_REFERENCE);
                            for (int i = 0; i < unknownReferences.length; i++)
                            {
                                // set not found references style
                                StyleRange styleRange = new StyleRange(unknownReferences[i].getOffset() + 1, unknownReferences[i].getLength() - 2, Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
                                styleRange.font = new Font(null, View.this.editorStyledText.getFont().getFontData()[0].getName(), 9, SWT.NORMAL);
                                styleRange.underline = true;
                                styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                                styleRange.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
                                View.this.editorStyledText.setStyleRange(styleRange);
                            }
                        }
                    });
                }
            };
            spellCheckThread.start();
        }
    }
    void updateFilesCombo()
    {
        for (int i = 0; i < this.filesCombo.getTable().getItemCount(); i++)
        {
            TableItem tableItem = this.filesCombo.getTable().getItem(i);
            LangFileObject langFileObject = (LangFileObject) tableItem.getData();

            CountObject counts = langFileObject.getCounts();

            String text1 = "";
            Color color1 = tableItem.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

            if (counts != null && counts.entryCount > 0)
            {
                double percent = (1 - ((double) counts.emptyCount) / ((double) counts.entryCount));
                if (percent > 1)
                {
                    percent = 1;
                }
                DecimalFormat decimalFormat = new DecimalFormat("0.00 %");
                //decimalFormat.setRoundingMode(RoundingMode.DOWN);
                decimalFormat.setMaximumFractionDigits(2);
                decimalFormat.setMinimumFractionDigits(2);

                text1 = decimalFormat.format(percent);

                if (percent == 0)
                {
                    color1 = tableItem.getDisplay().getSystemColor(SWT.COLOR_RED);
                }
                else if (percent == 1)
                {
                    color1 = new Color(tableItem.getDisplay(), 0, 128, 0);
                }
            }
            tableItem.setText(1, text1);

            tableItem.setForeground(1, color1);
        }
    }

    LangFileObject fillPluginsCombo()
    {
        synchronized (this.viewInitialized)
        {
            if (this.viewInitialized.get() == false)
            {
                return null;
            }
        }
        LangFileObject langFileObject = null;
        int selectedIndex = 0;
        this.pluginsCombo.getTable().removeAll();
        LocalizablePlugin[] localizablePlugins = LocalizablePluginManager.toArray();
        for (int i = 0; i < localizablePlugins.length; i++)
        {
            TableItem tableItem = new TableItem(View.this.pluginsCombo.getTable(), SWT.NULL);
            tableItem.setData(localizablePlugins[i]);
            tableItem.setText(localizablePlugins[i].getDisplayName());
            Image[] sidebarImages = new Image[0];
            if (localizablePlugins[i].getId().equals(LocalizablePluginManager.PLUGIN_CORE_ID) == false)
            {
                sidebarImages = this.imageLoader.getImages("image.sidebar." + localizablePlugins[i].getId());
                if (sidebarImages.length == 0)
                {
                    sidebarImages = this.imageLoader.getImages(localizablePlugins[i].getId() + ".image.sidebar");
                }
                if (sidebarImages.length == 0)
                {
                    sidebarImages = this.imageLoader.getImages("image.sidebar.plugin");
                }
            }
            else
            {
                sidebarImages = this.imageLoader.getImages("i18nAZ.image.vuze");
            }
            if (sidebarImages.length > 0)
            {
                tableItem.setImage(sidebarImages[0]);
            }

            if (LocalizablePluginManager.fileSelectedId != null == true)
            {
                if (localizablePlugins[i].getId().equals(LocalizablePluginManager.fileSelectedId.split("!")[0]) == true)
                {

                    selectedIndex = i;
                }
            }
            else
            {
                if ((LocalizablePluginManager.getCurrentLangFile() == null && i == 0) || (LocalizablePluginManager.getCurrentLangFile() != null && localizablePlugins[i].getId().equals(LocalizablePluginManager.getCurrentLangFile().getParent().getId()) == true))
                {
                    selectedIndex = i;
                }
            }
        }
        if (this.pluginsCombo.isDropped() == true)
        {
            langFileObject = LocalizablePluginManager.getCurrentLangFile();
        }
        else if (selectedIndex < this.pluginsCombo.getItemCount() - 1 && this.pluginsCombo.getSelectionIndex() != selectedIndex)
        {
            this.pluginsCombo.clearSelection();
            this.pluginsCombo.select(selectedIndex);
            langFileObject = this.fillFilesCombo(localizablePlugins[selectedIndex]);
        }

        View.this.pluginsCombo.setVisibleItemCount((View.this.pluginsCombo.getItemCount() == 0) ? 1 : 20);

        if (View.this.loadingComposite.getVisible() == false)
        {
            View.this.pluginComposite.setVisible(View.this.pluginsCombo.getItemCount() > 0);
            Util.setHeightHint(View.this.pluginComposite, (View.this.pluginsCombo.getItemCount() > 0) ? -1 : 0);
        }

        this.updatePluginsCombo();
        return langFileObject;
    }

    void updatePluginsCombo()
    {
        for (int i = 0; i < this.pluginsCombo.getTable().getItemCount(); i++)
        {
            TableItem tableItem = this.pluginsCombo.getTable().getItem(i);
            LocalizablePlugin localizablePlugin = (LocalizablePlugin) tableItem.getData();

            CountObject counts = localizablePlugin.getCounts();
            String text1 = "";
            String text2 = "";
            Color color1 = tableItem.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
            Color color2 = tableItem.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
            LangFileObject[] langFileObjects = localizablePlugin.toArray();
            if (counts.entryCount > 0)
            {
                if (langFileObjects.length > 1)
                {
                    text1 = "(" + String.valueOf(langFileObjects.length) + ")";
                    tableItem.setForeground(1, tableItem.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
                }
                double percent = (1 - ((double) counts.emptyCount) / ((double) counts.entryCount));
                if (percent > 1)
                {
                    percent = 1;
                }
                DecimalFormat decimalFormat = new DecimalFormat("0.00 %");
                //decimalFormat.setRoundingMode(RoundingMode.DOWN);
                decimalFormat.setMaximumFractionDigits(2);
                decimalFormat.setMinimumFractionDigits(2);
                text2 = decimalFormat.format(percent);
                if (percent == 0)
                {
                    color2 = tableItem.getDisplay().getSystemColor(SWT.COLOR_RED);
                }
                else if (percent == 1)
                {
                    color2 = new Color(tableItem.getDisplay(), 0, 128, 0);
                }
            }
            else
            {
                if (langFileObjects.length > 1)
                {
                    text2 = "(" + String.valueOf(langFileObjects.length) + ")";
                }
            }

            tableItem.setText(1, text1);
            tableItem.setText(2, text2);

            tableItem.setForeground(1, color1);
            tableItem.setForeground(2, color2);
        }
    }

    void updateProgressBar()
    {
        this.updateProgressBar(this.progressPercent);
    }

    void updateProgressBar(final float percent)
    {
        this.progressPercent = percent;
        if (this.display == null)
        {
            return;
        }
        if (this.display.getThread().equals(Thread.currentThread()) == false)
        {
            Utils.execSWTThread(new AERunnable()
            {
                
                public void runSupport()
                {
                    View.this.updateProgressBar(View.this.progressPercent);
                }
            });
            return;
        }
        if (this.progressLabel == null || this.progressLabel.isDisposed() == true)
        {
            return;
        }
        if (this.progressLabel.getListeners(SWT.Paint).length == 0)
        {
            this.progressLabel.addPaintListener(new PaintListener()
            {
                
                public void paintControl(PaintEvent e)
                {
                    Control c = (Control) e.widget;

                    Point size = c.getSize();
                    e.gc.setBackground(ColorCache.getColor(e.display, "#23a7df"));
                    float breakX = size.x * View.this.progressPercent / 100;
                    e.gc.fillRectangle(0, (size.y - 2) / 2, (int) breakX, 2);
                    e.gc.setBackground(ColorCache.getColor(e.display, "#cccccc"));
                    e.gc.fillRectangle((int) breakX, (size.y - 2) / 2, size.x - (int) breakX, 2);
                }
            });
        }
        this.progressLabel.setVisible(this.progressPercent > -1);
        this.progressLabel.redraw();
    }

    void updateLoadingMessage()
    {
        this.updateLoadingMessage(this.loadingMessage);
    }

    void updateLoadingMessage(final String message)
    {
        this.loadingMessage = message;
        if (this.display == null)
        {
            return;
        }
        if (this.display.getThread().equals(Thread.currentThread()) == false)
        {
            Utils.execSWTThread(new AERunnable()
            {
                
                public void runSupport()
                {
                    View.this.updateLoadingMessage(message);
                }
            });
            return;
        }
        if (this.loadingMessageLabel == null || this.loadingMessageLabel.isDisposed() == true)
        {
            return;
        }
        this.loadingMessageLabel.setVisible(this.loadingMessage != null);
        this.loadingMessageLabel.setText(this.loadingMessage != null ? this.loadingMessage : "");
        this.loadingMessageLabel.getParent().layout();
        this.loadingMessageLabel.update();
    }

    Display getDisplay()
    {
        return this.display;
    }

    ImageLoader getImageLoader()
    {
        return this.imageLoader;
    }

    private void importLanguage()
    {
        TreeTableManager.getCurrent().setFocus();

        if (LocalizablePluginManager.getCurrentLangFile() == null)
        {
            return;
        }

        FileDialog fileDialog = new FileDialog(SWTSkinFactory.getInstance().getShell().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.OPEN | SWT.MULTI);
        fileDialog.setFilterPath(COConfigurationManager.getStringParameter("i18nAZ.defaultPath"));
        fileDialog.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.Import"));
        fileDialog.setFilterExtensions(new String[] { "*.properties", "*.zip" });
        fileDialog.setFilterNames(new String[] { i18nAZ.getLocalisedMessageText("i18nAZ.Labels.FilterExtensions.Properties"), i18nAZ.getLocalisedMessageText("i18nAZ.Labels.FilterExtensions.Zip") });
        fileDialog.setFilterIndex(COConfigurationManager.getIntParameter("i18nAZ.filterIndex", 0));
        fileDialog.setOverwrite(false);

        String result = fileDialog.open();
        if (result != null)
        {
            // get folder path
            String folderPath = new File(result).getParent();

            // set default path parameter
            COConfigurationManager.setParameter("i18nAZ.defaultPath", folderPath);
            COConfigurationManager.setParameter("i18nAZ.filterIndex", fileDialog.getFilterIndex());

            // get file name
            List<ImportObject> importObjects = new ArrayList<ImportObject>();

            for (int i = 0; i < fileDialog.getFileNames().length; i++)
            {
                // insert folder Path
                importObjects.add(new ImportObject(Path.getUrl(folderPath + File.separator + fileDialog.getFileNames()[i])));
            }

            // found zip path
            for (int i = importObjects.size() - 1; i >= 0; i--)
            {
                // check if zip file
                ZipFile zipFile = null;
                try
                {
                    zipFile = new ZipFile(Path.getFile(importObjects.get(i).url));
                }
                catch (IOException e)
                {
                    continue;
                }
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                ArrayList<URL> zipPaths = new ArrayList<URL>();
                while (entries.hasMoreElements())
                {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.isDirectory() == false)
                    {
                        zipPaths.add(Path.getUrl(importObjects.get(i).url.toString() + "!/" + zipEntry.getName()));
                    }
                }
                if (zipPaths.size() > 0)
                {
                    for (int j = 0; j < zipPaths.size(); j++)
                    {
                        importObjects.add(new ImportObject(zipPaths.get(j)));
                    }
                    zipPaths.clear();
                    importObjects.remove(i);
                }
                try
                {
                    zipFile.close();
                }
                catch (IOException e)
                {
                }
            }
            for (int i = 0; i < importObjects.size(); i++)
            {
                // set vars
                String errorMessage = null;
                String fileName = Path.getFilenameWithoutExtension(importObjects.get(i).url);
                String extension = Path.getExtension(importObjects.get(i).url);

                error: while (true)
                {
                    // show error message
                    if (errorMessage != null)
                    {
                        i18nAZ.logWarning(errorMessage);
                        MessageBox messageBox = new MessageBox((Shell) SWTSkinFactory.getInstance().getShell(), SWT.ICON_ERROR);
                        messageBox.setMessage(errorMessage);
                        messageBox.open();
                        return;
                    }

                    // detect languageTag
                    Locale locale = Util.getLocaleFromFilename(fileName);
                    if (locale != null)
                    {
                        importObjects.get(i).locale = locale;
                    }

                    // check File is properties
                    importObjects.get(i).properties = Util.getLocaleProperties(importObjects.get(i).locale, importObjects.get(i).url);
                    if (importObjects.get(i).properties == null)
                    {
                        errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.InvalidMessageBundleFile");
                        continue error;
                    }

                    if (importObjects.get(i).locale == null || extension.equalsIgnoreCase(".properties") == false)
                    {
                        errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.NotFoundLanguage", new String[] { fileName + extension });
                        continue error;
                    }
                    break;
                }
            }
            TargetLocale[] targetLocales = TargetLocaleManager.toArray();
            for (int i = 0; i < importObjects.size(); i++)
            {
                boolean found = false;
                for (int j = 0; j < targetLocales.length; j++)
                {
                    if (targetLocales[j].isReadOnly() == false && importObjects.get(i).locale.equals(targetLocales[j].getLocale()))
                    {
                        found = true;
                        break;
                    }
                }
                if (found == true)
                {
                    importObjects.get(i).allowedActions |= ImportLanguageDialog.Actions.REPLACE;
                    importObjects.get(i).allowedActions |= ImportLanguageDialog.Actions.OPEN_READ_ONLY;
                }
                else
                {
                    importObjects.get(i).allowedActions |= ImportLanguageDialog.Actions.ADD;
                }
            }

            ImportLanguageDialog importLanguageDialog = new ImportLanguageDialog(SWTSkinFactory.getInstance().getShell().getShell(), importObjects);
            importObjects = importLanguageDialog.importObjects;
            boolean refreshColumns = false;
            for (int i = 0; i < importObjects.size(); i++)
            {
                int originIndex = -1;
                for (int j = 0; j < targetLocales.length; j++)
                {
                    if (importObjects.get(i).locale.equals(targetLocales[j].getLocale()) && (targetLocales[j].isReadOnly() == false))
                    {
                        originIndex = j;
                    }
                }
                TargetLocale targetLocale = null;
                switch (importObjects.get(i).selectedAction)
                {
                    case ImportLanguageDialog.Actions.OPEN_READ_ONLY:

                        int lastIndex = -1;
                        for (int j = originIndex; originIndex != -1 && j < targetLocales.length; j++)
                        {
                            if (importObjects.get(i).locale.equals(targetLocales[j].getLocale()))
                            {
                                if (targetLocales[j].isReadOnly() == true)
                                {
                                    if (targetLocales[j].isVisible() == false)
                                    {
                                        targetLocales[j].getExternalPaths().put(LocalizablePluginManager.getCurrentLangFile().getId(), new ExternalPath(LocalizablePluginManager.getCurrentLangFile().getId(), importObjects.get(i).url));
                                        refreshColumns = true;
                                        lastIndex = -1;
                                        break;
                                    }
                                }
                                lastIndex = j;
                            }
                        }
                        if (lastIndex != -1)
                        {
                            ExternalPathCollection externalPaths = new ExternalPathCollection();
                            externalPaths.put(LocalizablePluginManager.getCurrentLangFile().getId(), new ExternalPath(LocalizablePluginManager.getCurrentLangFile().getId(), importObjects.get(i).url));
                            TargetLocaleManager.add(lastIndex + 1, importObjects.get(i).locale, externalPaths, importObjects.get(i).properties);
                            refreshColumns = true;
                        }
                        break;
                    case ImportLanguageDialog.Actions.REPLACE:
                        if (originIndex != -1)
                        {
                            targetLocales[originIndex].setDefaultProperties(importObjects.get(i).properties);
                            this.saveObjects.add(new SaveObject(targetLocales[originIndex]));
                        }
                        break;
                    case ImportLanguageDialog.Actions.ADD:
                        targetLocale = TargetLocaleManager.add(importObjects.get(i).locale, importObjects.get(i).properties);
                        this.saveObjects.add(new SaveObject(targetLocale));
                        refreshColumns = true;
                        break;
                    default:
                        break;
                }
                importObjects.get(i).dispose();
                importObjects.set(i, null);
            }
            importObjects.clear();
            if (refreshColumns == true)
            {
                View.saveLocales();
                this.updateTreeTable(refreshColumns, true);
            }
        }
    }

    private void enable()
    {
        synchronized (this.viewInitialized)
        {
            if (this.viewInitialized.get() == false)
            {
                return;
            }
        }
        synchronized (this.initialized)
        {
            if (this.initialized.get() == false)
            {
                return;
            }
        }
        synchronized (this.wizard)
        {
            if (this.wizard == true)
            {
                return;
            }
        }
        i18nAZ.log("enabling...");
        AEThread2 enableThread = new AEThread2("i18nAZ.enable")
        {
            
            public void run()
            {
                Utils.execSWTThread(new AERunnable()
                {
                    
                    public void runSupport()
                    {
                        synchronized (View.this.viewCreated)
                        {
                            if (View.this.viewCreated.get() == false)
                            {
                                return;
                            }
                        }
                        View.this.createTopLevelMenuitem();
                        View.checkButton(View.this.treeModeButton, TreeTableManager.isTreeMode());
                        View.checkButton(View.this.redirectKeysFilterButton, FilterManager.getCurrentFilter().redirectKeys);
                        View.checkButton(View.this.urlsFilterButton, FilterManager.getCurrentFilter().urls, "i18nAZ.image.toolbar.urlsFilter", "i18nAZ.ToolTips.UrlsFilter");
                        View.checkButton(View.this.emptyFilterButton, FilterManager.getCurrentFilter().empty);
                        View.checkButton(View.this.unchangedFilterButton, FilterManager.getCurrentFilter().unchanged);
                        View.checkButton(View.this.extraFilterButton, FilterManager.getCurrentFilter().extra);
                        View.checkButton(View.this.multilineEditorButton, View.this.multilineEditor);

                        View.this.show(View.SHOW_SPLASH_SCREEN);
                    }
                });
                Utils.execSWTThread(new AERunnable()
                {
                    
                    public void runSupport()
                    {
                        synchronized (View.this.viewCreated)
                        {
                            if (View.this.viewCreated.get() == false)
                            {
                                return;
                            }
                        }
                        LangFileObject langFileObject = View.this.fillPluginsCombo();
                        LocalizablePluginManager.setCurrentLangFile(langFileObject);

                        View.this.mainChildContainer.getComposite().setRedraw(false);

                        // enable button & combos
                        View.this.multilineEditorButton.setDisabled(false);
                        View.this.helpButton.setDisabled(false);
                        View.this.pluginsCombo.setEnabled(true);
                        View.this.filesCombo.setEnabled(true);

                        // fill treetable
                        i18nAZ.log("fill treetable...");
                        View.this.updateTreeTable(true, true);
                        i18nAZ.log("treetable filled !");
                    }
                });
                Util.sleep(500);
                Utils.execSWTThread(new AERunnable()
                {
                    
                    public void runSupport()
                    {
                        synchronized (View.this.viewCreated)
                        {
                            if (View.this.viewCreated.get() == false)
                            {
                                return;
                            }
                        }
                        // show status label

                        Util.setGridData(View.this.statusLabel, SWT.FILL, SWT.NONE, true, false);
                        View.this.statusLabel.setVisible(true);

                        // show treetable
                        View.this.show(View.SHOW_TREETABLE);

                        // start save task
                        View.this.saveTask.start();
                    }
                });
            }
        };
        enableThread.start();
    }

    private void initializeMainToolBar()
    {
        this.headerContainer = new SWTSkinObjectContainer(SWTSkinFactory.getInstance(), SWTSkinFactory.getInstance().getSkinProperties(), "i18nAZ.main.header", "i18nAZ.main.header", this.mainChildContainer);
        this.headerContainer.getComposite().setVisible(false);
        Util.setGridData(this.headerContainer.getComposite(), SWT.FILL, SWT.TOP, true, false, SWT.DEFAULT, 0);

        this.toolBarContainer = (SWTSkinObjectContainer) SWTSkinFactory.getInstance().createSkinObject("mdientry.toolbar.full", "mdientry.toolbar.full", this.headerContainer);

        Control lastControl = null;

        // ADD LANGUAGE BUTTON
        this.addLanguageButton = this.addButton(this.toolBarContainer, "addLanguage", "left", "i18nAZ.image.toolbar.add", "i18nAZ.ToolTips.AddLanguage", 10, lastControl);
        this.addLanguageButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.addLanguage();
            }
        });
        this.addLanguageButton.setDisabled(true);
        lastControl = this.addLanguageButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // IMPORT BUTTON
        this.importLanguageButton = this.addButton(this.toolBarContainer, "importLanguage", "", "i18nAZ.image.toolbar.import", "i18nAZ.ToolTips.ImportLanguage", 0, lastControl);
        this.importLanguageButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.importLanguage();
            }
        });
        this.importLanguageButton.setDisabled(true);
        lastControl = this.importLanguageButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // EXPORT BUTTON
        this.exportLanguageButton = this.addButton(this.toolBarContainer, "exportLanguage", "", "i18nAZ.image.toolbar.export", "i18nAZ.ToolTips.ExportLanguage", 0, lastControl);
        this.exportLanguageButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.exportLanguage();
            }
        });
        this.exportLanguageButton.setDisabled(true);
        lastControl = this.exportLanguageButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // REMOVE BUTTON
        this.removeLanguageButton = this.addButton(this.toolBarContainer, "removeLanguage", "right", "image.toolbar.remove", "i18nAZ.ToolTips.RemoveLanguage", 0, lastControl);
        this.removeLanguageButton.setDisabled(true);
        this.removeLanguageButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.removeLanguage();
            }
        });
        lastControl = this.removeLanguageButton.getSkinObject().getControl();

        // TREE MODE BUTTON
        TreeTableManager.setMode(COConfigurationManager.getBooleanParameter("i18nAZ.treeMode"));
        this.treeModeButton = this.addButton(this.toolBarContainer, "treeMode", false, "left", "i18nAZ.image.toolbar.treeMode", "i18nAZ.ToolTips.TreeMode", 10, lastControl);
        this.treeModeButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();

                TreeTableManager.setMode((Boolean) buttonUtility.getSkinObject().getData("checked"));

                View.this.updateTreeTable(false);
                COConfigurationManager.setParameter("i18nAZ.treeMode", TreeTableManager.isTreeMode());
                COConfigurationManager.save();
            }
        });
        this.treeModeButton.setDisabled(true);
        lastControl = this.treeModeButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // SHOW REF BUTTON
        this.redirectKeysFilterButton = this.addButton(this.toolBarContainer, "redirectKeysFilter", false, "", "i18nAZ.image.toolbar.redirectKeysFilter", "i18nAZ.ToolTips.RedirectKeysFilter", 0, lastControl);
        this.redirectKeysFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();
                View.this.setRedirectKeysFilter((Boolean) buttonUtility.getSkinObject().getData("checked"));
                COConfigurationManager.setParameter("i18nAZ.redirectKeysFilter", FilterManager.getCurrentFilter().redirectKeys);
                COConfigurationManager.save();
                View.this.updateTreeTable();
            }
        });
        this.redirectKeysFilterButton.setDisabled(true);
        lastControl = this.redirectKeysFilterButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // SHOW URL BUTTON
        this.urlsFilterButton = this.addButton(this.toolBarContainer, "urlsFilter", false, "right", "i18nAZ.image.toolbar.urlsFilter" + (FilterManager.getCurrentFilter().urls == 2 ? "On" : (FilterManager.getCurrentFilter().urls == 1 ? "Off" : "")), "i18nAZ.ToolTips.UrlsFilter", 0, lastControl);
        this.urlsFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();
                switch (FilterManager.getCurrentFilter().urls)
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
                COConfigurationManager.setParameter("i18nAZ.urlsFilter", FilterManager.getCurrentFilter().urls);
                COConfigurationManager.save();
                View.this.updateTreeTable();
            }
        });
        this.urlsFilterButton.setDisabled(true);
        lastControl = this.urlsFilterButton.getSkinObject().getControl();

        // EMPTY FILTER BUTTTON
        this.emptyFilterButton = this.addButton(this.toolBarContainer, "emptyFilter", false, "left", "i18nAZ.image.toolbar.emptyFilter", "i18nAZ.ToolTips.EmptyFilter", 10, lastControl);
        this.emptyFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();
                View.this.setEmptyFilter((Boolean) buttonUtility.getSkinObject().getData("checked"));
                COConfigurationManager.setParameter("i18nAZ.emptyFilter", FilterManager.getCurrentFilter().empty);
                COConfigurationManager.save();
                View.this.updateTreeTable();
            }
        });
        this.emptyFilterButton.setDisabled(true);
        lastControl = this.emptyFilterButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // UNCHANGED FILTER BUTTTON
        this.unchangedFilterButton = this.addButton(this.toolBarContainer, "unchangedFilter", false, "", "i18nAZ.image.toolbar.unchangedFilter", "i18nAZ.ToolTips.UnchangedFilter", 0, lastControl);
        this.unchangedFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();
                View.this.setUnchangedFilter((Boolean) buttonUtility.getSkinObject().getData("checked"));
                COConfigurationManager.setParameter("i18nAZ.unchangedFilter", FilterManager.getCurrentFilter().unchanged);
                COConfigurationManager.save();
                View.this.updateTreeTable();
            }
        });
        this.unchangedFilterButton.setDisabled(true);
        lastControl = this.unchangedFilterButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // EXTRA FILTER BUTTTON
        this.extraFilterButton = this.addButton(this.toolBarContainer, "extraFilter", false, "right", "i18nAZ.image.toolbar.extraFilter", "i18nAZ.ToolTips.ExtraFilter", 0, lastControl);
        this.extraFilterButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                TreeTableManager.getCurrent().setFocus();

                View.this.setExtraFilter((Boolean) buttonUtility.getSkinObject().getData("checked"));
                COConfigurationManager.setParameter("i18nAZ.extraFilter", FilterManager.getCurrentFilter().extra);
                COConfigurationManager.save();
                View.this.updateTreeTable();
            }
        });
        this.extraFilterButton.setDisabled(true);
        lastControl = this.extraFilterButton.getSkinObject().getControl();

        // MULTILINE EDITOR BUTTON
        this.multilineEditorButton = this.addButton(this.toolBarContainer, "multilineEditor", false, "left", "i18nAZ.image.toolbar.multilineEditor", "i18nAZ.ToolTips.MultilineEditor", 10, lastControl);
        this.multilineEditorButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                String oldValue = null;
                Point selection = View.this.editorStyledText.getSelection();

                if (View.this.editorStyledText.getVisible() == true)
                {
                    oldValue = View.this.editorStyledText.getText();
                    if (View.this.multilineEditor == true)
                    {
                        oldValue = Util.escape(oldValue, false);
                    }
                    else
                    {
                        int offset = 0;
                        String[] result = new String[2];
                        while ((offset = Util.findEscape(oldValue, offset, result)) != -1)
                        {
                            if (selection.x >= offset)
                            {
                                selection.x -= result[1].length() - 1;
                            }
                            if (selection.y >= offset)
                            {
                                selection.y -= result[1].length() - 1;
                            }
                            offset += result[0].length();
                        }
                    }
                }
                View.this.multilineEditor = (Boolean) buttonUtility.getSkinObject().getData("checked");
                COConfigurationManager.setParameter("i18nAZ.multilineEditor", View.this.multilineEditor);
                COConfigurationManager.save();

                ToolTipText.set(View.this.validateToolItem, "i18nAZ.ToolTips.Validate" + ((View.this.multilineEditor == true) ? "" : ".Shortcut"));

                View.this.updateStyledTexts();
                if (oldValue != null)
                {
                    if (View.this.multilineEditor == true)
                    {
                        oldValue = Util.unescape(oldValue);
                    }
                    else
                    {
                        int offset = 0;
                        String[] result = new String[2];
                        while ((offset = Util.findEscape(oldValue, offset, result)) != -1)
                        {
                            if (selection.x >= offset)
                            {
                                selection.x += result[1].length() - 1;
                            }
                            if (selection.y >= offset)
                            {
                                selection.y += result[1].length() - 1;
                            }
                            offset += result[0].length();
                        }
                    }
                    View.this.editorStyledText.setText(oldValue);
                }
                View.this.editorStyledText.setSelection(selection);

            }
        });
        this.multilineEditorButton.setDisabled(true);
        lastControl = this.multilineEditorButton.getSkinObject().getControl();
        lastControl = this.addSeparator(this.toolBarContainer, lastControl).getControl();

        // SPELLCHECKER BUTTON
        this.spellCheckerButton = this.addButton(this.toolBarContainer, "spellChecker", "right", "i18nAZ.image.toolbar.spellChecker", "i18nAZ.ToolTips.SpellChecker", 0, lastControl);
        this.spellCheckerButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                View.this.valid(false, false);

                View.this.spellCheck();                
            }
        });
        this.spellCheckerButton.setDisabled(true);
        lastControl = this.spellCheckerButton.getSkinObject().getControl();

        // HELP BUTTON
        this.helpButton = this.addButton(this.toolBarContainer, "help", "all", "i18nAZ.image.toolbar.help", "i18nAZ.ToolTips.Help", 10, lastControl);
        this.helpButton.addSelectionListener(new ButtonListenerAdapter()
        {
            
            public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject, int stateMask)
            {
                HelpDialog.show();
            }
        });
        this.helpButton.setDisabled(true);
        lastControl = this.helpButton.getSkinObject().getControl();

        // SEARCH TEXTBOX
        this.searchTextbox = (SWTSkinObjectTextbox) SWTSkinFactory.getInstance().createSkinObject("i18nAZ.main.header.search", "i18nAZ.main.header.search", this.toolBarContainer);
        ToolTipText.set(this.searchTextbox.getTextControl(), "i18nAZ.ToolTips.Search");
        this.searchTextbox.getTextControl().setBackground(Display.getCurrent().getSystemColor(FilterManager.getCurrentFilter().isRegexEnabled() == true ? SWT.COLOR_INFO_BACKGROUND : SWT.COLOR_WIDGET_BACKGROUND));
        this.searchTextbox.getTextControl().addModifyListener(new ModifyListener()
        {
            
            public void modifyText(ModifyEvent e)
            {
                boolean result = false;
                if (View.this.searchTextbox.getText().equals("") == true)
                {
                    result = FilterManager.getCurrentFilter().clearText();
                }
                if (result == true)
                {
                    View.this.updateTreeTable();
                }
            }
        });
        this.searchTextbox.getTextControl().addKeyListener(new KeyAdapter()
        {
            
            public void keyPressed(KeyEvent e)
            {
                if (e.stateMask == SWT.CTRL && e.keyCode == 'x')
                {
                    FilterManager.getCurrentFilter().setRegexEnabled(FilterManager.getCurrentFilter().isRegexEnabled() == false);
                    COConfigurationManager.setParameter("i18nAZ.RegexSearch", FilterManager.getCurrentFilter().isRegexEnabled());
                    COConfigurationManager.save();
                    View.this.searchTextbox.getTextControl().setBackground(Display.getCurrent().getSystemColor(FilterManager.getCurrentFilter().isRegexEnabled() == true ? SWT.COLOR_INFO_BACKGROUND : SWT.COLOR_WHITE));
                }
            }
        });
        this.searchTextbox.getTextControl().addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetDefaultSelected(SelectionEvent e)
            {
                boolean result = false;
                if (View.this.searchTextbox.getText().equals("") == true || (e.detail & SWT.CANCEL) != 0)
                {
                    result = FilterManager.getCurrentFilter().clearText();
                }
                else
                {
                    result = FilterManager.getCurrentFilter().setText(View.this.searchTextbox.getText());
                }
                if (result == true)
                {
                    View.this.updateTreeTable();
                }
            }
        });
        this.searchTextbox.getTextControl().addFocusListener(new FocusAdapter()
        {
            
            public void focusLost(FocusEvent e)
            {
                boolean result = false;
                if (View.this.searchTextbox.getText().equals("") == true)
                {
                    result = FilterManager.getCurrentFilter().clearText();
                }
                else
                {
                    result = FilterManager.getCurrentFilter().setText(View.this.searchTextbox.getText());
                }
                if (result == true)
                {
                    View.this.updateTreeTable();
                }
            }
        });
        this.searchTextbox.getControl().setEnabled(false);

        // INFO TEXT
        this.infoText = (SWTSkinObjectText2) SWTSkinFactory.getInstance().createSkinObject("i18nAZ.main.header.info", "i18nAZ.main.header.info", this.toolBarContainer);
        FormData formData = (FormData) this.infoText.getControl().getLayoutData();
        formData.left = new FormAttachment(lastControl, 10);
    }

    private void initializeTableCombos()
    {
        this.pluginComposite = Util.getNewComposite(this.areaContainer.getComposite(), SWT.NULL, 2, SWT.FILL, SWT.NULL, true, false, 0);
        this.pluginComposite.setVisible(false);

        this.pluginsCombo = new TableCombo(this.pluginComposite, SWT.BORDER | SWT.READ_ONLY);
        Util.setGridData(this.pluginsCombo, SWT.FILL, SWT.NONE, false, false, 300, SWT.DEFAULT);
        Util.setCustomHotColor(this.pluginsCombo.getTable());
        this.pluginsCombo.setCursor(new org.eclipse.swt.graphics.Cursor(Display.getCurrent(), SWT.CURSOR_HAND));
        this.pluginsCombo.defineColumns(new int[] { SWT.DEFAULT, 30, 60 }, new int[] { SWT.NULL, SWT.RIGHT, SWT.RIGHT });
        this.pluginsCombo.addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {
                LocalizablePlugin localizablePlugin = (LocalizablePlugin) View.this.pluginsCombo.getTable().getItem(View.this.pluginsCombo.getSelectionIndex()).getData();
                String[] block = COConfigurationManager.getStringParameter("i18nAZ.FileSelectedId").split("!", 2);
                String pluginSelectedId = block[0];
                if (localizablePlugin.getId().equals(pluginSelectedId) == false)
                {
                    TreeTableManager.savePosition();
                    LangFileObject langFileObject = View.this.fillFilesCombo(localizablePlugin);
                    if (LocalizablePluginManager.setCurrentLangFile(langFileObject) == true)
                    {
                        View.this.updateTreeTable(false, false);
                    }
                }
            }
        });
        this.pluginsCombo.setEnabled(false);

        this.filesCombo = new TableCombo(this.pluginComposite, SWT.BORDER | SWT.READ_ONLY);
        Util.setGridData(this.filesCombo, SWT.FILL, SWT.NONE, true, false);
        Util.setCustomHotColor(this.filesCombo.getTable());
        this.filesCombo.setCursor(new org.eclipse.swt.graphics.Cursor(Display.getCurrent(), SWT.CURSOR_HAND));
        this.filesCombo.defineColumns(new int[] { SWT.DEFAULT, 60 }, new int[] { SWT.NULL, SWT.RIGHT });
        this.filesCombo.addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {
                LangFileObject langFileObject = LocalizablePluginManager.getCurrentLangFile().getParent().toArray()[View.this.filesCombo.getSelectionIndex()];
                if (langFileObject.getId().equals(COConfigurationManager.getStringParameter("i18nAZ.FileSelectedId")) == false)
                {
                    COConfigurationManager.setParameter("i18nAZ.FileSelectedId", langFileObject.getId());
                    COConfigurationManager.save();
                    TreeTableManager.savePosition();
                    LocalizablePluginManager.setCurrentLangFile(langFileObject);
                    View.this.updateTreeTable(false, false);
                }
            }
        });
        this.filesCombo.setEnabled(false);

    }

    private void initializeLoadingComposite()
    {
        this.loadingComposite = Util.getNewComposite(this.areaContainer.getComposite(), SWT.NONE, 1, SWT.CENTER, SWT.CENTER, false, false, 0);
        this.loadingComposite.setVisible(false);

        // Create sub composite
        this.loadingSubComposite = Util.getNewComposite(this.loadingComposite, SWT.NONE, 2, SWT.CENTER, SWT.BOTTOM, true, true);

        // Create banner label
        this.bannerLabel = new Label(this.loadingSubComposite, SWT.NONE);
        this.bannerLabel.setImage(i18nAZ.viewInstance.getImageLoader().getImage("i18nAZ.image.banner"));
        Util.setGridData(this.bannerLabel, SWT.RIGHT, SWT.BOTTOM, true, true, 0);

        // Create animated loading canvas
        this.animatedCanvas = new AnimatedCanvas(this.loadingSubComposite, SWT.NONE);
        this.animatedCanvas.setImageID("i18nAZ.image.loading");
        Util.setGridData(this.animatedCanvas, SWT.LEFT, SWT.BOTTOM, true, true, 0, 0);

        // Create progress bar
        this.progressLabel = new Label(this.loadingComposite, SWT.NONE);
        this.updateProgressBar();
        Util.setGridData(this.progressLabel, SWT.FILL, SWT.TOP, true, true, SWT.DEFAULT, 0, 30, 0, 2, 1);

        // Create message label
        this.loadingMessageLabel = new Label(this.loadingComposite, SWT.NONE);
        this.updateLoadingMessage();
        this.loadingMessageLabel.setForeground(this.loadingMessageLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        this.loadingMessageLabel.setFont(new Font(this.loadingMessageLabel.getDisplay(), this.loadingMessageLabel.getFont().getFontData()[0].getName(), 8, SWT.NORMAL));

        Util.setGridData(this.loadingMessageLabel, SWT.LEFT, SWT.TOP, 2, 1);
    }

    private void initializeWizardComposite()
    {
        // create composite
        this.wizardComposite = new Composite(this.areaContainer.getComposite(), SWT.NONE);
        Util.setGridData(this.wizardComposite, SWT.FILL, SWT.FILL, true, false);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 0;
        this.wizardComposite.setLayout(gridLayout);
        this.wizardComposite.setVisible(false);

        Util.setGridData(this.wizardComposite, SWT.FILL, SWT.FILL, true, false, 0);

        // create label
        Label label = new Label(this.wizardComposite, SWT.NULL);
        label.setImage(i18nAZ.viewInstance.getImageLoader().getImage("i18nAZ.image.logo_48"));
        Util.setGridData(label, SWT.FILL, SWT.CENTER, false, false, 60, 60);

        // create label
        label = new Label(this.wizardComposite, SWT.NULL);
        label.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.Wizard"));
        label.setFont(new Font(null, "Segoe UI Light", 20, SWT.NORMAL));
        label.setForeground(this.display.getSystemColor(SWT.COLOR_DARK_GRAY));
        Util.setGridData(label, SWT.LEFT, SWT.CENTER, true, false);

        // create separator
        label = new Label(this.wizardComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        Util.setGridData(label, SWT.FILL, SWT.FILL, true, false, SWT.DEFAULT, 0, SWT.DEFAULT, 0, 2, 1);

        // create composite
        Composite composite = new Composite(this.wizardComposite, SWT.NONE);
        composite.setBackground(this.display.getSystemColor(SWT.COLOR_WHITE));
        composite.setLayout(new GridLayout(1, false));
        Util.setGridData(composite, SWT.FILL, SWT.FILL, true, true, SWT.DEFAULT, 0, SWT.DEFAULT, 0, 2, 1);

        // create label
        label = new Label(composite, SWT.NULL);
        label.setFont(new Font(null, "Segoe UI Light", 11, SWT.NORMAL));
        label.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Messages.Wizard"));
        Util.setGridData(label, SWT.CENTER, SWT.CENTER, true, true);

        // create separator
        label = new Label(this.wizardComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        Util.setGridData(label, SWT.FILL, SWT.FILL, true, false, SWT.DEFAULT, 0, SWT.DEFAULT, 0, 2, 1);

        // create composite
        composite = new Composite(this.wizardComposite, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        Util.setGridData(composite, SWT.FILL, SWT.FILL, true, false, SWT.DEFAULT, 0, 70, 0, 2, 1);

        // Create button label
        this.wizardButton = new Button(composite, SWT.NULL);
        this.wizardButton.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Buttons.Next"));
        this.wizardButton.addSelectionListener(new SelectionAdapter()
        {

            
            public void widgetSelected(SelectionEvent e)
            {
                synchronized (View.this.wizard)
                {
                    /*
                     * View.this.wizard = false; View.this.wizardComposite.setVisible(true); Util.setGridData(View.this.wizardComposite, SWT.CENTER, SWT.CENTER, true, false, 0);
                     * 
                     * View.this.loadingComposite.setVisible(true); Util.setGridData(View.this.loadingComposite, SWT.CENTER, SWT.CENTER, true, true);
                     * 
                     * Util.redraw(View.this.loadingComposite); View.this.startInitialization();
                     */
                }
            }
        });
        Util.setGridData(this.wizardButton, SWT.RIGHT, SWT.CENTER, true, true, 100, SWT.DEFAULT);

        // create cancel button
        Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText(i18nAZ.getLocalisedMessageText("Button.cancel"));
        Util.setGridData(cancelButton, SWT.RIGHT, SWT.CENTER, false, true, 100, SWT.DEFAULT);
        cancelButton.addListener(SWT.Selection, new Listener()
        {
            
            public void handleEvent(Event event)
            {
            }
        });

    }

    private void initializeInfoLabel()
    {
        this.infoStyledText = new StyledText(this.footerComposite, SWT.BORDER | SWT.WRAP | SWT.MULTI);
        this.infoStyledText.setMargins(5, 5, 5, 5);
        this.infoStyledText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        this.infoStyledText.setFont(new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 8, SWT.NORMAL));
        this.infoStyledText.setEditable(false);
        Util.setGridData(this.infoStyledText, SWT.FILL, SWT.FILL, true, true, 0);
        Util.addLinkManager(this.infoStyledText, true, false);

    }

    private void initializeEditorToolBar()
    {
        this.toolBar = new ToolBar(this.footerComposite, SWT.FLAT);
        Util.setGridData(this.toolBar, SWT.FILL, SWT.FILL, true, false, 0);
        this.toolBar.setFont(new Font(null, "Times New Roman", 11, SWT.NORMAL));
        ToolTipText.config(this.toolBar);

        this.undoToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.undoToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.undo"));
        ToolTipText.set(this.undoToolItem, "i18nAZ.ToolTips.Undo");
        this.undoToolItem.addSelectionListener(new SelectionAdapter()
        {
            
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
            
            public void widgetSelected(SelectionEvent e)
            {
                // get selected column
                int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get selected locale
                TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

                Point selection = View.this.editorStyledText.getSelection();
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    View.this.editorStyledText.setText(View.this.editorStyledText.getText().toUpperCase(selectedTargetLocale.getLocale()));
                }
                else
                {
                    View.this.editorStyledText.insert(View.this.editorStyledText.getSelectionText().toUpperCase(selectedTargetLocale.getLocale()));
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
            
            public void widgetSelected(SelectionEvent e)
            {
                // get selected column
                int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get selected locale
                TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

                Point selection = View.this.editorStyledText.getSelection();
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    View.this.editorStyledText.setText(View.this.editorStyledText.getText().toLowerCase(selectedTargetLocale.getLocale()));
                }
                else
                {
                    View.this.editorStyledText.insert(View.this.editorStyledText.getSelectionText().toLowerCase(selectedTargetLocale.getLocale()));
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
            
            public void widgetSelected(SelectionEvent e)
            {
                // get selected column
                int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get selected locale
                TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

                Point selection = View.this.editorStyledText.getSelection();

                String text = "";
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {

                    text = View.this.editorStyledText.getText();
                }
                else
                {
                    text = View.this.editorStyledText.getSelectionText();
                }
                int index = 0;
                while (index < text.length() && (text.charAt(index) == ' ' || text.charAt(index) == '\t' || text.charAt(index) == '\r' || text.charAt(index) == '\n' || text.charAt(index) == '\\' || text.charAt(index) == '{' || text.charAt(index) == '[' || text.charAt(index) == '(' || text.charAt(index) == '}' || text.charAt(index) == ']' || text.charAt(index) == ')' || text.charAt(index) == '.' || text.charAt(index) == ',' || text.charAt(index) == ';' || text.charAt(index) == ':' || text.charAt(index) == '?' || text.charAt(index) == '!' || text.charAt(index) == '%' || text.charAt(index) == '/' || text.charAt(index) == '*' || text.charAt(index) == '-' || text.charAt(index) == '+' || text.charAt(index) == '_' || text.charAt(index) == '\'' || text.charAt(index) == '"' || text.charAt(index) == '#' || text.charAt(index) == '~' || text.charAt(index) == '&' || text.charAt(index) == '|'))
                {
                    index++;
                }
                if (index < text.length())
                {
                    text = text.substring(0, index) + Character.toTitleCase(text.charAt(index)) + text.substring(index + 1).toLowerCase(selectedTargetLocale.getLocale());
                }

                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    View.this.editorStyledText.setText(text);
                }
                else
                {
                    View.this.editorStyledText.insert(text);
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
        this.translateToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.translateToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.translate"));
        ToolTipText.set(this.translateToolItem, "i18nAZ.ToolTips.Translate");
        this.translateToolItem.addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {
                // get selected column
                int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get selected locale
                TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);
                
                // get selected row
                Item selectedRow = (Item) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);
                if (selectedRow == null || selectedRow.isDisposed() == true)
                {
                    return;
                }
                // get reference
                String reference = TreeTableManager.getText(selectedRow, 1);
                if (View.this.multilineEditor == true)
                {
                    reference = Util.escape(reference, false);
                }
                
                //Translate
                String result = View.this.translate(reference, selectedTargetLocale.getLocale());
                if (result != null)
                {
                    View.this.editorStyledText.setText(result);
                }
            }
        });
        new ToolItem(this.toolBar, SWT.SEPARATOR);
        this.validateToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.validateToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.validate"));
        ToolTipText.set(this.validateToolItem, "i18nAZ.ToolTips.Validate" + ((this.multilineEditor == true) ? "" : ".Shortcut"));
        this.validateToolItem.addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {
                View.this.valid(true, true);
            }
        });

        this.cancelToolItem = new ToolItem(this.toolBar, SWT.PUSH);
        this.cancelToolItem.setImage(this.imageLoader.getImage("i18nAZ.image.toolbar.cancel"));
        ToolTipText.set(this.cancelToolItem, "i18nAZ.ToolTips.Cancel");
        this.cancelToolItem.addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {
                View.this.cancel();
            }
        });
    }

    private void initializeEditor()
    {
        this.editorStyledText = new StyledText(this.footerComposite, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        this.editorStyledText.setMargins(5, 5, 5, 5);
        this.editorStyledText.setKeyBinding(SWT.MOD1 | 'X', ST.VerifyKey);
        this.editorStyledText.setKeyBinding(SWT.MOD1 | 'C', ST.VerifyKey);
        this.editorStyledText.setKeyBinding(SWT.MOD1 | 'V', ST.VerifyKey);
        Util.addLinkManager(this.editorStyledText, false, false);

        this.undoRedo = new UndoRedo(this.editorStyledText);
        this.undoRedo.addListener(SWT.CHANGED, new Listener()
        {
            
            public void handleEvent(Event e)
            {
                View.this.undoToolItem.setEnabled(View.this.undoRedo.canUndo());
                View.this.redoToolItem.setEnabled(View.this.undoRedo.canRedo());
            }
        });
        this.editorStyledText.setVisible(false);
        Util.setGridData(this.editorStyledText, SWT.FILL, SWT.FILL, true, true, 0);
        this.editorStyledText.addTraverseListener(new TraverseListener()
        {
            
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
                {
                    e.doit = false;
                }
            }
        });
        this.editorStyledText.addSelectionListener(new SelectionAdapter()
        {
            
            public void widgetSelected(SelectionEvent e)
            {

                // get selected row
                Item selectedRow = (Item) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);
                if (selectedRow == null || selectedRow.isDisposed() == true)
                {
                    View.this.updateStyledTexts();
                    return;
                }

                // get selected column
                int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get locale properties for save
                TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

                if (selectedTargetLocale.isReadOnly() == true)
                {
                    return;
                }

                if (View.this.editorStyledText.isFocusControl() == false)
                {
                    View.this.editorStyledText.setFocus();
                }
                View.this.cutToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0 && e.y - e.x > 0);
                View.this.copyToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0 && e.y - e.x > 0);
                View.this.selectAllToolItem.setEnabled(View.this.editorStyledText.getText().length() > 0 && e.y - e.x != View.this.editorStyledText.getText().length());
            }
        });

        final Menu menu = new Menu(this.editorStyledText);
        this.editorStyledText.setMenu(menu);
        this.editorStyledText.addMenuDetectListener(new MenuDetectListener()
        {
            
            public void menuDetected(MenuDetectEvent e)
            {
                while (menu.getItemCount() > 0)
                {
                    menu.getItem(0).dispose();
                }

                int offset = -1;
                if (View.this.editorStyledText.getSelectionCount() == 0)
                {
                    Point point = View.this.editorStyledText.toControl(e.x, e.y);
                    point = View.this.editorStyledText.toControl(e.x, e.y);
                    try
                    {
                        offset = View.this.editorStyledText.getOffsetAtLocation(point);
                        View.this.editorStyledText.setSelection(offset, offset);
                    }
                    catch (IllegalArgumentException ie)
                    {
                    }
                }
                else
                {
                    offset = View.this.editorStyledText.getSelection().x;
                }
                // get selected row
                Item selectedRow = (Item) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);
                if (selectedRow == null || selectedRow.isDisposed() == true)
                {
                    View.this.updateStyledTexts();
                    return;
                }

                // get selected column
                int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);
                
                final PrebuildItem prebuildItem = (PrebuildItem) selectedRow.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                final LangFileObject langFileObject = LocalizablePluginManager.getCurrentLangFile();
                
                // get locale properties for save
                TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

                SpellObject[] editorSpellObjects = View.this.editorSpellObjectManager.getSpellObjects(LocalizablePluginManager.getCurrentLangFile(), prebuildItem.getKey(), selectedTargetLocale.getLocale(), View.this.editorStyledText.getText(), View.this.multilineEditor);

                for (int i = 0; offset != -1 && i < editorSpellObjects.length; i++)
                {
                    final SpellObject editorSpellObject = editorSpellObjects[i];
                    if (offset >= editorSpellObject.getOffset() && offset <= editorSpellObject.getOffset() + editorSpellObject.getLength())
                    {
                        MenuItem menuItem = null;
                        switch (editorSpellObject.getType())
                        {
                            case SpellChecker.TYPE_PARAM:
                            case SpellChecker.TYPE_REFERENCE:
                                for (int j = 0; j < editorSpellObjects.length; j++)
                                {
                                    if (editorSpellObjects[j].getType() == editorSpellObject.getType() && editorSpellObjects[j].getValue().equals(editorSpellObject.getValue()) == false)
                                    {
                                        menuItem = new MenuItem(menu, SWT.PUSH);
                                        menuItem.setText(editorSpellObjects[j].getValue());
                                        menuItem.addSelectionListener(new SelectionAdapter()
                                        {
                                            
                                            public void widgetSelected(SelectionEvent e)
                                            {
                                                View.this.editorStyledText.setSelection(editorSpellObject.getOffset(), editorSpellObject.getOffset() + editorSpellObject.getLength());
                                                Point selection = View.this.editorStyledText.getSelection();
                                                String data = ((MenuItem) e.widget).getText();
                                                View.this.editorStyledText.insert(data);
                                                selection.x = selection.x + data.length();
                                                selection.y = selection.x;
                                                View.this.editorStyledText.setSelection(selection);
                                                View.this.editorStyledText.setFocus();
                                            }
                                        });
                                    }
                                }
                                break;

                            case SpellChecker.TYPE_URL:
                                menuItem = new MenuItem(menu, SWT.PUSH);
                                menuItem.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.FollowTheLink"));
                                menuItem.addSelectionListener(new SelectionAdapter()
                                {
                                    
                                    public void widgetSelected(SelectionEvent e)
                                    {
                                        Utils.launch(editorSpellObject.getValue());
                                    }
                                });
                                break;

                            default:
                                if (editorSpellObject.getType() != SpellChecker.TYPE_TRANSLATED_WORDS && (editorSpellObject.getSuggestions() == null || editorSpellObject.getSuggestions().length == 0))
                                {
                                    menuItem = new MenuItem(menu, SWT.PUSH);
                                    menuItem.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.NoSuggestion"));
                                    menuItem.setEnabled(false);
                                }
                                else
                                {
                                    if (editorSpellObject.getType() == SpellChecker.TYPE_TRANSLATED_WORDS)
                                    {
                                        if (editorSpellObject.getOffset() + editorSpellObject.getLength() > View.this.editorStyledText.getText().length())
                                        {
                                            continue;
                                        }
                                        if (View.this.editorStyledText.getText().substring(editorSpellObject.getOffset(), editorSpellObject.getOffset() + editorSpellObject.getLength()).equalsIgnoreCase(editorSpellObject.getValue()) == false)
                                        {
                                            continue;
                                        }
                                    }

                                    for (int j = 0; j < editorSpellObject.getSuggestions().length; j++)
                                    {
                                        final Suggestion suggestion = editorSpellObject.getSuggestions()[j];
                                        menuItem = new MenuItem(menu, SWT.PUSH);
                                        menuItem.setImage(View.this.imageLoader.getImage("i18nAZ.image.toolbar." + ((editorSpellObject.getType() == SpellChecker.TYPE_TRANSLATED_WORDS) ? "translate" : "spellChecker")));
                                        menuItem.setText(suggestion.getName());
                                        menuItem.addSelectionListener(new SelectionAdapter()
                                        {
                                            
                                            public void widgetSelected(SelectionEvent e)
                                            {
                                                View.this.editorStyledText.setSelection(editorSpellObject.getOffset(), editorSpellObject.getOffset() + editorSpellObject.getLength());
                                                Point selection = View.this.editorStyledText.getSelection();
                                                String data = suggestion.getValue();
                                                View.this.editorStyledText.insert(data);
                                                selection.x = selection.x + data.length();
                                                selection.y = selection.x;
                                                View.this.editorStyledText.setSelection(selection);
                                                View.this.editorStyledText.setFocus();
                                                if (editorSpellObject.getType() == SpellChecker.TYPE_TRANSLATED_WORDS)
                                                {
                                                    editorSpellObject.length = data.length();
                                                    editorSpellObject.value = data;
                                                }
                                            }
                                        });

                                    }
                                }
                                if ((editorSpellObject.getType() & (SpellChecker.TYPE_MISSPELLING_ERROR | SpellChecker.TYPE_MISSPELLING_VUZE | SpellChecker.TYPE_MISSPELLING_DUPLICATE | SpellChecker.TYPE_MISSPELLING_EXTRA_SPACES)) != 0)
                                {
                                    new MenuItem(menu, SWT.SEPARATOR);
                                }
                                if ((editorSpellObject.getType() & SpellChecker.TYPE_MISSPELLING_ERROR) != 0)
                                {   
                                    menuItem = new MenuItem(menu, SWT.PUSH);
                                    menuItem.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.AddToDictionary"));
                                    menuItem.addSelectionListener(new SelectionAdapter()
                                    {
                                        
                                        public void widgetSelected(SelectionEvent e)
                                        {
                                            View.this.editorStyledText.setSelection(editorSpellObject.getOffset(), editorSpellObject.getOffset() + editorSpellObject.getLength());
                                            SpellChecker.add(editorSpellObject.getValue());
                                            Point selection = View.this.editorStyledText.getSelection();
                                            String data = editorSpellObject.getValue();
                                            View.this.editorStyledText.insert(data);
                                            View.this.editorSpellObjectManager.reset();
                                            View.this.updateEditor();
                                            selection.x = selection.x + data.length();
                                            selection.y = selection.x;
                                            View.this.editorStyledText.setSelection(selection);
                                        }
                                    });
                                }
                                if ((editorSpellObject.getType() & (SpellChecker.TYPE_MISSPELLING_ERROR | SpellChecker.TYPE_MISSPELLING_VUZE | SpellChecker.TYPE_MISSPELLING_DUPLICATE | SpellChecker.TYPE_MISSPELLING_EXTRA_SPACES)) != 0)
                                {   
                                    menuItem = new MenuItem(menu, SWT.PUSH);
                                    menuItem.setText(i18nAZ.getLocalisedMessageText("i18nAZ.Menus.Ignore"));
                                    menuItem.addSelectionListener(new SelectionAdapter()
                                    {
                                        
                                        public void widgetSelected(SelectionEvent e)
                                        {
                                            View.this.editorStyledText.setSelection(editorSpellObject.getOffset(), editorSpellObject.getOffset() + editorSpellObject.getLength());

                                            Point selection = View.this.editorStyledText.getSelection();
                                            SpellChecker.ignore(langFileObject, prebuildItem.getKey(), editorSpellObject);                      
                                            
                                            View.this.editorSpellObjectManager.reset();
                                            View.this.updateEditor();
                                            selection.x = selection.y;
                                            View.this.editorStyledText.setSelection(selection);
                                        }
                                    });
                                }
                                break;
                        }
                        break;
                    }
                }
                if (menu.getItemCount() > 0)
                {
                    new MenuItem(menu, SWT.SEPARATOR);
                }
                View.this.populateMenu(menu, MenuOptions.EDITOR, false);
            }
        });
        this.editorStyledText.addExtendedModifyListener(new ExtendedModifyListener()
        {
            
            public void modifyText(ExtendedModifyEvent e)
            {                
                //update Editor
                View.this.updateEditor();
            }
        });

        this.editorStyledText.addFocusListener(new FocusAdapter()
        {
            
            public void focusGained(FocusEvent e)
            {
                // get selected row
                Item selectedRow = (Item) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);
                if (selectedRow == null || selectedRow.isDisposed() == true)
                {
                    View.this.updateStyledTexts();
                    return;
                }

                // get selected column
                int selectedColumn = (Integer) View.this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

                // get locale properties for save
                TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

                if (selectedTargetLocale.isReadOnly() == true)
                {
                    return;
                }

                View.this.infoStyledText.setSelection(0, 0);
                View.this.pasteToolItem.setEnabled(View.this.clipboard.getContents(TextTransfer.getInstance()) != null);
                View.this.selectEditor();
            }

            
            public void focusLost(FocusEvent e)
            {
                View.this.valid(false, false);
            }
        });
        this.editorStyledText.addKeyListener(new KeyAdapter()
        {
            
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
            
            public void verifyKey(VerifyEvent e)
            {
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.character == SWT.CR)
                {
                    if (View.this.multilineEditor == false)
                    {
                        View.this.valid(true, SWT.DOWN);
                    }
                    else
                    {
                        Point selection = View.this.editorStyledText.getSelection();
                        View.this.editorStyledText.insert(Character.toString(SWT.LF));
                        selection.x = selection.x + 1;
                        selection.y = selection.x;
                        View.this.editorStyledText.setSelection(selection);
                    }
                    e.doit = false;
                }
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.PAGE_UP)
                {
                    View.this.valid(false, SWT.UP);
                    e.doit = false;
                }
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.PAGE_DOWN)
                {
                    View.this.valid(true, SWT.DOWN);
                    e.doit = false;
                }
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.TAB && View.this.multilineEditor == false)
                {
                    View.this.valid(false, SWT.DOWN);
                    e.doit = false;
                }
                if (e.stateMask == SWT.MOD2 && e.keyCode == 9 && View.this.multilineEditor == false)
                {
                    View.this.valid(false, SWT.UP);
                    e.doit = false;
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'z' && View.this.undoToolItem.getEnabled() == true)
                {
                    View.this.undoToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'y' && View.this.redoToolItem.getEnabled() == true)
                {
                    View.this.redoToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'x' && View.this.cutToolItem.getEnabled() == true)
                {
                    View.this.cutToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'c' && View.this.copyToolItem.getEnabled() == true)
                {
                    View.this.copyToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'v' && View.this.pasteToolItem.getEnabled() == true)
                {
                    View.this.pasteToolItem.notifyListeners(SWT.Selection, null);
                }
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'a' && View.this.selectAllToolItem.getEnabled() == true)
                {
                    View.this.selectAllToolItem.notifyListeners(SWT.Selection, null);
                }
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0 && e.keyCode == SWT.F8 && View.this.spellCheckerButton.isDisabled() == false)
                {
                    View.this.spellCheckerButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                    View.this.spellCheckerButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                    e.doit = false;
                }
            }
        });
    }

    private void initialize(Composite composite, SWTSkinObjectContainer skinObjectContainer)
    {
        this.clipboard = new Clipboard(Display.getCurrent());

        this.display = composite.getShell().getDisplay();

        this.mainChildContainer = new SWTSkinObjectContainer(SWTSkinFactory.getInstance(), SWTSkinFactory.getInstance().getSkinProperties(), "i18nAZ.main", "i18nAZ.main", skinObjectContainer);
        this.mainChildContainer.setControl(composite);
        GridLayout gridLayout = null;
        gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 1;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        this.mainChildContainer.getComposite().setLayout(gridLayout);
        Util.setGridData(this.mainChildContainer.getComposite(), SWT.FILL, SWT.FILL, true, true);

        // initialize Main ToolBar
        this.initializeMainToolBar();

        // create area composite
        this.areaContainer = new SWTSkinObjectContainer(SWTSkinFactory.getInstance(), SWTSkinFactory.getInstance().getSkinProperties(), "i18nAZ.main.area", "i18nAZ.main.area", this.mainChildContainer);
        gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 1;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        this.areaContainer.getComposite().setLayout(gridLayout);
        Util.setGridData(this.areaContainer.getComposite(), SWT.FILL, SWT.FILL, true, true);

        // initialize TableCombos
        this.initializeTableCombos();

        // initialize treetable
        TreeTableManager.initTreeTable(this.areaContainer);
        Util.setGridData(TreeTableManager.getCurrent(), SWT.FILL, SWT.FILL, true, false, -100);
        TreeTableManager.setVisible(false);

        // initialize loading composite
        this.initializeLoadingComposite();

        // initialize wizard composite
        this.initializeWizardComposite();

        // Create footer composite
        this.footerComposite = Util.getNewComposite(this.areaContainer.getComposite(), SWT.NONE, SWT.FILL, SWT.NULL, true, false, 0);
        Util.setGridData(this.footerComposite, SWT.FILL, SWT.NONE, true, false, 0);
        this.footerComposite.setVisible(false);

        // initialize info styled text
        this.initializeInfoLabel();

        // initialize editor toolbar
        this.initializeEditorToolBar();

        // initialize editor
        this.initializeEditor();

        // create status label
        this.statusLabel = new Label(this.areaContainer.getComposite(), SWT.NULL);
        Util.setGridData(this.statusLabel, SWT.FILL, SWT.NONE, true, false, 0);
        this.statusLabel.setVisible(false);

        synchronized (this.viewInitialized)
        {
            this.viewInitialized.set(true);
        }
        synchronized (this.initialized)
        {
            synchronized (this.wizard)
            {
                if (this.initialized.get() == false)
                {
                    if (this.wizard == false)
                    {
                        View.this.show(View.SHOW_LOADING);
                        return;
                    }
                    else
                    {
                        View.this.show(View.SHOW_WIZARD);
                        return;
                    }
                }
            }
        }

        this.enable();
    }
    boolean isCreated()
    {
        synchronized (View.this.viewCreated)
        {
            return this.viewCreated.get();
        }
    }   
    void itemEnterEventOccurred(Item item, int columnIndex)
    {
        PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);

        String message = "";
        if (columnIndex == 0)
        {
            if (TreeTableManager.Cursor.getColumn() < 2 || TreeTableManager.getChildItemCount(item) == 0)
            {
                return;
            }
            TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()).getData(View.DATAKEY_TARGET_LOCALE);
            CountObject counts = FilterManager.getCounts(prebuildItem.getKey(), selectedTargetLocale);
            message += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Items.ExpandMessage", new String[] { String.valueOf(counts.entryCount), String.valueOf(counts.emptyCount), String.valueOf(counts.unchangedCount), String.valueOf(counts.extraCount) });
        }
        else
        {
            if (prebuildItem.isExist() == false)
            {
                return;
            }
            message = TreeTableManager.getText(item, columnIndex);
            if (this.multilineEditor == false)
            {
                message = Util.unescape(message);
            }
            if (message.equals("") == true)
            {
                message = i18nAZ.getLocalisedMessageText("i18nAZ.Labels.Empty");
            }
        }
        ToolTipText.set(item, columnIndex, null, new String[] { prebuildItem.getKey() }, new String[] { message });
    }

    void populateMenu(final Menu menu, final int visible)
    {
        this.populateMenu(menu, visible, visible, true);
    }

    void populateMenu(final Menu menu, final int visible, boolean clear)
    {
        this.populateMenu(menu, visible, visible, clear);
    }

    void populateMenu(final Menu menu, final int visible, int enabled)
    {
        this.populateMenu(menu, visible, enabled, true);
    }

    void populateMenu(final Menu menu, final int visible, int enabled, boolean clear)
    {
        if (clear == true)
        {
            while (menu.getItemCount() > 0)
            {
                menu.getItem(0).dispose();
            }
        }
        MenuItem menuItem = null;

        if ((visible & MenuOptions.REMOVE_COLUMN) != 0)
        {
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.RemoveLanguage", new Listener()
            {
                
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
                
                public void handleEvent(Event e)
                {
                    View.this.undoToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.undoToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Redo", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    View.this.redoToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.redoToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            new MenuItem(menu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Cut", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    View.this.cutToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.cutToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));
            if ((visible & MenuOptions.COPY_VALUE) == 0)
            {
                menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Copy", new Listener()
                {
                    
                    public void handleEvent(Event e)
                    {
                        View.this.copyToolItem.notifyListeners(SWT.Selection, null);
                    }
                }, SWT.PUSH);
                menuItem.setEnabled(View.this.copyToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));
            }
        }

        // ROW_COPY VALUE
        if ((visible & MenuOptions.COPY_VALUE) != 0)
        {
            if ((visible & MenuOptions.REMOVE_COLUMN) != 0)
            {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.CopyValue", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    String textData = TreeTableManager.getText(item, TreeTableManager.Cursor.getColumn());
                    if (textData.equals("") == true)
                    {
                        View.this.clipboard.clearContents();
                    }
                    else
                    {
                        View.this.clipboard.setContents(new Object[] { textData }, new Transfer[] { TextTransfer.getInstance() });
                    }
                }
            }, SWT.PUSH);
            menuItem.setEnabled(true && ((enabled & MenuOptions.COPY_VALUE) != 0));
        }

        // COPY_KEY
        if ((visible & MenuOptions.COPY_KEY) != 0)
        {
            if ((visible & MenuOptions.REMOVE_COLUMN) != 0 && (visible & MenuOptions.COPY_VALUE) == 0)
            {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.CopyKey", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                    String textData = prebuildItem.getKey();
                    View.this.clipboard.setContents(new Object[] { textData }, new Transfer[] { TextTransfer.getInstance() });
                }
            }, SWT.PUSH);
            menuItem.setEnabled(true && ((enabled & MenuOptions.COPY_KEY) != 0));
        }

        // COPY_REFERENCE
        if ((visible & MenuOptions.COPY_REFERENCE) != 0)
        {
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.CopyReference", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    String textData = TreeTableManager.getText(item, 1);
                    View.this.clipboard.setContents(new Object[] { textData }, new Transfer[] { TextTransfer.getInstance() });
                }
            }, SWT.PUSH);
            menuItem.setEnabled(true && ((enabled & MenuOptions.COPY_REFERENCE) != 0));
        }

        // EDITOR
        if ((visible & MenuOptions.EDITOR) != 0)
        {
            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Paste", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    View.this.pasteToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.pasteToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            new MenuItem(menu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.SelectAll", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    View.this.selectAllToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.selectAllToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            new MenuItem(menu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Uppercase", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    View.this.upperCaseToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.upperCaseToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Lowercase", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    View.this.lowerCaseToolItem.notifyListeners(SWT.Selection, null);
                }
            }, SWT.PUSH);
            menuItem.setEnabled(View.this.lowerCaseToolItem.getEnabled() && ((enabled & MenuOptions.EDITOR) != 0));

            menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Firstcase", new Listener()
            {
                
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
                
                public void handleEvent(Event e)
                {
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    int columnIndex = (Integer) menu.getData(TreeTableManager.DATAKEY_COLUMN_INDEX);
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

            if ((visible & MenuOptions.COPY_KEY) != 0 && (visible & MenuOptions.SEARCH) == 0)
            {
                new MenuItem(menu, SWT.SEPARATOR);
            }

            Menu topMenu = menu;
            if ((visible & MenuOptions.TOPFILTERS) != 0)
            {
                menuItem = MenuFactory.addMenuItem(menu, "i18nAZ.Menus.Filters", new Listener()
                {
                    
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
                
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.emptyFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.emptyFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);

                    if (((MenuItem) e.widget).getSelection() == FilterManager.getCurrentFilter().empty)
                    {
                        FilterManager.getCurrentFilter().emptyExcludedKey.remove(prebuildItem.getKey());
                    }
                    else
                    {
                        FilterManager.getCurrentFilter().emptyExcludedKey.add(prebuildItem.getKey());
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.emptyFilterButton.isDisabled() == false));

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.UnchangedFilter", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.unchangedFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.unchangedFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);

                    if (((MenuItem) e.widget).getSelection() == FilterManager.getCurrentFilter().unchanged)
                    {
                        FilterManager.getCurrentFilter().unchangedExcludedKey.remove(prebuildItem.getKey());
                    }
                    else
                    {
                        FilterManager.getCurrentFilter().unchangedExcludedKey.add(prebuildItem.getKey());
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.unchangedFilterButton.isDisabled() == false));

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.ExtraFilter", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.extraFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.extraFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);

                    if (((MenuItem) e.widget).getSelection() == FilterManager.getCurrentFilter().extra)
                    {
                        FilterManager.getCurrentFilter().extraExcludedKey.remove(prebuildItem.getKey());
                    }
                    else
                    {
                        FilterManager.getCurrentFilter().extraExcludedKey.add(prebuildItem.getKey());
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.extraFilterButton.isDisabled() == false));

            new MenuItem(topMenu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.RedirectKeysFilter", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        View.this.redirectKeysFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseDown, null);
                        View.this.redirectKeysFilterButton.getSkinObject().getControl().notifyListeners(SWT.MouseUp, null);
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);

                    if (((MenuItem) e.widget).getSelection() == FilterManager.getCurrentFilter().redirectKeys)
                    {
                        FilterManager.getCurrentFilter().hideRedirectKeysExcludedKey.remove(prebuildItem.getKey());
                    }
                    else
                    {
                        FilterManager.getCurrentFilter().hideRedirectKeysExcludedKey.add(prebuildItem.getKey());
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.redirectKeysFilterButton.isDisabled() == false));

            new MenuItem(topMenu, SWT.SEPARATOR);

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.HideUrlsFilter", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        if (FilterManager.getCurrentFilter().urls != 1)
                        {
                            View.this.setUrlsFilterState(1);
                        }
                        else
                        {
                            View.this.setUrlsFilterState(0);
                        }

                        if (TreeTableManager.getCurrent() != null)
                        {
                            i18nAZ.viewInstance.updateTreeTable();
                        }
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                    if (FilterManager.getCurrentFilter().urls == 1)
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.remove(prebuildItem.getKey());
                        }
                        else
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.put(prebuildItem.getKey(), 0);
                            TreeTableManager.setExpanded(item, true);
                        }
                    }
                    else
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.put(prebuildItem.getKey(), 1);
                        }
                        else
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.remove(prebuildItem.getKey());
                            TreeTableManager.setExpanded(item, true);
                        }
                    }
                    View.this.updateTreeTable();
                }
            }, SWT.CHECK);
            menuItem.setEnabled((enabled & MenuOptions.FILTERS) != 0 || ((enabled & MenuOptions.TOPFILTERS) != 0 && this.urlsFilterButton.isDisabled() == false));

            menuItem = MenuFactory.addMenuItem(topMenu, "i18nAZ.Menus.ShowUrlsFilter", new Listener()
            {
                
                public void handleEvent(Event e)
                {
                    if ((visible & MenuOptions.TOPFILTERS) != 0)
                    {
                        if (FilterManager.getCurrentFilter().urls != 2)
                        {
                            View.this.setUrlsFilterState(2);
                        }
                        else
                        {
                            View.this.setUrlsFilterState(0);
                        }
                        if (TreeTableManager.getCurrent() != null)
                        {
                            i18nAZ.viewInstance.updateTreeTable();
                        }
                        return;
                    }
                    Item item = (Item) menu.getData(TreeTableManager.DATAKEY_ITEM);
                    PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
                    if (FilterManager.getCurrentFilter().urls == 2)
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.remove(prebuildItem.getKey());
                        }
                        else
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.put(prebuildItem.getKey(), 0);
                        }
                    }
                    else
                    {
                        if (((MenuItem) e.widget).getSelection() == true)
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.put(prebuildItem.getKey(), 2);
                        }
                        else
                        {
                            FilterManager.getCurrentFilter().urlsOverriddenStates.remove(prebuildItem.getKey());
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
            final TargetLocale[] targetLocales = TargetLocaleManager.toArray();
            final TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(columnIndex).getData(View.DATAKEY_TARGET_LOCALE);

            MessageBoxShell messageBoxShell = null;
            if (selectedTargetLocale.isReadOnly() == false)
            {
                String[] buttons = new String[] { MessageText.getString("Button.yes"), MessageText.getString("Button.no"), MessageText.getString("Button.cancel") };
                messageBoxShell = new MessageBoxShell(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.RemoveLanguage"), i18nAZ.getLocalisedMessageText("i18nAZ.Messages.Remove1"), buttons, 1);
            }
            else
            {
                String[] buttons = new String[] { MessageText.getString("Button.confirm"), MessageText.getString("Button.cancel") };
                messageBoxShell = new MessageBoxShell(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.RemoveLanguage"), i18nAZ.getLocalisedMessageText("i18nAZ.Messages.Remove2"), buttons, 1);
            }
            messageBoxShell.setLeftImage("image.trash");
            messageBoxShell.setSize(500, 120);
            messageBoxShell.open(new UserPrompterResultListener()
            {

                
                public void prompterClosed(int result)
                {
                    ImageLoader.getInstance().releaseImage("image.trash");
                    if (selectedTargetLocale.isReadOnly() == false)
                    {
                        if (result == 2)
                        {
                            return;
                        }   
                        if (result == 0)
                        {
                            LocalizablePlugin[] localizablePlugins = LocalizablePluginManager.toArray();
                            for (int i = 0; i < localizablePlugins.length; i++)
                            {
                                LangFileObject[] langFileObjects = localizablePlugins[i].toArray();
                                for (int j = 0; j < langFileObjects.length; j++)
                                {
                                    URL internalPath = langFileObjects[j].getInternalPath(selectedTargetLocale);
                                    Path.getFile(internalPath).delete();
                                }
                            }
                            if (selectedTargetLocale.getLocale().equals(Locale.getDefault()))
                            {
                                Path.getFile(i18nAZ.mergedInternalFile).delete();
                            }
                        }
                        for (int i = targetLocales.length - 1; i >= 0; i--)
                        {
                            if (targetLocales[i].getLocale().equals(selectedTargetLocale.getLocale()))
                            {
                                TargetLocaleManager.remove(targetLocales[i]);
                                TreeTableManager.removeColumns(i + 1);
                            }
                        }
                    }
                    else
                    {
                        if (result == 1)
                        {
                            return;
                        }
                        for (int i = targetLocales.length - 1; i >= 0; i--)
                        {
                            if (targetLocales[i].equals(selectedTargetLocale))
                            {
                                String langFileId = LocalizablePluginManager.getCurrentLangFile().getId();
                                if (targetLocales[i].getExternalPaths().containsKey(langFileId) == true)
                                {
                                    targetLocales[i].getExternalPaths().remove(langFileId);
                                }
                                if (targetLocales[i].getExternalPaths().size() == 0)
                                {
                                    TargetLocaleManager.remove(selectedTargetLocale);
                                    TreeTableManager.removeColumns(i + 1);
                                }
                                break;
                            }
                        }
                    }

                    View.saveLocales();
                    View.this.updateTreeTable(false, true);
                }
            });
          

        }
    }

    private static void saveLocales()
    {
        List<String> selectedLocales = new ArrayList<String>();
        TargetLocale[] targetLocales = TargetLocaleManager.toArray();
        for (int i = 0; i < targetLocales.length; i++)
        {
            Locale locale = targetLocales[i].getLocale();
            if (targetLocales[i].isReference() == false)
            {
                String selectedLocale = Util.getLanguageTag(locale);
                if (targetLocales[i].isReadOnly() == true)
                {
                    for (Iterator<Entry<String, ExternalPath>> iterator = targetLocales[i].getExternalPaths().entrySet().iterator(); iterator.hasNext();)
                    {
                        Entry<String, ExternalPath> entry = iterator.next();
                        selectedLocale += "|" + entry.getValue().getLangFileId() + ":" + Path.getPath(entry.getValue().getUrl());

                    }
                }
                selectedLocales.add(selectedLocale);
            }
        }
        COConfigurationManager.setParameter("i18nAZ.LocalesSelected", selectedLocales);
        COConfigurationManager.save();
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
        FilterManager.getCurrentFilter().emptyExcludedKey.clear();
        FilterManager.getCurrentFilter().empty = checked;
        View.checkButton(this.emptyFilterButton, checked);
    }

    private void setUnchangedFilter(boolean checked)
    {
        FilterManager.getCurrentFilter().unchangedExcludedKey.clear();
        FilterManager.getCurrentFilter().unchanged = checked;
        View.checkButton(this.unchangedFilterButton, checked);
    }

    private void setExtraFilter(boolean checked)
    {
        FilterManager.getCurrentFilter().extraExcludedKey.clear();
        FilterManager.getCurrentFilter().extra = checked;
        View.checkButton(this.extraFilterButton, checked);
    }

    private void setRedirectKeysFilter(boolean checked)
    {
        FilterManager.getCurrentFilter().hideRedirectKeysExcludedKey.clear();
        FilterManager.getCurrentFilter().redirectKeys = checked;
        View.checkButton(this.redirectKeysFilterButton, checked);
    }

    private void setUrlsFilterState(int state)
    {
        FilterManager.getCurrentFilter().urlsOverriddenStates.clear();
        FilterManager.getCurrentFilter().urls = state;
        View.checkButton(this.urlsFilterButton, state, "i18nAZ.image.toolbar.urlsFilter", "i18nAZ.ToolTips.UrlsFilter");
    }

    private void show(int what)
    {
        boolean showHeader = false;
        boolean hideFooter = true;
        switch (what)
        {
            case View.SHOW_SPLASH_SCREEN:

            case View.SHOW_LOADING:
                break;

            case View.SHOW_TREETABLE:
                hideFooter = false;
                showHeader = true;
                break;

            case View.SHOW_WIZARD:
                hideFooter = true;
                break;
        }
        this.show(what, showHeader, hideFooter);
    }

    private void show(int what, boolean showHeader, boolean hideFooter)
    {
        boolean isSplashScreen = false;
        boolean showLoading = false;
        boolean showTreeTable = false;
        boolean showWizard = false;
        boolean showPlugin = false;
        switch (what)
        {
            case View.SHOW_SPLASH_SCREEN:
                isSplashScreen = true;

            case View.SHOW_LOADING:
                showLoading = true;
                View.this.updateLoadingMessage(null);
                View.this.updateProgressBar(-1);
                break;
            case View.SHOW_TREETABLE:
                showTreeTable = true;
                showPlugin = View.this.pluginsCombo.getItemCount() > 0;
                break;

            case View.SHOW_WIZARD:
                showWizard = true;
                break;
        }
        // FOOTER
        if (hideFooter == true)
        {
            Util.setHeightHint(this.footerComposite, 0);
        }
        else
        {
            Util.setHeightHint(this.footerComposite, SWT.DEFAULT);
        }

        // HEADER
        this.headerContainer.getComposite().setVisible(showHeader);
        Util.setGridData(this.headerContainer.getComposite(), SWT.FILL, SWT.TOP, true, false, SWT.DEFAULT, showHeader == true ? SWT.DEFAULT : 0);

        // LOADING
        Util.setGridData(View.this.loadingComposite, SWT.CENTER, SWT.CENTER, false, showLoading, showLoading == true ? SWT.DEFAULT : 0);
        View.this.loadingComposite.setVisible(showLoading);

        // SPLASHSCREEN
        if (showLoading)
        {
            GridLayout gridLayout = new GridLayout(isSplashScreen == true ? 1 : 2, false);
            gridLayout.verticalSpacing = 0;
            gridLayout.horizontalSpacing = 0;
            gridLayout.marginHeight = 0;
            gridLayout.marginWidth = 0;
            View.this.loadingSubComposite.setLayout(gridLayout);
            Util.setGridData(View.this.bannerLabel, isSplashScreen == true ? SWT.CENTER : SWT.RIGHT, SWT.BOTTOM, true, true);
            Util.setGridData(View.this.animatedCanvas, isSplashScreen == true ? SWT.CENTER : SWT.LEFT, isSplashScreen == true ? SWT.CENTER : SWT.BOTTOM, true, true, isSplashScreen == true ? 0 : 80, isSplashScreen == true ? 0 : 80);
            View.this.animatedCanvas.setVisible(isSplashScreen == false);
        }

        // TREETABLE
        Util.setGridData(TreeTableManager.getCurrent(), SWT.FILL, SWT.FILL, true, showTreeTable, showTreeTable == true ? SWT.DEFAULT : -100);
        TreeTableManager.setVisible(showTreeTable);

        Util.setHeightHint(View.this.pluginComposite, (showPlugin) ? SWT.DEFAULT : 0);
        View.this.pluginComposite.setVisible(showPlugin);

        // WIZARD
        Util.setGridData(View.this.wizardComposite, SWT.FILL, SWT.FILL, true, showWizard, showWizard == true ? SWT.DEFAULT : 0);
        View.this.wizardComposite.setVisible(showWizard);

        // redraw
        Util.redraw(View.this.areaContainer.getComposite());
        Util.redraw(View.this.mainChildContainer.getComposite());

    }

    void startInitialization()
    {
        synchronized (View.this.initialized)
        {
            if (View.this.initialized.get() == false)
            {
                i18nAZ.log("Main initializing...");
            }
        }

        LocalePropertiesLoader.start();

        if (TargetLocaleManager.isInitialized() == false)
        {
            i18nAZ.log("TargetLocaleManager init...");
            TargetLocaleManager.init();
            i18nAZ.log("TargetLocaleManager OK!");
        }

        LocalizablePluginManager.addListener(new LocalizablePluginListener()
        {
            
            public void changed(LocalizablePluginEvent e)
            {
                Utils.execSWTThread(new AERunnable()
                {
                    
                    public void runSupport()
                    {
                        LangFileObject langFileObject = View.this.fillPluginsCombo();
                        if (LocalizablePluginManager.setCurrentLangFile(langFileObject) == true)
                        {
                            synchronized (View.this.initialized)
                            {
                                if (View.this.initialized.get() == true)
                                {
                                    View.this.updateTreeTable(false, false);
                                }
                            }
                        }
                        synchronized (View.this.initialized)
                        {
                            if (View.this.initialized.get() == false)
                            {
                                View.this.initialized.set(true);
                                i18nAZ.log("Main initialized !");
                                View.this.enable();
                            }
                        }
                    }
                });
            }
        });

        LocalizablePluginManager.start();

        // init thread
        AEThread2 initThread = new AEThread2("i18nAZ.initThread")
        {
            
            public void run()
            {
                boolean initialized = false;
                synchronized (View.this.initialized)
                {
                    initialized = View.this.initialized.get();
                }
                if (initialized == false)
                {
                    if (View.this.sideBarEntry != null && View.this.sideBarEntry.getVitalityImages().length == 0)
                    {
                        View.this.sideBarEntry.addVitalityImage("image.sidebar.vitality.dots");
                    }
                    while (true)
                    {
                        synchronized (View.this.initialized)
                        {
                            if (View.this.initialized.get() == true)
                            {
                                if (View.this.sideBarEntry != null && View.this.sideBarEntry.getVitalityImages().length > 0)
                                {
                                    View.this.sideBarEntry.getVitalityImages()[0].setVisible(false);
                                }
                                TargetLocaleManager.notifyCountListeners(null);
                                break;
                            }
                        }
                        Util.sleep(1000);
                    }
                }
                LocaleProperties localeProperties = Util.loadLocaleProperties(Locale.getDefault(), i18nAZ.mergedInternalFile);
                if (localeProperties == null)
                {
                    i18nAZ.mergeBundleFile();
                    localeProperties = Util.loadLocaleProperties(Locale.getDefault(), i18nAZ.mergedInternalFile);
                }
                if (localeProperties != null && localeProperties.IsLoaded() == true)
                {
                    localeProperties.clear();
                }
            }
        };
        initThread.start();
    }

    void updateInfoText()
    {
        String localisedMessageText = "";
        if (TreeTableManager.getItemCount() == 0)
        {
            if (FilterManager.getCurrentFilter().isTextEnabled() == false)
            {
                localisedMessageText = i18nAZ.getLocalisedMessageText("i18nAZ.Labels.Noentry");
            }
            else
            {
                localisedMessageText = i18nAZ.getLocalisedMessageText("i18nAZ.Labels.UnsuccessfulSearch");
            }
        }
        else
        {
            if (TreeTableManager.Cursor.getColumn() >= 2)
            {
                TargetLocale selectedTargetLocale = ((TargetLocale) TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()).getData(View.DATAKEY_TARGET_LOCALE));
                CountObject counts = LocalizablePluginManager.getCurrentLangFile().getCounts(selectedTargetLocale);
                if (FilterManager.getCurrentFilter().isTextEnabled() == false)
                {
                    localisedMessageText = i18nAZ.getLocalisedMessageText("i18nAZ.Labels.Informations.Prefix");
                }
                else
                {
                    localisedMessageText = i18nAZ.getLocalisedMessageText("i18nAZ.Labels.SearchResult.Prefix");
                }
                localisedMessageText += " " + i18nAZ.getLocalisedMessageText("i18nAZ.Labels.Informations", new String[] { String.valueOf(counts.entryCount), String.valueOf(counts.emptyCount), String.valueOf(counts.unchangedCount), String.valueOf(counts.extraCount) });

            }
            else
            {
                localisedMessageText = i18nAZ.getLocalisedMessageText("i18nAZ.Labels.Nolanguage");
            }
        }
        this.infoText.setText(localisedMessageText);
        this.updatePluginsCombo();
        this.updateFilesCombo();
        if (TreeTableManager.Cursor.getColumn() < TreeTableManager.getColumnCount())
        {
            this.updateToolTipColumnHeader(TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()));
        }
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
        GridData footerGridData = (GridData) this.footerComposite.getLayoutData();
        GridData infoGridData = (GridData) this.infoStyledText.getLayoutData();
        GridData toolBarGridData = (GridData) this.toolBar.getLayoutData();
        GridData editorGridData = (GridData) this.editorStyledText.getLayoutData();

        // get selected row
        Item selectedRow = TreeTableManager.Cursor.getRow();

        // get selected column
        int selectedColumn = TreeTableManager.Cursor.getColumn();

        // show/hide label & editor
        if (TreeTableManager.getSelection().length == 0 || TreeTableManager.getColumnCount() < 2 || selectedRow == null)
        {
            // set datas
            this.editorStyledText.setData(View.DATAKEY_SELECTED_ROW, null);
            this.editorStyledText.setData(View.DATAKEY_SELECTED_COLUMN, 0);

            // set heights
            footerGridData.heightHint = 0;
            infoGridData.heightHint = 0;
            toolBarGridData.heightHint = 0;
            footerGridData.heightHint = 0;

            // set visible
            this.footerComposite.setVisible(false);

            // layout all
            Util.redraw(this.mainChildContainer.getComposite());
            return;
        }

        // set heights
        footerGridData.heightHint = SWT.DEFAULT;
        infoGridData.heightHint = SWT.DEFAULT;

        // set visible
        this.footerComposite.setVisible(true);

        // get default locales
        TargetLocale defaultTargetLocale = (TargetLocale) TreeTableManager.getColumn(1).getData(View.DATAKEY_TARGET_LOCALE);
        TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

        // get prebuild
        PrebuildItem prebuildItem = (PrebuildItem) selectedRow.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);

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
        String keyInfo = prebuildItem.getKey();
        if (selectedRow != null && TreeTableManager.getChildItemCount(selectedRow) > 0 && selectedColumn >= 2)
        {
            CountObject counts = FilterManager.getCounts(prebuildItem.getKey(), selectedTargetLocale);
            keyInfo += " - " + i18nAZ.getLocalisedMessageText("i18nAZ.Labels.Comments", new String[] { String.valueOf(counts.entryCount), String.valueOf(counts.emptyCount), String.valueOf(counts.unchangedCount), String.valueOf(counts.extraCount) });
        }
        if (selectedTargetLocale != null && selectedTargetLocale.isReadOnly() == true && selectedTargetLocale.isVisible() == true)
        {
            keyInfo += " [read-only]";
        }

        keyInfo += "\n";

        // get reference info
        String currentReferenceInfo = (reference == "") ? "" : "\n" + reference;

        // get comments info
        String commentsInfo = "";
        if (prebuildItem.getCommentsLines() != null)
        {
            for (int i = 0; i < prebuildItem.getCommentsLines().length; i++)
            {
                commentsInfo += prebuildItem.getCommentsLines()[i].replaceAll("\\n", "\\\\n") + "\n";
            }
            commentsInfo = Util.trimNewLine(commentsInfo);
            commentsInfo = (commentsInfo == "") ? "" : "\n\n" + commentsInfo;
        }

        // get references info
        String referencesInfo = "";
        if (selectedColumn >= 2)
        {
            String[] refs = Util.getReferences((String) defaultTargetLocale.getProperties().get(prebuildItem.getKey()));
            while (true)
            {
                if (refs.length > 0)
                {
                    List<String> values = new ArrayList<String>();
                    for (int i = 0; i < refs.length; i++)
                    {
                        referencesInfo += refs[i] + " => ";
                        String ref = refs[i].substring(1, refs[i].length() - 1);
                        String value = "";
                        if (selectedTargetLocale.getProperties().containsKey(ref))
                        {
                            value = (String) selectedTargetLocale.getProperties().get(ref);
                        }
                        else if (defaultTargetLocale.getProperties().containsKey(ref))
                        {
                            value = (String) defaultTargetLocale.getProperties().get(ref);
                        }
                        else if (i18nAZ.getPluginInterface().getUtilities().getLocaleUtilities().hasLocalisedMessageText(ref))
                        {
                            value = i18nAZ.getPluginInterface().getUtilities().getLocaleUtilities().getLocalisedMessageText(ref);
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
        styleRange = new StyleRange(0, prebuildItem.getKey().length(), this.infoStyledText.getForeground(), null);
        styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
        this.infoStyledText.setStyleRange(styleRange);

        // set styles for info key
        styleRange = new StyleRange(prebuildItem.getKey().length(), keyInfo.length() - prebuildItem.getKey().length(), this.infoStyledText.getForeground(), null);
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

        if (FilterManager.getCurrentFilter().isTextEnabled("r") == true)
        {
            for (Iterator<Entry<Pattern, Object>> iterator = FilterManager.getCurrentFilter().getPatterns(); iterator.hasNext();)
            {
                Entry<Pattern, Object> entry = iterator.next();
                Pattern searchPattern = entry.getKey();
                boolean searchResult = (Boolean) entry.getValue();
                Matcher matcher = searchPattern.matcher(currentReferenceInfo);
                matcher.reset();
                while (matcher.find() == searchResult)
                {
                    styleRange = new StyleRange(keyInfo.length() + matcher.start(), matcher.end() - matcher.start(), this.infoStyledText.getForeground(), Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
                    this.infoStyledText.setStyleRange(styleRange);
                }
            }
        }
        if (FilterManager.getCurrentFilter().isTextEnabled("c") == true)
        {
            for (Iterator<Entry<Pattern, Object>> iterator = FilterManager.getCurrentFilter().getPatterns(); iterator.hasNext();)
            {
                Entry<Pattern, Object> entry = iterator.next();
                Pattern searchPattern = entry.getKey();
                boolean searchResult = (Boolean) entry.getValue();
                Matcher matcher = searchPattern.matcher(commentsInfo);
                matcher.reset();
                while (matcher.find() == searchResult)
                {
                    int offset = keyInfo.length() + currentReferenceInfo.length() + matcher.start();
                    int length = matcher.end() - matcher.start();
                    StyleRange[] styleRanges = this.infoStyledText.getStyleRanges(offset, length);
                    if (styleRanges == null || styleRanges.length == 0)
                    {
                        styleRanges = new StyleRange[1];
                        styleRanges[0] = new StyleRange(offset, length, this.infoStyledText.getForeground(), Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
                    }
                    for (int j = 0; j < styleRanges.length; j++)
                    {
                        if (styleRanges[j].background.getRGB().equals(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW).getRGB()))
                        {
                            continue;
                        }
                        styleRanges[j].background = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
                        this.infoStyledText.setStyleRange(styleRanges[j]);
                    }
                }
            }
        }
        // set data for info
        this.infoStyledText.setData(View.DATAKEY_DEFAULT_STYLES, this.infoStyledText.getStyleRanges(true));

        // found params & references for info
        this.infoSpellObjects = SpellChecker.get(LocalizablePluginManager.getCurrentLangFile(),  prebuildItem.getKey(), keyInfo.length(), currentReferenceInfo, null);

        // apply styles for infoStyledText
        View.formatStyledText(this.infoStyledText, this.infoSpellObjects, true);

        // set data for editor
        this.editorStyledText.setData(View.DATAKEY_SELECTED_ROW, selectedRow);
        this.editorStyledText.setData(View.DATAKEY_SELECTED_COLUMN, selectedColumn);

        this.editorSpellObjectManager = prebuildItem.getSpellObjectManager(selectedColumn);

        // set editor value
        String text = currentValue;
        if (currentValue.equals("") == true && selectedTargetLocale.isReadOnly() == false)
        {
            text = reference;
            String result = View.this.translate(text, selectedTargetLocale.getLocale());
            if (result != null)
            {
                text = result;
            }
        }
        this.editorStyledText.setText(text);

        // show/hide editor
        if (prebuildItem.isExist() == true && selectedColumn >= 2 && !(reference.equals("") == true && currentValue.equals("") == true))
        {
            // set heights
            toolBarGridData.heightHint = SWT.DEFAULT;
            editorGridData.heightHint = SWT.DEFAULT;
            editorGridData.minimumHeight = 100;

            // set undo redo
            this.undoRedo.set(LocalizablePluginManager.getCurrentLangFile().getId(), prebuildItem.getKey());

            // set visibles
            this.toolBar.setVisible(true);
            this.editorStyledText.setVisible(true);
            if (selectedTargetLocale.isReadOnly() == true)
            {
                this.undoToolItem.setEnabled(false);
                this.redoToolItem.setEnabled(false);
                this.cutToolItem.setEnabled(false);
                this.copyToolItem.setEnabled(false);
                this.pasteToolItem.setEnabled(false);
                this.selectAllToolItem.setEnabled(false);
                this.upperCaseToolItem.setEnabled(false);
                this.lowerCaseToolItem.setEnabled(false);
                this.firstCaseToolItem.setEnabled(false);
                this.trademarkToolItem.setEnabled(false);
                this.translateToolItem.setEnabled(false);
                this.validateToolItem.setEnabled(false);
                this.cancelToolItem.setEnabled(false);
                this.editorStyledText.setEditable(false);
                this.editorStyledText.update();
            }
            else
            {
                this.trademarkToolItem.setEnabled(true);
                this.translateToolItem.setEnabled(true);
                this.editorStyledText.setEditable(true);
                this.editorStyledText.update();
            }
        }
        else
        {
            // set heights
            toolBarGridData.heightHint = 0;
            editorGridData.heightHint = 0;
            editorGridData.minimumHeight = 0;

            // set visibles
            this.toolBar.setVisible(false);
            this.editorStyledText.setVisible(false);
        }

        // layout all
        if (this.loadingComposite.isVisible() == false)
        {
            Util.redraw(this.mainChildContainer.getComposite());
        }
    }

    private void updateToolTipColumnHeader(Item column)
    {
        TargetLocale selectedTargetLocale = (TargetLocale) column.getData(View.DATAKEY_TARGET_LOCALE);
        if (selectedTargetLocale != null && selectedTargetLocale.isReference() == false)
        {
            CountObject counts = LocalizablePluginManager.getCurrentLangFile().getCounts(selectedTargetLocale);
            double percent = (1 - ((double) counts.emptyCount) / ((double) counts.entryCount));
            if (percent > 1)
            {
                percent = 1;
            }
            DecimalFormat decimalFormat = new DecimalFormat("0.00 %");
            //decimalFormat.setRoundingMode(RoundingMode.DOWN);
            decimalFormat.setMaximumFractionDigits(2);
            decimalFormat.setMinimumFractionDigits(2);

            String headerTooltTipText = i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Title") + "\n\n";
            headerTooltTipText += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.TranslationProgress", new String[] { decimalFormat.format(percent) }) + "\n";

            if (selectedTargetLocale.getLocale().getDisplayLanguage() != null && selectedTargetLocale.getLocale().getDisplayLanguage().equals("") == false)
            {
                String DisplayLanguage = selectedTargetLocale.getLocale().getDisplayLanguage();
                DisplayLanguage = Character.toTitleCase(DisplayLanguage.charAt(0)) + DisplayLanguage.substring(1).toLowerCase(selectedTargetLocale.getLocale());
                String LocalizedDisplayLanguage = selectedTargetLocale.getLocale().getDisplayLanguage(selectedTargetLocale.getLocale());
                LocalizedDisplayLanguage = Character.toTitleCase(LocalizedDisplayLanguage.charAt(0)) + LocalizedDisplayLanguage.substring(1).toLowerCase(selectedTargetLocale.getLocale());
                headerTooltTipText += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Language", new String[] { DisplayLanguage, LocalizedDisplayLanguage }) + "\n";
            }
            if (selectedTargetLocale.getLocale().getDisplayCountry() != null && selectedTargetLocale.getLocale().getDisplayCountry().equals("") == false)
            {
                String DisplayCountry = selectedTargetLocale.getLocale().getDisplayCountry();
                DisplayCountry = Character.toTitleCase(DisplayCountry.charAt(0)) + DisplayCountry.substring(1).toLowerCase(selectedTargetLocale.getLocale());
                String LocalizedDisplayCountry = selectedTargetLocale.getLocale().getDisplayCountry(selectedTargetLocale.getLocale());
                LocalizedDisplayCountry = Character.toTitleCase(LocalizedDisplayCountry.charAt(0)) + LocalizedDisplayCountry.substring(1).toLowerCase(selectedTargetLocale.getLocale());
                headerTooltTipText += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Country", new String[] { DisplayCountry, LocalizedDisplayCountry }) + "\n";
            }
            if (selectedTargetLocale.getLocale().getDisplayVariant() != null && selectedTargetLocale.getLocale().getDisplayVariant().equals("") == false)
            {
                String DisplayVariant = selectedTargetLocale.getLocale().getDisplayVariant();
                DisplayVariant = Character.toTitleCase(DisplayVariant.charAt(0)) + DisplayVariant.substring(1).toLowerCase(selectedTargetLocale.getLocale());
                String LocalizedDisplayVariant = selectedTargetLocale.getLocale().getDisplayVariant(selectedTargetLocale.getLocale());
                LocalizedDisplayVariant = Character.toTitleCase(LocalizedDisplayVariant.charAt(0)) + LocalizedDisplayVariant.substring(1).toLowerCase(selectedTargetLocale.getLocale());
                headerTooltTipText += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Variant", new String[] { DisplayVariant, LocalizedDisplayVariant }) + "\n";
            }
            headerTooltTipText += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.LanguageTag", new String[] { Util.getLanguageTag(selectedTargetLocale.getLocale()).replace("-", "") }) + "\n";
            if (selectedTargetLocale.isReadOnly() == true && selectedTargetLocale.isVisible() == true)
            {
                headerTooltTipText += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Path", new String[] { Path.getPath(selectedTargetLocale.getExternalPath()) });
            }
            else if (selectedTargetLocale.getInternalPath() != null)
            {
                headerTooltTipText += i18nAZ.getLocalisedMessageText("i18nAZ.ToolTips.Columns.Header.Path", new String[] { selectedTargetLocale.getInternalPath().toString() });
            }
            TreeTableManager.setToolTipColumnHeader(column, headerTooltTipText);
        }
    }

    private void updateTreeTable()
    {
        this.updateTreeTable(false, true);
    }

    private void updateTreeTable(boolean savePosition)
    {
        this.updateTreeTable(true, savePosition);
    }

    private void updateTreeTable(boolean refreshColumns, boolean savePosition)
    {
        synchronized (this.viewInitialized)
        {
            if (this.viewInitialized.get() == false)
            {
                return;
            }
        }
        this.addLanguageButton.setDisabled(true);
        this.exportLanguageButton.setDisabled(true);
        this.importLanguageButton.setDisabled(true);
        this.removeLanguageButton.setDisabled(true);
        this.emptyFilterButton.setDisabled(true);
        this.unchangedFilterButton.setDisabled(true);
        this.extraFilterButton.setDisabled(true);
        this.spellCheckerButton.setDisabled(true);
        this.treeModeButton.setDisabled(true);
        this.redirectKeysFilterButton.setDisabled(true);
        this.urlsFilterButton.setDisabled(true);
        this.searchTextbox.getControl().setEnabled(false);
        this.deletableRows.clear();

        TreeTableManager.removeAll(savePosition);

        TargetLocale[] targetLocales = TargetLocaleManager.toArray();
        if (refreshColumns == true || TreeTableManager.getColumnCount() == 0 || LocalizablePluginManager.getCurrentLangFile() == null)
        {
            TreeTableManager.removeAllColumns();

            if (LocalizablePluginManager.getCurrentLangFile() != null)
            {
                int width = COConfigurationManager.getIntParameter("i18nAZ.columnWidth.0", 200);
                Item column = TreeTableManager.addColumn(i18nAZ.getLocalisedMessageText("i18nAZ.Columns.Key"), width);
                if (column instanceof TableColumn)
                {
                    Util.addSortManager((TableColumn) column, Util.KEY_COMPARATOR);
                }

                for (int i = 0; i < targetLocales.length; i++)
                {
                    this.addLocaleColumn(targetLocales[i]);
                }
            }
        }
        for (int i = 1; LocalizablePluginManager.getCurrentLangFile() != null && i < targetLocales.length; i++)
        {
            TreeTableManager.setEnableColumn(TreeTableManager.getColumn(i + 1), targetLocales[i].isVisible());
        }
        if (TreeTableManager.getColumnCount() <= 1)
        {
            this.setRedirectKeysFilter(false);
            this.setUrlsFilterState(0);
        }
        else
        {
            if (FilterManager.getCurrentFilter().redirectKeys != COConfigurationManager.getBooleanParameter("i18nAZ.redirectKeysFilter"))
            {
                this.setRedirectKeysFilter(COConfigurationManager.getBooleanParameter("i18nAZ.redirectKeysFilter"));
            }
            if (FilterManager.getCurrentFilter().urls != COConfigurationManager.getIntParameter("i18nAZ.urlsFilter"))
            {
                this.setUrlsFilterState(COConfigurationManager.getIntParameter("i18nAZ.urlsFilter", 0));
            }
        }
        if (TreeTableManager.getColumnCount() <= 2)
        {
            this.setEmptyFilter(false);
            this.setUnchangedFilter(false);
            this.setExtraFilter(false);
        }
        else
        {
            if (FilterManager.getCurrentFilter().empty != COConfigurationManager.getBooleanParameter("i18nAZ.emptyFilter"))
            {
                this.setEmptyFilter(COConfigurationManager.getBooleanParameter("i18nAZ.emptyFilter"));
            }
            if (FilterManager.getCurrentFilter().unchanged != COConfigurationManager.getBooleanParameter("i18nAZ.unchangedFilter"))
            {
                this.setUnchangedFilter(COConfigurationManager.getBooleanParameter("i18nAZ.unchangedFilter"));
            }
            if (FilterManager.getCurrentFilter().extra != COConfigurationManager.getBooleanParameter("i18nAZ.extraFilter"))
            {
                this.setExtraFilter(COConfigurationManager.getBooleanParameter("i18nAZ.extraFilter"));
            }
        }
        if (TreeTableManager.getColumnCount() > 0)
        {
            this.addLanguageButton.setDisabled(false);
            this.exportLanguageButton.setDisabled(false);
            this.importLanguageButton.setDisabled(false);
            this.treeModeButton.setDisabled(false);
            this.redirectKeysFilterButton.setDisabled(false);
            this.urlsFilterButton.setDisabled(false);
            this.searchTextbox.getControl().setEnabled(true);
        }
        if (TreeTableManager.getColumnCount() > 2)
        {
            this.emptyFilterButton.setDisabled(false);
            this.unchangedFilterButton.setDisabled(false);
            this.extraFilterButton.setDisabled(false);
        }

        TreeTableManager.buildItems();

        TargetLocaleManager.notifyCountListeners(null);
    }

    private void valid(boolean force, int direction)
    {
        int columnIndex = TreeTableManager.Cursor.getColumn();
        Item selectedRow = TreeTableManager.Cursor.getRow();
        PrebuildItem prebuildItem = null;

        if (View.this.validateToolItem.getEnabled() == true)
        {
            if (View.this.valid(force, false) == false)
            {
                return;
            }
        }

        TreeTableManager.setRedraw(false);
        while (true)
        {
            TreeTableManager.Cursor.notifyListeners(SWT.KeyDown, Util.createKeyEvent(TreeTableManager.getCurrent(), direction == SWT.UP ? SWT.ARROW_UP : SWT.ARROW_DOWN));
            selectedRow = TreeTableManager.Cursor.getRow();
            if (TreeTableManager.Cursor.getRow() == null || selectedRow == null)
            {
                break;
            }
            if (selectedRow.equals(TreeTableManager.Cursor.getRow()) == true)
            {
                break;
            }
            selectedRow = TreeTableManager.Cursor.getRow();
            prebuildItem = (PrebuildItem) selectedRow.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
            if ((force == false || View.this.deletableRows.containsValue(selectedRow) == false) && prebuildItem.isExist() == true)
            {
                break;
            }
        }
        if (force == true)
        {
            View.this.clearDeletableRows();
        }
        if (selectedRow != null)
        {
            TreeTableManager.Cursor.setSelection(selectedRow, columnIndex);
        }
        TreeTableManager.setRedraw(true);
        View.this.selectEditor();
    }

    private boolean valid(boolean force, boolean deleteRows)
    {
        synchronized (this.onValid)
        {
            if (this.onValid.get() == true)
            {
                return false;
            }
            this.onValid.set(true);
        }

        // show/hide value editor
        if (this.editorStyledText.getVisible() == false)
        {
            synchronized (this.onValid)
            {
                this.onValid.set(false);
            }
            return false;
        }

        // init
        String errorMessage = null;

        // get selected row
        Item selectedRow = (Item) this.editorStyledText.getData(View.DATAKEY_SELECTED_ROW);
        if (selectedRow == null || selectedRow.isDisposed() == true)
        {
            this.updateStyledTexts();
            synchronized (this.onValid)
            {
                this.onValid.set(false);
            }
            return false;
        }

        // get selected column
        int selectedColumn = (Integer) this.editorStyledText.getData(View.DATAKEY_SELECTED_COLUMN);

        // get locale properties for save
        TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(selectedColumn).getData(View.DATAKEY_TARGET_LOCALE);

        // check readOnly
        if (selectedTargetLocale.isReadOnly() == true)
        {
            this.updateStyledTexts();
            synchronized (this.onValid)
            {
                this.onValid.set(false);
            }
            return false;
        }
        
        // get prebuild
        PrebuildItem prebuildItem = (PrebuildItem) selectedRow.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
        
        // find spellObject
        final SpellObject[] editorSpellObjects = View.this.editorSpellObjectManager.getSpellObjects(LocalizablePluginManager.getCurrentLangFile(), prebuildItem.getKey(), selectedTargetLocale.getLocale(), View.this.editorStyledText.getText(), View.this.multilineEditor);

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
        View.formatStyledText(this.infoStyledText, this.infoSpellObjects, true);

        // search missing params
        if (errorMessage == null)
        {
            SpellObject missingParam = SpellChecker.foundMissing(View.this.infoSpellObjects, editorSpellObjects, SpellChecker.TYPE_PARAM);
            if (missingParam != null)
            {
                // set not found param style
                StyleRange styleRange = new StyleRange(missingParam.getOffset(), missingParam.getLength(), new Color(Display.getCurrent(), 163, 21, 21), null);
                styleRange.font = new Font(null, this.infoStyledText.getFont().getFontData()[0].getName(), 9, SWT.BOLD);
                styleRange.underline = true;
                styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                styleRange.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
                this.infoStyledText.setStyleRange(styleRange);

                // show error message box
                errorMessage = "";
                if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                {
                    errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.MissingParam", new String[] { missingParam.getValue() });
                    i18nAZ.logWarning(errorMessage);
                }
            }
        }

        // search unknown params
        if (errorMessage == null)
        {
            SpellObject unknownParam = SpellChecker.foundMissing(editorSpellObjects, View.this.infoSpellObjects, SpellChecker.TYPE_PARAM);
            if (unknownParam != null)
            {
                // show error message box
                errorMessage = "";
                if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                {
                    errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.UnknownParam", new String[] { unknownParam.getValue() });
                    i18nAZ.logWarning(errorMessage);
                }
            }
        }

        // search missing references
        if (errorMessage == null)
        {
            SpellObject missingReference = SpellChecker.foundMissing(View.this.infoSpellObjects, editorSpellObjects, SpellChecker.TYPE_REFERENCE);
            if (missingReference != null && reference.equals(missingReference.getValue()) == false)
            {
                // set not found references style
                StyleRange styleRange = new StyleRange(missingReference.getOffset() + 1, missingReference.getLength() - 2, Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
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
                            errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.InvalidValue", new String[] { reference });
                            i18nAZ.logWarning(errorMessage);
                        }
                    }
                    else
                    {
                        errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.MissingReference", new String[] { missingReference.getValue() });
                        i18nAZ.logWarning(errorMessage);
                    }
                }
            }
        }

        // Search unknown references
        if (errorMessage == null)
        {
            SpellObject unknownReference = SpellChecker.foundMissing(editorSpellObjects, View.this.infoSpellObjects, SpellChecker.TYPE_REFERENCE);
            if (unknownReference != null)
            {
                // show error message box
                errorMessage = "";
                if (TreeTableManager.Cursor.isSetFocusedRow() == false || force == true)
                {
                    errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.UnknownReference", new String[] { unknownReference.getValue() });
                    i18nAZ.logWarning(errorMessage);
                }
            }
        }
    
        // search url error
        if (errorMessage == null)
        {
            if ((prebuildItem.getStates()[1] & State.URL) != 0)
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
                        errorMessage = i18nAZ.getLocalisedMessageText("i18nAZ.Messages.MalformedURL", new String[] { newValue });
                        i18nAZ.logWarning(errorMessage);
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
            synchronized (this.onValid)
            {
                this.onValid.set(false);
            }
            return false;
        }
        else
        {
            TreeTableManager.Cursor.cancelfocusedRow();
        }

        // get new state
        int newState = Util.getStateOfValue(reference, newValue);

        // check opportunity
        if ((!(oldValue.equals(newValue) == true && prebuildItem.getStates()[selectedColumn] == newState) && (!(prebuildItem.getStates()[selectedColumn] == 1 && newState == 2) || force == true)) || (force == true && this.deletableRows.size() > 0))
        {
            // update cell
            prebuildItem.setValue(selectedColumn, newValue, newState);
            TreeTableManager.refreshItem(selectedRow);
            TreeTableManager.setRedraw(true);

            // update resource bundle
            selectedTargetLocale.getProperties().put(prebuildItem.getKey(), Util.unescape(newValue));

            // add to save objects
            this.saveObjects.add(new SaveObject(selectedTargetLocale));

            // init values for check
            boolean showable = FilterManager.getPrebuildItem(prebuildItem.getKey(), prebuildItem.getLangFileObject()) != null;

            if (showable == false && TreeTableManager.getChildItemCount(selectedRow) == 0)
            {
                this.deletableRows.put(prebuildItem.getKey(), selectedRow);
            }
            else if (this.deletableRows.containsKey(prebuildItem.getKey()))
            {
                this.deletableRows.remove(prebuildItem.getKey());
            }
            if (deleteRows == true)
            {
                this.clearDeletableRows();
            }
        }

        // set focus
        if (TreeTableManager.getItemCount() > 0)
        {
            if (force)
            {
                this.updateStyledTexts();
            }
            View.this.validateToolItem.setEnabled(false);
            View.this.cancelToolItem.setEnabled(false);
            TreeTableManager.Cursor.setFocus();
        }
        else
        {
            this.updateStyledTexts();
        }
        synchronized (this.onValid)
        {
            this.onValid.set(false);
        }
        return true;
    }

    private void clearDeletableRows()
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

    public void updateSpellCheckerButton()
    {
        TargetLocale targetLocale = (TargetLocale) TreeTableManager.getColumn(TreeTableManager.Cursor.getColumn()).getData(View.DATAKEY_TARGET_LOCALE);
        Dictionary dictionary = null;
        if (targetLocale != null)
        {
            dictionary = SpellChecker.getDictionary(targetLocale.getLocale());
        }
        this.spellCheckerButton.setDisabled(TreeTableManager.Cursor.getColumn() < 2 || dictionary == null || targetLocale.isReadOnly() == true);
    }

    public void spellCheck()
    {
     // View.this.show(View.SHOW_LOADING, true, false);
        int columnIndex = TreeTableManager.Cursor.getColumn();
        Item selectedRow = TreeTableManager.Cursor.getRow();

        TargetLocale selectedTargetLocale = (TargetLocale) TreeTableManager.getColumn(columnIndex).getData(View.DATAKEY_TARGET_LOCALE);

        TreeTableManager.setRedraw(false);
        View.this.editorStyledText.setRedraw(false);
        View.this.infoStyledText.setRedraw(false);
        View.this.toolBar.setRedraw(false);
        boolean isFirst = true;
        boolean foundSelected = false;
        SpellObject spellObject = null;

        List<Item> items = Util.getAllItems(TreeTableManager.getCurrent(), null);

        for (int i = 0; i < items.size(); i++)
        {
            Item item = items.get(i);
            if (item.equals(selectedRow) == false && foundSelected == false)
            {
                continue;
            }
            foundSelected = true;
            PrebuildItem prebuildItem = (PrebuildItem) item.getData(TreeTableManager.DATAKEY_PREBUILD_ITEM);
            if (prebuildItem.isExist() == false)
            {
                continue;
            }
            String value = prebuildItem.getValues()[columnIndex];
            if (View.this.multilineEditor == true)
            {
                value = Util.unescape(value);
            }

            SpellObject[] spellObjects = prebuildItem.getSpellObjectManager(columnIndex).getSpellObjects(LocalizablePluginManager.getCurrentLangFile(), prebuildItem.getKey(),selectedTargetLocale.getLocale(), value, View.this.multilineEditor);
            int offset = 0;
            if (isFirst == true)
            {
                offset = View.this.editorStyledText.getSelection().y;
            }
            for (int j = 0; j < spellObjects.length; j++)
            {
                if ((spellObjects[j].getType() & SpellChecker.TYPE_MISSPELLING) != 0)
                {
                    if (offset <= spellObjects[j].getOffset())
                    {
                        spellObject = spellObjects[j];
                        break;
                    }
                }
            }
            if (spellObject != null)
            {
                if (TreeTableManager.Cursor.getRow() == null || TreeTableManager.Cursor.getRow().equals(item) == false)
                {
                    TreeTableManager.Cursor.setSelection(item, columnIndex);
                }
                break;
            }                   
            isFirst = false;
        }
        TreeTableManager.setRedraw(true);
        View.this.editorStyledText.setRedraw(true);
        View.this.infoStyledText.setRedraw(true);
        View.this.toolBar.setRedraw(true);
        // View.this.show(View.SHOW_TREETABLE); 
        if (View.this.editorStyledText.isFocusControl() == false)
        {
            View.this.editorStyledText.setFocus();
        }
        if (spellObject != null)
        {                   
            View.this.editorStyledText.setSelection(spellObject.getOffset(), spellObject.getOffset() + spellObject.getLength());
        }
        else
        {
            String[] buttons = new String[] { MessageText.getString("Button.ok")}; 
            MessageBoxShell messageBoxShell = new MessageBoxShell(i18nAZ.getLocalisedMessageText("i18nAZ.Titles.EndCheckSpelling"), i18nAZ.getLocalisedMessageText("i18nAZ.Messages.EndCheckSpelling"), buttons, 1);
            
            messageBoxShell.setLeftImage("i18nAZ.image.toolbar.spellChecker");
            messageBoxShell.setSize(500, 80);
            messageBoxShell.open(new UserPrompterResultListener()
            {
                
                public void prompterClosed(int result)
                {
                    
                }
            });
            if (TreeTableManager.getItemCount() > 0)
            {
                TreeTableManager.Cursor.setSelection(TreeTableManager.getItem(0), columnIndex);
            }
        }
        
    }
}