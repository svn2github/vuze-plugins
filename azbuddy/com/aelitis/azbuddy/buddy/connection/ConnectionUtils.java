package com.aelitis.azbuddy.buddy.connection;

import java.io.IOException;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;

final class ConnectionUtils {
	
	static byte[] bencode(Map toEncode)
	{
		
		try
		{
			return BEncoder.encode(toEncode);
		} catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	static Map bdecode(byte[] toDecode)
	{
		
		try
		{
			return BDecoder.decode(toDecode);
		} catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
