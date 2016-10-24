package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPeriodicComponent;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.apikey.ApiKeyAuthExecutor;
import io.apiman.plugins.auth3scale.authrep.apikey.ApiKeyAuthReporter;
import io.apiman.plugins.auth3scale.authrep.apikey.ApiKeyRepExecutor;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.BatchedReporter;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AuthRepFactory { 
    
    private ApiKeyAuthReporter apiKeyAuthReporter;
    private BatchedReporter batchedReporter;
    private volatile boolean reporterInitialised = false;

    public AuthRepFactory(BatchedReporter batchedReporter) {
        this.batchedReporter = batchedReporter;
        apiKeyAuthReporter = new ApiKeyAuthReporter();
        batchedReporter.addReporter(apiKeyAuthReporter);        
    }
    
    public AbstractAuthExecutor<?> createAuth(ApiRequest request, IPolicyContext context) {
        return new ApiKeyAuthExecutor(request, context);
    }

    public AbstractRepExecutor<?> createRep(ApiResponse response, ApiRequest request, IPolicyContext context) {
        safeInitialise(context);
        return new ApiKeyRepExecutor(response, request, context).setReporter(apiKeyAuthReporter);
    }
    
    private void safeInitialise(IPolicyContext context) {
        if (!reporterInitialised) {
            synchronized (this) {
                if (!reporterInitialised) {
                    System.out.println("Initialising reporter...");
                    batchedReporter.start(context.getComponent(IPeriodicComponent.class), context.getComponent(IHttpClientComponent.class));
                    reporterInitialised = true;
                }
            }
        }
    }
}
