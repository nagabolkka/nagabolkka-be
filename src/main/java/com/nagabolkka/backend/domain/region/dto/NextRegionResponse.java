package com.nagabolkka.backend.domain.region.dto;

import com.nagabolkka.backend.domain.region.entity.UserRegion;
import io.swagger.v3.oas.annotations.media.Schema;

public record NextRegionResponse(
        @Schema(description = "응답 후 지금 머무는 지역 (열기 = 다음 지역 · 머물기 = 그대로)") CurrentRegion currentRegion
) {

    public record CurrentRegion(
            @Schema(example = "2") int stageNo,
            @Schema(example = "바람의 숲") String name,
            @Schema(description = "복원도 0 ~ 100", example = "0") int restoration
    ) {
    }

    public static NextRegionResponse from(UserRegion userRegion) {
        return new NextRegionResponse(new CurrentRegion(userRegion.getRegion().getStageNo(),
                userRegion.getRegion().getName(), userRegion.getRestoration()));
    }
}