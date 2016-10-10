package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.executors.ApiKeyAuthExecutor;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AuthRepBuilder { // factory, really?

    private final AuthRepExecutor authRepExecutorDelegate;
    
    public AuthRepBuilder(ApiRequest request, IPolicyContext context) {
        // Let's imagine there's some switching code here for oauth2 vs user_key vs id+user_key
        authRepExecutorDelegate = new ApiKeyAuthExecutor(request, context);
    }
    
    public AuthRepBuilder(ApiResponse response, ApiRequest request, IPolicyContext context) {
        // Let's imagine there's some switching code here for oauth2 vs user_key vs id+user_key
        authRepExecutorDelegate = new ApiKeyAuthExecutor(response, request, context);
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
