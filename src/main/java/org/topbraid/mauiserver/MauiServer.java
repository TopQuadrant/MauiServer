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

	private final TaggerCollection taggers = new TaggerCollection(getDataDir());

	/**
	 * Establishes the file system directory where Maui Server keeps its data.
	 */
	public static String getDataDir() {
		// Default: /data subdirectory of current directory
		return getGlobalConfigurationOption(
				"MauiServer.dataDir", 
				"MAUI_SERVER_DATA_DIR", 
				System.getProperty("user.dir") + "/data");
	}

	/**
	 * Establishes the default language for stemming and stopword removal.
	 * Can be overridden on a per-tagger basis.
	 */
	public static String getDefaultLanguage() {
		return getGlobalConfigurationOption(
				"MauiServer.defaultLang", 
				"MAUI_SERVER_DEFAULT_LANG", 
				"en");
	}
	
	public Resource getResource(String requestURI, ServletContext context) {
		if (requestURI.startsWith("/")) {
			requestURI = requestURI.substring(1);
		}
		String[] path = requestURI.split("/", -1);
		log.debug("Path: " + Arrays.asList(path));

		if (path.length == 0 || (path.length == 1 && "".equals(path[0]))) {
			return new HomeResource(context, taggers);
		}
		String taggerId = path[0];
		Tagger tagger = taggers.getTagger(TaggerResource.decodeTaggerIdFromURL(taggerId));
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
		return null;
	}

	private static String getGlobalConfigurationOption(
			String systemProperty, String envVariable, String defaultValue) {
		// Check the Java system property, which can be set via -D on the Java command line
		String value = System.getProperty(systemProperty);
		if (value != null && !"".equals(value)) {
			return value;
		}
		// Check the OS environment variable
		value = System.getenv(envVariable);
		if (value != null && !"".equals(value)) {
			return value;
		}
		return defaultValue;
	}
}