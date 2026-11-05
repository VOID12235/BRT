package com.btr.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 Web MVC 配置。
 *
 * <p>上传文件统一通过 /api/v1/files/** 服务（md5 指纹 + 下载安全策略），
 * 不再注册 /uploads/** 静态映射，避免形成绕过指纹鉴权与 CSP 的双通道。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
