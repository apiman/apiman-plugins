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

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.AuthTypeEnum;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReportData;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public abstract class AbstractAuthExecutor<T extends AbstractReporter<? extends ReportData>> implements IdentityFromContext {
    protected static final String DEFAULT_BACKEND = "http://su1.3scale.net:80";

    protected final ApiRequest request;
    protected final IHttpClientComponent httpClient;
    protected final IPolicyFailureFactoryComponent failureFactory;
    protected final ParameterMap paramMap;
    protected final Api api;
    
    protected IAsyncHandler<PolicyFailure> policyFailureHandler;
    protected IPolicyContext context;
        
    private AbstractAuthExecutor(ApiRequest request, Api api, IPolicyContext context) {
        this.request = request;
        this.api = api;
        this.httpClient = context.getComponent(IHttpClientComponent.class);
        this.failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
        this.context = context;
        this.paramMap = new ParameterMap();
    }
    
    public AbstractAuthExecutor(ApiRequest request, IPolicyContext context) {
        this(request, request.getApi(), context);
    }

    public abstract AbstractAuthExecutor<T> auth(IAsyncResultHandler<Void> handler);

    public AbstractAuthExecutor<T> setPolicyFailureHandler(IAsyncHandler<PolicyFailure> policyFailureHandler) {
        this.policyFailureHandler = policyFailureHandler;
        return this;
    }

    protected ParameterMap setIfNotNull(ParameterMap in, String k, String v) {
        if (v == null)
            return in;

        in.add(k, v);
        return in;
    }

    protected boolean hasRoutes(ApiRequest req) {
        return api.getRouteMatcher().match(req.getDestination()).length > 0;
    }

    public abstract AuthTypeEnum getType();
}
