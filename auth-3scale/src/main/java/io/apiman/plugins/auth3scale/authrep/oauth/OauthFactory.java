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
package io.apiman.plugins.auth3scale.authrep.oauth;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Content;
import io.apiman.plugins.auth3scale.authrep.AuthRepFactory;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class OauthFactory implements AuthRepFactory {
    private OauthAuthReporter reporter = new OauthAuthReporter();

    @Override
    public OauthAuthExecutor createAuth(Content config, ApiRequest request, IPolicyContext context) {
        return new OauthAuthExecutor(config, request, context);
    }

    @Override
    public OauthRepExecutor createRep(Content config, ApiResponse response, ApiRequest request, IPolicyContext context) {
        return new OauthRepExecutor(config, response, request, context).setReporter(reporter);
    }

    @Override
    public AbstractReporter<?> getReporter() {
        return reporter;
    }
}
