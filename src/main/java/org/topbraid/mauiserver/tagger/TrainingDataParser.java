package org.topbraid.mauiserver.tagger;

import static org.topbraid.mauiserver.JsonUtil.getString;
import static org.topbraid.mauiserver.JsonUtil.hasValue;
import static org.topbraid.mauiserver.JsonUtil.isObject;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.JsonUtil;
import org.topbraid.mauiserver.framework.JsonLinesParser;

/**
 * Parses training documents in JSON Lines (.jsonl) format. Each line is a
 * JSON object representing one document.
 * Recognized keys on the JSON object are <code>"content"</code>
 * (string; the text content of the training document), <code>"topics"</code>
 * (array of strings; the topics of the document), and optionally <code>"id"</code>
 * (a unique identifier for the document, such as a URL or filename). 
 */
public class TrainingDataParser {
	private final static Logger log = LoggerFactory.getLogger(TrainingDataParser.class);
	
	private final JsonLinesParser in;
	private int skippedDocumentCount = 0;
	
	public TrainingDataParser(JsonLinesParser in) {
		this.in = in;
	}

	public int getSkippedDocumentCount() {
		return skippedDocumentCount + in.getSkippedBadLinesCount();
	}
	
	public List<TrainingDocument> getCorpus() {
		List<TrainingDocument> results = new ArrayList<TrainingDocument>();
		while (in.hasNext()) {
			JsonValue json = in.next();
			int line = in.getLineNumber();
			TrainingDocument doc = toTrainingDocument(json, line);
			if (doc == null) continue;
			results.add(doc);
		}
		return results;
	}

	private TrainingDocument toTrainingDocument(JsonValue json, int line) {
		String id = "doc-" + line;
		if (!isObject(json)) {
			logSkipDocument("doc-" + line, "Not a JSON object");
			return null;
		}
		JsonObject doc = json.asJsonObject();
		if (hasValue(doc, "id", ValueType.STRING, true)) {
			id = doc.getString("id");
		}
		if (!hasValue(doc, "content", ValueType.STRING, true)) {
			logSkipDocument(id, "Field 'content' missing, empty, or not a string");
			return null;
		}
		String textContent = getString(doc, "content");
		if (!hasValue(doc, "topics", ValueType.ARRAY, true)) {
			logSkipDocument(id, "Field 'topics' missing, empty, or not an array");
			return null;
		}
		List<String> topics = new ArrayList<String>();
		int skippedTopics = 0;
		for (JsonValue topic: doc.getJsonArray("topics")) {
			String s = JsonUtil.asString(topic);
			if (s == null || "".equals(s)) {
				skippedTopics++;
				continue;
			}
			topics.add(s);
		}
		if (topics.isEmpty()) {
			logSkipDocument(id, "All " + skippedTopics + " topics were invalid (non-string)");
			return null;
		}
		int wordCount = textContent.split("\\s+").length;
		logAddDocument(id, wordCount, topics.size(), skippedTopics);
		return new TrainingDocument(id, textContent, topics);
	}

	private void logSkipDocument(String id, String message) {
		log.warn("Skipping document " + id + ": " + message);
		skippedDocumentCount++;
	}
	
	private void logAddDocument(String id, int words, int validTopics, int invalidTopics) {
		log.debug("Adding training document " + id + ": " + words + " words, " + 
				validTopics + " topics" + 
				((invalidTopics > 0) ? ", " + invalidTopics + " non-string topics skipped" : ""));
	}
}
