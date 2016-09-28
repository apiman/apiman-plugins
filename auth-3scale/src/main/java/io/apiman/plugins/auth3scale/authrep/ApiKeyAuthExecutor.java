package io.apiman.plugins.auth3scale.authrep;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.beans.util.QueryMap;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.components.http.IHttpClientResponse;
import io.apiman.gateway.engine.policies.PolicyFailureCodes;
import io.apiman.gateway.engine.policy.IPolicyContext;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyAuthExecutor extends AuthRepExecutor {

//    protected final String BACKEND = "https://su1.3scale.net";
//    protected final ApiRequest request;
//    protected final Api api;
//    protected final IHttpClientComponent httpClient;
//    protected final IPolicyFailureFactoryComponent failureFactory;
//    protected final QueryMap queryMap;

    private static final AsyncResultImpl<Void> OK_RESPONSE = AsyncResultImpl.create((Void) null);
    // TODO Can't remember the place where we put the special exceptions for this... 
    private static final AsyncResultImpl<Void> FAIL_PROVIDE_USER_KEY = AsyncResultImpl.create(new RuntimeException("No user key provided!"));
    private final IApimanLogger logger;
    
    
    ApiKeyAuthExecutor(ApiRequest request, IPolicyContext context) {
    	super(request, context);
    	logger = context.getLogger(ApiKeyAuthExecutor.class);
    }
    
    ApiKeyAuthExecutor(ApiResponse response, Api api, IPolicyContext context) {
    	super(response, api, context);
    	logger = context.getLogger(ApiKeyAuthExecutor.class);
    }
    
    private QueryMap setIfNotNull(QueryMap in, String k, String v) {
    	if (v == null)
    		return in;
    	
    	return (QueryMap) in.add(k, v);
    }

    @Override
    public void auth(IAsyncResultHandler<Void> handler) {
        String userKey = getUserKey();
        if (userKey == null) {
        	handler.handle(FAIL_PROVIDE_USER_KEY);
        	return;
        }
    	
        // Auth elems
    	queryMap.put(AuthRepConstants.USER_KEY, userKey);
        queryMap.put(AuthRepConstants.PROVIDER_KEY, request.getApi().getProviderKey()); // maybe use endpoint properties or something. or new properties field.
        queryMap.put(AuthRepConstants.SERVICE_ID, Long.toString(request.getApi().getApiNumericId()));
        
    	setIfNotNull(queryMap, AuthRepConstants.REFERRER, request.getHeaders().get(AuthRepConstants.REFERRER));
    	setIfNotNull(queryMap, AuthRepConstants.USER_ID, request.getHeaders().get(AuthRepConstants.USER_ID));
    	
    	// TODO can also do predicted usage, if we see value in that..?
        // Switch between oauth, key, and id+key when added
        IHttpClientRequest get = httpClient.request(DEFAULT_BACKEND + AUTHORIZE_PATH + queryMap.toQueryString(), 
        		HttpMethod.GET, result -> {
            if (result.isSuccess()) {
                System.err.println("Successfully connected to backend");
                
                IHttpClientResponse response = result.getResult();
                PolicyFailure policyFailure = null;

                switch (response.getResponseCode()) {
                    case 200:
                        System.out.println("3scale backend was happy");
                        System.out.println(response.getBody());
                        handler.handle(OK_RESPONSE);
                        break;
                    case 403:
                        // May be able to treat all error cases without distinction by using parsed response, maybe?
                    	policyFailure = failureFactory.createFailure(PolicyFailureType.Authentication, 
                    			PolicyFailureCodes.BASIC_AUTH_FAILED, 
                    			response.getResponseMessage());
                        break;
                    case 409:  // Possibly over limit
                    	policyFailure = failureFactory.createFailure(PolicyFailureType.Other, 
                    			PolicyFailureCodes.RATE_LIMIT_EXCEEDED, 
                    			response.getResponseMessage());
                        break;
                    default:
                        System.err.println("Unexpected or undocumented response code"); // TODO catchall. policy failure or exception?
                        break;
                }
                
                if (policyFailure != null)
                	policyFailureHandler.handle(policyFailure);
                
                response.close();
            } else {
                System.err.println("HTTP request failed ...");
                result.getError().printStackTrace();
                handler.handle(AsyncResultImpl.create(result.getError()));
            }
        });
        
        get.setConnectTimeout(1000);
        get.setReadTimeout(1000);
        get.addHeader("Accept-Charset", "UTF-8");
        get.addHeader("X-3scale-User-Client", "apiman");
        get.end();
    }

    // Rep seems to require POST with URLEncoding 
	@Override
	public void rep(IAsyncResultHandler<Void> handler) {
        // Auth elems
    	queryMap.put(AuthRepConstants.USER_KEY, getUserKey());
        queryMap.put(AuthRepConstants.PROVIDER_KEY, request.getApi().getProviderKey()); // maybe use endpoint properties or something. or new properties field.
        queryMap.put(AuthRepConstants.SERVICE_ID, Long.toString(request.getApi().getApiNumericId()));
		
        // Metrics
        
        // Usage
		
		// Report
		IHttpClientRequest post = httpClient.request(DEFAULT_BACKEND + REPORT_PATH + queryMap.toQueryString(), 
        		HttpMethod.POST, result -> {
        			
        		});
		
		//post.write(data); TODO HERE
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
    
    public static void main(String... args) throws Exception {
    	URLEncoder.encode("abc", "UTF-8");
    }

//    public static void main(String... args) {
//        QueryMap qm = new QueryMap();
//        qm.put(AuthRepConstants.USER_KEY, "123user-key123");
//        qm.put(AuthRepConstants.SERVICE_ID, Long.toString(1234456l));
//
//        System.out.println(DEFAULT_BACKEND + AUTHREP_PATH + qm.toQueryString());
//    }
}
