package org.topbraid.mauiserver.tagger;

public interface AsyncJob {

	String getActivityName();
	
	void run(JobReport report) throws Exception;
}