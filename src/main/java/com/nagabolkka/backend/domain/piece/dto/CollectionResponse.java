package com.nagabolkka.backend.domain.piece.dto;

import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceCount;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record CollectionResponse(

        @Schema(description = "찾은 칸 수 (지역 칸만, 같은 조각 여러 번 = 1칸)", example = "2")
        int foundCount,

        @Schema(description = "전체 칸 수 (5지역 × 5칸)", example = "25")
        int total,

        @Schema(description = "지역 5개 (계단 순서)")
        List<RegionPieces> regions,

        @Schema(description = "받은 보너스 조각 (지역 칸 밖 · 없으면 [])")
        List<BonusPiece> bonusPieces
) {

    public record RegionPieces(
            @Schema(example = "1") int stageNo,
            @Schema(example = "시작의 방") String name,
            @Schema(example = "빛의 조각") String pieceFamily,
            @Schema(example = "#E8C06A") String color,
            @Schema(description = "지역 맑음 이미지 (도감은 비 버전 안 씀)", example = "/images/regions/1-start-room.png") String imageUrl,
            @Schema(description = "복원도 0 ~ 100 (홈과 같은 값)", example = "40") int restoration,
            @Schema(description = "칸 1 ~ 5") List<Slot> slots
    ) {
        public static RegionPieces of(UserRegion userRegion, List<Slot> slots) {
            Region region = userRegion.getRegion();
            return new RegionPieces(region.getStageNo(), region.getName(), region.getPieceFamily(),
                    region.getColor(), region.getImageUrl(), userRegion.getRestoration(), slots);
        }
    }

    public record Slot(
            @Schema(example = "1") Integer slotNo,
            @Schema(example = "1") Long pieceId,
            @Schema(description = "찾은 조각만 (못 찾았으면 null)", example = "햇살 조각", nullable = true) String name,
            @Schema(example = "EASY") PieceType type,
            @Schema(example = "true") boolean found,
            @Schema(description = "받은 횟수", example = "2") int count,
            @Schema(description = "찾은 조각만", example = "하늘에 색이 돌아왔어", nullable = true) String restoreText,
            @Schema(description = "못 찾은 조각만", example = "현관문 열고 햇빛 10분 쬐기", nullable = true) String hint
    ) {
        // pieceCount 가 null = 아직 못 찾음
        public static Slot of(Piece piece, PieceCount pieceCount) {
            boolean found = pieceCount != null;
            return new Slot(
                    piece.getSlotNo(),
                    piece.getId(),
                    found ? piece.getName() : null,
                    piece.getType(),
                    found,
                    found ? pieceCount.count().intValue() : 0,
                    found ? piece.getRestoreText() : null,
                    found ? null : piece.getHint()
            );
        }
    }

    public record BonusPiece(
            @Schema(example = "26") Long pieceId,
            @Schema(example = "첫 발견 조각") String name,
            @Schema(example = "BONUS") PieceType type,
            @Schema(description = "처음 받은 시각", example = "2026-11-14T14:00:00") LocalDateTime acquiredAt
    ) {
        public static BonusPiece of(Piece piece, PieceCount pieceCount) {
            return new BonusPiece(piece.getId(), piece.getName(), piece.getType(), pieceCount.firstAcquiredAt());
        }
    }
}