package org.topbraid.mauiserver.tagger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JobReport {
	private static String fieldCompleted = "completed";
	private static String fieldStartTime = "start_time";
	private static String fieldEndTime = "end_time";
	private static String fieldRuntimeMillis = "runtime_millis";
	private static String fieldDocuments = "documents";
	private static String fieldSkipped = "skipped";
	private static String fieldErrorMessage = "error_message";
	private static String fieldPrecision = "precision";
	private static String fieldRecall = "recall";

	private static final ObjectMapper mapper = new ObjectMapper();

	private final ObjectNode root;
	private Date startTime = null;
	private Date endTime = null;

	public JobReport() {
		this(null);
	}
	
	public JobReport(ObjectNode json) {
		root = mapper.createObjectNode();
		root.put(fieldCompleted, false);
		if (json != null) {
			root.setAll(json);
		}
		if (root.has(fieldStartTime)) {
			startTime = parseDate(root.get(fieldStartTime).textValue());
		}
		if (root.has(fieldEndTime)) {
			endTime = parseDate(root.get(fieldEndTime).textValue());
		}
	}
	
	public String getErrorMessage() {
		if (root.has(fieldErrorMessage)) {
			return root.get(fieldErrorMessage).textValue();
		}
		return null;
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
		if (root.has(fieldDocuments)) {
			return root.get(fieldDocuments).asInt(-1);
		}
		return -1;
	}
	
	public int getSkippedTrainingDocumentCount() {
		if (root.has(fieldSkipped)) {
			return root.get(fieldSkipped).asInt(-1);
		}
		return -1;
	}
	
	public void logStart() {
		startTime = new Date();
	}
	
	public void logEnd() {
		root.put(fieldCompleted, true);
		endTime = new Date();
	}
	
	public void logError(String message) {
		root.put(fieldErrorMessage, message);
		endTime = new Date();
	}
	
	public void logDocumentCounts(int total, int skipped) {
		if (total >= 0) {
			root.put(fieldDocuments, total);
		}
		if (skipped >= 0) {
			root.put(fieldSkipped, skipped);
		}
	}
	
	public void logPrecisionAndRecall(double precision, double recall) {
		root.put(fieldPrecision, precision);
		root.put(fieldRecall, recall);
	}
	
	public ObjectNode toJSON() {
		if (startTime != null) {
			root.put(fieldStartTime, formatDate(startTime));
		}
		if (endTime != null) {
			root.put(fieldEndTime, formatDate(endTime));
		}
		if (getRuntime() >= 0) {
			root.put(fieldRuntimeMillis, getRuntime());
		}
		return root;
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
}
