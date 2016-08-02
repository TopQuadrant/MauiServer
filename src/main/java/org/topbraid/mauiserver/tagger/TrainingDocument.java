package org.topbraid.mauiserver.tagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.entopix.maui.util.MauiDocument;

public class TrainingDocument {
	private final String id;
	private final String text;
	private final List<String> topics;
	
	public TrainingDocument(String id, String text, Collection<String> topics) {
		this.id = id;
		this.text = text;
		this.topics = Collections.unmodifiableList(new ArrayList<String>(topics));
	}
	
	public String getId() {
		return id;
	}
	
	public String getText() {
		return text;
	}
	
	public List<String> getTopics() {
		return topics;
	}
	
	public MauiDocument asMauiDocument() {
		return new MauiDocument(id, id, text, toMauiTopicsString(topics));		
	}
	
	private static String toMauiTopicsString(Collection<String> topics) {
		StringBuilder result = new StringBuilder();
		for (String topic: topics) {
			result.append(topic);
			result.append('\n');
		}
		return result.toString();
	}
}
