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
package io.apiman.plugins.auth3scale.authrep.appid;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.threescale.beans.Auth3ScaleBean;
import io.apiman.plugins.auth3scale.authrep.RepPrincipal;
import io.apiman.plugins.auth3scale.authrep.strategies.AuthStrategy;
import io.apiman.plugins.auth3scale.authrep.strategies.RepStrategy;
import io.apiman.plugins.auth3scale.authrep.AuthPrincipal;
import io.apiman.plugins.auth3scale.authrep.AuthRepFactory;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class AppIdAuthRepFactory implements AuthRepFactory {

    @Override
    public AuthPrincipal createAuth(Auth3ScaleBean config, ApiRequest request, IPolicyContext context, AuthStrategy authStrategy) {
        return new AppIdAuth(config, request, context, authStrategy);
    }

    @Override
    public RepPrincipal createRep(Auth3ScaleBean config, ApiResponse response, ApiRequest request, IPolicyContext context, RepStrategy repStrategy) {
        return new AppIdRep(config, request, response, context, repStrategy);
    }
}
