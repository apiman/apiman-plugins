package io.apiman.plugins.auth3scale.authrep.executors;

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.authrep.AuthRepExecutor;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.UsageReport;
import io.apiman.plugins.auth3scale.util.report.AuthResponseHandler;
import io.apiman.plugins.auth3scale.util.report.ReportResponseHandler;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyAuthExecutor extends AuthRepExecutor {

//    protected final String BACKEND = "https://su1.3scale.net";
//    protected final ApiRequest request;
//    protected final Api api;
//    protected final IHttpClientComponent httpClient;
//    protected final IPolicyFailureFactoryComponent failureFactory;
//    protected final ParamterMap queryMap;

    // TODO Can't remember the place where we put the special exceptions for this... 
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_USER_KEY = AsyncResultImpl.create(new RuntimeException("No user key provided!"));
    @SuppressWarnings("unused")
	private final IApimanLogger logger;
    
    public ApiKeyAuthExecutor(ApiRequest request, IPolicyContext context) {
    	super(request, context);
    	logger = context.getLogger(ApiKeyAuthExecutor.class);
    }
    
    public ApiKeyAuthExecutor(ApiResponse response, Api api, IPolicyContext context) {
    	super(response, api, context);
    	logger = context.getLogger(ApiKeyAuthExecutor.class);
    }
    
    private ParameterMap setIfNotNull(ParameterMap in, String k, String v) {
    	if (v == null)
    		return in;
    	
    	in.add(k, v);
    	return in;
    }

    @Override
    public void auth(IAsyncResultHandler<Void> resultHandler) {
        String userKey = getUserKey();
        if (userKey == null) {
        	resultHandler.handle(FAIL_PROVIDE_USER_KEY);
        	return;
        }
    	
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
        
        get.setConnectTimeout(1000);
        get.setReadTimeout(1000);
        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }
    
    public void rep(UsageReport[] reports, IAsyncResultHandler<Void> handler) {
    	// Batched reports? How does this work, precisely?
    }

    // Rep seems to require POST with URLEncoding 
	@Override
	public void rep(IAsyncResultHandler<Void> handler) {
        // Auth elems
		paramMap.add(AuthRepConstants.USER_KEY, getUserKey());
    	paramMap.add(AuthRepConstants.PROVIDER_KEY, request.getApi().getProviderKey()); // maybe use endpoint properties or something. or new properties field.
    	paramMap.add(AuthRepConstants.SERVICE_ID, Long.toString(request.getApi().getApiNumericId()));
		
        // Metrics / Usage
    	paramMap.addUsage(buildMetrics(api));
    	
		// Report
		IHttpClientRequest post = httpClient.request(DEFAULT_BACKEND + REPORT_PATH, 
        		HttpMethod.POST, 
        		new ReportResponseHandler(handler));
		
		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
		post.write(paramMap.encode(), "UTF-8");
	}

	private ParameterMap buildMetrics(Api api) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void authrep(IAsyncResultHandler<Void> handler) {
		// TODO Auto-generated method stub
		
	}

    private String getUserKey() {
        if (api.getUserKeyLocation() == Api.UserKeyLocationEnum.HEADER) {
            return request.getHeaders().get(api.getUserKeyField());
        } else { // else UserKeyLocationEnum.QUERY
            return request.getQueryParams().get(api.getUserKeyField());
        }
    }
}
