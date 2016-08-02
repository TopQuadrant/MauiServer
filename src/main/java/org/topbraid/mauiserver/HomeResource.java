package org.topbraid.mauiserver;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		ObjectNode root = response.getRoot();
		root.put("title", "Maui Server");
		root.put("data_dir", taggers.getDataDir());
		root.put("default_lang", MauiServer.getDefaultLanguage());
		root.put("version", MauiServer.getVersion());
		ArrayNode array = root.arrayNode();
		root.set("taggers", array);
		for (String id: taggers.getTaggers()) {
			Tagger tagger = taggers.getTagger(id);
			if (tagger == null) continue;
			ObjectNode o = root.objectNode();
			o.put("id", tagger.getId());
			o.put("href", getContextPath() + TaggerResource.getRelativeTaggerURL(tagger));
			o.put("title", tagger.getConfiguration().getTitle());
			if (tagger.getConfiguration().getDescription() != null) {
				o.put("description", tagger.getConfiguration().getDescription());
			}
			array.add(o);
		}
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
			JsonNode json = request.getBodyJSON();
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
