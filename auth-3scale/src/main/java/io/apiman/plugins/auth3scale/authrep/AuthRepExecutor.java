package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.util.QueryMap;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.policy.IPolicyContext;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public abstract class AuthRepExecutor {

    protected static final String DEFAULT_BACKEND = "https://su1.3scale.net:443";
    protected final ApiRequest request;
    protected final IHttpClientComponent httpClient;
    protected final IPolicyFailureFactoryComponent failureFactory;
    protected final QueryMap queryMap;
    protected final Api api;
    protected IAsyncHandler<PolicyFailure> policyFailureHandler;
	private final ApiResponse response;
    
    public AuthRepExecutor(ApiResponse response, Api api, IPolicyContext context) {
        this.response = response;
        this.request = null;
        this.api = api;
        this.httpClient = context.getComponent(IHttpClientComponent.class);
        this.failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
        this.queryMap = new QueryMap();
    }

    public AuthRepExecutor(ApiRequest request, IPolicyContext context) {
        this.request = request;
        this.response = null;
        this.api = request.getApi();
        this.httpClient = context.getComponent(IHttpClientComponent.class);
        this.failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
        this.queryMap = new QueryMap();
    }

    public abstract void auth(IAsyncResultHandler<Void> handler);
    
    public abstract void rep(IAsyncResultHandler<Void> handler);
    
    public abstract void authrep(IAsyncResultHandler<Void> handler);

    public void setPolicyFailureHandler(IAsyncHandler<PolicyFailure> policyFailureHandler) {
        this.policyFailureHandler = policyFailureHandler;
    }
}
