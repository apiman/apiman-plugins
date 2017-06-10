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

import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.AUTHREP_PATH;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.DEFAULT_BACKEND;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.REFERRER;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.SERVICE_ID;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.SERVICE_TOKEN;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.USAGE;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.USER_ID;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.USER_KEY;

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.AuthTypeEnum;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Content;
import io.apiman.plugins.auth3scale.authrep.AbstractAuthExecutor;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.AuthResponseHandler;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
@SuppressWarnings("nls")
public class ApiKeyAuthExecutor extends AbstractAuthExecutor<ApiKeyAuthReporter> {
    // TODO Can't remember the place where we put the special exceptions for this...
    private static final AsyncResultImpl<Void> OK_CACHED = AsyncResultImpl.create((Void) null);
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_USER_KEY = AsyncResultImpl.create(new RuntimeException("No user apikey provided!"));
    private static final AsyncResultImpl<Void> FAIL_NO_ROUTE = AsyncResultImpl.create(new RuntimeException("No valid route"));

    private final Content config;
    private final ApiRequest request;
    private final IPolicyContext context;
    private final ApiKeyCachingAuthenticator authCache;
    private final IHttpClientComponent httpClient;
    private final IPolicyFailureFactoryComponent failureFactory;
    private final IApimanLogger logger;
    // Handlers
    private IAsyncHandler<PolicyFailure> policyFailureHandler;

    ApiKeyAuthExecutor(Content config, ApiRequest request, IPolicyContext context, ApiKeyCachingAuthenticator authCache) {
        super(config, request, context);
        this.config = config;
        this.request = request;
        this.context = context;
        this.authCache = authCache;
        this.httpClient = context.getComponent(IHttpClientComponent.class);
        this.failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
        this.logger = context.getLogger(ApiKeyAuthExecutor.class);
    }

    @Override
    public ApiKeyAuthExecutor auth(IAsyncResultHandler<Void> resultHandler) {
        doAuth(resultHandler);
        return this;
    }

    private void doAuth(IAsyncResultHandler<Void> resultHandler) {
        String userKey = getUserKey();
        if (userKey == null) {
            resultHandler.handle(FAIL_PROVIDE_USER_KEY);
            return;
        }

        if (!hasRoutes(request)) { // TODO Optimise
            resultHandler.handle(FAIL_NO_ROUTE);
            return;
        }

        // If we have no cache entry, then block. Otherwise, the request can immediately go
        // through and we will resolve the rate limiting status post hoc (various strategies
        // depending on settings).
        if (authCache.isAuthCached(config, request, userKey)) {
            logger.debug("Cached auth on request " + request);
            resultHandler.handle(OK_CACHED);
            context.setAttribute("3scale.userKey", userKey);
        } else {
            logger.debug("Uncached auth on request " + request);
            context.setAttribute("3scale.blocking", true); // TODO
            doBlockingAuthRep(userKey, result -> {
                logger.debug("Blocking auth success: {0}", result.isSuccess());
                // Only cache if successful
                if (result.isSuccess()) {
                    authCache.cache(config, request, userKey);
                }
                // Pass result up.
                resultHandler.handle(result);
            });
        }
    }

    private void doBlockingAuthRep(String userKey, IAsyncResultHandler<Void> resultHandler) {
        String serviceId = Long.toString(config.getProxy().getServiceId());
        ParameterMap paramMap = new ParameterMap();
        paramMap.add(USER_KEY, userKey);
        paramMap.add(SERVICE_TOKEN, config.getBackendAuthenticationValue()); // TODO maybe use endpoint properties or something. or new properties field.
        paramMap.add(SERVICE_ID, serviceId);
        paramMap.add(USAGE, buildRepMetrics());

        setIfNotNull(paramMap, REFERRER, request.getHeaders().get(REFERRER));
        setIfNotNull(paramMap, USER_ID, request.getHeaders().get(USER_ID));

        IHttpClientRequest get = httpClient.request(DEFAULT_BACKEND + AUTHREP_PATH + paramMap.encode(),
                HttpMethod.GET,
                new AuthResponseHandler(failureFactory)
                .failureHandler(failure -> {
                    logger.debug("ServiceId: {0} | Blocking AuthRep failure: {1}", serviceId, failure.getResponseCode());
                    policyFailureHandler.handle(failure);
                })
                .exceptionHandler(exception -> resultHandler.handle(AsyncResultImpl.create(exception)))
                .statusHandler(status -> {
                    logger.debug("ServiceId: {0} | {1}", serviceId, status);
                    if (!status.isAuthorized()) {
                        flushCache();
                    } else {
                        resultHandler.handle(OK_CACHED);
                    }
                }));

        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }

    private void flushCache() {
        logger.debug("Invalidating cache");
        authCache.invalidate(config, request, context.getAttribute("3scale.userKey", ""));
    }

    private String getUserKey() {
        return getIdentityElement(config, request, AuthRepConstants.USER_KEY);
    }

    @Override
    public AuthTypeEnum getType() {
        return AuthTypeEnum.API_KEY;
    }

    @Override
    public ApiKeyAuthExecutor policyFailureHandler(IAsyncHandler<PolicyFailure> policyFailureHandler) {
        this.policyFailureHandler = policyFailureHandler;
        return this;
    }
}
