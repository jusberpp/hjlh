package com.huijulh.study.security;

import com.huijulh.study.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

@RestController
public class AuthController {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        var users = jdbcTemplate.query("""
                        SELECT user_id, username, password_hash, display_name, org_id, permissions
                        FROM sys_user WHERE username = ? AND enabled = 1
                        """,
                (rs, rowNum) -> new UserRow(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getLong("org_id"),
                        rs.getString("permissions")
                ),
                request.username()
        );
        if (users.isEmpty() || !passwordEncoder.matches(request.password(), users.get(0).passwordHash())) {
            return new ApiResponse<>(401, "用户名或密码错误", null);
        }
        UserRow user = users.get(0);
        AdminPrincipal principal = new AdminPrincipal(
                user.userId(),
                user.username(),
                user.displayName(),
                user.orgId(),
                new HashSet<>(Arrays.asList(user.permissions().split(",")))
        );
        return ApiResponse.ok(Map.of(
                "token", jwtService.issue(principal),
                "expiresIn", jwtService.expirationSeconds()
        ));
    }

    @GetMapping("/getInfo")
    public ApiResponse<Map<String, Object>> getInfo() {
        AdminPrincipal principal = SecurityContext.currentAdmin();
        return ApiResponse.ok(Map.of(
                "user", Map.of(
                        "userId", principal.userId(),
                        "userName", principal.username(),
                        "nickName", principal.displayName(),
                        "orgId", principal.orgId()
                ),
                "roles", new String[]{"admin"},
                "permissions", principal.permissions()
        ));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    private record UserRow(
            long userId,
            String username,
            String passwordHash,
            String displayName,
            long orgId,
            String permissions
    ) {}
}
