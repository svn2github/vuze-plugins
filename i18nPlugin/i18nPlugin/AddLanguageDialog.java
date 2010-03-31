/*
 * AddLanguageDialog.java
 *
 * Created on February 23, 2004, 4:09 PM
 */

package i18nPlugin;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/** Prompts the user for a Language to add to the editor.  If successful,
 * localSelected will not be null.
 *
 * @author  TuxPaper
 */
public class AddLanguageDialog {
  ArrayList alFoundLocales;
  org.eclipse.swt.widgets.List list;
  private String sDefaultPath;

  /** Which Locale the user selected to add.  Null if user chose to cancel 
   */
  public Locale[] localesSelected = null;
  public URL[] urls = null;
  
  /**
   * Creates a new instance of AddLanguageDialog
   * @param display Where to link the Dialog to
   * @param defaultURLs Load language list from here at open
   * @param _sDefaultPath When user chooses to browse, start at this path
   */
  public AddLanguageDialog(final Display display, URL[] defaultURLs, String _sDefaultPath) {
    GridData gridData;
    urls = defaultURLs;
    sDefaultPath = _sDefaultPath;
    final Shell shell = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    
    shell.setText("Add Language to Edit");
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    shell.setLayout(layout);

    if (sDefaultPath != null) {
      Label label = new Label(shell, SWT.NONE);
      label.setText("Languages Available from:");
      gridData = new GridData();
      gridData.widthHint = 200;
      gridData.horizontalSpan = 2;
      label.setLayoutData(gridData);

      final Label labelDirs = new Label(shell, SWT.NONE);
      gridData = new GridData();
      labelDirs.setLayoutData(gridData);
      String sText = "";
      for (int i = 0; i < urls.length; i++) {
        sText += urls[i].toString() + "\n";
      }
      labelDirs.setText(sText);

      Button browse = new Button(shell, SWT.PUSH);
      browse.setText("Browse..");

      browse.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
          DirectoryDialog dialog = new DirectoryDialog(shell, SWT.APPLICATION_MODAL);
          dialog.setFilterPath(sDefaultPath);
          dialog.setText("Change " + View.BUNDLE_NAME + " Folder");
          dialog.setMessage(" Select a folder with some MessagesBundles_*.properties in it");
          String path = dialog.open();
          if (path != null) {
            sDefaultPath = path;
            File fDir = new File(path);
            try {
              urls = new URL[] { fDir.toURL() };
              buildList();
              labelDirs.setText(path);
            } catch (Exception e) { e.printStackTrace(); }

          }
        }
      });
    } else {
      Label label = new Label(shell, SWT.NONE);
      label.setText("Select a Language to create:");
    }
    
    list = new org.eclipse.swt.widgets.List(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    list.setLayoutData(gridData);
    
    
    if (sDefaultPath != null) {
      buildList();
    } else {
      buildAllList();
    }
    
    Button ok = new Button(shell, SWT.PUSH);
    ok.setText("Ok");
    gridData = new GridData(GridData.VERTICAL_ALIGN_END);
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        int[] indexes = list.getSelectionIndices();
        localesSelected = new Locale[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
        	localesSelected[i] = (Locale)list.getData(String.valueOf(indexes[i]));
				}
        shell.dispose();
      }
    });

    Button cancel = new Button(shell, SWT.PUSH);
    cancel.setText("Cancel");
    gridData = new GridData();
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        shell.dispose();
      }
    });

    shell.pack();
    shell.setSize(shell.getSize().x, 450);
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
  }
  
  private void refreshListWidget() {
    Collections.sort(alFoundLocales, new Comparator() {
      public final int compare (Object a, Object b) {
        return ((Locale)a).getDisplayName((Locale)a).compareToIgnoreCase(((Locale)b).getDisplayName((Locale)b));
      }
    });

    list.removeAll();
    for (int i = 0; i < alFoundLocales.size(); i++) {
      //System.out.println( ((Locale)alFoundLocales.get(i)).getDisplayName() );
      Locale locale = ((Locale)alFoundLocales.get(i));
      String sName;
      if ((locale.getCountry() == "") && (locale.getLanguage() == ""))
        sName = "* Default *";
      else
        sName = locale.getDisplayName() + " [" + locale.getDisplayName(locale) + "]";
      list.add(sName);
      list.setData(String.valueOf(i), locale);
    }
  }

  private void buildAllList() {
    alFoundLocales = new ArrayList();
    String[] languages = Locale.getISOLanguages();

    for (int i = 0; i < languages.length; i++) {
      alFoundLocales.add(new Locale(languages[i]));
    }

    refreshListWidget();
  }

  private void buildList() {
    alFoundLocales = new ArrayList();
    for (int i = 0; i < urls.length; i++) {
      String[] bundles = null;
      String urlString = urls[i].toString();
      urlString = urlString.replaceAll(" ", "%20" );

      if (urlString.startsWith("jar:file:")) {

        if ( !urlString.startsWith("jar:file:/")){
          urlString = "jar:file:/".concat(urlString.substring(9));
        }
        try {
          // you can see that the '!' must be present and that we can safely use the last occurrence of it

          int posPling = urlString.lastIndexOf('!');

          String jarName = urlString.substring(4, posPling);
          URI uri = URI.create(jarName);
          File jar = new File(uri);
          JarFile jarFile = new JarFile(jar);
          Enumeration entries = jarFile.entries();
          ArrayList list = new ArrayList(250);
          while (entries.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) entries.nextElement();
            if (jarEntry.getName().startsWith(View.BUNDLE_FOLDER + "/" + View.BUNDLE_NAME) && 
                jarEntry.getName().endsWith(View.BUNDLE_EXT) && 
                jarEntry.getName().length() < 
                  View.BUNDLE_FOLDER.length() + View.BUNDLE_NAME.length() + 2 +  View.BUNDLE_EXT.length() + 7) {
              list.add(jarEntry.getName().substring(View.BUNDLE_FOLDER.length() + 1));
            }
          }
          bundles = (String[]) list.toArray(new String[list.size()]);
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        File bundleDirectory = new File(URI.create(urlString));

        bundles = bundleDirectory.list(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.startsWith(View.BUNDLE_NAME) && name.endsWith(View.BUNDLE_EXT);
          }
        });
      }

      for (int j = 0; j < bundles.length; j++) {
        String locale = bundles[j].substring(View.BUNDLE_NAME.length(), 
                                             bundles[j].length() - View.BUNDLE_EXT.length());
        // _xx_XX
        if (locale.length() >= 6) {
          alFoundLocales.add(new Locale(locale.substring(1, 3), locale.substring(4, 6)));
        } else if (locale.length() >= 3) {
          alFoundLocales.add(new Locale(locale.substring(1, 3)));
        } else {
          alFoundLocales.add(new Locale(""));
        }
      }
    }

    refreshListWidget();
  }
}
