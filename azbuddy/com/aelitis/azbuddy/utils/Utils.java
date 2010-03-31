package com.aelitis.azbuddy.utils;

import java.nio.charset.Charset;

public final class Utils {

	private static final Charset utf8_set = Charset.forName("UTF-8");

	public static String bytesToString(byte[] toConvert)
	{
		return new String(toConvert,utf8_set);		
	}

	public static byte[] stringToBytes(String toConvert)
	{
		return toConvert.getBytes(utf8_set);
	}

}
