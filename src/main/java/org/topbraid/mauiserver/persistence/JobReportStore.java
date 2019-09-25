package org.topbraid.mauiserver.persistence;

import java.io.File;

import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.topbraid.mauiserver.tagger.JobReport;

public class JobReportStore extends JSONFileStore<JobReport> {
	
	public JobReportStore(String taggerId, File reportFile) {
		super(taggerId, reportFile);
	}

	@Override
	protected JobReport decode(JsonObject json) {
		return new JobReport(json);
	}

	@Override
	protected JsonStructure encode(JobReport report) {
		return report.toJSON().build();
	}
}
