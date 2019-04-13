package com.bkjk.platform.redis.cache;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;

import com.bkjk.platform.redis.CaffeineProperties;

public class JedisSecondLevelCacheManager extends AbstractSecondLevelCacheManager {

    protected class JedisCaffeineCache extends AbstractValueAdaptingCache {
        private final Logger logger = LoggerFactory.getLogger(JedisCaffeineCache.class);

        private String name;

        private RedisTemplate<Object, Object> stringKeyRedisTemplate;

        private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache;

        private long defaultExpiration = 0;

        private Map<String, Long> expires;

        private String topic = DEFALUT_TOPIC;

        private Map<String, ReentrantLock> keyLockMap = new ConcurrentHashMap<>();

        public JedisCaffeineCache(boolean allowNullValues) {
            super(allowNullValues);
        }

        public JedisCaffeineCache(String name, RedisTemplate<Object, Object> stringKeyRedisTemplate,
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache,
            CaffeineProperties caffeineProperties) {
            super(caffeineProperties.isCacheNullValues());
            this.name = name;
            this.stringKeyRedisTemplate = stringKeyRedisTemplate;
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

            stringKeyRedisTemplate.delete(getKey(key));

            push(new RedisMessage(this.name, key.toString()));

            caffeineCache.invalidate(key);
        }

        @SuppressWarnings("unchecked")
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

        private Object getKey(Object key) {
            return this.name.concat(":").concat(key.toString());
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        protected Object lookup(Object key) {
            Object cacheKey = getKey(key);
            Object value = caffeineCache.getIfPresent(key);
            if (value != null) {
                logger.debug("get cache from caffeine, the key is : {}", cacheKey);
                return value;
            }

            value = stringKeyRedisTemplate.opsForValue().get(cacheKey);

            if (value != null) {
                logger.debug("get cache from Jedis and put in caffeine, the key is : {}", cacheKey);
                caffeineCache.put(key, value);
            }
            return value;
        }

        private void push(RedisMessage message) {
            stringKeyRedisTemplate.convertAndSend(topic, message);
        }

        @Override
        public void put(Object key, Object value) {
            if (!super.isAllowNullValues() && value == null) {
                this.evict(key);
                return;
            }
            long expire = getExpire();
            if (expire > 0) {
                stringKeyRedisTemplate.opsForValue().set(getKey(key), toStoreValue(value), expire,
                    TimeUnit.MILLISECONDS);
            } else {
                stringKeyRedisTemplate.opsForValue().set(getKey(key), toStoreValue(value));
            }

            push(new RedisMessage(this.name, key.toString()));

            caffeineCache.put(key, value);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            Object cacheKey = getKey(key);
            Object prevValue = null;
            synchronized (key) {
                prevValue = stringKeyRedisTemplate.opsForValue().get(cacheKey);
                if (prevValue == null) {
                    long expire = getExpire();
                    if (expire > 0) {
                        stringKeyRedisTemplate.opsForValue().set(getKey(key), toStoreValue(value), expire,
                            TimeUnit.MILLISECONDS);
                    } else {
                        stringKeyRedisTemplate.opsForValue().set(getKey(key), toStoreValue(value));
                    }

                    push(new RedisMessage(this.name, key.toString()));

                    caffeineCache.put(key, toStoreValue(value));
                }
            }
            return toValueWrapper(prevValue);
        }

    }

    private final RedisTemplate<Object, Object> redisTemplate;

    public JedisSecondLevelCacheManager(CaffeineProperties caffeineProperties,
        RedisTemplate<Object, Object> redisTemplate) {
        super(caffeineProperties);
        this.redisTemplate = redisTemplate;
    }

    public void clearLocal(String cacheName, Object key) {
        Cache cache = CACHE_MAP.get(cacheName);
        if (cache == null) {
            return;
        }
        JedisCaffeineCache jedisCaffeineCache = (JedisCaffeineCache)cache;
        jedisCaffeineCache.clearLocal(key);
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = CACHE_MAP.get(name);
        if (cache != null) {
            return cache;
        }
        cache = new JedisCaffeineCache(name, redisTemplate, buildCaffeineCache(caffeineProperties), caffeineProperties);
        Cache oldCache = CACHE_MAP.putIfAbsent(name, cache);
        logger.debug("create cache instance, the cache name is : {}", name);
        return oldCache == null ? cache : oldCache;
    }

}
