package hyper.keygen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ws.commons.util.Base64;
import org.cornell.hyper.overlay.MovieEntry.TimedPublicKey;

// TODO: Add method to generate TimedPublicKey and its associate private key to a 
// file given a expiry time and the master private key.
// 
// Modify the crawler so that it reads the TimedPublicKey/private key from a file
// instead of generating it on the fly.
public class Generator {
	public static KeyPair generateKeyPair() {
		KeyPair pair = null;
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
	        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
	        keyGen.initialize(1024, random);
	        pair = keyGen.generateKeyPair();
	        /*
	        String privKey = Base64.encode(pair.getPrivate().getEncoded());
	        String publicKey = Base64.encode(pair.getPublic().getEncoded());
	        System.out.println("Private Key: " + privKey);
	        System.out.println("Public Key: " + publicKey);
	        */	        
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();			
		}
		return pair;
	}
	
	public static TimedPublicKey generateTimedPublicKey(
			PublicKey pubKey, Date dueDate) {
		TimedPublicKey timedKey = new TimedPublicKey(
				pubKey.getEncoded(), dueDate);
		return timedKey;
	}
	
	private static String stripBR(String inString) {
		return inString.replaceAll("\n", "").replaceAll("\r", "");
	}
	
	public static boolean writeTimedKeys(
			String filename, byte[] timedSig, TimedPublicKey timedPub, PrivateKey privKey) {
		
		boolean success = false;
		BufferedWriter fw = null;
		try {
			fw = new BufferedWriter(new FileWriter(filename));				
			fw.write("Signature:" + stripBR(Base64.encode(timedSig)) + "\n");
			fw.write("PublicKey:" + stripBR(Base64.encode(timedPub.getPublicKey())) + "\n");
			fw.write("DueDate:" + timedPub.getExpireDate().getTime() + "\n");
			fw.write("PrivateKey:" + stripBR(Base64.encode(privKey.getEncoded())) + "\n");
			success = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					System.err.println("Failed on file close");
					e.printStackTrace();
				}
			}
		}		
		return success;
	}
	
	public static List<Object> readTimedKeys(String filename) {
		List<Object> keysList = new ArrayList<Object>();
		BufferedReader fr = null;
		String[] strArray = null;
		try {
			fr = new BufferedReader(new FileReader(filename));
			strArray = fr.readLine().split(":", 2);
			if (strArray.length == 2 && strArray[0].equals("Signature")) {
				keysList.add(Base64.decode(strArray[1]));		
			}
			strArray = fr.readLine().split(":", 2);
			if (strArray.length == 2 && strArray[0].equals("PublicKey")) {
				X509EncodedKeySpec pubKeySpec = 
					new X509EncodedKeySpec(Base64.decode(strArray[1]));				
				try {
					KeyFactory keyFactory = KeyFactory.getInstance(MovieAdder.defaultKeyInst);
					PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
					keysList.add(pubKey);
				} catch (NoSuchAlgorithmException e) {
					System.err.println("Cannot parse public key, exiting");
					e.printStackTrace();
					return null;
				} catch (InvalidKeySpecException e) {
					System.err.println("Cannot parse public key, exiting");
					e.printStackTrace();
					return null;
				}
			}
			strArray = fr.readLine().split(":", 2);
			if (strArray.length == 2 && strArray[0].equals("DueDate")) {
				keysList.add(Long.parseLong(strArray[1]));
			}
			strArray = fr.readLine().split(":", 2);
			if (strArray.length == 2 && strArray[0].equals("PrivateKey")) {
				PKCS8EncodedKeySpec privKeySpec = 
					new PKCS8EncodedKeySpec(Base64.decode(strArray[1]));
				try {
					KeyFactory keyFactory = KeyFactory.getInstance(MovieAdder.defaultKeyInst);
					PrivateKey masterPrivKey = keyFactory.generatePrivate(privKeySpec);
					keysList.add(masterPrivKey);
				} catch (NoSuchAlgorithmException e) {
					System.err.println("Cannot parse private key, exiting");
					e.printStackTrace();
					return null;
				} catch (InvalidKeySpecException e) {
					System.err.println("Cannot parse private key, exiting");
					e.printStackTrace();
					return null;
				}	
			}			
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open file");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			System.err.println("IOException: " + e);
			e.printStackTrace();
			return null;
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					System.err.println("Failed on file close");
					e.printStackTrace();
				}
			}
		}
		return keysList;
	}
	
	public static List<Object> GeneratePublisherKey(long timeoutMS) {
		byte[] rawPrivKey = MovieAdder.getMasterPrivate();
		if (rawPrivKey == null) {
			System.err.println("Cannot retrieve private key, exiting");
			return null;
		}
		PKCS8EncodedKeySpec privKeySpec = 
			new PKCS8EncodedKeySpec(rawPrivKey);
		PrivateKey masterPrivKey = null;
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(MovieAdder.defaultKeyInst);
			masterPrivKey = keyFactory.generatePrivate(privKeySpec);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Cannot parse private key, exiting");
			e.printStackTrace();
			return null;
		} catch (InvalidKeySpecException e) {
			System.err.println("Cannot parse private key, exiting");
			e.printStackTrace();
			return null;
		}
					
		// Generate new public/private key pair
		KeyPairGenerator keyGen = null;
		try {
			keyGen = KeyPairGenerator.getInstance("DSA");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			keyGen.initialize(1024, random);			
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Cannot generate keypair, exiting");
			e.printStackTrace();
			return null;
		} catch (NoSuchProviderException e) {
			System.err.println("Cannot generate keypair, exiting");
			e.printStackTrace();
			return null;
		}		
		KeyPair pair = keyGen.generateKeyPair();
		PrivateKey prodPriv = pair.getPrivate();
		PublicKey prodPub = pair.getPublic();
		
		// Have a timeout of 30 days
		//long timeoutMS = 30L * 24L * 60L * 60L * 1000L;
		//System.out.println("Timeout in MS is: " + timeoutMS);
		Date dueDate = new Date(System.currentTimeMillis() + timeoutMS);
		//System.out.println("Due dates is: " + dueDate.getTime());
		//System.out.println("Cur time is: " + new Date().getTime());
		TimedPublicKey timedKey = new TimedPublicKey(prodPub.getEncoded(), dueDate);
				
		// Generate signature for timedKey
		byte[] prodSig = null;
		try {
			Signature dsa = Signature.getInstance("SHA1withDSA");
			dsa.initSign(masterPrivKey);
			/* Update and sign the data */
			dsa.update(timedKey.toByteArray());
			prodSig = dsa.sign();			
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Cannot generate signature for timed key, exiting");
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			System.err.println("Cannot generate signature for timed key, exiting");
			e.printStackTrace();
			return null;
		} catch (SignatureException e) {
			System.err.println("Cannot generate signature for timed key, exiting");
			e.printStackTrace();
			return null;
		}		
		List<Object> keyList = new ArrayList<Object>();		
		keyList.add(prodSig);
		keyList.add(timedKey);
		keyList.add(prodPriv);	
		return keyList;
	}
	
	public static boolean GeneratePublisherKey(long timeoutMS, String outFile) {
		List<Object> keyList = GeneratePublisherKey(timeoutMS);
		if (keyList == null) {
			return false;
		}			
		
		// Get the keys from the keyList
		byte[] prodSig 			= (byte[])keyList.get(0);
		TimedPublicKey pubKey 	= (TimedPublicKey) keyList.get(1);
		PrivateKey privKey 		= (PrivateKey) keyList.get(2);
		
		boolean success = false;
		BufferedWriter fw = null;
		try {
			fw = new BufferedWriter(new FileWriter(outFile));				
			fw.write("Signature:" + stripBR(Base64.encode(prodSig)) + "\n");
			fw.write("PublicKey:" + stripBR(Base64.encode(pubKey.getPublicKey())) + "\n");
			fw.write("DueDate:" + pubKey.getExpireDate().getTime() + "\n");
			fw.write("PrivateKey:" + stripBR(Base64.encode(privKey.getEncoded())) + "\n");
			success = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					System.err.println("Failed on file close");
					e.printStackTrace();
				}
			}
		}		
		return success;
	}
	
//	public static boolean GeneratePublisherKey(long timeoutMS, String outFile) {
//		byte[] rawPrivKey = MovieAdder.getMasterPrivate();
//		if (rawPrivKey == null) {
//			System.err.println("Cannot retrieve private key, exiting");
//			return false;
//		}
//		PKCS8EncodedKeySpec privKeySpec = 
//			new PKCS8EncodedKeySpec(rawPrivKey);
//		PrivateKey masterPrivKey = null;
//		try {
//			KeyFactory keyFactory = KeyFactory.getInstance(MovieAdder.defaultKeyInst);
//			masterPrivKey = keyFactory.generatePrivate(privKeySpec);
//		} catch (NoSuchAlgorithmException e) {
//			System.err.println("Cannot parse private key, exiting");
//			e.printStackTrace();
//			return false;
//		} catch (InvalidKeySpecException e) {
//			System.err.println("Cannot parse private key, exiting");
//			e.printStackTrace();
//			return false;
//		}
//					
//		// Generate new public/private key pair
//		KeyPairGenerator keyGen = null;
//		try {
//			keyGen = KeyPairGenerator.getInstance("DSA");
//			SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
//			keyGen.initialize(1024, random);			
//		} catch (NoSuchAlgorithmException e) {
//			System.err.println("Cannot generate keypair, exiting");
//			e.printStackTrace();
//			return false;
//		} catch (NoSuchProviderException e) {
//			System.err.println("Cannot generate keypair, exiting");
//			e.printStackTrace();
//			return false;
//		}		
//		KeyPair pair = keyGen.generateKeyPair();
//		PrivateKey prodPriv = pair.getPrivate();
//		PublicKey prodPub = pair.getPublic();
//		
//		// Have a timeout of 30 days
//		//long timeoutMS = 30L * 24L * 60L * 60L * 1000L;
//		//System.out.println("Timeout in MS is: " + timeoutMS);
//		Date dueDate = new Date(System.currentTimeMillis() + timeoutMS);
//		//System.out.println("Due dates is: " + dueDate.getTime());
//		//System.out.println("Cur time is: " + new Date().getTime());
//		TimedPublicKey timedKey = new TimedPublicKey(
//				prodPub.getEncoded(), dueDate);
//				
//		// Generate signature for timedKey
//		byte[] prodSig = null;
//		try {
//			Signature dsa = Signature.getInstance("SHA1withDSA");
//			dsa.initSign(masterPrivKey);
//			/* Update and sign the data */
//			dsa.update(timedKey.toByteArray());
//			prodSig = dsa.sign();			
//		} catch (NoSuchAlgorithmException e) {
//			System.err.println("Cannot generate signature for timed key, exiting");
//			e.printStackTrace();
//			return false;
//		} catch (InvalidKeyException e) {
//			System.err.println("Cannot generate signature for timed key, exiting");
//			e.printStackTrace();
//			return false;
//		} catch (SignatureException e) {
//			System.err.println("Cannot generate signature for timed key, exiting");
//			e.printStackTrace();
//			return false;
//		}					
//		return writeTimedKeys(outFile, prodSig, timedKey, prodPriv);		
//	}	
	
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Generator timeoutDays outfile");
			return;
		}
		long timeoutMS = Integer.parseInt(args[0]) * 24L * 60L * 60L * 1000L;
		System.out.println("Timeout length: " + timeoutMS + " ms");
		GeneratePublisherKey(timeoutMS, args[1]);
		/*
		KeyPair pair = generateKeyPair();
		if (pair != null) {
			String privKey = Base64.encode(pair.getPrivate().getEncoded());
			String publicKey = Base64.encode(pair.getPublic().getEncoded());
			System.out.println("Private Key: " + privKey);
			System.out.println("Public Key: " + publicKey);	     
		}
		*/			
	}
}
