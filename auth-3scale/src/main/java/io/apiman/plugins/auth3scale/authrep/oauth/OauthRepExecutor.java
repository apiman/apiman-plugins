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

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Content;
import io.apiman.plugins.auth3scale.authrep.AbstractRepExecutor;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;

import java.time.OffsetDateTime;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class OauthRepExecutor extends AbstractRepExecutor<OauthAuthReporter> {
    private OauthAuthReporter reporter;

    OauthRepExecutor(Content config, ApiResponse response, ApiRequest request, IPolicyContext context) {
        super(config, request, response, context);
    }

    @Override
    public OauthRepExecutor rep() {
        doRep();
        return this;
    }

    private void doRep() {
        OauthReportData report = new OauthReportData()
                .setEndpoint(REPORT_ENDPOINT)
                .setServiceToken(config.getBackendAuthenticationValue())
                .setServiceId(Long.toString(config.getProxy().getServiceId()))
                .setAppId(getAppId())
                .setUserId(getUserId())
                .setTimestamp(OffsetDateTime.now().toString())
                .setUsage(buildRepMetrics(api))
                .setLog(buildLog());
        reporter.addRecord(report);
    }

    private String getAppId() {
        return getIdentityElement(config, request, AuthRepConstants.APP_ID);
    }

    private String getUserId() {
        return request.getHeaders().get(AuthRepConstants.USER_ID);
    }

    @Override
    public OauthRepExecutor setReporter(OauthAuthReporter reporter) {
        this.reporter = reporter;
        return this;
    }
}
