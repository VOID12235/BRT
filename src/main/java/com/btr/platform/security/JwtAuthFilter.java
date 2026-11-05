package com.btr.platform.security;

import com.btr.platform.common.CacheService;
import com.btr.platform.entity.User;
import com.btr.platform.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_CACHE_PREFIX = "user:auth:";
    private static final Duration AUTH_CACHE_TTL = Duration.ofSeconds(60);

    private final JwtService jwtService;
    private final CacheService cacheService;
    private final UserMapper userMapper;

    public JwtAuthFilter(JwtService jwtService, CacheService cacheService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.cacheService = cacheService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String type = claims.get("type", String.class);
                String jti = claims.getId();
                boolean blacklisted = cacheService.get("jwt:blacklist:" + jti) != null;
                if ("access".equals(type) && !blacklisted
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserPrincipal fromToken = jwtService.toPrincipal(claims);
                    // 以数据库为准：账号禁用、角色调整、密码重置后旧 Token 立即失效。
                    // 60 秒短缓存降低每请求 DB 开销；revokeTokens 时主动清缓存保证即时生效。
                    User user = loadUser(fromToken.id());
                    if (user != null && "ACTIVE".equals(user.getStatus())
                            && tokenVersion(user) == fromToken.tokenVersion()) {
                        UserPrincipal principal = new UserPrincipal(user.getId(), user.getAccount(),
                                user.getNickname(), user.getRole(), tokenVersion(user));
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // 无效 Token 交给后续鉴权流程返回 401
            }
        }
        chain.doFilter(request, response);
    }

    private User loadUser(Long userId) {
        String cacheKey = AUTH_CACHE_PREFIX + userId;
        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            String[] parts = cached.split(":");
            // 格式1（新）：status:role:tokenVersion；格式0（旧）：status:tokenVersion
            if (parts.length >= 3) {
                User user = new User();
                user.setId(userId);
                user.setStatus(parts[0]);
                user.setRole(parts[1]);
                try {
                    user.setTokenVersion(Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {
                    user.setTokenVersion(0);
                }
                return user;
            }
            // 旧格式缓存缺角色信息，视为无效，回源数据库
        }
        User user = userMapper.selectById(userId);
        if (user != null) {
            cacheService.set(cacheKey, user.getStatus() + ":" + user.getRole() + ":" + tokenVersion(user), AUTH_CACHE_TTL);
        }
        return user;
    }

    private int tokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }
}
