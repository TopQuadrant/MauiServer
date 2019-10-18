package org.topbraid.mauiserver.persistence;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServerException;
import org.topbraid.mauiserver.tagger.JobReport;
import org.topbraid.mauiserver.tagger.TaggerConfiguration;

import com.entopix.maui.filters.MauiFilter;

public class TaggerStore {
	private static final Logger log = LoggerFactory.getLogger(TaggerStore.class);
	
	private final File path;

	public TaggerStore(String path) {
		log.info("Data directory: " + path);
		this.path = new File(path.endsWith("/") ? path : path + "/");
		if (!this.path.exists()) {
			throw new MauiServerException(
					"Data directory does not exist: '" + path + "'");
		}
		if (!this.path.isDirectory()) {
			throw new MauiServerException(
					"Data directory path is not a directory: '" + path + "'");
		}
		if (!this.path.canWrite()) {
			throw new MauiServerException(
					"Data directory is not writeable: '" + path + "'");
		}
	}
	
	public Collection<String> listTaggers() {
		List<String> results = new ArrayList<String>();
		for (File f: path.listFiles()) {
			if (!f.isDirectory()) continue;
			String taggerId = decodeTaggerIdFromFilename(f.getName());
			if (encodeTaggerIdAsFilename(taggerId).equals(f.getName())) {
				results.add(taggerId);
			} else {
				log.warn("Skipping malformed tagger directory " + f.getName());
			}
		}
		log.debug("Listed " + results.size() + " taggers");
		return results;
	}
	
	public boolean taggerExists(String id) {
		File f = getTaggerDirectory(id);
		if (!f.exists()) return false;
		if (!f.isDirectory()) return false;
		return true;
	}
	
	public void createTagger(String id) {
		log.info("Creating tagger: " + id);
		if (taggerExists(id)) {
			throw new MauiServerException(
					"Attempted to create tagger with id that is already in use: '" + id + "'");
		}
		File f = getTaggerDirectory(id);
		if (!f.mkdir()) {
			throw new MauiServerException(
					"Failed to create directory: '" + f.getAbsolutePath() + "'");
		};
		getConfigurationStore(id).put(TaggerConfiguration.createWithDefaults(id));
	}
	
	public void deleteTagger(String id) {
		if (!taggerExists(id)) return;
		File f = getTaggerDirectory(id);
		try {
			FileUtils.deleteDirectory(f);
		} catch (IOException ex) {
			throw new MauiServerException("Failed to delete tagger directory " + 
					f + ": " + ex.getMessage(), ex);
		}
	}

	private File getTaggerDirectory(String id) {
		return new File(path.getAbsolutePath() + "/" + encodeTaggerIdAsFilename(id));
	}
	
	private File getTaggerFile(String id, String filename) {
		return new File(path.getAbsolutePath() + "/" + encodeTaggerIdAsFilename(id) + "/" + filename);
	}
	
	public ObjectStore<TaggerConfiguration> getConfigurationStore(String taggerId) {
		return new ConfigurationStore(taggerId, getTaggerFile(taggerId, "config.json"));
	}

	public ObjectStore<Model> getVocabularyStore(String taggerId) {
		return new VocabularyStore(taggerId, getVocabularyFile(taggerId));
	}
	
	public File getVocabularyFile(String taggerId) {
		return getTaggerFile(taggerId, "vocabulary.ttl");
	}
	
	public ObjectStore<MauiFilter> getMauiModelStore(String taggerId) {
		return new MauiModelStore(taggerId, getTaggerFile(taggerId, "model.maui"));
	}
	
	public ObjectStore<JobReport> getTrainerReportStore(String taggerId) {
		return new JobReportStore(taggerId, getTaggerFile(taggerId, "trainer-report.json"));
	}
	
	public ObjectStore<JobReport> getCrossValidatorReportStore(String taggerId) {
		return new JobReportStore(taggerId, getTaggerFile(taggerId, "xvalidator-report.json"));
	}

	/**
	 * Encodes special characters in a string to make it safe for use
	 * as a filename. This is approximate; it encodes many characters
	 * that are in fact safe. Characters are %-encoded.
	 * 
	 * @param s An arbitrary string
	 * @return A version of the input that is safe for use as a filename
	 */
	private String encodeTaggerIdAsFilename(String s) {
		try {
			s = URLEncoder.encode(s, "utf-8");
			if (s.startsWith(".")) {
				// Catch cases like "." and ".."
				s = "%2E" + s.substring(1);
			}
			if (s.startsWith("~")) {
				// Catch cases like "~" and "~root"
				s = "%7E" + s.substring(1);
			}
			return s;
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("Can't happen", ex);
		}
	}
	
	private String decodeTaggerIdFromFilename(String s) {
		try {
			return URLDecoder.decode(s, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("Can't happen", ex);
		}
	}
}
