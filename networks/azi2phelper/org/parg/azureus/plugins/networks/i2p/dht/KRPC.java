package org.parg.azureus.plugins.networks.i2p.dht;

/*
 *  GPLv2
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net.i2p.I2PAppContext;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.I2PSessionMuxedListener;
import net.i2p.client.SendMessageOptions;
import net.i2p.client.datagram.I2PDatagramDissector;
import net.i2p.client.datagram.I2PDatagramMaker;
import net.i2p.client.datagram.I2PInvalidDatagramException;
import net.i2p.data.DataFormatException;
import net.i2p.data.DataHelper;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;
import net.i2p.util.SimpleTimer2;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SystemTime;
import org.klomp.snark.TrackerClient;
import org.klomp.snark.bencode.BDecoder;
import org.klomp.snark.bencode.BEncoder;
import org.klomp.snark.bencode.BEValue;
import org.klomp.snark.bencode.InvalidBEncodingException;
import org.parg.azureus.plugins.networks.i2p.I2PHelperAdapter;


/**
 * Standard BEP 5
 * Mods for I2P:
 * <pre>
 * - The UDP port need not be pinged after receiving a PORT message.
 *
 * - The UDP (datagram) port listed in the compact node info is used
 *   to receive repliable (signed) datagrams.
 *   This is used for queries, except for announces.
 *   We call this the "query port".
 *   In addition to that UDP port, we use a second datagram
 *   port equal to the signed port + 1. This is used to receive
 *   unsigned (raw) datagrams for replies, errors, and announce queries..
 *   We call this the "response port".
 *
 * - Compact peer info is 32 bytes (32 byte SHA256 Hash)
 *   instead of 4 byte IP + 2 byte port. There is no peer port.
 *
 * - Compact node info is 54 bytes (20 byte SHA1 Hash + 32 byte SHA256 Hash + 2 byte port)
 *   instead of 20 byte SHA1 Hash + 4 byte IP + 2 byte port.
 *   Port is the query port, the response port is always the query port + 1.
 *
 * - The trackerless torrent dictionary "nodes" key is a list of
 *   32 byte binary strings (SHA256 Hashes) instead of a list of lists
 *   containing a host string and a port integer.
 * </pre>
 *
 * Questions:
 *   - nodes (in the find_node and get_peers response) is one concatenated string, not a list of strings, right?
 *   - Node ID enforcement, keyspace rotation?
 *
 * @since 0.9.2
 * @author zzz
 */
public class KRPC implements I2PSessionMuxedListener, DHT {

    private final I2PAppContext _context;
    private final Log _log;
    private final I2PHelperAdapter	_adapter;
    private boolean force_bootstrap;
    
    /** our tracker */
    private final DHTTracker _tracker;
    /** who we know */
    private final DHTNodes _knownNodes;
    /** index to sent queries awaiting reply */
    private final ConcurrentHashMap<MsgID, ReplyWaiter> _sentQueries;
    /** index to outgoing tokens we generated, sent in reply to a get_peers query */
    private final ConcurrentHashMap<Token, NodeInfo> _outgoingTokens;
    /** index to incoming opaque tokens, received in a peers or nodes reply */
    private final ConcurrentHashMap<NID, Token> _incomingTokens;
    /** recently unreachable, with lastSeen() as the added-to-blacklist time  */
    private final Set<NID> _blacklist;

    /** hook to inject and receive datagrams */
    private final I2PSession _session;
    /** 20 byte random id */
    private final byte[] _myID;
    /** 20 byte random id */
    private final NID _myNID;
    /** 20 byte random id + 32 byte Hash + 2 byte port */
    private final NodeInfo _myNodeInfo;
    /** unsigned dgrams */
    private final int _rPort;
    /** signed dgrams */
    private final int _qPort;
    private final File _dhtFile;
    private final File _backupDhtFile;
    private volatile boolean _isRunning;
    private volatile boolean _hasBootstrapped;
    /** stats */
    private final AtomicLong _rxPkts = new AtomicLong();
    private final AtomicLong _txPkts = new AtomicLong();
    private final AtomicLong _rxBytes = new AtomicLong();
    private final AtomicLong _txBytes = new AtomicLong();
    private long _started;

    /** all-zero NID used for pings */
    public static final NID FAKE_NID = new NID(new byte[NID.HASH_LENGTH]);

    /** Max number of nodes to return. BEP 5 says 8 */
    private static final int K = 8;
    /** Max number of peers to return. BEP 5 doesn't say. We'll use the same as I2PSnarkUtil.MAX_CONNECTIONS */
    private static final int MAX_WANT = 16;

    /** overloads error codes which start with 201 */
    private static final int REPLY_NONE = 0;
    private static final int REPLY_PONG = 1;
    private static final int REPLY_PEERS = 2;
    private static final int REPLY_NODES = 3;
    private static final int REPLY_NETWORK_FAIL = 4;

    public static final boolean SECURE_NID = true;

    /** how long since generated do we delete - BEP 5 says 10 minutes */
    private static final long MAX_TOKEN_AGE = 10*60*1000;
    private static final long MAX_INBOUND_TOKEN_AGE = MAX_TOKEN_AGE - 2*60*1000;
    private static final int MAX_OUTBOUND_TOKENS = 5000;
    /** how long since sent do we wait for a reply */
    private static final long MAX_MSGID_AGE = 2*60*1000;
    /** how long since sent do we wait for a reply */
    private static final long DEFAULT_QUERY_TIMEOUT = 75*1000;
    private static final long DEST_LOOKUP_TIMEOUT = 10*1000;
    /** stagger with other cleaners */
    private static final long CLEAN_TIME = 63*1000;
    private static final long EXPLORE_TIME = 877*1000;
    private static final long BLACKLIST_CLEAN_TIME = 17*60*1000;
    private static final String DHT_FILE_SUFFIX = ".dht.dat";

    private static final int SEND_CRYPTO_TAGS = 8;
    private static final int LOW_CRYPTO_TAGS = 4;

    private NodeInfo	bootstrap_node;
    
    private int			dest_lookup_fail;
    private int			dest_lookup_ok;
    private int			refresh_find_node_ok;
    private int			refresh_find_node_fail;
    private int			refresh_ping_ok;
    private int			refresh_ping_fail;
    private int			announce_peer_in;
    private int			find_node_in;
    private int			get_peers_in;
    private int			ping_in;
    
    /**
     *  @param baseName generally "i2psnark"
     */
    	// PARG - added constructor to allow using fixed NID
    
    public KRPC(I2PAppContext ctx, String baseName, I2PSession session, I2PHelperAdapter logger) {
    	
    	 this( 		ctx, baseName, session,
    			 	TrackerClient.PORT + 10 + ctx.random().nextInt(65535 - 20 - TrackerClient.PORT ),
    			 	null, logger );
    }
    
    public KRPC(I2PAppContext ctx, String baseName, I2PSession session, int qport, NID nid, I2PHelperAdapter logger ) {
        _context = ctx;
        _session = session;
        _log = ctx.logManager().getLog(KRPC.class);
        _tracker = new DHTTracker(ctx);
        _adapter = logger;
        _sentQueries = new ConcurrentHashMap<MsgID, ReplyWaiter>();
        _outgoingTokens = new ConcurrentHashMap<Token, NodeInfo>();
        _incomingTokens = new ConcurrentHashMap<NID, Token>();
        _blacklist = new ConcurrentHashSet<NID>();

        // Construct my NodeInfo
        // Pick ports over a big range to marginally increase security
        // If we add a search DHT, adjust to stay out of each other's way
        _qPort = qport;
        _rPort = _qPort + 1;
        
        if ( nid == null ){
	        if (SECURE_NID) {
	            _myNID = NodeInfo.generateNID(session.getMyDestination().calculateHash(), _qPort, _context.random());
	            _myID = _myNID.getData();
	        } else {
	            _myID = new byte[NID.HASH_LENGTH];
	            ctx.random().nextBytes(_myID);
	            _myNID = new NID(_myID);
	        }
        }else{
        	_myNID 	= nid;
        	_myID	= nid.getData();
        }
        _myNodeInfo = new NodeInfo(_myNID, session.getMyDestination(), _qPort);
        _dhtFile = new File(ctx.getConfigDir(), baseName + DHT_FILE_SUFFIX);
        _backupDhtFile = baseName.equals("i2psnark") ? null : new File(ctx.getConfigDir(), "i2psnark" + DHT_FILE_SUFFIX);
        _knownNodes = new DHTNodes(ctx, _myNID);

        start();
    }

    ///////////////// Public methods

    /**
     * Known nodes, not estimated total network size.
     */
    public int sizeInKAD() {
        return _knownNodes.sizeInKAD();
    }

    /**
     *  @return The UDP query port
     */
    public int getPort() {
        return _qPort;
    }

    /**
     *  @return The UDP response port
     */
    public int getRPort() {
        return _rPort;
    }
    
    	// PARG
    
    public void
    requestBootstrap()
    {
    	force_bootstrap = true;
    }
    
    public NID
    getNID()
    {
    	return( _myNID );
    }

    public NodeInfo
    getNodeInfo(
    	byte[]		nid_hash )
    {
    	return( _knownNodes.get( new NID( nid_hash )));
    }
    
    public List<NodeInfo>
    getNodesForBootstrap( 
    	int	num )
    {
    	return( _knownNodes.getNodesForBootstrap( num ));
    }
    
    public void
    ping(
    	NodeInfo		ni )
   	{
		ReplyWaiter waiter = sendPing( ni );
		
		if ( waiter != null ){
			
    		synchronized( waiter ){
				try{
					waiter.wait(30*1000);
					
					int replyType = waiter.getReplyCode();
					
					_adapter.log( "Ping of " + ni + " -> " + ( replyType == REPLY_PONG ));
					
				}catch( InterruptedException ie ){	
				}
			}
		}else{
			
			_adapter.log( "Failed to send ping" );
		}
   	}
    
    public Log
    getLog()
    {
    	return( _log );
    }
    
    @Override
    public void setBootstrapNode(NodeInfo ni) {
    	bootstrap_node = ni;
    }
    /**
     *  Ping. We don't have a NID yet so the node is presumed
     *  to be absent from our DHT.
     *  Non-blocking, does not wait for pong.
     *  If and when the pong is received the node will be inserted in our DHT.
     */
    public void ping(Destination dest, int port) {
        NodeInfo nInfo = new NodeInfo(dest, port);
        sendPing(nInfo);
    }

    /**
     *  Bootstrapping or background thread.
     *  Blocking!
     *  This is almost the same as getPeers()
     *
     *  @param target the key we are searching for
     *  @param maxNodes how many to contact
     *  @param maxWait how long to wait for each to reply (not total) must be > 0
     *  @param parallel how many outstanding at once (unimplemented, always 1)
     */
    private void explore(NID target, int maxNodes, long maxWait, int parallel) {
        List<NodeInfo> nodes = _knownNodes.findClosest(target, maxNodes);
        if (nodes.isEmpty()) {
            if (_log.shouldLog(Log.WARN))
                _log.info("DHT is empty, cannot explore");
            return;
        }
        SortedSet<NodeInfo> toTry = new TreeSet<NodeInfo>(new NodeInfoComparator(target));
        toTry.addAll(nodes);
        Set<NodeInfo> tried = new HashSet<NodeInfo>();

        if (_log.shouldLog(Log.INFO))
            _log.info("Starting explore of " + target);
        for (int i = 0; i < maxNodes; i++) {
            if (!_isRunning)
                break;
            NodeInfo nInfo;
            try {
                nInfo = toTry.first();
            } catch (NoSuchElementException nsee) {
                break;
            }
            toTry.remove(nInfo);
            tried.add(nInfo);

            ReplyWaiter waiter = sendFindNode(nInfo, target);
            if (waiter == null)
                continue;
            synchronized(waiter) {
                try {
                    waiter.wait(maxWait);
                } catch (InterruptedException ie) {}
            }

            int replyType = waiter.getReplyCode();
            if (replyType == REPLY_NONE) {
                 if (_log.shouldLog(Log.DEBUG))
                     _log.debug("Got no reply");
            } else if (replyType == REPLY_NODES) {
                 List<NodeInfo> reply = (List<NodeInfo>) waiter.getReplyObject();
                 // It seems like we are just going to get back ourselves all the time
                 if (_log.shouldLog(Log.DEBUG))
                     _log.debug("Got " + reply.size() + " nodes");
                 for (NodeInfo ni : reply) {
                     if (! (ni.equals(_myNodeInfo) || (toTry.contains(ni) && tried.contains(ni))))
                         toTry.add(ni);
                 }
            } else if (replyType == REPLY_NETWORK_FAIL) {
                 break;
            } else {
                 if (_log.shouldLog(Log.INFO))
                     _log.info("Got unexpected reply " + replyType + ": " + waiter.getReplyObject());
            }
        }
        if (_log.shouldLog(Log.INFO))
            _log.info("Finished explore of " + target);
    }

    /**
     *  Local lookup only
     *  @param ih a 20-byte info hash
     *  @param max max to return
     *  @return list or empty list (never null)
     */
    public List<NodeInfo> findClosest(byte[] ih, int max) {
        List<NodeInfo> nodes = _knownNodes.findClosest(new InfoHash(ih), max);
        return nodes;
    }

    /**
     *  Get peers for a torrent, and announce to the closest node we find.
     *  This is an iterative lookup in the DHT.
     *  Blocking!
     *  Caller should run in a thread.
     *
     *  @param ih the Info Hash (torrent)
     *  @param max maximum number of peers to return
     *  @param maxWait the maximum time to wait (ms) must be > 0
     *  @param annMax the number of peers to announce to
     *  @param annMaxWait the maximum total time to wait for announces, may be 0 to return immediately without waiting for acks
     *  @return possibly empty (never null)
     */
    
    public Collection<Hash> getPeersAndNoAnnounce(byte[] ih, int max, long maxWait, int annMax, long annMaxWait) {
    	return(getPeersAndAnnounce(ih,max,maxWait,annMax,annMaxWait,false));
    }
    public Collection<Hash> getPeersAndAnnounce(byte[] ih, int max, long maxWait, int annMax, long annMaxWait) {
    	return(getPeersAndAnnounce(ih,max,maxWait,annMax,annMaxWait,true));
    }
    public Collection<Hash> getPeersAndAnnounce(byte[] ih, int max, long maxWait, int annMax, long annMaxWait,boolean do_announce) {
        // check local tracker first
        InfoHash iHash = new InfoHash(ih);
        Collection<Hash> rv = _tracker.getPeers(iHash, max);
        rv.remove(_myNodeInfo.getHash());
        if (rv.size() >= max)
            return rv;
        rv = new HashSet<Hash>(rv);
        long endTime = _context.clock().now() + maxWait;

        // needs to be much higher than log(size) since many lookups will fail
        // at first and we will give up too early
        int maxNodes = 30;
        // Initial set to try, will get added to as we go
        List<NodeInfo> nodes = _knownNodes.findClosest(iHash, maxNodes);
        NodeInfoComparator comp = new NodeInfoComparator(iHash);
        SortedSet<NodeInfo> toTry = new TreeSet<NodeInfo>(comp);
        SortedSet<NodeInfo> heardFrom = new TreeSet<NodeInfo>(comp);
        toTry.addAll(nodes);
        SortedSet<NodeInfo> tried = new TreeSet<NodeInfo>(comp);

        if (_log.shouldLog(Log.INFO))
            _log.info("Starting getPeers for " + iHash + " (b64: " + new NID(ih) + ") " + " with " + nodes.size() + " to try");
        for (int i = 0; i < maxNodes; i++) {
            if (!_isRunning)
                break;
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Now to try: " + toTry);
            NodeInfo nInfo;
            try {
                nInfo = toTry.first();
            } catch (NoSuchElementException nsee) {
                break;
            }
            toTry.remove(nInfo);
            tried.add(nInfo);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Try " + i + ": " + nInfo);

            ReplyWaiter waiter = sendGetPeers(nInfo, iHash);
            if (waiter == null)
                continue;
            synchronized(waiter) {
                try {
                    waiter.wait(Math.max(30*1000, (Math.min(45*1000, endTime - _context.clock().now()))));
                } catch (InterruptedException ie) {}
            }

            int replyType = waiter.getReplyCode();
            if (replyType == REPLY_NONE) {
                 if (_log.shouldLog(Log.DEBUG))
                     _log.debug("Got no reply");
            } else if (replyType == REPLY_PONG) {
                 if (_log.shouldLog(Log.DEBUG))
                     _log.debug("Got pong");
            } else if (replyType == REPLY_PEERS) {
                 heardFrom.add(waiter.getSentTo());
                 if (_log.shouldLog(Log.DEBUG))
                     _log.debug("Got peers");
                 List<Hash> reply = (List<Hash>) waiter.getReplyObject();
                 // shouldn't send us an empty peers list but through
                 // 0.9.8.1 it will
                 if (!reply.isEmpty()) {
                     for (int j = 0; j < reply.size() && rv.size() < max; j++) {
                          Hash h = reply.get(j);
                          if (!h.equals(_myNodeInfo.getHash()))
                              rv.add(h);
                     }
                 }
                 if (_log.shouldLog(Log.INFO))
                     _log.info("Finished get Peers, got " + reply.size() + " from DHT, returning " + rv.size());
                 break;
            } else if (replyType == REPLY_NODES) {
                 heardFrom.add(waiter.getSentTo());
                 List<NodeInfo> reply = (List<NodeInfo>) waiter.getReplyObject();
                 if (_log.shouldLog(Log.DEBUG))
                     _log.debug("Got " + reply.size() + " nodes");
                 for (NodeInfo ni : reply) {
                     if (! (ni.equals(_myNodeInfo) || tried.contains(ni) || toTry.contains(ni)))
                         toTry.add(ni);
                 }
            } else if (replyType == REPLY_NETWORK_FAIL) {
                 break;
            } else {
                 if (_log.shouldLog(Log.INFO))
                     _log.info("Got unexpected reply " + replyType + ": " + waiter.getReplyObject());
            }
            if (_context.clock().now() > endTime)
                break;
            if (!toTry.isEmpty() && !heardFrom.isEmpty() &&
                comp.compare(toTry.first(), heardFrom.first()) >= 0) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Finished get Peers, nothing closer to try after " + (i+1));
                break;
            }
        }
        // now announce
        if ( do_announce ){
	        if (!heardFrom.isEmpty()) {
	            announce(ih);
	            // announce to the closest we've heard from
	            int annCnt = 0;
	            long start = _context.clock().now();
	            for (Iterator<NodeInfo> iter = heardFrom.iterator(); iter.hasNext() && annCnt < annMax && _isRunning; ) {
	                NodeInfo annTo = iter.next();
	                if (_log.shouldLog(Log.INFO))
	                    _log.info("Announcing to closest from get peers: " + annTo);
	                long toWait = annMaxWait > 0 ? Math.min(annMaxWait, 60*1000) : 0;
	                if (announce(ih, annTo, toWait))
	                    annCnt++;
	                if (annMaxWait > 0) {
	                    annMaxWait -= _context.clock().now() - start;
	                    if (annMaxWait < 1000)
	                        break;
	                }
	            }
	        } else {
	            // spray it, but unlikely to work, we just went through the kbuckets,
	            // so this is essentially just a retry
	            if (_log.shouldLog(Log.INFO))
	               _log.info("Announcing to closest in kbuckets after get peers failed");
	            announce(ih, annMax, annMaxWait);
	        }
	        if (_log.shouldLog(Log.INFO)) {
	            _log.info("Finished get Peers, returning " + rv.size());
	            _log.info("Tried: " + tried);
	            _log.info("Heard from: " + heardFrom);
	            _log.info("Not tried: " + toTry);
	        }
        }
        
        return rv;
    }

    /**
     *  Announce to ourselves.
     *  Non-blocking.
     *
     *  @param ih the Info Hash (torrent)
     */
    public void announce(byte[] ih) {
        InfoHash iHash = new InfoHash(ih);
        _tracker.announce(iHash, _myNodeInfo.getHash());
    }

    /**
     *  Announce somebody else we know about.
     *  Non-blocking.
     *
     *  @param ih the Info Hash (torrent)
     *  @param peerHash the peer's Hash
     */
    public void announce(byte[] ih, byte[] peerHash) {
        InfoHash iHash = new InfoHash(ih);
        _tracker.announce(iHash, new Hash(peerHash));
        // Do NOT do this, corrupts the Hash cache and the Peer ID
        //_tracker.announce(iHash, Hash.create(peerHash));
    }

    /**
     *  Remove reference to ourselves in the local tracker.
     *  Use when shutting down the torrent locally.
     *  Non-blocking.
     *
     *  @param ih the Info Hash (torrent)
     */
    public void unannounce(byte[] ih) {
        InfoHash iHash = new InfoHash(ih);
        _tracker.unannounce(iHash, _myNodeInfo.getHash());
    }

    /**
     *  Not recommended - use getPeersAndAnnounce().
     *
     *  Announce to the closest peers in the local DHT.
     *  This is NOT iterative - call getPeers() first to get the closest
     *  peers into the local DHT.
     *  Blocking unless maxWait <= 0
     *  Caller should run in a thread.
     *  This also automatically announces ourself to our local tracker.
     *  For best results do a getPeersAndAnnounce() instead, as this announces to
     *  the closest in the kbuckets, it does NOT sort through the known nodes hashmap.
     *
     *  @param ih the Info Hash (torrent)
     *  @param max maximum number of peers to announce to
     *  @param maxWait the maximum total time to wait (ms) or 0 to do all in parallel and return immediately.
     *  @return the number of successful announces, not counting ourselves.
     */
    public int announce(byte[] ih, int max, long maxWait) {
        announce(ih);
        int rv = 0;
        long start = _context.clock().now();
        InfoHash iHash = new InfoHash(ih);
        List<NodeInfo> nodes = _knownNodes.findClosest(iHash, max);
        if (_log.shouldLog(Log.INFO))
            _log.info("Found " + nodes.size() + " to announce to for " + iHash);
        for (NodeInfo nInfo : nodes) {
            if (!_isRunning)
                break;
            if (announce(ih, nInfo, Math.min(maxWait, 60*1000)))
                rv++;
            maxWait -= _context.clock().now() - start;
            if (maxWait < 1000)
                break;
        }
        return rv;
    }

    /**
     *  Announce to a single DHT peer.
     *  Blocking unless maxWait <= 0
     *  Caller should run in a thread.
     *  For best results do a getPeers() first so we have a token.
     *
     *  @param ih the Info Hash (torrent)
     *  @param nInfo the peer to announce to
     *  @param maxWait the maximum time to wait (ms) or 0 to return immediately.
     *  @return success
     */
    private boolean announce(byte[] ih, NodeInfo nInfo, long maxWait) {
        InfoHash iHash = new InfoHash(ih);
        // it isn't clear from BEP 5 if a token is bound to a single infohash?
        // for now, just bind to the NID
        //TokenKey tokenKey = new TokenKey(nInfo.getNID(), iHash);
        Token token = _incomingTokens.get(nInfo.getNID());
        if (token != null && token.lastSeen() < _context.clock().now() - MAX_INBOUND_TOKEN_AGE) {
            // too old, cleaner will get it soon
            token = null;
        }
        if (token == null) {
            // we have no token, have to do a getPeers first to get a token
            if (maxWait <= 0)
                return false;
            if (_log.shouldLog(Log.INFO))
                _log.info("No token for announce to " + nInfo + ", sending get_peers first");
            ReplyWaiter waiter = sendGetPeers(nInfo, iHash);
            if (waiter == null)
                return false;
            long start = _context.clock().now();
            synchronized(waiter) {
                try {
                    waiter.wait(maxWait);
                } catch (InterruptedException ie) {}
            }
            int replyType = waiter.getReplyCode();
            if (!(replyType == REPLY_PEERS || replyType == REPLY_NODES)) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Get_peers in announce() failed to " + nInfo);
                return false;
            }
            // we should have a token now
            token = _incomingTokens.get(nInfo.getNID());
            if (token == null || token.lastSeen() < _context.clock().now() - MAX_INBOUND_TOKEN_AGE) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Huh? no token after get_peers in announce() succeeded to " + nInfo);
                return false;
            }
            maxWait -= _context.clock().now() - start;
            if (maxWait < 1000) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Ran out of time after get_peers in announce() succeeded to " + nInfo);
                return false;
            }
        }

        // send and wait on rcv msg lock unless maxWait <= 0
        ReplyWaiter waiter = sendAnnouncePeer(nInfo, iHash, token);
        if (waiter == null)
            return false;
        if (maxWait <= 0)
            return true;
        synchronized(waiter) {
            try {
                waiter.wait(maxWait);
            } catch (InterruptedException ie) {}
        }
        int replyType = waiter.getReplyCode();
        return replyType == REPLY_PONG;
    }

    /**
     *  Loads the DHT from file.
     *  Can't be restarted after stopping?
     */
    public synchronized void start() {
        if (_isRunning)
            return;
        _session.addMuxedSessionListener(this, I2PSession.PROTO_DATAGRAM_RAW, _rPort);
        _session.addMuxedSessionListener(this, I2PSession.PROTO_DATAGRAM, _qPort);
        _knownNodes.start();
        _tracker.start();
        PersistDHT.loadDHT(this, _dhtFile, _backupDhtFile);
        // start the explore thread
        _isRunning = true;
        // no need to keep ref, it will eventually stop
        new Cleaner();
        new Explorer(5*1000);
        new Refresher();
        _txPkts.set(0);
        _rxPkts.set(0);
        _txBytes.set(0);
        _rxBytes.set(0);
        _started = _context.clock().now();
    }

    /**
     *  Stop everything.
     */
    public synchronized void stop() {
        if (!_isRunning)
            return;
        _isRunning = false;
        // FIXME stop the explore thread
        // unregister port listeners
        _session.removeListener(I2PSession.PROTO_DATAGRAM, _qPort);
        _session.removeListener(I2PSession.PROTO_DATAGRAM_RAW, _rPort);
        // clear the DHT and tracker
        _tracker.stop();
        // don't lose all our peers if we didn't have time to check them
        //  boolean saveAll = _context.clock().now() - _started < 20*60*1000;
        PersistDHT.saveDHT(_knownNodes, _dhtFile);
        _knownNodes.stop();
        for (Iterator<ReplyWaiter> iter = _sentQueries.values().iterator(); iter.hasNext(); ) {
            ReplyWaiter waiter = iter.next();
            iter.remove();
            waiter.networkFail();
        }
        _outgoingTokens.clear();
        _incomingTokens.clear();
        _blacklist.clear();
    }

    /**
     * Clears the tracker and DHT data.
     * Call after saving DHT data to disk.
     */
    public void clear() {
        _tracker.stop();
        _knownNodes.clear();
    }

    /**
     * Debug info, HTML formatted
     */
    public String renderStatusHTML() {
        long uptime = Math.max(1000, _context.clock().now() - _started);
        StringBuilder buf = new StringBuilder(256);
        buf.append("<br><b>DHT DEBUG</b><br>TX: ").append(_txPkts.get()).append(" pkts / ")
           .append(DataHelper.formatSize2(_txBytes.get())).append("B / ")
           .append(DataHelper.formatSize2(_txBytes.get() * 1000 / uptime)).append("Bps<br>" +
                   "RX: ").append(_rxPkts.get()).append(" pkts / ")
           .append(DataHelper.formatSize2(_rxBytes.get())).append("B / ")
           .append(DataHelper.formatSize2(_rxBytes.get() * 1000 / uptime)).append("Bps<br>" +
                   "DHT Peers: ").append( _knownNodes.sizeInKAD()).append("<br>" +
                   "Blacklisted: ").append(_blacklist.size()).append("<br>" +
                   "Sent tokens: ").append(_outgoingTokens.size()).append("<br>" +
                   "Rcvd tokens: ").append(_incomingTokens.size()).append("<br>" +
                   "Pending queries: ").append(_sentQueries.size()).append("<br>");
        _tracker.renderStatusHTML(buf);
        _knownNodes.renderStatusHTML(buf);
        return buf.toString();
    }

    ////////// All private below here /////////////////////////////////////

    ///// Sending.....

    // Queries.....
    // The first 3 queries use the query port.
    // Announces use the response port.

    /**
     *  Blocking if we have to look up the dest for the nodeinfo
     *
     *  @param nInfo who to send it to
     *  @return null on error
     */
    private ReplyWaiter sendPing(NodeInfo nInfo) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending ping to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("q", "ping");
        Map<String, Object> args = new HashMap<String, Object>();
        map.put("a", args);
        return sendQuery(nInfo, map, true);
    }

    /**
     *  Blocking if we have to look up the dest for the nodeinfo
     *
     *  @param nInfo who to send it to
     *  @param tID target ID we are looking for
     *  @return null on error
     */
    private ReplyWaiter sendFindNode(NodeInfo nInfo, NID tID) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending find node of " + tID + " to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("q", "find_node");
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("target", tID.getData());
        map.put("a", args);
        return sendQuery(nInfo, map, true);
    }

    /**
     *  Blocking if we have to look up the dest for the nodeinfo
     *
     *  @param nInfo who to send it to
     *  @return null on error
     */
    private ReplyWaiter sendGetPeers(NodeInfo nInfo, InfoHash ih) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending get peers of " + ih + " to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("q", "get_peers");
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("info_hash", ih.getData());
        map.put("a", args);
        ReplyWaiter rv = sendQuery(nInfo, map, true);
        // save the InfoHash so we can get it later
        if (rv != null)
            rv.setSentObject(ih);
        return rv;
    }

    /**
     *  Non-blocking, will fail if we don't have the dest for the nodeinfo
     *
     *  @param nInfo who to send it to
     *  @return null on error
     */
    private ReplyWaiter sendAnnouncePeer(NodeInfo nInfo, InfoHash ih, Token token) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending announce of " + ih + " to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("q", "announce_peer");
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("info_hash", ih.getData());
        // port ignored
        args.put("port", Integer.valueOf(TrackerClient.PORT));
        args.put("token", token.getData());
        map.put("a", args);
        // an announce need not be signed, we have a token
        ReplyWaiter rv = sendQuery(nInfo, map, false);
        return rv;
    }

    // Responses.....
    // All responses use the response port.

    /**
     *  @param nInfo who to send it to
     *  @return success
     */
    private boolean sendPong(NodeInfo nInfo, MsgID msgID) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending pong to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> resps = new HashMap<String, Object>();
        map.put("r", resps);
        return sendResponse(nInfo, msgID, map);
    }

    /** response to find_node (no token) */
    private boolean sendNodes(NodeInfo nInfo, MsgID msgID, byte[] ids) {
        return sendNodes(nInfo, msgID, null, ids);
    }

    /**
     *  response to find_node (token is null) or get_peers (has a token)
     *  @param nInfo who to send it to
     *  @return success
     */
    private boolean sendNodes(NodeInfo nInfo, MsgID msgID, Token token, byte[] ids) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending nodes to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> resps = new HashMap<String, Object>();
        map.put("r", resps);
        if (token != null)
            resps.put("token", token.getData());
        resps.put("nodes", ids);
        return sendResponse(nInfo, msgID, map);
    }

    /** @param token non-null */
    private boolean sendPeers(NodeInfo nInfo, MsgID msgID, Token token, List<byte[]> peers) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending peers to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> resps = new HashMap<String, Object>();
        map.put("r", resps);
        resps.put("token", token.getData());
        resps.put("values", peers);
        return sendResponse(nInfo, msgID, map);
    }

    // All errors use the response port.

    /**
     *  @param nInfo who to send it to
     *  @return success
     */
    private boolean sendError(NodeInfo nInfo, MsgID msgID, int err, String msg) {
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending error " + msg + " to: " + nInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> resps = new HashMap<String, Object>();
        map.put("r", resps);
        return sendResponse(nInfo, msgID, map);
    }

    // Low-level send methods

    // TODO sendQuery with onReply / onTimeout args

    /**
     *  Blocking if repliable and we must lookup b32
     *  @param repliable true for all but announce
     *  @return null on error
     */
    private ReplyWaiter sendQuery(NodeInfo nInfo, Map<String, Object> map, boolean repliable) {
        if (nInfo.equals(_myNodeInfo))
            throw new IllegalArgumentException("wtf don't send to ourselves");
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Sending query to: " + nInfo);
        if (nInfo.getDestination() == null) {
            NodeInfo newInfo = _knownNodes.get(nInfo.getNID());
            if (newInfo != null && newInfo.getDestination() != null) {
                nInfo = newInfo;
            } else if (!repliable) {
                // Don't lookup for announce query, we should already have it
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Dropping non-repliable query, no dest for " + nInfo);
                return null;
            } else {
                // Lookup the dest for the hash
                // TODO spin off into thread or queue? We really don't want to block here
                if (!lookupDest(nInfo)) {
                    if (_log.shouldLog(Log.INFO))
                        _log.info("Dropping repliable query, no dest for " + nInfo);
                    timeout(nInfo);
                    return null;
                }
            }
        }
        map.put("y", "q");
        MsgID mID = new MsgID(_context);
        map.put("t", mID.getData());
        Map<String, Object> args = (Map<String, Object>) map.get("a");
        if (args == null)
            throw new IllegalArgumentException("no args");
        args.put("id", _myID);
        int port = nInfo.getPort();
        if (!repliable)
            port++;
        boolean success = sendMessage(nInfo.getDestination(), port, map, repliable);
        if (success) {
            // save for the caller to get
            ReplyWaiter rv = new ReplyWaiter(mID, nInfo, null, null);
            _sentQueries.put(mID, rv);
            return rv;
        }
        return null;
    }

    /**
     * @param toPort the query port, we will increment here
     *  @return success
     */
    private boolean sendResponse(NodeInfo nInfo, MsgID msgID, Map<String, Object> map) {
        if (nInfo.equals(_myNodeInfo))
            throw new IllegalArgumentException("wtf don't send to ourselves");
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Sending response to: " + nInfo);
        if (nInfo.getDestination() == null) {
            NodeInfo newInfo = _knownNodes.get(nInfo.getNID());
            if (newInfo != null && newInfo.getDestination() != null) {
                nInfo = newInfo;
            } else {
                // lookup b32?
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Dropping response, no dest for " + nInfo);
                return false;
            }
        }
        map.put("y", "r");
        map.put("t", msgID.getData());
        Map<String, Object> resps = (Map<String, Object>) map.get("r");
        if (resps == null)
            throw new IllegalArgumentException("no resps");
        resps.put("id", _myID);
        return sendMessage(nInfo.getDestination(), nInfo.getPort() + 1, map, false);
    }

    /**
     *  @param toPort the query port, we will increment here
     *  @return success
     */
    private boolean sendError(NodeInfo nInfo, MsgID msgID, Map<String, Object> map) {
        if (nInfo.equals(_myNodeInfo))
            throw new IllegalArgumentException("wtf don't send to ourselves");
        if (_log.shouldLog(Log.INFO))
            _log.info("Sending error to: " + nInfo);
        if (nInfo.getDestination() == null) {
            NodeInfo newInfo = _knownNodes.get(nInfo.getNID());
            if (newInfo != null && newInfo.getDestination() != null) {
                nInfo = newInfo;
            } else {
                // lookup b32?
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Dropping sendError, no dest for " + nInfo);
                return false;
            }
        }
        map.put("y", "e");
        map.put("t", msgID.getData());
        return sendMessage(nInfo.getDestination(), nInfo.getPort() + 1, map, false);
    }

    /**
     *  Get the dest for a NodeInfo lacking it, and store it there.
     *  Blocking.
     *  @return success
     */
    private boolean lookupDest(NodeInfo nInfo) {
        if (_log.shouldLog(Log.INFO))
            _log.info("looking up dest for " + nInfo);
        try {
            // use a short timeout for now
            Destination dest = _session.lookupDest(nInfo.getHash(), DEST_LOOKUP_TIMEOUT);
            if (dest != null) {
            	//System.out.println( "Destination lookup OK for " + nInfo );
            	dest_lookup_ok++;
                nInfo.setDestination(dest);
                if (_log.shouldLog(Log.INFO))
                    _log.info("lookup success for " + nInfo);
                return true;
            }
        } catch (I2PSessionException ise) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("lookup fail", ise);
        }
        if (_log.shouldLog(Log.INFO))
            _log.info("lookup fail for " + nInfo);
        
        //System.out.println( "Destination lookup FAIL for " + nInfo );
        dest_lookup_fail++;
        return false;
    }

    /**
     *  Lowest-level send message call.
     *  @param repliable true for all but announce
     *  @return success
     */
    private boolean sendMessage(Destination dest, int toPort, Map<String, Object> map, boolean repliable) {
        if (_session.isClosed()) {
            // Don't allow DHT to open a closed session
            if (_log.shouldLog(Log.WARN))
                _log.warn("Not sending message, session is closed");
            return false;
        }
        if (dest.calculateHash().equals(_myNodeInfo.getHash()))
            throw new IllegalArgumentException("wtf don't send to ourselves");
        byte[] payload = BEncoder.bencode(map);
        if (_log.shouldLog(Log.DEBUG)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            try {
                _log.debug("Sending to: " + dest.calculateHash() + ' ' + BDecoder.bdecode(bais).toString());
            } catch (IOException ioe) {}
        }

        // Always send query port, peer will increment for unsigned replies
        int fromPort = _qPort;
        if (repliable) {
            I2PDatagramMaker dgMaker = new I2PDatagramMaker(_session);
            payload = dgMaker.makeI2PDatagram(payload);
            if (payload == null) {
                if (_log.shouldLog(Log.WARN))
                    _log.warn("WTF DGM fail");
                return false;
            }
        }

        SendMessageOptions opts = new SendMessageOptions();
        opts.setDate(_context.clock().now() + 60*1000);
        opts.setTagsToSend(SEND_CRYPTO_TAGS);
        opts.setTagThreshold(LOW_CRYPTO_TAGS);
        if (!repliable)
            opts.setSendLeaseSet(false);
        try {
            boolean success = _session.sendMessage(dest, payload, 0, payload.length,
                                                   repliable ? I2PSession.PROTO_DATAGRAM : I2PSession.PROTO_DATAGRAM_RAW,
                                                   fromPort, toPort, opts);
            if (success) {
                _txPkts.incrementAndGet();
                _txBytes.addAndGet(payload.length);
            } else {
                if (_log.shouldLog(Log.WARN))
                    _log.warn("WTF sendMessage fail");
            }
            return success;
        } catch (I2PSessionException ise) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("sendMessage fail", ise);
            return false;
        }
    }

    ///// Reception.....

    /**
     *  @param from dest or null if it didn't come in on signed port
     */
    private void receiveMessage(Destination from, int fromPort, byte[] payload) {
        try {
            InputStream is = new ByteArrayInputStream(payload);
            BDecoder dec = new BDecoder(is);
            BEValue bev = dec.bdecodeMap();
            Map<String, BEValue> map = bev.getMap();
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Got KRPC message " + bev.toString());

            // Lazy here, just let missing Map entries throw NPEs, caught below

            byte[] msgIDBytes = map.get("t").getBytes();
            MsgID mID = new MsgID(msgIDBytes);
            String type = map.get("y").getString();
            if (type.equals("q")) {
                // queries must be repliable
                String method = map.get("q").getString();
                Map<String, BEValue> args = map.get("a").getMap();
                receiveQuery(mID, from, fromPort, method, args);
            } else if (type.equals("r") || type.equals("e")) {
               // get dest from id->dest map
                ReplyWaiter waiter = _sentQueries.remove(mID);
                if (waiter != null) {
                    // TODO verify waiter NID and port?
                    if (type.equals("r")) {
                        Map<String, BEValue> response = map.get("r").getMap();
                        receiveResponse(waiter, response);
                    } else {
                        List<BEValue> error = map.get("e").getList();
                        receiveError(waiter, error);
                    }
                } else {
                    if (_log.shouldLog(Log.WARN))
                        _log.warn("Rcvd msg with no one waiting: " + bev.toString());
                }
            } else {
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Unknown msg type rcvd: " + bev.toString());
                throw new InvalidBEncodingException("Unknown type: " + type);
            }
            // success
      /***
        } catch (InvalidBEncodingException e) {
        } catch (IOException e) {
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (IllegalArgumentException e) {
        } catch (ClassCastException e) {
        } catch (NullPointerException e) {
       ***/
        } catch (Exception e) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("Receive error for message", e);
        }
    }


    // Queries.....

    /**
     *  Adds sender to our DHT.
     *  @param dest may be null for announce_peer method only
     *  @throws NPE too
     */
    private void receiveQuery(MsgID msgID, Destination dest, int fromPort, String method, Map<String, BEValue> args) throws InvalidBEncodingException {
        if (dest == null && !method.equals("announce_peer")) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("Received non-announce_peer query method on reply port: " + method);
            return;
        }
        byte[] nid = args.get("id").getBytes();
        NodeInfo nInfo;
        if (dest != null) {
            nInfo = new NodeInfo(new NID(nid), dest, fromPort);
            nInfo = heardFrom(nInfo);
            nInfo.setDestination(dest);
            // ninfo.checkport ?
        } else {
            nInfo = null;
        }
       
        if (method.equals("ping")) {
        	ping_in++;
            receivePing(msgID, nInfo);
        } else if (method.equals("find_node")) {
        	find_node_in++;
            byte[] tid = args.get("target").getBytes();
            NID tID = new NID(tid);
            receiveFindNode(msgID, nInfo, tID);
        } else if (method.equals("get_peers")) {
        	get_peers_in++;
            byte[] hash = args.get("info_hash").getBytes();
            InfoHash ih = new InfoHash(hash);
            receiveGetPeers(msgID, nInfo, ih);
        } else if (method.equals("announce_peer")) {
        	announce_peer_in++;
            byte[] hash = args.get("info_hash").getBytes();
            InfoHash ih = new InfoHash(hash);
            // this is the "TCP" port, we don't care
            //int port = args.get("port").getInt();
            byte[] token = args.get("token").getBytes();
            receiveAnnouncePeer(msgID, ih, token);
        } else {
            if (_log.shouldLog(Log.WARN))
                _log.warn("Unknown query method rcvd: " + method);
        }
    }

    /**
     *  Called for a request or response
     *  @return old NodeInfo or nInfo if none, use this to reduce object churn
     */
    private NodeInfo heardFrom(NodeInfo nInfo) {
        // try to keep ourselves out of the DHT
        if (nInfo.equals(_myNodeInfo))
            return _myNodeInfo;
        NID nID = nInfo.getNID();
        NodeInfo oldInfo = _knownNodes.get(nID);
        if (oldInfo == null) {
            if (_log.shouldLog(Log.INFO))
                _log.info("Adding node: " + nInfo);
            oldInfo = nInfo;
            NodeInfo nInfo2 = _knownNodes.putIfAbsent(nInfo);
            if (nInfo2 != null)
                oldInfo = nInfo2;
        } else {
            if (oldInfo.getDestination() == null && nInfo.getDestination() != null)
                oldInfo.setDestination(nInfo.getDestination());
        }
        nID = oldInfo.getNID();
        nID.setAlive();
        if (_blacklist.remove(nID)) {
            if (_log.shouldLog(Log.INFO))
                _log.info("UN-blacklisted: " + nID);
        }
        return oldInfo;
    }

    /**
     *  Called for bootstrap or for all nodes in a receiveNodes reply.
     *  Package private for PersistDHT.
     *  @return non-null nodeInfo from DB if present, otherwise the nInfo parameter is returned
     */
    public NodeInfo heardAbout(NodeInfo nInfo) {
        // try to keep ourselves out of the DHT
        if (nInfo.equals(_myNodeInfo))
            return _myNodeInfo;
        NodeInfo rv = _knownNodes.putIfAbsent(nInfo);
        if (rv == null) {
            rv = nInfo;
            // if we didn't know about it before, set the timestamp
            // so it isn't immediately removed by the DHTNodes cleaner
            // PARG - use creation data now // rv.getNID().setLastSeen( false );
        }
        return rv;
    }

    public NodeInfo heardAbout(Map map){
    	try{
	    	byte[]	nid_bytes 	= (byte[])map.get( "n" );
	    	int		port 		= ((Number)map.get( "p" )).intValue();
	    	byte[]	dest_bytes	= (byte[])map.get( "d" );
	    	
	    	NID nid = new NID( nid_bytes );
	    	
	    	Destination destination = new Destination();
	    
	    	destination.fromByteArray( dest_bytes );
	    	
	    	NodeInfo ni = new NodeInfo( nid, destination, port );
	    	
	    	if (ni.equals(_myNodeInfo))
	             return _myNodeInfo;
	        
	    	NodeInfo rv = _knownNodes.putIfAbsent( ni );
	    	
	    	return( rv==null?ni:rv );
	    	
    	}catch( Throwable e ){
    		
    		e.printStackTrace();
    		
    		return( null );
    	}
    }
    
    /**
     *  Called when a reply times out
     */
    private void timeout(NodeInfo nInfo) {
        NID nid = nInfo.getNID();
        boolean remove = nid.timeout();
        if (remove) {
            if (_knownNodes.remove(nid) != null) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Removed after consecutive timeouts: " + nInfo);
            }
            if (!_blacklist.contains(nid)) {
                // used as when-added time
                nid.resetCreated();
                _blacklist.add(nid);
                if (_log.shouldLog(Log.INFO))
                    _log.info("Blacklisted: " + nid);
            }
        }
    }

    /**
     *  Handle and respond to the query
     */
    private void receivePing(MsgID msgID, NodeInfo nInfo) throws InvalidBEncodingException {
        if (_log.shouldLog(Log.INFO))
            _log.info("Rcvd ping from: " + nInfo);
        sendPong(nInfo, msgID);
    }

    /**
     *  Handle and respond to the query
     *  @param tID target ID they are looking for
     */
    private void receiveFindNode(MsgID msgID, NodeInfo nInfo, NID tID) throws InvalidBEncodingException {
        if (_log.shouldLog(Log.INFO))
             _log.info("Rcvd find_node from: " + nInfo + " for: " + tID);
        NodeInfo peer = _knownNodes.get(tID);
        if (peer != null) {
            // success, one answer
            sendNodes(nInfo, msgID, peer.getData());
        } else {
            // get closest from DHT
            List<NodeInfo> nodes = _knownNodes.findClosest(tID, K);
            nodes.remove(nInfo);        // him
            nodes.remove(_myNodeInfo);  // me
            byte[] nodeArray = new byte[nodes.size() * NodeInfo.LENGTH];
            for (int i = 0; i < nodes.size(); i ++) {
                System.arraycopy(nodes.get(i).getData(), 0, nodeArray, i * NodeInfo.LENGTH, NodeInfo.LENGTH);
            }
            sendNodes(nInfo, msgID, nodeArray);
        }
    }

    /**
     *  Handle and respond to the query
     */
    private void receiveGetPeers(MsgID msgID, NodeInfo nInfo, InfoHash ih) throws InvalidBEncodingException {
        if (_log.shouldLog(Log.INFO))
             _log.info("Rcvd get_peers from: " + nInfo + " for: " + ih);
        // generate and save random token
        Token token = new Token(_context);
        _outgoingTokens.put(token, nInfo);
        if (_log.shouldLog(Log.INFO))
             _log.info("Stored new OB token: " + token + " for: " + nInfo);

        List<Hash> peers = _tracker.getPeers(ih, MAX_WANT);
        peers.remove(nInfo.getHash());   // him
        if (peers.isEmpty()) {
            // similar to find node, but with token
            // get closest from DHT
            List<NodeInfo> nodes = _knownNodes.findClosest(ih, K);
            nodes.remove(nInfo);        // him
            nodes.remove(_myNodeInfo);  // me
            byte[] nodeArray = new byte[nodes.size() * NodeInfo.LENGTH];
            for (int i = 0; i < nodes.size(); i ++) {
                System.arraycopy(nodes.get(i).getData(), 0, nodeArray, i * NodeInfo.LENGTH, NodeInfo.LENGTH);
            }
            sendNodes(nInfo, msgID, token, nodeArray);
        } else {
            List<byte[]> hashes = new ArrayList<byte[]>(peers.size());
            for (Hash peer : peers) {
                 hashes.add(peer.getData());
            }
            sendPeers(nInfo, msgID, token, hashes);
        }
    }

    /**
     *  Handle and respond to the query.
     *  We have no node info here, it came on response port, we have to get it from the token.
     *  So we can't verify that it came from the same peer, as BEP 5 specifies.
     */
    private void receiveAnnouncePeer(MsgID msgID, InfoHash ih, byte[] tok) throws InvalidBEncodingException {
        Token token = new Token(tok);
        NodeInfo nInfo = _outgoingTokens.get(token);
        if (nInfo == null) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("Unknown token in announce_peer: " + token);
            //if (_log.shouldLog(Log.INFO))
            //    _log.info("Current known tokens: " + _outgoingTokens.keySet());
            return;
        }
        if (_log.shouldLog(Log.INFO))
             _log.info("Rcvd announce from: " + nInfo + " for: " + ih);

        _tracker.announce(ih, nInfo.getHash());
        // the reply for an announce is the same as the reply for a ping
        sendPong(nInfo, msgID);
    }

    // Responses.....

    /**
     *  Handle the response and alert whoever sent the query it is responding to.
     *  Adds sender nodeinfo to our DHT.
     *  @throws NPE, IllegalArgumentException, and others too
     */
    private void receiveResponse(ReplyWaiter waiter, Map<String, BEValue> response) throws InvalidBEncodingException {
        NodeInfo nInfo = waiter.getSentTo();

        BEValue nodes = response.get("nodes");
        BEValue values = response.get("values");

        // token handling - save it for later announces
        if (nodes != null || values != null) {
            BEValue btok = response.get("token");
            InfoHash ih = (InfoHash) waiter.getSentObject();
            if (btok != null && ih != null) {
                byte[] tok = btok.getBytes();
                Token token = new Token(_context, tok);
                _incomingTokens.put(nInfo.getNID(), token);
                if (_log.shouldLog(Log.DEBUG))
                    _log.debug("Got token: " + token + ", must be a response to get_peers");
            } else {
                if (_log.shouldLog(Log.DEBUG))
                    _log.debug("No token and saved infohash, must be a response to find_node");
            }
        }

        // now do the right thing
        if (nodes != null) {
            // find node or get peers response - concatenated NodeInfos
            byte[] ids = nodes.getBytes();
            List<NodeInfo> rlist = receiveNodes(nInfo, ids);
            waiter.gotReply(REPLY_NODES, rlist);
        } else if (values != null) {
            // get peers response - list of Hashes
            List<BEValue> peers = values.getList();
            List<Hash> rlist = receivePeers(nInfo, peers);
            waiter.gotReply(REPLY_PEERS, rlist);
        } else {
            // a ping response or an announce peer response
            byte[] nid = response.get("id").getBytes();
            receivePong(nInfo, nid);
            waiter.gotReply(REPLY_PONG, null);
        }
    }

    /**
     *  rcv concatenated 54 byte NodeInfos, return as a List
     *  Adds all received nodeinfos to our DHT.
     *  @throws NPE, IllegalArgumentException, and others too
     */
    private List<NodeInfo> receiveNodes(NodeInfo nInfo, byte[] ids) throws InvalidBEncodingException {
        int max = Math.min(K, ids.length / NodeInfo.LENGTH);
        List<NodeInfo> rv = new ArrayList<NodeInfo>(max);
        for (int off = 0; off < ids.length && rv.size() < max; off += NodeInfo.LENGTH) {
            NodeInfo nInf = new NodeInfo(ids, off);
            if (_blacklist.contains(nInf.getNID())) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Ignoring blacklisted " + nInf.getNID() + " from: " + nInfo);
                continue;
            }
            nInf = heardAbout(nInf);
            rv.add(nInf);
        }
        if (_log.shouldLog(Log.INFO))
             _log.info("Rcvd nodes from: " + nInfo + ": " + DataHelper.toString(rv));
        return rv;
    }

    /**
     *  rcv 32 byte Hashes, return as a List
     *  @throws NPE, IllegalArgumentException, and others too
     */
    private List<Hash> receivePeers(NodeInfo nInfo, List<BEValue> peers) throws InvalidBEncodingException {
        if (_log.shouldLog(Log.INFO))
             _log.info("Rcvd peers from: " + nInfo);
        int max = Math.min(MAX_WANT, peers.size());
        List<Hash> rv = new ArrayList<Hash>(max);
        for (BEValue bev : peers) {
            byte[] b = bev.getBytes();
            //Hash h = new Hash(b);
            Hash h = Hash.create(b);
            rv.add(h);
            if (rv.size() >= max)
                break;
        }
        if (_log.shouldLog(Log.INFO))
             _log.info("Rcvd " + peers.size() + " peers from: " + nInfo + ": " + DataHelper.toString(rv));
        return rv;
    }

    /**
     *  If node info was previously created with the dummy NID,
     *  replace it with the received NID.
     */
    private void receivePong(NodeInfo nInfo, byte[] nid) {
        if (nInfo.getNID().equals(FAKE_NID)) {
            NodeInfo newInfo = new NodeInfo(new NID(nid), nInfo.getHash(), nInfo.getPort());
            Destination dest = nInfo.getDestination();
            if (dest != null)
                newInfo.setDestination(dest);
            heardFrom(newInfo);
        }
        if (_log.shouldLog(Log.INFO))
             _log.info("Rcvd pong from: " + nInfo);
    }

    // Errors.....

    /**
     *  @throws NPE, and others too
     */
    private void receiveError(ReplyWaiter waiter, List<BEValue> error) throws InvalidBEncodingException {
        int errorCode = error.get(0).getInt();
        String errorString = error.get(1).getString();
        if (_log.shouldLog(Log.WARN))
            _log.warn("Rcvd error from: " + waiter +
                      " num: " + errorCode +
                      " msg: " + errorString);
        // this calls heardFrom()
        waiter.gotReply(errorCode, errorString);
    }

    /**
     * Callback for replies
     */
    private class ReplyWaiter extends SimpleTimer2.TimedEvent {
        private final MsgID mid;
        private final NodeInfo sentTo;
        private final Runnable onReply;
        private final Runnable onTimeout;
        private volatile int replyCode;
        private Object sentObject;
        private Object replyObject;

        /**
         *  Either wait on this object with a timeout, or use non-null Runnables.
         *  Any sent data to be remembered may be stored by setSentObject().
         *  Reply object may be in getReplyObject().
         *  @param onReply must be fast, otherwise set to null and wait on this UNUSED
         *  @param onTimeout must be fast, otherwise set to null and wait on this UNUSED
         */
        public ReplyWaiter(MsgID mID, NodeInfo nInfo, Runnable onReply, Runnable onTimeout) {
            super(SimpleTimer2.getInstance(), DEFAULT_QUERY_TIMEOUT);
            this.mid = mID;
            this.sentTo = nInfo;
            this.onReply = onReply;
            this.onTimeout = onTimeout;
        }

        public NodeInfo getSentTo() {
            return sentTo;
        }

        /** only used for get_peers, to save the Info Hash */
        public void setSentObject(Object o) {
            sentObject = o;
        }

        /** @return that stored with setSentObject() */
        public Object getSentObject() {
            return sentObject;
        }

        /**
         *  Should contain null if getReplyCode is REPLY_PONG.
         *  Should contain List<Hash> if getReplyCode is REPLY_PEERS.
         *  Should contain List<NodeInfo> if getReplyCode is REPLY_NODES.
         *  Should contain String if getReplyCode is > 200.
         *  @return may be null depending on what happened. Cast to expected type.
         */
        public Object getReplyObject() {
            return replyObject;
        }

        /**
         *  If nonzero, we got a reply, and getReplyObject() may contain something.
         *  @return code or 0 if no error
         */
        public int getReplyCode() {
            return replyCode;
        }

        /**
         *  Will notify this and run onReply.
         *  Also removes from _sentQueries and calls heardFrom().
         */
        public void gotReply(int code, Object o) {
            cancel();
            _sentQueries.remove(mid);
            replyObject = o;
            replyCode = code;
            // if it is fake, heardFrom is called by receivePong()
            if (!sentTo.getNID().equals(FAKE_NID))
                heardFrom(sentTo);
            if (onReply != null)
                onReply.run();
            synchronized(this) {
                this.notifyAll();
            }
        }

        /** timer callback on timeout */
        public void timeReached() {
            _sentQueries.remove(mid);
            if (onTimeout != null)
                onTimeout.run();
            timeout(sentTo);
            if (_log.shouldLog(Log.INFO))
                _log.warn("timeout waiting for reply from " + sentTo);
            synchronized(this) {
                this.notifyAll();
            }
        }

        /**
         *  Will notify this but not run onReply or onTimeout,
         *  or remove from _sentQueries, or call heardFrom().
         */
        public void networkFail() {
            cancel();
            replyCode = REPLY_NETWORK_FAIL;
            synchronized(this) {
                this.notifyAll();
            }
        }
    }

    // I2PSessionMuxedListener interface ----------------

    /**
     * Instruct the client that the given session has received a message
     *
     * Will be called only if you register via addMuxedSessionListener().
     * Will be called only for the proto(s) and toPort(s) you register for.
     *
     * @param session session to notify
     * @param msgId message number available
     * @param size size of the message - why it's a long and not an int is a mystery
     * @param proto 1-254 or 0 for unspecified
     * @param fromPort 1-65535 or 0 for unspecified
     * @param toPort 1-65535 or 0 for unspecified
     */
    public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromPort, int toPort) {
        // TODO throttle
        try {
            byte[] payload = session.receiveMessage(msgId);
            _rxPkts.incrementAndGet();
            _rxBytes.addAndGet(payload.length);
            if (toPort == _qPort) {
                // repliable
                I2PDatagramDissector dgDiss = new I2PDatagramDissector();
                dgDiss.loadI2PDatagram(payload);
                payload = dgDiss.getPayload();
                Destination from = dgDiss.getSender();
                // TODO per-dest throttle
                receiveMessage(from, fromPort, payload);
            } else if (toPort == _rPort) {
                // raw
                receiveMessage(null, fromPort, payload);
            } else {
                if (_log.shouldLog(Log.WARN))
                    _log.warn("msg on bad port");
            }
        } catch (DataFormatException e) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("bad msg");
        } catch (I2PInvalidDatagramException e) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("bad msg");
        } catch (I2PSessionException e) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("bad msg");
        }
    }

    /** for non-muxed */
    public void messageAvailable(I2PSession session, int msgId, long size) {}

    public void reportAbuse(I2PSession session, int severity) {}

    public void disconnected(I2PSession session) {
        if (_log.shouldLog(Log.WARN))
            _log.warn("KRPC disconnected");
    }

    public void errorOccurred(I2PSession session, String message, Throwable error) {
        if (_log.shouldLog(Log.WARN))
            _log.warn("KRPC got error msg: ", error);
    }

    /**
     * Cleaner-upper
     */
    private class Cleaner extends SimpleTimer2.TimedEvent {

    	private long last_save = _context.clock().now();
    	
        public Cleaner() {
            super(SimpleTimer2.getInstance(), 7 * CLEAN_TIME);
        }

        public void timeReached() {
            if (!_isRunning)
                return;
            long now = _context.clock().now();
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("KRPC cleaner starting with " +
                          _blacklist.size() + " in blacklist, " +
                          _outgoingTokens.size() + " sent Tokens, " +
                          _incomingTokens.size() + " rcvd Tokens");
            int cnt = 0;
            long expire = now - MAX_TOKEN_AGE;
            for (Iterator<Token> iter = _outgoingTokens.keySet().iterator(); iter.hasNext(); ) {
                Token tok = iter.next();
                // just delete at random if we have too many
                // TODO reduce the expire time and iterate again?
                if (tok.lastSeen() < expire || cnt >= MAX_OUTBOUND_TOKENS)
                    iter.remove();
                else
                    cnt++;
            }
            expire = now - MAX_INBOUND_TOKEN_AGE;
            for (Iterator<Token> iter = _incomingTokens.values().iterator(); iter.hasNext(); ) {
                Token tok = iter.next();
                if (tok.lastSeen() < expire)
                    iter.remove();
            }
            expire = now - BLACKLIST_CLEAN_TIME;
            for (Iterator<NID> iter = _blacklist.iterator(); iter.hasNext(); ) {
                NID nid = iter.next();
                // lastSeen() is actually when-added
                if (nid.getCreated() < expire)
                    iter.remove();
            }
            // TODO sent queries?
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("KRPC cleaner done, now with " +
                          _blacklist.size() + " in blacklist, " +
                          _outgoingTokens.size() + " sent Tokens, " +
                          _incomingTokens.size() + " rcvd Tokens, " +
                          _knownNodes.sizeInKAD() + " known peers, " +
                          _sentQueries.size() + " queries awaiting response");
            
            // PARG 
  
            now = _context.clock().now();
            
            if ( now - last_save >= 5*60*1000 ){
            
            	last_save = now;
            	
            	PersistDHT.saveDHT(_knownNodes, _dhtFile);
            }
            
            schedule(CLEAN_TIME);
        }
    }

    /**
     * Fire off explorer thread
     */
    private class Explorer extends SimpleTimer2.TimedEvent {

        public Explorer(long delay) {
            super(SimpleTimer2.getInstance(), delay);
        }

        public void timeReached() {
            if (!_isRunning)
                return;
            if (_knownNodes.sizeInKAD() > 0)
                (new I2PAppThread(new ExplorerThread(), "DHT Explore", true)).start();
            else
                schedule(60*1000);
        }
    }

    /**
     * explorer thread
     */
    private class ExplorerThread implements Runnable {

        public void run() {
            if (!_isRunning)
                return;
            if (!_hasBootstrapped) {
                if (_log.shouldLog(Log.INFO))
                    _log.info("Bootstrap start, size: " + _knownNodes.sizeInKAD());
                explore(_myNID, 8, 60*1000, 1);
                if (_log.shouldLog(Log.INFO))
                    _log.info("Bootstrap done, size: " + _knownNodes.sizeInKAD());
                _hasBootstrapped = true;
            }
            if (!_isRunning)
                return;
            if (_log.shouldLog(Log.INFO))
                _log.info("Explore start. size: " + _knownNodes.sizeInKAD());
            List<NID> keys = _knownNodes.getExploreKeys();
            for (NID nid : keys) {
                explore(nid, 8, 60*1000, 1);
                if (!_isRunning)
                    return;
            }
            if (_log.shouldLog(Log.INFO))
                _log.info("Explore of " + keys.size() + " buckets done, new size: " + _knownNodes.sizeInKAD());
            new Explorer(EXPLORE_TIME);
        }
    }
    
    private class Refresher extends SimpleTimer2.TimedEvent {
    	
    	private long 	next_bootstrap;
    	private int		consec_bootstraps;
    	
    	private long	last_external_bootstrap;
    	
        public Refresher() {
            super(SimpleTimer2.getInstance(), 15*1000 );
        }

        public void 
        timeReached() 
        {
            if ( !_isRunning ){
            	
                return;
            }
            
            int	live_node_count = 0;           
            
            try{            	
            	if ( bootstrap_node != null ){
            		
            			// in case it has found its way into the DHT
            		
            		_knownNodes.remove( bootstrap_node.getNID());
            	}
            	
            	boolean 	all_failing 	= true;
            	
            	List<NodeInfo> all_nodes = new ArrayList<NodeInfo>(_knownNodes.valuesInKAD());
            	
            	int	all_nodes_count = all_nodes.size();
            	
               	List<NodeInfo>	live_nodes 		= new ArrayList<NodeInfo>( all_nodes_count );
               	List<NodeInfo>	unknown_nodes 	= new ArrayList<NodeInfo>( all_nodes_count );
            	
            	for ( NodeInfo node: all_nodes ){
            		
            		NID nid = node.getNID();
            		
            		if ( nid.getFailCount() == 0 ){
            			
            			all_failing = false;
            			
            			if ( nid.getLastAlive() > 0 ){
            				
            				live_nodes.add( node );
            				
            			}else{
            				
            				unknown_nodes.add( node );
            			}
            		}
            	}
            	
            	live_node_count = live_nodes.size();
            	
            	long now =_context.clock().now();
            	
            	if ( all_nodes_count < 20 || all_failing || force_bootstrap ){
            		
            			// not enough nodes, see if we can grab some more from most recently heard from node
            			// track to ensure we don't keep hitting the same most recent one...
            			// boostrap if screwed
            		
            		if ( 	force_bootstrap || 
            					(( all_failing || all_nodes.size() < 5 ) && 
            					bootstrap_node != null &&
            					( next_bootstrap == 0 || ( now > next_bootstrap )))){
            			
            			if ( force_bootstrap ){
            				
            				force_bootstrap = false;
            				
            				consec_bootstraps = 0;
            			}
            			
            			_adapter.log( "Bootstrapping..." );
            			       
            			if ( lookupDest( bootstrap_node )){

            				if ( !_isRunning ){
            					
            					return;
            				}
            				
	            			int delay = 1*60*1000;
	            			
	            			for (int i=0;i<consec_bootstraps;i++){
	            				
	            				delay *= 2;
	            				
	            				if ( delay > 10*60*1000 ){
	            				
	            					delay = 10*60*1000;
	            					
	            					break;
	            				}
	            			}
	            			
	            			next_bootstrap = now + delay;
	            			
	            			consec_bootstraps++;
	             			
	            			ReplyWaiter waiter = sendFindNode( bootstrap_node, new NID( RandomUtils.nextSecureHash()));
	
							if ( waiter != null){
							
	    						synchronized( waiter ){
	    							
	    							try{
	    								waiter.wait(60*1000);
	    								
	    							}catch (InterruptedException ie) {}
	    						}	
	    						
	    						if ( waiter.getReplyCode() == REPLY_NODES ){
	    							
	    							_adapter.log( "Bootstrap worked" );
	    							
	    						}else{
	    							
	    							_adapter.log( "Bootstrap failed" );
	    						}
							}
            			}else{
            				
            				_adapter.log( "Bootstrap not resolved" );
            				
            				if ( 	last_external_bootstrap == 0 || 
            						now - last_external_bootstrap >= 30*60*1000 ){
            					
            					last_external_bootstrap = now;
            					
            					if ( _adapter.tryExternalBootstrap()){
            						
            							// reschedule with a 2 min delay
            						
            						last_external_bootstrap = now - 28*60*1000;
            					}
            				}
            			}
            		}else{
            			
            				// try and grab some more nodes from existing non-dead ones
            			            			
            			List<NodeInfo> nodes_to_use = live_node_count>0?live_nodes:unknown_nodes;
            			
            			if ( nodes_to_use.size() > 0 ){
            				
            				if ( !_isRunning ){
            					
            					return;
            				}
            				
            				NodeInfo node = nodes_to_use.get( RandomUtils.nextInt( nodes_to_use.size()));
            				
                			ReplyWaiter waiter = sendFindNode( node, new NID( RandomUtils.nextSecureHash()));

    						if ( waiter != null){
    						
        						synchronized( waiter ){
        							
        							try{
        								waiter.wait(60*1000);
        								
        								int replyType = waiter.getReplyCode();
										
										if ( replyType == REPLY_NODES ){
											
											refresh_find_node_ok++;
											
										}else{
											
											refresh_find_node_fail++;
										}
        							}catch (InterruptedException ie) {}
        						}
    						}
            			}
            		}
            	}else{
            		
            		consec_bootstraps = 0;
            		
	            	Collections.shuffle( all_nodes );
	            	
	            	Collections.sort(
	            		all_nodes,
	            		new Comparator<NodeInfo>()
	            		{
	            			public int 
	            			compare(
	            				NodeInfo o1, 
	            				NodeInfo o2) 
	            			{
	            					// failing nodes get lower attention
	            				
	            				NID n1 = o1.getNID();
	            				NID n2 = o2.getNID();
	            				
	               				boolean n1_fail = n1.getFailCount() > 0;
	               				boolean n2_fail = n2.getFailCount() > 0;
	            				
	               				if ( n1_fail && n2_fail ){
	               					return( 0 );
	               				}else if ( n1_fail ){
	               					return( 1 );
	               				}else if ( n2_fail ){
	               					return( -1 );
	               				}
	               				
	              				long n1_alive = n1.getLastAlive();
	              				long n2_alive = n2.getLastAlive();
	              				
	              				if ( n1_alive == 0 && n2_alive == 0 ){
	              					
	              					return( 0 );
	              					
	              				}else if ( n1_alive == 0 ){
	              					
	              					return( -1 );
	              					
	              				}else if ( n2_alive == 0 ){
	              					
	              					return( 1 );
	              				}
	              				
	              				if ( n1_alive < n2_alive ){
	              					
	              					return( -1 );
	              					
	              				}else if ( n1_alive > n2_alive ){
	              					
	              					return( 1 );
	              				}
	              				
	              				return( 0 );
	            			}
	            		});
	            	
	            	int	done = 0;
	            	
	            	boolean	try_fn = live_node_count < 5;
	            	
	            	for ( NodeInfo ni: all_nodes ){
	            	
        				if ( !_isRunning ){
        					
        					return;
        				}
        				
        				if ( force_bootstrap ){
        					
        					break;
        				}
        				
	            		if ( done > (live_node_count>10?5:10 )){
	            			
	            			break;
	            		}
	            		
	            		done++;
	            			 
	            		if ( try_fn ){
	            			
	            			try_fn = false;
	            			
	               			ReplyWaiter waiter = sendFindNode( ni, new NID( RandomUtils.nextSecureHash()));

    						if ( waiter != null){
    						
        						synchronized( waiter ){
        							
        							try{
        								waiter.wait(60*1000);
        								
        								int replyType = waiter.getReplyCode();
										
										if ( replyType == REPLY_NODES ){
											
											refresh_find_node_ok++;
											
										}else{
											
											refresh_find_node_fail++;
										}
        							}catch (InterruptedException ie ){
        							}
        						}
    						}
	            		}else{
	            			
		            		ReplyWaiter waiter = sendPing( ni );
		            		
		            		if ( waiter != null ){
		            			
			            		synchronized( waiter ){
									try{
										waiter.wait(30*1000);
										
										int replyType = waiter.getReplyCode();
										
										if ( replyType == REPLY_PONG ){
											
											refresh_ping_ok++;
											
										}else{
											
											refresh_ping_fail++;
										}
										//System.out.println( "Ping of " + ni + " -> " + ( replyType == REPLY_PONG ));
			    						
									}catch( InterruptedException ie ){	
									}
								}
		            		}
	            		}
	            	}
	            	
	            	if ( done < all_nodes_count ){
	            		
	            		int	bad_sent = 0;
	            		
	            			// ping a couple of the failing ones to precipitate their demise if possible
	            		
	            		for ( int i=all_nodes_count-1;i>=done&&bad_sent<2;i--){
	            			
            				if ( !_isRunning ){
            					
            					return;
            				}
            				
              				if ( force_bootstrap ){
            					
            					break;
            				}
              				
	            			ReplyWaiter waiter = sendPing( all_nodes.get( i ));
		            		
		            		if ( waiter != null ){
		            			
			            		synchronized( waiter ){
									try{
										waiter.wait(30*1000);
										
									}catch( InterruptedException ie ){									
									}
			            		}
		            		}
	            		}
	            	}
            	}
            }finally{
            	
				if ( _isRunning ){
					
	            	if ( bootstrap_node == null ){
	            		
	            			// we're bootstrap node, keep things fresh
	            		
	            		schedule(10*1000 );
	            		
	            	}else{
	            	
	            		if ( force_bootstrap ){
	            			
	            			schedule( 100 );
	            			
	            		}else{
	            			
	            			schedule( live_node_count>10?60*1000:15*1000 );
	            		}
	            	}
				}
            }
        }
    }
    
    public void
    crawl()
    {
    	List<NodeInfo> all_nodes = new LinkedList<NodeInfo>(_knownNodes.valuesInKAD());
    	
    	_adapter.log( "Crawling " + all_nodes.size() + " nodes" );
    	
    	Iterator<NodeInfo>	it = all_nodes.iterator();
    	
    	while( !all_nodes.isEmpty()){
    		
    		final AESemaphore sem = new AESemaphore( "" );
    		
    		int	done = 0;
    		
    		for (int i=0;i<16;i++){
    			
    			if ( all_nodes.isEmpty()){
    				
    				break;
    			}
    			
    			final NodeInfo nid = it.next();
    			
    			it.remove();
    			
    			if ( _blacklist.contains( nid )){
    				
    				_adapter.log( "Blacklisted" );
    				
    				continue;
    			}
    			
    			done++;
    			
    			new AEThread2("")
    			{
    				public void
    				run()
    				{
    					try{
    						_adapter.log( "Searching " + nid.toString());

    						long start = SystemTime.getMonotonousTime();
    						
    						ReplyWaiter waiter = sendFindNode( nid, new NID( RandomUtils.nextSecureHash()));

    						if ( waiter == null){
    							
    							_adapter.log( "No dest" );
    							
    						}else{
    						
	    						synchronized(waiter) {
	    							try {
	    								waiter.wait(60*1000);
	    							} catch (InterruptedException ie) {}
	    						}
	
	    						int replyType = waiter.getReplyCode();
	    						if (replyType == REPLY_NONE) {
	    							_adapter.log("Got no reply");
	    						} else if (replyType == REPLY_NODES) {
	    							List<NodeInfo> reply = (List<NodeInfo>) waiter.getReplyObject();

	    							_adapter.log( "Got " + reply.size() + " nodes from " + nid.toString() + " in " + (SystemTime.getMonotonousTime() - start ));
	
	    						}else{
	    							_adapter.log( "Got derp" );
	    						}
    						}
    					}finally{
    						
    						sem.release();
    					}
    				}
    			}.start();
    		}
    		
    		for ( int i=0;i<done;i++){
    			
    			sem.reserve();
    		}
    	}
    }
    
    public void
    print()
    {
    	List<NodeInfo> known_nodes = new LinkedList<NodeInfo>(_knownNodes.valuesInKAD());
    	
       	Set<NID> kad_nids = _knownNodes.kadValues();

       	_tracker.print( _adapter );
       	
       	_adapter.log( "Nodes: " + known_nodes.size() + ", NIDs=" + kad_nids.size());
    	
    	int	alive 	= 0;
    	int dead1	= 0;
    	int	dead2	= 0;
    	int	no_dest	= 0;
    	int	unknown	= 0;
    	
     	for ( NodeInfo ni: known_nodes ){
     		
     		NID nid = ni.getNID();
     		
     		_adapter.log( ni.toString() );
     		
     		int fails = nid.getFailCount();
     		
     		if ( fails == 0 ){
     			
     			if ( nid.getLastAlive() > 0 ){
     				
     				alive++;
     				
     			}else{
     				unknown++;
     				
     				if ( ni.getDestination() == null ){
     					
     					no_dest++;
     				}
     			}
     		}else if ( fails == 1 ){
     			
     			dead1++;
     			
     		}else{
     			
     			dead2++;
     		}
     	}
     	
     	_adapter.log( "Alive=" + alive + ", dead1=" + dead1 + ", dead2=" + dead2 + ", unknown=" + unknown + ", no-dest=" + no_dest );
    }
    
    public String
    getStats()
    {
        return( "in: pi=" + ping_in + ",an=" + announce_peer_in + ",fn=" + find_node_in +",gp=" + get_peers_in + "\r\n" +
        		"dest: ok=" + dest_lookup_ok + ",fail=" + dest_lookup_fail + "; ref_pi: ok=" + refresh_ping_ok + ",fail=" + refresh_ping_fail + "; ref_fn: ok=" + refresh_find_node_ok + ",fail=" + refresh_find_node_fail );
    	
    }
}
