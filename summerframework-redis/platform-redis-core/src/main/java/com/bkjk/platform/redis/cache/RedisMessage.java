package com.bkjk.platform.redis.cache;

import java.io.Serializable;

public class RedisMessage implements Serializable {
    private static final long serialVersionUID = 5987219310442078193L;

    private String cacheName;
    private String key;

    public RedisMessage() {
    }

    public RedisMessage(String cacheName, String key) {
        super();
        this.cacheName = cacheName;
        this.key = key;
    }

    public String getCacheName() {
        return cacheName;
    }

    public String getKey() {
        return key;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
