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

package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.vertx.polling.fetchers.threescale.beans.Content;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class CachingAuthenticator {
    private static final int CAPACITY = 1_000_000;

    protected Cache<Integer, Boolean> lruCache = CacheBuilder.newBuilder()
            .initialCapacity(CAPACITY) // TODO sensible capacity?
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(CAPACITY) // LRU capacity
//            .concurrencyLevel(4) TODO
            .build();


    protected int getCacheKey(Object... objects) {
        return Objects.hash(objects);
    }

    protected int hashArray(Content config, ApiRequest req) {
        return Arrays.hashCode(config.getProxy().getRouteMatcher().match(req.getDestination()));
    }
}
