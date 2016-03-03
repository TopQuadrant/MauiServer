package org.topbraid.mauiserver.tagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServerException;

public class TaggerCollection {
	private final static Logger log = LoggerFactory.getLogger(TaggerCollection.class);

	private final String dataDir;
	private final TaggerStore store;
	private final Map<String, Tagger> cachedTaggers = new HashMap<String, Tagger>();

	public TaggerCollection(String dataDir) {
		this.dataDir = dataDir;
		store = new TaggerStore(dataDir);
	}

	public String getDataDir() {
		return dataDir;
	}
	
	public Collection<String> getTaggers() {
		Collection<String> results = new ArrayList<String>();
		for (String id: store.listTaggers()) {
			if (!isValidTaggerId(id)) {
				log.warn("Skipping invalid tagger ID listed in TaggerStore: " + id);
				continue;
			}
			results.add(id);
		}
		return results;
	}
	
	public Tagger getTagger(String id) {
		if (taggerExists(id)) {
			if (!cachedTaggers.containsKey(id)) {
				Tagger tagger = Tagger.create(id, store);
				if (tagger != null) {
					cachedTaggers.put(id, tagger);
				}
			}
		} else {
			cachedTaggers.remove(id);
		}
		return cachedTaggers.get(id);
	}
	
	public boolean taggerExists(String id) {
		return isValidTaggerId(id) && store.taggerExists(id);
	}
	
	public Tagger createTagger(String id) {
		if (!isValidTaggerId(id)) {
			throw new MauiServerException("Malformed tagger id: '" + id + "'");
		}
		if (taggerExists(id)) {
			throw new MauiServerException("Tagger id already in use: '" + id + "'");
		}
		store.createTagger(id);
		return getTagger(id);
	}
	
	public void deleteTagger(String id) {
		if (!store.taggerExists(id)) {
			throw new MauiServerException("Tagger does not exist: '" + id + "'");
		}
		store.deleteTagger(id);
		cachedTaggers.remove(id);
	}
	
	public boolean isValidTaggerId(String id) {
		if (id == null) return false;
		if ("".equals(id)) return false;
		// Tomcat disallows forward and back slashes
		// (encoded as %2F and %5C) in the path part of the URL,
		// so we disallow them
		if (id.contains("/")) return false;
		if (id.contains("\\")) return false;
		return true;
	}
}
