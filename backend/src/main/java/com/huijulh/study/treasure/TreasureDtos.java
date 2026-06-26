package com.huijulh.study.treasure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class TreasureDtos {
    private TreasureDtos() {}

    public record ConfirmRequest(
            @NotNull Long courseId,
            @NotNull Long homeworkMaterialId,
            @NotBlank String parseToken
    ) {}
}
