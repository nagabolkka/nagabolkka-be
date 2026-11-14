package com.nagabolkka.backend.domain.piece.repository;

import java.time.LocalDateTime;

// 사용자가 조각 하나를 몇 번 받았는지 · 처음 받은 시각 (도감)
public record PieceCount(Long pieceId, Long count, LocalDateTime firstAcquiredAt) {
}