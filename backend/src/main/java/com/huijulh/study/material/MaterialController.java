package com.huijulh.study.material;

import com.huijulh.study.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/course/material")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/course-list")
    @PreAuthorize("@auth.has('course:material:list')")
    public ApiResponse<List<Map<String, Object>>> courseList(
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(materialService.courseList(keyword));
    }

    @GetMapping("/list")
    @PreAuthorize("@auth.has('course:material:list')")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam long courseId,
            @RequestParam(required = false) String materialType
    ) {
        return ApiResponse.ok(materialService.list(courseId, materialType));
    }

    @PostMapping("/upload")
    @PreAuthorize("@auth.has('course:material:add')")
    public ApiResponse<List<Map<String, Object>>> upload(@RequestParam("files") List<MultipartFile> files) {
        return ApiResponse.ok(materialService.upload(files));
    }

    @PostMapping("/batch")
    @PreAuthorize("@auth.has('course:material:add')")
    public ApiResponse<Map<String, Object>> createBatch(@Valid @RequestBody MaterialDtos.BatchRequest request) {
        return ApiResponse.ok(materialService.createBatch(request));
    }

    @GetMapping("/{materialId}")
    @PreAuthorize("@auth.has('course:material:query')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long materialId) {
        return ApiResponse.ok(materialService.detail(materialId));
    }

    @PutMapping
    @PreAuthorize("@auth.has('course:material:edit')")
    public ApiResponse<Void> update(@Valid @RequestBody MaterialDtos.UpdateRequest request) {
        materialService.update(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{materialIds}")
    @PreAuthorize("@auth.has('course:material:remove')")
    public ApiResponse<Void> delete(@PathVariable String materialIds) {
        materialService.delete(Arrays.stream(materialIds.split(",")).map(Long::parseLong).toList());
        return ApiResponse.ok();
    }
}
