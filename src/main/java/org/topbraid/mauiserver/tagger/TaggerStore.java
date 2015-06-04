package org.topbraid.mauiserver.tagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServerException;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.util.DataLoader;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;

public class TaggerStore {
	private static final Logger log = LoggerFactory.getLogger(TaggerStore.class);
	
	private static final String configFileName = "config.json";
	private static final String vocabularyFileName = "vocabulary.ttl";
	private static final String mauiModelFileName = "model.maui";
	private static final String trainerReportFileName = "trainer-report.json";
	private static final ObjectMapper mapper = new ObjectMapper();
	
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
		log.debug("Listed " + results.size() + " taggers: [" + StringUtils.join(results, ", ") + "]");
		return results;
	}
	
	public boolean taggerExists(String id) {
		File f = getTaggerDirectory(id);
		log.debug("Checking for tagger " + id + "; directory: " + f);
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
		writeConfiguration(id, TaggerConfiguration.createWithDefaults(id));
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

	private boolean fileExists(String taggerId, String filename) {
		return getTaggerFile(taggerId, filename).exists();
	}
	
	private InputStream openFile(String taggerId, String filename) throws FileNotFoundException {
		return new FileInputStream(getTaggerFile(taggerId, filename));
	}
	
	public TaggerConfiguration readConfiguration(String taggerId) {
		try {
			return TaggerConfiguration.fromJSON(mapper.readTree(openFile(taggerId, configFileName)));
		} catch (FileNotFoundException ex) {
			log.warn(configFileName + " for tagger " + taggerId + " not found. Using defaults.");
			TaggerConfiguration result = TaggerConfiguration.createWithDefaults(taggerId);
			writeConfiguration(taggerId, result);
			return result;
		} catch (JsonProcessingException ex) {
			throw new MauiServerException(
					"JSON processing error on " + configFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new MauiServerException(
					"Error reading from " + configFileName + " for tagger " + taggerId +": " + ex.getMessage(), ex);
		}
	}

	public void writeConfiguration(String taggerId, TaggerConfiguration config) {
		try {
			mapper.writeValue(getTaggerFile(taggerId, configFileName),
					config.toJSON(mapper.getNodeFactory()));
		} catch (JsonMappingException ex) {
			throw new MauiServerException(
					"JSON processing error on " + configFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		} catch (JsonGenerationException ex) {
			throw new MauiServerException(
					"JSON processing error on " + configFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new MauiServerException(
					"Error writing to " + configFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		}
	}

	public boolean hasVocabulary(String taggerId) {
		return fileExists(taggerId, vocabularyFileName);
	}
	
	/**
	 * Returns the vocabulary for this tagger as a Jena {@link Model}.
	 * @param taggerId The ID of the tagger 
	 * @return The vocabulary model, or <code>null</code> if none was uploaded/provided for the tagger
	 */
	public Model readVocabulary(String taggerId) {
		if (!hasVocabulary(taggerId)) return null;
		try {
			return FileManager.get().loadModel(getVocabularyFileName(taggerId));
		} catch (JenaException ex) {
			throw new MauiServerException(
					"Error processing " + vocabularyFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		}
	}

	public String getVocabularyFileName(String taggerId) {
		return getTaggerFile(taggerId, vocabularyFileName).getAbsolutePath();		
	}
	
	/**
	 * Writes the vocabulary for this tagger from the given Jena {@link Model}.
	 * If <code>null</code> is passed as the model, the vocabulary will be deleted
	 * and the tagger will have no associated vocabulary.
	 * 
	 * @param taggerId The ID of the tagger 
	 * @return The vocabulary model, or <code>null</code> if none was uploaded/provided for the tagger
	 */
	public void writeVocabulary(String taggerId, Model vocabulary) {
		if (vocabulary == null) {
			getTaggerFile(taggerId, vocabularyFileName).delete();
			return;
		}
		try {
			vocabulary.write(new FileOutputStream(getTaggerFile(taggerId, vocabularyFileName)), "TURTLE");
		} catch (FileNotFoundException ex) {
			throw new MauiServerException(
					"Error writing to " + vocabularyFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		} catch (JenaException ex) {
			throw new MauiServerException(
					"Error writing to " + vocabularyFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		}
	}

	public boolean hasMauiModel(String taggerId) {
		return fileExists(taggerId, mauiModelFileName);
	}
	
	public File getMauiModelFile(String taggerId) {
		return getTaggerFile(taggerId, mauiModelFileName);
	}
	
	public MauiFilter readMauiModel(String taggerId) {
		if (!hasMauiModel(taggerId)) return null;
		try {
			return DataLoader.loadModel(getMauiModelFile(taggerId).getAbsolutePath());
		} catch (Exception ex) {
			if (ex.getCause() == null && ex.getMessage() == null) {
				throw new MauiServerException("Error reading Maui model " + mauiModelFileName + " for tagger " + taggerId);
			}
			throw new MauiServerException("Error reading Maui model " + mauiModelFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		}
	}
	
	public void writeMauiModel(String taggerId, MauiFilter mauiModel) {
		if (mauiModel == null) {
			if (hasMauiModel(taggerId)) {
				log.info("Deleting Maui model: " + getMauiModelFile(taggerId).getAbsolutePath());
				getMauiModelFile(taggerId).delete();
			}
			return;
		}
		// TODO: This should write the new model to a temporary location and replace the old one only on success
		log.info("Writing Maui model: " + getMauiModelFile(taggerId).getAbsolutePath());
		MauiModelBuilder modelBuilder = new MauiModelBuilder();
		modelBuilder.modelName = getMauiModelFile(taggerId).getAbsolutePath();
		try {
			modelBuilder.saveModel(mauiModel);
		} catch (Exception ex) {
			throw new MauiServerException("Error while saving model after training: " + ex.getMessage(), ex);
		}
	}
	
	public File getTrainerReportFile(String taggerId) {
		return getTaggerFile(taggerId, trainerReportFileName);
	}
	
	public TrainerReport readTrainerReport(String taggerId) {
		try {
			return TrainerReport.fromJSON(mapper.readTree(openFile(taggerId, trainerReportFileName)));
		} catch (FileNotFoundException ex) {
			return new TrainerReport();
		} catch (JsonProcessingException ex) {
			throw new MauiServerException(
					"JSON processing error on " + trainerReportFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new MauiServerException(
					"Error reading from " + trainerReportFileName + " for tagger " + taggerId +": " + ex.getMessage(), ex);
		}
	}

	public void writeTrainerReport(String taggerId, TrainerReport report) {
		File f = getTrainerReportFile(taggerId); 
		if (report == null) {
			report = new TrainerReport();
		}
		ObjectNode root = mapper.createObjectNode();
		report.toJSON(root);
		try {
			mapper.writeValue(f, root);
		} catch (IOException ex) {
			throw new MauiServerException(
					"Error writing to " + trainerReportFileName + " for tagger " + taggerId + ": " + ex.getMessage(), ex);
		}
	}
	
	private File getTaggerDirectory(String id) {
		return new File(path.getAbsolutePath() + "/" + encodeTaggerIdAsFilename(id));
	}
	
	private File getTaggerFile(String id, String filename) {
		return new File(path.getAbsolutePath() + "/" + encodeTaggerIdAsFilename(id) + "/" + filename);
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
