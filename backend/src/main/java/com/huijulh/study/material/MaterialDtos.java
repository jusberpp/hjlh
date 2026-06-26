package com.huijulh.study.material;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class MaterialDtos {
    private MaterialDtos() {}

    public record QuestionScore(
            @NotNull Integer questionNo,
            @NotNull BigDecimal score
    ) {}

    public record BatchFile(
            @NotBlank String uploadToken,
            @NotNull LocalDateTime openTime,
            LocalDateTime submitDeadline,
            Integer questionCount,
            List<@Valid QuestionScore> questionScores
    ) {}

    public record BatchRequest(
            @NotNull Long courseId,
            @NotBlank String materialType,
            @NotEmpty List<@Valid BatchFile> files
    ) {}

    public record UpdateRequest(
            @NotNull Long materialId,
            @NotBlank String materialType,
            @NotNull LocalDateTime openTime,
            LocalDateTime submitDeadline,
            Integer questionCount,
            List<@Valid QuestionScore> questionScores
    ) {}
}
