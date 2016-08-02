package org.topbraid.mauiserver.tagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.persistence.ObjectStore;

/**
 * Allows asynchronous execution of jobs. Ensures that only a single job
 * is being executed at a time. Jobs can be cancelled. Details about
 * the ongoing job execution, or the most recent completed job, can
 * be obtained from the job report. The controller is in charge of
 * logging start and end of jobs, as well as exceptions, in the
 * job report. Information about completed jobs persists beyond server
 * restarts.
 */
public class JobController {

	private static final Logger log = LoggerFactory.getLogger(JobController.class);
	private boolean locked = false;
	private Thread jobThread = null;
	private JobReport report; 
	private JobReport previousReport;
	private final ObjectStore<JobReport> reportStore;

	public JobController(ObjectStore<JobReport> reportStore) {
		this.reportStore = reportStore;
		this.report = reportStore.get();
		if (this.report == null) {
			this.report = createDefaultJobReport();
		}
	}

	public JobReport getReport() {
		return report;
	}
	
	public synchronized void lock() {
		if (locked) throw new IllegalStateException();
		locked = true;
		previousReport = report;
		report = createDefaultJobReport();
		report.logStart();
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public boolean isFailed() {
		return report.getErrorMessage() != null;
	}

	public void startJob(final AsyncJob job) {
		if (!locked) throw new IllegalStateException("Must lock() before starting " + job.getActivityName() + " job");
		jobThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					job.run(report);
					if (!Thread.currentThread().isInterrupted()) {
						reportStore.put(report);
					}
				} catch (Throwable ex) {
					String errorMessage = "Error while " + job.getActivityName() + ": " + ex.getMessage();
					report.logError(errorMessage);
					reportStore.put(report);
					log.error(errorMessage, ex);
				} finally {
					unlock();
				}
			}
		});
		jobThread.start();
	}
	
	private synchronized void unlock() {
		if (!locked) return;
		report.logEnd();
		locked = false;
		jobThread = null;
	}
	
	public synchronized void cancel() {
		if (!locked) return;
		// TODO: Find a way of actually stopping the MauiModelBuilder
		if (jobThread != null) {
			jobThread.interrupt();
		}
		unlock();
		report = previousReport;
	}
	
	public synchronized void reset() {
		if (locked) throw new IllegalStateException();
		report = createDefaultJobReport();
		reportStore.put(report);
	}
	
	protected JobReport createDefaultJobReport() {
		return new JobReport();
	}
}
