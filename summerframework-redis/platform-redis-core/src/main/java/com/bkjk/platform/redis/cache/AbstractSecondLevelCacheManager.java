package com.bkjk.platform.redis.cache;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.bkjk.platform.redis.CaffeineProperties;
import com.github.benmanes.caffeine.cache.Caffeine;

public abstract class AbstractSecondLevelCacheManager implements CacheManager {

    public static final String DEFALUT_TOPIC = "redis:caffeine:topic";

    protected static final ConcurrentMap<String, Cache> CACHE_MAP = new ConcurrentHashMap<String, Cache>();

    protected final Logger logger = LoggerFactory.getLogger(AbstractSecondLevelCacheManager.class);

    protected final CaffeineProperties caffeineProperties;

    public AbstractSecondLevelCacheManager(CaffeineProperties caffeineProperties) {
        super();
        this.caffeineProperties = caffeineProperties;
    }

    protected com.github.benmanes.caffeine.cache.Cache<Object, Object>
        buildCaffeineCache(CaffeineProperties caffeineProperties) {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
        if (caffeineProperties.getLocal().getExpireAfterAccess() > 0) {
            cacheBuilder.expireAfterAccess(caffeineProperties.getLocal().getExpireAfterAccess(), TimeUnit.MILLISECONDS);
        }
        if (caffeineProperties.getLocal().getExpireAfterWrite() > 0) {
            cacheBuilder.expireAfterWrite(caffeineProperties.getLocal().getExpireAfterWrite(), TimeUnit.MILLISECONDS);
        }
        if (caffeineProperties.getLocal().getInitialCapacity() > 0) {
            cacheBuilder.initialCapacity(caffeineProperties.getLocal().getInitialCapacity());
        }
        if (caffeineProperties.getLocal().getMaxSize() > 0) {
            cacheBuilder.maximumSize(caffeineProperties.getLocal().getMaxSize());
        }
        // if (caffeineProperties.getLocal().getRefreshAfterWrite() > 0) {
        // cacheBuilder.refreshAfterWrite(caffeineProperties.getLocal().getRefreshAfterWrite(), TimeUnit.MILLISECONDS);
        // }
        return cacheBuilder.build();
    }

    @Override
    public Collection<String> getCacheNames() {
        return caffeineProperties.getCacheNames();
    }
}
