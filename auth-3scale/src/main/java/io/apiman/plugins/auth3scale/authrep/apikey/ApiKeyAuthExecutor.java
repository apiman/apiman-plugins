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
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.AuthTypeEnum;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.authrep.AbstractAuthExecutor;
import io.apiman.plugins.auth3scale.util.report.AuthResponseHandler;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyAuthExecutor extends AbstractAuthExecutor<ApiKeyAuthReporter> {
    private static final String AUTHORIZE_PATH = "/transactions/authorize.xml?";
    // TODO Can't remember the place where we put the special exceptions for this...
    private static final AsyncResultImpl<Void> OK_CACHED = AsyncResultImpl.create((Void) null);
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_USER_KEY = AsyncResultImpl.create(new RuntimeException("No user apikey provided!"));
    private static final AsyncResultImpl<Void> FAIL_NO_ROUTE = AsyncResultImpl.create(new RuntimeException("No valid route"));
    private static final ApiKeyCachingAuthenticator cachingAuthenticator = new ApiKeyCachingAuthenticator(); // TODO again, shared DS...

    private final IApimanLogger logger;

    ApiKeyAuthExecutor(ApiRequest request, IPolicyContext context) {
        super(request, context);
        logger = context.getLogger(ApiKeyAuthExecutor.class);
    }

    @Override
    public ApiKeyAuthExecutor auth(IAsyncResultHandler<Void> resultHandler) {
        doAuth(resultHandler);
        return this;
    }

    @Override
    public AuthTypeEnum getType() {
        return AuthTypeEnum.API_KEY;
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

        if (cachingAuthenticator.isAuthCached(request, userKey)) {
            logger.debug("Cached auth on request " + request);
            resultHandler.handle(OK_CACHED);
        } else {
            logger.debug("Uncached auth on request " + request);
            doBlockingAuth(resultHandler, userKey);
            cachingAuthenticator.cache(request, userKey);
        }
    }

    private void doBlockingAuth(IAsyncResultHandler<Void> resultHandler, String userKey) {
        // Auth elems
        paramMap.add(AuthRepConstants.USER_KEY, userKey);
        paramMap.add(AuthRepConstants.PROVIDER_KEY, request.getApi().getProviderKey()); // maybe use endpoint properties or something. or new properties field.
        paramMap.add(AuthRepConstants.SERVICE_ID, Long.toString(request.getApi().getApiNumericId()));

        setIfNotNull(paramMap, AuthRepConstants.REFERRER, request.getHeaders().get(AuthRepConstants.REFERRER));
        setIfNotNull(paramMap, AuthRepConstants.USER_ID, request.getHeaders().get(AuthRepConstants.USER_ID));

        // TODO can also do predicted usage, if we see value in that..?
        // Switch between oauth, apikey, and id+apikey when added
        IHttpClientRequest get = httpClient.request(DEFAULT_BACKEND + AUTHORIZE_PATH + paramMap.encode(),
                HttpMethod.GET,
                new AuthResponseHandler(resultHandler, policyFailureHandler, failureFactory));

        //get.setConnectTimeout(1000);
        //get.setReadTimeout(1000);
        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }

    private String getUserKey() {
        return getIdentityElementFromContext(context, request, api, AuthRepConstants.USER_KEY);
    }
}
