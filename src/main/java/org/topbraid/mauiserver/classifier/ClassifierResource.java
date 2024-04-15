package org.topbraid.mauiserver.classifier;

import java.util.Iterator;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Resource.Puttable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import jakarta.servlet.ServletContext;

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
			ObjectNode root = response.getRoot();
			ArrayNode array = root.arrayNode();
			Iterator<String> it = WekaClassifiers.get().iterator();
			while(it.hasNext()) {
				array.add(it.next());
			}
			root.set("classifiers", array);
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
		JsonNode json = request.getBodyJSON();
		try {
			String nodeString = classifier.classifyInstance(json);
			JSONResponse response = request.okJSON();
			if(nodeString != null) {
				ObjectNode root = response.getRoot();
				root.set("node", new TextNode(nodeString));
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
			JsonNode json = request.getBodyJSON();
			WekaClassifier classifier = new WekaClassifier(json);
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
