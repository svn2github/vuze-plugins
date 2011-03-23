package com.vuze.plugin.btapp;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.browser.Browser;

import com.aelitis.azureus.util.MapUtils;

public class BtAppDataSource
{
	private Map<String, String> properties = new HashMap<String, String>();

	private String appId = "?";
	
	private static final String keys[] = { "category", "name", "btapp_url", "publisher", "version", "descr" };
	private static final String displayKeys[] = { "category", "name", "version", "publisher" };
	private static final int displayWidths[] = { 100, 200, 100, 150 };

	public BtAppDataSource(Map map, Browser browser) {
		for (String key : keys) {
			properties.put(key, getAndDecode(browser, map, key, ""));
		}
		String url = properties.get("btapp_url");
		if (url != null) {
			Pattern pattern = Pattern.compile("/([^/]+)\\.btapp");
			Matcher matcher = pattern.matcher(url);
			
			// http://apps.bittorrent.com/outspark/outspark.btapp
			if (matcher.find()) {
				appId = Integer.toHexString(matcher.group(1).hashCode());
			}
		}
	}

	private String getAndDecode(Browser browser, Map map, String id, String def) {
		String js = 
				"   var htmlNode = document.createElement(\"DIV\");\n" + 
				"   htmlNode.innerHTML = \"" + Plugin.jsTextify(MapUtils.getMapString(map, id, "")) + "\";\n" +
				"   if(htmlNode.innerText !== undefined)\n" + 
				"      return htmlNode.innerText; // IE\n" + 
				"   return htmlNode.textContent; // FF\n";
		return (String) browser.evaluate(js);
	}
	
	public String getProperty(String id) {
		return MapUtils.getMapString(properties, id, "");
	}

	public static String[] getKeys() {
		return keys;
	}

	public static String[] getDisplaykeys() {
		return displayKeys;
	}

	public String getOurAppId() {
		return appId;
	}

	public static int getDisplayWidth(String name) {
		for (int i = 0; i < displayKeys.length; i++) {
			String displayKey = displayKeys[i];
			if (displayKey.equals(name)) {
				return displayWidths[i];
			}
		}
		return 100;
	}
}
