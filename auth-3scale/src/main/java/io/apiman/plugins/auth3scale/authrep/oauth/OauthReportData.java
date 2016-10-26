/*
 * Copyright 2016 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.plugins.auth3scale.authrep.oauth;

import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReportData;

import java.net.URI;
import java.util.Objects;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class OauthReportData implements ReportData {
    private URI endpoint;
    private String serviceToken;
    private String serviceId;
    private String appId;
    private String userId;
    private String timestamp;
    private ParameterMap usage;
    private ParameterMap log;

    @Override
    public ParameterMap getUsage() {
        return usage;
    }

    @Override
    public ParameterMap getLog() {
        return log;
    }

    @Override
    public int groupId() {
        return hashCode();
    }

    public int hashCode() { //TODO
        return Objects.hash(endpoint, serviceToken, serviceId, appId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OauthReportData that = (OauthReportData) o;

        if (!endpoint.equals(that.endpoint)) return false;
        if (!serviceToken.equals(that.serviceToken)) return false;
        if (!serviceId.equals(that.serviceId)) return false;
        return appId.equals(that.appId);
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public OauthReportData setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public OauthReportData setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public OauthReportData setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getAppId() {
        return appId;
    }

    public OauthReportData setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public OauthReportData setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public OauthReportData setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public OauthReportData setUsage(ParameterMap usage) {
        this.usage = usage;
        return this;
    }

    public OauthReportData setLog(ParameterMap log) {
        this.log = log;
        return this;
    }

}
