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

package com.aelitis.azureus.plugins.jpc.license.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;


import java.util.*;

import com.aelitis.azureus.plugins.jpc.JPCException;
import com.aelitis.azureus.plugins.jpc.license.JPCLicense;
import com.aelitis.azureus.plugins.jpc.license.JPCLicenseEntry;
import com.aelitis.azureus.plugins.jpc.license.JPCLicenseVerifier;

/**
 * @author parg
 *
 */

public class 
JPCLicenseVerifierImpl
	implements JPCLicenseVerifier
{
	private static final byte[] raw_key = {(byte)0xF5, (byte)0x26, (byte)0x5E, (byte)0x1B, (byte)0x1F, (byte)0xFC, (byte)0x63, (byte)0x71,  (byte)0x8B, (byte)0xB1, (byte)0x64, (byte)0x74, (byte)0xA8, (byte)0x7A, (byte)0x24, (byte)0x85,  (byte)0xF6, (byte)0x49, (byte)0xF0, (byte)0xF9, (byte)0xB5, (byte)0x8F, (byte)0x85, (byte)0xD5,  (byte)0xC9, (byte)0x2E, (byte)0x0F, (byte)0x55, (byte)0x9B, (byte)0xBE, (byte)0xD7, (byte)0x45,  (byte)0x77, (byte)0xC7, (byte)0xC1, (byte)0x9A, (byte)0x58, (byte)0xDF, (byte)0x93, (byte)0x51,  (byte)0x21, (byte)0xA4, (byte)0x6F, (byte)0x43, (byte)0xD8, (byte)0x7A, (byte)0xAC, (byte)0xB7,  (byte)0xF8, (byte)0x7C, (byte)0xDD, (byte)0x00, (byte)0xCF, (byte)0x49, (byte)0xDC, (byte)0x6E,  (byte)0x59, (byte)0x4B, (byte)0xC0, (byte)0x84, (byte)0x48, (byte)0x72, (byte)0xD8, (byte)0xA3,  (byte)0x4D, (byte)0x00, (byte)0xBB, (byte)0x4B, (byte)0xD5, (byte)0x5F, (byte)0x82, (byte)0x5F,  (byte)0xA8, (byte)0xEE, (byte)0xC9, (byte)0xA9, (byte)0x8D, (byte)0x05, (byte)0xA5, (byte)0xF7,  (byte)0x57, (byte)0x82, (byte)0xD3, (byte)0x40, (byte)0x7B, (byte)0x66, (byte)0xD3, (byte)0xD0,  (byte)0x5F, (byte)0x2E, (byte)0x36, (byte)0x4B, (byte)0xC5, (byte)0x80, (byte)0xFC, (byte)0x8A,  (byte)0x81, (byte)0x59, (byte)0x2B, (byte)0x1B, (byte)0x04, (byte)0x0B, (byte)0xAC, (byte)0xE5,  (byte)0x3E, (byte)0xCF, (byte)0xB3, (byte)0x36, (byte)0x74, (byte)0x62, (byte)0x61, (byte)0x41,  (byte)0xF2, (byte)0x8F, (byte)0x44, (byte)0x5E, (byte)0x83, (byte)0xFF, (byte)0xDB, (byte)0x56,  (byte)0x39, (byte)0xA9, (byte)0xF2, (byte)0x7F, (byte)0x4B, (byte)0x10, (byte)0xB1, (byte)0x92,  (byte)0x35, (byte)0xEA, (byte)0x03, (byte)0x86, (byte)0xAB, (byte)0x79, (byte)0xAE, (byte)0xFB,  (byte)0x74, (byte)0x0C, (byte)0x91, (byte)0xCE, (byte)0xC2, (byte)0x4E, (byte)0x56, (byte)0x28,  (byte)0xD8, (byte)0x4F, (byte)0x4B, (byte)0x4E, (byte)0xFC, (byte)0x6E, (byte)0xB7, (byte)0xF7,  (byte)0x16, (byte)0x69, (byte)0x30, (byte)0x4B, (byte)0x99, (byte)0x68, (byte)0xBF, (byte)0x0B,  (byte)0x54, (byte)0x60, (byte)0x65, (byte)0x20, (byte)0x8E, (byte)0xA7, (byte)0x55, (byte)0x2C,  (byte)0xB6, (byte)0xBD, (byte)0x45, (byte)0xEE, (byte)0xC9, (byte)0x7A, (byte)0xB6, (byte)0xA8,  (byte)0xCC, (byte)0x1F, (byte)0x74, (byte)0x0B, (byte)0x43, (byte)0x3E, (byte)0x36, (byte)0xB0,  (byte)0x4C, (byte)0x66, (byte)0xCE, (byte)0x6A, (byte)0xBD, (byte)0x88, (byte)0x30, (byte)0x38,  (byte)0x13, (byte)0xE8, (byte)0xDA, (byte)0x49, (byte)0xCF, (byte)0x21, (byte)0x9C, (byte)0x7C,  (byte)0x81, (byte)0x0F, (byte)0x86, (byte)0x94, (byte)0x38, (byte)0x0D, (byte)0x88, (byte)0x56,  (byte)0xAD, (byte)0xEF, (byte)0xE3, (byte)0x6C, (byte)0x1D, (byte)0x34, (byte)0x83, (byte)0xE9,  (byte)0x1A, (byte)0xE7, (byte)0x78, (byte)0x4D, (byte)0x53, (byte)0xAF, (byte)0x39, (byte)0x55,  (byte)0xC4, (byte)0x6B, (byte)0x94, (byte)0x3B, (byte)0x3F, (byte)0xD9, (byte)0xB1, (byte)0x6C,  (byte)0x36, (byte)0xFC, (byte)0x08, (byte)0x31, (byte)0x4A, (byte)0xF2, (byte)0x6A, (byte)0xB7,  (byte)0xB7, (byte)0xB2, (byte)0xF9, (byte)0x82, (byte)0xC7, (byte)0x65, (byte)0xFD, (byte)0x94,  (byte)0x15, (byte)0x1E, (byte)0xE7, (byte)0x47, (byte)0x08, (byte)0x90, (byte)0x9A, (byte)0xCE };

	public JPCLicense
	verify(
		byte[]		content )
	
		throws JPCException
	{
			// content consists of two parts.
			// 1) data
			// 2) last 256 bytes of the file - this is the signature for the data

		int	signature_length 	= 256;

		if ( content.length < signature_length ){
			
			throw( new JPCException( "content is too short, must be at least 256 bytes" ));
		}

		int	data_length			= content.length - signature_length;
		
		try{			
			byte[]	signature = new byte[signature_length];
			
			System.arraycopy( content, content.length - signature_length, signature, 0, signature_length );

			/* from http://www.di-mgt.com.au/rsa_alg.html
			Uses sender A's public key (n, e) to compute integer v = s^e mod n. 
			Extracts the message digest from this integer. 
			Independently computes the message digest of the information that has been signed. 
			If both message digests are identical, the signature is valid. 
			*/
			
			BigInteger	sig = decodeBigInteger( signature, true );
			
			BigInteger	exponent 	= new BigInteger( "3", 16 );
			
			BigInteger	modulus		= decodeBigInteger( raw_key, true );
			
			BigInteger 	dec_sig 	= sig.modPow( exponent, modulus);
				
			byte[]		dec_bytes	= encodeBigInteger( dec_sig );
			
			MessageDigest	d = MessageDigest.getInstance("MD5");
			
			d.update(content, 0, data_length);
			
			byte[]  digest_bytes = d.digest();
			
			/*
			System.out.println( "sig    :" + sig.toString(16));
			System.out.println( "exp    :" + exponent.toString(16));
			System.out.println( "modulus:" + modulus.toString(16));
			System.out.println( "digest :" + ByteFormatter.nicePrint(digest_bytes));
			System.out.println( "dec_sig:" + dec_sig.toString(16));
			System.out.println( "ds_byte:" + ByteFormatter.nicePrint( dec_bytes ));
			*/
			
			for (int i=0;i<digest_bytes.length;i++){
				
				if ( digest_bytes[i] != dec_bytes[dec_bytes.length-(i+1)]){
					
					throw( new JPCException( "Signature verification fails" ));
				}
			}

		
			String	content_str = new String( content, 0, content.length-256 );
			
			List	entries = new ArrayList();
			
			int	pos = 0;
			
			while( pos < content_str.length()){
			
				int	p1 = content_str.indexOf( '\n', pos );
				
				final String	bit;
				
				if ( p1 == -1 ){
					
					bit = content_str.substring(pos).trim();
					
				}else{
					
					bit = content_str.substring(pos,p1).trim();
				}
				
				final int	eq = bit.indexOf( '=' );
				
				if ( eq == -1 ){
					
					throw( new JPCException( "Invalid license file - '=' missing at position " + pos ));
				}
				
				entries.add(
					new JPCLicenseEntry()
					{
						public String
						getName()
						{
							return( bit.substring(0,eq).trim());
						}
						
						public String
						getValue()
						{
							return( bit.substring(eq+1).trim());
						}
					});
				
				if ( p1 == -1 ){
					
					break;
				}
				
				pos	= p1+1;
			}
				
			final JPCLicenseEntry[]	entry_array = new JPCLicenseEntry[ entries.size()];
			
			entries.toArray( entry_array );
			
			return( 
				new JPCLicense()
				{
					public JPCLicenseEntry[]
					getEntries()
					{
						return( entry_array );
					}
				});
			
		}catch( JPCException e ){
			
			throw ( e );
			
		}catch( Throwable e ){
			
			throw( new JPCException( "Signature verification fails", e ));
		}
	}
	
	/*
	 * Unfortunately we can't use the built-in stuff, we need to do it by hand :(
	 * 
	protected RSAPublicKey
	getPublicKey()
	
		throws Exception
	{
			// test key!
		
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
				
		random.setSeed(System.currentTimeMillis());
				
		keyGen.initialize( 2048, random );
				
		KeyPair pair = keyGen.generateKeyPair();
		
		RSAPublicKey pub = (RSAPublicKey)pair.getPublic();
	
		System.out.println( "mod = " + pub.getModulus().toString(16));
		
		System.out.println( "exp = " + pub.getPublicExponent().toString(16));
		
		return( pub );
	
						
		KeyFactory key_factory = KeyFactory.getInstance("RSA");
		
		BigInteger	exponent 	= new BigInteger("3",16);
		
		BigInteger	modulus		= decodeBigIntegerReverse( raw_key );
		
		RSAPublicKeySpec key_spec = new RSAPublicKeySpec( modulus, exponent );
	
		RSAPublicKey	key = (RSAPublicKey)key_factory.generatePublic( key_spec );
				
		return( key );
		
		Signature rsa_md5_signature = Signature.getInstance("MD5withRSA"); 
		
		RSAPublicKey	key = getPublicKey();
		
		rsa_md5_signature.initVerify( key );
		
		rsa_md5_signature.update( data );
					
		if ( !rsa_md5_signature.verify( signature )){
			
			throw( new JPCException( "Signature verification fails" ));
		}
	}
	*/
	
	protected BigInteger
	decodeBigInteger(
		byte[]		data,
		boolean		reverse )
	{
		String	str_key = "";
		
		for (int i=0;i<data.length;i++){
			
			String	hex = Integer.toHexString( data[i]&0xff );
			
			while( hex.length() < 2 ){
				
				hex = "0" + hex;
			}
			
			if ( reverse ){
				
				str_key = hex + str_key;

			}else{
				
				str_key += hex;
			}
		}
				
		BigInteger	res		= new BigInteger( str_key, 16 );	
		
		return( res );
	}
	
	protected byte[]
	encodeBigInteger(
		BigInteger	num )
	{
		String	str = num.toString( 16 );
		
			// pad on LHS if not mult of two
				
		if ( str.length()%2 == 1 ){
		
			str = "0" + str;
		}
		
		byte[]	res = new byte[ str.length()/2 ];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = (byte)Integer.parseInt( str.substring(i*2,i*2+2), 16 );
		}
		
		return( res );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
  			InputStream is = 
  				JPCLicenseVerifierImpl.class.getClassLoader().getResourceAsStream(
  					"com/aelitis/azureus/plugins/jpc/license/impl/test_license.dat");

  			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
  			
  			while( true ){
  				byte[]	buffer = new byte[65536];
  				
  				int	len = is.read( buffer );
  				
  				if ( len <= 0 ){
  					
  					break;
  				}
  				
  				baos.write( buffer, 0, len );
  			}
  			
  			JPCLicense	license = new JPCLicenseVerifierImpl().verify( baos.toByteArray());
  			
  			JPCLicenseEntry[]	entries = license.getEntries();
  			
  			for (int i=0;i<entries.length;i++){
  				
  				System.out.println( "entry: " + entries[i].getName() + " -> " + entries[i].getValue());
  			}
  			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}	
}
