package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public interface AuthRepFactory {
    AbstractAuthExecutor<?> createAuth(ApiRequest request, IPolicyContext context);
    AbstractRepExecutor<?> createRep(ApiResponse response, ApiRequest request, IPolicyContext context);
    AbstractReporter<?> getReporter();
}
