package com.bkjk.platform.redis.redisson;

import java.util.ArrayList;
import java.util.List;

public class MultiRedissonProperties {

    private List<RedissonProperties> source = new ArrayList<>();

    public List<RedissonProperties> getSource() {
        return source;
    }

    public void setSource(List<RedissonProperties> source) {
        this.source = source;
    }
}
