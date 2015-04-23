package org.topbraid.mauiserver.framework;

import javax.servlet.ServletContext;

public interface Server {

	public Resource getResource(String requestURI, ServletContext context);
}
