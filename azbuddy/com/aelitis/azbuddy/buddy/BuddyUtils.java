package com.aelitis.azbuddy.buddy;

import java.net.URLEncoder;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;


import com.aelitis.azbuddy.BuddyPlugin;

public final class BuddyUtils {

	static String createMagnet(String nickname, SEPublicKey pubkey)
	{
		try
		{
			return "magnet:?xt=urn:azbd:"+serializePubkey(pubkey)+"&dn="+URLEncoder.encode(nickname,"UTF-8");
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}

	}
	
	public static String serializePubkey(SEPublicKey pubkey)
	{
		return Base32.encode(pubkey.encodePublicKey()); 
	}
	
	public static SEPublicKey deserializePubKey(String toDecode)
	{
		try
		{
			return BuddyPlugin.getSecurityManager().decodePublicKey(Base32.decode(toDecode));
		} catch (Exception e)
		{
			Debug.printStackTrace(e);
			return null;
		}
	}
}
	
