package com.huijulh.study.security;

import java.util.Set;

public record AdminPrincipal(
        long userId,
        String username,
        String displayName,
        long orgId,
        Set<String> permissions
) {
    public boolean hasPermission(String permission) {
        return permissions.contains("*") || permissions.contains(permission);
    }
}
