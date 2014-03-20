package org.parg.azureus.plugins.networks.i2p.dht;
/*
 *  From zzzot, modded and relicensed to GPLv2
 */

import net.i2p.data.Hash;

/**
 *  A single peer for a single torrent.
 *  This is what the DHT tracker remembers.
 *
 * @since 0.9.2
 * @author zzz
 */
class Peer extends Hash {

    private long lastSeen;

    public Peer(byte[] data) {
        super(data);
    }

    public long lastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long now) {
        lastSeen = now;
    }
}
