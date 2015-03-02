package com.aelitis.azureus.plugins.xmwebui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.RandomUtils;

import com.aelitis.azureus.core.content.ContentException;
import com.aelitis.azureus.core.content.RelatedAttributeLookupListener;
import com.aelitis.azureus.core.content.RelatedContentManager;

public class TagSearchInstance
{
	long numActiveLookups = 0;

	Map<String, List<String>> mapTags = new HashMap<String, List<String>>();

	private String sid;

	public TagSearchInstance() {
		byte[] bytes = new byte[16];

		RandomUtils.nextSecureBytes(bytes);

		sid = Base32.encode(bytes);
	}

	public void addSearch(final String hashString, byte[] torrentHash,
			String[] networks)
			throws ContentException {

		RelatedContentManager rcm = RelatedContentManager.getSingleton();
		synchronized (this) {
			numActiveLookups++;
		}
		try {
			rcm.lookupAttributes(torrentHash, networks,
					new RelatedAttributeLookupListener() {

						public void tagFound(String tag, String network) {
							synchronized (mapTags) {
								List<String> list = mapTags.get(hashString);
								if (list == null) {
									list = new ArrayList<String>();
									mapTags.put(hashString, list);
								}
								list.add(tag);
							}
						}

						public void lookupStart() {
						}

						public void lookupFailed(ContentException error) {
						}

						public void lookupComplete() {
							synchronized (TagSearchInstance.this) {
								numActiveLookups--;
							}
						}
					});
		} catch (Exception e) {
			synchronized (this) {
				numActiveLookups--;
			}
		}
	}

	public String getID() {
		return sid;
	}

	public boolean getResults(Map result) {

		result.put("id", sid);

		List<Map> torrentList = new ArrayList<Map>();
		result.put("torrents", torrentList);

		synchronized (this) {
			synchronized (mapTags) {
				for (String hash : mapTags.keySet()) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(TransmissionVars.FIELD_TORRENT_HASH, hash);
					map.put("tags", new ArrayList<String>(mapTags.get(hash)));
					torrentList.add(map);
				}
			}
			boolean all_complete = numActiveLookups == 0;

			result.put("complete", all_complete);

			return all_complete;
		}
	}
}
