package org.topbraid.mauiserver;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.RecommendationResult;
import org.topbraid.mauiserver.tagger.Tagger;

public class SuggestResource extends Resource implements Gettable, Postable {
	private final Tagger tagger;
	
	public SuggestResource(ServletContext context, Tagger tagger) {
		super(context);
		this.tagger = tagger;
	}
	
	@Override
	public String getURL() {
		return getContextPath() + getRelativeSuggesterURL(tagger);
	}
	
	@Override
	public Response doGet(Request request) {
		if (request.get("text") != null) {
			return doSuggest(request, request.get("text"));
		}
		JSONResponse r = request.okJSON();
		r.getRoot().add("title", "Tag Suggestion Service for Tagger: " + tagger.getId());
		r.getRoot().add("usage", "GET or POST with parameter 'text' to get tag suggestions");
		r.getRoot().add("is_ready", tagger.isTrained());
		return r;
	}
	
	@Override
	public Response doPost(Request request) {
		if (request.get("text") == null)
			return request.badRequest("text", "Missing field: 'text'");
		if (request.get("text").trim().isEmpty()) {
			return request.badRequest("text", "Empty text");
		}
		return doSuggest(request, request.get("text"));
	}

	private Response doSuggest(Request request, String text) {
		if (!tagger.isTrained()) {
			return request.badRequest(
					"Tagger must be trained before Tag Suggestion Service can be used");
		}
		RecommendationResult recommendation = tagger.recommendTags(text);
		JSONResponse response = request.okJSON();
		response.getRoot().add("title", recommendation.size() + " recommendations from " + tagger.getId());
		recommendation.toJSON(response.getRoot());
		return response;
	}
	
	public static String getRelativeSuggesterURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/suggest";
	}
}
