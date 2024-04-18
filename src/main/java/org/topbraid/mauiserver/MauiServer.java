package org.topbraid.mauiserver;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.classifier.ClassifierResource;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Server;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TaggerCollection;

import jakarta.servlet.ServletContext;

public class MauiServer implements Server {
	public final static Logger log = LoggerFactory.getLogger(MauiServer.class);

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

	/**
	 * Returns the application version, as defined in pom.xml
	 */
	public static String getVersion() {
		return version;
	}

	private static String version;
	static {
	    Properties p = new Properties();
	    try (InputStream in = MauiServer.class.getResourceAsStream("/application.properties")) {
	    	p.load(in);
	    	version = p.getProperty("application.version", "0.0.0");
	    } catch (Exception ex) {
	    	log.error("Failed to load application.properties", ex);
	    }
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

		if (path.length > 0 && ClassifierResource.URL_PART.equals(path[0])) {
			return new ClassifierResource(context, path.length > 1 ? path[1] : null);
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
		if (path.length == 2 && "xvalidate".equals(path[1])) {
			return new CrossValidationResource(context, tagger);
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