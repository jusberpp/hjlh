package com.huijulh.study.studentauth;

import java.time.Duration;
import java.util.Optional;

public interface StudentTokenStore {
    String issue(long studentId, Duration ttl);
    Optional<Long> resolve(String token);
    void refresh(String token, Duration ttl);
}
