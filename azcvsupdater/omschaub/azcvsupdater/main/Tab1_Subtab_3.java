/*
 * Created on Oct 19, 2005
 * Created by omschaub
 * 
 */
package omschaub.azcvsupdater.main;

import omschaub.azcvsupdater.utilities.DirectoryChanger;
import omschaub.azcvsupdater.utilities.DirectoryUtils;
import omschaub.azcvsupdater.utilities.imagerepository.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class Tab1_Subtab_3 {

    
    public static Label backupDirectory;
    static ToolItem changeBackupDir;
    
    /**
     * Subtab 3 on Tab1 open 
     * @param Composite parent
     * @return Composite 
     * 
     */
    
    static Composite open(Composite parent){
//      new sub composite 2
        Composite mini_comp2 = new Composite(parent, SWT.NONE);
        //mini_comp2.setText("Settings");
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.horizontalSpan =1;
        gridData.horizontalAlignment = GridData.FILL;
        mini_comp2.setLayoutData(gridData);
        GridLayout mini_comp2_layout = new GridLayout();
        mini_comp2_layout.marginHeight = 3;
        mini_comp2_layout.verticalSpacing = 0;
        mini_comp2_layout.numColumns = 5;
        mini_comp2_layout.makeColumnsEqualWidth = false;     
        mini_comp2.setLayout(mini_comp2_layout);
        
//      new sub composite for settings
        Composite mini_comp2_settings = new Composite(mini_comp2, SWT.NONE);
        
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =1;
        gridData.horizontalAlignment = GridData.FILL;
        mini_comp2_settings.setLayoutData(gridData);
        GridLayout mini_comp2_settings_layout = new GridLayout();
        mini_comp2_settings_layout.marginHeight = 0;
        mini_comp2_settings_layout.numColumns = 1;
        mini_comp2_settings_layout.makeColumnsEqualWidth = false;     
        mini_comp2_settings.setLayout(mini_comp2_settings_layout);

        
//      Label for auto download -- inside cvsgroup
        
        Composite autoDownloadComp = new Composite(mini_comp2_settings, SWT.NONE);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =1;
        gridData.horizontalAlignment = GridData.FILL;
        autoDownloadComp.setLayoutData(gridData);
        GridLayout autoDownloadComp_layout = new GridLayout();
        autoDownloadComp_layout.marginHeight = 0;
        autoDownloadComp_layout.marginWidth = 0;
        autoDownloadComp_layout.numColumns = 3;
        autoDownloadComp_layout.makeColumnsEqualWidth = false;
        autoDownloadComp.setLayout(autoDownloadComp_layout);
        
        

        Label autoDownload = new Label (autoDownloadComp,SWT.NULL );
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING );
        gridData.horizontalSpan = 1;
        autoDownload.setLayoutData(gridData);
        autoDownload.setText("Auto Download Latest:");
        //autoDownload.setToolTipText("Automatically download latest CVS version");
        
        final Button autoDownload_Radio = new Button(autoDownloadComp,SWT.CHECK);
        autoDownload_Radio.setSelection(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoDownload"));
        //autoDownload_Radio.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoDownload"))?"On  ":"Off");
        autoDownload_Radio.setToolTipText("Automatically download latest CVS version");
        autoDownload_Radio.pack();

        Label autoDownload_end = new Label (autoDownloadComp,SWT.NULL);
        autoDownload_end.setText("and...");
        
        
        
//Label for auto Restart -- inside cvsgroup
        Composite autoRestartComp = new Composite(mini_comp2_settings, SWT.CHECK);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =1;
        gridData.horizontalAlignment = GridData.FILL;
        autoRestartComp.setLayoutData(gridData);
        GridLayout autoRestartComp_layout = new GridLayout();
        autoRestartComp_layout.marginHeight = 0;
        //autoRestartComp_layout.marginWidth = 0;
        autoRestartComp_layout.numColumns = 4;
        autoRestartComp_layout.makeColumnsEqualWidth = false;
        autoRestartComp.setLayout(autoRestartComp_layout);
        
        Label autoInsert = new Label (autoRestartComp,SWT.NULL );
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING );
        gridData.horizontalSpan = 1;
        autoInsert.setLayoutData(gridData);
        autoInsert.setText ("...Insert:");
        
        
        final Button autoInsert_Check = new Button(autoRestartComp,SWT.CHECK);
        autoInsert_Check.setSelection(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoInsert",false));
        //autoInsert_Check.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoInsert"))?"On  ":"Off");
        autoInsert_Check.pack();
        autoInsert_Check.setToolTipText("After a successful Auto Download, this option will insert the file to be updated the next time Azureus restarts");
        
        
        Label autoRestart = new Label (autoRestartComp,SWT.NULL );
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING );
        gridData.horizontalSpan = 1;
        autoRestart.setLayoutData(gridData);
        autoRestart.setText ("  and Restart Immediately:");
        
        
        final Button autoRestart_Check = new Button(autoRestartComp,SWT.CHECK);
        autoRestart_Check.setSelection(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoRestart",false));
        if(autoRestart_Check.getSelection() && !autoInsert_Check.getSelection()){
            autoInsert_Check.setSelection(true);
            //autoInsert_Check.setText("On");
            View.getPluginInterface().getPluginconfig().setPluginParameter("AutoInsert",true);
            
        }
        //autoRestart_Check.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoRestart"))?"On  ":"Off");
        autoRestart_Check.pack();
        autoRestart_Check.setToolTipText("Instead of waiting until the next restart of Azureus, this option will restart Azureus immediately, inserting the Auto Downloaded CVS version");
        
        final Label autoSeed_label = new Label(autoRestartComp, SWT.NULL);
        autoSeed_label.setText("Force Continual Seeding of Latest Download:");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =3;
        autoSeed_label.setLayoutData(gridData);
        
        final Button autoSeed_Check = new Button(autoRestartComp, SWT.CHECK);
        autoSeed_Check.setSelection(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed",false));
        if(autoSeed_Check.getSelection()){
            autoSeed_Check.setSelection(true);
            //autoSeed_Check.setText("On");
            View.getPluginInterface().getPluginconfig().setPluginParameter("auto_seed", true);
        }else{
            autoSeed_Check.setSelection(false);
            //autoSeed_Check.setText("Off");
            View.getPluginInterface().getPluginconfig().setPluginParameter("auto_seed", false);
        }
        //Listeners for settings
        
        autoDownload_Radio.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                autoDownload_Radio.setSelection((autoDownload_Radio.getSelection())?true:false);
                View.getPluginInterface().getPluginconfig().setPluginParameter("AutoDownload",autoDownload_Radio.getSelection());
                autoDownload_Radio.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoDownload"))?"On  ":"Off");
                if(!autoDownload_Radio.getSelection()){
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoRestart",autoRestart_Check.getSelection());
                    autoRestart_Check.setSelection(false);
                    //autoRestart_Check.setText("Off");
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoRestart",false);
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoInsert",autoInsert_Check.getSelection());
                    autoInsert_Check.setSelection(false);
                    //autoInsert_Check.setText("Off");
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoInsert",false);
                    autoSeed_Check.setSelection(false);
                    //autoSeed_Check.setText("Off");
                    View.getPluginInterface().getPluginconfig().setPluginParameter("auto_seed", false);
                }else{
                    if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoRestart",false)){
                        autoRestart_Check.setSelection(true);
                        //autoRestart_Check.setText("On");
                    }
                    if(View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoInsert",false)){
                        autoInsert_Check.setSelection(true);
                        //autoInsert_Check.setText("On");
                    }        
                }
                View.tab.setFocus();
            }
        });
        
        autoRestart_Check.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                
                if(!autoRestart_Check.getSelection())
                    autoRestart_Check.setSelection(false);
                else{
                    autoRestart_Check.setSelection(true);
                    autoDownload_Radio.setSelection(true);
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoDownload",true);
                   // autoDownload_Radio.setText("On ");
                }
                

                View.getPluginInterface().getPluginconfig().setPluginParameter("AutoRestart",autoRestart_Check.getSelection());
                autoRestart_Check.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoRestart"))?"On  ":"Off");
                if(autoRestart_Check.getSelection()){
                    autoInsert_Check.setSelection(true);
                    //autoInsert_Check.setText("On");
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoInsert",true);
                }
                Tab1.listTable.setFocus();
            }
        });
        
        autoInsert_Check.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                
                if(!autoInsert_Check.getSelection()){
                    autoInsert_Check.setSelection(false);
                    autoRestart_Check.setSelection(false);
                    //autoRestart_Check.setText("Off");
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoRestart",false);
                }                               
                else {
                    autoInsert_Check.setSelection(true);
                    autoDownload_Radio.setSelection(true);
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoDownload",true);
                    //autoDownload_Radio.setText("On ");
                }
                

                View.getPluginInterface().getPluginconfig().setPluginParameter("AutoInsert",autoInsert_Check.getSelection());
                autoInsert_Check.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("AutoInsert"))?"On  ":"Off");
                if(autoInsert_Check.getSelection()){
                    //View.getPluginInterface().getPluginconfig().setPluginParameter("AutoRestart",false);
                    autoRestart_Check.setSelection(false);
                    //autoRestart_Check.setText("Off");
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoRestart",false);
                }
                Tab1.listTable.setFocus();
            }
        });
        
        autoSeed_Check.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                autoSeed_Check.setSelection((autoSeed_Check.getSelection())?true:false);
                View.getPluginInterface().getPluginconfig().setPluginParameter("auto_seed",autoSeed_Check.getSelection());
                //autoSeed_Check.setText((View.getPluginInterface().getPluginconfig().getPluginBooleanParameter("auto_seed"))?"On  ":"Off");
                if(autoSeed_Check.getSelection()){
                    autoDownload_Radio.setSelection(true);
                    View.getPluginInterface().getPluginconfig().setPluginParameter("AutoDownload",true);
                    //autoDownload_Radio.setText("On ");
                }else{
                    
                }
            }
        });
        
        Label vert_label = new Label(mini_comp2, SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = false;
        vert_label.setLayoutData(gridData);
        
        Composite tb_and_label = new Composite(mini_comp2,SWT.NULL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =1;
        gridData.horizontalAlignment = GridData.FILL;
        tb_and_label.setLayoutData(gridData);
        GridLayout tb_and_label_layout = new GridLayout();
        tb_and_label_layout.marginHeight = 0;
        //autoRestartComp_layout.marginWidth = 0;
        tb_and_label_layout.numColumns = 1;
        tb_and_label_layout.makeColumnsEqualWidth = false;
        tb_and_label.setLayout(tb_and_label_layout);
        
        
        Label topLabel = new Label(tb_and_label,SWT.NULL);
        topLabel.setText("Current CVS backup location is:");
        
        backupDirectory = new Label(tb_and_label,SWT.NULL);
        backupDirectory.setText(DirectoryUtils.getBackupDirectory());
        
        
        
        
        Label vert_label2 = new Label(mini_comp2, SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = false;
        vert_label2.setLayoutData(gridData);
        
        Composite tb_and_label2 = new Composite(mini_comp2,SWT.NULL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan =1;
        gridData.horizontalAlignment = GridData.FILL;
        tb_and_label2.setLayoutData(gridData);
        GridLayout tb_and_label_layout2 = new GridLayout();
        tb_and_label_layout2.marginHeight = 0;
        //autoRestartComp_layout.marginWidth = 0;
        tb_and_label_layout2.numColumns = 1;
        tb_and_label_layout2.makeColumnsEqualWidth = false;
        tb_and_label2.setLayout(tb_and_label_layout2);
        
        ToolBar tb_settings = new ToolBar(tb_and_label2,SWT.FLAT);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.horizontalSpan = 1;
        
        tb_settings.setLayoutData(gridData);
        
        
        changeBackupDir = new ToolItem(tb_settings,SWT.PUSH);
        //changeBackupDir.setText("Change Backup Directory");
        changeBackupDir.setImage(ImageRepository.getImage("open_folder"));
        
        changeBackupDir.setToolTipText("Change Directory");
        //changeBackupDir.setToolTipText("Change the backup directory. Current location is:\n" + DirectoryUtils.getBackupDirectory());
        changeBackupDir.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                DirectoryChanger.openChanger();
            
            }
        });
        
        return mini_comp2;
    }
    
    
    
}//EOF
