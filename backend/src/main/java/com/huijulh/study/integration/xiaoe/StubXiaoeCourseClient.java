package com.huijulh.study.integration.xiaoe;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "study.xiaoe.mode", havingValue = "stub", matchIfMissing = true)
public class StubXiaoeCourseClient implements XiaoeCourseClient {
    @Override
    public CourseMetadata validate(String sourceValue) {
        if (sourceValue == null || sourceValue.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_XIAOE_COURSE, "小鹅通课程链接或课程 ID 不能为空");
        }
        String trimmed = sourceValue.trim();
        String courseId = trimmed;
        String normalizedUrl;
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                URI uri = URI.create(trimmed);
                courseId = Arrays.stream(uri.getRawQuery() == null ? new String[0] : uri.getRawQuery().split("&"))
                        .map(value -> value.split("=", 2))
                        .filter(pair -> pair.length == 2 && ("id".equals(pair[0]) || "resource_id".equals(pair[0])))
                        .map(pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8))
                        .findFirst()
                        .orElseGet(() -> {
                            String path = uri.getPath();
                            return path == null ? "" : path.substring(path.lastIndexOf('/') + 1);
                        });
            } catch (Exception exception) {
                throw new BusinessException(ErrorCode.INVALID_XIAOE_COURSE, "小鹅通课程地址格式错误");
            }
            normalizedUrl = trimmed;
        } else {
            if (!trimmed.matches("[A-Za-z0-9_-]{3,128}")) {
                throw new BusinessException(ErrorCode.INVALID_XIAOE_COURSE, "小鹅通课程 ID 格式错误");
            }
            normalizedUrl = "https://app.xiaoe-tech.com/detail?id=" + trimmed;
        }
        if (courseId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_XIAOE_COURSE, "无法从地址解析小鹅通课程 ID");
        }
        return new CourseMetadata(courseId, normalizedUrl, null, null);
    }
}
