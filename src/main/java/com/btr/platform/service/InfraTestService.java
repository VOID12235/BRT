package com.btr.platform.service;

import com.btr.platform.dto.Requests.DbConfigReq;
import com.btr.platform.dto.Requests.RedisConfigReq;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基础设施连通性测试：用管理后台配置的 MySQL / Redis 参数建立真实连接并返回状态。
 * 仅做只读探测（SELECT 1 / PING），不修改任何数据。
 */
@Slf4j
@Service
public class InfraTestService {

    private final ConfigService configService;

    public InfraTestService(ConfigService configService) {
        this.configService = configService;
    }

    /** 测试 MySQL：优先使用请求参数，缺省（空 / 脱敏占位）时回退已保存配置 */
    public Map<String, Object> testMySql(DbConfigReq req) {
        ConfigService.DbRuntime saved = configService.dbRuntime();
        String host = req == null || req.host() == null || req.host().isBlank() ? saved.host : req.host().trim();
        int port = req == null || req.port() == null ? saved.port : req.port();
        String database = req == null || req.database() == null || req.database().isBlank()
                ? saved.database : req.database().trim();
        String username = req == null || req.username() == null || req.username().isBlank()
                ? saved.username : req.username().trim();
        String password = resolvePassword(req == null ? null : req.password(), saved.password);

        if (host.isBlank() || database.isBlank()) {
            return fail("请先填写 MySQL 主机地址与数据库名");
        }
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?connectTimeout=3000&socketTimeout=3000&useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Shanghai";
        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String version = conn.getMetaData().getDatabaseProductVersion();
            long cost = System.currentTimeMillis() - start;
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("message", "MySQL 连接成功");
            ok.put("detail", "MySQL " + version);
            ok.put("costMs", cost);
            return ok;
        } catch (Exception e) {
            log.warn("MySQL 连接测试失败：{}", e.getMessage());
            return fail(e.getMessage() == null ? "连接失败" : e.getMessage());
        }
    }

    /** 测试 Redis：优先使用请求参数，缺省时回退已保存配置 */
    public Map<String, Object> testRedis(RedisConfigReq req) {
        ConfigService.RedisRuntime saved = configService.redisRuntime();
        String host = req == null || req.host() == null || req.host().isBlank() ? saved.host : req.host().trim();
        int port = req == null || req.port() == null ? saved.port : req.port();
        int database = req == null || req.database() == null ? saved.database : req.database();
        String password = resolvePassword(req == null ? null : req.password(), saved.password);

        RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withDatabase(database)
                .withTimeout(Duration.ofSeconds(3));
        if (password != null && !password.isEmpty()) {
            builder.withPassword(password);
        }
        RedisClient client = RedisClient.create(builder.build());
        long start = System.currentTimeMillis();
        try (StatefulRedisConnection<String, String> conn = client.connect()) {
            String pong = conn.sync().ping();
            long cost = System.currentTimeMillis() - start;
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("message", "Redis 连接成功");
            ok.put("detail", "PONG " + pong.trim());
            ok.put("costMs", cost);
            return ok;
        } catch (Exception e) {
            log.warn("Redis 连接测试失败：{}", e.getMessage());
            return fail(e.getMessage() == null ? "连接失败" : e.getMessage());
        } finally {
            try {
                client.shutdown();
            } catch (Exception ignored) {
                /* 关闭失败不影响结果 */
            }
        }
    }

    private String resolvePassword(String requested, String saved) {
        if (requested == null || requested.isBlank() || ConfigService.MASKED.equals(requested)) {
            return saved == null ? "" : saved;
        }
        return requested;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
