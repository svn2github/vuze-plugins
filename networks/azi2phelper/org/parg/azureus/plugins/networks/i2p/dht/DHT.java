package org.parg.azureus.plugins.networks.i2p.dht;

/*
 *  GPLv2
 */

import java.util.Collection;

import net.i2p.data.Destination;
import net.i2p.data.Hash;


/**
 * Stub for KRPC
 * @since 0.8.4
 */
public interface DHT {


    /**
     *  @return The UDP query port
     */
    public int getPort();

    /**
     *  @return The UDP response port
     */
    public int getRPort();

    	// PARG
    
    public NID
    getNID();
        
    public NodeInfo
    getNodeInfo(
    	byte[]		nid_hash );
    
    public void
    ping(
    	NodeInfo			ni );
    
    public void
    requestBootstrap();
    
    public void
    crawl();
    
    public void
    print();
    
    public String
    getStats();
    
    public void
    setBootstrapNode( NodeInfo ni );
    
    public NodeInfo heardAbout(NodeInfo nInfo);
    
    public Collection<Hash> getPeersAndNoAnnounce(byte[] ih, int max, long maxWait, int annMax, long annMaxWait);

    /**
     *  Ping. We don't have a NID yet so the node is presumed
     *  to be absent from our DHT.
     *  Non-blocking, does not wait for pong.
     *  If and when the pong is received the node will be inserted in our DHT.
     */
    public void ping(Destination dest, int port);

    /**
     *  Get peers for a torrent, and announce to the closest node we find.
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
    public Collection<Hash> getPeersAndAnnounce(byte[] ih, int max, long maxWait, int annMax, long annMaxWait);

    /**
     *  Announce to ourselves.
     *  Non-blocking.
     *
     *  @param ih the Info Hash (torrent)
     */
    public void announce(byte[] ih);

    /**
     *  Announce somebody else we know about.
     *  Non-blocking.
     *
     *  @param ih the Info Hash (torrent)
     *  @param peerHash the peer's Hash
     */
    public void announce(byte[] ih, byte[] peerHash);

    /**
     *  Remove reference to ourselves in the local tracker.
     *  Use when shutting down the torrent locally.
     *  Non-blocking.
     *
     *  @param ih the Info Hash (torrent)
     */
    public void unannounce(byte[] ih);

    /**
     *  Announce to the closest DHT peers.
     *  Blocking unless maxWait <= 0
     *  Caller should run in a thread.
     *  This also automatically announces ourself to our local tracker.
     *  For best results do a getPeers() first so we have tokens.
     *
     *  @param ih the Info Hash (torrent)
     *  @param maxWait the maximum total time to wait (ms) or 0 to do all in parallel and return immediately.
     *  @return the number of successful announces, not counting ourselves.
     */
    public int announce(byte[] ih, int max, long maxWait);

    /**
     * Stop everything.
     */
    public void stop();

    /**
     * Known nodes, not estimated total network size.
     */
    public int sizeInKAD();

    /**
     * Debug info, HTML formatted
     */
    public String renderStatusHTML();
}
