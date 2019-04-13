package com.bkjk.platform.redis.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedissonMessageListenerContainer {
    protected final Logger LOGGER = LoggerFactory.getLogger(RedissonMessageListenerContainer.class);
    private RedissonClient redissonClient;

    private Map<String, List<RedisMessageListener>> messageListenersMap = new ConcurrentHashMap<>();

    public RedissonMessageListenerContainer() {
    }

    public void addMessageListener(RedisMessageListener messageListener, String topic) {
        List<RedisMessageListener> list = messageListenersMap.get(topic);
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            messageListenersMap.put(topic, list);
            doSubscribe(topic);
        }
        list.add(messageListener);
    }

    private void doBroadcast(String topic, RedisMessage redisMessage) {
        List<RedisMessageListener> listeners = messageListenersMap.get(topic);
        for (RedisMessageListener listener : listeners) {
            try {
                listener.onMessage(redisMessage);
            } catch (Exception e) {
                LOGGER.error("Broadcast message error, e:{}", e);
            }
        }
    }

    private void doSubscribe(String topicName) {
        RTopic rTopic = redissonClient.getTopic(topicName);
        rTopic.addListener(RedisMessage.class, new MessageListener<RedisMessage>() {
            @Override
            public void onMessage(final CharSequence channel, final RedisMessage msg) {
                doBroadcast(channel.toString(), msg);
            }
        });

    }

    public void setRedissonClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
}
