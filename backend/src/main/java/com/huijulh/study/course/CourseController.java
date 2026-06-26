package com.huijulh.study.course;

import com.huijulh.study.common.ApiResponse;
import com.huijulh.study.common.TableDataInfo;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/course/course")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/options")
    @PreAuthorize("@auth.has('course:course:list')")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.ok(courseService.options());
    }

    @GetMapping("/list")
    @PreAuthorize("@auth.has('course:course:list')")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        CourseService.PageResult result = courseService.list(grade, subject, keyword, status, pageNum, pageSize);
        return TableDataInfo.of(result.total(), result.rows());
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("@auth.has('course:course:query')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long courseId) {
        return ApiResponse.ok(courseService.detail(courseId));
    }

    @PostMapping
    @PreAuthorize("@auth.has('course:course:add')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CourseDtos.SaveRequest request) {
        return ApiResponse.ok(courseService.create(request));
    }

    @PutMapping
    @PreAuthorize("@auth.has('course:course:edit')")
    public ApiResponse<Void> update(@Valid @RequestBody CourseDtos.SaveRequest request) {
        courseService.update(request);
        return ApiResponse.ok();
    }

    @PutMapping("/status")
    @PreAuthorize("@auth.has('course:course:edit')")
    public ApiResponse<Void> status(@Valid @RequestBody CourseDtos.StatusRequest request) {
        courseService.updateStatus(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{courseIds}")
    @PreAuthorize("@auth.has('course:course:remove')")
    public ApiResponse<Void> delete(@PathVariable String courseIds) {
        List<Long> ids = Arrays.stream(courseIds.split(",")).map(Long::parseLong).toList();
        courseService.delete(ids);
        return ApiResponse.ok();
    }
}
