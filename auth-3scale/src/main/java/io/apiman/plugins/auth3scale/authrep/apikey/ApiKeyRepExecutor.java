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
package io.apiman.plugins.auth3scale.authrep.apikey;

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
public class ApiKeyRepExecutor extends AbstractRepExecutor<ApiKeyAuthReporter> {
    private ApiKeyAuthReporter reporter;

    public ApiKeyRepExecutor(Content config, ApiResponse response, ApiRequest request, IPolicyContext context) {
        super(config, request, response, context);
    }

    // Rep seems to require POST with URLEncoding
    @Override
    public ApiKeyRepExecutor rep() {
        doRep();
        return this;
    }

    private void doRep() {
        ApiKeyReportData report = new ApiKeyReportData()
                .setEndpoint(REPORT_ENDPOINT)
                .setServiceToken(config.getBackendAuthenticationValue())
                .setUserKey(getUserKey())
                .setServiceId(Long.toString(config.getProxy().getServiceId()))
                .setTimestamp(OffsetDateTime.now().toString())
                .setUserId(getUserId())
                .setUsage(buildRepMetrics(api))
                .setLog(buildLog());
        reporter.addRecord(report);
    } // ApiKeyReportData [endpoint=http://su1.3scale.net:80/transactions.xml, serviceToken=null, userKey=6ade731336760382403649c5d75886ee,
    // serviceId=2555417735060, timestamp=2016-10-28T22:57:44.273+01:00, userId=null, usage=ParameterMap [data={foo/fooId=1}], log=ParameterMap [data={code=200}]]

    private String getUserId() {
        return request.getHeaders().get(AuthRepConstants.USER_ID);
    }

    private String getUserKey() {
        return getIdentityElement(config, request, "user_key");
    }

    @Override
    public ApiKeyRepExecutor setReporter(ApiKeyAuthReporter reporter) {
        this.reporter = reporter;
        return this;
    }
}
