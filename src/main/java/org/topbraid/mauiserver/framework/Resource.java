package org.topbraid.mauiserver.framework;

import jakarta.servlet.ServletContext;

public abstract class Resource {
	private final ServletContext context;

	public Resource(ServletContext context) {
		this.context = context;
	}

	public String getContextPath() {
		return context.getContextPath();
	}
	
	public abstract String getURL();

	public interface Gettable {
		Response doGet(Request request);
	}

	public interface Postable {
		Response doPost(Request request);
	}

	public interface Puttable {
		Response doPut(Request request);
	}

	public interface Deletable {
		Response doDelete(Request request);
	}
}
