/*
 * Created on Jun 9, 2004

 */
package omschaub.azcvsupdater.utilities.commentmaker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;


import omschaub.azcvsupdater.utilities.DirectoryUtils;

public class CommentMaker {
	
	public static String commentOpen(String filename, boolean firstline) {
		try {
			File checkdir = new File(DirectoryUtils.getBackupDirectory(), "comments");
			File inFile = new File(checkdir, filename + ".txt");
			if (!inFile.isFile()) {return "";}
			
			String fileText = "", inputLine = "";
			
			// Read contents.
			final BufferedReader in = new BufferedReader(new FileReader(inFile));
			while((inputLine = in.readLine()) != null){
				fileText = fileText + inputLine;
				if (firstline) {break;}
				fileText = fileText + "\n";			
			}
			in.close();
			
			return fileText;
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static void commentWriter(final String fileName, final String fileText){
	try{	
		String backup_dir;
		//String inputLine;	
		backup_dir = DirectoryUtils.getBackupDirectory();
		//System.out.println(fileName);
		File checkdir = new File(backup_dir + System.getProperty("file.separator") + "comments");
		if (!checkdir.isDirectory()) {
				checkdir.mkdir();
			}
		File fileWriter = new File(checkdir + System.getProperty("file.separator") + fileName+".txt");
		if (!fileWriter.isFile()){
			fileWriter.createNewFile() ;
		}
		
		
		
		
		BufferedWriter bufWriter = new BufferedWriter(new FileWriter(fileWriter)); 
	     
	     bufWriter.write(fileText); 
	         
	   // System.out.println(fileText);

	    
	    bufWriter.close(); 
		
		
	}catch(Exception e) {
		e.printStackTrace();	
	}
	}

	public static boolean commentCheck(final String fileName){
        try{
            //open file
          
            String backup_dir;
   
            backup_dir = DirectoryUtils.getBackupDirectory();

            File checkdir = new File(backup_dir + System.getProperty("file.separator") + "comments");
            if (!checkdir.isDirectory()) {
                    checkdir.mkdir();
                }
            File inFile = new File(checkdir + System.getProperty("file.separator") + fileName+".txt");
            if (inFile.isFile()){
                return true;
            }
            
        
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    
	
}//EOF
