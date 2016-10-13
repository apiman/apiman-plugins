package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.util.ParameterMap;

public class ApiKeyAuthReporter extends AbstractReporter<ApiKeyReportData> {
	// Just temporary
	Map<Integer, ConcurrentLinkedQueue<ApiKeyReportData>> reports = new ConcurrentHashMap<>();
	
	private static final int DEFAULT_LIST_CAPAC = 800;
	private static final int FULL_TRIGGER_CAPAC = 500;
	private static final int MAX_RECORDS = 1000; //
	private volatile boolean encoding = false;
	
	@Override // TODO need locking?
	public List<ReportToSend> encode() {
		List<ReportToSend> encodedReports = new ArrayList<>(reports.size());
		for (ConcurrentLinkedQueue<ApiKeyReportData> queue : reports.values()) {
			if (queue.isEmpty())
				continue;
			
			ApiKeyReportData reportData = queue.poll();
			// Base report
			ParameterMap data = new ParameterMap();
	    	data.add(AuthRepConstants.PROVIDER_KEY, reportData.getProviderKey()); 
	    	data.add(AuthRepConstants.SERVICE_ID, reportData.getServiceId());
	    	
	    	// Transactions
	    	List<ParameterMap> transactions = new ArrayList<>(); //TODO approximate - size is O(n) on linkedqueue
			int i = 0;
			do {
				ParameterMap transaction = new ParameterMap();
				transactions.add(transaction);
				
				transaction.add(AuthRepConstants.USER_KEY, reportData.getUserKey());
		    	setIfNotNull(transaction, AuthRepConstants.REFERRER, reportData.getReferrer());
		    	setIfNotNull(transaction, AuthRepConstants.USER_ID, reportData.getUserId());
		    	transaction.add("usage", reportData.getUsage());
		    	setIfNotNull(transaction, "log", reportData.getLog());
		    	
				i++;
				reportData = queue.poll();
			} while (reportData != null && i < MAX_RECORDS);
			
	    	data.add("transactions", (ParameterMap[]) transactions.toArray());
	    	encodedReports.add(new ApiKeyAuthReportToSend(reportData.getEndpoint(), data.encode()));
		}
		return encodedReports;
	}

	@Override
	public ApiKeyAuthReporter addRecord(ApiKeyReportData record) {
		ConcurrentLinkedQueue<ApiKeyReportData> reportGroup = reports.get(record.groupId());
		if (reportGroup == null) {
			reportGroup = new ConcurrentLinkedQueue<>();
			reports.put(record.groupId(), reportGroup);
		}
		
		reportGroup.add(record);
		
		if (reportGroup.size() > FULL_TRIGGER_CAPAC) {
			full(); // This is just approximate, we don't care whether it's somewhat out.
		}
		return this;
	}
	
	private static final class ApiKeyAuthReportToSend implements ReportToSend {
		private final URI endpoint;
		private String data;
		
		public ApiKeyAuthReportToSend(URI endpoint, String data) {
			this.endpoint = endpoint;
			this.data = data;
		}
		
		@Override
		public String getData() {
			return data;
		}

		@Override
		public String getEncoding() {
			return "www/url-encoding..."; //TODO
		}

		@Override
		public URI getEndpoint() {
			return endpoint;
		}
	}
}
