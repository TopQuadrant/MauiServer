package org.topbraid.mauiserver.tagger;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.vocab.Vocabulary;

/**
 * A training job that can be executed asynchronously on its own thread.
 * Results are stored in the given {@link Tagger} instance, as its
 * Maui model.
 */
public class TrainingJob implements AsyncJob {
	private static final Logger log = LoggerFactory.getLogger(TrainingJob.class);
	
	private final Tagger tagger;
	private final List<TrainingDocument> corpus;
	
	public TrainingJob(Tagger tagger, List<TrainingDocument> corpus) {
		this.tagger = tagger;
		this.corpus = corpus;
	}
	
	public String getActivityName() {
		return "training";
	}
	
	public void run(JobReport report) throws MauiFilterException {
		log.debug("Training started");
		MauiFilter result = doTrainModel();
		if (Thread.currentThread().isInterrupted()) return;
		tagger.setMauiModel(result);
		log.debug("Training stopped");
	}
	
	public MauiFilter doTrainModel() throws MauiFilterException {
		List<MauiDocument> mauiDocs = new ArrayList<MauiDocument>(corpus.size());
		for (TrainingDocument doc: corpus) {
			mauiDocs.add(doc.asMauiDocument());
		}
		return createModelBuilder().buildModel(mauiDocs);
	}
	
	private MauiModelBuilder createModelBuilder() {
		MauiModelBuilder modelBuilder = new MauiModelBuilder();

		// Set features
		modelBuilder.setBasicFeatures(true);
		modelBuilder.setKeyphrasenessFeature(true);
		modelBuilder.setFrequencyFeatures(true);
		modelBuilder.setPositionsFeatures(true);
		modelBuilder.setLengthFeature(true);
		modelBuilder.setThesaurusFeatures(true);
		
		// change to 1 for short documents
//		modelBuilder.minNumOccur = 2;

		// Language selection stuff
		modelBuilder.documentLanguage = tagger.getConfiguration().getEffectiveLang();
		log.info("Using document language: " + modelBuilder.documentLanguage);
		modelBuilder.stemmer = tagger.getConfiguration().getStemmer();
		log.info("Using stemmer: " + modelBuilder.stemmer.getClass().getCanonicalName());
		modelBuilder.stopwords = tagger.getConfiguration().getStopwords();
		log.info("Using stopwords: " + modelBuilder.stopwords.getClass().getCanonicalName());
		
		// Vocabulary stuff
		// TODO: Get the actual vocabulary name as it is used in the Vocabulary instance
		Vocabulary vocabulary = tagger.getVocabularyMaui();
		modelBuilder.setVocabularyName("dummy.ttl");
		modelBuilder.setVocabulary(vocabulary);
		vocabulary.setStemmer(modelBuilder.stemmer);
		vocabulary.setStopwords(modelBuilder.stopwords);

		return modelBuilder;
	}
}
