package com.bkjk.platform.redis.cache;

public interface RedisMessageListener {

    void onMessage(RedisMessage message);
}
