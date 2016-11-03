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
package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.AuthTypeEnum;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPeriodicComponent;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.apikey.ApiKeyFactory;
import io.apiman.plugins.auth3scale.authrep.appid.AppIdFactory;
import io.apiman.plugins.auth3scale.authrep.oauth.OauthFactory;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.BatchedReporter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AuthRep {
    private Map<AuthTypeEnum, AuthRepFactory> factories = new HashMap<>();
    private BatchedReporter batchedReporter;
    private volatile boolean reporterInitialised = false;

    public AuthRep(BatchedReporter batchedReporter) {
        this.batchedReporter = batchedReporter;

        ApiKeyFactory apiKeyFactory = new ApiKeyFactory();
        AppIdFactory appIdFactory = new AppIdFactory();
        OauthFactory oauthFactory = new OauthFactory();

        factories.put(AuthTypeEnum.API_KEY, apiKeyFactory);
        factories.put(AuthTypeEnum.APP_ID, appIdFactory);
        factories.put(AuthTypeEnum.OAUTH, oauthFactory);

        batchedReporter.addReporter(apiKeyFactory.getReporter())
                .addReporter(appIdFactory.getReporter())
                .addReporter(oauthFactory.getReporter());
    }
    
    public AbstractAuthExecutor<?> createAuth(ApiRequest request, IPolicyContext context) {
        return factories.get(request.getApi().getAuthType()).createAuth(request, context);
    }

    public AbstractRepExecutor<?> createRep(ApiResponse response, ApiRequest request, IPolicyContext context) {
        safeInitialise(context);
        return factories.get(request.getApi().getAuthType()).createRep(response, request, context);
    }
    
    private void safeInitialise(IPolicyContext context) {
        if (!reporterInitialised) {
            synchronized (this) {
                if (!reporterInitialised) {
                    System.out.println("Initialising reporter...");
                    batchedReporter.start(context.getComponent(IPeriodicComponent.class), context.getComponent(IHttpClientComponent.class));
                    reporterInitialised = true;
                }
            }
        }
    }
}
