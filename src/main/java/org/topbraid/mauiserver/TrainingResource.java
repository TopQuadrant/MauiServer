package org.topbraid.mauiserver;

import java.util.List;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.JsonLinesParser;
import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.Trainer;
import org.topbraid.mauiserver.tagger.TrainingDataParser;

import com.entopix.maui.util.MauiDocument;

public class TrainingResource extends Resource implements Gettable, Postable, Deletable {
	private final Tagger tagger;
	
	public TrainingResource(ServletContext context, Tagger tagger) {
		super(context);
		this.tagger = tagger;
	}
	
	@Override
	public String getURL() {
		return getContextPath() + getRelativeTrainingURL(tagger);
	}
	
	@Override
	public Response doGet(Request request) {
		return createStatusReport(request);
	}
	
	@Override
	public Response doPost(Request request) {
		if (!tagger.hasVocabulary()) {
			return request.conflict("Tagger must have a vocabulary before it can be trained.");
		}
		Trainer trainer = tagger.getTrainer();
		try {
			trainer.lock();
		} catch (IllegalStateException ex) {
			return request.conflict("Training already in progress. You may use HTTP DELETE to cancel.");
		}
		JsonLinesParser in = request.getBodyJsonLines(false);
		if (in == null) {
			trainer.cancel();
			return request.badRequest("Training corpus in JSON Lines format must be sent in request body");
		}
		int skipped = -1;
		try {
			TrainingDataParser parser = new TrainingDataParser(in);
			List<MauiDocument> corpus = parser.getCorpus();
			skipped = parser.getSkippedDocumentCount();
			if (corpus.isEmpty()) {
				throw new MauiServerException("0 out of " + skipped + " documents were usable as training documents; check expected JSON format!");
			}
			trainer.train(corpus, skipped, tagger.getVocabularyMaui());
		} catch (MauiServerException ex) {
			trainer.cancel();
			return request.badRequest(ex.getMessage());
		}
		return createStatusReport(request);
		// TODO: Should persist the report, and load it on startup, so that we know when last training was done
	}
	
	@Override
	public Response doDelete(Request request) {
		if (tagger.getTrainer().isLocked()) {
			tagger.getTrainer().cancel();
		}
		tagger.setMauiModel(null, null);
		return createStatusReport(request);
	}

	private JSONResponse createStatusReport(Request request) {
		Trainer trainer = tagger.getTrainer();
		JSONResponse response = request.okJSON();
		String status;
		if (trainer.isLocked()) {
			status = "running";
		} else if (trainer.isFailed()) {
			status = "error";
		} else if (tagger.hasVocabulary()) {
			status = "ready";
		} else {
			status = "no vocabulary";
		}
		response.getRoot().put("training_status", status);
		if (trainer.getReport().getRuntime() >= 0) {
			response.getRoot().put("runtime_millis", trainer.getReport().getRuntime());
		}
		trainer.getReport().toJSON(response.getRoot());
		return response;
	}

	public static String getRelativeTrainingURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/train";
	}
}
