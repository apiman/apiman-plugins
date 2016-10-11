package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.apiman.gateway.engine.components.IPeriodicComponent;

public class BatchedReporter {
	// Change list to RingBuffer?
	private HashMap<ReportData.ReportGroupType, List<ReportData>> reports = new LinkedHashMap<>(); // TODO concurrency implications?
	private IPeriodicComponent periodic;
	private boolean started = false;
	
	private static final int REPORTING_PERIOD = 5000;
	private static final int INITIAL_WAIT = 5000;
	
	public BatchedReporter() {
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void start(IPeriodicComponent periodic) {
		if (started)
			throw new IllegalStateException("Already started");
		
		this.periodic = periodic;
		this.started = true;
		
		periodic.setPeriodicTimer(REPORTING_PERIOD, INITIAL_WAIT, timerId -> {
			for (List<ReportData> reports : reports.values()) {
				
			}
		});
	}
	
	// TODO Depending on which platform this is run on, could this become thread unsafe?
	public void report(ReportData report) {
		reports.get(report.getBatchedReportGroup()).add(report);
	}
}
