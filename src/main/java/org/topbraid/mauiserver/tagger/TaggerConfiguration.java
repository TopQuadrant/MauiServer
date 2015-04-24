package org.topbraid.mauiserver.tagger;

import org.topbraid.mauiserver.MauiServerException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaggerConfiguration {
	private String title;
	private String description = null;
	
	private final static String fieldTitle = "title";
	private final static String fieldDescription = "description";
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public ObjectNode toJSON(JsonNodeCreator factory) {
		ObjectNode result = factory.objectNode();
		result.put(fieldTitle, title);
		result.put(fieldDescription, description);
		return result;
	}

	public void updateFromJSON(JsonNode config) {
		if (!config.isObject()) {
			throw new MauiServerException("Configuration JSON must be an object, was " + config.getNodeType());
		}
		if (config.has(fieldTitle)) setTitle(config.get(fieldTitle).textValue());
		if (config.has(fieldDescription)) setDescription(config.get(fieldDescription).textValue());
	}
	
	public static TaggerConfiguration fromJSON(JsonNode config) {
		TaggerConfiguration result = new TaggerConfiguration();
		result.updateFromJSON(config);
		if (!config.has("title")) {
			throw new MauiServerException("Configuration JSON must have a value for 'title'");
		}
		return result;
	}
	
	public static TaggerConfiguration createWithDefaults(String taggerId) {
		TaggerConfiguration result = new TaggerConfiguration();
		result.setTitle(taggerId);
		return result;
	}
}
