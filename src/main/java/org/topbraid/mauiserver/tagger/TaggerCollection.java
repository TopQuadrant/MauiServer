package org.topbraid.mauiserver.tagger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.topbraid.mauiserver.MauiServerException;

public class TaggerCollection {
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
		return store.listTaggers();
	}
	
	public Tagger getTagger(String id) {
		if (taggerExists(id)) {
			if (!cachedTaggers.containsKey(id)) {
				cachedTaggers.put(id, new Tagger(id, store));
			}
		} else {
			cachedTaggers.remove(id);
		}
		return cachedTaggers.get(id);
	}
	
	public boolean taggerExists(String id) {
		return store.taggerExists(id);
	}
	
	public Tagger createTagger(String id) {
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
}
