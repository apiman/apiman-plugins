package io.apiman.plugins.auth3scale.authrep.apikey;


import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AbstractRepExecutor;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.util.ParameterMap;

import java.net.URI;
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

    @SuppressWarnings("unused")
    private final IApimanLogger logger;
    private ApiKeyAuthReporter reporter;

    public ApiKeyRepExecutor(ApiResponse response, ApiRequest request, IPolicyContext context) {
        super(request, response, context);
        logger = context.getLogger(ApiKeyRepExecutor.class);
    }

    private boolean hasRoutes(ApiRequest req) {
        return api.getRouteMatcher().match(req.getDestination()).length > 0;
    }

    // Rep seems to require POST with URLEncoding 
    @Override
    public ApiKeyRepExecutor rep() {
        doRep();
        return this;
    }
    
    private static final URI REPORT_ENDPOINT = URI.create(DEFAULT_BACKEND+REPORT_PATH);

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
        String userKey = context.getAttribute("user-apikey", null); // TODO
        if (userKey == null) {
          if (api.getUserKeyLocation() == Api.UserKeyLocationEnum.HEADER) {
              userKey = request.getHeaders().get(api.getUserKeyField());
          } else { // else UserKeyLocationEnum.QUERY
              userKey = request.getQueryParams().get(api.getUserKeyField());
          }
          context.setAttribute("user-apikey", userKey);
        }
        return userKey;
    }

    @Override
    public ApiKeyRepExecutor setReporter(ApiKeyAuthReporter reporter) {
        this.reporter = reporter;
        return this;
    }
}
