package org.topbraid.mauiserver.persistence;

import java.io.File;

import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.tagger.TaggerConfiguration;

public class ConfigurationStore extends JSONFileStore<TaggerConfiguration> {
	private static final Logger log = LoggerFactory.getLogger(ConfigurationStore.class);
	
	public ConfigurationStore(String taggerId, File configFile) {
		super(taggerId, configFile);
	}
	
	@Override
	protected TaggerConfiguration decode(JsonObject json) {
		TaggerConfiguration result = TaggerConfiguration.fromJSON(json, getTaggerId(), false);
		if (!result.getId().equalsIgnoreCase(getTaggerId())) {
			log.warn("Tagger ID \"" + result.getId() + 
					"\" in configuration does not match directory name \"" + getTaggerId() + "\"; skipping tagger");
			return null;
		}
		return result;
	}

	@Override
	protected JsonStructure encode(TaggerConfiguration config) {
		return config.toJSON().build();
	}
}
