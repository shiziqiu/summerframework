package com.bkjk.platform.redis.jedis;

import java.util.ArrayList;
import java.util.List;

public class MultiCacheCloudProperties {

    private List<CacheCloudProperties> source = new ArrayList<>();

    public List<CacheCloudProperties> getSource() {
        return source;
    }

    public void setSource(List<CacheCloudProperties> source) {
        this.source = source;
    }

}
