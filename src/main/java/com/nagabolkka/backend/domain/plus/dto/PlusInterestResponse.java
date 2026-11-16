package com.nagabolkka.backend.domain.plus.dto;

import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.user.dto.MyInfoResponse.PlusInterestInfo;
import io.swagger.v3.oas.annotations.media.Schema;

public record PlusInterestResponse(
        PlusInterestInfo plusInterest,
        @Schema(description = "첫 응답 때만 첫 발견 조각, 응답을 바꾸면 null", nullable = true) RewardPiece rewardPiece
) {

    public record RewardPiece(
            @Schema(example = "26") Long pieceId,
            @Schema(example = "첫 발견 조각") String name,
            @Schema(example = "BONUS") PieceType type,
            @Schema(description = "처음 받은 조각 → NEW 뱃지", example = "true") boolean isNew
    ) {
    }
}