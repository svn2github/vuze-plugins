package com.vuze.plugin.btapp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.browser.Browser;

import org.gudy.azureus2.core3.util.FileUtil;

import com.aelitis.azureus.util.MapUtils;

public class BtAppDataSource
{
	private Map<String, String> properties = new HashMap<String, String>();

	private String btappid = "?";
	
	private static final String keys[] = { "category", "name", "btapp_url", "publisher", "version", "descr" };
	private static final String displayKeys[] = { "category", "name", "version", "publisher", "action", "descr" };
	private static final int displayWidths[] = { 100, 180, 60, 90, 120, 350 };
	
	private long accessMode = BtAppView.PRIV_LOCAL;

	public BtAppDataSource(Map map, Browser browser) {
		for (String key : keys) {
			properties.put(key, getAndDecode(browser, map, key, ""));
		}
		btappid = properties.get("btappid");
		if (btappid == null) {
  		String url = properties.get("btapp_url");
  		if (url != null) {
  			Pattern pattern = Pattern.compile("/([^/]+)\\.btapp");
  			Matcher matcher = pattern.matcher(url);
  			
  			// http://apps.bittorrent.com/outspark/outspark.btapp
  			if (matcher.find()) {
  				btappid = Integer.toHexString(matcher.group(1).hashCode());
  			}
  		}
		}
	}
	
	public BtAppDataSource(File basePath) {
		File file = new File(basePath, "btapp");
		btappid = file.getParentFile().getName();
		if (!file.exists()) {
			return;
		}
		try {
			String s = FileUtil.readFileAsString(file, 0xffff);
			String[] lines = s.split("[\r\n]+");
			for (String line : lines) {
				String[] sections = line.split(":", 2);
				if (sections.length == 2) {
					String id = sections[0].toLowerCase();
					String val = sections[1].trim();
					if (id.equals("access")) {
						if (val.equalsIgnoreCase("list_restricted")) {
							accessMode = BtAppView.PRIV_READALL;
						}
						if (val.equalsIgnoreCase("restricted")) {
							accessMode = BtAppView.PRIV_READALL | BtAppView.PRIV_WRITEALL;
						}
					} else if (id.equals("description")) {
						properties.put("descr", val);
					} else if (!properties.containsKey(id)) {
						properties.put(id, val);
					}
				}
			}

		} catch (IOException e) {
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
		return btappid;
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

	public void setAccessMode(long accessMode) {
		this.accessMode = accessMode;
	}

	public long getAccessMode() {
		return accessMode;
	}
	
	public void setProperty(String key, String Value) {
		properties.put(key, Value);
	}
}
