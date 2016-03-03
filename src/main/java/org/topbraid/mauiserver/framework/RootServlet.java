package org.topbraid.mauiserver.framework;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServer;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Resource.Puttable;

@SuppressWarnings("serial")
public class RootServlet extends HttpServlet {
	Logger log = LoggerFactory.getLogger(RootServlet.class);
	
	/**
	 * Creates the server instance. This instance will be shared between
	 * servlets through the {@link ServletContext}. If this was a true
	 * framework, the implementation wouldn't be hardcoded here but
	 * specified in a configuration file.
	 */
	public Server createServer() {
		return new MauiServer();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doRequest(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doRequest(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doRequest(req, resp);
	}

	private void doRequest(HttpServletRequest req, HttpServletResponse resp) 
			throws IOException {
		Request request = createRequest(req, resp);
		Response response = null;
		String requestURI = getLocalRequestUriWithoutQuery(req);
		log.debug(req.getMethod() + " " + requestURI);
		try {
			Resource resource = getServer(req.getServletContext()).getResource(requestURI, req.getServletContext());
			response = createResponse(request, resource);
		} catch (Exception ex) {
			log.error("Uncaught exception in servlet", ex);
			response = request.serverError(ex);
		}
		response.send();
		log.info(req.getMethod() + " " + requestURI + 
				(response == null ? "" : " " + response.getSummary()));
	}

	/**
	 * Returns a processed version of the request URI. The scheme, host
	 * name and port are omitted. The query string, such as 
	 * <code>?param1=value&param2=value</code>, is omitted. If the webapp
	 * doesn't run as the servlet container's root application, then the
	 * path to the application is omitted. The resulting string always
	 * starts with a '/'. For example, if the webapp is deployed as
	 * <code>webapp-name</code>, then the URI
	 * <code>http://myserver:8080/webapp-name/foo/bar?x=123</code> would be
	 * turned into <code>/foo/bar</code>.
	 */
	private String getLocalRequestUriWithoutQuery(HttpServletRequest req) {
		String result = req.getRequestURI();
		if (req.getContextPath() != null) {
			if (req.getRequestURI().startsWith(req.getContextPath())) {
				return result.substring(req.getContextPath().length());
			}
			log.warn("Mismatch between context path '" + 
					req.getContextPath() + "' and request URI '" + 
					req.getRequestURI() + "'");
		}		
		return result;
	}
	
	private Response createResponse(Request request, Resource resource) {
		if (resource == null) {
			return request.notFound();
		}
		if (("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) 
				&& resource instanceof Gettable) {
			return ((Gettable) resource).doGet(request);
		}
		if ("PUT".equals(request.getMethod()) && resource instanceof Puttable) {
			return ((Puttable) resource).doPut(request);
		}
		if ("POST".equals(request.getMethod()) && resource instanceof Postable) {
			return ((Postable) resource).doPost(request);
		}
		if ("DELETE".equals(request.getMethod()) && resource instanceof Deletable) {
			return ((Deletable) resource).doDelete(request);
		}
		return request.methodNotAllowed(request.getMethod(), resource);
	}

	private Request createRequest(HttpServletRequest req, HttpServletResponse resp) {
		return new Request(req, resp);
	}

	private final static String serverInstanceAttribute = "framework.server-instance";
	private Server getServer(ServletContext context) {
		if (context.getAttribute(serverInstanceAttribute) == null) {
			context.setAttribute(serverInstanceAttribute, createServer());
		}
		return (Server) context.getAttribute(serverInstanceAttribute);
	}
}
