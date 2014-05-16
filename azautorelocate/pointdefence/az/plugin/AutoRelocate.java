package pointdefence.az.plugin;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.DirectoryParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;

public class AutoRelocate implements Plugin {

    private boolean checkCommonRoot;
    private boolean running = true;
    private boolean scan = false;
    private int scanPeriod = 1;

    private void addScanLocation(File f) {
        scanLocations.add(f);
    }

    private void addScanLocation(String newLocation) {
        scanLocations.add(new File(newLocation));
    }

    private void buildScanLocations(String value) {
        if (!value.endsWith("]") || !value.startsWith("[")) {
            return;
        }

        for (String s : value.substring(1, value.length() - 1).split(",")) {
            addScanLocation(s.trim());
        }

        System.out.println("using scan locations " + scanLocations.toString());
    }

    private void buildDontScanLocations(String value) {
        dontScanRegexp.addAll(Arrays.asList(value.split(" ")));
        System.out.println("not scan regexp " + dontScanRegexp.toString());

    }

    private String[] getScanLocations() {
        List<String> results = new ArrayList<String>();
        for (File f : scanLocations) {
            results.add(f.toString());
        }
        return (String[]) results.toArray(new String[results.size()]);
    }

    public void initialize(final PluginInterface pi) {

        System.out.println("Starting plugin " + pi.getPluginState().isOperational() + " " + pi.isInitialisationThread());

        BasicPluginConfigModel config_page = pi.getUIManager().createBasicPluginConfigModel("Auto Relocate Data");
        config_page.addLabelParameter2("autorelocate.config.title");

        final DirectoryParameter sp = config_page.addDirectoryParameter2("autorelocate.base.label", "autorelocate.base", "/store/media/movies/");
        final StringParameter sp2 = config_page.addStringParameter2("autorelocate.base.locations.label", "autorelocate.base.locations", "");
        final StringParameter sp3 = config_page.addStringParameter2("autorelocate.base.dontscanregex.label", "autorelocate.base.dontscanregex", "^\\..*");

        final IntParameter sp4 = config_page.addIntParameter2("autorelocate.base.scanperiod.label", "autorelocate.base.scanperiod", 1);

        ActionParameter add_button = config_page.addActionParameter2("autorelocate.add.label", "autorelocate.add.text");
        ActionParameter delete_button = config_page.addActionParameter2("autorelocate.delete.labels", "autorelocate.delete.text");

        buildScanLocations(sp2.getValue());
        buildDontScanLocations(sp3.getValue());

        scanPeriod = sp4.getValue();

        sp.addListener(new ParameterListener() {

            public void parameterChanged(Parameter newLocation) {
            }
        });
        sp3.addListener(new ParameterListener() {

            public void parameterChanged(Parameter newLocation) {
                buildDontScanLocations(sp3.getValue());
            }
        });
        sp4.addListener(new ParameterListener() {

            public void parameterChanged(Parameter newLocation) {
                scanPeriod = sp4.getValue();
            }
        });

        add_button.addListener(new ParameterListener() {

            public void parameterChanged(Parameter newLocation) {
                String value = sp.getValue();

                File f = new File(value);

                if (!f.isDirectory()) {
                    f = f.getParentFile();
                }


                if (f.exists()) {
                    addScanLocation(f);
                }

                sp2.setValue(scanLocations.toString());
            }
        });

        delete_button.addListener(new ParameterListener() {

            public void parameterChanged(Parameter arg0) {
                scanLocations.clear();
                sp2.setValue(scanLocations.toString());
            }
        });

        Runnable mainTask = new Runnable() {

            public void run() {
                int dupCount=0;

                scan = true; //hey I'm pessimistic
                try {

                    LoggerChannel log = pi.getLogger().getChannel(AutoRelocate.class.getName());

                    for (Download d : pi.getDownloadManager().getDownloads(false)) {
                        Thread.yield();
                        boolean wasChanged = false;
                        //d.getTorrent().getHash();
                        System.out.println("Checking download " + d.getName());

                        for (DiskManagerFileInfo dmfi : d.getDiskManagerFileInfo()) {
                            Thread.yield();
                            //       RELOOP:
                            Thread.yield();
                            File fff = dmfi.getLink();

                            if (null == fff) {
                                fff = dmfi.getFile();
                            }

                            if (!fff.exists()) {

                            	DiskManagerFileInfo dmfiProxy = proxyFor(dmfi);
                                //dont add file already found
                                if (foundFiles.containsKey(dmfiProxy) || missingFiles.contains(dmfiProxy)) {
                                    System.out.println(dmfi.getFile().toURI() + " " + dmfi.getLength() + " not found - not added");
                                    dupCount ++;
                                } else {
                                    System.out.println(dmfi.getFile().toURI() + " " + dmfi.getLength() + " not found - added " + dmfi.hashCode());
                                    missingFiles.add(dmfiProxy);
                                }
                            }
                        }
                    }
                    if(dupCount == missingFiles.size()){
                        System.out.println("nothingNew scan aborted");
                        scan = false;
                    }
                } catch (Exception ex) {
                    System.out.println("eeek " + ex.toString());
                    ex.printStackTrace();

                }


            }
        };

        Runnable locateTask = new Runnable() {

            public void run() {
                if (scan) {
                    System.out.println("locating missing files " + missingFiles.size());
                    File newFile = locateNewFile();
                    System.out.println("waiting for next scan");
                }

            }
        };

        Runnable updateTask = new Runnable() {

            public void run() {
                System.out.println("updating found files " + foundFiles.size());

                List<Download> downloads = new ArrayList<Download>();

                for (DiskManagerFileInfo dmfi : foundFiles.keySet()) {
                    try {
                        downloads.add(dmfi.getDownload());
                    } catch (DownloadException ex) {
                        ex.printStackTrace();
                    }
                }

                for (Download d : downloads) {
                    if (d.getState() != d.ST_STOPPING || d.getState() != d.ST_STOPPED) {
                        try {
                            d.stop();
                        } catch (DownloadException ex) {
                        }
                    }
                }

                for (DiskManagerFileInfo dmfi : foundFiles.keySet()) {
                    try {
                        boolean wasChanged = false;
                        File newFile = foundFiles.get(dmfi);
                        Download d = dmfi.getDownload();
                        if (!downloads.contains(d)) {
                            downloads.add(d);
                        }

                        if (newFile != null) {
                            System.out.println("Found file " + newFile.toURI());
                            File f = new File(dmfi.getDownload().getSavePath());
                            File ff = newFile;
                            while ((ff = ff.getParentFile()) != null) {
                                if (ff.getName().equals(f.getName())) {
                                    if (!scanLocations.contains(ff)) {
                                        System.out.println("new base " + ff.toURI());
                                        scanLocations.add(ff);
                                    }
                                }
                            }
                            dmfi.setLink(newFile);

                            synchronized (missingFiles) {
                                missingFiles.remove(proxyFor(dmfi));
                            }

                            wasChanged = true;
                        } else {
                            System.out.println("could not locate file");
                        }

                        System.out.println("torrent complete " + !wasChanged);
                        if (checkCommonRoot) {
                            wasChanged = checkForCommonRoot(d);
                        }
                    } catch (DownloadException ex) {
                        ex.printStackTrace();
                    }

                }

                for (Download d : downloads) {
                    try {
                        if (!d.isChecking() && (d.getState() == d.ST_ERROR || d.getState() == d.ST_QUEUED || d.getState() == d.ST_STOPPED)) {
                            d.recheckData();
                            System.out.println("forcing recheck on " + d.getName());
                        }
                    } catch (DownloadException ex) {
                        ex.printStackTrace();
                    }

                    for (DiskManagerFileInfo dmfi : d.getDiskManagerFileInfo()) {
                        synchronized (foundFiles) {
                            foundFiles.remove(proxyFor(dmfi));
                            //System.out.println(foundFiles.size());
                        }
                    }
                }

                System.out.println("update complete");
            }

            private boolean checkForCommonRoot(Download d) {
                //check for common root
                List<File[]> links = new ArrayList<File[]>();
                for (DiskManagerFileInfo dmfi : d.getDiskManagerFileInfo()) {
                    Thread.yield();

                    List<File> chain = new ArrayList<File>();
                    File ff = dmfi.getFile();

                    if (ff == null) {
                        System.out.println("Link was null " + d.getName() + " " + dmfi.getFile());
                    }

                    while (ff != null && (ff = ff.getParentFile()) != null && !ff.getName().equals(d.getName())) {
                        Thread.yield();
                        if (chain != null) {
                            chain.add(0, ff);
                            //System.out.println("chain " + chain.toString());
                        }
                    }
                }

                if (links.size() > 0) {
                    System.out.println("looking for root");
                    boolean same = true;

                    int p = 0;
                    File b = null, commonRoot = null;

                    while (same) {
                        Thread.yield();
                        for (File[] c : links) {
                            if (b == null) {
                                b = c[p];
                                continue;
                            }
                            same &= b.equals(c[p]);
                        }

                        if (!same) {
                            break;
                        }

                        commonRoot = b;

                        b = null;
                        p++;

                    }

                    if (commonRoot != null && !commonRoot.equals(new File(d.getSavePath()))) {
                        System.out.println("setting common root to " + commonRoot);
                        try {
                            d.moveDataFiles(commonRoot);
                        } catch (DownloadException ex) {
                            System.out.println("can't set commonRoot");
                        }
                        return true;
                    }
                } else {
                    System.out.println("no common root");
                }
                return false;
            }
        };

        readerExecutorPool = Executors.newScheduledThreadPool(2);

        readerExecutorPool.scheduleAtFixedRate(mainTask, 0, scanPeriod, TimeUnit.MINUTES);
        readerExecutorPool.scheduleAtFixedRate(locateTask, 1, scanPeriod, TimeUnit.MINUTES);
        readerExecutorPool.scheduleAtFixedRate(updateTask, 2, scanPeriod, TimeUnit.MINUTES);
         
        /* TEST CODE
         
         boolean test = true;

        readerExecutorPool.scheduleAtFixedRate(mainTask, 0, 20, TimeUnit.SECONDS);
        readerExecutorPool.scheduleAtFixedRate(locateTask, 5, 20, TimeUnit.SECONDS);
        readerExecutorPool.scheduleAtFixedRate(updateTask, 10, 20, TimeUnit.SECONDS);


        if (test) {
            while (Thread.currentThread().isAlive()) {
                try {
                    Thread.sleep(5000);
                    // Do something here.
                } catch (InterruptedException ex) {
                }
            }
        }
        System.out.println("plugin failed!!!!!!!");

        //readerExecutorPool.shutdownNow();
        System.out.println("plugin EXITED");
         *
         */

    }

    private ScheduledExecutorService readerExecutorPool;
    static List<File> scanLocations = new ArrayList<File>();
    static Set<DiskManagerFileInfo> missingFiles = new HashSet<DiskManagerFileInfo>();
    static Map<DiskManagerFileInfo, File> foundFiles = new HashMap<DiskManagerFileInfo, File>();
    static List<String> dontScanRegexp = new ArrayList<String>();

    //TODO add multiple files
    public static synchronized File locateNewFile() {
        File rf = null;

        for (File scanLocation : scanLocations) {
            System.out.println("--scanning " + scanLocation.toURI());
            //FIXME a new thread for each location
            rf = locateNewFile(scanLocation);
            if (rf != null) {
                return rf;
            }
        }
        return rf;
    }
    static int depth = 0;

    public static File locateNewFile(File root) {
        if (depth % 99 == 0) {
            Thread.yield();
        }
        File result = null;

        if (root.isDirectory()) {
            depth++;

            for (String rx : dontScanRegexp) {
                if (root.getAbsolutePath().matches(rx) || root.getName().matches(rx)) {
                    return null;
                }
            }

            File[] list = root.listFiles();

            System.out.println("-scanning " + root.getAbsolutePath() + " " + root.isDirectory() + " " + (list == null));

            if (list != null) {
                for (File fh : list) {
                    //System.out.println("found" + fh.getName());
                    //System.out.println("-scanning " + fh.getName());
                    if (fh.isDirectory()) {
                        Thread.yield();
                        result = locateNewFile(fh);

                        if (result != null) {
                            depth--;
                            return result;
                        }
                    } else {
                        synchronized (missingFiles) {
                            for (DiskManagerFileInfo ff : missingFiles) {
                                if (fh.getName().equals(ff.getFile().getName()) && fh.length() == ff.getLength()) {
                                    System.out.println("found " + fh.getName() + " " + fh.length());
                                    foundFiles.put(proxyFor(ff), fh);
                                    depth--;
                                }
                            }
                        }

                    }
                }
            }
        }
        depth--;
        return result;
    }

    public static DiskManagerFileInfo proxyFor(final DiskManagerFileInfo target) {
        return (DiskManagerFileInfo) Proxy.newProxyInstance(
                DiskManagerFileInfo.class.getClassLoader(),
                new Class[]{DiskManagerFileInfo.class},
                new EqualsHashCodeProxy(target) {
                    public int generateHashCode() {
                    	String file_name =  target.getFile().getAbsolutePath();
                        int res = file_name.hashCode();
                        //System.out.println( "Generated " + res + " for " + file_name );
                        return( res );
                    }
                });
    }
}
