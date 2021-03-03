package org.topbraid.mauiserver.framework;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.JenaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class Response {
	private final static Logger log = LoggerFactory.getLogger(Response.class);

	protected final HttpServletResponse http;
	private int status = HttpServletResponse.SC_OK;
	private String locationURL = null;
	
	public Response(HttpServletResponse response) {
		this.http = response;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}

	public String getSummary() {
		return status + (locationURL == null ? "" : " => " + locationURL);
	}
	
	public void setHeader(String header, String value) {
		http.setHeader(header, value);
	}
	
	public void setRedirectLocation(String url) {
		setHeader("Location", url);
		locationURL = url;
	}
	
	public void send() throws IOException {
		if (status != HttpServletResponse.SC_OK) {
			http.setStatus(status);
		}
	}
	
	public boolean isOk() {
		return status >= 200 && status < 300;
	}
	
	public static class JSONResponse extends Response {
		private final ObjectMapper json;
		private final ObjectNode root;
		
		public JSONResponse(HttpServletResponse response, ObjectMapper json) {
			super(response);
			this.json = json;
			this.root = JsonNodeFactory.instance.objectNode();
		}

		public ObjectNode getRoot() {
			return root;
		}
		
		@Override
		public void send() throws IOException {
			super.send();
			http.setContentType("application/json");
			try {
				json.writeValue(http.getOutputStream(), root);
			} catch (IOException ex) {
				// Probably the client disconnected
				log.warn("Failed to write response, possibly due to client closing the connection: " + ex.getMessage());
			}
		}
		
		@Override
		public String getSummary() {
			String message = null;
			if (root.get("message") != null && root.get("message").asText() != null) {
				message = root.get("message").asText();
			} else if (root.get("title") != null && root.get("title").asText() != null) {
				message = root.get("title").asText();
			}
			return super.getSummary() + ", json" + (message == null ? "": ": \"" + message + "\""); 
		}
		
		@Override
		public void setStatus(int status) {
			super.setStatus(status);
			if (status >= 300) {
				root.put("status", status);
				String text = getStatusText(status);
				if (text != null) {
					root.put("status_text", text);
				}
			}
		}
	}
	
	public static class RDFResponse extends Response {
		private final Model model;
		
		public RDFResponse(HttpServletResponse response, Model model) {
			super(response);
			this.model = model;
		}

		public RDFResponse(HttpServletResponse response) {
			this(response, ModelFactory.createDefaultModel());
		}
		
		public Model getModel() {
			return model;
		}
		
		@Override
		public void send() throws IOException {
			super.send();
			http.setContentType("text/turtle;charset=utf-8");
			try {
				model.write(http.getOutputStream(), "TURTLE");
			} catch (JenaException ex) {
				// Probably the client disconnected
				log.warn("Failed to write response, possibly due to client closing the connection: " + ex.getMessage());
			}
		}

		@Override
		public String getSummary() {
			return super.getSummary() + ", ttl (" + model.size() + "t)"; 
		}
	}
	
	public static String getStatusText(int code) {
		if (code == 100) return "Continue";
		if (code == 101) return "Switching Protocols";
		if (code == 200) return "OK";
		if (code == 201) return "Created";
		if (code == 202) return "Accepted";
		if (code == 203) return "Non-Authoritative Information";
		if (code == 204) return "No Content";
		if (code == 205) return "Reset Content";
		if (code == 206) return "Partial Content";
		if (code == 300) return "Multiple Choices";
		if (code == 301) return "Moved Permanently";
		if (code == 302) return "Found";
		if (code == 303) return "See Other";
		if (code == 304) return "Not Modified";
		if (code == 305) return "Use Proxy";
		if (code == 307) return "Temporary Redirect";
		if (code == 400) return "Bad Request";
		if (code == 401) return "Unauthorized";
		if (code == 402) return "Payment Required";
		if (code == 403) return "Forbidden";
		if (code == 404) return "Not Found";
		if (code == 405) return "Method Not Allowed";
		if (code == 406) return "Not Acceptable";
		if (code == 407) return "Proxy Authentication Required";
		if (code == 408) return "Request Timeout";
		if (code == 409) return "Conflict";
		if (code == 410) return "Gone";
		if (code == 411) return "Length Required";
		if (code == 412) return "Precondition Failed";
		if (code == 413) return "Request Entity Too Large";
		if (code == 414) return "Request-URI Too Long";
		if (code == 415) return "Unsupported Media Type";
		if (code == 416) return "Requested Range Not Satisfiable";
		if (code == 417) return "Expectation Failed";
		if (code == 500) return "Internal Server Error";
		if (code == 501) return "Not Implemented";
		if (code == 502) return "Bad Gateway";
		if (code == 503) return "Service Unavailable";
		if (code == 504) return "Gateway Timeout";
		if (code == 505) return "HTTP Version Not Supported";
		return null;
	}
}
