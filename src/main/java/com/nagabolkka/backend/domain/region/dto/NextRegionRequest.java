package com.nagabolkka.backend.domain.region.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record NextRegionRequest(

        @Schema(description = "true 열어볼게요 (다음 지역으로) · false 지금이 좋아요 (7일 뒤 다시 제안)", example = "true")
        @NotNull
        Boolean accept
) {
}