package com.nagabolkka.backend.domain.piece.repository;

import java.time.LocalDateTime;

// 도감 잉크 : 조각을 받은 시각 · 그 퀘스트의 기분 (퀘스트로 받지 않았으면 mood null)
public record PieceMood(Long pieceId, LocalDateTime acquiredAt, Integer mood) {
}