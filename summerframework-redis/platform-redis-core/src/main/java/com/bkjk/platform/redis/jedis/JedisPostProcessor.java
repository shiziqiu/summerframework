package com.bkjk.platform.redis.jedis;

import static com.bkjk.platform.redis.DistributedLock.DISTRIBUTED_LOCK_NAME;
import static com.bkjk.platform.redis.jedis.SerializerUtil.getDefaultSerializer;
import static com.bkjk.platform.redis.report.CacheCloudInfoHttpHelper.getClusterNodeFromCacheCloud;
import static com.bkjk.platform.redis.report.CacheCloudInfoHttpHelper.getSentinelConfigurationFromCacheCloud;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import redis.clients.jedis.JedisPoolConfig;

public class JedisPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JedisPostProcessor.class);
    private static final String REDIS_TEMPLATE_NAME = "redisTemplate";
    private static final String STRING_REDIS_TEMPLATE_NAME = "stringRedisTemplate";
    private static final String REDIS_CONNECTION_FACTORY_NAME = "redisConnectionFactory";

    private final MultiCacheCloudProperties cacheCloudProperties;
    private final boolean initJedisLock;
    private final Environment env;

    public JedisPostProcessor(MultiCacheCloudProperties cacheCloudProperties, boolean initJedisLock, Environment env) {
        this.initJedisLock = initJedisLock;
        this.cacheCloudProperties = cacheCloudProperties;
        this.env = env;
    }

    private JedisConnectionFactory createJedisConnectionFactory(CacheCloudProperties cacheCloudProperties) {
        Long appId = cacheCloudProperties.getAppId();
        String url = cacheCloudProperties.getReportUrl();
        CacheCloudProperties.Type type = cacheCloudProperties.getType();
        if (appId == null) {
            return createOriginalJedisConnectionFactory(cacheCloudProperties);
        }

        if (StringUtils.isEmpty(url) || type == null) {
            throw new RuntimeException(
                "Missing params, " + ", appId: " + appId + ", url: " + "" + url + ", type: " + type);
        }

        if (CacheCloudProperties.Type.CLUSTER == type) {
            List<String> nodes = getClusterNodeFromCacheCloud(appId, url);
            return new JedisConnectionFactory(new RedisClusterConfiguration(nodes),
                jedisPoolConfig(cacheCloudProperties));
        } else if (CacheCloudProperties.Type.SENTINEL == type) {
            RedisSentinelConfiguration redisSentinelConfiguration = getSentinelConfigurationFromCacheCloud(
                cacheCloudProperties.getAppId(), cacheCloudProperties.getReportUrl());
            return new JedisConnectionFactory(redisSentinelConfiguration, jedisPoolConfig(cacheCloudProperties));
        } else {
            return null;
        }
    }

    private JedisConnectionFactory
        createOriginalJedisConnectionFactory(final CacheCloudProperties cacheCloudProperties) {
        RedisProperties.Sentinel sentinel = cacheCloudProperties.getSentinel();
        RedisProperties.Cluster cluster = cacheCloudProperties.getCluster();
        if (sentinel != null) {
            return new JedisConnectionFactory(
                new RedisSentinelConfiguration(sentinel.getMaster(), new HashSet<>(sentinel.getNodes())));
        }
        if (cluster != null) {
            return new JedisConnectionFactory(new RedisClusterConfiguration(cluster.getNodes()));
        }

        String host = cacheCloudProperties.getHost();
        Integer port = cacheCloudProperties.getPort();
        RedisProperties.Pool pool = cacheCloudProperties.getJedis().getPool();
        JedisConnectionFactory factory = new JedisConnectionFactory(new RedisStandaloneConfiguration(host, port));
        if (pool != null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMinIdle(pool.getMinIdle());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());
            factory.setPoolConfig(poolConfig);
        }
        return factory;
    }

    private void createJedisDistributedLockBean(ConfigurableListableBeanFactory beanFactory) {
        RedisTemplate redisTemplate = beanFactory.getBean(REDIS_TEMPLATE_NAME, RedisTemplate.class);
        JedisDistributedLock lock = new JedisDistributedLock(redisTemplate, env);
        beanFactory.registerSingleton(DISTRIBUTED_LOCK_NAME, lock);
        beanFactory.applyBeanPostProcessorsAfterInitialization(lock, DISTRIBUTED_LOCK_NAME);
    }

    private void createProxyHandler(RedisTemplate redisTemplate, Class clazz, String name) {
        try {
            Class cl = RedisTemplate.class;
            Field field = cl.getDeclaredField(name);
            field.setAccessible(true);
            Object object = field.get(redisTemplate);
            TemplateOpsProxyHandler handler = new TemplateOpsProxyHandler(object);
            Object proxy = Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] {clazz}, handler);
            field.set(redisTemplate, proxy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createProxyHandlers(RedisTemplate redisTemplate) {
        createProxyHandler(redisTemplate, ValueOperations.class, "valueOps");
        createProxyHandler(redisTemplate, ListOperations.class, "listOps");
        createProxyHandler(redisTemplate, SetOperations.class, "setOps");
        createProxyHandler(redisTemplate, ZSetOperations.class, "zSetOps");
        createProxyHandler(redisTemplate, GeoOperations.class, "geoOps");
        createProxyHandler(redisTemplate, HyperLogLogOperations.class, "hllOps");
    }

    private RedisTemplate createRedisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(getDefaultSerializer());
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        redisTemplate.opsForValue();
        redisTemplate.opsForList();
        redisTemplate.opsForSet();
        redisTemplate.opsForZSet();
        redisTemplate.opsForGeo();
        redisTemplate.opsForHash();
        redisTemplate.opsForHyperLogLog();
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    private StringRedisTemplate createStringRedisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(jedisConnectionFactory);
        stringRedisTemplate.opsForValue();
        stringRedisTemplate.opsForList();
        stringRedisTemplate.opsForSet();
        stringRedisTemplate.opsForZSet();
        stringRedisTemplate.opsForGeo();
        stringRedisTemplate.opsForHash();
        stringRedisTemplate.opsForHyperLogLog();
        stringRedisTemplate.afterPropertiesSet();
        return stringRedisTemplate;
    }

    private JedisPoolConfig jedisPoolConfig(CacheCloudProperties cacheCloudProperties) {
        JedisPoolConfig config = new JedisPoolConfig();
        if (cacheCloudProperties.getJedis().getPool() == null) {
            return config;
        }
        RedisProperties.Pool props = cacheCloudProperties.getJedis().getPool();
        config.setMaxTotal(props.getMaxActive());
        config.setMaxIdle(props.getMaxIdle());
        config.setMinIdle(props.getMinIdle());
        config.setMaxWaitMillis(props.getMaxWait().toMillis());
        return config;
    }

    public void process(ConfigurableListableBeanFactory beanFactory) throws RuntimeException {
        if (cacheCloudProperties == null || cacheCloudProperties.getSource() == null
            || cacheCloudProperties.getSource().size() == 0) {
            return;
        }
        List<CacheCloudProperties> list = cacheCloudProperties.getSource();
        for (int index = 0; index < list.size(); index++) {
            JedisConnectionFactory jedisConnectionFactory = createJedisConnectionFactory(list.get(index));
            jedisConnectionFactory.afterPropertiesSet();
            RedisTemplate<String, Object> redisTemplate = createRedisTemplate(jedisConnectionFactory);
            createProxyHandlers(redisTemplate);
            String qualifier;
            String redisConnectionFactoryQualifier;
            if (index == 0) {
                qualifier = REDIS_TEMPLATE_NAME;
                redisConnectionFactoryQualifier = REDIS_CONNECTION_FACTORY_NAME;
            } else {
                qualifier = REDIS_TEMPLATE_NAME + index;
                redisConnectionFactoryQualifier = REDIS_CONNECTION_FACTORY_NAME + index;
            }
            beanFactory.registerSingleton(qualifier, redisTemplate);
            beanFactory.applyBeanPostProcessorsAfterInitialization(redisTemplate, qualifier);
            LOGGER.info("register redisTemplate bean {}.", qualifier);
            RedisConnectionFactory redisConnectionFactory = redisTemplate.getConnectionFactory();
            beanFactory.registerSingleton(redisConnectionFactoryQualifier, redisConnectionFactory);
            beanFactory.applyBeanPostProcessorsAfterInitialization(redisConnectionFactory,
                redisConnectionFactoryQualifier);
            LOGGER.info("register redisConnectionFactory bean {}.", redisConnectionFactoryQualifier);

            StringRedisTemplate stringRedisTemplate = createStringRedisTemplate(jedisConnectionFactory);
            createProxyHandlers(stringRedisTemplate);
            if (index == 0) {
                qualifier = STRING_REDIS_TEMPLATE_NAME;
            } else {
                qualifier = STRING_REDIS_TEMPLATE_NAME + index;
            }
            beanFactory.registerSingleton(qualifier, stringRedisTemplate);
            beanFactory.applyBeanPostProcessorsAfterInitialization(stringRedisTemplate, qualifier);
            LOGGER.info("register stringRedisTemplate bean {}.", qualifier);
        }
        if (initJedisLock) {
            createJedisDistributedLockBean(beanFactory);
        }
    }

}
