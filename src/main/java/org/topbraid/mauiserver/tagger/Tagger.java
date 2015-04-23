package org.topbraid.mauiserver.tagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServerException;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.main.MauiWrapper;
import com.entopix.maui.vocab.Vocabulary;
import com.hp.hpl.jena.rdf.model.Model;

public class Tagger {
	private final static Logger log = LoggerFactory.getLogger(Tagger.class);
	private final static int topicsPerDocument = 10;

	private final String id;
	private final TaggerStore store;
	private TaggerConfiguration configuration;
	private Model jenaVocabulary;
	private Vocabulary mauiVocabulary;
	private Trainer trainer;
	private MauiFilter mauiModel;
	private MauiWrapper mauiWrapper;
	
	public Tagger(String id, TaggerStore store) {
		this.id = id;
		this.store = store;
		this.configuration = store.readConfiguration(id);
		this.jenaVocabulary = store.readVocabulary(id);
		this.trainer = new Trainer(this);
		this.mauiModel = store.readMauiModel(id);
		this.mauiVocabulary = toMauiVocabulary(jenaVocabulary);
		updateMauiWrapper();
	}
	
	public String getId() {
		return id;
	}

	public boolean isTrained() {
		return store.hasMauiModel(id);
	}
	
	public boolean hasVocabulary() {
		return jenaVocabulary != null;
	}
	
	public Model getVocabularyJena() {
		return jenaVocabulary;
	}

	public Vocabulary getVocabularyMaui() {
		return mauiVocabulary;
	}
	
	/**
	 * Sets the vocabulary model. An argument of <code>null</code> deletes the vocabulary.
	 */
	public void setVocabulary(Model model) {
		this.jenaVocabulary = model;
		this.mauiVocabulary = toMauiVocabulary(model);
		store.writeVocabulary(id, model);
		updateMauiWrapper();
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
	
	public void setMauiModel(MauiFilter mauiModel) {
		this.mauiModel = mauiModel;
		store.writeMauiModel(id, mauiModel);
		updateMauiWrapper();
	}
	
	public RecommendationResult recommendTags(String text) {
		try {
			if (log.isDebugEnabled()) {
				String shortText = text.substring(0, Math.min(text.length(), 100));
				if (text.length() > 100) shortText += "…";
				log.debug("Running recommender: " + shortText);
			}
			RecommendationResult result = new RecommendationResult(
					mauiWrapper.extractTopicsFromText(text, topicsPerDocument));
			if (log.isDebugEnabled()) {
				log.debug("Recommendation result: " + result);
			}
			return result;
		} catch (Exception ex) {
			log.error("Error running recommender", ex);
			throw new MauiServerException("Error running recommender: " + ex.getMessage(), ex);
		}
	}

	private Vocabulary toMauiVocabulary(Model vocabulary) {
		if (vocabulary == null) return null;
		MauiModelBuilder modelBuilder = new MauiModelBuilder();
		Vocabulary result = new Vocabulary();
		result.setStemmer(modelBuilder.stemmer);
		result.setStopwords(modelBuilder.stopwords);
		result.setLanguage(modelBuilder.documentLanguage);
		result.setVocabularyName(store.getVocabularyFileName(id));
		try {
			result.initializeFromModel(vocabulary);
		} catch (Exception ex) {
			log.error("Error initializing the vocabulary in Maui", ex);
			throw new MauiServerException("Error initializing the vocabulary in Maui: " + ex.getMessage(), ex);
		}
		return result;
	}
	
	private void updateMauiWrapper() {
		if (mauiVocabulary == null || mauiModel == null) {
			this.mauiWrapper = null;
			return;
		}
		mauiModel.setVocabulary(mauiVocabulary);
		this.mauiWrapper = new MauiWrapper(mauiVocabulary, mauiModel);
	}
}
