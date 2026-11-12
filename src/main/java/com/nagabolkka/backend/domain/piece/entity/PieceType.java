package com.nagabolkka.backend.domain.piece.entity;

// 조각 종류
// 칸 1~3 : EASY · NORMAL · CHALLENGE
// 칸 4   : 1~4계단 RAINY · 5계단 PRACTICE
// 칸 5   : 1계단 REFLECTION · 2~3계단 SAVED_PLACE · 4계단 PRACTICE · 5계단 GOAL
// BONUS  : 도감 25칸 밖 (플러스 관심 등)
public enum PieceType {
    EASY,
    NORMAL,
    CHALLENGE,
    RAINY,
    SAVED_PLACE,
    PRACTICE,
    GOAL,
    REFLECTION,
    BONUS
}