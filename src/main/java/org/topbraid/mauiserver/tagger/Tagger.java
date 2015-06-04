package org.topbraid.mauiserver.tagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServerException;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiWrapper;
import com.entopix.maui.vocab.Vocabulary;
import com.hp.hpl.jena.rdf.model.Model;

public class Tagger {
	private final static Logger log = LoggerFactory.getLogger(Tagger.class);
	private final static int topicsPerDocument = 10;

	private final String id;
	private final TaggerStore store;
	private TaggerConfiguration configuration;
	private Model jenaVocabulary = null;		// lazy loading
	private Vocabulary mauiVocabulary = null;	// lazy loading
	private Trainer trainer;
	private MauiFilter mauiModel = null;		// lazy loading
	private MauiWrapper mauiWrapper = null;		// lazy loading
	
	public Tagger(String id, TaggerStore store) {
		this.id = id;
		this.store = store;
		this.configuration = store.readConfiguration(id);
		this.trainer = new Trainer(this, store.readTrainerReport(id));
	}
	
	public String getId() {
		return id;
	}

	public boolean isTrained() {
		return store.hasMauiModel(id);
	}
	
	public boolean hasVocabulary() {
		return store.hasVocabulary(id);
	}
	
	public Model getVocabularyJena() {
		if (jenaVocabulary == null) {
			jenaVocabulary = store.readVocabulary(id);
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
		store.writeVocabulary(id, model);
		// Model needs to be retrained on new vocabulary, so delete the old model
		setMauiModel(null, null);
	}
	
	public TaggerConfiguration getConfiguration() {
		return configuration;
	}
	
	public void setConfiguration(TaggerConfiguration configuration) {
		this.configuration = configuration;
		store.writeConfiguration(id, configuration);
	}
	
	public Trainer getTrainer() {
		return trainer;
	}
	
	private MauiFilter getMauiModel() {
		if (mauiModel == null) {
			mauiModel = store.readMauiModel(id);
		}
		return mauiModel;
	}
	
	public void setMauiModel(MauiFilter mauiModel, TrainerReport report) {
		this.mauiModel = mauiModel;
		store.writeMauiModel(id, mauiModel);
		store.writeTrainerReport(id, report);
		mauiWrapper = null;
	}
	
	public RecommendationResult recommendTags(String text) {
		if (!hasVocabulary() || !isTrained()) return null;
		try {
			if (log.isDebugEnabled()) {
				String shortText = text.substring(0, Math.min(text.length(), 100));
				if (text.length() > 100) shortText += "…";
				log.debug("Running recommender: " + shortText);
			}
			RecommendationResult result = new RecommendationResult(
					getMauiWrapper().extractTopicsFromText(text, topicsPerDocument));
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
		result.setVocabularyName(store.getVocabularyFileName(id));
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
			if (getVocabularyMaui() == null) return null;
			if (!isTrained()) return null;
			mauiModel.setVocabulary(getVocabularyMaui());
			mauiWrapper = new MauiWrapper(getVocabularyMaui(), getMauiModel());
		}
		return mauiWrapper;
	}
}
