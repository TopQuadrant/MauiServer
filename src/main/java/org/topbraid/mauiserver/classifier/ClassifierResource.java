package org.topbraid.mauiserver.classifier;

import static javax.json.Json.createArrayBuilder;
import static org.topbraid.mauiserver.JsonUtil.asObject;
import static org.topbraid.mauiserver.JsonUtil.isObject;

import javax.json.JsonStructure;
import javax.servlet.ServletContext;

import org.topbraid.mauiserver.JsonUtil;
import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Resource.Puttable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;

/**
 * Example Ajax calls:
 * 
 * PUT /classifier/myClassifier (body=JSON payload) -> OK
 * GET /classifier -> list of all known classifiers, e.g. "[\"myClassifier\"]"
 * POST /classifier/myClassifier (body=myInstanceData) -> TTL of result node, e.g. "ex:Male"
 * DELETE /classifier/myClassifier -> OK
 * 
 * @author Holger Knublauch
 */
public class ClassifierResource extends Resource implements Deletable, Gettable, Postable, Puttable {
	
	public static String URL_PART = "classifier";
	
	private String classifierKey;
	

	public ClassifierResource(ServletContext context, String classifierKey) {
		super(context);
		this.classifierKey = classifierKey;
	}

	
	@Override
	public Response doDelete(Request request) {
		if(classifierKey == null) {
			return request.badRequest("Missing classifier key");
		}
		else if(WekaClassifiers.get().remove(classifierKey)) {
			return request.okJSON();
		}
		else {
			return request.badRequest("Unknown classifier with key " + classifierKey);
		}
	}

	
	@Override
	public Response doGet(Request request) {
		if(classifierKey == null) {
			JSONResponse response = request.okJSON();
			response.getRoot().add("classifiers", createArrayBuilder(WekaClassifiers.get().getKeys()));
			return response;
		}
		return null;
	}

	
	@Override
	public Response doPost(Request request) {
		if(classifierKey == null) {
			return request.badRequest("Missing classifier key");
		}
		WekaClassifier classifier = WekaClassifiers.get().get(classifierKey);
		if(classifierKey == null) {
			return request.badRequest("Unknown classifier with key " + classifierKey);
		}
		JsonStructure json = request.getBodyJSON();
		if (json == null || !isObject(json)) {
			return request.badRequest("Instance must be sent as JSON object in request body");
		}
		try {
			String nodeString = classifier.classifyInstance(JsonUtil.asObject(json));
			JSONResponse response = request.okJSON();
			if(nodeString != null) {
				response.getRoot().add("node", nodeString);
			}
			return response;
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return request.serverError(ex);
		}
	}
	
	
	@Override
	public Response doPut(Request request) {
		if(classifierKey == null) {
			return request.badRequest("Missing classifier key");
		}
		try {
			JsonStructure json = request.getBodyJSON();
			if (json == null || !isObject(json)) {
				request.badRequest("Classifier definition must be sent as JSON object in request body");
			}
			WekaClassifier classifier = new WekaClassifier(asObject(json));
			WekaClassifiers.get().put(classifierKey, classifier);
			return request.okJSON();
		}
		catch(Exception ex) {
			return request.serverError(ex);
		}
	}

	
	@Override
	public String getURL() {
		return getContextPath() + "/" + URL_PART;
	}
}
