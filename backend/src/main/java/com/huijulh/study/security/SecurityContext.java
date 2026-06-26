package com.huijulh.study.security;

import com.huijulh.study.common.BusinessException;
import org.springframework.security.core.Authentication;

public final class SecurityContext {
    private SecurityContext() {}

    public static AdminPrincipal currentAdmin() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
            throw new BusinessException(401, "管理员登录已失效");
        }
        return principal;
    }

    public static long orgId() {
        return currentAdmin().orgId();
    }

    public static String username() {
        return currentAdmin().username();
    }
}
