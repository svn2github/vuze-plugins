/******************************************************************************
Cubit distribution
Copyright (C) 2008 Bernard Wong

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

The copyright owner can be contacted by e-mail at bwong@cs.cornell.edu
*******************************************************************************/

package org.cornell.hyper.overlay;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.DecodingException;

// TODO: If a single node receives two movie entries that are exactly the
// same exact for the timeout, only keep the one with the longer timeout
// and throw out the other.
public class MovieEntry {
	//protected final static String defaultEncode 	= "US-ASCII";
	protected final static String defaultEncode 	= "UTF-8";
	protected final static String defaultKeyInst	= "DSA";
	protected final static String defaultSigType	= "SHA1withDSA";
	
	public static class MovieEntryException extends Exception {
		private static final long serialVersionUID = -7425634690572522977L;	
	}
	
	public static class TimedException extends Exception {
		private static final long serialVersionUID = -5065222072601947255L;		
	}
	
	protected static byte[] getMasterPublic() {
		final String MasterPublicKey = "PubKey";
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle(
				"org.cornell.hyper.overlay.MasterPublic");
		} catch (MissingResourceException e) {
			System.err.println("Cannot find MasterPublic.properties");
			return null;
		}
		String keyStr = rb.getString(MasterPublicKey);
		if (keyStr == null) {
			System.err.println("Cannot find PubKey in properties file");
			return null;
		}
		try {
			return Base64.decode(keyStr);
		} catch (DecodingException e) {
			System.err.println("Cannot base64 decode string");
		}
		return null;
	}
		
	public static class TimedPublicKey {
		private byte[]	publicKey;
		private Date	expireDate;
					
		public TimedPublicKey(byte[] publicKey, String expireDate) 
				throws TimedException {
			this.publicKey 	= publicKey.clone();
			try {
				Long dateLong = Long.parseLong(expireDate);
				this.expireDate = new Date(dateLong);
			} catch (NumberFormatException e) {
				throw new TimedException();
			}
		}
		
		public TimedPublicKey(byte[] publicKey, Date expireDate) {
			this.publicKey 	= publicKey.clone();
			this.expireDate = new Date(expireDate.getTime());			
		}
		
		public byte[] getPublicKey() {
			return publicKey;
		}
		
		public Date getExpireDate() {
			return expireDate;
		}
		
		public boolean isExpired() {
			//System.out.println("Expire date: " + getExpireDate());
			//System.out.println("Current date: " + (new Date()));			
			return getExpireDate().before(new Date());
			//return false;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof TimedPublicKey)) {
				return false;		
			}
			TimedPublicKey otherEntry = (TimedPublicKey)o;		
			if (Arrays.equals(getPublicKey(), otherEntry.getPublicKey()) &&
				getExpireDate().getTime() == otherEntry.getExpireDate().getTime()) {
				return true;
			}
			return false;
		}
		
		public int hashCode() {
			return Arrays.hashCode(getPublicKey()) ^ (int)getExpireDate().getTime();
		}
		
		public byte[] toByteArray() {
			byte[] timeArray = null;			
			try {
				timeArray = String.valueOf(
					expireDate.getTime()).getBytes(defaultEncode);
			} catch (UnsupportedEncodingException e) {
				System.err.println("Cannot encode string");
				return null;
			}
			byte[] retResult = new byte[publicKey.length + timeArray.length];
			int byteCount = 0;
			for (int i = 0; i < publicKey.length; i++) {
				retResult[byteCount++] = publicKey[i];
			}
			for (int i = 0; i < timeArray.length; i++) {
				retResult[byteCount++] = timeArray[i];
			}
			return retResult;
		}		
		
		public Object[] xmlMarshal() {
			Object[] objArray = new Object[2];
			objArray[0] = Base64.encode(getPublicKey());
			objArray[1] = String.valueOf(getExpireDate().getTime());
			return objArray;
		}
		
		public static TimedPublicKey xmlUnMarshal(Object o) 
				throws TimedException {
			if (!(o instanceof Object[])) {
				return null;
			}
			Object[] objArray = (Object[]) o;
			if (objArray.length != 2 				||
				!(objArray[0] instanceof String) 	||
				!(objArray[1] instanceof String)) {
				return null;
			}			
			try {
				byte[] decodedKey = Base64.decode((String)objArray[0]);
				return new TimedPublicKey(decodedKey, (String)objArray[1]);
			} catch (DecodingException e) {
				System.err.println("Cannot base64 decode string");
			}
			return null;
		}
	}
	
	public static class MovieEntryLite {
		private String movieName;
		private String magnetLink;
		private String torrentName;
		
		public MovieEntryLite(
				String movieName, String magnetLink, String torrentName) {
			this.movieName 		= movieName;
			this.magnetLink		= magnetLink;
			this.torrentName	= torrentName;			
		}
		
		public String getMovName()	{ return movieName;		}		
		public String getMagLink() 	{ return magnetLink; 	}
		public String getTorName()	{ return torrentName;	}
		
		public boolean equals(Object o){
			if (!(o instanceof MovieEntryLite)) {
				return false;		
			}
			MovieEntryLite otherEntry = (MovieEntryLite)o;		
			if (getMovName().equals(otherEntry.getMovName()) &&
				getMagLink().equals(otherEntry.getMagLink()) &&
				getTorName().equals(otherEntry.getTorName())) {
				return true;
			}
			return false;
		}
		
		public int hashCode() {
			return getMovName().hashCode() ^ 
					getMagLink().hashCode() ^ getTorName().hashCode();
		}
		
		public byte[] toByteArray() {
			byte[] movA = null;
			byte[] magA = null;
			byte[] torA = null;
			try {
				movA = getMovName().getBytes(defaultEncode);
				magA = getMagLink().getBytes(defaultEncode);
				torA = getTorName().getBytes(defaultEncode);
			} catch (UnsupportedEncodingException e) {
				System.err.println("Cannot encode string");
				return null;
			}
			
			// Combine all the arrays together into this one
			byte[] retArray = 
				new byte[movA.length + magA.length + torA.length];
			
			int retCount = 0;
			for (int i = 0; i < movA.length; i++) {
				retArray[retCount++] = movA[i];
			}
			for (int i = 0; i < magA.length; i++) {
				retArray[retCount++] = magA[i];
			}
			for (int i = 0; i < torA.length; i++) {
				retArray[retCount++] = torA[i];
			}
			return retArray;
		}

		public Object[] xmlMarshal() {
			Object[] objArray = new Object[3];
			objArray[0] = getMovName();
			objArray[1] = getMagLink();
			objArray[2] = getTorName();
			return objArray;
		}
		
		public static MovieEntryLite xmlUnMarshal(Object o) {
			if (!(o instanceof Object[])) {
				return null;
			}
			Object[] objArray = (Object[]) o;
			if (objArray.length != 3 				||
				!(objArray[0] instanceof String) 	||
				!(objArray[1] instanceof String)	||
				!(objArray[2] instanceof String)) {
				return null;
			}		
			return new MovieEntryLite((String)objArray[0], 
					(String)objArray[1], (String)objArray[2]);			
		}
	}	
	
	private byte[] 			producerSig		= null;
	private TimedPublicKey	producerPubKey	= null;
	private byte[] 			datumSig		= null;
	private MovieEntryLite 	datum			= null;	
	
	private static boolean verifySig(
			byte[] rawPublicKey, byte[] rawSignature, byte[] data) {
		/*
		X509EncodedKeySpec pubKeySpec = 
			new X509EncodedKeySpec(rawPublicKey);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(defaultKeyInst);
		    PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);	
		    Signature sig = Signature.getInstance(defaultSigType);
		    sig.initVerify(pubKey);
		    sig.update(data);
		    return sig.verify(rawSignature);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return false;
		*/
		return true;
	}
	
	public MovieEntry(
				byte[] inProdSig,				// Producer signature	 
				TimedPublicKey inProdPubKey, 	// Producer's public-key/expiry-time
				byte[] prodPrivKey, 			// Producer's private key
				String movieName, 				// Torrent's comments
				String magnetLink, 				// Torrent link
				String movieAdd) 				// Actual torrent name 
			throws MovieEntryException {
		// Copy the signature file
		producerSig = inProdSig.clone();
		
		// And the producer's public-key/expiry-time
		producerPubKey = new TimedPublicKey(
				inProdPubKey.getPublicKey(), 
				inProdPubKey.getExpireDate());
		
		// Create a MovieEntryLite
		datum = new MovieEntryLite(movieName, magnetLink, movieAdd);
		
		// Fetch the Master Public key from the properties file
		byte[] masterPub = getMasterPublic();
		if (masterPub == null) {			
			throw new MovieEntryException();
		}
		
		// Check to see that the producer's public key is still valid
		if (producerPubKey.isExpired()) {
			System.err.println("TimedPublicKey expired");
			throw new MovieEntryException();
		}
		
		// Verify that producerPubKey has not been tampered
		if (!verifySig(masterPub, 
				producerSig, producerPubKey.toByteArray())) {
			System.err.println("Cannot verify the TimedPublicKey");
			throw new MovieEntryException();	
		}
			    
	    // Create signature for datum
		try {
			Signature dsa = Signature.getInstance(defaultSigType);
			PKCS8EncodedKeySpec privKeySpec =
				new PKCS8EncodedKeySpec(prodPrivKey);
		    KeyFactory keyFactory = KeyFactory.getInstance(defaultKeyInst);
		    PrivateKey privKey = keyFactory.generatePrivate(privKeySpec);
		    dsa.initSign(privKey);
		    dsa.update(datum.toByteArray());
		    datumSig = dsa.sign();			
		} catch (NoSuchAlgorithmException e) {
			throw new MovieEntryException();
		} catch (InvalidKeySpecException e) {
			throw new MovieEntryException();
		} catch (InvalidKeyException e) {
			throw new MovieEntryException();
		} catch (SignatureException e) {
			throw new MovieEntryException();
		}
	}
	
	private MovieEntry(
				byte[] curProducerSig, 
				TimedPublicKey curProducerPub,
				byte[] curDatumSig,
				MovieEntryLite curDatum) 
			throws MovieEntryException {
		this.producerSig 	= curProducerSig;
		this.producerPubKey	= curProducerPub;
		this.datumSig		= curDatumSig;
		this.datum			= curDatum;
		// Check to see that the producer's public key is still valid
		if (producerPubKey.isExpired()) {
			System.err.println("TimedPublicKey expired");
			throw new MovieEntryException();
		}		
		if (!verify()) {
			throw new MovieEntryException();
		}		
	}
		
	public String getMovName()	{ return datum.getMovName(); }		
	public String getMagLink() 	{ return datum.getMagLink(); }	
	public String getTorName()	{ return datum.getTorName(); }
	
	public Date getExpireDate()	{ return producerPubKey.expireDate; }
		
	public boolean verify() {
		// Fetch the Master Public key from the properties file
		byte[] masterPub = getMasterPublic();
		if (masterPub == null) {
			return false;
		}		
		// Verify that producerPubKey has not been tampered
		if (!verifySig(masterPub, 
				producerSig, producerPubKey.toByteArray())) {
			return false;	
		}
		// Verify that the datum has not been tampered
		if (!verifySig(producerPubKey.getPublicKey(),
				datumSig, datum.toByteArray())) {
			return false;
		}
		return true;	
	}
		
	public boolean equals(Object o) {
		if (!(o instanceof MovieEntry)) {
			return false;		
		}
		MovieEntry otherEntry = (MovieEntry)o;
		// Don't include the public key (specifically the expiry date) 
		// when comparing MovieEntry objects for equality
		if (//Arrays.equals(producerSig, otherEntry.producerSig) 	&&
			//producerPubKey.equals(otherEntry.producerPubKey) 	&&
			//Arrays.equals(datumSig, otherEntry.datumSig) 		&&
			datum.equals(otherEntry.datum)) {
			return true;
		}
		return false;	
	}
	
	public int hashCode() {
		//return //Arrays.hashCode(producerSig) ^ 
			// producerPubKey.hashCode() ^
			// Arrays.hashCode(datumSig) ^ datum.hashCode(); 
		return datum.hashCode();
	}
		
	public Object[] xmlMarshal() {
		Object[] objArray = new Object[4];
		objArray[0] = Base64.encode(producerSig);
		objArray[1] = producerPubKey.xmlMarshal();
		objArray[2] = Base64.encode(datumSig);
		objArray[3] = datum.xmlMarshal();		
		return objArray;
	}
	
	public static MovieEntry xmlUnMarshal(Object o) {
		if (!(o instanceof Object[])) {
			return null;
		}
		Object[] objArray = (Object[]) o;
		if (objArray.length != 4 				||
			!(objArray[0] instanceof String) 	||
			!(objArray[1] instanceof Object[])	||
			!(objArray[2] instanceof String)	||
			!(objArray[3] instanceof Object[])) {
			return null;
		}		
		// Retrieve the movie entry items 
		byte[] curProducerSig 			= null;
		TimedPublicKey curProducerPub 	= null;
		byte[] curDatumSig				= null;
		MovieEntryLite curDatum			= null;		
		MovieEntry curMovieEntry		= null;
		try {
			curProducerSig = Base64.decode((String) objArray[0]);
			curProducerPub = TimedPublicKey.xmlUnMarshal((Object)objArray[1]);
			if (curProducerPub == null) {
				return null;
			}
			curDatumSig = Base64.decode((String) objArray[2]);
			curDatum = MovieEntryLite.xmlUnMarshal((Object)objArray[3]);
			if (curDatum == null) {
				return null;
			}
			// Try to create a MovieEntry. The act of creation automatically
			// tries to verify the signature.
			curMovieEntry = new MovieEntry(
					curProducerSig, curProducerPub, curDatumSig, curDatum);			
		} catch (DecodingException e) 	{
		} catch (TimedException e) 		{
		} catch (MovieEntryException e) {			
		}
		return curMovieEntry;
	}
}
