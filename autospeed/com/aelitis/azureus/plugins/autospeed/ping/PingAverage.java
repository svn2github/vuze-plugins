/*
 * Created on 12 déc. 2004
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

/**
 * @author Olivier Chalouhi
 *
 */
public class PingAverage implements PingListener {
  
  Ping ping;
  int values[];
  int index;
  public PingAverage(String ip, int nb_pings) {
    values = new int[nb_pings];
    index = 0;
    ping = new Ping(ip);
    ping.addListener(this);
    ping.start();
  }
  
  public void pingFailure(Ping ping) {
    pingSuccess(ping,10000);
  }

  public void pingSuccess(Ping ping, int ms) {
    values[index++] = ms;
    if(index >= values.length) {
      index = 0;
    }
    int average = 0;
    for(int i = 0 ; i < values.length ; i++) {
      average += values[i];
    }
    average /= values.length;
    System.out.print("\rAverage over " + values.length + " pings : " + average + " ms   ");    
  }
  
  public static void main(String args[]) {
    if(args.length < 2) usage();
    try {
      String ip = args[0];
      int nb_pings = Integer.parseInt(args[1]);
      new PingAverage(ip,nb_pings);
      while(true) {
        Thread.sleep(1000);
      }
    }catch(Exception e) {
      System.out.println(e.getMessage());
    }
  }
  
  public static void usage() {
    System.out.println("Usage : PingAverage <IP> <number of pings>");
    System.exit(0);
  }
}
