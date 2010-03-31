package com.aelitis.azbuddy.buddy.connection;

import org.gudy.azureus2.plugins.utils.security.SEPublicKey;

public interface ConnectionControlHandler {
	/**
	 * 
	 * when the identity of the peer is verified it'll match it against a known buddy 
	 * @return true if the associated buddy will be handled, false otherwise
	 */
	public BuddyConnectionHandler matchNewConnection(BuddyConnection conn, SEPublicKey remoteKey);
}
