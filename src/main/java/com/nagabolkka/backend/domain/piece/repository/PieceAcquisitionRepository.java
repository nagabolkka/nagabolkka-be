package com.nagabolkka.backend.domain.piece.repository;

import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PieceAcquisitionRepository extends JpaRepository<PieceAcquisition, Long> {

    // 이미 찾은 조각 id (배정 : 지나온 계단에서 아직 못 찾은 조각을 먼저)
    @Query("select distinct pa.piece.id from PieceAcquisition pa where pa.user.id = :userId")
    List<Long> findFoundPieceIds(@Param("userId") Long userId);

    // 홈 ◆ 배지 : 지금까지 모은 조각 수 (같은 조각 여러 번 포함)
    long countByUserId(Long userId);
}