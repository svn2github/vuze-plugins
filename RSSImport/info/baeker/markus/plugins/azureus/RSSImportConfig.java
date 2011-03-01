/*
 * Created on 13.06.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package info.baeker.markus.plugins.azureus;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.model.*;

/**
 * @author BaekerAdmin
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RSSImportConfig {
	public static final String RSSIMPORT = "RSSImport";
	public static final String RSSIMPORT_FEED = RSSIMPORT + ".Feed";
	public static final String RSSIMPORT_RECHECK = RSSIMPORT + ".Recheck";
	public static final String RSSIMPORT_FILTER = RSSIMPORT + ".Filter";
	public static final String RSSIMPORT_ACTIVE = RSSIMPORT + ".Active";
	public static final String RSSIMPORT_DEFAULT_DIR_WARNING =
		RSSIMPORT + ".DefaultDirWarning";
	public static final String RSSIMPORT_FEED_WARNING =
		RSSIMPORT + ".FeedWarning";
	public static final String RSSIMPORT_LAST_CHECK = RSSIMPORT + ".LastCheck";
	public static final String RSSIMPORT_CHECK_CHANNEL =
		RSSIMPORT + ".CheckChannel";
	public static final String RSSIMPORT_STATUS = RSSIMPORT + ".Status";
	public static final String RSSIMPORT_STATUS_ACTIVE =
		RSSIMPORT_STATUS + ".Active";
	public static final String RSSIMPORT_STATUS_DEACTIVATED =
		RSSIMPORT_STATUS + ".Deactivated";
	public static final String RSSIMPORT_STATUS_RUNNING =
		RSSIMPORT_STATUS + ".Running";
	public static final String RSSIMPORT_TIMEOUT = RSSIMPORT + ".TimeOut";
	public static final String RSSIMPORT_FASTCHECK = RSSIMPORT + ".FastCheck";
	public static final String RSSIMPORT_USEHISTORY=RSSIMPORT+".UseHistory";
	public static final String RSSIMPORT_HISTORY=RSSIMPORT+".History";
	public static final String RSSIMPORT_HISTORYSIZE=RSSIMPORT+".HistorySize";
		private RSSImport rSSImport = null;
	private static RSSImportConfig ric = new RSSImportConfig();
	public static RSSImportConfig getInstance() {
		return ric;
	}

	public String getMessage(String key) {
		if (rSSImport != null && rSSImport.getAzureus() != null) {
			return rSSImport
				.getAzureus()
				.getUtilities()
				.getLocaleUtilities()
				.getLocalisedMessageText(key);
		}
		return "!" + key + "!";
	}
	/**
	 * @return
	 */
	public RSSImport getRSSImport() {
		return rSSImport;
	}

	/**
	 * @param import1
	 */
	public void setRSSImport(RSSImport import1) {
		rSSImport = import1;
		initalize();
	}

	/**
	 * 
	 */
	private void initalize() {
		if (getRSSImport() != null && getRSSImport().getAzureus() != null) {
			PluginInterface azureus = getRSSImport().getAzureus();
			BasicPluginConfigModel model = azureus.getUIManager().createBasicPluginConfigModel(RSSIMPORT);
			model.addBooleanParameter2(RSSIMPORT_ACTIVE, RSSIMPORT_ACTIVE, false);
			model.addStringParameter2(RSSIMPORT_FEED, RSSIMPORT_FEED, "");
			model.addStringParameter2(RSSIMPORT_FILTER, RSSIMPORT_FILTER, "");
			model.addIntParameter2(RSSIMPORT_RECHECK, RSSIMPORT_RECHECK, 10);
			model.addIntParameter2(RSSIMPORT_TIMEOUT, RSSIMPORT_TIMEOUT, 10);
			model.addBooleanParameter2(RSSIMPORT_FASTCHECK,	RSSIMPORT_FASTCHECK, false);
			model.addBooleanParameter2(RSSIMPORT_USEHISTORY, RSSIMPORT_USEHISTORY, false);						
		}

	}

	public void setPluginConfig(String key, String value) {
		if (rSSImport != null && rSSImport.getAzureus() != null) {
			rSSImport.getAzureus().getPluginconfig().setPluginParameter(
				key,
				value);
		}
	}

	public void setPluginConfig(String key, boolean value) {
		if (rSSImport != null && rSSImport.getAzureus() != null) {
			rSSImport.getAzureus().getPluginconfig().setPluginParameter(
				key,
				value);
		}
	}

	public void setPluginConfig(String key, int value) {
		if (rSSImport != null && rSSImport.getAzureus() != null) {
			rSSImport.getAzureus().getPluginconfig().setPluginParameter(
				key,
				value);
		}
	}

	public String getPluginConfig(String key, String def) {
		if (rSSImport != null && rSSImport.getAzureus() != null) {
			return rSSImport
				.getAzureus()
				.getPluginconfig()
				.getPluginStringParameter(
				key,
				def);
		}
		return def;
	}

	public boolean getPluginConfigBoolean(String key, boolean def) {
		if (rSSImport != null && rSSImport.getAzureus() != null) {
			return rSSImport
				.getAzureus()
				.getPluginconfig()
				.getPluginBooleanParameter(
				key,
				def);
		}
		return def;
	}

	public int getPluginConfigInt(String key, int def) {
		if (rSSImport != null && rSSImport.getAzureus() != null) {
			return rSSImport
				.getAzureus()
				.getPluginconfig()
				.getPluginIntParameter(
				key,
				def);
		}
		return def;
	}

	public boolean isHistory() {
		return getPluginConfigBoolean(RSSIMPORT_USEHISTORY, false);
	}

	public void setHistory(boolean set) {
		setPluginConfig(RSSIMPORT_USEHISTORY, set);
	}
	
	public boolean isActive() {
		return getPluginConfigBoolean(RSSIMPORT_ACTIVE, false);
	}

	public void setActive(boolean set) {
		setPluginConfig(RSSIMPORT_ACTIVE, set);
	}

	public boolean isFastCheck() {
		return getPluginConfigBoolean(RSSIMPORT_FASTCHECK, false);
	}

	public void setFastCheck(boolean set) {
		setPluginConfig(RSSIMPORT_FASTCHECK, set);
	}

	public String getFeed() {
		return getPluginConfig(RSSIMPORT_FEED, "");
	}

	public void setFeed(String set) {
		setPluginConfig(RSSIMPORT_FEED, set);
	}

	public String getFilter() {
		return getPluginConfig(RSSIMPORT_FILTER, "");
	}

	public void setFilter(String set) {
		setPluginConfig(RSSIMPORT_FILTER, set);
	}

	public int getRecheck() {
		return getPluginConfigInt(RSSIMPORT_RECHECK, 30);
	}

	public void setRecheck(int set) {
		setPluginConfig(RSSIMPORT_RECHECK, set);
	}

	public int getTimeout() {
		return getPluginConfigInt(RSSIMPORT_TIMEOUT, 10);
	}

	public void setTimeout(int set) {
		setPluginConfig(RSSIMPORT_TIMEOUT, set);
	}
	
	public int getHistorySize() {
		return getPluginConfigInt(RSSIMPORT_HISTORYSIZE, 0);
	}

	public void setHistorySize(int set) {
		setPluginConfig(RSSIMPORT_HISTORYSIZE, set);
	}
	
	public String getHistory(int entry) {
		return getPluginConfig(RSSIMPORT_HISTORY+"."+entry, "");
	}

	public void setHistory(int entry,String set) {
		setPluginConfig(RSSIMPORT_HISTORY+"."+entry, set);
	}
	
	public boolean isInHistory(String torrent) {
		if (!isHistory() || torrent==null) {
			return false;
		}
		for (int i=1;i<getHistorySize()+1;i++) {
			if (torrent.equals(getHistory(i))) {
				return true;
			}
		}
		return false;
	}
	
	public void addHistory(String torrent) {
		if (!isInHistory(torrent)) {
			setHistory(getHistorySize()+1,torrent);
			setHistorySize(getHistorySize()+1);
		}
		
	}
}
