package com.nagabolkka.backend.domain.piece.repository;

import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
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

    // 처음 받는 조각인지 (NEW 뱃지 · 쉼의 조각 1번만)
    boolean existsByUserIdAndPieceId(Long userId, Long pieceId);

    // 도감 : 조각별 받은 횟수 · 처음 받은 시각 (받은 조각만)
    @Query("select new com.nagabolkka.backend.domain.piece.repository.PieceCount(pa.piece.id, count(pa), min(pa.createdAt)) "
            + "from PieceAcquisition pa where pa.user.id = :userId group by pa.piece.id")
    List<PieceCount> countByPiece(@Param("userId") Long userId);

    // 마이페이지 도감 진행 : 찾은 칸 수 (지역 칸만 · 같은 조각 여러 번 = 1칸)
    @Query("select count(distinct pa.piece.id) from PieceAcquisition pa "
            + "where pa.user.id = :userId and pa.piece.region is not null")
    long countFoundSlots(@Param("userId") Long userId);

    // 이 종류 조각을 받은 적 있는지 (쉼의 조각 안내 : REFLECTION)
    boolean existsByUserIdAndPieceType(Long userId, PieceType type);
}