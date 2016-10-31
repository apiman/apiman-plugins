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

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AbstractRepExecutor;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;

import java.time.OffsetDateTime;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AppIdRepExecutor extends AbstractRepExecutor<AppIdAuthReporter> {
    private AppIdAuthReporter reporter;

    public AppIdRepExecutor(ApiResponse response, ApiRequest request, IPolicyContext context) {
        super(request, response, context);
    }

    // Rep seems to require POST with URLEncoding 
    @Override
    public AppIdRepExecutor rep() {
        doRep();
        return this;
    }

    private void doRep() {
        AppIdReportData report = new AppIdReportData()
                .setEndpoint(REPORT_ENDPOINT)
                .setServiceToken(api.getProviderKey())
                .setServiceId(Long.toString(api.getApiNumericId()))
                .setAppId(getAppId())
                .setUserId(getUserId())
                .setTimestamp(OffsetDateTime.now().toString())
                .setUsage(buildRepMetrics(api))
                .setLog(buildLog());
        reporter.addRecord(report);
    }

    private String getAppId() {
        return getIdentityElementFromContext(context, request, api, AuthRepConstants.APP_ID);
    }

    private String getUserId() {
        return getIdentityElementFromContext(context, request, api, AuthRepConstants.USER_ID);
    }

    @Override
    public AppIdRepExecutor setReporter(AppIdAuthReporter reporter) {
        this.reporter = reporter;
        return this;
    }
}
