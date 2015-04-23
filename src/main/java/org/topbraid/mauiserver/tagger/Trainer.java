package org.topbraid.mauiserver.tagger;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.vocab.Vocabulary;

public class Trainer {
	private static final Logger log = LoggerFactory.getLogger(Trainer.class);
	
	private final Tagger tagger;
	private boolean locked = false;
	private Thread trainingThread = null;
	private Date startTime = null;
	private Date endTime = null;
	private int documentCount = -1;
	private int skippedCount = -1;
	private String errorMessage = null;
	
	public Trainer(Tagger tagger) {
		this.tagger = tagger;
	}
	
	public synchronized void lock() {
		if (locked) throw new IllegalStateException();
		locked = true;
		startTime = new Date();
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public boolean isFailed() {
		return errorMessage != null;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Starts a training job that will be executed asynchronously on its own thread.
	 * @param corpus The document corpus
	 * @param skipped The number of "bad" input documents excluded from the corpus, for reporting purposes
	 * @param vocabulary The vocabulary to use, in Jena Model form
	 */
	public void train(final List<MauiDocument> corpus, int skipped, Vocabulary vocabulary) {
		final MauiModelBuilder modelBuilder = createModelBuilder(vocabulary);
		documentCount = corpus.size();
		skippedCount = skipped;
		trainingThread = new Thread() {
			@Override
			public void run() {
				log.debug("Training thread started");
				try {
					MauiFilter result = modelBuilder.buildModel(corpus);
					if (!Thread.currentThread().isInterrupted()) {
						tagger.setMauiModel(result);
					}
				} catch (MauiFilterException ex) {
					errorMessage = "Error while training: " + ex.getMessage();
					log.error(errorMessage, ex);
				} finally {
					done();
				}
				log.debug("Training thread stopped");
			}
		};
		trainingThread.start();
	}
	
	private synchronized void done() {
		endTime = new Date();
		locked = false;
		trainingThread = null;
	}
	
	public synchronized void cancel() {
		// TODO: Find a way of actually stopping the MauiModelBuilder
		if (trainingThread != null) {
			trainingThread.interrupt();
		}
		startTime = null;
		endTime = null;
		documentCount = -1;
		skippedCount = -1;
		errorMessage = null;
		done();
	}
	
	public long getRuntime() {
		Date end = endTime == null ? new Date() : endTime;
		return end.getTime() - startTime.getTime();
	}
	
	public Date getStartTime() {
		return startTime;
	}
	
	public Date getEndTime() {
		return endTime;
	}
	
	public int getTrainingDocumentCount() {
		return documentCount;
	}
	public int getSkippedTrainingDocumentCount() {
		return skippedCount;
	}
	
	protected MauiModelBuilder createModelBuilder(Vocabulary vocabulary) {
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

		// Vocabulary stuff
		modelBuilder.setVocabulary(vocabulary);

		return modelBuilder;
	}
}
