package com.btr.platform.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 客户端 IP 解析。
 *
 * <p>默认只信任 TCP 连接的对端地址，防止客户端伪造代理头绕过限流。
 * 当应用部署在 Nginx 等可信反向代理之后时，通过 app.security.trust-proxy=true 开启代理头解析：
 * 仅信任由 Nginx 强制覆写的 X-Real-IP（proxy_set_header X-Real-IP $remote_addr）。
 * <b>不读取 X-Forwarded-For</b>——该头由 $proxy_add_x_forwarded_for 生成时会把客户端
 * 自带的原始值保留在最左侧，直接信任最左项等于允许客户端伪造 IP 绕过限流与日志追踪。</p>
 */
@Component
public class ClientIpResolver {

    private static final String UNKNOWN = "unknown";
    private static final int MAX_IP_LENGTH = 64;

    private final boolean trustProxy;

    public ClientIpResolver(@Value("${app.security.trust-proxy:false}") boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public String resolve(HttpServletRequest request) {
        if (trustProxy) {
            // 仅信任 Nginx 强制覆写的 X-Real-IP；缺失时回退到 TCP 对端地址
            String realIp = normalize(request.getHeader("X-Real-IP"));
            if (realIp != null) {
                return realIp;
            }
        }
        String remote = normalize(request.getRemoteAddr());
        return remote == null ? UNKNOWN : remote;
    }

    /** 过滤非法字符，避免日志注入与超长输入。 */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String ip = value.trim();
        if (ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip) || ip.length() > MAX_IP_LENGTH) {
            return null;
        }
        if (ip.startsWith("[")) {
            int end = ip.indexOf(']');
            if (end > 0) {
                ip = ip.substring(1, end);
            }
        }
        for (int i = 0; i < ip.length(); i++) {
            char c = ip.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                    || c == '.' || c == ':' || c == '%';
            if (!ok) {
                return null;
            }
        }
        return ip;
    }
}
