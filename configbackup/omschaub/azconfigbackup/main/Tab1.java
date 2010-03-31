/*
 * Created on Feb 6, 2005
 * Created by omschaub
 * 
 */
package omschaub.azconfigbackup.main;

import omschaub.azconfigbackup.utilities.ColorUtilities;
import omschaub.azconfigbackup.utilities.DirectoryChanger;
import omschaub.azconfigbackup.utilities.DirectoryUtils;
import omschaub.azconfigbackup.utilities.imagerepository.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;



/**
 * Tab1 Graphic initialization
 */
public class Tab1 {
    
    public static Label backupDirectory;
    static ToolItem changeBackupDir;

    static void open(){
        
        View.sash.setBackground(ColorUtilities.getBackgroundColor());
        //      cvsgroup Layout -- 
		
        subopen();

        //redrawing certain items
        View.composite_for_tab1.layout();
        
   }
    
    /**
     * Subtab 3 on Tab1 open 
     * @param Composite parent
     * @return Composite 
     * 
     */
    
    static void subopen(){

//      Label for auto download -- inside cvsgroup
        
        Composite tb_and_label = new Composite(View.composite_for_tab1, SWT.NULL);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
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
        
        Composite tb_and_label2 = new Composite(View.composite_for_tab1,SWT.NULL);
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
        
    }

    
//EOF
}
