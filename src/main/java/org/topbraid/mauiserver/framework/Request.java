package org.topbraid.mauiserver.framework;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.topbraid.mauiserver.MauiServerException;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Resource.Puttable;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.framework.Response.RDFResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;

public class Request {
	private final HttpServletRequest request;
	private final HttpServletResponse response;

	// We'll pass this ObjectMapper around and use it wherever
	// JSON needs to be serialized.
	private final static ObjectMapper json = new ObjectMapper();

	public Request(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}
	
	public String getMethod() {
		return request.getMethod().toUpperCase();
	}
	
	public String get(String key) {
		return request.getParameter(key);
	}
	
	/**
	 * @return The request body parsed as JSON, or null if no request body was posted
	 * @throws MauiServerException on JSON parse error 
	 */
	public JsonNode getBodyJSON() throws MauiServerException {
		try {
			// TODO Should support application/x-www-form-urlencoded on PUT. Servlets don't do that automatically. Is there some ready-made code for parsing form-encoded bodies?
			if ("POST".equals(request.getMethod()) && 
					"application/x-www-form-urlencoded".equals(getContentType())) {
				return paramsToJSON(request.getParameterMap());
			}
			InputStream in = getBodyInputStream();
			if (in == null) return null;
			return json.readTree(in);
		} catch (JsonProcessingException ex) {
			throw new MauiServerException("Could not parse request body as JSON: " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new MauiServerException("Could not read request body: " + ex.getMessage(), ex);
		}
	}
	
	public JsonLinesParser getBodyJsonLines(boolean skipJsonSyntaxErrors) {
		try {
			InputStream in = getBodyInputStream();
			if (in == null) return null;
			JsonLinesParser result = new JsonLinesParser(in, json);
			result.setSkipBadJsonLines(skipJsonSyntaxErrors);
			return result;
		} catch (IOException ex) {
			throw new MauiServerException("Could not read request body: " + ex.getMessage(), ex);
		}
	}
	
	/**
	 * @return The request body parsed as RDF, or null if no request body was posted
	 * @throws MauiServerException on RDF parse error 
	 */
	public Model getBodyRDF() throws MauiServerException {
		try {
			InputStream in = getBodyInputStream();
			if (in == null) return null;
			String mediaType = getContentType();
			if (mediaType == null) {
				mediaType = "text/turtle";
			}
			String lang = isRdfXmlMediaType(mediaType) ? "RDF/XML" : "Turtle";
			Model result = ModelFactory.createDefaultModel();
			try {
				result.read(in, null, lang.toUpperCase());
				return result;
			} catch (JenaException ex) {
				if (ex.getCause() != null && ex.getCause() instanceof IOException) {
					// Unpack the IOException so we get a more accurate error message
					throw (IOException) ex.getCause();
				}
				throw new MauiServerException("Could not parse request body as " + lang + ": " + ex.getMessage(), ex);
			}
		} catch (IOException ex) {
			throw new MauiServerException("Could not read request body: " + ex.getMessage(), ex);
		}
	}

	private InputStream getBodyInputStream() throws IOException {
		InputStream in = request.getInputStream();
		// Do a little dance to detect the case where no body has been included at all
		if (!in.markSupported()) in = new BufferedInputStream(in);
		in.mark(1);
		boolean hasContent = in.read() != -1;
		if (!hasContent) {
			return null;
		}
		in.reset();
		return in;
	}
	
	private String getContentType() {
		if (request.getContentType() == null) {
			return null;
		}
		String s = request.getContentType().trim();
		// Handle cases like "application/x-www-url-formencoded ; charset = utf-8"
		if (s.indexOf(';') > -1) {
			s = s.substring(0, s.indexOf(';')).trim();
		}
		return s.toLowerCase();
	}
	
	private boolean isRdfXmlMediaType(String mediaType) {
		if ("application/rdf+xml".equals(mediaType)) return true;
		if ("text/xml".equals(mediaType)) return true;
		if ("application/xml".equals(mediaType)) return true;
		return false;
	}
	
	public JSONResponse respondJSON(int status) {
		JSONResponse result = new JSONResponse(response, json);
		result.setStatus(status);
		return result;
	}

	public JSONResponse okJSON() {
		return respondJSON(HttpServletResponse.SC_OK);
	}
	

	public RDFResponse okTurtle(Model model) {
		RDFResponse result = new RDFResponse(response, model);
		result.setStatus(HttpServletResponse.SC_OK);
		return result;
	}
	
	public Response noContent() {
		return new Response(response) {
			// Override so we prevent sending the body
			@Override
			public void send() throws IOException {
				http.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}
		};
	}
	public Response seeOther(String url, String message) {
		JSONResponse r = respondJSON(HttpServletResponse.SC_SEE_OTHER);
		r.setRedirectLocation(url);
		r.getRoot().put("href", url);
		r.getRoot().put("message", message);
		return r;
	}
	
	public Response badRequest(String message) { 
		return badRequest(null, null, message);
	}

	public Response badRequest(String field, String message) { 
		return badRequest(field, null, message);
	}

	public Response badRequest(String field, String value, String message) {
		JSONResponse r = respondJSON(HttpServletResponse.SC_BAD_REQUEST);
		if (field != null) {
			r.getRoot().put("field", field);
		}
		if (value != null) {
			r.getRoot().put("value", value);
		}
		r.getRoot().put("message", message);
		return r;
	}

	public Response notFound() {
		JSONResponse r = respondJSON(HttpServletResponse.SC_NOT_FOUND);
		r.getRoot().put("message", "The resource does not exist");
		return r;
	}
	
	public Response methodNotAllowed(String unsupportedMethod, Resource resource) {
		List<String> allowedMethods = new ArrayList<String>();
		allowedMethods.add("GET");
		allowedMethods.add("HEAD");
		if (resource instanceof Postable) {
			allowedMethods.add("POST");
		}
		if (resource instanceof Puttable) {
			allowedMethods.add("PUT");
		}
		if (resource instanceof Deletable) {
			allowedMethods.add("DELETE");
		}
		JSONResponse r = respondJSON(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		r.setHeader("Allow", StringUtils.join(allowedMethods, ", "));
		r.getRoot().put("message", "HTTP method " + unsupportedMethod + 
				" not supported on this resource");
		ArrayNode list = r.getRoot().arrayNode();
		for (String method: allowedMethods) {
			list.add(method);
		}
		r.getRoot().set("allowed_methods", list);
		return r;
	}
	
	public Response serverError(Exception ex) {
		StringWriter stack = new StringWriter();
		PrintWriter pw = new PrintWriter(stack);
		ex.printStackTrace(pw);
		pw.flush();
		JSONResponse r = respondJSON(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		r.getRoot().put("message", 
				ex.getMessage() == null ? "General server error" : ex.getMessage());
		r.getRoot().put("stacktrace", stack.toString());
		return r;
	}
	
	public Response conflict(String error) {
		JSONResponse r = respondJSON(HttpServletResponse.SC_CONFLICT);
		r.getRoot().put("message", error);
		return r;
	}
	
	/**
	 * Converts a parameter map (such as returned by {@link HttpServletRequest#getParameterMap()})
	 * to a JSON object. Parameters with multiple values are converted to a JSON array. 
	 */
	private ObjectNode paramsToJSON(Map<String,String[]> params) {
		System.out.println(params);
		ObjectNode result = json.createObjectNode();
		for (String key: params.keySet()) {
			if (params.get(key) == null) {
				result.putNull(key);
			} else if (params.get(key).length == 1) {
				String value = params.get(key)[0];
				if ("".equals(value)) {
					result.putNull(key);
				} else {
					result.put(key, value);
				}
			} else {
				ArrayNode values = result.arrayNode();
				for (String value: params.get(key)) {
					values.add(value);
				}
				result.set(key, values);
			}
		}
		return result;
	}
}
