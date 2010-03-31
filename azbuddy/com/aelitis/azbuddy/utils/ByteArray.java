package com.aelitis.azbuddy.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ByteArray {
	
	final private byte[] array;
	
	public ByteArray(byte[] arr) { array = arr; }
	public ByteArray(ByteBuffer buffer)
	{
		// might throw UnsupportedOperationException
		array = buffer.array();
	}
	
	public boolean equals(Object o)
	{
		if(o instanceof byte[])
			return Arrays.equals(array, (byte[])o);
		else if(o instanceof ByteArray)
			return Arrays.equals(array, ((ByteArray)o).getArray());
		else
			return false;
	}
	
	public int hashCode() { return Arrays.hashCode(array); }
	public byte[] getArray() { return array;}
}
