package com.aelitis.azbuddy.dht;

import java.net.InetAddress;

public interface DHTManagerListener {
	public void externalAddressChanged(InetAddress newAddress);
	public void dhtUsabilityChanged(boolean isUsable);
	
	
	public static final int READ_QUERY = 1;
	public static final int WRITE_QUERY = 2;
	public static final int DELETE_QUERY = 4;
	public static final int STATS_QUERY = 8;
	/**
	 * is set if the query succeeded, otherwise it timed out or another error occured
	 */
	public static final int QUERY_DONE = 16;
	
	public void operationFinished(DDBKeyValuePair handler, int flags);
}
