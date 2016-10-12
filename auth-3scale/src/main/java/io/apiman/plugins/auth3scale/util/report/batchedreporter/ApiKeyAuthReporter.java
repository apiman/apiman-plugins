package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ApiKeyAuthReporter extends AbstractReporter<ApiKeyAuthReportToSend> {
	// Just temporary
	Queue<ApiKeyAuthReportToSend> reports = new ConcurrentLinkedQueue<>(); 

	@Override
	public ReportToSend encode() {
		
	}

	@Override
	public void addRecord(ApiKeyAuthReportToSend record) {
		reports.add(record);
	}
}
