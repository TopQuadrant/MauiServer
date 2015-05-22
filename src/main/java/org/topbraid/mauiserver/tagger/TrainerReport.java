package org.topbraid.mauiserver.tagger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TrainerReport {
	private Date startTime = null;
	private Date endTime = null;
	private int documentCount = -1;
	private int skippedCount = -1;
	private String errorMessage = null;

	public String getErrorMessage() {
		return errorMessage;
	}
	
	public long getRuntime() {
		if (startTime == null) return -1;
		Date end = (endTime == null ? new Date() : endTime);
		return end.getTime() - startTime.getTime();
	}
	
	public Date getStartTime() {
		return startTime;
	}
	
	public Date getEndTime() {
		return endTime;
	}
	
	public int getTrainingDocumentCount() {
		return documentCount;
	}
	
	public int getSkippedTrainingDocumentCount() {
		return skippedCount;
	}
	
	public void logStart() {
		startTime = new Date();
	}
	
	public void logEnd() {
		endTime = new Date();
	}
	
	public void logError(String message) {
		errorMessage = message;
	}
	
	public void logDocumentCounts(int total, int skipped) {
		documentCount = total;
		skippedCount = skipped;
	}
	
	private static String fieldStartTime = "start_time";
	private static String fieldEndTime = "end_time";
	private static String fieldDocuments = "documents";
	private static String fieldSkipped = "skipped";
	private static String fieldErrorMessage = "error_message";

	public void toJSON(ObjectNode root) {
		if (startTime != null) {
			root.put(fieldStartTime, formatDate(startTime));
		}
		if (endTime != null) {
			root.put(fieldEndTime, formatDate(endTime));
		}
		if (documentCount >= 0) {
			root.put(fieldDocuments, documentCount);
		}
		if (skippedCount >= 0) {
			root.put(fieldSkipped, skippedCount);
		}
		if (errorMessage != null) {
			root.put(fieldErrorMessage, errorMessage);
		}
	}

	private void updateFromJSON(JsonNode json) {
		if (json.isObject()) {
			if (json.has(fieldStartTime)) {
				startTime = parseDate(json.get(fieldStartTime).textValue());
			}
			if (json.has(fieldEndTime)) {
				endTime = parseDate(json.get(fieldEndTime).textValue());
			}
			if (json.has(fieldDocuments)) {
				documentCount = json.get(fieldDocuments).asInt(-1);
			}
			if (json.has(fieldSkipped)) {
				skippedCount = json.get(fieldSkipped).asInt(-1);
			}
			if (json.has(fieldErrorMessage)) {
				errorMessage = json.get(fieldErrorMessage).textValue();
			}
		}
	}
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	{
		dateFormat.setTimeZone(TimeZone.getDefault());
	}

	private static String formatDate(Date date) {
	    return dateFormat.format(date);
	}

	private static Date parseDate(String date) {
		try {
			return dateFormat.parse(date);
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static TrainerReport fromJSON(JsonNode json) {
		TrainerReport result = new TrainerReport();
		result.updateFromJSON(json);
		return result;
	}
}
