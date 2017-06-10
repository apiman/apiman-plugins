/*
 * Copyright 2016 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2016 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.plugins.auth3scale;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.ProxyConfigRoot;
import io.apiman.plugins.auth3scale.authrep.AuthRep;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.BatchedReporter;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
@SuppressWarnings("nls")
public class Auth3Scale extends AbstractMappedPolicy<ProxyConfigRoot> {
    private static final String AUTH3SCALE_REQUEST = "Auth3Scale.Req";
    private static final BatchedReporter BATCHED_REPORTER = new BatchedReporter();
    private static final AuthRep authRepFactory = new AuthRep(BATCHED_REPORTER);

    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, ProxyConfigRoot config, IPolicyChain<ApiRequest> chain) {
        authRepFactory.getAuth(config.getProxyConfig().getContent(), request, context)
                // If a policy failure occurs, call chain.doFailure.
                .policyFailureHandler(chain::doFailure)
                // If succeeded or error.
                .auth(result -> {
                    if (result.isSuccess()) {
                        // Keep the API request around so the auth apikey(s) can be accessed, etc.
                        context.setAttribute(AUTH3SCALE_REQUEST, request);
                        chain.doApply(request);
                    } else {
                        chain.throwError(result.getError());
                    }
                });
    }

    @Override
    protected void doApply(ApiResponse response, IPolicyContext context, ProxyConfigRoot config, IPolicyChain<ApiResponse> chain) {
        // Just let it go ahead, and report stuff at our leisure.
        chain.doApply(response);

        ApiRequest request = context.getAttribute(AUTH3SCALE_REQUEST, null);
        authRepFactory.getRep(config.getProxyConfig().getContent(), response, request, context)
            .rep();
    }

    @Override
    protected Class<ProxyConfigRoot> getConfigurationClass() {
        return ProxyConfigRoot.class;
    }
}
