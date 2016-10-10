/*
 * Copyright 2015 JBoss Inc
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
import io.apiman.plugins.auth3scale.authrep.AuthRepBuilder;
import io.apiman.plugins.auth3scale.beans.Auth3ScaleBean;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class Auth3Scale extends AbstractMappedPolicy<Auth3ScaleBean> {

    protected Class<Auth3ScaleBean> getConfigurationClass() {
        return Auth3ScaleBean.class;
    }
    
    private static final String AUTH3SCALE_REQUEST = Auth3Scale.class.getCanonicalName() + "-REQ";
    
    //private static final 

    protected void doApply(ApiRequest request, IPolicyContext context, Auth3ScaleBean config, IPolicyChain<ApiRequest> chain) {
        // Get HTTP Client TODO compare perf with singleton
        // TODO take this from services.backend.endpoint
        AuthRepBuilder auth = new AuthRepBuilder(request, context);

        // If a policy failure occurs, call chain.doFailure
        auth.setPolicyFailureHandler(chain::doFailure);

        // If succeeds, or exception.
        auth.auth(result -> {
            if (result.isSuccess()) {
            	// Keep the API request around so the auth key(s) can be accessed, etc.
            	context.setAttribute(AUTH3SCALE_REQUEST, request);
                chain.doApply(request);
            } else {
                chain.throwError(result.getError()); // TODO review whether all these cases are appropriate or should use PolicyFailure (e.g. no key provided).
            }
        });
    }

    protected void doApply(ApiResponse response, IPolicyContext context, Auth3ScaleBean config, IPolicyChain<ApiResponse> chain) {
    	// Just let it go ahead, and report stuff at our leisure.
    	chain.doApply(response);
    	
    	ApiRequest request = context.getAttribute(AUTH3SCALE_REQUEST, null);
        AuthRepBuilder rep = new AuthRepBuilder(response, request, context);
        
        rep.setPolicyFailureHandler(chain::doFailure);
        
        rep.rep(result -> {
            if (result.isSuccess()) {
                
            } else {
                //chain.throwError(result.getError()); // TODO review - likely can't do anything useful at this point!
            	//retry?
            	
            }
        });
    }
}
