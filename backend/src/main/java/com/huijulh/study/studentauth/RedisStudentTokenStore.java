package com.huijulh.study.studentauth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "study.auth.student-token-store", havingValue = "redis", matchIfMissing = true)
public class RedisStudentTokenStore implements StudentTokenStore {
    private static final String PREFIX = "e-study:student-auth:";
    private final StringRedisTemplate redisTemplate;

    public RedisStudentTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String issue(long studentId, Duration ttl) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(PREFIX + token, Long.toString(studentId), ttl);
        return token;
    }

    @Override
    public Optional<Long> resolve(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String value = redisTemplate.opsForValue().get(PREFIX + token);
        return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
    }

    @Override
    public void refresh(String token, Duration ttl) {
        redisTemplate.expire(PREFIX + token, ttl);
    }
}
