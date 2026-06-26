package com.huijulh.study.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.security.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class FileTokenService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Duration tokenTtl;

    public FileTokenService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${study.storage.token-ttl}") Duration tokenTtl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.tokenTtl = tokenTtl;
    }

    public IssuedToken issue(String businessType, Map<String, Object> payload) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(tokenTtl);
        try {
            jdbcTemplate.update("""
                            INSERT INTO edu_file_token(
                              token_value, business_type, operator_id, org_id, payload_json, expires_at
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    token, businessType, SecurityContext.currentAdmin().userId(), SecurityContext.orgId(),
                    objectMapper.writeValueAsString(payload), Timestamp.from(expiresAt));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot issue file token", exception);
        }
        return new IssuedToken(token, expiresAt);
    }

    @Transactional
    public Map<String, Object> consume(String token, String businessType, int expiredCode) {
        var rows = jdbcTemplate.query("""
                        SELECT payload_json FROM edu_file_token
                        WHERE token_value=? AND business_type=? AND operator_id=? AND org_id=?
                          AND used_at IS NULL AND expires_at>CURRENT_TIMESTAMP
                        FOR UPDATE
                        """,
                (rs, rowNum) -> rs.getString("payload_json"),
                token, businessType, SecurityContext.currentAdmin().userId(), SecurityContext.orgId()
        );
        if (rows.isEmpty()) throw new BusinessException(expiredCode, "令牌不存在、已过期或已使用");
        int updated = jdbcTemplate.update("""
                        UPDATE edu_file_token SET used_at=CURRENT_TIMESTAMP
                        WHERE token_value=? AND used_at IS NULL
                        """, token);
        if (updated != 1) throw new BusinessException(expiredCode, "令牌已被使用");
        try {
            return objectMapper.readValue(rows.get(0), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot parse file token", exception);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {}
}
