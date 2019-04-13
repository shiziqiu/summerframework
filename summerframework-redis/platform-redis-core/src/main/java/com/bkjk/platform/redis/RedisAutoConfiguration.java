package com.bkjk.platform.redis;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.bkjk.platform.redis.cache.JedisSecondLevelCacheManager;
import com.bkjk.platform.redis.cache.RedisMessage;
import com.bkjk.platform.redis.cache.RedissionSecondLevelCacheManager;
import com.bkjk.platform.redis.cache.RedissonMessageListenerContainer;
import com.github.benmanes.caffeine.cache.Cache;

import redis.clients.jedis.Jedis;

@Configuration
@EnableConfigurationProperties(CaffeineProperties.class)
public class RedisAutoConfiguration {

    @Configuration
    @ConditionalOnClass({Cache.class, Jedis.class})
    @ConditionalOnBean(RedisTemplate.class)
    @EnableConfigurationProperties(CaffeineProperties.class)
    protected static class JedisCaffeineAutoConfiguration {
        @Autowired
        private CaffeineProperties caffeineProperties;

        @Bean("cacheManager")
        @ConditionalOnClass(Jedis.class)
        public JedisSecondLevelCacheManager jedisCacheManager(RedisTemplate redisTemplate) {
            return new JedisSecondLevelCacheManager(caffeineProperties, redisTemplate);
        }

        @Bean
        @ConditionalOnClass(Jedis.class)
        public RedisMessageListenerContainer redisMessageListenerContainer(
            JedisSecondLevelCacheManager jedisSencondLevelCacheManager, RedisTemplate redisTemplate) {
            RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
            redisMessageListenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
            redisMessageListenerContainer.addMessageListener((message, pattern) -> {
                RedisMessage cacheMessage =
                    (RedisMessage)redisTemplate.getValueSerializer().deserialize(message.getBody());
                LOGGER.debug("recevice a jedis topic message, clear local cache, the cacheName is {}, the key is {}",
                    cacheMessage.getCacheName(), cacheMessage.getKey());
                jedisSencondLevelCacheManager.clearLocal(cacheMessage.getCacheName(), cacheMessage.getKey());
            }, new ChannelTopic(caffeineProperties.getRemote().getTopic()));
            return redisMessageListenerContainer;
        }
    }

    @Configuration
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnClass({Cache.class, RedissonClient.class})
    protected static class RedissonCaffeineAutoConfiguration {

        @Autowired
        private CaffeineProperties caffeineProperties;

        @Bean("cacheManager")
        @ConditionalOnClass(RedissonClient.class)
        public RedissionSecondLevelCacheManager redissonCacheManager(RedissonClient redissonClient) {
            return new RedissionSecondLevelCacheManager(caffeineProperties, redissonClient);
        }

        @Bean
        @ConditionalOnClass(RedissonClient.class)
        public RedissonMessageListenerContainer redissonMessageListenerContainer(
            RedissionSecondLevelCacheManager cacheManager, RedissonClient redissonClient) {
            RedissonMessageListenerContainer container = new RedissonMessageListenerContainer();
            container.setRedissonClient(redissonClient);
            container.addMessageListener(message -> {
                LOGGER.debug("recevice a redisson topic message, clear local cache, the cacheName" + " is {}, the "
                    + "key is {}", message.getCacheName(), message.getKey());
                cacheManager.clearLocal(message.getCacheName(), message.getKey());
            }, caffeineProperties.getRemote().getTopic());

            return container;
        }
    }

    protected static final Logger LOGGER = LoggerFactory.getLogger(RedisAutoConfiguration.class);
}
