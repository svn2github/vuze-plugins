package org.parg.azureus.plugins.networks.i2p.dht;

import java.util.Set;

import net.i2p.I2PAppContext;
import net.i2p.kademlia.KBucket;
import net.i2p.kademlia.KBucketTrimmer;

/**
 *  Removes an element older than 15 minutes, but only if the bucket hasn't changed in 5 minutes.
 *  @since 0.9.2
 */
class KBTrimmer implements KBucketTrimmer<NID> {
    private final I2PAppContext _ctx;
    private final int _max;

    private static final long MIN_BUCKET_AGE = 5*60*1000;
    private static final long MAX_NODE_AGE = 15*60*1000;

    public KBTrimmer(I2PAppContext ctx, int max) {
        _ctx = ctx;
        _max = max;
    }

    public boolean trim(KBucket<NID> kbucket, NID toAdd) {
        long now = _ctx.clock().now();
        if (kbucket.getLastChanged() > now - MIN_BUCKET_AGE)
            return false;
        Set<NID> entries = kbucket.getEntries();
        NID oldest_fail  = null;
        for (NID nid : entries) {
            if (nid.getLastKnown() < now - MAX_NODE_AGE) {
                if (kbucket.remove(nid))
                    return true;
            }
            if ( nid.getFailCount() > 0 ){
            	if ( oldest_fail == null ){
            		oldest_fail = nid;
            	}else if ( nid.getFirstFailed() < oldest_fail.getFirstFailed()){
            		oldest_fail = nid;
            	}
            }
        }
        
        if ( entries.size() < _max ){
        	
        	return( true );
        }
        
        if ( oldest_fail != null ){
        	
        	//System.out.println( "KADTrim: discarding failed node: " + oldest_fail );
        	
        	return(  kbucket.remove(oldest_fail));
        }
        
        return( false );
    }
}
