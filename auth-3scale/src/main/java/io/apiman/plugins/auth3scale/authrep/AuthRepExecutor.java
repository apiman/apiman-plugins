package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReportData;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public abstract class AuthRepExecutor<T extends AbstractReporter<? extends ReportData>> {

    protected static final String DEFAULT_BACKEND = "http://su1.3scale.net:80";
    protected static final String AUTHORIZE_PATH = "/transactions/authorize.xml?";
    protected static final String REPORT_PATH = "/transactions.xml";
    protected static final String AUTHREP_PATH = "/transactions/authrep.xml?";
    
    protected final ApiRequest request;
    protected final ApiResponse response;
    protected final IHttpClientComponent httpClient;
    protected final IPolicyFailureFactoryComponent failureFactory;
    protected final ParameterMap paramMap;
    protected final Api api;
    
    protected IAsyncHandler<PolicyFailure> policyFailureHandler;
    protected IPolicyContext context;
        
    private AuthRepExecutor(ApiRequest request, ApiResponse response, Api api, IPolicyContext context) {
        this.request = request;
        this.response = response;
        this.api = api;
        this.httpClient = context.getComponent(IHttpClientComponent.class);
        this.failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
        this.context = context;
        this.paramMap = new ParameterMap();
    }
    
    public AuthRepExecutor(ApiRequest request, ApiResponse response, IPolicyContext context) {
        this(request, response, request.getApi(), context);
    }

    public AuthRepExecutor(ApiRequest request, IPolicyContext context) {
        this(request, null, request.getApi(), context);
    }

    public abstract AuthRepExecutor<T> auth(IAsyncResultHandler<Void> handler);
    public abstract AuthRepExecutor<T> rep();
    
    public AuthRepExecutor<T> setPolicyFailureHandler(IAsyncHandler<PolicyFailure> policyFailureHandler) {
        this.policyFailureHandler = policyFailureHandler;
        return this;
    }
    
    public abstract AuthRepExecutor<T> setReporter(T reporter);
}
