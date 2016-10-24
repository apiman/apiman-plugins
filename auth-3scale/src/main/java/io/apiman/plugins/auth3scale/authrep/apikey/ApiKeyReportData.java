package io.apiman.plugins.auth3scale.authrep.apikey;

import java.net.URI;
import java.util.Objects;

import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReportData;

public class ApiKeyReportData implements ReportData {
    
    private final URI endpoint;
    private final String serviceToken;
    private final String userKey;
    private final String serviceId;
    private final String timestamp;
    private final String userId;
    private final ParameterMap metrics;
    private final ParameterMap log;

    public ApiKeyReportData(URI endpoint,
            String serviceToken,
            String userKey, 
            String serviceId,
            String timestamp,
            String userId,
            ParameterMap usage,
            ParameterMap log) {
        this.endpoint = endpoint;
        this.serviceToken = serviceToken;
        this.userKey = userKey;
        this.serviceId = serviceId;
        this.timestamp = timestamp;
        this.userId = userId;
        this.metrics = usage;
        this.log = log; }
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

    public String getTimestamp() {
        return timestamp;
    }
    
    public String getUserId() {
        return userId;
    }
}
