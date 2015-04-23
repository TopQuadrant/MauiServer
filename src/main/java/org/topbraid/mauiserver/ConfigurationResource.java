package org.topbraid.mauiserver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Resource.Puttable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TaggerConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

public class ConfigurationResource extends Resource
		implements Gettable, Postable, Puttable {
	private Tagger tagger;

	public ConfigurationResource(ServletContext context, Tagger tagger) {
		super(context);
		this.tagger = tagger;
	}

	@Override
	public String getURL() {
		return getContextPath() + getRelativeConfigurationURL(tagger);
	}
	
	@Override
	public Response doGet(Request request) {
		JSONResponse r = request.respondJSON(HttpServletResponse.SC_OK);
		r.getRoot().setAll(tagger.getConfiguration().toJSON(r.getRoot()));
		return r;
	}

	@Override
	public Response doPut(Request request) {
		try {
			JsonNode json = request.getBodyJSON();
			if (json == null) {
				return request.badRequest("Configuration in JSON format must be sent in request body");
			}
			tagger.setConfiguration(TaggerConfiguration.fromJSON(json));
			JSONResponse response = request.respondJSON(200);
			response.getRoot().setAll(tagger.getConfiguration().toJSON(response.getRoot()));
			return response;
		} catch (MauiServerException ex) {
			return request.badRequest(ex.getMessage());
		}
	}

	@Override
	public Response doPost(Request request) {
		try {
			JsonNode json = request.getBodyJSON();
			if (json == null) {
				return request.badRequest("Configuration in JSON format must be sent in request body");
			}
			TaggerConfiguration config = tagger.getConfiguration();
			config.updateFromJSON(json);
			tagger.setConfiguration(config);
			JSONResponse response = request.respondJSON(200);
			response.getRoot().setAll(tagger.getConfiguration().toJSON(response.getRoot()));
			return response;
		} catch (MauiServerException ex) {
			return request.badRequest(ex.getMessage());
		}
	}

	public static String getRelativeConfigurationURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/config";
	}
}
