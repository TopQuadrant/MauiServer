package org.topbraid.mauiserver.framework;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.topbraid.mauiserver.MauiServer;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Resource.Puttable;

@SuppressWarnings("serial")
public class RootServlet extends HttpServlet {

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
		Response response;
		try {
			String requestURI = (req.getServletPath() == null || "".equals(req.getServletPath()))
					? "/" : req.getServletPath();
			Resource resource = getServer(req.getServletContext()).getResource(requestURI, req.getServletContext());
			response = createResponse(request, resource);
		} catch (Exception ex) {
			response = request.serverError(ex);
		}
		response.send();
	}

	private Response createResponse(Request request, Resource resource) {
		if (resource == null) {
			return request.notFound();
		}
		if ("GET".equals(request.getMethod()) && resource instanceof Gettable) {
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
