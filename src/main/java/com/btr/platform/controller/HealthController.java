package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量健康探针：供容器编排、Nginx 与监控使用，不触发任何业务查询。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        boolean dbUp = checkDb();
        body.put("status", dbUp ? "UP" : "DEGRADED");
        body.put("db", dbUp ? "UP" : "DOWN");
        body.put("time", OffsetDateTime.now().toString());
        return ApiResponse.ok(body);
    }

    private boolean checkDb() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.warn("健康检查数据库连接失败：{}", e.getMessage());
            return false;
        }
    }
}
