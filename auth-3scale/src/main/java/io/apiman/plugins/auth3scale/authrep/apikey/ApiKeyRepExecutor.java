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

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AbstractRepExecutor;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.util.ParameterMap;

import java.time.OffsetDateTime;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyRepExecutor extends AbstractRepExecutor<ApiKeyAuthReporter> {
    // TODO Can't remember the place where we put the special exceptions for this...
    private static final AsyncResultImpl<Void> OK_CACHED = AsyncResultImpl.create((Void) null);
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_USER_KEY = AsyncResultImpl.create(new RuntimeException("No user apikey provided!"));
    private static final AsyncResultImpl<Void> FAIL_NO_ROUTE = AsyncResultImpl.create(new RuntimeException("No valid route"));
    private static final ApiKeyCachingAuthenticator cachingAuthenticator = new ApiKeyCachingAuthenticator(); // TODO again, shared DS...

    private ApiKeyAuthReporter reporter;

    public ApiKeyRepExecutor(ApiResponse response, ApiRequest request, IPolicyContext context) {
        super(request, response, context);
    }

    // Rep seems to require POST with URLEncoding 
    @Override
    public ApiKeyRepExecutor rep() {
        doRep();
        return this;
    }

    private void doRep() {
        reporter.addRecord(new ApiKeyReportData(REPORT_ENDPOINT,
                request.getApi().getProviderKey(), // serviceToken, 
                getUserKey(), // userKey,
                Long.toString(api.getApiNumericId()), // serviceId, 
                OffsetDateTime.now().toString(), // timestamp, 
                request.getHeaders().get(AuthRepConstants.USER_ID), // userId, 
                buildRepMetrics(api), // usage, 
                new ParameterMap().add("code", (long) response.getCode()) // log (not holding raw req/res)
        ));
    }

    private String getUserKey() {
        return getIdentityElementFromContext(context, request, api, "user_key");
    }

    @Override
    public ApiKeyRepExecutor setReporter(ApiKeyAuthReporter reporter) {
        this.reporter = reporter;
        return this;
    }
}
