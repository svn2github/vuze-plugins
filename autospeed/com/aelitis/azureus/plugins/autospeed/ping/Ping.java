/*
 * Created on 27 sept. 2004
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
package com.aelitis.azureus.plugins.autospeed.ping;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.gudy.azureus2.core3.util.Constants;

/**
 * @author Olivier Chalouhi
 *
 */
public class Ping {

	boolean active = true;
	List listeners;
	
	Thread runner;
	Process ping;
	BufferedReader br;
	
	public Ping(final String hostName) {
		listeners = new ArrayList();
		runner = new Thread("Ping") {
			public void run() {
				try {
          
          //Windows version
          if(Constants.isWindows) {
            ping = Runtime.getRuntime().exec("ping -t " + hostName);
          }
					
          //Mac OSX version
          if(Constants.isOSX || Constants.isLinux) {
            ping = Runtime.getRuntime().exec(new String[] {"ping", hostName});
          }          
          
					br = new BufferedReader(new InputStreamReader(ping.getInputStream()));
				} catch(Exception e) {
					e.printStackTrace();
					active = false;
				}
				while(active) {
					try {
					String line = br.readLine();
					if(line == null) {
						active = false;
						continue;
					}
						try {
              
              //Windows version
              if(Constants.isWindows) {
  							if(line.length() > 10) {
  								int end = line.indexOf("ms");							
  								int start = line.indexOf("=",end-5);
  								//FIX for win2k and older reporting <10ms instead of =Xms
  								if(start == -1 || start > end ) start = line.indexOf("<",end-5);
  								String time = line.substring(start+1,end);
  								int ms = Integer.parseInt(time.trim());
  								Ping.this.notifyListenersSuccess(ms);
  							}
              }
              
              //Mac OSX version
              if(Constants.isOSX || Constants.isLinux) {
                final int end = line.indexOf(" ms");
                if(end != -1) {
                  final int start = line.indexOf("time=");
                  final String time = line.substring(start+5,end);
                  final int ms = Double.valueOf(time.trim()).intValue();
                  Ping.this.notifyListenersSuccess(ms);
                }
              }
              
						} catch(Exception e) {
							Ping.this.notifyListenersFailure();
						}
					} catch(Exception e) {
						e.printStackTrace();
						active = false;
					}
				}
				if(ping != null) {
					ping.destroy();
				}
			}
		};
		runner.setDaemon(true);
	}		
	
	public void start() {
		runner.start();
	}
	
	public void stop() {
		active = false;
	}
	
	
	public void addListener(PingListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(PingListener listener) {
		listeners.remove(listener);
	}
	
	private void notifyListenersSuccess(int ms) {
		Iterator iter = listeners.iterator();
		while(iter.hasNext()) {
			try {
				((PingListener)iter.next()).pingSuccess(this,ms);
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private void notifyListenersFailure() {
		Iterator iter = listeners.iterator();
		while(iter.hasNext()) {
			try {
				((PingListener)iter.next()).pingFailure(this);
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static void main(String args[]) {
		Ping ping = new Ping("www.google.com");
    ping.addListener(new PingListener() {
      public void pingFailure(Ping ping) {        
        System.out.println("Ping failure");
      }
      public void pingSuccess(Ping ping, int ms) {
       System.out.println("Ping success : " + ms + " ms"); 
      }
    });
		ping.start();
    try {Thread.sleep(20000); } catch(Exception e) {}
	}
}
