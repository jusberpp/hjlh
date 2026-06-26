package com.huijulh.study.course;

import com.huijulh.study.common.ApiResponse;
import com.huijulh.study.storage.LocalFileStorage;
import com.huijulh.study.storage.StoredFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/course/course")
public class CourseFileController {
    private final LocalFileStorage storage;

    public CourseFileController(LocalFileStorage storage) {
        this.storage = storage;
    }

    @PostMapping("/lecturer-avatar")
    @PreAuthorize("@auth.has('course:course:add') or @auth.has('course:course:edit')")
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        StoredFile stored = storage.storeFormal(
                file, "lecturer", Set.of("jpg", "jpeg", "png", "webp"), 2L * 1024 * 1024);
        return ApiResponse.ok(Map.of(
                "lecturerAvatarUrl", "/common/files?key=" +
                        URLEncoder.encode(stored.storageKey(), StandardCharsets.UTF_8),
                "storageKey", stored.storageKey()
        ));
    }
}
