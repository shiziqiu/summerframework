package com.bkjk.platform.redis.cache;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import com.bkjk.platform.redis.CaffeineProperties;

public class RedissionSecondLevelCacheManager extends AbstractSecondLevelCacheManager {

    protected class RedissonCaffeineCache extends AbstractValueAdaptingCache {
        private String name;

        private RedissonClient redissonClient;

        private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache;

        private long defaultExpiration = 0;

        private Map<String, Long> expires;

        private String topic = DEFALUT_TOPIC;

        private Map<String, ReentrantLock> keyLockMap = new ConcurrentHashMap<>();

        public RedissonCaffeineCache(boolean allowNullValues) {
            super(allowNullValues);
        }

        public RedissonCaffeineCache(String name, RedissonClient redissonClient,
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache,
            CaffeineProperties caffeineProperties) {
            super(caffeineProperties.isCacheNullValues());
            this.name = name;
            this.redissonClient = redissonClient;
            this.caffeineCache = caffeineCache;
            this.defaultExpiration = caffeineProperties.getRemote().getGlobalExpiration();
            this.expires = caffeineProperties.getRemote().getExpires();
            this.topic = caffeineProperties.getRemote().getTopic();
        }

        @Override
        public void clear() {
            push(new RedisMessage(this.name, null));

            caffeineCache.invalidateAll();
        }

        public void clearLocal(Object key) {
            logger.debug("clear local cache, the key is : {}", key);
            if (key == null) {
                caffeineCache.invalidateAll();
            } else {
                caffeineCache.invalidate(key);
            }
        }

        @Override
        public void evict(Object key) {

            redissonClient.getBucket(getKey(key)).delete();

            push(new RedisMessage(this.name, key.toString()));

            caffeineCache.invalidate(key);
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            Object value = lookup(key);
            if (value != null) {
                return (T)value;
            }

            ReentrantLock lock = keyLockMap.get(key.toString());
            if (lock == null) {
                logger.debug("create tryLock for key : {}", key);
                lock = new ReentrantLock();
                keyLockMap.putIfAbsent(key.toString(), lock);
            }
            try {
                lock.lock();
                value = lookup(key);
                if (value != null) {
                    return (T)value;
                }
                value = valueLoader.call();
                Object storeValue = toStoreValue(value);
                put(key, storeValue);
                return (T)value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e.getCause());
            } finally {
                lock.unlock();
            }
        }

        private long getExpire() {
            long expire = defaultExpiration;
            Long cacheNameExpire = expires.get(this.name);
            return cacheNameExpire == null ? expire : cacheNameExpire.longValue();
        }

        private String getKey(Object key) {
            return this.name.concat(":").concat(key.toString());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        protected Object lookup(Object key) {
            String cacheKey = getKey(key);
            Object value = caffeineCache.getIfPresent(key);
            if (value != null) {
                logger.debug("get cache from caffeine, the key is : {}", cacheKey);
                return value;
            }
            value = redissonClient.getBucket(cacheKey).get();
            if (value != null) {
                logger.debug("get cache from redisson and put in caffeine, the key is : {}", cacheKey);
                caffeineCache.put(key, value);
            }
            return value;
        }

        private void push(RedisMessage message) {
            RTopic rtopic = redissonClient.getTopic(topic);
            rtopic.publish(message);
        }

        @Override
        public void put(Object key, Object value) {
            if (!super.isAllowNullValues() && value == null) {
                this.evict(key);
                return;
            }
            long expire = getExpire();
            if (expire > 0) {
                redissonClient.getBucket(getKey(key)).set(value, expire, TimeUnit.MILLISECONDS);
            } else {
                redissonClient.getBucket(getKey(key)).set(value);
            }

            push(new RedisMessage(this.name, key.toString()));

            caffeineCache.put(key, value);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            Object prevValue;
            synchronized (key) {
                prevValue = redissonClient.getBucket(getKey(key)).get();
                if (prevValue == null) {
                    long expire = getExpire();
                    if (expire > 0) {
                        redissonClient.getBucket(getKey(key)).set(toStoreValue(value), expire, TimeUnit.MILLISECONDS);
                    } else {
                        redissonClient.getBucket(getKey(key)).set(toStoreValue(value));
                    }

                    push(new RedisMessage(this.name, key.toString()));

                    caffeineCache.put(key, toStoreValue(value));
                }
            }
            return toValueWrapper(prevValue);
        }
    }

    private final RedissonClient redissonClient;

    public RedissionSecondLevelCacheManager(CaffeineProperties caffeineProperties, RedissonClient redissonClient) {
        super(caffeineProperties);
        this.redissonClient = redissonClient;
    }

    public void clearLocal(String cacheName, Object key) {
        Cache cache = CACHE_MAP.get(cacheName);
        if (cache == null) {
            return;
        }

        RedissonCaffeineCache redissonCaffeineCache = (RedissonCaffeineCache)cache;
        redissonCaffeineCache.clearLocal(key);
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = CACHE_MAP.get(name);
        if (cache != null) {
            return cache;
        }

        cache =
            new RedissonCaffeineCache(name, redissonClient, buildCaffeineCache(caffeineProperties), caffeineProperties);
        Cache oldCache = CACHE_MAP.putIfAbsent(name, cache);
        logger.debug("create cache instance, the cache name is : {}", name);
        return oldCache == null ? cache : oldCache;
    }

}
