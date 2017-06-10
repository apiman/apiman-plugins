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
package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Content;
import io.apiman.plugins.auth3scale.authrep.apikey.ApiKeyAuthReporter;
import io.apiman.plugins.auth3scale.util.ParameterMap;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.AbstractReporter;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReportData;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 * @param <T> the type
 */
public abstract class AbstractRepExecutor<T extends AbstractReporter<? extends ReportData>> extends AbstractAuthRepBase {
    private ApiResponse response;

    private AbstractRepExecutor(Content config, ApiRequest request, ApiResponse response, IPolicyContext context,
            ApiKeyAuthReporter reporter) {
        super(config, request);
        this.response = response;
    }

    public AbstractRepExecutor(Content config, ApiRequest request, ApiResponse response, IPolicyContext context,
            ApiKeyAuthReporter reporter, CachingAuthenticator authCache) {
        this(config, request, response, context, reporter);
    }

    public abstract AbstractRepExecutor<T> rep();

    protected ParameterMap buildLog() {
        return new ParameterMap().add("code", (long) response.getCode()); //$NON-NLS-1$
    }

    @Override
    protected ParameterMap setIfNotNull(ParameterMap in, String k, String v) {
        if (v == null)
            return in;

        in.add(k, v);
        return in;
    }
}
