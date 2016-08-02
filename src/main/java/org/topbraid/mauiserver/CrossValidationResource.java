package org.topbraid.mauiserver;

import java.util.List;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.tagger.AsyncJob;
import org.topbraid.mauiserver.tagger.CrossValidationJob;
import org.topbraid.mauiserver.tagger.JobReport;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TrainingDocument;

public class CrossValidationResource extends AbstractTrainingJobResource {
	private final Tagger tagger;
	
	public CrossValidationResource(ServletContext context, Tagger tagger) {
		super(context, tagger, getRelativeCrossValidationURL(tagger), tagger.getCrossValidator(), "cross-validation");
		this.tagger = tagger;
	}
	
	@Override
	protected AsyncJob createJob(List<TrainingDocument> corpus, JobReport report) {
		return new CrossValidationJob(tagger, corpus);
	}
	
	public static String getRelativeCrossValidationURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/xvalidate";
	}
}
