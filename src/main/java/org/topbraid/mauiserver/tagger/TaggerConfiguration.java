package org.topbraid.mauiserver.tagger;

import java.util.HashMap;
import java.util.Map;

import org.topbraid.mauiserver.MauiServer;
import org.topbraid.mauiserver.MauiServerException;

import com.entopix.maui.stemmers.FrenchStemmer;
import com.entopix.maui.stemmers.GermanStemmer;
import com.entopix.maui.stemmers.PorterStemmer;
import com.entopix.maui.stemmers.SpanishStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsEnglish;
import com.entopix.maui.stopwords.StopwordsFrench;
import com.entopix.maui.stopwords.StopwordsGerman;
import com.entopix.maui.stopwords.StopwordsSpanish;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaggerConfiguration {
	private final String id;
	private String title;
	private String description = null;
	private String lang = null;
	private String stemmerClass = null;
	private String stopwordsClass = null;
	private final String defaultLang = MauiServer.getDefaultLanguage();
	private int crossValidationPasses = 10;
	private int maxTopicsPerDocument = 10;
	private double probabilityThreshold = 0.0;
	
	private final static String fieldId = "id";
	private final static String fieldTitle = "title";
	private final static String fieldDescription = "description";
	private final static String fieldLang = "lang";
	private final static String fieldStemmerClass = "stemmer_class";
	private final static String fieldStopwordsClass = "stopwords_class";
	private final static String fieldCrossValidationPasses = "cross_validation_passes";
	private final static String fieldMaxTopicsPerDocument = "max_topics_per_document";
	private final static String fieldProbabilityThreshold = "probability_threshold";
	
	@SuppressWarnings("serial")
	private final static Map<String,Class<? extends Stemmer>> stemmerRegistry = new HashMap<String,Class<? extends Stemmer>>() {{
		put("en", PorterStemmer.class);
		put("fr", FrenchStemmer.class);
		put("de", GermanStemmer.class);
		put("es", SpanishStemmer.class);
	}};
	
	@SuppressWarnings("serial")
	private final static Map<String,Class<? extends Stopwords>> stopwordsRegistry = new HashMap<String,Class<? extends Stopwords>>() {{
		put("en", StopwordsEnglish.class);
		put("fr", StopwordsFrench.class);
		put("de", StopwordsGerman.class);
		put("es", StopwordsSpanish.class);
	}};
	
	public TaggerConfiguration(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
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
	
	public String getLang() {
		return lang;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public String getEffectiveLang() {
		return lang != null ? lang : defaultLang;
	}

	@SuppressWarnings("unchecked")
	private <T> Class<? extends T> getClass(Class<T> superclass, String className) {
		try {
			Class<?> result = Class.forName(className);
			if (!superclass.isAssignableFrom(result)) {
				throw new MauiServerException("Class " + className + " does not implement/extend " + superclass.getCanonicalName());
			}
			return (Class<? extends T>) result;
		} catch (ClassNotFoundException ex) {
			throw new MauiServerException(ex);
		}
	}
	
	private <T> T instantiate(Class<T> class_) {
		try {
			return class_.newInstance();
		} catch (InstantiationException ex) {
			throw new MauiServerException(ex);
		} catch (IllegalAccessException ex) {
			throw new MauiServerException(ex);
		}
	}
	
	public String getStemmerClass() {
		return stemmerClass;
	}
	
	public void setStemmerClass(String className) {
		this.stemmerClass = className;
	}

	private Class<? extends Stemmer> getEffectiveStemmerClass() {
		if (stemmerClass != null) {
			return getClass(Stemmer.class, stemmerClass);
		}
		if (!stemmerRegistry.containsKey(getEffectiveLang())) {
			throw new MauiServerException("No stemmer class registered for language '" + getEffectiveLang() + "'");
		}
		return stemmerRegistry.get(getEffectiveLang());
	}

	public Stemmer getStemmer() throws MauiServerException {
		return instantiate(getEffectiveStemmerClass());
	}
	
	public String getStopwordsClass() {
		return stopwordsClass;
	}
	
	public void setStopwordsClass(String className) {
		this.stopwordsClass = className;
	}
	
	private Class<? extends Stopwords> getEffectiveStopwordsClass() {
		if (stopwordsClass != null) {
			return getClass(Stopwords.class, stopwordsClass);
		}
		if (!stopwordsRegistry.containsKey(getEffectiveLang())) {
			throw new MauiServerException("No stopwords class registered for language '" + getEffectiveLang() + "'");
		}
		return stopwordsRegistry.get(getEffectiveLang());
	}

	public Stopwords getStopwords() throws MauiServerException {
		return instantiate(getEffectiveStopwordsClass());
	}
	
	public int getCrossValidationPasses() {
		return crossValidationPasses;
	}
	
	public void setCrossValidationPasses(int number) {
		if (number <= 1) {
			throw new IllegalArgumentException(fieldCrossValidationPasses + " must be at least 2");
		}
		crossValidationPasses = number;
	}
	
	public int getMaxTopicsPerDocument() {
		return maxTopicsPerDocument;
	}
	
	public void setMaxTopicsPerDocument(int number) {
		if (number <= 1) {
			throw new IllegalArgumentException(fieldMaxTopicsPerDocument + " must be at least 2");
		}
		maxTopicsPerDocument = number;
	}
	
	public double getProbabilityThreshold() {
		return probabilityThreshold;
	}
	
	public void setProbabilityThreshold(double number) {
		if (number < 0.0) {
			number = 0.0;
		}
		if (number > 1.0) {
			number = 1.0;
		}
		probabilityThreshold = number;
	}
	
	public ObjectNode toJSON() {
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.put(fieldId, id);
		result.put(fieldTitle, title);
		result.put(fieldDescription, description);
		result.put(fieldLang, lang);
		result.put(fieldStemmerClass, stemmerClass);
		result.put(fieldStopwordsClass, stopwordsClass);
		result.put(fieldCrossValidationPasses, crossValidationPasses);
		result.put(fieldMaxTopicsPerDocument, maxTopicsPerDocument);
		result.put(fieldProbabilityThreshold, probabilityThreshold);
		return result;
	}

	public void updateFromJSON(JsonNode config) {
		if (!config.isObject()) {
			throw new MauiServerException("Configuration JSON must be an object, was " + config.getNodeType());
		}
		if (config.has(fieldTitle)) setTitle(config.get(fieldTitle).textValue());
		if (config.has(fieldDescription)) setDescription(config.get(fieldDescription).textValue());
		if (config.has(fieldLang)) setLang(config.get(fieldLang).textValue());
		if (config.has(fieldStemmerClass)) setStemmerClass(config.get(fieldStemmerClass).textValue());
		if (config.has(fieldStopwordsClass)) setStopwordsClass(config.get(fieldStopwordsClass).textValue());
		if (config.has(fieldCrossValidationPasses)) setCrossValidationPasses(config.get(fieldCrossValidationPasses).asInt());
		if (config.has(fieldMaxTopicsPerDocument)) setMaxTopicsPerDocument(config.get(fieldMaxTopicsPerDocument).asInt());
		if (config.has(fieldProbabilityThreshold)) setProbabilityThreshold(config.get(fieldProbabilityThreshold).asDouble());
	}

	public static TaggerConfiguration fromJSON(JsonNode config, String defaultId, boolean ignoreIdInConfig) {
		String id;
		if (ignoreIdInConfig) {
			id = defaultId;
		} else {
			id = config.has(fieldId) ? config.get(fieldId).textValue() : defaultId;
		}
		TaggerConfiguration result = new TaggerConfiguration(id);
		result.updateFromJSON(config);
		if (!config.has("title")) {
			throw new MauiServerException("Configuration JSON must have a value for 'title'");
		}
		return result;
	}
	
	public static TaggerConfiguration createWithDefaults(String taggerId) {
		TaggerConfiguration result = new TaggerConfiguration(taggerId);
		result.setTitle(taggerId);
		return result;
	}
}
