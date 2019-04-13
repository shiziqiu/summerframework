package com.bkjk.platform.redis.redisson;

import com.bkjk.platform.redis.jedis.CacheCloudProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.bkjk.platform.redis.DistributedLock.DISTRIBUTED_LOCK_NAME;
import static com.bkjk.platform.redis.report.CacheCloudInfoHttpHelper.getClusterNodeFromCacheCloud;
import static com.bkjk.platform.redis.report.CacheCloudInfoHttpHelper.getSentinelConfigurationFromCacheCloud;

public class RedissonPostProcessor {
    private static final String REDISSON_NAME = "redissonClient";

    private final MultiRedissonProperties redissonProperties;
    private final Environment env;

    public RedissonPostProcessor(MultiRedissonProperties redissonProperties, Environment env) {
        this.redissonProperties = redissonProperties;
        this.env = env;
    }

    private Config applyConfig(RedissonProperties redissonProperties) {
        Long appId = redissonProperties.getAppId();
        com.bkjk.platform.redis.redisson.Config mConfig = redissonProperties.getConfig();
        Config config = new Config();
        if (appId == null) {
            if (mConfig.getClusterServersConfig() != null) {
                setConfigPrivateFiled("clusterServersConfig", config, mConfig.getClusterServersConfig());
            }

            if (mConfig.getSentinelServersConfig() != null) {
                setConfigPrivateFiled("sentinelServersConfig", config, mConfig.getSentinelServersConfig());
            }

            if (mConfig.getSingleServerConfig() != null) {
                setConfigPrivateFiled("singleServerConfig", config, mConfig.getSingleServerConfig());
            }

            config.setNettyThreads(mConfig.getNettyThreads());
            config.setThreads(mConfig.getThreads());
            return config;
        }

        String url = redissonProperties.getReportUrl();
        CacheCloudProperties.Type type = redissonProperties.getType();
        if (StringUtils.isEmpty(url) || type == null) {
            throw new RuntimeException(
                "Missing params, " + ", appId: " + appId + ", url: " + "" + url + ", type: " + type);
        }

        String reportUrl = redissonProperties.getReportUrl();
        if (type == CacheCloudProperties.Type.SENTINEL) {
            RedisSentinelConfiguration redisSentinelConfiguration =
                getSentinelConfigurationFromCacheCloud(appId, reportUrl);
            config.useSentinelServers()
                .addSentinelAddress(convertNode2String(redisSentinelConfiguration.getSentinels()));
            config.useSentinelServers().setMasterName(redisSentinelConfiguration.getMaster().getName());
        } else if (type == CacheCloudProperties.Type.CLUSTER) {
            List<String> nodes = getClusterNodeFromCacheCloud(appId, reportUrl);
            config.useClusterServers().addNodeAddress(convert2RedisProtocal(nodes));
        }
        return config;
    }

    private String[] convert2RedisProtocal(List<String> nodes) {
        String[] temp = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            temp[i] = "redis://" + nodes.get(i);
        }
        return temp;
    }

    private String[] convertNode2String(Set<RedisNode> sentinels) {
        String[] nodes = new String[sentinels.size()];
        int i = 0;
        Iterator<RedisNode> iterator = sentinels.iterator();
        while (iterator.hasNext()) {
            RedisNode node = iterator.next();
            String hostPort = node.getHost() + ":" + node.getPort();
            nodes[i] = hostPort;
            i++;
        }
        return nodes;
    }

    private RedissonClient createProxyHandler(RedissonClient redissonClient) {
        RedissonClientProxyHandler handler = new RedissonClientProxyHandler(redissonClient);
        Object proxy =
            Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] {RedissonClient.class}, handler);
        return (RedissonClient)proxy;
    }

    private void createRedissonDistributedLockBean(ConfigurableListableBeanFactory beanFactory) {
        RedissonClient redissonClient = beanFactory.getBean(REDISSON_NAME, RedissonClient.class);
        RedissonDistributedLock lock = new RedissonDistributedLock(redissonClient, env);
        beanFactory.registerSingleton(DISTRIBUTED_LOCK_NAME, lock);
        beanFactory.applyBeanPostProcessorsAfterInitialization(lock, DISTRIBUTED_LOCK_NAME);
    }

    private void setConfigPrivateFiled(String fieldName, Config config, Object obj) {
        try {
            Class cl = Config.class;
            Field field = cl.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(config, obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void process(ConfigurableListableBeanFactory beanFactory) throws RuntimeException, IOException {
        if (redissonProperties == null || redissonProperties.getSource() == null
            || redissonProperties.getSource().size() == 0) {
            return;
        }
        List<RedissonProperties> source = redissonProperties.getSource();
        for (int i = 0; i < source.size(); i++) {
            Config rConfig = applyConfig(source.get(i));
            RedissonClient redissonClient = Redisson.create(rConfig);
            RedissonClient proxy = createProxyHandler(redissonClient);
            String qualifier;
            if (i == 0) {
                qualifier = REDISSON_NAME;
            } else {
                qualifier = REDISSON_NAME + i;
            }
            beanFactory.registerSingleton(qualifier, proxy);
            beanFactory.applyBeanPostProcessorsAfterInitialization(proxy, qualifier);
        }
        createRedissonDistributedLockBean(beanFactory);
    }

}
