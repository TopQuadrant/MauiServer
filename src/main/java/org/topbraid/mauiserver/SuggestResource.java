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
		r.getRoot().put("title", "Tag Suggestion Service for Tagger: " + tagger.getId());
		r.getRoot().put("usage", "GET or POST with parameter 'text' to get tag suggestions");
		return r;
	}
	
	@Override
	public Response doPost(Request request) {
		if (request.get("text") != null) {
			return doSuggest(request, request.get("text"));
		}
		return request.badRequest("text", "Missing field: 'text'");
	}

	private Response doSuggest(Request request, String text) {
		if (!tagger.isTrained()) {
			return request.badRequest(
					"Tagger must be trained before Tag Suggestion Service can be used");
		}
		RecommendationResult recommendation = tagger.recommendTags(text);
		JSONResponse response = request.okJSON();
		recommendation.toJSON(response.getRoot());
		return response;
	}
	
	public static String getRelativeSuggesterURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/suggest";
	}
}
