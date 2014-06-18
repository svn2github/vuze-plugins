package hyper.keygen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.DecodingException;
import org.cornell.hyper.overlay.AddressPort;
import org.cornell.hyper.overlay.EntryContainer;
import org.cornell.hyper.overlay.HyperNode;
import org.cornell.hyper.overlay.KeyVal;
import org.cornell.hyper.overlay.MovieEntry;
import org.cornell.hyper.overlay.RemoteHyperNode;
import org.cornell.hyper.overlay.MovieEntry.MovieEntryException;
import org.cornell.hyper.overlay.MovieEntry.TimedPublicKey;

public class MovieAdder {
	private static Logger log = Logger.getLogger(MovieAdder.class.getName());
	public static int DefaultNumReplica 			= 8;
	protected final static String defaultKeyInst	= "DSA";
	protected final static String defaultSigType	= "SHA1withDSA";
	
	protected static byte[] getMasterPrivate() {
		final String MasterPrivateKey = "PrivKey";
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle(
				"hyper.keygen.MasterPrivate");
		} catch (MissingResourceException e) {
			System.err.println("Cannot find MasterPrivate.properties");
			return null;
		}
		String keyStr = rb.getString(MasterPrivateKey);
		if (keyStr == null) {
			System.err.println("Cannot find PrivKey in properties file");
			return null;
		}
		try {
			//System.out.println("PrivateKey is: " + keyStr);
			return Base64.decode(keyStr);
		} catch (DecodingException e) {
			System.err.println("Cannot base64 decode string");
		}
		return null;
	}	
	
	protected static Set<MovieEntry> parseFile(String fileName, 
			byte[] inProdSig, TimedPublicKey inProdPubKey, byte[] prodPrivKey) {		
		Set<MovieEntry> movieSet = new HashSet<MovieEntry>();
		final String torrentKey = "torrent_name";
		final String commentKey = "comment_str";
		final String urlKey		= "url"; 
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			Map<String, String> curTuple = new HashMap<String, String>();
			while (true) {
				String curLine = in.readLine();
				if (curLine == null) {
					break;	// End of file
				}
				String[] strArray = curLine.split("=", 2);							
				if (strArray.length == 2) {
					curTuple.put(strArray[0], strArray[1]);
				}
				if (curTuple.containsKey(torrentKey) &&
					curTuple.containsKey(commentKey) &&
					curTuple.containsKey(urlKey)) {
					try {
						MovieEntry curMovie = new MovieEntry(
								inProdSig, inProdPubKey, prodPrivKey, 
								curTuple.get(commentKey), curTuple.get(urlKey), 
								curTuple.get(torrentKey));
						movieSet.add(curMovie);
					} catch (MovieEntryException e) {
						log.error("Error generating movie entry, skipping");
						e.printStackTrace();
					}
					curTuple.clear();
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return movieSet;
	}
	
	public static void main(String[] args) {
		// Configure the log system
		BasicConfigurator.configure();		
		log.setLevel(Level.INFO);

		// Set the XML-RPC logger to log only warnings and up.
		Logger xmlServerLog = Logger.getLogger(
			"org.apache.xmlrpc.server.XmlRpcStreamServer");
		xmlServerLog.setLevel(Level.INFO);

		// Set the XML-RPC logger to log only warnings and up.
		Logger xmlClientLog = Logger.getLogger(
			"org.cornell.hyper.overlay.XmlRPCClient");
		xmlClientLog.setLevel(Level.INFO);

		EntryContainer entryContainer = new EntryContainer(
				HyperNode.getPunct(), HyperNode.getCommonWords());
	
		
		int argCounter = 0;
		
		List<Object> pubKeyList = Generator.readTimedKeys(args[argCounter++]);
		if (pubKeyList.size() != 4) {
			log.error("Cannot read timed public key file");
			return;
		}
		byte[] prodSig 		= null;
		PublicKey prodPub 	= null;
		long dueDateMS		= 0;
		PrivateKey prodPriv = null;
		try {
			prodSig 	= (byte[]) pubKeyList.get(0);
			prodPub 	= (PublicKey) pubKeyList.get(1);
			dueDateMS	= ((Long) pubKeyList.get(2)).longValue();
			prodPriv 	= (PrivateKey) pubKeyList.get(3);		
		} catch (ClassCastException e) {
			log.error("Unexpected data in public key file, exiting");
			return;
		}		
		
		// Grab the movies
		//Map<String, String> movies = HyperNode.parseFile(args[argCounter++]);
		String movieFilename = args[argCounter++];
		
		// Grab the seed addresses
		ArrayList<AddressPort> seedAddrs = new ArrayList<AddressPort>();
		for (int i = argCounter; i < args.length; i++) {
			String curStr = args[i];
			String[] addrPort = curStr.split(":");
			if (addrPort.length == 2) {
				try {
					InetAddress remoteAddr = InetAddress.getByName(addrPort[0]);
					String addStr = remoteAddr.getHostAddress();
					int port = Integer.parseInt(addrPort[1]);
					seedAddrs.add(new AddressPort(addStr, port));
					log.info("Remote node: " + addStr + ":" + port);
				} catch (UnknownHostException e) {
					log.warn("Cannot use seednode: " + curStr);					
				} catch (NumberFormatException e) {
					log.warn("Cannot use seednode: " + curStr);				
				}
			}
		}			
		
		//	TODO: May need to add a log.properties file to remove debugging info
		//	from xml-rpc. Look at 
		//	http://www.johnmunsch.com/projects/Presentations/docs/Log4J/log.properties
		Set<String> defaultKW = new HashSet<String>();
		Iterator<String> movieIt = HyperNode.getDefaultMovies().iterator();		
		while (movieIt.hasNext()) {
			String[] curKey = entryContainer.removeSymbols(movieIt.next());
			ArrayList<String> curList = new ArrayList<String>();
			for (int i = 0; i < curKey.length; i++) {
				curList.add(curKey[i]);
			}
			defaultKW.addAll(entryContainer.normalizeKeywords(curList));
		}

		KeyVal<String, Set<RemoteHyperNode>> curKeyVal = 
			HyperNode.findKeyAndSeeds(seedAddrs, defaultKW);		
		if (curKeyVal == null) {
			System.out.println("Cannot access any seed nodes, exiting");
		}
		//String keyword 				= curKeyVal.getKey();
		Set<RemoteHyperNode> seedNodes 	= curKeyVal.getVal();		
		
		//log.info("Timeout in MS is: " + timeoutMS);
		//Date dueDate = new Date(System.currentTimeMillis() + timeoutMS);
		Date dueDate = new Date(dueDateMS);
		log.info("Due date is: " + dueDate.getTime());
		//log.info("Cur time is: " + new Date().getTime());
		//log.info("Generating TimedPublicKey...");
		TimedPublicKey timedKey = 
				new TimedPublicKey(prodPub.getEncoded(), dueDate);
		log.info("done");		
		
		Set<MovieEntry> moviesSet = parseFile(movieFilename, 
				prodSig, timedKey, prodPriv.getEncoded());
		
		Iterator<MovieEntry> setIt = moviesSet.iterator();
		while (setIt.hasNext()) {
			MovieEntry curMovie = setIt.next();
			log.info("Adding movie: " + curMovie.getTorName());
			HyperNode.insertEntry(entryContainer, 
					seedNodes, DefaultNumReplica, curMovie);	
		}
	}
}
