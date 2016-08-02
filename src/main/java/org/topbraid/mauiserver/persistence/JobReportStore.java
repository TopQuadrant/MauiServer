package org.topbraid.mauiserver.persistence;

import java.io.File;

import org.topbraid.mauiserver.tagger.JobReport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JobReportStore extends JSONFileStore<JobReport> {
	
	public JobReportStore(String taggerId, File reportFile) {
		super(taggerId, reportFile);
	}

	@Override
	protected JobReport decode(ObjectNode json) {
		return new JobReport(json);
	}

	@Override
	protected JsonNode encode(JobReport report) {
		return report.toJSON();
	}
}
