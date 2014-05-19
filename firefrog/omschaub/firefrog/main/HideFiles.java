package omschaub.firefrog.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class HideFiles {
	
	
	
	public static String[] read(){
        String[] hiddenFiles = new String[500];
		
				try{
                    
                    int counter = 0;
                    
                    //open file
    				
    				String plugin_dir = Plugin.getPluginInterface().getPluginDirectoryName()+System.getProperty("file.separator");
    				String plugin_file = "hidden.txt";
                    String inputLine;	
    				
    				File inFile = new File(plugin_dir + plugin_file);
    				
    				
    				if (!inFile.isFile()){
    					inFile.createNewFile() ;
    				}
    			
    				final BufferedReader in = new BufferedReader(new FileReader(inFile));
					
    				//Text Getter
    				while((inputLine = in.readLine()) != null){
    					hiddenFiles[counter] = inputLine;
                        counter++;
    				}
    
    				in.close();

    				return hiddenFiles;	
					
			}catch(Exception e) {
				e.printStackTrace();	
			}
            return hiddenFiles;
	}
	
    
	public static void addFile(final String fileName){
        String[] check = read();
        for(int i = 0; i < check.length ; i++){
            if(check[i] != null && check[i].equalsIgnoreCase(fileName))
                return;
        }
        
        try{	
            String plugin_dir = Plugin.getPluginInterface().getPluginDirectoryName()+System.getProperty("file.separator");
            String plugin_file = "hidden.txt";
              
            
            File inFile = new File(plugin_dir + plugin_file);
            
            
            if (!inFile.isFile()){
                inFile.createNewFile() ;
            }
    		
    		
    		BufferedWriter bufWriter = new BufferedWriter(new FileWriter(inFile,true)); 
    	     
    	    bufWriter.write(fileName); 
            bufWriter.newLine();
    	    bufWriter.close(); 
    		
    		
    	}catch(Exception e) {
    		e.printStackTrace();	
    	}
}
	
	
    public static int getCount(){
        String[] check = read();
        int counter = 0;
        for(int i = 0; i < check.length; i++){
            if(check[i] != null)
                counter++;
        }
        
        return counter;
    }
    
    
public static void removeFile(String fileName){
    String[] check = read();
    
    try{
        String plugin_dir = Plugin.getPluginInterface().getPluginDirectoryName()+System.getProperty("file.separator");
        String plugin_file = "hidden.txt";
        File toDie = new File(plugin_dir + plugin_file);
        toDie.delete();
    
    }catch(Exception e){
        e.printStackTrace();
    }
    
    for(int i = 0; i < check.length ; i++){
        if(check[i] != null && !check[i].equalsIgnoreCase(fileName)){
            addFile(check[i]);
        }
            
            
                
    }

}
	
	
	
	
	
	
}//EOF
