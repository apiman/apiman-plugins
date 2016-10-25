package io.apiman.plugins.auth3scale.authrep.appid;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AuthRepFactory;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AppIdFactory implements AuthRepFactory {
    private AppIdAuthReporter reporter = new AppIdAuthReporter();

    @Override
    public AppIdAuthExecutor createAuth(ApiRequest request, IPolicyContext context) {
        return new AppIdAuthExecutor(request, context);
    }

    @Override
    public AppIdRepExecutor createRep(ApiResponse response, ApiRequest request, IPolicyContext context) {
        return new AppIdRepExecutor(response, request, context).setReporter(reporter);
    }

    @Override
    public AbstractReporter<?> getReporter() {
        return reporter;
    }
}
