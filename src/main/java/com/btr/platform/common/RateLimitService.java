package com.btr.platform.common;

import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 轻量限流：基于 {@link CacheService#increment(String, Duration)}，
 * Redis 可用时多实例共享计数，不可用时退化为单机计数。
 */
@Service
public class RateLimitService {

    private final CacheService cacheService;

    public RateLimitService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 计数并校验，超过阈值抛出 429。
     *
     * @param bucket 限流桶（如 login-ip / mail-ip）
     * @param key    维度值（IP、邮箱等）
     * @param limit  时间窗口内允许的最大次数
     * @param window 时间窗口
     */
    public void check(String bucket, String key, int limit, Duration window) {
        if (key == null || key.isBlank() || limit <= 0) {
            return;
        }
        long count = cacheService.increment("rl:" + bucket + ":" + key, window);
        if (count > limit) {
            throw new BizException(429, "操作过于频繁，请稍后再试");
        }
    }
}
