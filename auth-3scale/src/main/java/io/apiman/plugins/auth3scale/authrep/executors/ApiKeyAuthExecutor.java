package io.apiman.plugins.auth3scale.authrep.executors;


import java.net.URI;
import java.time.OffsetDateTime;

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.ProxyBean;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.authrep.AuthRepExecutor;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.auth.ApiKeyCachingAuthenticator;
import io.apiman.plugins.auth3scale.util.report.AuthResponseHandler;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ApiKeyAuthReporter;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ApiKeyReportData;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyAuthExecutor extends AuthRepExecutor<ApiKeyAuthReporter> {   
    // TODO Can't remember the place where we put the special exceptions for this...
    private static final AsyncResultImpl<Void> OK_CACHED = AsyncResultImpl.create((Void) null);
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_USER_KEY = AsyncResultImpl.create(new RuntimeException("No user key provided!"));
    private static final AsyncResultImpl<Void> FAIL_NO_ROUTE = AsyncResultImpl.create(new RuntimeException("No valid route"));
    private static final ApiKeyCachingAuthenticator cachingAuthenticator = new ApiKeyCachingAuthenticator(); // TODO again, shared DS...

    @SuppressWarnings("unused")
    private final IApimanLogger logger;
    private ApiKeyAuthReporter reporter;
    
    public ApiKeyAuthExecutor(ApiRequest request, IPolicyContext context) {
        super(request, context);
        logger = context.getLogger(ApiKeyAuthExecutor.class);
    }
    
    public ApiKeyAuthExecutor(ApiResponse response, ApiRequest request, IPolicyContext context) {
        super(request, response, context);
        logger = context.getLogger(ApiKeyAuthExecutor.class);
    }
    
    private ParameterMap setIfNotNull(ParameterMap in, String k, String v) {
        if (v == null)
            return in;
        
        in.add(k, v);
        return in;
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
            System.out.println("Cached");
            resultHandler.handle(OK_CACHED);
        } else {
            System.out.println("Doing uncached auth");
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
        // Switch between oauth, key, and id+key when added
        IHttpClientRequest get = httpClient.request(DEFAULT_BACKEND + AUTHORIZE_PATH + paramMap.encode(),
                HttpMethod.GET,
                new AuthResponseHandler(resultHandler, policyFailureHandler, failureFactory));

        //get.setConnectTimeout(1000);
        //get.setReadTimeout(1000);
        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }

    // Rep seems to require POST with URLEncoding 
    @Override
    public ApiKeyAuthExecutor rep() {
        doRep();
        return this;
    }
    
    private static final URI REPORT_ENDPOINT = URI.create(DEFAULT_BACKEND+REPORT_PATH);
    
    public void doRep() {
        // Auth elems
        //paramMap.add(AuthRepConstants.USER_KEY, getUserKey()); // maybe use endpoint properties or something. or new properties field.
        //paramMap.add(AuthRepConstants.PROVIDER_KEY, request.getApi().getProviderKey()); 
        //paramMap.add(AuthRepConstants.SERVICE_ID, Long.toString(api.getApiNumericId()));
        
        //ParameterMap transactionArray[] = new ParameterMap[1]; // Will bump this up. Just testing...
        //transactionArray[0] = new ParameterMap();
        
        // Metrics / Usage
        //paramMap.add("transactions", transactionArray);
        //transactionArray[0].add("usage", buildRepMetrics(api));



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


    /**
     * Mapping Rules Syntax A Mapping Rule has to start with '/'. This looks for
     * matches from the beginning of the string, ignoring the rest if the first
     * characters match. The pattern can only contain valid URL characters and
     * 'wildcards' - words inside curly brackets ('{}') that match any string up
     * to the following slash, ampersand or question mark.
     * 
     * Examples /v1/word/{whateverword}.json /{version}/word/{foo}.json
     * /{version}/word/{bar}.json?action=create
     * /{version}/word/{baz}.json?action={my_action} More than one Mapping Rule
     * can match the request path but if none matches, the request is discarded
     * (404).
     * 
     * Add a dollar sign ($) to the end of a pattern to apply stricter matching.
     * For example, /foo/bar$ will only match /foo/bar requests and won't match
     * /foo/bar/baz requests.
     */

    private ParameterMap buildRepMetrics(Api api) {
        ParameterMap pm = new ParameterMap(); // TODO could be interesting to cache a partially built map and just replace values?  
        
        int[] matches = api.getRouteMatcher().match(request.getDestination());
        if (matches.length > 0) { // TODO could put this into request and process it up front. This logic could be removed from bean.
            for (int matchIndex : matches) {
                ProxyBean proxyRule = api.getProxyBean().get(matchIndex+1); // Get specific proxy rule that matches. (e.g. / or /foo/bar)
                String metricName = proxyRule.getMetricSystemName();
    
                if (pm.containsKey(metricName)) {
                    long newValue = pm.getLongValue(metricName) + proxyRule.getDelta(); // Add delta.
                    pm.setLongValue(metricName, newValue);
                } else {
                    pm.setLongValue(metricName, proxyRule.getDelta()); // Otherwise value is delta.
                }
                                                
                // I don't understand this, the nginx code never seems to actually DO anything with the values... Confused.             
                //              Pattern regex = proxyRule.getRegex(); // Regex with group matching support.
                //              Matcher matcher = regex.matcher(request.getDestination()); // Match against path
                //              for (int g = 1; g < matcher.groupCount(); g++) {
                //                  String metricName = proxyRule.getMetricSystemName();
                //                  String metricValue = matcher.group(g);
                //              }
            }
        }
        return pm;
    }
    
    private String getUserKey() {
        String userKey = context.getAttribute("user-key", null); // TODO
        if (userKey == null) {
          if (api.getUserKeyLocation() == Api.UserKeyLocationEnum.HEADER) {
              userKey = request.getHeaders().get(api.getUserKeyField());
          } else { // else UserKeyLocationEnum.QUERY
              userKey = request.getQueryParams().get(api.getUserKeyField());
          }
          context.setAttribute("user-key", userKey);
        }
        return userKey;
    }

    @Override
    public ApiKeyAuthExecutor setReporter(ApiKeyAuthReporter reporter) {
        this.reporter = reporter;
        return this;
    }
}
