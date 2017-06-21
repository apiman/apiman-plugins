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

package io.apiman.plugins.auth3scale.authrep.strategies;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Content;
import io.apiman.plugins.auth3scale.authrep.AbstractCachingAuthenticator;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.UncheckedExecutionException;

public class StandardAuthCache extends AbstractCachingAuthenticator<Boolean> {

    public String randomId = UUID.randomUUID().toString();

    @Override
    public boolean isAuthCached(Content config, ApiRequest req, Object... elems) {
        try {
            return lruCache.get(getCacheKey(config, req, elems), () -> false);
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    @Override
    public StandardAuthCache cache(Content config, ApiRequest req, Object... elems) {
        lruCache.put(getCacheKey(config, req, elems), true);
        return this;
    }

    @Override
    public StandardAuthCache invalidate(Content config, ApiRequest req, Object... elems) {
        lruCache.invalidate(getCacheKey(config, req, elems));
        return this;
    }

}
