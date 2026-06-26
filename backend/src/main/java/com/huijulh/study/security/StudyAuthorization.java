package com.huijulh.study.security;

import org.springframework.stereotype.Component;

@Component("auth")
public class StudyAuthorization {
    public boolean has(String permission) {
        try {
            return SecurityContext.currentAdmin().hasPermission(permission);
        } catch (Exception exception) {
            return false;
        }
    }
}
