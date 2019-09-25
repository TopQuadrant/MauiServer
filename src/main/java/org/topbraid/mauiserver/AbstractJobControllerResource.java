package org.topbraid.mauiserver;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Postable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.AsyncJob;
import org.topbraid.mauiserver.tagger.JobController;
import org.topbraid.mauiserver.tagger.JobReport;

/**
 * A resource in charge of a {@link JobController}.
 * GET returns job status; POST starts job; DELETE cancels job and/or resets job status.
 */
public abstract class AbstractJobControllerResource extends Resource implements Gettable, Postable, Deletable {
	private final String relativeURL;
	private final JobController jobController;
	private final String processName;
	
	public AbstractJobControllerResource(ServletContext context, String relativeURL, JobController jobController, String processName) {
		super(context);
		this.relativeURL = relativeURL;
		this.jobController = jobController;
		this.processName = processName;
	}

	public String getProcessName() {
		return processName;
	}

	public String getProcessNameInitialCap() {
		return processName.substring(0, 1).toUpperCase() + processName.substring(1);
	}

	abstract AsyncJob createJob(Request request, JobReport report);
	
	@Override
	public String getURL() {
		return getContextPath() + relativeURL;
	}
	
	@Override
	public Response doGet(Request request) {
		return createStatusReport(request);
	}

	@Override
	public Response doPost(Request request) {
		try {
			jobController.lock();
		} catch (IllegalStateException ex) {
			return request.conflict(getProcessNameInitialCap() + " already in progress. You may use HTTP DELETE to cancel.");
		}
		try {
			AsyncJob job = createJob(request, jobController.getReport());
			if (job == null) {
				jobController.cancel();
			} else {
				jobController.startJob(job);
			}
			return createStatusReport(request);
		} catch (MauiServerException ex) {
			jobController.cancel();
			return request.badRequest(ex.getMessage());
		} catch (Exception ex) {
			jobController.cancel();
			throw ex;
		}
	}
	
	@Override
	public Response doDelete(Request request) {
		if (jobController.isLocked()) {
			jobController.cancel();
		}
		jobController.reset();
		return createStatusReport(request);
	}
	
	protected JSONResponse createStatusReport(Request request) {
		String status;
		if (jobController.isLocked()) {
			status = "running";
		} else if (jobController.isFailed()) {
			status = "error";
		} else {
			status = "ready";
		}
		JSONResponse response = request.okJSON();
		response.getRoot().add("service_status", status);
		response.getRoot().addAll(jobController.getReport().toJSON());
		return response;
	}
}
