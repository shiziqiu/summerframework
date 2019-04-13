package com.bkjk.platform.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import com.bkjk.platform.common.spring.SpringAutoConfigurationUtil;

public class ExcludeSpringBootRedisAutoConfigProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String REDIS_AUTOCONFIGURATION =
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration";

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        SpringAutoConfigurationUtil.excludeAutoConfiguration(environment, application, REDIS_AUTOCONFIGURATION);
    }
}
