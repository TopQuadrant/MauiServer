package org.topbraid.mauiserver.tagger;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiWrapper;

/**
 * A cross-validation job that can be executed asynchronously on its own thread.
 * Results are returned in the {@link JobReport}. 
 */
public class CrossValidationJob implements AsyncJob {
	private static final Logger log = LoggerFactory.getLogger(CrossValidationJob.class);
	
	private final Tagger tagger;
	private final List<TrainingDocument> corpus;
	
	public CrossValidationJob(Tagger tagger, List<TrainingDocument> corpus) {
		this.tagger = tagger;
		this.corpus = corpus;
	}
	
	public String getActivityName() {
		return "cross-validation";
	}
	
	public void run(JobReport report) throws Exception {
		log.debug("Cross-validation started");
		
		int passes = tagger.getConfiguration().getCrossValidationPasses();
		double precisionSum = 0;
		double recallSum = 0;

		for (int pass = 0; pass < passes; pass++) {

			// Split corpus into n parts, use n-1 parts as training set, and
			// set aside one part as test set
			List<TrainingDocument> trainingSet = new ArrayList<TrainingDocument>();
			List<TrainingDocument> testSet = new ArrayList<TrainingDocument>();
			int testSetStart = pass * corpus.size() / passes;
			int testSetEnd = (pass + 1) * corpus.size() / passes;
			trainingSet.addAll(corpus.subList(0, testSetStart));
			trainingSet.addAll(corpus.subList(testSetEnd, corpus.size()));
			testSet.addAll(corpus.subList(testSetStart, testSetEnd));

			// Train a model on the training set
			MauiFilter model = new TrainingJob(tagger, trainingSet).doTrainModel();
			if (Thread.currentThread().isInterrupted()) return;
			
			double passPrecisionSum = 0;
			double passRecallSum = 0;
			
			// Tag the test set with the trained model
			MauiWrapper mauiWrapper = tagger.getMauiWrapper(model);
			
			// Compute average precision and recall over results
			for (TrainingDocument trainingDoc: testSet) {
				RecommendationResult result = new RecommendationResult(
						mauiWrapper.extractTopicsFromText(
								trainingDoc.getText(), 
								tagger.getConfiguration().getMaxTopicsPerDocument()),
						tagger.getConfiguration().getProbabilityThreshold());

				// Compute precision and recall for this document
				int correctTopicsFound = 0;
				int foundTopicsCorrect = 0;
				for (String correctId: trainingDoc.getTopics()) {
					if (result.getTitles().contains(correctId)) {
						correctTopicsFound++;
					}
				}
				for (String foundId: result.getTitles()) {
					if (trainingDoc.getTopics().contains(foundId)) {
						foundTopicsCorrect++;
					}
				}
				double docPrecision = safeDivision(foundTopicsCorrect, result.getRecommendations().size());
				double docRecall = safeDivision(correctTopicsFound, trainingDoc.getTopics().size());
//				log.debug("Document " + trainingDoc.getId() + ": " + correctTopicsFound + "/" + trainingDoc.getTopics().size() + " training topics recommended, " + foundTopicsCorrect + "/" + result.getRecommendations().size() + " recommendations correct; " + report(docPrecision, docRecall));
//				log.debug("    Training topics: " + trainingDoc.getTopics());
//				log.debug("    Recommended: " + result.getTitles());
				passPrecisionSum += docPrecision;
				passRecallSum += docRecall;
			}
			double passPrecision = safeDivision(passPrecisionSum, testSet.size());
			double passRecall = safeDivision(passRecallSum, testSet.size());
			precisionSum += passPrecision;
			recallSum += passRecall;
			log.debug("Cross-validation pass " + (pass + 1) + "/" + passes + " on " + testSet.size() + " documents complete, "
					+ report(passPrecision, passRecall));
		}
		
		double precision = precisionSum / passes;
		double recall = recallSum / passes;
		report.logPrecisionAndRecall(precision, recall);
		log.info("Cross-validation results: " + report(precision, recall));
		log.debug("Cross-validation stopped");
	}

	private double safeDivision(double x, double y) {
		if (y == 0) return 0;
		return x / y;
	}
	
	private String report(double precision, double recall) {
		return "precision = " + asPercent(precision) + ", "
				+ "recall = " + asPercent(recall);
	}
	private String asPercent(double d) {
		return Math.round(d * 10000) / 100.0 + "%";
	}
}
