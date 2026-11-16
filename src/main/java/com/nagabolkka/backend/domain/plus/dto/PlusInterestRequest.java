package com.nagabolkka.backend.domain.plus.dto;

import com.nagabolkka.backend.domain.plus.entity.PriceOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PlusInterestRequest(
        @Schema(description = "PRICE_1900 · PRICE_3900 · CUSTOM", example = "CUSTOM")
        @NotNull
        PriceOption priceOption,

        @Schema(description = "CUSTOM 일 때 필수 · 0 ~ 50,000 (원). 다른 옵션이면 무시", example = "2500", nullable = true)
        Integer customPrice
) {
}