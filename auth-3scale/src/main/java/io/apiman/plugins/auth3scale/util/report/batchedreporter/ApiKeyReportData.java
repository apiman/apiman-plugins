package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.net.URI;
import java.util.Objects;

import io.apiman.plugins.auth3scale.util.ParameterMap;

public class ApiKeyReportData implements ReportData {
	
	private final URI endpoint;
	private final String serviceToken;
	private final String userKey;
	private final String providerKey;
	private final String serviceId;
	private final String referrer;
	private final String userId;
	private final ParameterMap metrics;
	private final ParameterMap log;

	public ApiKeyReportData(URI endpoint,
			String serviceToken,
			String userKey, 
			String providerKey, 
			String serviceId,
			String referrer,
			String userId,
			ParameterMap usage,
			ParameterMap log) {
		this.endpoint = endpoint;
		this.serviceToken = serviceToken;
		this.userKey = userKey;
		this.providerKey = providerKey;
		this.serviceId = serviceId;
		this.referrer = referrer;
		this.userId = userId;
		this.metrics = usage;
		this.log = log;	}
	///////
	
	public URI getEndpoint() {
		return endpoint;
	}
	
	public int groupId() {
		return hashCode();
	}
	
	@Override
	public int hashCode() { //TODO
		return Objects.hash(endpoint, serviceToken, serviceId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApiKeyReportData other = (ApiKeyReportData) obj;
		if (endpoint == null) {
			if (other.endpoint != null)
				return false;
		} else if (!endpoint.equals(other.endpoint))
			return false;
		if (serviceId == null) {
			if (other.serviceId != null)
				return false;
		} else if (!serviceId.equals(other.serviceId))
			return false;
		if (serviceToken == null) {
			if (other.serviceToken != null)
				return false;
		} else if (!serviceToken.equals(other.serviceToken))
			return false;
		return true;
	}

	public String getServiceToken() {
		return serviceToken;
	}

	public String getUserKey() {
		return userKey;
	}

	public String getProviderKey() {
		return providerKey;
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public ParameterMap getUsage() {
		return metrics;
	}
	
	@Override
	public ParameterMap getLog() {
		return log;
	}

	public String getReferrer() {
		return referrer;
	}
	
	public String getUserId() {
		return userId;
	}
}
