package omschaub.azcvsupdater.main;

import omschaub.azcvsupdater.utilities.imagerepository.ImageRepository;

import org.eclipse.swt.widgets.TableItem;

public class BackupUserTableItemAdapter {
	
	String fileName;
    long lastModified;
	
	/**
	 * @param String fileName for the TableItem object.
	 * @param long lastModified to format time.
	 */
	BackupUserTableItemAdapter (String fileName, long lastModified) {
		this.fileName = fileName;
		this.lastModified = lastModified; 
	}
	
	/**
	 * This function will fill the TableItem
	 * with the prior provided info.
	 * 
	 * @param item TableItem that will be filled
	 * @return the filled TableItem
	 */
	TableItem getTableItem (TableItem item) {
        item.setImage(0,ImageRepository.getImage("folder"));
        item.setText(1,fileName);
        String date = View.formatDate(lastModified);
        item.setText(2,date);
		return item;
	}
}
