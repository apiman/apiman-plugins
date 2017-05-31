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
package io.apiman.plugins.auth3scale.authrep.appid;

import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReportData;

import java.net.URI;
import java.util.Objects;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AppIdReportData implements ReportData {
    private URI endpoint;
    private String serviceToken;
    private String serviceId;
    private String appId;
    private String userId;
    private String timestamp;
    private ParameterMap usage;
    private ParameterMap log;

    public AppIdReportData() {}

    public URI getEndpoint() {
        return endpoint;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getAppId() {
        return appId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public ParameterMap getUsage() {
        return usage;
    }

    @Override
    public ParameterMap getLog() {
        return log;
    }

    public AppIdReportData setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public AppIdReportData setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
        return this;
    }

    public AppIdReportData setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public AppIdReportData setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public AppIdReportData setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public AppIdReportData setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public AppIdReportData setUsage(ParameterMap usage) {
        this.usage = usage;
        return this;

    }

    public AppIdReportData setLog(ParameterMap log) {
        this.log = log;
        return this;
    }

    @Override
    public int groupId() {
        return hashCode();
    }

    @Override
    public String toString() {
        return "AppIdReportData [endpoint=" + endpoint + ", serviceToken=" + serviceToken + ", serviceId="
                + serviceId + ", appId=" + appId + ", userId=" + userId + ", timestamp=" + timestamp
                + ", usage=" + usage + ", log=" + log + "]";
    }

    public int hashCode() { //TODO
        return Objects.hash(endpoint, serviceToken, serviceId, appId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppIdReportData that = (AppIdReportData) o;

        if (!endpoint.equals(that.endpoint)) return false;
        if (!serviceToken.equals(that.serviceToken)) return false;
        if (!serviceId.equals(that.serviceId)) return false;
        return appId.equals(that.appId);
    }
}
