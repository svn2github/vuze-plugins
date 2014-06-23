
/**********************************************************************

  Class name   [ManejadorFichero.java]

  Abstract     [In charge of the necessary operations to alter the file that
   			    keeps the user ID]

  Description  [Enables the main class to check whether a user has already been
  				assigned an ID, lets it write/alter/delete the file]

*************************************************************************************/

package torrent_guard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

public class file_handler{

    private static int[] ndigitos; // Stores the integer values of the ID 
	protected static String ident=""; // Stores the ID in a String 
	
	// Variables to store the file path where the ID will be stored
	// The whole file path is stored in the variable 'ruta_completa':
    private static String nombre = System.getProperty("user.home"); // By default user's file path
    private static String nombre_fichero = "id_usuario.txt"; // File name
    private static String ruta_completa = nombre + "/" + nombre_fichero; // Whole filepath    
    
    /* comprobacion_fichero_cliente Method ************************************************
    Description     [Checks the existence of the user's ID file]
    Parameters  	[-]  
    Returns	        [True if the file does exist or False if it does not]
    ***************************************************************************************/
    public static boolean comprobacion_fichero_cliente(){
    	File archivo = null;
    	FileReader fr = null;
    	BufferedReader br = null;
    	String linea = "";
    	boolean cliente = true;
    	try { 
    	    // File is opened:
    		archivo = new File(ruta_completa);
    		// If the file does exist the ID is read and the variable 'cliente' is set to true. 
    		// If not 'cliente' is set to false:
    		if(archivo.exists()){
    	    	fr = new FileReader (archivo);
    	    	br = new BufferedReader(fr);

    	    	if((linea=br.readLine())!=null){ // There is a client ID stored in the file
    	    		ident = linea;
    	    		cliente = true;
    	    	}
    	    }else{
    	    	cliente = false;
    	    }
    	 }catch(Exception e){
    	        System.out.println("Error en la lectura del identificador de usuario del fichero");
    	 }finally{ 
    		 // Cierre del fichero en el bloque 'finally' para asegurar que se cierra el fichero 
    		 // haya o no errores en su lectura:
    	     try{
    	       if( null != fr ){         
    	    	   fr.close();  
    	       }
    	     }catch (Exception e2){
    	        System.out.println("Error en el cierre del fichero");
    	     }
    	  }
    	return cliente;
    }   
    
    /* generar_id Method*******************************************************
    Description     [Generates a 20 random numbers sequence and stores them
     				in the String attribute 'ident']
    Parameters  	[-]  
    ****************************************************************************/
    public static void generar_id(){
    	ndigitos = new int[20];
    	Random rnd = new Random();
    	for (int i = 0; i < 20; i++) {
    		ndigitos[i]= (int)(rnd.nextDouble() * 10.0);
    	}
    	if (ndigitos.length > 0) {
    		ident = Integer.toString(ndigitos[0]);
    		for (int i=1; i<ndigitos.length; i++) {
    			ident = ident + Integer.toString(ndigitos[i]);
    		}
    	}
    }
    
    /* escribe_fichero Method*******************************************************
    Description     [Writes in the file the ID of client]
    Parameters  	[ID of the users received as a parameter]  
    ********************************************************************************/
    public static void escribe_fichero(String ident){
    	FileWriter fichero = null;
        PrintWriter pw = null;
        try{
        	fichero = new FileWriter(ruta_completa,true);
            pw = new PrintWriter(fichero);
            // The ID is written in the file:
            pw.println(ident);
         }
        catch (Exception e) {
            System.out.println("Problema al escribir en fichero");
        }finally {
        	try {
        		if (null != fichero)
        			// File is closed
        			fichero.close();   
        	   	} catch (Exception e2) {
        	   		System.out.println("Problema al cerrar el fichero");
                }
            }
    }
   
   
}