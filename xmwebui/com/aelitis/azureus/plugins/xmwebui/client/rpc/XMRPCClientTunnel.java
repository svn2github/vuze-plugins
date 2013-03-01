/*
 * Created on Feb 28, 2013
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


package com.aelitis.azureus.plugins.xmwebui.client.rpc;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.agreement.srp.SRP6Client;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;
import org.gudy.azureus2.core3.util.Base32;
import org.json.simple.JSONObject;

import com.aelitis.azureus.util.JSONUtils;

public class 
XMRPCClientTunnel 
	implements XMRPCClient
{
	private static BigInteger 
    fromHex(
    	String hex )
    {
        return new BigInteger(1, Hex.decode( hex.replaceAll( " ", "" )));
    }
    
    private static final BigInteger N_3072 = fromHex(
		    "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1 29024E08" +
		    "8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD EF9519B3 CD3A431B" +
		    "302B0A6D F25F1437 4FE1356D 6D51C245 E485B576 625E7EC6 F44C42E9" +
		    "A637ED6B 0BFF5CB6 F406B7ED EE386BFB 5A899FA5 AE9F2411 7C4B1FE6" +
		    "49286651 ECE45B3D C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8" +
		    "FD24CF5F 83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D" +
		    "670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B E39E772C" +
		    "180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9 DE2BCBF6 95581718" +
		    "3995497C EA956AE5 15D22618 98FA0510 15728E5A 8AAAC42D AD33170D" +
		    "04507A33 A85521AB DF1CBA64 ECFB8504 58DBEF0A 8AEA7157 5D060C7D" +
		    "B3970F85 A6E1E4C7 ABF5AE8C DB0933D7 1E8C94E0 4A25619D CEE3D226" +
		    "1AD2EE6B F12FFA06 D98A0864 D8760273 3EC86A64 521F2B18 177B200C" +
		    "BBE11757 7A615D6C 770988C0 BAD946E2 08E24FA0 74E5AB31 43DB5BFC" +
		    "E0FD108E 4B82D120 A93AD2CA FFFFFFFF FFFFFFFF" );
	
    private static final BigInteger G_3072 = BigInteger.valueOf(5);
	    
    private static SecureRandom rand = new SecureRandom();

    private String		access_code;
    private String		username;
    private String		password;
    
    private	SecretKeySpec		_secret;
    private String				_tunnel_url;
    
    private int		calls_ok;
    private boolean	destroyed;
    
	public
	XMRPCClientTunnel(
		String		ac,
		String		tunnel_user,
		String		tunnel_password )
	{
		access_code		= ac;
		username		= tunnel_user;
		password		= tunnel_password;
	}
	
	private Object[]
	getCurrentTunnel(
		boolean		for_destroy )
	
		throws XMRPCClientException
	{
		synchronized( this ){
					
			if ( for_destroy ){
				
				destroyed = true;

				if ( _tunnel_url == null ){
					
					return( null );
				}
				
				Object[]	result = new Object[]{ _tunnel_url, _secret };
				
				_secret			= null;
				_tunnel_url	 = null;
				
				return( result );
				
			}else if ( destroyed ){
				
				throw( new XMRPCClientException( "Tunnel has been destroyed" ));
			}
			
			if ( _tunnel_url == null ){
								
			    try{
				    byte[] I = username.getBytes( "UTF-8" );
				    byte[] P = password.getBytes( "UTF-8" );

					String str = XMRPCClientUtils.getFromURL( PAIRING_URL + "pairing/tunnel/create?ac=" + access_code + "&sid=" + SID );
					
					System.out.println( "create result: " + str );

					JSONObject map = (JSONObject)JSONUtils.decodeJSON( str );
					
					JSONObject error = (JSONObject)map.get( "error" );

					if ( error != null ){
						
						long code = (Long)error.get( "code" );
						
							// 1, 2, 3 -> bad code/not registered
						
						if ( code == 1 ){
							
							throw( new XMRPCClientException( XMRPCClientException.ET_BAD_ACCESS_CODE ));
							
						}else if ( code == 2 || code == 3 ){
							
							throw( new XMRPCClientException( XMRPCClientException.ET_NO_BINDING ));
						}else{
							
							throw( new XMRPCClientException( "Uknown error creating tunnel: " + str ));
						}
					}
					
					JSONObject result1 = (JSONObject)map.get( "result" );
										
					byte[]		salt 	= Base32.decode((String)result1.get( "srp_salt" ));
					BigInteger	B		= new BigInteger( Base32.decode((String)result1.get( "srp_b" )));
					
					String url		= (String)result1.get( "url" );
										
					SRP6Client client = new SRP6Client();
					
			        client.init( N_3072, G_3072, new SHA256Digest(), rand );
			        
			        BigInteger A = client.generateClientCredentials( salt, I, P );
			       
			        BigInteger client_secret = client.calculateSecret( B );
	
					byte[] key = new byte[16];
					
					System.arraycopy( client_secret.toByteArray(), 0, key, 0, 16 );
					
					_secret 	= new SecretKeySpec( key, "AES");
					_tunnel_url	= url;
					
					Cipher encipher = Cipher.getInstance ("AES/CBC/PKCS5Padding");
					
					encipher.init( Cipher.ENCRYPT_MODE, _secret);
					
					AlgorithmParameters params = encipher.getParameters ();
					
					byte[] IV = params.getParameterSpec(IvParameterSpec.class).getIV();
			
					JSONObject activate = new JSONObject();
					
					activate.put( "url", url );
					activate.put( "endpoint", "/transmission/rpc" );
					activate.put( "rnd", rand.nextLong());
			
					byte[] activate_bytes = JSONUtils.encodeToJSON( activate ).getBytes( "UTF-8" );
			        
					byte[] enc = encipher.doFinal( activate_bytes );
					
					String str2 = 
						XMRPCClientUtils.getFromURL( url + 
							"?srp_a=" + Base32.encode( A.toByteArray()) + 
							"&enc_data=" + Base32.encode( enc )+ 
							"&enc_iv=" + Base32.encode( IV ));
			
					JSONObject map2 = (JSONObject)JSONUtils.decodeJSON( str2 );
			
					JSONObject error2 = (JSONObject)map2.get( "error" );

					if ( error2 != null ){

						String msg = (String)error2.get( "msg" );
						
						XMRPCClientException e =  new XMRPCClientException( "Authentication failed: " + msg );
						
						e.setType( XMRPCClientException.ET_CRYPTO_FAILED );
						
						throw( e );
					}
					
					JSONObject result2 = (JSONObject)map2.get( "result" );
			
					System.out.println( result2 );
					
				}catch( XMRPCClientException e ){
					
					throw( e );
					
				}catch( Throwable e ){
					
					throw( new XMRPCClientException( "Failed to create tunnel", e ));
				}
			}
			
			return( new Object[]{ _tunnel_url, _secret });
		}
	}
	
	public JSONObject
	call(
		JSONObject		request )
	
		throws XMRPCClientException
	{
		Object[] tunnel = getCurrentTunnel( false );
		
		String			url		= (String)tunnel[0];
		SecretKeySpec	secret 	= (SecretKeySpec)tunnel[1];
		
		String json = JSONUtils.encodeToJSON( request );
		
		try{
			System.out.println( "Sending request: " + json );
			
			byte[] plain_bytes = json.getBytes( "UTF-8"  );
			
			byte[]	encrypted;
			
			{
				Cipher encipher = Cipher.getInstance ("AES/CBC/PKCS5Padding");
					
				encipher.init( Cipher.ENCRYPT_MODE, secret );
					
				AlgorithmParameters params = encipher.getParameters ();
					
				byte[] IV = params.getParameterSpec(IvParameterSpec.class).getIV();
					
				byte[] enc = encipher.doFinal( plain_bytes );

				encrypted = new byte[ IV.length + enc.length ];
				
				System.arraycopy( IV, 0, encrypted, 0, IV.length );
				System.arraycopy( enc, 0, encrypted, IV.length, enc.length );
			}
		
			byte[]	reply_bytes = XMRPCClientUtils.postToURL( url + "?client=true", encrypted );
						
			byte[]	decrypted;
			
			try{
				byte[]	IV = new byte[16];
				
				System.arraycopy( reply_bytes, 0, IV, 0, IV.length );
				
				Cipher decipher = Cipher.getInstance ("AES/CBC/PKCS5Padding");
	
				decipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec( IV ));
				
				decrypted = decipher.doFinal( reply_bytes, 16, reply_bytes.length-16 );
				
			}catch( Throwable e ){
				
				XMRPCClientException error = new XMRPCClientException( "decrypt failed: " + new String( reply_bytes, 0, reply_bytes.length>256?256:reply_bytes.length ), e );
									
				error.setType( XMRPCClientException.ET_CRYPTO_FAILED );
				
				throw( error );
			}
			
			JSONObject reply = new JSONObject();
			
			reply.putAll( JSONUtils.decodeJSON( new String( decrypted, "UTF-8" )));
			
			System.out.println( "Received reply: " + reply);

			calls_ok++;
			
			return( reply );
			
		}catch( XMRPCClientException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new XMRPCClientException( "Failed to use tunnel", e ));
		}
	}
		
	public void
	destroy()
	{
		try{
			Object[] tunnel = getCurrentTunnel( true );

			if ( tunnel != null ){
		
				XMRPCClientUtils.postToURL( (String)tunnel[1] + "?client=true&close=true", new byte[0]);
			}		
		}catch( Throwable e ){	
		}
	}
}
