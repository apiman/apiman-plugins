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
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Auth3ScaleBean;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
@SuppressWarnings("nls")
public class Auth3Scale extends AbstractMappedPolicy<Auth3ScaleBean> {
    private static final String AUTH3SCALE_REQUEST = "Auth3Scale.Req";
    private AuthRep auth3scale;
    private volatile boolean init = false;

    private void init(IPolicyContext context) {
        if (!init) {
            synchronized (this) {
                auth3scale = new AuthRep(context);
                init = true;
            }
        }
    }

    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, Auth3ScaleBean config, IPolicyChain<ApiRequest> chain) {
        init(context);

        try {
            auth3scale.getAuth(config, request, context)
                // If a policy failure occurs, call chain.doFailure.
                .policyFailureHandler(chain::doFailure)
                // If succeeded or error.
                .auth(result -> {
                    if (result.isSuccess()) {
                        // Keep the API request around so the auth apikey(s) can be accessed, etc.
                        context.setAttribute(AUTH3SCALE_REQUEST, request);
                        chain.doApply(request);
                    } else {
                        result.getError().printStackTrace();
                        chain.throwError(result.getError());
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doApply(ApiResponse response, IPolicyContext context, Auth3ScaleBean config, IPolicyChain<ApiResponse> chain) {
        try {
        // Just let it go ahead, and report stuff at our leisure.
        chain.doApply(response);
        ApiRequest request = context.getAttribute(AUTH3SCALE_REQUEST, null);
        auth3scale.getRep(config, response, request, context).rep();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<Auth3ScaleBean> getConfigurationClass() {
        return Auth3ScaleBean.class;
    }
}
