package org.topbraid.mauiserver.framework;

import static javax.json.JsonValue.ValueType.STRING;
import static org.topbraid.mauiserver.JsonUtil.getString;
import static org.topbraid.mauiserver.JsonUtil.hasValue;

import java.io.IOException;
import java.util.Collections;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.JsonUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;

public abstract class Response {
	private final static Logger log = LoggerFactory.getLogger(Response.class);
	private final static JsonWriterFactory writerFactory = 
//			Json.createWriterFactory(Collections.emptyMap());
			Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
	
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
		private final JsonObjectBuilder root;
		
		public JSONResponse(HttpServletResponse response) {
			super(response);
			this.root = JsonUtil.createObjectBuilderThatIgnoresNulls();
		}

		public JsonObjectBuilder getRoot() {
			return root;
		}
		
		@Override
		public void send() throws IOException {
			super.send();
			http.setContentType("application/json");
			try {
				writerFactory.createWriter(http.getOutputStream()).write(root.build());
			} catch (IOException ex) {
				// Probably the client disconnected
				log.warn("Failed to write response, possibly due to client closing the connection: " + ex.getMessage());
			}
		}
		
		@Override
		public String getSummary() {
			JsonObject o = root.build();
			String message = null;
			if (hasValue(o, "message", STRING, true)) {
				message = getString(o, "message");
			} else if (hasValue(o, "title", STRING, true)) {
				message = getString(o, "title");
			}
			return super.getSummary() + ", json" + (message == null ? "": ": \"" + message + "\""); 
		}
		
		@Override
		public void setStatus(int status) {
			super.setStatus(status);
			if (status >= 300) {
				root.add("status", status);
				String text = getStatusText(status);
				if (text != null) {
					root.add("status_text", text);
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
