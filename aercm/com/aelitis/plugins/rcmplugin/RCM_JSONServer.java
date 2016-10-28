/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.plugins.rcmplugin;

import java.util.*;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.search.*;

import com.aelitis.azureus.core.content.*;
import com.aelitis.azureus.util.MapUtils;

public final class RCM_JSONServer
	implements Utilities.JSONServer
{
	private final RCMPlugin rcmPlugin;

	RCM_JSONServer(RCMPlugin rcmPlugin) {
		this.rcmPlugin = rcmPlugin;
	}

	private Map<String, SearchInstance> mapSearchInstances = new HashMap<String, SearchInstance>();

	private Map<String, Map> mapSearchResults = new HashMap<String, Map>();

	private List<String> methods = new ArrayList<String>();
	{
		methods.add("rcm-is-enabled");
		methods.add("rcm-get-list");
		methods.add("rcm-lookup-start");
		methods.add("rcm-lookup-remove");
		methods.add("rcm-lookup-get-results");
		methods.add("rcm-set-enabled");
	}

	public String getName() {
		return ("SwarmDiscoveries");
	}

	public List<String> getSupportedMethods() {
		return (methods);
	}

	public Map call(String method, Map args)

			throws PluginException {
		if (rcmPlugin.isDestroyed()) {

			throw (new PluginException("Plugin unloaded"));
		}

		Map<String, Object> result = new HashMap<String, Object>();

		if (method.equals("rcm-is-enabled")) {

			result.put("enabled", rcmPlugin.isRCMEnabled());
			result.put("sources", rcmPlugin.getSourcesList());
			result.put("is-all-sources", rcmPlugin.isAllSources());
			result.put("is-default-sources", rcmPlugin.isDefaultSourcesList());
			result.put("ui-enabled", rcmPlugin.isUIEnabled());

		} else if (method.equals("rcm-get-list")) {

			if (rcmPlugin.isRCMEnabled() && rcmPlugin.isUIEnabled()) {
				rpcGetList(result, args);
			} else {
				throw (new PluginException("RCM not enabled"));
			}

		} else if (method.equals("rcm-set-enabled")) {

			boolean enable = MapUtils.getMapBoolean(args, "enable", false);
			boolean all = MapUtils.getMapBoolean(args, "all-sources", false);
			if (enable) {
				rcmPlugin.setRCMEnabled(enable);
			}

			rcmPlugin.setSearchEnabled(enable);
			rcmPlugin.setUIEnabled(enable);

			rcmPlugin.setFTUXBeenShown(true);

			if (all) {
				rcmPlugin.setToAllSources();
			} else {
				rcmPlugin.setToDefaultSourcesList();
			}

		} else if (method.equals("rcm-lookup-start")) {

			if (rcmPlugin.isRCMEnabled() && rcmPlugin.isUIEnabled()) {
				rpcLookupStart(result, args);
			} else {
				throw (new PluginException("RCM not enabled"));
			}

		} else if (method.equals("rcm-lookup-remove")) {

			if (rcmPlugin.isRCMEnabled() && rcmPlugin.isUIEnabled()) {
				rpcLookupRemove(result, args);
			} else {
				throw (new PluginException("RCM not enabled"));
			}

		} else if (method.equals("rcm-lookup-get-results")) {

			if (rcmPlugin.isRCMEnabled() && rcmPlugin.isUIEnabled()) {
				rpcLookupGetResults(result, args);
			} else {
				throw (new PluginException("RCM not enabled"));
			}

		} else {

			throw (new PluginException("Unsupported method"));
		}

		return (result);
	}

	public void unload() {
		if (mapSearchResults != null) {
			mapSearchResults.clear();
		}
		if (mapSearchInstances != null) {
			for (SearchInstance si : mapSearchInstances.values()) {
				try {
					si.cancel();
				} catch (Throwable t) {
				}
			}
			mapSearchInstances.clear();
		}
	}

	private Map<String, Object> relatedContentToMap(RelatedContent item) {
		HashMap<String, Object> map = new HashMap<String, Object>();

		long changedLocallyOn = item.getChangedLocallyOn();

		map.put("changedOn", changedLocallyOn);
		map.put("contentNetwork", item.getContentNetwork());
		map.put("hash", ByteFormatter.encodeString(item.getHash()));
		map.put("lastSeenSecs", item.getLastSeenSecs());
		map.put("peers", item.getLeechers());
		map.put("level", item.getLevel());
		map.put("publishDate", item.getPublishDate());
		map.put("rank", item.getRank());
		map.put("relatedToHash",
				ByteFormatter.encodeString(item.getRelatedToHash()));
		map.put("seeds", item.getSeeds());
		map.put("size", item.getSize());
		map.put("tags", item.getTags());
		map.put("title", item.getTitle());
		map.put("tracker", item.getTracker());
		map.put("unread", item.isUnread());
		return map;
	}

	protected void rpcGetList(Map result, Map args)
			throws PluginException {
		long since = MapUtils.getMapLong(args, "since", 0);
		long until = 0;

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		result.put("related", list);
		try {
			RelatedContentManager manager = RelatedContentManager.getSingleton();
			RelatedContent[] relatedContent = manager.getRelatedContent();

			for (RelatedContent item : relatedContent) {
				if (!rcmPlugin.isVisible(item)) {
					continue;
				}

				long changedLocallyOn = item.getChangedLocallyOn();
				if (changedLocallyOn < since) {
					continue;
				}
				if (changedLocallyOn > until) {
					until = changedLocallyOn;
				}

				Map map = relatedContentToMap(item);
				list.add(map);
			}
		} catch (Exception e) {
			throw new PluginException(e);
		}

		result.put("until", until);
	}

	protected void rpcLookupGetResults(Map result, Map args)
			throws PluginException {
		// TODO: filter by "since"
		long since = MapUtils.getMapLong(args, "since", 0);

		Map map = mapSearchResults.get(MapUtils.getMapString(args, "lid", null));
		if (map == null) {
			throw new PluginException("No results for Lookup ID");
		}
		result.putAll(map);
	}

	protected void rpcLookupRemove(Map result, Map args)
			throws PluginException {
		String lid = MapUtils.getMapString(args, "lid", null);
		if (lid == null) {
			throw new PluginException("No Lookup ID");
		}
		mapSearchInstances.remove(lid);
		mapSearchResults.remove(lid);
	}

	protected void rpcLookupStart(Map result, Map args)
			throws PluginException {
		String searchTerm = MapUtils.getMapString(args, "search-term", null);
		String lookupByTorrent = MapUtils.getMapString(args, "torrent-hash", null);
		long lookupBySize = MapUtils.getMapLong(args, "file-size", 0);

		String[] networks = new String[] {
			AENetworkClassifier.AT_PUBLIC
		};
		String net_str = RCMPlugin.getNetworkString(networks);
		try {
			RelatedContentManager manager = RelatedContentManager.getSingleton();

			if (searchTerm != null) {
				final String lookupID = Integer.toHexString(
						(searchTerm + net_str).hashCode());

				result.put("lid", lookupID);

				//SearchInstance searchInstance = mapSearchInstances.get(searchID);

				Map<String, Object> parameters = new HashMap<String, Object>();
				parameters.put(SearchProvider.SP_SEARCH_TERM, searchTerm);

				//if ( networks != null && networks.length > 0 ){
				//parameters.put( SearchProvider.SP_NETWORKS, networks );
				//}

				Map map = mapSearchResults.get(lookupID);
				if (map == null) {
					map = new HashMap();
					mapSearchResults.put(lookupID, map);
				}
				int activeSearches = MapUtils.getMapInt(map, "active-searches", 0);
				map.put("active-searches", ++activeSearches);
				map.put("complete", activeSearches > 0 ? false : true);
				SearchInstance searchRCM = manager.searchRCM(parameters,
						new SearchObserver() {

							public void resultReceived(SearchInstance search,
									SearchResult result) {
								synchronized (mapSearchResults) {
									Map map = mapSearchResults.get(lookupID);
									if (map == null) {
										return;
									}

									List list = MapUtils.getMapList(map, "results", null);
									if (list == null) {
										list = new ArrayList();
										map.put("results", list);
									}

									SearchRelatedContent src = new SearchRelatedContent(result);

									Map mapResult = relatedContentToMap(src);

									list.add(mapResult);

								}
							}

							public Object getProperty(int property) {
								// TODO Auto-generated method stub
								return null;
							}

							public void complete() {
								synchronized (mapSearchResults) {
									Map map = mapSearchResults.get(lookupID);
									if (map == null) {
										return;
									}
									int activeSearches = MapUtils.getMapInt(map,
											"active-searches", 0);
									if (activeSearches > 0) {
										activeSearches--;
									}
									map.put("active-searches", activeSearches);
									map.put("complete", activeSearches > 0 ? false : true);
								}
							}

							public void cancelled() {
								complete();
							}
						});
			} else if (lookupByTorrent != null || lookupBySize > 0) {

				final String lookupID = lookupByTorrent != null ? lookupByTorrent
						: Integer.toHexString(
								(String.valueOf(lookupBySize) + net_str).hashCode());

				result.put("lid", lookupID);

				Map map = mapSearchResults.get(lookupID);
				if (map == null) {
					map = new HashMap();
					mapSearchResults.put(lookupID, map);
				}
				int activeSearches = MapUtils.getMapInt(map, "active-searches", 0);
				map.put("active-searches", ++activeSearches);
				map.put("complete", activeSearches > 0 ? false : true);
				RelatedContentLookupListener l = new RelatedContentLookupListener() {

					public void lookupStart() {
					}

					public void lookupFailed(ContentException error) {
						lookupComplete();
					}

					public void lookupComplete() {
						synchronized (mapSearchResults) {
							Map map = mapSearchResults.get(lookupID);
							if (map == null) {
								return;
							}
							int activeSearches = MapUtils.getMapInt(map, "active-searches",
									0);
							if (activeSearches > 0) {
								activeSearches--;
							}
							map.put("active-searches", activeSearches);
							map.put("complete", activeSearches > 0 ? false : true);
						}
					}

					public void contentFound(RelatedContent[] content) {
						synchronized (mapSearchResults) {
							Map map = mapSearchResults.get(lookupID);
							if (map == null) {
								return;
							}

							List list = MapUtils.getMapList(map, "results", null);
							if (list == null) {
								list = new ArrayList();
								map.put("results", list);
							}

							for (RelatedContent item : content) {
								Map<String, Object> mapResult = relatedContentToMap(item);
								list.add(mapResult);
							}

						}
					}
				};
				if (lookupByTorrent != null) {
					byte[] hash = ByteFormatter.decodeString(lookupByTorrent);
					manager.lookupContent(hash, networks, l);
				} else if (lookupBySize > 0) {
					manager.lookupContent(lookupBySize, l);
				}

			} else {
				throw new PluginException("No search-term, torrent-hash or file-size");
			}
		} catch (Exception e) {
			throw new PluginException(e);
		}
	}
}