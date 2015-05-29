package org.topbraid.mauiserver;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.TaggerCollection;
import org.topbraid.mauiserver.tagger.Tagger;

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
		ArrayNode array = root.arrayNode();
		root.set("taggers", array);
		for (String id: taggers.getTaggers()) {
			Tagger tagger = taggers.getTagger(id);
			ObjectNode o = root.objectNode();
			o.put("id", tagger.getId());
			o.put("href", getContextPath() + TaggerResource.getRelativeTaggerURL(tagger));
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
		if (taggers.taggerExists(taggerId)) {
			return request.badRequest("id", taggerId, 
					"A tagger with that ID already exists");
		}
		try {
			taggers.createTagger(taggerId);
			return doGet(request);
		} catch (MauiServerException ex) {
			return request.serverError(ex);
		}
	}
}
