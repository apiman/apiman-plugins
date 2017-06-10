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
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.LOG;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.REFERRER;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.REPORT_PATH;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.SERVICE_ID;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.SERVICE_TOKEN;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.USAGE;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.USER_ID;
import static io.apiman.plugins.auth3scale.authrep.AuthRepConstants.USER_KEY;

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Content;
import io.apiman.plugins.auth3scale.authrep.AbstractRepExecutor;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.Status;
import io.apiman.plugins.auth3scale.util.report.AuthResponseHandler;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
@SuppressWarnings("nls")
public class ApiKeyRepExecutor extends AbstractRepExecutor<ApiKeyAuthReporter> {
    private static final URI REPORT_ENDPOINT = URI.create(DEFAULT_BACKEND+REPORT_PATH);
    private final Content config;
    private final ApiRequest request;
    private final IPolicyContext context;
    private final ApiKeyAuthReporter reporter;
    private final ApiKeyCachingAuthenticator authCache;
    private final IHttpClientComponent httpClient;
    private final IPolicyFailureFactoryComponent failureFactory;
    private final IApimanLogger logger;

    public ApiKeyRepExecutor(Content config, ApiRequest request, ApiResponse response, IPolicyContext context, ApiKeyAuthReporter reporter, ApiKeyCachingAuthenticator authCache) {
        super(config, request, response, context, reporter, authCache);
        this.config = config;
        this.request = request;
        this.context = context;
        this.reporter = reporter;
        this.authCache = authCache;
        this.httpClient = context.getComponent(IHttpClientComponent.class);
        this.failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
        this.logger = context.getLogger(ApiKeyRepExecutor.class);
    }

    // Rep seems to require POST with URLEncoding
    @Override
    public ApiKeyRepExecutor rep() {
        doRep();
        return this;
    }

    private void doRep() {
        // If was a blocking request then we already reported, so do nothing.
        if (context.getAttribute("3scale.blocking", false))
            return;

        if (config.getRateLimitingStrategy().isBatched()) {
            doBatchedReport();
        } else {
            doAsyncAuthRep();
        }
    }

    // ApiKeyReportData [endpoint=http://su1.3scale.net:80/transactions.xml, serviceToken=null, userKey=6ade731336760382403649c5d75886ee,
    private void doAsyncAuthRep() {
        // Auth elems
        ParameterMap paramMap = new ParameterMap();
        paramMap.add(USER_KEY, context.getAttribute("3scale.userKey", ""));
        paramMap.add(SERVICE_TOKEN, config.getBackendAuthenticationValue());// maybe use endpoint properties or something. or new properties field.
        paramMap.add(SERVICE_ID, Long.toString(config.getProxy().getServiceId()));
        paramMap.add(USAGE, buildRepMetrics());
        paramMap.add(LOG, buildLog());

        setIfNotNull(paramMap, REFERRER, request.getHeaders().get(REFERRER));
        setIfNotNull(paramMap, USER_ID, request.getHeaders().get(USER_ID));

        IHttpClientRequest get = httpClient.request(DEFAULT_BACKEND + AUTHREP_PATH + paramMap.encode(),
                HttpMethod.GET,
                new AuthResponseHandler(failureFactory)
                .failureHandler(failure -> {
                    // At this point can't do anything but log it.
                    logger.debug("Async AuthRep failure code {0} on: {1}",  failure.getResponseCode(), paramMap);
                })
                .exceptionHandler(ex -> AsyncResultImpl.create(ex))
                .statusHandler(status -> {
                    if (!status.isAuthorized() || rateLimitReached(status)) {
                        flushCache();
                    }
                }));

        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }

    private boolean rateLimitReached(Status status) {
        return status.getUsageReports()
            .stream()
            .filter(report -> report.getCurrentValue() == report.getMaxValue())
            .findFirst()
            .isPresent();
    }

    private void flushCache() {
        logger.debug("Invalidating cache");
        authCache.invalidate(config, request, context.getAttribute("3scale.userKey", ""));
    }

    // serviceId=2555417735060, timestamp=2016-10-28T22:57:44.273+01:00, userId=null, usage=ParameterMap [data={foo/fooId=1}], log=ParameterMap [data={code=200}]]
    private void doBatchedReport() {
        ApiKeyReportData report = new ApiKeyReportData()
                .setEndpoint(REPORT_ENDPOINT)
                .setServiceToken(config.getBackendAuthenticationValue())
                .setUserKey(getUserKey())
                .setServiceId(Long.toString(config.getProxy().getServiceId()))
                .setTimestamp(OffsetDateTime.now().toString())
                .setUserId(getUserId())
                .setUsage(buildRepMetrics())
                .setLog(buildLog());

        logger.debug("Adding a report to batch.");
        reporter.addRecord(report);
    }

    private String getUserId() {
        return request.getHeaders().get(USER_ID);
    }

    private String getUserKey() {
        return getIdentityElement(config, request, "user_key");
    }

}
