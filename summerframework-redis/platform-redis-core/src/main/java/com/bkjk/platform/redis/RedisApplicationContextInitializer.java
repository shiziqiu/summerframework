package com.bkjk.platform.redis;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import com.bkjk.platform.redis.jedis.JedisPostProcessor;
import com.bkjk.platform.redis.jedis.MultiCacheCloudProperties;
import com.bkjk.platform.redis.redisson.MultiRedissonProperties;
import com.bkjk.platform.redis.redisson.RedissonPostProcessor;

public class RedisApplicationContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisApplicationContextInitializer.class);
    private static final String JEDIS_PATH = "redis.clients.jedis.Jedis";
    private static final String REDISSON_PATH = "org.redisson.api.RedissonClient";
    private static final String REDISSON_PROPERTIES_PREFIX = "platform.cache.remote.redisson";
    private static final String JEDIS_PROPERTIES_PREFIX = "platform.cache.remote.jedis";

    private boolean checkRedissonLockProperties(final MultiRedissonProperties redissonProperties) {
        if (redissonProperties != null && redissonProperties.getSource().size() != 0) {
            return true;
        }
        return false;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        MultiRedissonProperties redissonProperties = loadRedissonProperties(applicationContext);
        MultiCacheCloudProperties jedisProperties = loadJedisProperties(applicationContext);
        Environment env = applicationContext.getEnvironment();
        if (ClassUtils.isPresent(JEDIS_PATH, RedisApplicationContextInitializer.class.getClassLoader())) {
            boolean redissonLockExit = checkRedissonLockProperties(redissonProperties);
            new JedisPostProcessor(jedisProperties, !redissonLockExit, env)
                .process(applicationContext.getBeanFactory());
        }
        if (ClassUtils.isPresent(REDISSON_PATH, RedisApplicationContextInitializer.class.getClassLoader())) {
            try {
                new RedissonPostProcessor(redissonProperties, env).process(applicationContext.getBeanFactory());
            } catch (IOException e) {
                LOGGER.error("Initial Redisson error, e:{}", e);
                throw new RuntimeException(e);
            }
        }
    }

    private MultiCacheCloudProperties loadJedisProperties(ConfigurableApplicationContext applicationContext) {
        Binder binder = Binder.get(applicationContext.getEnvironment());
        try {
            MultiCacheCloudProperties properties =
                binder.bind(JEDIS_PROPERTIES_PREFIX, Bindable.of(MultiCacheCloudProperties.class)).get();
            return properties;
        } catch (Exception e) {
            LOGGER.debug("Platform do not load Jedis Properties correctly", e);
        }
        return null;
    }

    private MultiRedissonProperties loadRedissonProperties(ConfigurableApplicationContext applicationContext) {
        try {
            Binder binder = Binder.get(applicationContext.getEnvironment());
            MultiRedissonProperties properties =
                binder.bind(REDISSON_PROPERTIES_PREFIX, Bindable.of(MultiRedissonProperties.class)).get();
            return properties;
        } catch (Exception e) {
            LOGGER.debug("Platform do not load Redisson Properties correctly", e);
        }
        return null;
    }

}
