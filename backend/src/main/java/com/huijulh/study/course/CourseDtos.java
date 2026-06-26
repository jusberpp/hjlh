package com.huijulh.study.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class CourseDtos {
    private CourseDtos() {}

    public record ScheduleRequest(
            Long scheduleId,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime
    ) {}

    public record SaveRequest(
            Long courseId,
            @NotBlank String grade,
            @NotBlank String subject,
            @NotBlank @Size(max = 30) String lecturerName,
            String lecturerAvatarUrl,
            @NotBlank String sourceValue,
            @NotEmpty List<@Valid ScheduleRequest> classTimes,
            String status
    ) {}

    public record StatusRequest(@NotNull Long courseId, @NotBlank String status) {}

    public record CourseSummary(
            long courseId,
            String courseName,
            String grade,
            String gradeName,
            String subject,
            String subjectName
    ) {}
}
