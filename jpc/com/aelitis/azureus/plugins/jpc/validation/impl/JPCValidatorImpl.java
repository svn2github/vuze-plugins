/*
 * Created on 08-Feb-2005
 * Created by Paul Gardner
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

package com.aelitis.azureus.plugins.jpc.validation.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.aelitis.azureus.plugins.jpc.JPCException;
import com.aelitis.azureus.plugins.jpc.JPCPlugin;
import com.aelitis.azureus.plugins.jpc.license.JPCLicense;
import com.aelitis.azureus.plugins.jpc.license.JPCLicenseEntry;
import com.aelitis.azureus.plugins.jpc.validation.JPCValidator;

/**
 * @author parg
 *
 */

public class 
JPCValidatorImpl
	implements JPCValidator
{
	private JPCPlugin		plugin;
	
	public
	JPCValidatorImpl(
		JPCPlugin	_plugin )
	{
		plugin	= _plugin;
	}
	
	public void
	validate(
		InetSocketAddress		cache_address,
		InetAddress				public_ip_address,
		JPCLicense				license )
	
		throws JPCException
	{

		JPCLicenseEntry[]	entries = license.getEntries();
		
		boolean	bt_enabled		= false;
		boolean	cache_ip_ok		= false;
		boolean	client_ip_ok	= false;
		
		Date	expire_date		= null;
		Date	bt_expire_date	= null;
		
		long	public_address_long = addressToLong( public_ip_address );
		
    plugin.log( "Validating license:", JPCPlugin.LOG_DEBUG );
    
		for (int i=0;i<entries.length;i++){
			
			String	name 	= entries[i].getName().toLowerCase();
			String	value	= entries[i].getValue();
      
      plugin.log( "   " +name+ "=" +value, JPCPlugin.LOG_DEBUG );
			
			try{
				if ( name.equals( "cacheip" )){
					
					if ( !cache_ip_ok ){
						
						cache_ip_ok = cache_address.getAddress().getHostAddress().equals( value );
					}
					
				}else if ( name.equals( "clientiprange" )){
					
					if ( !client_ip_ok ){
						
						int	pos = value.indexOf( "-" );
						
						if( pos == -1 ){
							
							client_ip_ok = public_address_long == addressToLong( value ); 
						}else{
							
							String	range_start = value.substring( 0, pos ).trim();
							String	range_end	= value.substring( pos+1 ).trim();
							
							client_ip_ok = 	public_address_long >= addressToLong( range_start ) &&
											public_address_long <= addressToLong( range_end );
						}
					}
				}else if ( name.equals( "expiredate" )){
					
					expire_date = new SimpleDateFormat( "yyyyMMdd" ).parse( value );
					
				}else if ( name.equals( "btexpiredate" )){
					
					bt_expire_date = new SimpleDateFormat( "yyyyMMdd" ).parse( value );

				}else if ( name.equals( "enablebt" )){
					
					bt_enabled = value.equals( "1" );
				}
			}catch( Throwable e ){
				
			}
		}
		
		plugin.log( 
				"Validator:bt_enabled = " + bt_enabled + ", bt_exp = " + bt_expire_date + 
				", exp = " + expire_date + ", cache_ip_ok = " + cache_ip_ok,
				JPCPlugin.LOG_DEBUG );
		
		if ( !bt_enabled ){
			
			throw( new JPCException( "BT is not enabled for the cache" ));
		}
		
		if( !cache_ip_ok ){
			
			throw( new JPCException( "Actual cache IP and that in license file differ" ));
		}
		
		Date	check_date = bt_expire_date != null? bt_expire_date: expire_date;
		
		if ( check_date == null ){
			
			throw( new JPCException( "Expiry date not found in license file" ));
		}
		
			// one day grace period
		
		if ( new Date( System.currentTimeMillis()+24*60*60*1000).compareTo( check_date ) > 0 ) {
			throw( new JPCException( "Cache license has expired" ));
		}
		
		if ( !client_ip_ok ){
			throw( new JPCException( "Cache does not support this IP (" + public_ip_address + ")"));
		}
	}
	
	private long
	addressToLong(
		String		address )
	
		throws UnknownHostException
	{
		return( addressToLong( InetAddress.getByName( address )));
	}
	
	private static long
	addressToLong(
		InetAddress		i_address )
	{		
		byte[]	bytes = i_address.getAddress();
		long	resp = (bytes[0]<<24)&0xff000000 | (bytes[1] << 16)&0x00ff0000 | (bytes[2] << 8)&0x0000ff00 | bytes[3]&0x000000ff;
	
    
    if ( resp < 0 ) {
      resp += 0x100000000L;     
    }
    
		return( resp );
	}
  
  //A small main method to debug the addressToLong method
  /*
  public static void main(String[] args) {  
    try { 
      InetAddress add = InetAddress.getByName("212.107.43.11");
      long lStart = addressToLong(add);
      add = InetAddress.getByName("212.107.43.13");
      long lEnd = addressToLong(add);
      add = InetAddress.getByName("212.107.43.12");
      long lReal = addressToLong(add);      
      System.out.print(lStart  + " - " + lReal + " - " + lEnd + " - " + (lReal >= lStart) + " - " + (lReal <= lEnd));
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
  }*/
}
