package com.nagabolkka.backend.domain.piece.repository;

import com.nagabolkka.backend.domain.piece.entity.Piece;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nagabolkka.backend.domain.piece.entity.PieceType;

import java.util.Optional;

public interface PieceRepository extends JpaRepository<Piece, Long> {

    // 시드 : 지역 · 칸으로 찾기
    Optional<Piece> findByRegionIdAndSlotNo(Long regionId, Integer slotNo);

    // 쉼의 조각 (1계단 5칸 · REFLECTION) — 시드에 1개뿐
    Optional<Piece> findFirstByType(PieceType type);
}