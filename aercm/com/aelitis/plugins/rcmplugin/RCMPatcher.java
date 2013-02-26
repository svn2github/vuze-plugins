/*
 * Created on Feb 26, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.plugins.rcmplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;

public class 
RCMPatcher 
{
	protected
	RCMPatcher()
	{
		applyPatches();
	}
	
	private void
	applyPatches()
	{
		try{
			applyPatch1();

		}catch( Throwable e ){	
		}
		
		try{
			applyPatch2();

		}catch( Throwable e ){	
		}
		
	}
	
	private void
	applyPatch1()
	{
			/*
			 * Hack for 4800 OSX default save dir issue
			 */
		
		if ( 	Constants.isOSX &&
				( Constants.AZUREUS_VERSION.startsWith( "4.8.0.0" ) || Constants.AZUREUS_VERSION.startsWith( "4.8.0.1" ))){
		
			String	key = "Default save path";
			
			if ( !COConfigurationManager.doesParameterNonDefaultExist( key )){
				
				String docPath =  SystemProperties.getDocPath();
				
				File f = new File( docPath, "Azureus Downloads" );
				
					// switch to Vuze Downloads for new installs
				
				if ( !f.exists()){
					
					f = new File( docPath, "Vuze Downloads" );
				}
				
				Debug.out( "Hack: Updating default save path from '" + COConfigurationManager.getParameter( key ) + "' to '" + f.getAbsolutePath() + "'" );
				
				COConfigurationManager.setParameter( key, f.getAbsolutePath());
				
				COConfigurationManager.save();
				
			}else{
				
				// Debug.out( "Non def exists: " +  COConfigurationManager.getParameter( key ) + ",user-path='" + SystemProperties.getUserPath() + "'" );
			}
		}
	}
	
	private void
	applyPatch2()
	{
		if ( Constants.isCurrentVersionLT( "4.9.0.2 ")){
			
			try{
				if ( COConfigurationManager.getBooleanParameter( "rcmplugin.patcher.2.done.2", false )){
					
					return;
				}
				
				String	KEYSTORE_TYPE = null;
				
				String[]	types = { "JKS", "GKR", "BKS" };
				
				for (int i=0;i<types.length;i++){
					
					try{
						KeyStore.getInstance( types[i] );
						
						KEYSTORE_TYPE	= types[i];
						
						break;
						
					}catch( Throwable e ){
					}
				}
				
				if ( KEYSTORE_TYPE == null ){
					
						// it'll fail later but we need to use something here
					
					KEYSTORE_TYPE	= "JKS";
				}
				
				boolean	copy_certs = false;
				
				KeyStore keystore = KeyStore.getInstance( KEYSTORE_TYPE );
				
				String truststore_name 	= FileUtil.getUserFile(SESecurityManager.SSL_CERTS).getAbsolutePath();
	
				File truststore_file = new File( truststore_name );
				
				if ((!truststore_file.exists()) || truststore_file.length() < 512 ){
			
					copy_certs = true;
					
				}else{
				
					try{
						FileInputStream		in 	= null;
		
						try{
							in = new FileInputStream(truststore_name);
					
							keystore.load( in, SESecurityManager.SSL_PASSWORD.toCharArray());
							
						}finally{
							
							if ( in != null ){
								
								in.close();
							}
						}
					}catch( Throwable e ){
						
						copy_certs = true;
					}
				}
				
				if ( !copy_certs ){
					
					try{
						new URL( Constants.PAIRING_URL ).openConnection().getInputStream();
						
					}catch( Throwable e ){
						
						String str = Debug.getNestedExceptionMessage( e );
						
						System.err.println( str );
						
						e.printStackTrace();
						
						if ( 	str.contains( "CertificateParsingException") || 
								str.contains( "NoSuchAlgorithmException" ) || 
								str.contains( "InvalidAlgorithm")){
							
							copy_certs = true;
						}
					}
				}
				
				if ( copy_certs ){
					
					InputStream is = getClass().getClassLoader().getResourceAsStream( "com/aelitis/plugins/rcmplugin/resources/cacerts.def" );
					
					if ( is != null ){
						
						try{
							FileUtil.copyFile( is, new File(truststore_name));
					
							KeyStore new_keystore = KeyStore.getInstance( KEYSTORE_TYPE );

							FileInputStream		in 	= null;
			
							try{
								in = new FileInputStream( truststore_name );
						
								new_keystore.load( in, SESecurityManager.SSL_PASSWORD.toCharArray());
								
								TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
								
								tmf.init( new_keystore );
								
								SSLContext ctx = SSLContext.getInstance("SSL");
								
								ctx.init(null, tmf.getTrustManagers(), null);
											
								SSLSocketFactory	factory = ctx.getSocketFactory();
																		
								HttpsURLConnection.setDefaultSSLSocketFactory( factory );
	
							}finally{
								
								if ( in != null ){
									
									in.close();
								}
							}

						}finally{
							
							is.close();
						}
					}
				}
			}catch( Throwable e ){
				
			}finally{
				
				COConfigurationManager.setParameter( "rcmplugin.patcher.2.done.2", true );
			}
		}
	}
}
