/*
 * Copyright 2017 JBoss Inc
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

package io.apiman.plugins.auth3scale.authrep.strategies.impl;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.threescale.beans.Auth3ScaleBean;
import io.apiman.plugins.auth3scale.authrep.PrincipalStrategyFactory;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.BatchedReportData;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.BatchedReporter;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReporterImpl;
import io.apiman.plugins.auth3scale.util.report.batchedreporter.ReporterOptions;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class BatchedStrategyFactory implements PrincipalStrategyFactory {
    // is this safe for mixing multiple different users? probably not. TODO verify it's fixed
    // Maybe caller should sort that out (seems better) -- might be OK with user ID figured in?
    private ReporterImpl<BatchedReportData> reporter;
    private StandardAuthCache standardCache = new StandardAuthCache();
    private BatchedAuthCache heuristicCache = new BatchedAuthCache();

    public BatchedStrategyFactory(BatchedReporter batchedReporter) {
        reporter = new ReporterImpl<>(new ReporterOptions());

        reporter.flushHandler(result -> {
            BatchedReportData entry = result.getResult().get(0);
            // Make cache entry in heuristic cache to force subsequent N entries to be blocking authrep
            // with the hope that the backend has caught up and we will catch the updated rate limiting status.
            heuristicCache.cache(entry.getConfig(), entry.getRequest(), entry.getKeyElems());
        });

        batchedReporter.addReporter(reporter);
    }

    @Override
    public BatchedAuth getAuthPrincipal(Auth3ScaleBean bean,
            ApiRequest request,
            IPolicyContext context) {
        return new BatchedAuth(bean, request, context, standardCache, heuristicCache);
    }

    @Override
    public BatchedRep getRepPrincipal(Auth3ScaleBean bean,
            ApiRequest request,
            ApiResponse response,
            IPolicyContext context) {
        return new BatchedRep(bean, request, response, context, reporter, standardCache, heuristicCache);
    }

}
