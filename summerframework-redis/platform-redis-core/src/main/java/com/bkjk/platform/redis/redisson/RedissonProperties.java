package com.bkjk.platform.redis.redisson;

import com.bkjk.platform.redis.jedis.CacheCloudProperties;

public class RedissonProperties {

    private Long appId;

    private CacheCloudProperties.Type type = CacheCloudProperties.Type.CLUSTER;

    private String reportUrl = "http://cachecloud.bkjk.cn";

    private Config config = new Config();

    public Long getAppId() {
        return appId;
    }

    public Config getConfig() {
        return config;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public CacheCloudProperties.Type getType() {
        return type;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public void setType(CacheCloudProperties.Type type) {
        this.type = type;
    }

}
