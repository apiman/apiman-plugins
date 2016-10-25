package io.apiman.plugins.auth3scale.authrep.apikey;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AuthRepFactory;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyFactory implements AuthRepFactory {
    private final ApiKeyAuthReporter reporter = new ApiKeyAuthReporter();

    public ApiKeyAuthExecutor createAuth(ApiRequest request, IPolicyContext context) {
        return new ApiKeyAuthExecutor(request, context);
    }

    public ApiKeyRepExecutor createRep(ApiResponse response, ApiRequest request, IPolicyContext context) {
        return new ApiKeyRepExecutor(response, request, context).setReporter(reporter);
    }

    @Override
    public AbstractReporter<?> getReporter() {
        return reporter;
    }
}
