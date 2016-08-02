package org.topbraid.mauiserver.tagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServerException;
import org.topbraid.mauiserver.persistence.TaggerStore;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiWrapper;
import com.entopix.maui.vocab.Vocabulary;
import com.hp.hpl.jena.rdf.model.Model;

public class Tagger {
	private final static Logger log = LoggerFactory.getLogger(Tagger.class);

	/**
	 * Creates a Tagger instance that is connected to a particular
	 * directory in the tagger store. May be null if something is
	 * wrong with the store.
	 */
	public static Tagger create(String id, TaggerStore store) {
		TaggerConfiguration config = store.getConfigurationStore(id).get();
		if (config == null) {
			log.warn("Configuration file for tagger " + id + " not found. Using defaults.");
			config = TaggerConfiguration.createWithDefaults(id);
			store.getConfigurationStore(id).put(config);
		}
		return new Tagger(config, store);
	}
	
	private final String id;
	private final TaggerStore store;
	private TaggerConfiguration configuration;
	private Model jenaVocabulary = null;		// lazy loading
	private Vocabulary mauiVocabulary = null;	// lazy loading
	private JobController trainer;
	private JobController crossValidator;
	private MauiFilter mauiModel = null;		// lazy loading
	private MauiWrapper mauiWrapper = null;		// lazy loading
	
	private Tagger(TaggerConfiguration config, TaggerStore store) {
		this.id = config.getId();
		this.configuration = config;
		this.store = store;
		this.trainer = new JobController(store.getTrainerReportStore(this.id));
		this.crossValidator = new JobController(store.getCrossValidatorReportStore(this.id));
	}
	
	public String getId() {
		return id;
	}

	public boolean isTrained() {
		return store.getMauiModelStore(id).contains();
	}
	
	public boolean hasVocabulary() {
		return store.getVocabularyStore(id).contains();
	}
	
	public Model getVocabularyJena() {
		if (jenaVocabulary == null) {
			jenaVocabulary = store.getVocabularyStore(id).get();
		}
		return jenaVocabulary;
	}

	public Vocabulary getVocabularyMaui() {
		if (!hasVocabulary()) return null;
		if (mauiVocabulary == null) {
			mauiVocabulary = toMauiVocabulary(getVocabularyJena());
		}
		return mauiVocabulary;
	}
	
	/**
	 * Sets the vocabulary model.
	 * 
	 * TODO: The two awkward parameters indicate that we probably should have our own Vocabulary class that encapsulates both
	 * 
	 * @param model The vocabulary as a Jena model using SKOS; <code>null</code> deletes the vocabulary
	 * @param mauiVocabulary Must be the result of {@link #toMauiVocabulary(Model)}
	 */
	public void setVocabulary(Model model, Vocabulary mauiVocabulary) {
		this.jenaVocabulary = model;
		this.mauiVocabulary = mauiVocabulary;
		store.getVocabularyStore(id).put(model);
		// Model needs to be retrained on new vocabulary, so delete the old model
		setMauiModel(null);
		store.getTrainerReportStore(id).delete();
	}
	
	public TaggerConfiguration getConfiguration() {
		return configuration;
	}
	
	public void setConfiguration(TaggerConfiguration configuration) {
		this.configuration = configuration;
		store.getConfigurationStore(id).put(configuration);
	}
	
	public JobController getTrainer() {
		return trainer;
	}

	private MauiFilter getMauiModel() {
		if (mauiModel == null) {
			mauiModel = store.getMauiModelStore(id).get();
		}
		return mauiModel;
	}
	
	public void setMauiModel(MauiFilter mauiModel) {
		this.mauiModel = mauiModel;
		store.getMauiModelStore(id).put(mauiModel);
		mauiWrapper = null;
	}

	public void setTrainingReport(JobReport report) {
		store.getTrainerReportStore(id).put(report);
	}
	
	public JobController getCrossValidator() {
		return crossValidator;
	}

	public RecommendationResult recommendTags(String text) {
		if (!hasVocabulary() || !isTrained()) return null;
		try {
			if (log.isDebugEnabled()) {
				String shortText = text.substring(0, Math.min(text.length(), 50));
				if (text.length() > 50) shortText += "…";
				log.debug("Running recommender on " + text.length() + "b: " + shortText);
			}
			RecommendationResult result = new RecommendationResult(
					getMauiWrapper().extractTopicsFromText(text, configuration.getMaxTopicsPerDocument()));
			if (log.isDebugEnabled()) {
				log.debug("Recommendation result: " + result);
			}
			return result;
		} catch (Exception ex) {
			log.error("Error running recommender", ex);
			throw new MauiServerException("Error running recommender: " + ex.getMessage(), ex);
		}
	}

	public Vocabulary toMauiVocabulary(Model vocabulary) {
		if (vocabulary == null) return null;
		Vocabulary result = new Vocabulary();
		result.setStemmer(getConfiguration().getStemmer());
		result.setStopwords(getConfiguration().getStopwords());
		result.setLanguage(getConfiguration().getEffectiveLang());
		result.setVocabularyName(store.getVocabularyFile(id).getAbsolutePath());
		try {
			result.initializeFromModel(vocabulary);
		} catch (Exception ex) {
			log.error("Error initializing the vocabulary in Maui", ex);
			throw new MauiServerException("Error initializing the vocabulary in Maui: " + ex.getMessage(), ex);
		}
		return result;
	}
	
	private MauiWrapper getMauiWrapper() {
		if (mauiWrapper == null) {
			if (!isTrained()) return null;
			mauiWrapper = getMauiWrapper(getMauiModel());
		}
		return mauiWrapper;
	}
	
	public MauiWrapper getMauiWrapper(MauiFilter forMauiModel) {
		if (getVocabularyMaui() == null) return null;
		forMauiModel.setVocabulary(getVocabularyMaui());
		return new MauiWrapper(getVocabularyMaui(), forMauiModel);
	}
}
