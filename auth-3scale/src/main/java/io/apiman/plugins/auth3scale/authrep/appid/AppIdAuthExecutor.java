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

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.AuthTypeEnum;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AbstractAuthExecutor;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.authrep.apikey.ApiKeyAuthReporter;
import io.apiman.plugins.auth3scale.util.report.AuthResponseHandler;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AppIdAuthExecutor extends AbstractAuthExecutor<ApiKeyAuthReporter> {
    private static final String AUTHORIZE_PATH = "/transactions/authorize.xml?";
    private static final AsyncResultImpl<Void> OK_CACHED = AsyncResultImpl.create((Void) null);
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_APP_ID = AsyncResultImpl.create(new RuntimeException("No user app id provided")); // TODO mirror 3scale errors
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_APP_KEY = AsyncResultImpl.create(new RuntimeException("No user app key provided")); // TODO mirror 3scale errors
    private static final AsyncResultImpl<Void> FAIL_NO_ROUTE = AsyncResultImpl.create(new RuntimeException("No valid route"));
    private static final AppIdCachingAuthenticator cachingAuthenticator = new AppIdCachingAuthenticator(); // TODO again, shared DS...

    private final IApimanLogger logger;

    public AppIdAuthExecutor(ApiRequest request, IPolicyContext context) {
        super(request, context);
        logger = context.getLogger(AppIdAuthExecutor.class);
    }

    @Override
    public AppIdAuthExecutor auth(IAsyncResultHandler<Void> resultHandler) {
        doAuth(resultHandler);
        return this;
    }

    @Override
    public AuthTypeEnum getType() {
        return AuthTypeEnum.APP_ID;
    }

    private void doAuth(IAsyncResultHandler<Void> resultHandler) {
        String appId = getAppId();
        String appKey = getAppKey();

        if (appId == null) {
            resultHandler.handle(FAIL_PROVIDE_APP_ID);
            return;
        }

        if (appKey == null) {
            resultHandler.handle(FAIL_PROVIDE_APP_KEY);
            return;
        }

        if (!hasRoutes(request)) { // TODO Optimise
            resultHandler.handle(FAIL_NO_ROUTE);
            return;
        }

        if (cachingAuthenticator.isAuthCached(request, appId, appKey)) {
            logger.debug("Cached auth on request " + request);
            resultHandler.handle(OK_CACHED);
        } else {
            logger.debug("Uncached auth on request " + request);
            doBlockingAuth(resultHandler, appId, appKey);
            cachingAuthenticator.cache(request, appId, appKey);
        }
    }

    private void doBlockingAuth(IAsyncResultHandler<Void> resultHandler, String appId, String appKey) {
        // Auth elems
        paramMap.add(AuthRepConstants.APP_ID, appId);
        paramMap.add(AuthRepConstants.APP_KEY, appKey); // TODO possibly optional according to API docs, but not 100% clear
        paramMap.add(AuthRepConstants.PROVIDER_KEY, request.getApi().getProviderKey());
        paramMap.add(AuthRepConstants.SERVICE_ID, Long.toString(request.getApi().getApiNumericId()));

        setIfNotNull(paramMap, AuthRepConstants.REFERRER, request.getHeaders().get(AuthRepConstants.REFERRER));
        setIfNotNull(paramMap, AuthRepConstants.USER_ID, request.getHeaders().get(AuthRepConstants.USER_ID));

        IHttpClientRequest get = httpClient.request(DEFAULT_BACKEND + AUTHORIZE_PATH + paramMap.encode(),
                HttpMethod.GET,
                new AuthResponseHandler(resultHandler, policyFailureHandler, failureFactory));

        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }

    private String getAppKey() {
        return getIdentityElementFromContext(context, request, api, AuthRepConstants.APP_KEY);
    }

    private String getAppId() {
        return getIdentityElementFromContext(context, request, api, AuthRepConstants.APP_ID);
    }
}
