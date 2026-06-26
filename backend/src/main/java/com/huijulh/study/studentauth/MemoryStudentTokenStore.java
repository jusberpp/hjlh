package com.huijulh.study.studentauth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "study.auth.student-token-store", havingValue = "memory")
public class MemoryStudentTokenStore implements StudentTokenStore {
    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    @Override
    public String issue(long studentId, Duration ttl) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new TokenEntry(studentId, Instant.now().plus(ttl)));
        return token;
    }

    @Override
    public Optional<Long> resolve(String token) {
        TokenEntry entry = tokens.get(token);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.studentId());
    }

    @Override
    public void refresh(String token, Duration ttl) {
        resolve(token).ifPresent(studentId ->
                tokens.put(token, new TokenEntry(studentId, Instant.now().plus(ttl))));
    }

    private record TokenEntry(long studentId, Instant expiresAt) {}
}
