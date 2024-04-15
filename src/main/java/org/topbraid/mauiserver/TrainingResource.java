package org.topbraid.mauiserver;

import java.util.List;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.AsyncJob;
import org.topbraid.mauiserver.tagger.JobReport;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TrainingDocument;
import org.topbraid.mauiserver.tagger.TrainingJob;

import jakarta.servlet.ServletContext;

public class TrainingResource extends AbstractTrainingJobResource {
	private final Tagger tagger;
	
	public TrainingResource(ServletContext context, Tagger tagger) {
		super(context, tagger, getRelativeTrainingURL(tagger), tagger.getTrainer(), "training");
		this.tagger = tagger;
	}
	
	@Override
	public Response doDelete(Request request) {
		tagger.setMauiModel(null);
		return super.doDelete(request);
	}

	@Override
	protected AsyncJob createJob(List<TrainingDocument> corpus, JobReport report) {
		return new TrainingJob(tagger, corpus);
	}
	
	@Override
	protected JSONResponse createStatusReport(Request request) {
		JSONResponse response = super.createStatusReport(request);

		// We trust the model store more than the report;
		// more importantly, this ensures that completed and is_trained have same value
		response.getRoot().put("completed", tagger.isTrained());
		// Deprecated legacy field; now "completed"
		response.getRoot().put("is_trained", tagger.isTrained());
		// Deprecated legacy field; now "job_status"
		response.getRoot().put("training_status", response.getRoot().get("service_status").asText());
		return response;
	}
	
	public static String getRelativeTrainingURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/train";
	}
}
