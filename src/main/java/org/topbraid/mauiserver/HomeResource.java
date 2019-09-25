package org.topbraid.mauiserver;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TaggerCollection;
import org.topbraid.mauiserver.tagger.TaggerConfiguration;

public class HomeResource extends Resource implements Gettable, Postable {
	private TaggerCollection taggers;
	
	public HomeResource(ServletContext context, TaggerCollection taggers) {
		super(context);
		this.taggers = taggers;
	}
	
	public String getURL() {
		return getContextPath() + "/";
	}

	@Override
	public Response doGet(Request request) {
		JSONResponse response = request.okJSON();
		JsonArrayBuilder array = createArrayBuilder();
		for (String id: taggers.getTaggers()) {
			Tagger tagger = taggers.getTagger(id);
			if (tagger == null) continue;
			JsonObjectBuilder o = JsonUtil.createObjectBuilderThatIgnoresNulls()
					.add("id", tagger.getId())
					.add("href", getContextPath() + TaggerResource.getRelativeTaggerURL(tagger))
					.add("title", tagger.getConfiguration().getTitle());
				if (tagger.getConfiguration().getDescription() != null) {
					o.add("description", tagger.getConfiguration().getDescription());
				}
			array.add(o);
		}
		response.getRoot()
				.add("title", "Maui Server")
				.add("data_dir", taggers.getDataDir())
				.add("default_lang", MauiServer.getDefaultLanguage())
				.add("version", MauiServer.getVersion())
				.add("taggers", array);
		return response;
	}

	@Override
	public Response doPost(Request request) {
		// TODO Handle JSON payload
		String taggerId = request.get("id");
		if (taggerId == null) {
			return request.badRequest("id", "Missing POST parameter: id");
		}
		if (!taggers.isValidTaggerId(taggerId)) {
			return request.badRequest("id", taggerId, 
					"Tagger ID must not contain \\ or / characters");
		}
		if (taggers.taggerExists(taggerId)) {
			return request.badRequest("id", taggerId, 
					"A tagger with that ID already exists");
		}
		try {
			Tagger tagger = taggers.createTagger(taggerId);
			TaggerConfiguration config = tagger.getConfiguration();
			JsonStructure json = request.getBodyJSON();
			if (json != null) {
				config.updateFromJSON(json);
			}
			tagger.setConfiguration(config);
			return doGet(request);
		} catch (MauiServerException ex) {
			return request.serverError(ex);
		}
	}
}
