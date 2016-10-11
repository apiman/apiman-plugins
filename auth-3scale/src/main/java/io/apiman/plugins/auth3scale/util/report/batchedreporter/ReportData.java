package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.util.List;

import io.apiman.plugins.auth3scale.util.ParameterMap;

// Report data itself.
public interface ReportData {
	
	public enum ReportGroupType {
		OAUTH, API_KEY, APP_ID
	}

	// E.g. OAUTH/KEY/
	ReportGroupType getBatchedReportGroup();
	
	String getTimestamp();
	String getServiceToken();
	String getServiceId();
	
	// Do not need ServiceId or ServiceToken here
	List<ParameterMap> getUsage();
	
	// Key to indicate which item(s) can be reported together
	// For instance, combination of a couple of IDs. (user id, service id, ...)
	// Could just be a string?
//	public abstract class BatchedReportGroup {
//		public abstract String getKey();
//		public abstract boolean equals(Object other);
//		public abstract int hashCode();
//	}

}
