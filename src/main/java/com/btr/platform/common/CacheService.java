package com.btr.platform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量缓存门面：生产使用 Redis，本地开发无 Redis 时自动降级为带 TTL 的进程内缓存。
 */
@Slf4j
@Component
public class CacheService {

    private final StringRedisTemplate redis;
    private final Map<String, Entry> local = new ConcurrentHashMap<>();
    private volatile long redisDownUntil = 0L;

    public CacheService(ObjectProvider<StringRedisTemplate> provider) {
        this.redis = provider.getIfAvailable();
    }

    public void set(String key, String value, Duration ttl) {
        if (redisAvailable()) {
            try {
                redis.opsForValue().set(key, value, ttl);
                return;
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        local.put(key, new Entry(value, System.currentTimeMillis() + ttl.toMillis()));
    }

    public String get(String key) {
        if (redisAvailable()) {
            try {
                return redis.opsForValue().get(key);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        Entry entry = local.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            local.remove(key);
            return null;
        }
        return entry.value;
    }

    public void delete(String key) {
        if (redisAvailable()) {
            try {
                redis.delete(key);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        local.remove(key);
    }

    /** 供榜单模块使用；无 Redis 时返回 null。 */
    public StringRedisTemplate redis() {
        return redisAvailable() ? redis : null;
    }

    public void markRedisDown(Exception e) {
        redisDownUntil = System.currentTimeMillis() + 60_000L;
        log.warn("Redis 不可用，已降级为本地缓存：{}", e.getMessage());
    }

    private boolean redisAvailable() {
        return redis != null && System.currentTimeMillis() > redisDownUntil;
    }

    private record Entry(String value, long expireAt) {
    }
}
