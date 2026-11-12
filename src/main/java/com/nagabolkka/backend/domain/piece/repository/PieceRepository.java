package com.nagabolkka.backend.domain.piece.repository;

import com.nagabolkka.backend.domain.piece.entity.Piece;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PieceRepository extends JpaRepository<Piece, Long> {

    // 시드 : 지역 · 칸으로 찾기
    Optional<Piece> findByRegionIdAndSlotNo(Long regionId, Integer slotNo);
}