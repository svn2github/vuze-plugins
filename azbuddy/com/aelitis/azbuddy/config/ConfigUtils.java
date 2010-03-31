package com.aelitis.azbuddy.config;

import java.util.Map;
import java.util.Set;

import com.aelitis.azbuddy.utils.Utils;

final class ConfigUtils {
	
	static void deserializeContainer(Map bdecodedMap, InheritableConfigContainer target)
	{
		Set<Map.Entry> entrySet = bdecodedMap.entrySet();
		for(Map.Entry i : entrySet)
		{
			String rawkey;  
			if(i.getKey() instanceof byte[])
				rawkey = Utils.bytesToString((byte[])i.getKey());
			else if(i.getKey() instanceof String)
				rawkey = (String)i.getKey();
			else
				continue;
			
			Object value = i.getValue();

			if(value instanceof Integer && rawkey.startsWith(InheritableBoolean.PREFIX))
				new InheritableBoolean(
						target,
						rawkey.substring(InheritableBoolean.PREFIX.length()),
						false,
						((Integer)value).intValue() != 0 ? true : false);

			if(value instanceof Integer && rawkey.startsWith(InheritableInt.PREFIX))
				new InheritableInt(
						target,
						rawkey.substring(InheritableInt.PREFIX.length()),
						false,
						((Integer)value).intValue());
			
			if(value instanceof byte[] && rawkey.startsWith(InheritableString.PREFIX))
				new InheritableString(
						target,
						rawkey.substring(InheritableString.PREFIX.length()),
						false,
						(byte[])value);
			
			if(value instanceof String && rawkey.startsWith(InheritableString.PREFIX))
				new InheritableString(
						target,
						rawkey.substring(InheritableString.PREFIX.length()),
						false,
						(String)value);
			
			if(value instanceof Map && rawkey.startsWith(InheritableConfigTree.PREFIX))
				deserializeContainer((Map)value, InheritableConfigTree.getInstance(target, rawkey.substring(InheritableConfigTree.PREFIX.length())));
			
			if(value instanceof Map && rawkey.startsWith(UninheritableMap.PREFIX))
				deserializeContainer((Map)value, UninheritableMap.getInstance(target, rawkey.substring(UninheritableMap.PREFIX.length())));
		}
	}
}
