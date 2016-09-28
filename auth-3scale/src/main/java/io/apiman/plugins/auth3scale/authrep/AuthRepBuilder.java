package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.policy.IPolicyContext;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AuthRepBuilder { // factory, really?

//    private final ApiRequest request;
//    private final Api api;
//    private final IHttpClientComponent httpClient;
//    private final IPolicyFailureFactoryComponent failureFactory;
    private final AuthRepExecutor authRepExecutorDelegate;


    //private final UsageData usageData = new UsageData();
    //private final String userKey;
    //private final String serviceId;
    //private final String providerKey;
    //private

//    public AuthRepBuilder(String userKey, String serviceId, String providerKey) {
//        this.userKey = userKey;
//        this.serviceId = serviceId;
//        this.providerKey = providerKey;
//    }

    public AuthRepBuilder(ApiRequest request, IPolicyContext context) {
        // Let's imagine there's some switching code here for oauth2 vs user_key vs id+user_key
        authRepExecutorDelegate = new ApiKeyAuthExecutor(request, context);
    }
    
    public AuthRepBuilder(ApiResponse response, Api api, IPolicyContext context) {
        // Let's imagine there's some switching code here for oauth2 vs user_key vs id+user_key
        authRepExecutorDelegate = new ApiKeyAuthExecutor(response, api, context);
    }

    public void auth(IAsyncResultHandler<Void> handler) {
        authRepExecutorDelegate.auth(handler);
    }
    
    public void rep(IAsyncResultHandler<Void> handler) {
        authRepExecutorDelegate.rep(handler);
    }
    
    public void authrep(IAsyncResultHandler<Void> handler) {
        authRepExecutorDelegate.authrep(handler);
    }

    public void setPolicyFailureHandler(IAsyncHandler<PolicyFailure> pfh) {
        authRepExecutorDelegate.setPolicyFailureHandler(pfh);
    }
}
