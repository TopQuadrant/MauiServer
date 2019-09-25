package org.topbraid.mauiserver.tagger;

import static javax.json.JsonValue.ValueType.NUMBER;
import static javax.json.JsonValue.ValueType.STRING;
import static org.topbraid.mauiserver.JsonUtil.createObjectBuilderThatIgnoresNulls;
import static org.topbraid.mauiserver.JsonUtil.getInt;
import static org.topbraid.mauiserver.JsonUtil.getString;
import static org.topbraid.mauiserver.JsonUtil.hasValue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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

	private final JsonObjectBuilder root;
	private Date startTime = null;
	private Date endTime = null;

	public JobReport() {
		this(null);
	}
	
	public JobReport(JsonObject json) {
		root = createObjectBuilderThatIgnoresNulls()
				.add(fieldCompleted, false);
		if (json != null) {
			root.addAll(Json.createObjectBuilder(json));
		}
		if (hasValue(json, fieldStartTime, STRING)) {
			startTime = parseDate(getString(json, fieldStartTime));
		}
		if (hasValue(json, fieldEndTime, STRING)) {
			endTime = parseDate(getString(json, fieldEndTime));
		}
	}
	
	public String getErrorMessage() {
		JsonObject o = root.build();
		if (hasValue(o, fieldErrorMessage, STRING)) {
			return getString(o, fieldErrorMessage);
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
		JsonObject o = root.build();
		if (hasValue(o, fieldDocuments, NUMBER)) {
			return getInt(o, fieldDocuments, -1);
		}
		return -1;
	}
	
	public int getSkippedTrainingDocumentCount() {
		JsonObject o = root.build();
		if (hasValue(o, fieldSkipped, NUMBER)) {
			return getInt(o, fieldSkipped, -1);
		}
		return -1;
	}
	
	public void logStart() {
		startTime = new Date();
	}
	
	public void logEnd() {
		root.add(fieldCompleted, true);
		endTime = new Date();
	}
	
	public void logError(String message) {
		root.add(fieldErrorMessage, message);
		endTime = new Date();
	}
	
	public void logDocumentCounts(int total, int skipped) {
		if (total >= 0) {
			root.add(fieldDocuments, total);
		}
		if (skipped >= 0) {
			root.add(fieldSkipped, skipped);
		}
	}
	
	public void logPrecisionAndRecall(double precision, double recall) {
		root.add(fieldPrecision, precision);
		root.add(fieldRecall, recall);
	}
	
	public JsonObjectBuilder toJSON() {
		if (startTime != null) {
			root.add(fieldStartTime, formatDate(startTime));
		}
		if (endTime != null) {
			root.add(fieldEndTime, formatDate(endTime));
		}
		if (getRuntime() >= 0) {
			root.add(fieldRuntimeMillis, getRuntime());
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
