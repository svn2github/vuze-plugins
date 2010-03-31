/*
 * Created on 28 sept. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.plugins.autospeed.tracert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import org.gudy.azureus2.core3.util.Constants;

/**
 * @author Olivier Chalouhi
 *
 */
public class TraceRoute {
	
	public static String[] traceRoute(String host) {
		final List results = new LinkedList();
		
    
    try {
      //Windows version
      if(Constants.isWindows) {
        final Process trace = Runtime.getRuntime().exec("tracert -h 7 -d -w 500 " + host);
        final BufferedReader br = new BufferedReader(new InputStreamReader(trace.getInputStream()));
        String line;
        while((line = br.readLine()) != null) {     
          if(!line.startsWith("  ")) continue;
          if(line.length() < 32) continue;
          String ip = line.substring(32,line.length()).trim();       
          results.add(ip);
        }
      }
      
      //Mac-OSX version
      if(Constants.isOSX || Constants.isLinux) {
        final Process trace = Runtime.getRuntime().exec(new String[]{"traceroute", "-m", "7", "-n", "-w", "2", host});
        final BufferedReader br = new BufferedReader(new InputStreamReader(trace.getInputStream()));
        String line;
        while((line = br.readLine()) != null) {
          if(line.indexOf("*") != -1) continue;
          final String ip = line.substring(4, line.indexOf(" ", 4)).trim();
          results.add(ip);
        }
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
   
    
    if(Constants.isOSX) {
      
    }
  		
      
		
		return (String[]) results.toArray(new String[results.size()]);
	}
	
	public static String getGatewayIP(String[] ips, int nbByPass) {
		for(int i = 0 ; i < ips.length - 1; i++) {
			String ip = ips[i];
			//Filter non public addresses
			//Local loop-back
			if(ip.equals("127.0.0.1")) continue;
			if(ip.startsWith("10.")) continue;
			if(ip.startsWith("192.168.")) continue;
			if(ip.startsWith("169.254.")) continue;

			//Lazy way to test the 172.16 - 172.31 range :
			boolean inRange = false;
			for(int j = 16 ; j < 32 ; j++) {
				if(ip.startsWith("172." + j + ".")) {inRange = true; break;}
			}
			if(inRange){
			  continue;			  
			}
			
			if(nbByPass > 0) {
			    nbByPass--;
			    continue;
			} 
			
			//Fix for time-out of first hops sometimes.
			if(! checkValidIp(ip)) continue;
			
			return ip;
		}
		
		//Last we return the last IP in the range
    if(ips.length > 1)
      return ips[ips.length - 1];
    return null;
	}
	
	public static boolean checkValidIp(String ip) {
		if(ip == null)
			return false;
		StringTokenizer st = new StringTokenizer(ip,".");
		if(st.countTokens() != 4 ) return false;
		while(st.hasMoreTokens()) {
			String subIp = st.nextToken();
			try {
				Integer.parseInt(subIp);
			} catch(NumberFormatException e) {
				return false;
			}
		}		
		return true;
	}
	
	public static void main(String args[]) {
		String ips[] = traceRoute("www.google.com");
		System.out.println(getGatewayIP(ips,0));
	}
}
