package com.bkjk.platform.redis.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import com.bkjk.platform.redis.DistributedLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

public class JedisDistributedLock implements DistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(JedisDistributedLock.class);

    public static final String UNLOCK_LUA;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if redis.call(\"get\",KEYS[1]) == ARGV[1] ");
        sb.append("then ");
        sb.append("    return redis.call(\"del\",KEYS[1]) ");
        sb.append("else ");
        sb.append("    return 0 ");
        sb.append("end ");
        UNLOCK_LUA = sb.toString();
    }

    private RedisTemplate<Object, Object> redisTemplate;

    private final ThreadLocal<String> lockFlag = new ThreadLocal<>();

    private final Environment env;

    public JedisDistributedLock(RedisTemplate<Object, Object> redisTemplate, Environment env) {
        this.redisTemplate = redisTemplate;
        this.env = env;
    }

    public RedisTemplate<Object, Object> getRedisTemplate() {
        return redisTemplate;
    }

    @Override
    public void lock() {
        tryLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    public void setRedisTemplate(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock() {
        return tryLock(getAppLockDefaultKey(env), DEFAULT_WAIT_MILLS, DEFAULT_EXPIRE_MILLS);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        Long millis = unit.toMillis(time);
        return tryLock(getAppLockDefaultKey(env), millis, DEFAULT_EXPIRE_MILLS);
    }

    @Override
    public boolean tryLock(String key, long wait, long expire) {
        long start = System.currentTimeMillis();
        long duration = 0;
        boolean success = false;
        while (!success && (duration <= wait)) {
            try {
                String result = redisTemplate.execute((RedisCallback<String>)connection -> {
                    JedisCommands commands = (JedisCommands)connection.getNativeConnection();
                    String uuid = UUID.randomUUID().toString();
                    lockFlag.set(uuid);
                    return commands.set(key, uuid, "NX", "PX", expire);
                });
                if (!StringUtils.isEmpty(result)) {
                    success = true;
                    return success;
                } else {
                    LOGGER.warn("try lock fail, will retry lockKey: {}.", key);
                    try {
                        TimeUnit.MILLISECONDS.sleep(300);
                    } catch (InterruptedException e) {
                        LOGGER.debug("tryLock occured an exception", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("set redis occured an exception", e);
            }
            duration = System.currentTimeMillis() - start;
        }
        return success;
    }

    @Override
    public void unlock() {
        unlock(getAppLockDefaultKey(env));
    }

    @Override
    public void unlock(String key) {
        try {
            List<String> keys = new ArrayList<>();
            keys.add(key);
            List<String> args = new ArrayList<>();
            args.add(lockFlag.get());
            redisTemplate.execute((RedisCallback<Long>)connection -> {
                Object nativeConnection = connection.getNativeConnection();
                if (nativeConnection instanceof JedisCluster) {
                    return (Long)((JedisCluster)nativeConnection).eval(UNLOCK_LUA, keys, args);
                } else if (nativeConnection instanceof Jedis) {
                    return (Long)((Jedis)nativeConnection).eval(UNLOCK_LUA, keys, args);
                }
                return 0L;
            });
        } catch (Exception e) {
            LOGGER.debug("release tryLock occur an exception", e);
        } finally {
            lockFlag.remove();
        }
    }
}
