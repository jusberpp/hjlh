package com.huijulh.study.student;

import com.huijulh.study.common.ApiResponse;
import com.huijulh.study.common.TableDataInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/study/student")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/list")
    @PreAuthorize("@auth.has('study:student:list')")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String phoneStatus,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        StudentService.PageResult result =
                studentService.list(keyword, grade, courseId, phoneStatus, pageNum, pageSize);
        return TableDataInfo.of(result.total(), result.rows());
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("@auth.has('study:student:query')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long studentId) {
        return ApiResponse.ok(studentService.detail(studentId));
    }

    @PostMapping
    @PreAuthorize("@auth.has('study:student:add')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody StudentDtos.SaveRequest request) {
        return ApiResponse.ok(studentService.create(request));
    }

    @PutMapping
    @PreAuthorize("@auth.has('study:student:edit')")
    public ApiResponse<Void> update(@Valid @RequestBody StudentDtos.SaveRequest request) {
        studentService.update(request);
        return ApiResponse.ok();
    }
}
