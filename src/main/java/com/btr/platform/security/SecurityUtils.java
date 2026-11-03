package com.btr.platform.security;

import com.btr.platform.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BizException(401, "请先登录");
        }
        return principal;
    }

    public static UserPrincipal currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal principal ? principal : null;
    }

    public static Long userIdOrNull() {
        UserPrincipal principal = currentOrNull();
        return principal == null ? null : principal.id();
    }

    public static boolean isAdmin() {
        UserPrincipal principal = currentOrNull();
        return principal != null && "ADMIN".equals(principal.role());
    }
}
