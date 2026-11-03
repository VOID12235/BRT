package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.dto.Views.SiteConfig;
import com.btr.platform.service.ConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开只读配置：供前台 header/footer 动态注入站点信息。
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/public-site")
    public ApiResponse<SiteConfig> publicSite() {
        return ApiResponse.ok(configService.getSite());
    }
}
