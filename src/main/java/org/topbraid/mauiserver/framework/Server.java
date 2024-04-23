package org.topbraid.mauiserver.framework;

import jakarta.servlet.ServletContext;

public interface Server {

	public Resource getResource(String requestURI, ServletContext context);
}
