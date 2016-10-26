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
public class OauthAuthExecutor extends AbstractAuthExecutor<ApiKeyAuthReporter> {
    private static final String AUTHORIZE_PATH = "/transactions/oauth_authorize.xml?";
//    private static final AsyncResultImpl<Void> OK_CACHED = AsyncResultImpl.create((Void) null);
//    private static final AsyncResultImpl<Void> FAIL_PROVIDE_APP_ID = AsyncResultImpl.create(new RuntimeException("No user app id provided")); // TODO mirror 3scale errors
//    private static final AsyncResultImpl<Void> FAIL_PROVIDE_APP_KEY = AsyncResultImpl.create(new RuntimeException("No user app key provided")); // TODO mirror 3scale errors
//    private static final AsyncResultImpl<Void> FAIL_NO_ROUTE = AsyncResultImpl.create(new RuntimeException("No valid route"));
//    private static final AppIdCachingAuthenticator cachingAuthenticator = new AppIdCachingAuthenticator(); // TODO again, shared DS...

    OauthAuthExecutor(ApiRequest request, IPolicyContext context) {
        super(request, context);
    }

    @Override
    public OauthAuthExecutor auth(IAsyncResultHandler<Void> resultHandler) {
        doAuth(resultHandler);
        return this;
    }

    @Override
    public AuthTypeEnum getType() {
        return AuthTypeEnum.OAUTH;
    }

    private void doAuth(IAsyncResultHandler<Void> resultHandler) {
        doBlockingAuth(resultHandler);
    }

    private void doBlockingAuth(IAsyncResultHandler<Void> resultHandler) { // TODO add caching.
        // Auth elems
        paramMap.add(AuthRepConstants.PROVIDER_KEY, request.getApi().getProviderKey());
        paramMap.add(AuthRepConstants.SERVICE_ID, Long.toString(request.getApi().getApiNumericId()));

        setIfNotNull(paramMap, AuthRepConstants.ACCESS_TOKEN, request.getHeaders().get(AuthRepConstants.ACCESS_TOKEN));
        setIfNotNull(paramMap, AuthRepConstants.APP_ID, getAppId());
        setIfNotNull(paramMap, AuthRepConstants.APP_ID, getClientId()); // from 3scale api docs "client_id == app_id"
        setIfNotNull(paramMap, AuthRepConstants.REFERRER, request.getHeaders().get(AuthRepConstants.REFERRER));
        setIfNotNull(paramMap, AuthRepConstants.USER_ID, request.getHeaders().get(AuthRepConstants.USER_ID));
        setIfNotNull(paramMap, AuthRepConstants.REDIRECT_URL, request.getHeaders().get(AuthRepConstants.REDIRECT_URL));
        setIfNotNull(paramMap, AuthRepConstants.REDIRECT_URI, request.getHeaders().get(AuthRepConstants.USER_ID));

        IHttpClientRequest get = httpClient.request(DEFAULT_BACKEND + AUTHORIZE_PATH + paramMap.encode(),
                HttpMethod.GET,
                new AuthResponseHandler(resultHandler, policyFailureHandler, failureFactory));

        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }

    private String getAppId() {
        return getIdentityElementFromContext(context, request, api, AuthRepConstants.APP_ID);
    }

    private String getClientId() {
        return getIdentityElementFromContext(context, request, api, AuthRepConstants.CLIENT_ID);
    }

}
