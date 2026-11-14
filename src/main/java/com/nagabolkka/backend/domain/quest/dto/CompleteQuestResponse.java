package com.nagabolkka.backend.domain.quest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nagabolkka.backend.global.common.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 위기 감지면 crisisDetected 만 (null 필드는 JSON 에서 빠짐)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompleteQuestResponse(

        @Schema(description = "true 면 연출 없이 도움 받기 화면으로", example = "false")
        boolean crisisDetected,

        @Schema(description = "받은 조각 (보통 1개, 1계단 첫 회고면 쉼의 조각까지 2개)")
        List<PieceItem> pieces,

        @Schema(description = "루미 한마디 (AI, 실패하면 템플릿)",
                example = "\"바람이 시원했다\" ㅡ 그 말 기억할게. 네가 가져온 햇살 조각 덕분에 하늘에 색이 돌아왔어!")
        String lumiMessage,

        Restoration restoration,

        @Schema(description = "다음 날 난이도 (TDD 전까지 현재 난이도)", example = "EASY")
        Difficulty nextDifficulty,

        @Schema(description = "다음 지역 제안 (없으면 필드 없음)")
        NextRegion nextRegionSuggestion
) {

    public record PieceItem(
            @Schema(example = "1") Long pieceId,
            @Schema(example = "햇살 조각") String name,
            @Schema(example = "1") Integer slotNo,
            @Schema(description = "처음 받은 조각 → NEW 뱃지", example = "true") boolean isNew
    ) {
    }

    public record Restoration(
            @Schema(description = "복원도가 오른 계단 (지나온 계단 미션이면 현재보다 낮음)", example = "1") int stageNo,
            @Schema(example = "시작의 방") String regionName,
            @Schema(example = "0") int before,
            @Schema(example = "20") int after
    ) {
    }

    public record NextRegion(
            @Schema(example = "2") int stageNo,
            @Schema(example = "바람의 숲") String name
    ) {
    }

    public static CompleteQuestResponse crisis() {
        return new CompleteQuestResponse(true, null, null, null, null, null);
    }
}