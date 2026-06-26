package com.huijulh.study.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class StudentDtos {
    private StudentDtos() {}

    public record SaveRequest(
            Long studentId,
            @NotBlank @Size(max = 100) String school,
            @NotBlank String grade,
            @NotBlank @Size(max = 50) String className,
            @NotBlank @Size(max = 50) String studentName,
            @NotBlank @Size(max = 64) String studentNo,
            String learningLevels,
            @NotEmpty List<@NotNull Long> courseIds,
            Long primaryCourseId,
            String authorizedPhone
    ) {}
}
