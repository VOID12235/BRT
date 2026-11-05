package com.btr.platform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量缓存门面：生产使用 Redis，本地开发无 Redis 时自动降级为带 TTL 的进程内缓存。
 *
 * <p>本地缓存增加了容量上限与定时清理，避免长期运行后内存无限增长；
 * 同时提供计数自增能力，供限流、验证码错误次数等场景使用。</p>
 */
@Slf4j
@Component
public class CacheService {

    /** 本地缓存最大键数量，超出后先清理过期键，仍然超出则整体清空（极端兜底）。 */
    private static final int LOCAL_MAX_ENTRIES = 10_000;

    private final StringRedisTemplate redis;
    private final Map<String, Entry> local = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
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
        guardCapacity();
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
        counters.remove(key);
    }

    /**
     * 原子自增计数（Redis INCR / 本地近似原子），并刷新过期时间。
     * 用于登录限流、验证码错误次数等场景。
     */
    public long increment(String key, Duration ttl) {
        if (redisAvailable()) {
            try {
                Long value = redis.opsForValue().increment(key);
                if (value != null && value == 1L) {
                    redis.expire(key, ttl);
                }
                return value == null ? 0L : value;
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        long now = System.currentTimeMillis();
        guardCapacity();
        Counter counter = counters.compute(key, (k, current) -> {
            if (current == null || current.expireAt <= now) {
                return new Counter(1L, now + ttl.toMillis());
            }
            current.value++;
            current.expireAt = now + ttl.toMillis();
            return current;
        });
        return counter.value;
    }

    /** 供榜单模块使用；无 Redis 时返回 null。 */
    public StringRedisTemplate redis() {
        return redisAvailable() ? redis : null;
    }

    public void markRedisDown(Exception e) {
        redisDownUntil = System.currentTimeMillis() + 60_000L;
        log.warn("Redis 不可用，已降级为本地缓存：{}", e.getMessage());
    }

    /** 每分钟清理过期键，避免本地缓存长期占用内存。 */
    @Scheduled(fixedDelay = 60_000L)
    public void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> entries = local.entrySet().iterator();
        while (entries.hasNext()) {
            if (entries.next().getValue().expireAt < now) {
                entries.remove();
            }
        }
        counters.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }

    private void guardCapacity() {
        if (local.size() < LOCAL_MAX_ENTRIES && counters.size() < LOCAL_MAX_ENTRIES) {
            return;
        }
        cleanup();
        if (local.size() >= LOCAL_MAX_ENTRIES) {
            log.warn("本地缓存键数量达到上限 {}，执行整体清理", LOCAL_MAX_ENTRIES);
            local.clear();
        }
        if (counters.size() >= LOCAL_MAX_ENTRIES) {
            counters.clear();
        }
    }

    private boolean redisAvailable() {
        return redis != null && System.currentTimeMillis() > redisDownUntil;
    }

    private record Entry(String value, long expireAt) {
    }

    private static final class Counter {
        private long value;
        private long expireAt;

        private Counter(long value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
