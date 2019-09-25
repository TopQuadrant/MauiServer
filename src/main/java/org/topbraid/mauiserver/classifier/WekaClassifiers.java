package org.topbraid.mauiserver.classifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Singleton managing access to all known classifiers.
 * 
 * @author Holger Knublauch
 */
public class WekaClassifiers {
	
	private static WekaClassifiers singleton = new WekaClassifiers();
	
	public static WekaClassifiers get() {
		return singleton;
	}

	
	private Map<String,WekaClassifier> classifiers = new HashMap<>();
	
	

	public WekaClassifier get(String key) {
		return classifiers.get(key);
	}
	
	public Set<String> getKeys() {
		return classifiers.keySet();
	}
	
	public void put(String key, WekaClassifier classifier) {
		classifiers.put(key, classifier);
	}
	
	public synchronized boolean remove(String key) {
		if(classifiers.containsKey(key)) {
			classifiers.remove(key);
			return true;
		}
		else {
			return false;
		}
	}
}
