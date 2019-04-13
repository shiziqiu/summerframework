package com.bkjk.platform.redis;

import static com.bkjk.platform.redis.cache.AbstractSecondLevelCacheManager.DEFALUT_TOPIC;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.cache")
public class CaffeineProperties {

    public class Caffeine {

        private long expireAfterAccess = 60_000;

        private long expireAfterWrite = 60_000;

        private long refreshAfterWrite = 60_000;

        private int initialCapacity = 100;

        private long maxSize;

        public long getExpireAfterAccess() {
            return expireAfterAccess;
        }

        public long getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public int getInitialCapacity() {
            return initialCapacity;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public long getRefreshAfterWrite() {
            return refreshAfterWrite;
        }

        public void setExpireAfterAccess(long expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }

        public void setExpireAfterWrite(long expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public void setInitialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public void setRefreshAfterWrite(long refreshAfterWrite) {
            this.refreshAfterWrite = refreshAfterWrite;
        }
    }

    public class Remote {

        private long globalExpiration = 6000000;

        private Map<String, Long> expires = new HashMap<>();

        private String topic = DEFALUT_TOPIC;

        public Map<String, Long> getExpires() {
            return expires;
        }

        public long getGlobalExpiration() {
            return globalExpiration;
        }

        public String getTopic() {
            return topic;
        }

        public void setExpires(Map<String, Long> expires) {
            this.expires = expires;
        }

        public void setGlobalExpiration(long globalExpiration) {
            this.globalExpiration = globalExpiration;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

    }

    private Set<String> cacheNames = new HashSet<>();

    private boolean cacheNullValues = true;

    private Remote remote = new Remote();

    private Caffeine local = new Caffeine();

    public Set<String> getCacheNames() {
        return cacheNames;
    }

    public Caffeine getLocal() {
        return local;
    }

    public Remote getRemote() {
        return remote;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public void setCacheNames(Set<String> cacheNames) {
        this.cacheNames = cacheNames;
    }

    public void setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
    }

    public void setLocal(Caffeine local) {
        this.local = local;
    }

    public void setRemote(Remote remote) {
        this.remote = remote;
    }
}
