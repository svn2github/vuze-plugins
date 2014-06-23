package lanpeerscanner.scan;

import java.util.Hashtable;
import java.util.Map;

public class SyncPeerMap
{

	Map<String, Peer> peers;

    public SyncPeerMap()
    {
        peers = new Hashtable<String, Peer>();
    }

    public synchronized void addPeer(Peer peer)
    {
        peers.put(peer.getIp(), peer);
    }

    public synchronized Map<String, Peer> getPeers()
    {
        return peers;
    }
}
