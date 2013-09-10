/*
 * Created on May 16, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.update;

import java.io.*;
import java.util.*;
import java.net.*;


public class 
Updater 
{
	private static 		 String MAIN_CLASS		= "org.gudy.azureus2.ui.swt.Main";
	private static final String UPDATER_JAR 	= "Updater.jar";

	public static final String		LOG_FILE_NAME		= "update.log";
	public static final String		UPDATE_PROPERTIES	= "update.properties";
	
	private static String JAVA_EXEC_DIR = System.getProperty("java.home") +
																				System.getProperty("file.separator") +
																				"bin" +
																				System.getProperty("file.separator");
	
	
	protected static String	APPLICATION_NAME 	= "Azureus";
	protected static String	APPLICATION_CMD 	= null;
	
  		// change these and you'll need to change the UpdateInstaller !!!!
  
	protected static final String	UPDATE_DIR 				= "updates";
	protected static final String	ACTIONS_LEGACY_FILE		= "install.act";
	protected static final String	ACTIONS_UTF8_FILE		= "install.act.utf8";
	protected static final String	FAIL_FILE				= "install.fail";
	
	public static void 
	main(
		String[] args) 
	{   
		new Updater().update( args );
	}
	
    private PrintWriter			log_writer = null;
	private volatile boolean	do_restart;
	
	protected void
	update(
		String[]	args )
	{
		if (args.length < 3) {
			System.out.println("Usage: Updater full_classpath full_librarypath full_userpath [config override]");
			System.exit(-1);
		}
    
		if ( args[0].equals( "restart" )){
	    	
			restart( args, true );
				
		}else if ( args[0].equals( "updateonly" )){
			    	
			restart( args, false );
						
		}else{
    	
			System.out.println( "Old restart mechanism no longer supported");
		}
	}
  
  	protected void
  	restart(
		String[]	args,
		boolean		restart_azureus )
	{
  		String command	    	= args[0];
  		String app_path			= args[1];
  		String user_path   		= args[2];
  		String config_override	= args.length>3?args[3]:"";
  		
  		do_restart	= restart_azureus;
  		
  		
  		// 2.1.0.0 shagged restart example:
  		// "C:\Program Files\Java\jdk1.5.0\jre\bin\javaw" 
  		// -classpath "blah" 
  		// -Djava.library.path="C:\Program Files\Azureus" org.gudy.azureus2.update.Updater 
  		// "restart" "C:\Program Files\Azureus\" "C:\Documents and Settings\stuff\Application Data\Azureus" ""
  		// 
  		// trailing \ escapes quote -> app_path has a \ " in it
  		// note, only win32 as the user.dir trailing \ is added by the new azureus.exe launcher
  
  		String	log_dir;
  		boolean	shagged_restart = false;
  		
  		int	shag_pos = app_path.indexOf("\" ");
  		
  		if (  shag_pos != -1 ){
  			
  				// shagged
  			
  			log_dir = app_path.substring(0,shag_pos);
  		
  			shagged_restart = true;
  			
  		}else{
  			
  			log_dir	= user_path;
  		}
  		  		
	    File logFile = new File( log_dir, LOG_FILE_NAME );
  		    
	    if ( !logFile.exists()){
	    	
	    	try{
	    		logFile.createNewFile();
	    		
	    	}catch( IOException e ){
	    		
	    		e.printStackTrace();
	    	}
	    	
	    	if ( !logFile.exists()){
	    		
	    		try{
	    		
	    			logFile = File.createTempFile( "AZU", ".log" );
	    			
	    		}catch( IOException e ){
	    			
	    			e.printStackTrace();
	    			
	    				// last ditch attempt, use file relative to current location
	    			
	    			logFile = new File("update.log");
	    		}
	    	}
	    }
	    
	    Properties	update_properties	= new Properties();
	    
	    ServerSocket server_socket = null;
	    
  		try{
  		    log_writer = new PrintWriter(new FileWriter(logFile, true));

  		    log( "Update Starts: " + getDate());  	

  		    log( "app  dir = " + app_path );
  		    log( "user dir = " + user_path );
  		    log( "config override = " + config_override );
  			
  	  		if ( shagged_restart ){
  	  			
  	  			log( "invalid restart parameters, abandoning restart" );
  	  			
  	  			log( "**** PLEASE MANUALLY INSTALL AZUREUS FROM THE WIN32 .EXE SETUP PACKAGE ****");
  
  	  			do_restart = false;
  	  			
  	  			log_writer.close();
  	  			
  	  			System.exit(1);
  	  			
  	  			return;
  	  			
  	  		}else{
  	  			
  				if ( isWindows ){
  	  				
  		  				// 2.2.0.0 shagged restart. unicode still not working. we end up with a user dir
  		  				// of, say, C:\Documents and Settings\t?stt?st\Application Data\Azureus
  		  				// with the unicode chars replaced with something unspeakable
  		  			
  		  			try{
  		  				File	up_file = new File( user_path );
  		  				
  		  				if ( !up_file.exists()){
  		  				
  		  					log( "user path not found, attempting to fix" );
  		  					
  		  						// see if we can patch things up

  		  					String	user_home = System.getProperty( "user.home" );
  		  					
  		  					File	az_config = findConfig(new File( user_home ));
  		  					
  		  					if ( az_config != null ){
  		  						
  		  						log( "    found config in " + az_config.toString() + ", using parent as user dir" );
  		  						
  		  						user_path	= az_config.getParent();
  		  					}
  		  					
  		  				}
  		  			}catch( Throwable e ){
  		  			
  		  				log( e );
  		  			}
  				}
  	  		}
  	  
  	  		int	instance_port = 6880;
  	  		
  	  		{
  	  				// read the update properties
  	  			
  	  			FileInputStream	fis = null;
  	  			
  	  			try{
  	  			
  	  				File	props_file = new File( user_path, UPDATE_PROPERTIES );
  	  				
  	  				if ( props_file.exists()){
  	  					  					
  	  					update_properties.load( new FileInputStream( props_file ));
  	  					
  	  					Iterator	it = update_properties.keySet().iterator();
  	  					
  	  					log("Loaded 'update.properties'" );
  	  					
  	  					while( it.hasNext()){
  	  						
  	  						String	key = (String)it.next();
  	  						
  	  						log( "    " + key + " = " + update_properties.get(key));
  	  					}
  				
  	  					String	app_name = (String)update_properties.get( "app_name" );
  	  					
  	  					if ( app_name != null && app_name.trim().length() > 0 ){
  	  						
  	  						APPLICATION_NAME	= app_name.trim();
  	  					}
  	  					
  	  					String	app_entry = (String)update_properties.get( "app_entry" );
  	  					
  	  					if ( app_entry != null && app_entry.trim().length() > 0 ){
  	  						
  	  						MAIN_CLASS	= app_entry.trim();
  	  					}
  	  					
  	  					String	app_cmd = (String)update_properties.get( "app_cmd" );
  	  					
  	  					if ( app_cmd != null && app_cmd.trim().length() > 0 ){
  	  						
  	  						APPLICATION_CMD	= app_cmd.trim();
  	  					}

  	  					String	ip = (String)update_properties.get( "instance_port" );
  	  					
  	  					if ( ip != null ){
  	  						
  	  						try{
  	  							instance_port = Integer.parseInt( ip.trim());
  	  							
  	  						}catch( Throwable e ){
  	  							
  	  							log( "Invalid instance port: " + ip );
  	  						}
  	  					}

  	  				}else{
  	  					
  	  		 		    log( "No update.properties found" );
  	  		 		 
  	  				}
  	  			}catch( Throwable e ){
  	  				
  	  				log( "Failed to read update.properties" );
  	  				
  	  				log( e );
  	  				
  	  			}finally{
  	  				if ( fis != null ){
  	  					
  	  					try{
  	  						fis.close();
  	  						
  	  					}catch( Throwable e ){
  	  						
  	  						log( e );
  	  					}
  	  				}
  	  			}
  	  		}
  	  		
  	  		if ( !update_properties.getProperty( "no_wait", "0" ).equals( "1")){
  	  			
  	  			server_socket = waitForAzureusClose( instance_port );
  	  		}
		
  		    File	update_dir = new File( user_path, UPDATE_DIR );
  		    
  		    File[]	inst_dirs = update_dir.listFiles();
  		    
  		    if ( inst_dirs == null ){
  		    	
  		    	inst_dirs = new File[0];
  		    }
  		    
  		    for (int i=0;i<inst_dirs.length;i++){
  		    	
  		    	File	inst_dir = inst_dirs[i];
  		    	
  		    	if ( inst_dir.isDirectory()){
  		    		
  		    		processUpdate( inst_dir );
  		    	}
  		    }
  		    
  		}catch( Throwable e ){
  							
  			log( "Update Fails" );
  				
  			log( e );
  			
  		}finally{
  			
  			if ( do_restart ){
  					  			
	  			log( "Restarting " + APPLICATION_NAME );
	  			
	  			String	vendor = System.getProperty( "java.vm.vendor" );
	  			
	  			Vector	vm_args = new Vector();
	  			
	  				// only for Sun JVM
	  			
	  			if ( vendor != null && ( vendor.toLowerCase().startsWith( "sun" ) || vendor.toLowerCase().startsWith( "oracle" ))){
	  				
		  			String	max_mem = (String)update_properties.get( "max_mem" );
		  			
		  				// for previous versions we default to 128m
		  			
		  			long	max_mem_l = 0;
		  					  				
		  			if ( max_mem == null ){
		  				
		  				max_mem_l = 128*1024*1024;
		  				
		  			}else{
		  				
		  				try{
		  					
		  					max_mem_l = Long.parseLong( max_mem );
		  					
		  				}catch( Throwable e ){
		  					
		  					log( e );
		  				}
		  			}
		  			
		  				// don't allow < 64m to be specified
		  			
		  			if ( max_mem_l < 64*1024*1024 ){
		  				
		  				max_mem_l = 64*1024*1024;
		  			}
		  			
		  			vm_args.add( "-Xmx" + (max_mem_l/(1024*1024)) + "m" );
	  			}
	  			
	  			vm_args.add( "-Duser.dir=\"" + app_path + "\"" );
	  				  			
	  			if ( config_override.length() > 0 ){
	  				
	  				vm_args.add( "-Dazureus.config.path=\"" + config_override + "\"" );
	  			}
	  				
	  			String[]	props = new String[vm_args.size()];
	  		
	  			vm_args.toArray( props );
	  			
	  			if ( server_socket != null ){
	  				
	  				try{  					
	  					server_socket.close();
	  					
	  					server_socket = null;
	  					
	  				}catch( Throwable e ){
	  					
	  					log( "Failed to close server socket" );
	  				}
	  			}
	  			
	            restartAzureus( MAIN_CLASS, props, new String[0] );
	            	 	  			
	 			log( "Restart initiated: " + getDate());
	 			
  			}else{
  						  			
		  		log( "Not restarting " + APPLICATION_NAME + ": " + getDate());
  			}
	 		
  			if ( server_socket != null ){
  				
  				try{  					
  					server_socket.close();
  					
  					server_socket = null;
  					
  				}catch( Throwable e ){
  					
  					log( "Failed to close server socket" );
  				}
  			}
  			
  			if ( log_writer != null ){
	 	  				 	  			
  				log_writer.close();
	 	  	}
  		}
	}
  	
  	protected String
  	getDate()
  	{
		Calendar now = Calendar.getInstance();

		return( now.get(Calendar.YEAR) + "/"
		  		+ ( now.get(Calendar.MONTH) + 1 ) + "/"	// month is 0 based!
		  		+ now.get(Calendar.DAY_OF_MONTH) + " "
		  		+ now.get(Calendar.HOUR_OF_DAY) + ":" 
		  		+ format(now.get(Calendar.MINUTE)) + ":" 
		  		+ format(now.get(Calendar.SECOND)));        
  	}
  	
  	protected File
  	findConfig(
  		File	dir )
  	{
  		File[]	files = dir.listFiles();
  		
  		if ( files != null ){
  			
  			for (int i=0;i<files.length;i++){
  				
  				File	f = files[i];
  				
  				if ( f.isDirectory()){
  					
  					File	res = findConfig( f );
  					
  					if ( res != null ){
  						
  						return( res );
  					}
  				}else{
  					
  					if ( f.getName().equals( "azureus.config" )){
  						
  						return( f );
  					}
  				}
  			}
  		}
  		
  		return( null );
  	}
  	
  	protected void
	processUpdate(
		File		inst_dir )
	{
		log( "processing " + inst_dir.toString());
	
		try{
			
			LineNumberReader	lnr = null;
			
			{
				File	commands = new File( inst_dir, ACTIONS_UTF8_FILE );
				
				if ( commands.exists()){
						
					try{
						lnr = new LineNumberReader( new InputStreamReader( new FileInputStream( commands ), "UTF-8" ));
						
						log( "using utf-8 commands" );
						
					}catch( Throwable e ){
						
						log( "utf8 file failed to open" );
						
						log( e );
					}
				}
			}
			
			if ( lnr == null ){
				
				File	commands = new File( inst_dir, ACTIONS_LEGACY_FILE );
				
				if ( !commands.exists()){
					
					log( "    command file '" + ACTIONS_LEGACY_FILE + "' not found, aborting");
					
					return;
				}
				
				lnr = new LineNumberReader( new FileReader( commands ));
			}
			
			String	failed = null;
			
			while(true){
					
				String	line = lnr.readLine();
					
				if ( line == null ){
						
					break;
				}
					
				log( "    command:" + line );
				
				StringTokenizer tok = new StringTokenizer(line, ",");
				
				String	command = tok.nextToken();
				
				if ( command.equals( "move" )){
					
					File	from_file 	= new File(tok.nextToken());
					File	to_file		= new File(tok.nextToken());
					
						// if something screws up with the update process (e.g. another copy of Azureus starts
						// while an update is in progress) it is possible for the origin files to be removed
						// In this case we don't want to delete the target first. Obviously things could
						// be left in a somewhat inconsistent state (we may have already copied one file of
						// a multi-file update) but making things fully recoverable is beyond my current remit
					
					if ( !from_file.exists()){
						
						failed	= "Update failure: source file '" + from_file + "' has been deleted";
												
						break;
					}
					
					if ( !from_file.canRead()){
						
						failed	= "Update failure: source file '" + from_file + "' is not readable";
												
						break;
					}
					
					if ( to_file.exists()){
						
						deleteDirOrFile( to_file );
					}
					
					if ( !renameFile( from_file, to_file )){
						
							// problem here with updates to azplatform2 updates on Vista that
							// try to overwrite AzureusUpdater.exe. Unfortunately they can't as it
							// is running this update process and is therefore "in use". After much
							// deliberation we decided that the best fix is just to ignore this failure
							// under the assumption that the AzureusUpdater.exe file will never change.
							// If it needs to in the future then we'll have to release an AzureusUpdater2.exe
						
						Set<String>	skip_files = new HashSet<String>();
						
						skip_files.add( "AzureusUpdater.exe".toLowerCase());
						skip_files.add( "msvcr71.dll".toLowerCase());
						
						if ( to_file.exists() && skip_files.contains( from_file.getName().toLowerCase())){
							
							// ignore failure
							
							log( "Update failure of " + from_file.getName() + " ignored as already up-to-date" );
							
						}else{
						
							failed	= "Update failure: failed to rename '" + from_file + "' to '" + to_file + "'";
						}
					}
					
				}else if ( command.equals( "remove" )){
					
					File	file 	= new File(tok.nextToken());
					
					if ( file.exists()){
						
						if ( !deleteDirOrFile( file )){
							
							failed	= "Update failure: failed to delete '" + file + "'";
						}
					}
					
				}else if ( command.equals("chmod")){
					
					String rights = tok.nextToken();
					
					String fileName = tok.nextToken();
					
					chMod(fileName,rights);
					
				}else{
					
					failed = "Update failure: unrecognised command '" + command + "'";
					
				}
			}
			
			lnr.close();
			
			if ( failed == null ){
				
				deleteDirOrFile( inst_dir );
				
			}else{
				
				log( failed );
				
				writeError( inst_dir, failed );
			}
		}catch( Throwable e ){
  			 				
  			log( "processing fails" );
  				
  			log( e );
  			
  			StringWriter writer = new StringWriter();
  			
  			e.printStackTrace( new PrintWriter( writer ));
  			
  			writeError( inst_dir, "Update failure: processing failed - " + writer.getBuffer());
		}
	}

  	private void
  	writeError(
  		File		inst_dir,
  		String		error )
  	{
  		try{
  			File	file = new File( inst_dir, FAIL_FILE );
  			
  			PrintWriter pw = new PrintWriter( file );
  			
  			try{
  				
  				pw.println( error );
  				
  			}finally{
  				
  				pw.close();
  			}
  		}catch( Throwable e ){
  		}
  	}
  	
  	private boolean
	renameFile(
		File		from_file,
		File		to_file )
	{			
   		try{
   			log( "rename " + from_file + " to " + to_file);

   			File	to_dir = to_file.getParentFile();
   			
   			if ( !to_dir.exists()){
   				
   				if ( to_dir.mkdirs()){
   					
   					log( "    created parent directory " + to_dir );
   					
   				}else{
   					
   					log( "    failed to create parent directory " + to_dir );
   				}
   			}
   			
 			Throwable 	last_error = null;
  	  	    	  	      
  			for (int i=0;i<10;i++){
  				
  				if ( i > 0 ){
  					
	 				try{
	  					Thread.sleep(1000);
	  	      		
	  				}catch( InterruptedException e ){
	  				} 
	  	 	      	
	  				log( "   rename of '" + from_file.toString() + "' failed, retrying" );
 				}
  				
  				if ( from_file.renameTo( to_file )){
  					
  		 			log("   direct rename succeeded");
  	      		  	  					
  					return( true );
  					
  				}else{
  					
  		 			log("   direct rename failed.. trying copy+delete");

  		 			// can't rename across file systems under Linux - try copy+delete
  					
					FileInputStream		fis = null;
						
					FileOutputStream	fos = null;

  					try{
  						fis = new FileInputStream( from_file );
  						
  						fos = new FileOutputStream( to_file );

  						byte[]	buffer = new byte[65536];
  						
  						long total = 0;
  						
  						while( true ){
  							
  							int	len = fis.read( buffer );
  							
  							if ( len <= 0 ){
  								
  								break;
  							}
  							total += len;
  							
  							fos.write( buffer, 0, len );
  						}
  						
						log("   wrote " + total + " bytes");
						
  						fos.close();
  						
  						fos	= null;
  						
  						fis.close();
  						
  						fis = null;
  						
  						long from_file_len = from_file.length();
  						long to_file_len = to_file.length();

  						boolean deleted = from_file.delete();
  						
  						if (!deleted) {
  							
  							log("   delete " + from_file + " failed");
  						}
  						
						log("   size: " + from_file_len + " bytes");

  						return( from_file_len == to_file_len );
  						
  					}catch( Throwable e ){
  						
  						last_error	= e;
  						
  					}finally{
  						
  						if ( fis != null ){
  							
  							try{
  								fis.close();
  							}catch( Throwable e ){
  							}
  						}
  						
  						if ( fos != null ){
							try{
  								fos.close();
  							}catch( Throwable e ){
  							}
  						}
  					}
  				}
   			}
  			  				
  			log( "failed to rename '" + from_file.toString() + "' to ' " + to_file.toString() + "'");
  			
  			if ( last_error != null ){
  				
  				log( last_error );
  			}
  			
  	    }catch( Throwable e ){
  	    	
  	    	log( e );
  	    }
  	    
  	    return( false );
  	}
  	
  	private boolean 
  	deleteDirOrFile(
		File 		f) 
	{
  		try{
  			if ( f.isDirectory()){
  	      	
  				File[] files = f.listFiles();
  	        
  				if ( files != null ){
  					
 	  				for (int i = 0; i < files.length; i++){
		  	
 	  						// don't propagate failures here, if something goes wrong
 	  						// then it'll fail higher up the hierarchy
 	  					
	  					deleteDirOrFile( files[i]);
  					}
  				}
  			} 
  	      	      
  			for (int i=0;i<10;i++){
  				
  				if ( i > 0 ){
  					
	 				try{
	  					Thread.sleep(1000);
	  	      		
	  				}catch( InterruptedException e ){
	  				} 
	  	 	      	
	  				log( "   delete of '" + f.toString() + "' failed, retrying" );
 				}
  				
  				if ( f.delete()){
  					
  					if (f.exists()) {
  						log("Warning. Deleted '" + f.toString() + "' but file still exists");
  						log("Second Delele results in " + f.delete());
  					}
  	      		
  					return( true );
  				}
   			}
  			  				
  			log( "Failed to delete '" + f.toString() + "'" );
 
  	    }catch( Exception e ){
  	    	
  	    	log( e );
  	    }
  	    
  	    return( false );
  	}
  	
  	private ServerSocket
	waitForAzureusClose(
		int		port )
	{
  		log( "Waiting to bind to port " + port );
  	     
  		ServerSocket server = null;
  		
  	    int		loop	= 0;
  	    
  	    try{
  
	  	    while( server == null ){
	  	    	
	  	    	try{
	  	    		server = new ServerSocket( port, 50, InetAddress.getByName("127.0.0.1"));
		  	    	
	  	    		if ( !do_restart ){
	  	    			
	  	    				// if we're not restarting then hook into the server socket so that
	  	    				// if someone manually starts Azureus during this phase it'll 
	  	    				// restart
	  	    			
		    			final ServerSocket f_server = server;
	  	    				
		  	    		Thread accepter = 
		  	    			new Thread()
			  	    		{
			  	    			public void
			  	    			run()
			  	    			{
			  	    				while( true ){
			  	    					
			  	    					try{
			  	    						Socket	s = f_server.accept();
			  	    						
			  	    						do_restart = true;
			  	    						
			  	    						try{
			  	    							log( "Received server socket connection during update, flipping restart" );
			  	    							
			  	    						}catch( Throwable e ){	
			  	    						}
			  	    						
			  	    						s.close();
			  	    						
			  	    						break;
			  	    						
			  	    					}catch( Throwable e ){
			  	    						
			  	    						break;
			  	    					}
			  	    					
			  	    				}
			  	    			}
			  	    		};
		  	    		
			  	   		accepter.setDaemon( true );
			  	    		
			  	   		accepter.start();
	  	    		}	
	  	    	}catch(Exception e){
	  	        	
	  	    		if ( loop >= 5 ){
	  	    		
	  	    			log( "Exception while trying to bind on port " + port + ": " + e );
	  	    		}
	  	    		
	  	    		loop++;
	  	        	
	  	    		if ( loop == 30 ){
	  	        		
	  	    			log( "Giving up on bind" );
	  	        		
	  	    			return( null );
	  	    		}
	  	        	
	  	    		Thread.sleep(2000);
	  	    	}
	  	    }
		  	      
		  	    //Wait 2 sec more anyway.
	  	    
	  	    Thread.sleep(2000);
	  	    
  	    }catch( InterruptedException e ){
  	    	
  	    	log( e );
  	    }
  	    
  	    return( server );
  	}
  	
  	private void
  	log(
  		String		str )
  	{
  		try{
  			if ( log_writer != null ){
  		
  				log_writer.println( str );
  			}
  		}catch( Throwable e ){
  		}
  	}
  	
 	private void
  	log(
  		Throwable	e )
  	{
  		try{
  			if ( log_writer != null ){
  		
  				e.printStackTrace( log_writer );
  			}
  		}catch( Throwable f ){
  		}
  	}
  	
	private String 
	format(
		int n) 
	{
	   if(n < 10) return "0".concat(String.valueOf(n));
	   return String.valueOf(n);
	}  
	
	
	private String
	getClassPath()
	{
		String _classPath = System.getProperty("java.class.path");
	    	// we've got to remove Updater.jar from the classpath else if there's a
			// updater to Updater.jar we're stuffed
		
		StringTokenizer tok = new StringTokenizer( _classPath, System.getProperty("path.separator"));
		
		String	classPath = "";
			
		while( tok.hasMoreTokens()){
			
			String	bit = tok.nextToken();
			
			if ( !bit.endsWith( File.separator + UPDATER_JAR )){
				
				classPath += (classPath.length()==0?"":System.getProperty("path.separator")) + bit;
			}
		}
			    
	    return( "-classpath \"" + classPath + "\" " );
	}
	
	private boolean
	win32NativeRestart(
		String		exec )
	{
		return( false );
	}
	
	public static final String  OSName = System.getProperty("os.name");
	  
  public static final boolean isOSX	= OSName.toLowerCase().startsWith("mac os");
  public static final boolean isWindows	= OSName.toLowerCase().startsWith("windows");
  // If it isn't windows or osx, it's most likely an unix flavor
  public static final boolean isUnix = !isWindows && !isOSX;
	
  
	
	 // ****************** This code is somewhat duplicated in AzureusRestarterImpl / Updater so make changes there too !!!
	  
	  //Beware that for OSX no SPECIAL Java will be used with
	  //This method.
	  

	  public void 
	  restartAzureus(
	    String    mainClass,
	    String[]  properties,
	    String[]  parameters ) 
	  {
	    if(isOSX){
	    	
	    	restartAzureus_OSX(mainClass,properties,parameters);
	    	
	    }else if( isUnix ){
	    	
	    	restartAzureus_Linux(mainClass,properties,parameters);
	      
	    }else{
	    	
	    	restartAzureus_win32(mainClass,properties,parameters);
	    }
	  }
	  
	  private void 
	  restartAzureus_win32(
	    String    mainClass,
	    String[]  properties,
	    String[]  parameters) 
	  {
	    
		  String	exec;
		  
		  if ( APPLICATION_CMD != null ){
			  
			  exec = APPLICATION_CMD;
			  
		  }else{
			  
		    //Classic restart way using Runtime.exec directly on java(w)		    
		    exec = "\"" + JAVA_EXEC_DIR + "javaw\" "+ getClassPath() + getLibraryPath();
		    
		    for (int i=0;i<properties.length;i++){
		      exec += properties[i] + " ";
		    }
		    
		    exec += mainClass;
		    
		    for(int i = 0 ; i < parameters.length ; i++) {
		      exec += " \"" + parameters[i] + "\"";
		    }
		  }
		  

	      log( "  " + exec );
	
	    
	    if ( !win32NativeRestart( exec )){
	      
	    	javaSpawn( exec);
	    }
	  }
	  
	  
		private boolean
		javaSpawn(
			String execString) 
		{
			try {
				// hmm, try java method - this WILL inherit handles but might work :)

				log("Using java spawn");

				//NOTE: no logging done here, as we need the method to return right away, before the external process completes
				Process p = Runtime.getRuntime().exec( new String[]{ execString });

				return true;
			} catch (Throwable t) {

				t.printStackTrace();
				return false;
			}
		}
	  
	  private void 
	  restartAzureus_OSX(
	    String mainClass,
	    String[]  properties,
	    String[] parameters) 
	  {
			
	  	String bundle_path = System.getProperty("user.dir") + "/" + APPLICATION_NAME + ".app";
	  	bundle_path = new File( bundle_path ).getAbsolutePath();
	  	
	  	String exec;
	  	
		  if( APPLICATION_CMD != null ) {
		  	
		  	log( "Updater: app_cmd key found: " + APPLICATION_CMD );
		  	
		  	exec = APPLICATION_CMD;
		  }
		  else {
		  	//THIS SHOULD NEVER GET CALLED 2306 ONWARDS

		    exec = "\"" +bundle_path+ "/Contents/MacOS/java_swt\" " + getClassPath() +getLibraryPath();
		    
		    for (int i=0;i<properties.length;i++){
		      exec += properties[i] + " ";
		    }
		    
		    exec += mainClass;
		    
		    for(int i = 0 ; i < parameters.length ; i++) {
		      exec += " \"" + parameters[i] + "\"";
		    }
		  }
		  		  
		  runExternalCommandViaUnixShell( exec );
	  }
	  
	  
	  
	  private int getUnixScriptVersion() {
			String sVersion = System.getProperty("azureus.script.version", "0");
			int version = 0;
			try {
				version = Integer.parseInt(sVersion);
			} catch (Throwable t) {
				
				log( "getScriptVersion failed for '" + sVersion + "'" );
				log(t);
			}
			
			log( "getScriptVersion -> " + version );
			return version;
	  }

	  private void 
	  restartAzureus_Linux(
	  String    mainClass,
	  String[]  properties,
	  String[]  parameters) 
	  {
	  	if (getUnixScriptVersion() >= 1) {
	  		// Already setup for restart by core and script
	  		
	  		log( "restartLinux: script performing restart");
	  		
	  		return;
	  	}
	    
	    String exec = "\"" + JAVA_EXEC_DIR + "java\" " + getClassPath() + getLibraryPath();
	    
	    for (int i=0;i<properties.length;i++){
	      exec += properties[i] + " ";
	    }
	    
	    exec += mainClass ;
	    
	    for(int i = 0 ; i < parameters.length ; i++) {
	      exec += " \"" + parameters[i] + "\"";
	    }
	    
	    runExternalCommandViaUnixShell( exec );
	  }
	  
	  
	  
	  private String
	  getLibraryPath()
	  {
	    String libraryPath = System.getProperty("java.library.path");
	    
	    if ( libraryPath == null ){
	    	
	      libraryPath = "";
	      
	    }else{
	    	
	    		// remove any quotes from the damn thing
	    	
	    	String	temp = "";
	    	
	    	for (int i=0;i<libraryPath.length();i++){
	    		
	    		char	c = libraryPath.charAt(i);
	    		
	    		if ( c != '"' ){
	    			
	    			temp += c;
	    		}
	    	}
	    	
	    	libraryPath	= temp;
	    	
	    		// remove trailing separator chars if they exist as they stuff up
	    		// the following "
	    	
	    	while( libraryPath.endsWith(File.separator)){
	    	
	    		libraryPath = libraryPath.substring( 0, libraryPath.length()-1 );
	    	}
	    	
	    	if ( libraryPath.length() > 0 ){
	  
	    		libraryPath = "-Djava.library.path=\"" + libraryPath + "\" ";
	    	}
	    }
	    
	    return( libraryPath );
	  }
	  
	  

	  private void logStream(String message,InputStream stream) {
	    BufferedReader br = new BufferedReader (new InputStreamReader(stream));
	    String line = null;
	    boolean first = true;
	    
	    try {
	      while((line = br.readLine()) != null) {
	      	if( first ) {
	      		log(message);
	      		first = false;
	      	}
	      	
	        log(line);
	      }
	    } catch(Exception e) {
	       log( e.toString());
	       log(e);
	    }
	  }
	  
	  
	  private void chMod(String fileName,String rights) {
	    String[] execStr = new String[3];
	    execStr[0] = findCommand( "chmod" );
	    execStr[1] = rights;
	    execStr[2] = fileName;
	    
	    runExternalCommandsLogged( execStr );
	  }
	  
	  private String
	  findCommand(
		String	name )
	  {
		final String[]  locations = { "/bin", "/usr/bin" };
		
		for ( String s: locations ){
			
			File f = new File( s, name );
			
			if ( f.exists() && f.canRead()){
				
				return( f.getAbsolutePath());
			}
		}
		
		return( name );
	  }
	  
	  /*
	   * If you enable this then fix it not to use runtime.exec( String ) as this doesn't handle spaces from
	   * 1.7_B21+
	  private Process runExternalCommandLogged( String command ) {  //NOTE: will not return until external command process has completed
	  	log("About to execute: U:[" +command+ "]" );
	  	
	  	try {
	  		Process runner = Runtime.getRuntime().exec( command );
	  		runner.waitFor();		
	  		logStream( "runtime.exec() output:", runner.getInputStream());
	      logStream( "runtime.exec() error:", runner.getErrorStream());
	      return runner;
	  	}
	  	catch( Throwable t ) {
	  		log( t.getMessage() != null ? t.getMessage() : "<null>" );
	  		log( t.toString());
	  		log( t );
	  		return null;
	  	}
	  }
	  */
	  
	  private Process runExternalCommandsLogged( String[] commands ) {  //NOTE: will not return until external command process has completed
	  	String cmd = "About to execute: U:[";
	  	for( int i=0; i < commands.length; i++ ) {
	  		cmd += commands[i];
	  		if( i < commands.length -1 )  cmd += " ";
	  	}
	  	cmd += "]";
	  	
	  	log( cmd );
	  	
	  	try {
	  		Process runner = Runtime.getRuntime().exec( commands );
	  		runner.waitFor();		
	  		logStream( "runtime.exec() output:", runner.getInputStream());
	      logStream( "runtime.exec() error:", runner.getErrorStream());
	      return runner;
	  	}
	  	catch( Throwable t ) {
	  		log( t.getMessage() != null ? t.getMessage() : "<null>" );
	  		log( t.toString() );
	  		log( t );
	  		return null;
	  	}
	  }
	  
	  
	  private void runExternalCommandViaUnixShell(String command ) {
	  	String[] to_run = new String[3];
	  	to_run[0] = "/bin/sh";
	  	to_run[1] = "-c";
	  	to_run[2] = command;
	   	 
	  	log("Executing: U:[" +to_run[0]+ " " +to_run[1]+ " " +to_run[2]+ "]" );

	  	try {
	  		//NOTE: no logging done here, as we need the method to return right away, before the external process completes
	  		Runtime.getRuntime().exec( to_run );	
	  	}
	  	catch(Throwable t) {
	  			log( t.getMessage() != null ? t.getMessage() : "<null>" );
	  			log( t.toString());
	  			log( t );
	  	}
	  }
	}