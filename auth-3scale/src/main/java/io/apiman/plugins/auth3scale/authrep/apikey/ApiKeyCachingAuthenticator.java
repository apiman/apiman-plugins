package io.apiman.plugins.auth3scale.authrep.apikey;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.google.common.util.concurrent.UncheckedExecutionException;
import io.apiman.gateway.engine.beans.ApiRequest;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class ApiKeyCachingAuthenticator {
    private static final int CAPACITY = 10_000_000;

    private Cache<Integer, Boolean> lruCache = CacheBuilder.newBuilder()
            .initialCapacity(CAPACITY) // TODO sensible capacity?
            .expireAfterWrite(10, TimeUnit.MINUTES) // TODO async?
            .maximumSize(CAPACITY) // LRU capacity
//            .concurrencyLevel(4) TODO
            .build();

    public boolean isAuthCached(ApiRequest serviceRequest, String apiKey) {
        try {
            return lruCache.get(getCacheKey(serviceRequest.getApiId(), apiKey,
                    hashArray(serviceRequest)), () -> false); // TODO cache routematcher result into request?
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    public ApiKeyCachingAuthenticator cache(ApiRequest serviceRequest, String apiKey) {
        lruCache.put(getCacheKey(serviceRequest.getApiId(), apiKey,
                hashArray(serviceRequest)), true);
        return this;
    }

    public ApiKeyCachingAuthenticator invalidate(ApiRequest serviceRequest, String apiKey) { // TODO invalidate will be with what apikey..?
        lruCache.invalidate(getCacheKey(serviceRequest.getApiId(), apiKey,
                hashArray(serviceRequest))); // TODO optmise
        return this;
    }

    private int getCacheKey(Object... objects) {
        return Objects.hash(objects);
    }

    private int hashArray(ApiRequest req) {
        return Arrays.hashCode(req.getApi().getRouteMatcher().match(req.getDestination()));
    }
}
