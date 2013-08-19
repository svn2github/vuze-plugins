/*
 * AMP - Azureus Monitoring Plugin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.pmm.monitoringplugin.text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.eclipse.swt.SWT.BEGINNING;
import static org.eclipse.swt.SWT.COLOR_BLACK;
import static org.eclipse.swt.SWT.COLOR_WHITE;
import static org.eclipse.swt.SWT.DRAW_MNEMONIC;
import static org.eclipse.swt.SWT.DRAW_TRANSPARENT;
import static org.eclipse.swt.SWT.NONE;
import static org.eclipse.swt.SWT.PUSH;
import static org.eclipse.swt.SWT.VERTICAL;
import static org.eclipse.swt.SWT.WRAP;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import static org.eclipse.swt.layout.GridData.HORIZONTAL_ALIGN_BEGINNING;
import static org.eclipse.swt.layout.GridData.HORIZONTAL_ALIGN_END;
import static org.eclipse.swt.layout.GridData.VERTICAL_ALIGN_BEGINNING;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import static org.eclipse.ui.forms.widgets.ExpandableComposite.CLIENT_INDENT;
import static org.eclipse.ui.forms.widgets.ExpandableComposite.EXPANDED;
import static org.eclipse.ui.forms.widgets.ExpandableComposite.TREE_NODE;
import static org.eclipse.ui.forms.widgets.ExpandableComposite.TWISTIE;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.Formatters;
import org.pmm.monitoringplugin.AMPConfig;
import org.pmm.monitoringplugin.layout.FixedColumnLayout;
import org.pmm.monitoringplugin.layout.KeyValueLayout;

import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * @author kustos
 * 
 * The textual view of the AMP Plugin.
 */
public class AMPView implements UISWTViewEventListener  {

    private static final String SWT_PLATFORM = "SWT Platform";
    private static final String VM_INFO = "VM Info:";
    private static final String DATA_MODEL = "Data Model:";
    private static final String ISA_LIST = "ISA List:";
    private static final String SWT_VERSION = "SWT Version:";
    private static final String MEMORY_MANAGERS_TITLE = "Memory Managers";
    private static final String MAX_FILE_DESCRIPTORS = "Max file descriptors:";
    private static final String OPEN_FILE_DESCRIPTORS = "Open file descriptors:";
    private static final String TOTAL_SWAP_SPACE = "Total swap space:";
    private static final String FREE_SWAP_SPACE = "Free swap space:";
    private static final String TOTAL_PHYSICAL_MEMORY = "Total physical memory:";
    private static final String FREE_PHYSICAL_MEMORY = "Free physical memory:";
    private static final String COMMITED_VIRTUAL_MEMORY = "Commited virtual memory:";
    private static final String PROCESS_CPU_TIME = "Process CPU time:";
    private static final String BYTES_UNSUPPORTED = "N/A";
    private static final String POOL_TYPE = "Type:";
    private static final String TYPE_NON_HEAP = "non-heap";
    private static final String TYPE_HEAP = "heap";
    private static final String MEMORY_POOLS_TITLE = "Memory Pools";
    private static final String GC_BUTTON_TITLE = "Garbage Collect";
    private static final String COLLECTION_TIME = "Collection time:";
    private static final String COLLECTION_COUNT = "Collection count:";
    private static final String GARBAGE_TITLE = "Garbage Collection";
    private static final String CLASSES_UNLOADED = "unloaded:";
    private static final String CLASSES_TOTAL_LOADED = "Total loaded:";
    private static final String CLASSES_TITLE = "Classes";
    private static final String CLASSES_CURRENTLY_LOADED = "Currently loaded:";
    private static final String UPTIME = "Uptime:";
    private static final String START_TIME = "Start time:";
    private static final String VM_VENDOR = "VM Vendor:";
    private static final String VM_VERSION = "VM Version:";
    private static final String VM_NAME = "VM Name:";
    private static final String OBJECTS_PENDING_FINALIZATION = "Objects pending finalization:";
    private static final String RUNTIME_TITLE = "Runtime";
    private static final String AVAILABLE_PROCESSORS = "Available processors:";
    private static final String OS_ARCH = "Architecture:";
    private static final String OS_VERSION = "Version:";
    private static final String OS_NAME = "Name:";
    private static final String OS_TITLE = "Operating System";
    private static final String UNSUPPORTED_COMPILETIME = "Unsupported";
    private static final String NO_COMPILER = "None";
    private static final String COMPILE_TIME = "Compile time:";
    private static final String COMPILER_NAME = "Compiler name:";
    private static final String COMPILER_TITLE = "Compiler";
    private static final String MAXIMUM = "Maximum:";
    private static final String COMMITTED = "Committed:";
    private static final String USED = "Used:";
    private static final String INITIAL = "Initial:";
    private static final String NON_HEAP_TITLE = "Non-Heap";
    private static final String HEAP_TITLE = "Heap";
    private static final String TOTAL_THREADS_STARTED = "Total started:";
    private static final String NON_DAEMON_THREADS = "Non-Daemon threads:";
    private static final String DAEMON_THREADS = "Daemon threads:";
    private static final String LIVE_THREADS = "Live threads:";
    private static final String THREAD_SECTION_TITLE = "Threads";
    private static final String MEMORY_SECTION_TITLE = "Memory";
    private static final String GC_BUTTON_IMAGE = "org/pmm/monitoringplugin/icons/stock_trash-16.gif";

    private static final String FORM_TITLE = "Azureus Monitoring";

    private PluginInterface pluginInterface;

    protected ScrolledForm summaryForm;

    private FormToolkit toolKit;

    private MemoryMXBean memoryBean;
    private ThreadMXBean threadBean;
    private CompilationMXBean compilationBean;
    private RuntimeMXBean runtimeBean;
    private ClassLoadingMXBean classBean;
    private List<GarbageCollectorMXBean> garbageBeans;
    private List<MemoryPoolMXBean> poolBeans;
    private OperatingSystemMXBean osBean;

    private Label pendingFinalizationLabel;    
    private Label liveThreadsLabel;
    private Label nonDeamonThreadsLabel;
    private Label daemonThreadsLabel;
    private Label totalThreadsStartedLabel;
    private Label heapInitialLabel;
    private Label heapUsedLabel;
    private Label heapCommittedLabel;
    private Label heapMaximumLabel;
    private Label nonHeapInitialLabel;
    private Label nonHeapUsedLabel;
    private Label nonHeapCommittedLabel;
    private Label nonHeapMaximumLabel;    
    private Label compilerNameLabel;
    private Label compileTimeLabel;    
    private Label uptimeLabel;
    private Label classesCurrentlyLoadedLabel;
    private Label classesTotalLoaded;
    private Label classesUnloadedLabel;
    private List<Label> collectionCountLabels;
    private List<Label> collectionTimeLabels;
    private LinkedList<Label> poolInitLabels;
    private LinkedList<Label> poolUsedLabels;
    private LinkedList<Label> poolCommittedLabels;
    private LinkedList<Label> poolMaxLabels;
    
    /**
     * Flag to signal if all components are initialized.
     */
    protected boolean initialized;
    private Label processCPUTimeLabel;
    private Label commitedVirtMemLabel;
    private Label freePhysicalLabel;
    private Label totalPhysicalLabel;
    private Label freeSwapLabel;
    private Label totalSwapLabel;
    private Label openFileDescriptorCountLabel;
    private Label maxFileDescriptorCountLabel;
    
    protected AMPConfig config;
    
    /**
     * Constructs a new AMPView.
     * @param pluginInterface the plugin interface
     */
    public AMPView(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
        this.config = new AMPConfig(pluginInterface.getPluginconfig());
    }

    /**
     * This method is caled when the view is destroyed. 
     * @see org.gudy.azureus2.ui.swt.views.IView#delete()
     */
    private void delete() {
        this.config.save();
        this.toolKit.dispose();        
        this.summaryForm.dispose();        
        
        Class<?> currentClass = AMPView.class;
        Field[] fields = currentClass.getDeclaredFields();
        for (Field nextField : fields) {
            String fieldName = nextField.getName();
            if (fieldName.endsWith("Beans") || fieldName.endsWith("Labels")) { //if it's a list, clear it
                try {
                    List<?> list = (List<?>) nextField.get(this);
                    try {
                        list.clear();
                    } catch (UnsupportedOperationException use) {
                        //Weblogic JRockit uses unmodifiable lists, I can't clear them
                    }
                } catch (IllegalArgumentException e) {
//                  shouldn't happen, we are modifing ourselves and the fields are not primitive
                } catch (IllegalAccessException e) {
//                  shouldn't happen, we are modifing ourselves
                }
            }
            
            if (fieldName.endsWith("Label") || fieldName.endsWith("Bean")|| fieldName.endsWith("Beans")) {
                try {
                    nextField.set(this, null); //set beans and labels to null to allow garbage collection
                } catch (IllegalArgumentException e) {
                    //shouldn't happen, we are modifing ourselves and the fields are not primitive
                } catch (IllegalAccessException e) {
                    //shouldn't happen, we are modifing ourselves
                }
            }
        }
        
        //set to null to allow garbage collection
        this.summaryForm = null;
        this.toolKit = null;
    }

    /**
     * This method is called when the view is instanciated, it initializes all
     * GUI components.
     * 
     * @param parent the parent composite
     */
    private void initialize(final Composite parent) {
        this.initBeans();
        this.toolKit = new FormToolkit(parent.getDisplay());
        
        this.initializeSummaryForm(parent);
        this.initialized = false;        
        parent.getDisplay().asyncExec(new Runnable() {
            public void run() {
                AMPView.this.createMemorySection();
                AMPView.this.createMemoryPoolsSection();
                AMPView.this.createGarbageCollectionSection();
                AMPView.this.createThreadSection();        
                AMPView.this.createClassSection();
                AMPView.this.createCompilerSection();        
                AMPView.this.createOSSection();
                AMPView.this.createRuntimeSection();
                AMPView.this.createMemoryManagersSection();
                AMPView.this.initialized = true;
                AMPView.this.refresh();
                AMPView.this.relayoutSections();
            }
        });
    }
    
    /**
     * Relayouts all sections in the form. Call this method after all the contens
     * and values have been set.
     */
    protected void relayoutSections() {
        //XXX might be replaced with layout(true, true) in SWT 3.1
        for (Control nextControl : this.summaryForm.getBody().getChildren()) {
            if (nextControl instanceof Composite) {
                Composite nextComposite = (Composite) nextControl;
                nextComposite.layout(true);
            }
        }
    }
    
    /**
     * Initializes all MXBeans.
     */
    private void initBeans() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.compilationBean = ManagementFactory.getCompilationMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.classBean = ManagementFactory.getClassLoadingMXBean();
        this.garbageBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.poolBeans = ManagementFactory.getMemoryPoolMXBeans();
    }

    /**
     * This builds and initialzes the main from and also sets the layout
     * of the parent Composite.
     * @param parent the parent Composite
     */
    private void initializeSummaryForm(Composite parent) {
        
        parent.setLayout(new FillLayout());
        
        this.summaryForm = this.toolKit.createScrolledForm(parent);
        this.summaryForm.setText(FORM_TITLE);
        
        this.summaryForm.getBody().setLayout(new FixedColumnLayout());
    }

    protected void createMemorySection() {
        Section section = this.createSectionNamed(MEMORY_SECTION_TITLE);
        this.expandAndAddListener(section, AMPConfig.MEMORY_SECTION);

        Composite client = this.createSectionClient(section, true);        
        
        this.createHeapSection(client);
        this.createNonHeapSection(client);
        
        this.createNameLabel(client, OBJECTS_PENDING_FINALIZATION);
        this.pendingFinalizationLabel = this.createValueTextLabel(client);
        
        this.createGCButton(section, client);
        
        section.setClient(client);
    }

    private void createGCButton(Section section, Composite parent) {
        Button gcButton = this.toolKit.createButton(parent, GC_BUTTON_TITLE, PUSH);
        InputStream imageStream = new BufferedInputStream(this.getClass().getClassLoader().getResourceAsStream(GC_BUTTON_IMAGE));
        ImageData imageData = new ImageData(imageStream);
        Image icon = new Image(section.getDisplay(), imageData);
        setButtonUI(gcButton, GC_BUTTON_TITLE, icon);
        icon.dispose();
        try {
            imageStream.close();
        } catch (IOException e) {
            //so we couldn't close it, probably was already, we can't do anything about that now
        }
        this.addGCFunction(gcButton);
        gcButton.setLayoutData(this.getButtonLayoutData());
    }
    
    private void addGCFunction(Button button) {
        final Runnable gcRun = new Runnable() {
            public void run() {
                System.gc();
            }
        };
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Thread t = new Thread(gcRun);
                t.start();
            }
        });
    }
    
    private List<Label> createMemorySubSection(Composite parent, String title) {
        List<Label> labels = new LinkedList<Label>();
        Section section = this.createSubSectionNamed(title, parent);
        section.setLayoutData(this.createSubSectionLayoutData());
        
        Composite client = this.createSectionClient(section, false);
        
        this.createNameLabel(client, INITIAL);
        labels.add(this.createValueNumberLabel(client));
        
        this.createNameLabel(client, USED);
        labels.add(this.createValueNumberLabel(client));
        
        this.createNameLabel(client, COMMITTED);
        labels.add(this.createValueNumberLabel(client));
        
        this.createNameLabel(client, MAXIMUM);
        labels.add(this.createValueNumberLabel(client));
        
        section.setClient(client);
        
        return labels;
    }
    
    private void createHeapSection(Composite parent) {
        List<Label> labels = this.createMemorySubSection(parent, HEAP_TITLE); 
        this.heapInitialLabel = labels.get(0);
        this.heapUsedLabel = labels.get(1);
        this.heapCommittedLabel = labels.get(2);
        this.heapMaximumLabel = labels.get(3);
    }
    
    private void createNonHeapSection(Composite parent) {                
        List<Label> labels = this.createMemorySubSection(parent, NON_HEAP_TITLE); 
        this.nonHeapInitialLabel = labels.get(0);
        this.nonHeapUsedLabel = labels.get(1);
        this.nonHeapCommittedLabel = labels.get(2);
        this.nonHeapMaximumLabel = labels.get(3);
    }
    
    protected void createThreadSection() {        
        Section section = this.createSectionNamed(THREAD_SECTION_TITLE);
        this.expandAndAddListener(section, AMPConfig.THREADS_SECTION);
        
        Composite client = this.createSectionClient(section, false);
        
        this.createNameLabel(client, LIVE_THREADS);
        this.liveThreadsLabel = this.createValueNumberLabel(client);
        
        this.createNameLabel(client, NON_DAEMON_THREADS);
        this.nonDeamonThreadsLabel = this.createValueNumberLabel(client);
        
        this.createNameLabel(client, DAEMON_THREADS);
        this.daemonThreadsLabel = this.createValueNumberLabel(client);
        
        this.createNameLabel(client, TOTAL_THREADS_STARTED);
        this.totalThreadsStartedLabel = this.createValueNumberLabel(client);
        
        section.setClient(client);
    }
    
    protected void createCompilerSection() {        
        Section section = this.createSectionNamed(COMPILER_TITLE);
        this.expandAndAddListener(section, AMPConfig.COMPILER_SECTION);
        Composite client = this.createSectionClient(section, true);
        
        this.createNameLabel(client, COMPILER_NAME);
        this.compilerNameLabel = this.createValueTextLabel(client);
        
        this.createNameLabel(client, COMPILE_TIME);        
        this.compileTimeLabel = this.createValueTextLabel(client);
        
        section.setClient(client);
    }
    
    protected void createOSSection() {                
        Section section = this.createSectionNamed(OS_TITLE);
        this.expandAndAddListener(section, AMPConfig.OS_SECTION);
        Composite client = this.createSectionClient(section, true);
        
        this.createNameLabel(client, OS_NAME);
        Label osNameLabel = this.createValueTextLabel(client);
        osNameLabel.setText(this.osBean.getName());
        
        this.createNameLabel(client, OS_VERSION);
        Label osVersionLabel = this.createValueTextLabel(client);
        osVersionLabel.setText(this.osBean.getVersion());
        
        this.createNameLabel(client, OS_ARCH);
        Label osArchLabel = this.createValueTextLabel(client);
        osArchLabel.setText(this.osBean.getArch());
        
        this.createNameLabel(client, AVAILABLE_PROCESSORS);
        Label availableProcessorsLabel = this.createValueTextLabel(client);
        availableProcessorsLabel.setText(this.formatInt(this.osBean.getAvailableProcessors()));
                
        createSunExtensionOsSection(client);
        section.setClient(client);
    }

    private void createSunExtensionOsSection(Composite client) {      
        if (this.isSunOsBeanExtension()) {
            
            this.createNameLabel(client, PROCESS_CPU_TIME);
            this.processCPUTimeLabel = this.createValueTextLabel(client);
            
            this.createNameLabel(client, COMMITED_VIRTUAL_MEMORY);
            this.commitedVirtMemLabel = this.createValueTextLabel(client);
            
            this.createNameLabel(client, FREE_PHYSICAL_MEMORY);
            this.freePhysicalLabel = this.createValueTextLabel(client);
            
            this.createNameLabel(client, TOTAL_PHYSICAL_MEMORY);
            this.totalPhysicalLabel = this.createValueTextLabel(client);
            
            this.createNameLabel(client, FREE_SWAP_SPACE);
            this.freeSwapLabel = this.createValueTextLabel(client);
            
            this.createNameLabel(client, TOTAL_SWAP_SPACE);
            this.totalSwapLabel = this.createValueTextLabel(client);
            if (this.isSunUnixOsBeanExtension()) {
                this.createNameLabel(client, OPEN_FILE_DESCRIPTORS);
                this.openFileDescriptorCountLabel = this.createValueTextLabel(client);
                
                this.createNameLabel(client, MAX_FILE_DESCRIPTORS);
                this.maxFileDescriptorCountLabel = this.createValueTextLabel(client);
            }
        }
    }
    
    private void refreshOSSection() {
        if (this.isSunOsBeanExtension()) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) this.osBean;
            long cpuTime = sunOsBean.getProcessCpuTime();
            //XXX docu says value is in nanos, but I have to divide it by 10^6 to make sense
            String text = this.formatMilliseconds(cpuTime / 1000000, false);
            this.processCPUTimeLabel.setText(text);
            
            text = this.formatBytes(sunOsBean.getCommittedVirtualMemorySize());
            this.commitedVirtMemLabel.setText(text);
            
            text = this.formatBytes(sunOsBean.getFreePhysicalMemorySize());
            this.freePhysicalLabel.setText(text);
            
            text = this.formatBytes(sunOsBean.getTotalPhysicalMemorySize());
            this.totalPhysicalLabel.setText(text);
            
            text = this.formatBytes(sunOsBean.getFreeSwapSpaceSize());
            this.freeSwapLabel.setText(text);
            
            text = this.formatBytes(sunOsBean.getTotalSwapSpaceSize());
            this.totalSwapLabel.setText(text);
            
            if (this.isSunUnixOsBeanExtension()) {
            	
            	// AMC: I can't use the compile with the below code, since the class
            	// isn't available to me. I'll have to do it all with reflection instead.
            	
            	/*
            	com.sun.management.UnixOperatingSystemMXBean sunUnixBean = (com.sun.management.UnixOperatingSystemMXBean) this.osBean;
                text = this.formatLong(sunUnixBean.getOpenFileDescriptorCount());
                this.openFileDescriptorCountLabel.setText(text);
                
                text = this.formatLong(sunUnixBean.getMaxFileDescriptorCount());
                this.maxFileDescriptorCountLabel.setText(text);
                */
            	
            	try {
            		this.openFileDescriptorCountLabel.setText(String.valueOf(
            			this.osBean.getClass().getMethod("getOpenFileDescriptorCount").invoke(this.osBean)
            		));
            	}
            	catch (Throwable t) {}

            	try {
            		this.maxFileDescriptorCountLabel.setText(String.valueOf(
            			this.osBean.getClass().getMethod("getMaxFileDescriptorCount").invoke(this.osBean)
            		));
            	}
            	catch (Throwable t) {}

            
            }
        }
    }
    
    private boolean isSunOsBeanExtension() {
        return this.doesOsBeanImplementInterface("com.sun.management.OperatingSystemMXBean");
    }
    
    private boolean isSunUnixOsBeanExtension() {
        return this.doesOsBeanImplementInterface("com.sun.management.UnixOperatingSystemMXBean");
    }
    
    private boolean doesOsBeanImplementInterface(String interfaceName) {
        try {
            Class<?> sunOsBean = Class.forName(interfaceName);
            return sunOsBean.isAssignableFrom(this.osBean.getClass());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    protected void createRuntimeSection() {
        Formatters formatter = this.pluginInterface.getUtilities().getFormatters();        
        Section section = this.createSectionNamed(RUNTIME_TITLE);
        this.expandAndAddListener(section, AMPConfig.RUNTIME_SECTION);
        Composite client = this.createSectionClient(section, true);
        
        this.createNameLabel(client, VM_NAME);
        Label vmNameLabel = this.createValueTextLabel(client);
        vmNameLabel.setText(this.runtimeBean.getVmName());
        
        this.createNameLabel(client, VM_VERSION);
        Label vmVersionLabel = this.createValueTextLabel(client);
        vmVersionLabel.setText(this.runtimeBean.getVmVersion());
        
        this.createNameLabel(client, VM_VENDOR);
        Label vmVendorLabel = this.createValueTextLabel(client);
        vmVendorLabel.setText(this.runtimeBean.getVmVendor());
        
        this.createLabelIfExists("java.vm.info", VM_INFO, client);
        
        this.createNameLabel(client, SWT_PLATFORM);
        Label swtPlatformLabel = this.createValueTextLabel(client);
        swtPlatformLabel.setText(SWT.getPlatform());
        
        String swtVersion = this.getSWTVersion();
        if (swtVersion != null) {
            this.createNameLabel(client, SWT_VERSION);
            Label swtVersionLabel = this.createValueTextLabel(client);
            swtVersionLabel.setText(swtVersion);
        }
        
        this.createLabelIfExists("sun.cpu.isalist", ISA_LIST, client);
        
        String dataModel = System.getProperty("sun.arch.data.model");
        if (dataModel != null && dataModel.length() > 0) {
            this.createNameLabel(client, DATA_MODEL);
            Label isaListLabel = this.createValueTextLabel(client);
            isaListLabel.setText(dataModel + " bit");
        }
        
        this.createNameLabel(client, START_TIME);
        Label startTimeLabel = this.createValueTextLabel(client);
        String startTime = formatter.formatDate(this.runtimeBean.getStartTime());
        startTimeLabel.setText(startTime);
        
        this.createNameLabel(client, UPTIME);
        this.uptimeLabel = this.createValueTextLabel(client);
        
        section.setClient(client);
    }
    
    private void createLabelIfExists(String key, String description, Composite parent) {
        String value = System.getProperty(key);
        if (value != null && value.length() > 0) {
            this.createNameLabel(parent, description);
            Label label = this.createValueTextLabel(parent);
            label.setText(value);
        }
    }
    
    protected void createMemoryManagersSection() {        
        Section section = this.createSectionNamed(MEMORY_MANAGERS_TITLE);
//        this.expandAndAddListener(section, AMPConfig.MEMORY_MANAGERS_SECTION, false);
        Composite client = this.toolKit.createComposite(section, NONE);
        client.setLayout(new RowLayout(VERTICAL));
        
        List<MemoryManagerMXBean> managers = ManagementFactory.getMemoryManagerMXBeans();
        for (MemoryManagerMXBean nextMangager : managers) {
            Section managerSection = this.createSubSectionNamed(nextMangager.getName(), client);
            Composite managerClient = this.toolKit.createComposite(managerSection, NONE);
            managerClient.setLayout(new RowLayout(VERTICAL));
            for (String nextPoolName : nextMangager.getMemoryPoolNames()) {
                Label nextPoolLabel = this.toolKit.createLabel(managerClient, nextPoolName, NONE);
                RowData rowData = new RowData();
                rowData.width = 120;
                nextPoolLabel.setLayoutData(rowData);
            }
            managerSection.setClient(managerClient);
        }        
        section.setClient(client);
        
        section.setExpanded(false);
        section.setExpanded(true);
        this.expandAndAddListener(section, AMPConfig.MEMORY_MANAGERS_SECTION, false);
    }
    
    protected void createClassSection() {        
        Section section = this.createSectionNamed(CLASSES_TITLE);
        this.expandAndAddListener(section, AMPConfig.CLASSES_SECTION);
        Composite client = this.createSectionClient(section, false);
        
        this.createNameLabel(client, CLASSES_CURRENTLY_LOADED);
        this.classesCurrentlyLoadedLabel = this.createValueNumberLabel(client);
        
        this.createNameLabel(client, CLASSES_TOTAL_LOADED);
        this.classesTotalLoaded = this.createValueNumberLabel(client);
        
        this.createNameLabel(client, CLASSES_UNLOADED);
        this.classesUnloadedLabel = this.createValueNumberLabel(client);
        
        section.setClient(client);
    }
    
    protected void createGarbageCollectionSection() {         
        Section section = this.createSectionNamed(GARBAGE_TITLE);       
        this.expandAndAddListener(section, AMPConfig.GARBAGE_COLLECTION_SECTION);
        Composite client = this.createSectionClient(section, true);
        
        this.collectionCountLabels = new LinkedList<Label>();
        this.collectionTimeLabels = new LinkedList<Label>();
        
        for (GarbageCollectorMXBean nextBean : this.garbageBeans) {
            this.createGarbageCollectorSubsection(client, nextBean);
        }
        
        section.setClient(client);
    }
    
    private void createGarbageCollectorSubsection(Composite parent, GarbageCollectorMXBean garbageBean) {
        Section section = this.createSubSectionNamed(garbageBean.getName(), parent);
        section.setLayoutData(this.createSubSectionLayoutData());
        
        Composite client = this.createSectionClient(section, true);
        this.createNameLabel(client, COLLECTION_COUNT);
        Label collectionCountLabel = this.createValueTextLabel(client);
        this.collectionCountLabels.add(collectionCountLabel);
        
        this.createNameLabel(client, COLLECTION_TIME);
        Label collectionTimeLabel = this.createValueTextLabel(client);
        this.collectionTimeLabels.add(collectionTimeLabel);
        
        section.setClient(client);
    }
    
    protected void createMemoryPoolsSection() {        
        Section section = this.createSectionNamed(MEMORY_POOLS_TITLE);
        this.expandAndAddListener(section, AMPConfig.MEMORY_POOLS_SECTION, false);
        Composite client = this.createSectionClient(section, true);
        
        this.poolInitLabels = new LinkedList<Label>();
        this.poolUsedLabels = new LinkedList<Label>();
        this.poolCommittedLabels = new LinkedList<Label>();
        this.poolMaxLabels = new LinkedList<Label>();
        
        for (MemoryPoolMXBean nextBean : this.poolBeans) {
            this.createMemoryPoolSubsection(client, nextBean);
        }
        
        section.setClient(client);
    }
    
    protected void createMemoryPoolSubsection(Composite parent, MemoryPoolMXBean poolBean) {
        Section section = this.createSubSectionNamed(poolBean.getName(), parent);
        section.setLayoutData(this.createSubSectionLayoutData());
        
        Composite client = this.createSectionClient(section, false);
        
        this.createNameLabel(client, POOL_TYPE); 
        Label typeLabel = this.createValueTextLabel(client);
        if (poolBean.getType() == MemoryType.HEAP) {
            typeLabel.setText(TYPE_HEAP);
        } else {
            typeLabel.setText(TYPE_NON_HEAP);
        }
        
        this.createNameLabel(client, INITIAL);
        Label initalLabel = this.createValueNumberLabel(client);
        this.poolInitLabels.add(initalLabel);
        
        this.createNameLabel(client, USED);
        Label usedLabel = this.createValueNumberLabel(client);
        this.poolUsedLabels.add(usedLabel);        
        
        this.createNameLabel(client, COMMITTED);
        Label commitedLabel = this.createValueNumberLabel(client);
        this.poolCommittedLabels.add(commitedLabel);
        
        this.createNameLabel(client, MAXIMUM);
        Label maxumimLabel = this.createValueNumberLabel(client);
        this.poolMaxLabels.add(maxumimLabel);
        
        section.setClient(client);
        section.setExpanded(false);
    }
    
    private Composite createSectionClient(Composite parent, boolean composed) {
        Composite client = this.toolKit.createComposite(parent, NONE);
        client.setLayout(this.createClientLayout(composed));
        return client;
    }

    private void createNameLabel(Composite parent, String text) {
        Label label = this.toolKit.createLabel(parent, text, NONE);
        label.setLayoutData(this.getLeftLayoutData());
    }
    
    private Object getLeftLayoutData() {
        return new GridData(HORIZONTAL_ALIGN_END | VERTICAL_ALIGN_BEGINNING);
    }

    private Label createValueTextLabel(Composite parent) {
        Label label = this.toolKit.createLabel(parent, "", WRAP);        
        label.setLayoutData(this.getRightTextLayoutData());
        return label;
    }
    
    private Object getRightTextLayoutData() {
        GridData data = new GridData(HORIZONTAL_ALIGN_BEGINNING | VERTICAL_ALIGN_BEGINNING);
        data.widthHint = 150;
        return data;
    }
    
    private Object getButtonLayoutData() {
        GridData data = new GridData();
        data.horizontalAlignment = BEGINNING;
        data.horizontalSpan = 2;
        data.horizontalIndent = 24;
        return data;
    }
    
    private Label createValueNumberLabel(Composite parent) {
        Label valueLabel = this.toolKit.createLabel(parent, "", NONE);
        return valueLabel;
    }
    
    private Object createSubSectionLayoutData() {
        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalSpan = 2;
        
        return data;
    }
    
    private Section createSectionNamed(String sectionName) {
        int style = TWISTIE | EXPANDED | CLIENT_INDENT;
        Section section = this.toolKit.createSection(this.summaryForm.getBody(), style);
        section.setText(sectionName);
        this.toolKit.createCompositeSeparator(section);
        
        return section;
    }
    
    private Section createSubSectionNamed(String sectionName, Composite parent) {        
        int style = TREE_NODE | EXPANDED | CLIENT_INDENT;        
        Section section = this.toolKit.createSection(parent, style);
        section.setText(sectionName);
        
        return section;
    }

    private Layout createClientLayout(boolean composed) {
        if (composed) {
            GridLayout layout = new GridLayout();
            layout.numColumns = 2;
            return layout;
        }
        return new KeyValueLayout();        
    }

    /**
     * This method is called on each refresh. 
     * @see org.gudy.azureus2.ui.swt.views.IView#refresh()
     */
    private void refresh() {
        if (this.initialized) { //refresh only if all components are initialized
            this.refreshMemorySection();        
            this.refreshThreadSection();        
            this.refreshCompilerSection();        
            this.refreshRuntimeSection();        
            this.refreshClassSection();        
            this.refreshGarbageCollectionSection();        
            this.refreshMemoryPoolSection();
            this.refreshOSSection();
        }
    }

    private void refreshMemoryPoolSection() {
        ListIterator<MemoryPoolMXBean> beanIterator = this.poolBeans.listIterator();
        Label initialLabel, usedLabel, committedLabel, maxLabel;
        while (beanIterator.hasNext()) {
            initialLabel = this.poolInitLabels.get(beanIterator.nextIndex());
            usedLabel = this.poolUsedLabels.get(beanIterator.nextIndex());
            committedLabel = this.poolCommittedLabels.get(beanIterator.nextIndex());
            maxLabel = this.poolMaxLabels.get(beanIterator.nextIndex());
            MemoryPoolMXBean nextBean = beanIterator.next();
            MemoryUsage usage = nextBean.getUsage();
            String newText = this.formatBytes(usage.getInit());
            initialLabel.setText(newText);
            newText = this.formatBytes(usage.getUsed());
            this.setAndRealoutIfNeccessary(usedLabel, newText);
            newText = this.formatBytes(usage.getCommitted());
            this.setAndRealoutIfNeccessary(committedLabel, newText);
            newText = this.formatBytes(usage.getMax());
            maxLabel.setText(newText);
        }
    }

    private void refreshGarbageCollectionSection() {
        ListIterator<GarbageCollectorMXBean> beanIterator = this.garbageBeans.listIterator();
        while (beanIterator.hasNext()) {
            Label nextCollectionCountLabel = this.collectionCountLabels.get(beanIterator.nextIndex());
            Label nextCollectionTimeLabel = this.collectionTimeLabels.get(beanIterator.nextIndex());
            GarbageCollectorMXBean nextBean = beanIterator.next();
            nextCollectionCountLabel.setText(Long.toString(nextBean.getCollectionCount()));
            nextCollectionTimeLabel.setText(this.formatMilliseconds(nextBean.getCollectionTime(), true));
        }
    }

    private void refreshClassSection() {
        String newText = this.formatInt(this.classBean.getLoadedClassCount());
        this.setAndRealoutIfNeccessary(this.classesCurrentlyLoadedLabel, newText);
        
        newText = this.formatLong(this.classBean.getUnloadedClassCount());
        this.setAndRealoutIfNeccessary(this.classesUnloadedLabel, newText);
        
        newText = this.formatLong(this.classBean.getTotalLoadedClassCount());
        this.setAndRealoutIfNeccessary(this.classesTotalLoaded, newText);
    }

    private void refreshRuntimeSection() {
        long uptime = this.runtimeBean.getUptime();
        String uptimeText = this.formatMilliseconds(uptime, false);
        this.uptimeLabel.setText(uptimeText);
    }    

    private void refreshCompilerSection() {        
        if (this.compilationBean == null) {
            this.compilerNameLabel.setText(NO_COMPILER);
            this.compileTimeLabel.setText(UNSUPPORTED_COMPILETIME);
        } else {
            this.compilerNameLabel.setText(this.compilationBean.getName());
            if (this.compilationBean.isCompilationTimeMonitoringSupported()) {
                long compilationTime = this.compilationBean.getTotalCompilationTime();
                String text = this.formatMilliseconds(compilationTime, true);
                this.compileTimeLabel.setText(text);
            } else {
                this.compileTimeLabel.setText(UNSUPPORTED_COMPILETIME);
            }
        }
    }

    private void refreshMemorySection() {
        MemoryUsage heapUsage = this.memoryBean.getHeapMemoryUsage();
        String newText = formatBytes(heapUsage.getInit());
        this.heapInitialLabel.setText(newText);
        newText = formatBytes(heapUsage.getUsed());
        this.setAndRealoutIfNeccessary(this.heapUsedLabel, newText);
        newText = formatBytes(heapUsage.getCommitted());
        this.setAndRealoutIfNeccessary(this.heapCommittedLabel, newText);
        newText = formatBytes(heapUsage.getMax());
        this.heapMaximumLabel.setText(newText);
        
        MemoryUsage nonHeapUsage = this.memoryBean.getNonHeapMemoryUsage();
        newText = formatBytes(nonHeapUsage.getInit());
        this.nonHeapInitialLabel.setText(newText);
        newText = formatBytes(nonHeapUsage.getUsed());
        this.setAndRealoutIfNeccessary(this.nonHeapUsedLabel, newText);
        newText = formatBytes(nonHeapUsage.getCommitted());
        this.setAndRealoutIfNeccessary(this.nonHeapCommittedLabel, newText);
        newText = formatBytes(nonHeapUsage.getMax());
        this.nonHeapMaximumLabel.setText(newText);
        
        int pending = this.memoryBean.getObjectPendingFinalizationCount();
        this.pendingFinalizationLabel.setText(this.formatInt(pending));
    }

    private void refreshThreadSection() {
        int live = this.threadBean.getThreadCount();
        String newText = this.formatLong(live);
        this.setAndRealoutIfNeccessary(this.liveThreadsLabel, newText);
        
        int daemon = this.threadBean.getDaemonThreadCount();
        newText = this.formatLong(daemon);
        this.setAndRealoutIfNeccessary(this.daemonThreadsLabel, newText);
        
        int nonDeamon = live - daemon;
        newText = this.formatLong(nonDeamon);
        this.setAndRealoutIfNeccessary(this.nonDeamonThreadsLabel, newText);
        
        long totalStarted = this.threadBean.getTotalStartedThreadCount();
        newText = this.formatLong(totalStarted);
        this.setAndRealoutIfNeccessary(this.totalThreadsStartedLabel, newText);
    }
    
    private void setAndRealoutIfNeccessary(Label label, String newText) {
        String oldText = label.getText();
        label.setText(newText);
        if (this.needsRelayot(oldText, newText)) {
            this.relayoutParent(label);
        }
    }
    
    private boolean needsRelayot(String oldText, String newText) {
        return oldText.length() != 0 
                && newText.length() != oldText.length();
    }
    
    private void relayoutParent(Control control) {
        control.getParent().layout(true);
    }
    
    private String formatBytes(long bytes) {
        if (bytes == -1) {
            return BYTES_UNSUPPORTED;
        }
        Formatters formaters = this.pluginInterface.getUtilities().getFormatters();
        return formaters.formatByteCountToKiBEtc(bytes);
    }
    
    private String formatLong(long l) {
        return String.format("%,d", l);
    }
    
    private String formatInt(int i) {
        return String.format("%,d", i);
    }
    
    private String formatMilliseconds(long time, boolean includeMillis) {
        long millis = time % 1000;
        long seconds = (time / 1000) % 60;
        long minutes = (time / (60 * 1000)) % 60;
        long hours = (time / (60 * 60 * 1000)) % 24;
        long days = (time / (24 * 60 * 60 * 1000));
        
        StringBuilder buffer = new StringBuilder();
        
        this.appendValue(days, "day", buffer);
        this.appendValue(hours, "hour", buffer);
        this.appendValue(minutes, "minute", buffer);
        
        if (includeMillis) {
            this.spaceIfNeccessary(buffer);
            buffer.append(seconds);
            buffer.append('.');
            buffer.append(String.format("%03d", millis));
            buffer.append(" seconds");
        } else {
            this.appendValue(seconds, "second", buffer);
        }
        
        return buffer.toString();        
//        Formatters formatter = this.pluginInterface.getUtilities().getFormatters();
//        return formatter.formatTimeFromSeconds(time / 1000);
    }
    
    private void appendValue(long value, String text, StringBuilder buffer) {
        if (value > 0) {
            this.spaceIfNeccessary(buffer);
            if (value == 1) {
                buffer.append("1 ");
                buffer.append(text);
            } else {
                buffer.append(value);                
                buffer.append(' ');
                buffer.append(text);
                buffer.append('s');
            }
        }
    }
    
    private void spaceIfNeccessary(StringBuilder buffer) {
        if (buffer.length() > 0) {
            buffer.append(' ');
        }
    }
    
    /**
     * Set the button's UI.
     * @author <a href="mailto:daveo@asc-iseries.com">David J. Orme </a>
     * @param caption - The button's text property.  This is required.
     * @param icon - The buttons icon or null if there is none.
     */
    public static void setButtonUI(Button button, String caption, Image icon) {
        if (icon != null) {
            /*
             * FEATURE IN SWT:  Button can't display both an image and a 
             * text caption at the same time.  The workaround is to make 
             * your own image containing the icon and the caption and use
             * that instead.
             */
            
            // We still have to set the text in order for accelerator keys to
            // work correctly.  The text just won't be displayed once the 
            // image is set.
            button.setText(caption);

            // Figure out how big everything has to be
            Rectangle iconSize = icon.getBounds();
            
            GC gc = new GC(button);
            Point captionSize = gc.textExtent(caption, DRAW_MNEMONIC);

            Rectangle iconTotalSize = icon.getBounds();
            iconTotalSize.width += 4;
            iconTotalSize.width += captionSize.x;
            gc.dispose();
            
            // Draw the icon
            Image image = new Image(Display.getDefault(), iconTotalSize.width, 
                                    iconTotalSize.height);
            gc = new GC(image);
            gc.setBackground(Display.getDefault().getSystemColor(COLOR_WHITE));
            gc.setForeground(button.getForeground());
            gc.fillRectangle(iconTotalSize);
            if (iconSize.height > captionSize.y) {
                gc.drawImage(icon, 0, 0);
                gc.setFont(button.getFont());
                gc.drawText(caption, iconSize.width+2, 
                   iconSize.height-captionSize.y-(iconSize.height-captionSize.y)/2,
                   DRAW_MNEMONIC | DRAW_TRANSPARENT);
            } else {
                gc.drawImage(icon, 0, 
                   captionSize.y-iconSize.height-(captionSize.y-iconSize.height)/2);
                gc.setFont(button.getFont());
                gc.drawText(caption, iconSize.width+2, 0, 
                   DRAW_MNEMONIC | DRAW_TRANSPARENT);
            }
            gc.dispose();
            
            // Draw the transparancy mask
            ImageData iconTransparancy = icon.getImageData().getTransparencyMask();
            Image iconTransparancyMask = new Image(Display.getDefault(), 
                iconTransparancy);
            PaletteData palette = new PaletteData (
                new RGB [] {
                    new RGB (0, 0, 0),          // transparant pixels are white
                    new RGB (0xFF, 0xFF, 0xFF), // opaque pixels are black
                });
            ImageData maskData = new ImageData (iconTotalSize.width, 
                iconTotalSize.height, 1, palette);
            Image mask = new Image (Display.getDefault(), maskData);
            gc = new GC (mask);
            gc.setBackground(Display.getDefault().getSystemColor(COLOR_BLACK));
            gc.fillRectangle(0, 0, iconTotalSize.width, iconTotalSize.height);
            if (iconSize.height > captionSize.y) {
                gc.setBackground( 
                    Display.getDefault().getSystemColor(COLOR_WHITE));
                gc.drawImage(iconTransparancyMask, 0, 0);
                gc.setForeground(
                    Display.getDefault().getSystemColor(COLOR_WHITE));
                gc.drawText(caption, iconSize.width+2, 
                    iconSize.height-captionSize.y-(iconSize.height-captionSize.y)/2,
                    DRAW_MNEMONIC | DRAW_TRANSPARENT);
            } else {
                gc.setBackground(
                    Display.getDefault().getSystemColor(COLOR_WHITE));
                gc.drawImage(iconTransparancyMask, 0, 
                   captionSize.y-iconSize.height-(captionSize.y-iconSize.height)/2);
                gc.setForeground(
                    Display.getDefault().getSystemColor(COLOR_WHITE));
                gc.drawText(caption, iconSize.width+2, 0, 
                    DRAW_MNEMONIC | DRAW_TRANSPARENT);
            }
            gc.dispose();

            // Get the data for the image and mask so we can compose them into
            // the final icon...
            maskData = mask.getImageData ();
            mask.dispose();

            ImageData imageData = image.getImageData();
            image.dispose();
            
            // Make the final image (including transparancy)
            final Image iconPlusText = new Image(Display.getDefault(), 
                imageData, maskData);
            
            button.setImage(iconPlusText);
            button.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent e) {
                    iconPlusText.dispose();                    
                }                
            });
        } else {
            button.setText(caption);
        }
    }
    
    private void expandAndAddListener(Section section, final String key) {
        this.expandAndAddListener(section, key, true);
    }
    
    private void expandAndAddListener(Section section, final String key, boolean defaultValue) {
        boolean expanded = this.config.getExpand(key, defaultValue);
        section.setExpanded(expanded);
        section.addExpansionListener(new ExpansionAdapter() {
            public void expansionStateChanged(ExpansionEvent e) {
                AMPView.this.config.setExpand(key, e.getState());                
            }
        });
    }
    
    private String getSWTVersion() {
        int version = SWT.getVersion();
        char[] chars = new char[5];
        
        chars[0] = Integer.toString(version % 10000).charAt(0);
        chars[1] ='.';
        String minor = Integer.toString(version % 1000);
        for (int i = 0; i < 3; ++i) {
            if (minor.length() > i) {
                chars[4 - i] = minor.charAt(minor.length() - 1 - i);
            } else {
                chars[4 - i] = '0';
            }
        }
        return new String(chars);
    }
    
    boolean isCreated = false;
    public boolean eventOccurred(UISWTViewEvent event) {
      switch (event.getType()) {
        case UISWTViewEvent.TYPE_CREATE:
          if (isCreated) // Comment this out if you want to allow multiple views!
            return false;

          isCreated = true;
          break;

        case UISWTViewEvent.TYPE_DESTROY:
          delete(); // Remove if not defined
          isCreated = false;
          break;

        case UISWTViewEvent.TYPE_INITIALIZE:
          initialize((Composite)event.getData());
          break;

        case UISWTViewEvent.TYPE_REFRESH:
          refresh(); // Remove if not defined
          break;
      }

      return true;
    }
    
}
