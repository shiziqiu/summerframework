package com.bkjk.platform.redis.jedis;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

public class CacheCloudProperties extends RedisProperties {

    public enum Type {
        CLUSTER, SENTINEL, STANDALONE
    }

    private Long appId;

    private Type type;

    private String reportUrl = "http://cachecloud.bkjk.cn";

    public Long getAppId() {
        return appId;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public Type getType() {
        return type;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public void setType(Type type) {
        this.type = type;
    }

}
