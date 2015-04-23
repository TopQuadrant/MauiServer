package org.topbraid.mauiserver;

import java.util.Arrays;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.RootServlet;
import org.topbraid.mauiserver.framework.Server;
import org.topbraid.mauiserver.tagger.TaggerCollection;
import org.topbraid.mauiserver.tagger.Tagger;

public class MauiServer implements Server {
	private final static Logger log = LoggerFactory.getLogger(RootServlet.class);

	private final TaggerCollection taggers = new TaggerCollection(System.getProperty("user.dir") + "/data");
	
	public Resource getResource(String requestURI, ServletContext context) {
		String[] path = requestURI.substring(1).split("/", -1);
		log.debug("Path: " + Arrays.asList(path));

		if (path.length == 0 || (path.length == 1 && "".equals(path[0]))) {
			return new HomeResource(context, taggers);
		}
		String taggerId = path[0];
		Tagger tagger = taggers.getTagger(taggerId);
		if (tagger == null) return null;
		if (path.length == 1) {
			return new TaggerResource(context, taggers, tagger);
		}
		if (path.length == 2 && "suggest".equals(path[1])) {
			return new SuggestResource(context, tagger);
		}
		if (path.length == 2 && "config".equals(path[1])) {
			return new ConfigurationResource(context, tagger);
		}
		if (path.length == 2 && "vocab".equals(path[1])) {
			return new VocabularyResource(context, tagger);
		}
		if (path.length == 2 && "train".equals(path[1])) {
			return new TrainingResource(context, tagger);
		}
		if (path.length == 2 && "log".equals(path[1])) {
			return new LogResource(context, tagger);
		}
		return null;
	}
}