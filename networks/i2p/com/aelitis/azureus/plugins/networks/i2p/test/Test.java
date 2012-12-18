/*
 * Created on 28-Dec-2004
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

package com.aelitis.azureus.plugins.networks.i2p.test;

/**
 * @author parg
 *
 */

import java.io.*;

import net.i2p.*;
import net.i2p.client.*;
import net.i2p.client.streaming.*;
import net.i2p.data.*;
import net.i2p.util.I2PThread;

public class 
Test 
{
	/*
	public static void
	main(
		String[]	args )
	{
		
		//String tracker = "5HT3V86J7vEA33svEHXOidL-6TN5ZQ3FmCc3BOnjLottSZ6kEiunXXnBMETrBjq-jqq0~RwL-2pKWIGYQ8Pg4eVGq9ZsAuuXGvanxjh8PzpTZC7H-okHxUuqGTKkclBdF~wMzQhAGxI-6giv~f2NfkgSYHV-dnu7DHxFtcWWWQm7cTnb3Hp-jlX3m6Tl4TcSVBh3hnMTWAVnGajSKCnrCUdqcm7J0PxIwR6pMC0A8v1oqkEcxo~LM-xgP3mq3gk3sZkCSxRIZ8sZmRYvVDFoGz~gVnmhN9I5D5wSU3nk9jOHHj~arpSMhuSSr31OXcj5nG5HgN6qVM4ioKy-Km1TZj4eN2lV~r6tGhW04~h9GwZXjPXrKaDF-uY6NjP7bPlgELy3g9Yk6iGWSFetuxK3wmqnYC60MSLgzeYxxVMVSih1NK~SngOhpN2vUqDg3IsLxYTEP-nBU-79hTC27~ES1V1kQY8tvUDI9kqMe1ieH246cg0k4TlvHSBq-ImvsHZBAAAA";
		String data    = "z0-oh9bivFmYlhFMVbdHmRtOV72ZH128JBxmB8gKdQ2A8yOVRRsxJJ-PAVG7QUO4wsqng9Pq0~lwsK5eUBVPnnc8LcHD-iZV8YlcZziylM-phbVKieaCTa4KbzefVc321XhcFVlTZEsNpt~D2INpU0V73HWe48w9Ff5~K82F0rh8s~8Vq-UOr6HGCYmVySPQA05gvcOHsXq7riZQTkEh~H1YInIec20puPmHtwWobQgq2oc8qA98bVR5hhXjgRSE7-ycGIFPMX6s4ghl8TqKn5j8Rd8MImIeRLaTJfU35gu2r5Izb2wfQXIo4NYP6DVOsZqbpVkA89WVAr9D63l8J1lXHqaHg~XYsA7I0N-ENI632LqnpXFq5Qq6D1yCnGdgpbqRzjh4fLUmsczV~T9BYZPQX0F38DpqQyEWYL~Kiz7A8LQ-yAN4KokoTLue07DMzAIObSQQnq2Vo6wM23iuXxH9nhXutQHUaLHRzAk7f-jyFitQIcdjybuDzBKyFsXLAAAA";
		
		try{
	        I2PAppContext ctx = new I2PAppContext();
	
	        Destination tracker = ctx.namingService().lookup( "tracker2.postman.i2p" );
	        
	        Destination remote_dest = new Destination();
	        
	        remote_dest.fromBase64( data );
	        	       
	        final I2PSocketManager	socket_manager = I2PSocketManagerFactory.createManager();

	        System.out.println( "got socket manager" );
	        
	        I2PSession session = socket_manager.getSession();
	        
	        final I2PServerSocket serverSocket = socket_manager.getServerSocket();
	        
	        new I2PThread()
	        {
	        	public void
	        	run()
	        	{
	        		while( true ){
	        			try{
	        				I2PSocket sock = serverSocket.accept();
	        			
	        				System.out.println( "accepted" );
	        				sock.close();
	        				
	        			}catch( Throwable e ){
	        				e.printStackTrace();
	        			}
	        		}
	        	}
	        }.start();
	        
	        System.out.println( "got session" );
	        
	        final Destination my_dest = session.getMyDestination();
	        
	        System.out.println( "got my dest" );

	        System.out.println( my_dest.toBase64());
	        
	        new I2PThread()
	        {
	        	public void
	        	run()
	        	{
	        		while( true ){
	        			try{
	        			   boolean res = socket_manager.ping( my_dest, 10*1000 );
	        		        
	        		       System.out.println( "ping commplete: " + res);
	        		        
	        		       I2PSocket	socket = socket_manager.connect( my_dest );
	        		        
	        		        System.out.println( "got socket" );
	        		        
	        			}catch( Throwable e ){
	        				e.printStackTrace();
	        			}
	        		}
	        	}
	        }.start();
	       // boolean res = socket_manager.ping( my_dest, 60*1000 );
	        
	        //System.out.println( "ping commplete: " + res);
	        
	        I2PSocket	socket = socket_manager.connect( tracker );
	        
	        System.out.println( "got socket" );
	        
	        OutputStream	os = socket.getOutputStream();
	        
	        //os.write( "GET moooo\r\n\r\n".getBytes());
	       
	        //os.write( "GET moooo HTTP/1.1\r\n\r\n".getBytes());
	        
	        os.write( "GET /announce?info_hash=%CE%15%EF%A1%C5%3Cj%94%A4%86%CE%E7%0B%10%1F%E1L%DB%CBB&peer_id=-AZ2203-3AfSYfvOJLHa&port=7881&uploaded=0&downloaded=0&left=39013&event=started&numwant=160&compact=1&ip=xMiEMkGNYqs4yZkEsl14GoSqhQsPUmmkbgGBp-QS2VYYJMf~YPQCgIFK06~PJPI~x2COzahXdlbwmhVu9-NKzX0l7Mr690snMKsStlbuW5PXzKJBnfWnwxg5dRl0NPD2LLJMnvtUG0N857COcnBJwnvicDH1vsX~SMavbjMqr0k7Zhb46cyVTMwHRYH~dx5X0~wZNBsRG-81D2wK37xPzcqPW5PG380BoT2Q-8TSNM5Ss-T9oN02EVrixDb1EjkXRU5LnnJ2y4uw17y1SXKrulj-4b5Wq6IKQnfSE~ebenEjaFM-fOJEiPxOLfOj3ehTZJ7vQUdmnDVEOHWsMo6th1UmWyu-JhZc5wmw2lpY31UI9KV33zu1lCfPOOWgx4tLWtEcgU004VeaAgiZB1NAJpwZq2PRN~THLYxOCZLfyw6MSMouEirpT9i9bpNGa2rS~59Ej1tPJp6GNBe~hnjhO9QDm4YLyAMf4o0Ay63jzNWjvaMaVwwUltl~wkUQXXlaAAAA.i2p&key=RyI3cGQv HTTP/1.1\r\n\r\n".getBytes());
	        
	        os.flush();
	        
	        System.out.println( "wrote data" );
	        
	        InputStream	is = socket.getInputStream();
	        
	        while(true){
	        	
	        	byte[]	buffer = new byte[1024];
	        	
	        	int	len = is.read( buffer );
	        	
	        	if ( len < 0 ){
	        		
	        		break;
	        	}
	        	
	        	System.out.println( new String( buffer, 0, len ));
	        }
		}catch( Throwable e ){
			
			e.printStackTrace();
		}   
	}
	*/
	
	public static void
	main(
		String[]	args )
	{
		try{
	        final I2PSocketManager	socket_manager = I2PSocketManagerFactory.createManager();

	        System.out.println( "got socket manager" );
	        
	        I2PSession session = socket_manager.getSession();
	        
	        System.out.println( "got session" );
	        
	        final Destination my_dest = session.getMyDestination();
	        
	        System.out.println( "got my dest" );

	        System.out.println( my_dest.toBase64());
	        
	        new I2PThread()
	        {
	        	public void
	        	run()
	        	{
	        		while( true ){
	        			try{
	        				Thread.sleep( 1000 );
	        				
	        				boolean res = socket_manager.ping( my_dest, 10*1000 );
	        		        
	        				System.out.println( "ping commplete: " + res);
		        		        
	        			}catch( Throwable e ){
	        				e.printStackTrace();
	        			}
	        		}
	        	}
	        }.start();

		}catch( Throwable e ){
			
			e.printStackTrace();
		}   
	}
}
