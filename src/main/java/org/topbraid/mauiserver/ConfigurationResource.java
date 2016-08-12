package org.topbraid.mauiserver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Resource.Puttable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TaggerConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

public class ConfigurationResource extends Resource
		implements Gettable, Postable, Puttable, Deletable {
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
		return createConfigResponse(request);
	}

	@Override
	public Response doPut(Request request) {
		try {
			JsonNode json = request.getBodyJSON();
			if (json == null) {
				return request.badRequest("Configuration in JSON format must be sent in request body");
			}
			tagger.setConfiguration(TaggerConfiguration.fromJSON(json, tagger.getId(), true));
			return createConfigResponse(request);
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
			return createConfigResponse(request);
		} catch (MauiServerException ex) {
			return request.badRequest(ex.getMessage());
		}
	}

	@Override
	public Response doDelete(Request request) {
		TaggerConfiguration config = TaggerConfiguration.createWithDefaults(tagger.getId());
		tagger.setConfiguration(config);
		return createConfigResponse(request);
	}
	
	private Response createConfigResponse(Request request) {
		JSONResponse response = request.respondJSON(HttpServletResponse.SC_OK);
		response.getRoot().setAll(tagger.getConfiguration().toJSON());
		return response;
	}
	
	public static String getRelativeConfigurationURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/config";
	}
}
