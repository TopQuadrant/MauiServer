package org.topbraid.mauiserver;

import java.util.List;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.JsonLinesParser;
import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.AsyncJob;
import org.topbraid.mauiserver.tagger.JobController;
import org.topbraid.mauiserver.tagger.JobReport;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TrainingDataParser;
import org.topbraid.mauiserver.tagger.TrainingDocument;

/**
 * A resource in charge of a {@link JobController} that processes
 * training data and requires a configured vocabulary.
 * 
 * TODO: 
 */
public abstract class AbstractTrainingJobResource extends AbstractJobControllerResource {
	private final Tagger tagger;
	
	public AbstractTrainingJobResource(ServletContext context, Tagger tagger, String relativeURL, JobController jobController, String processName) {
		super(context, relativeURL, jobController, processName);
		this.tagger = tagger;
	}

	abstract AsyncJob createJob(List<TrainingDocument> corpus, JobReport report);
	
	@Override
	public AsyncJob createJob(Request request, JobReport report) {
		JsonLinesParser in = request.getBodyJsonLines(false);
		if (in == null) {
			throw new MauiServerException("Training corpus in JSON Lines format must be sent in request body");
		}
		int skipped = -1;
		TrainingDataParser parser = new TrainingDataParser(in);
		List<TrainingDocument> corpus = parser.getCorpus();
		skipped = parser.getSkippedDocumentCount();
		if (corpus.isEmpty()) {
			throw new MauiServerException("0 out of " + skipped + " documents were usable as training documents; check expected JSON format!");
		}
		report.logDocumentCounts(corpus.size(), skipped);
		return createJob(corpus, report);
	}
	
	@Override
	public Response doPost(Request request) {
		if (!tagger.hasVocabulary()) {
			return request.conflict("Tagger must have a vocabulary before " + getProcessName() + " can be started.");
		}
		return super.doPost(request);
	}

	@Override
	protected JSONResponse createStatusReport(Request request) {
		JSONResponse response = super.createStatusReport(request);
		if ("ready".equals(response.getRoot().get("service_status").asText()) && !tagger.hasVocabulary()) {
			response.getRoot().put("service_status", "no vocabulary");
		}
		return response;
	}
}
