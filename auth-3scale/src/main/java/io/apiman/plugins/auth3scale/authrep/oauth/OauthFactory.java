package io.apiman.plugins.auth3scale.authrep.oauth;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.auth3scale.authrep.AuthRepFactory;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class OauthFactory implements AuthRepFactory {
    private OauthAuthReporter reporter = new OauthAuthReporter();

    @Override
    public OauthAuthExecutor createAuth(ApiRequest request, IPolicyContext context) {
        return new OauthAuthExecutor(request, context);
    }

    @Override
    public OauthRepExecutor createRep(ApiResponse response, ApiRequest request, IPolicyContext context) {
        return new OauthRepExecutor(response, request, context).setReporter(reporter);
    }

    @Override
    public AbstractReporter<?> getReporter() {
        return reporter;
    }
}
