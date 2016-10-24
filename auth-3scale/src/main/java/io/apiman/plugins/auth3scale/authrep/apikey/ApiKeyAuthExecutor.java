package io.apiman.plugins.auth3scale.authrep.apikey;


import java.net.URI;
import java.time.OffsetDateTime;

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.authrep.AbstractAuthExecutor;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.AuthResponseHandler;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyAuthExecutor extends AbstractAuthExecutor<ApiKeyAuthReporter> {
    // TODO Can't remember the place where we put the special exceptions for this...
    private static final AsyncResultImpl<Void> OK_CACHED = AsyncResultImpl.create((Void) null);
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_USER_KEY = AsyncResultImpl.create(new RuntimeException("No user apikey provided!"));
    private static final AsyncResultImpl<Void> FAIL_NO_ROUTE = AsyncResultImpl.create(new RuntimeException("No valid route"));
    private static final ApiKeyCachingAuthenticator cachingAuthenticator = new ApiKeyCachingAuthenticator(); // TODO again, shared DS...

    private final IApimanLogger logger;

    public ApiKeyAuthExecutor(ApiRequest request, IPolicyContext context) {
        super(request, context);
        logger = context.getLogger(ApiKeyAuthExecutor.class);
    }

    private boolean hasRoutes(ApiRequest req) {
        return api.getRouteMatcher().match(req.getDestination()).length > 0;
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
}
