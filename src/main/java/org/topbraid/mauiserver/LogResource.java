package org.topbraid.mauiserver;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.tagger.Tagger;

public class LogResource extends Resource implements Gettable, Deletable {
	private final Tagger tagger;
	
	public LogResource(ServletContext context, Tagger tagger) {
		super(context);
		this.tagger = tagger;
	}
	
	@Override
	public String getURL() {
		return getContextPath() + getRelativeLogURL(tagger);
	}
	
	@Override
	public Response doGet(Request request) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Response doDelete(Request request) {
		// TODO Auto-generated method stub
		return null;
	}

	public static String getRelativeLogURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/log";
	}
}
