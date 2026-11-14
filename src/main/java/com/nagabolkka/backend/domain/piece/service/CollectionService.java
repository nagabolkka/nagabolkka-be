package com.nagabolkka.backend.domain.piece.service;

import com.nagabolkka.backend.domain.piece.dto.CollectionResponse;
import com.nagabolkka.backend.domain.piece.dto.CollectionResponse.BonusPiece;
import com.nagabolkka.backend.domain.piece.dto.CollectionResponse.RegionPieces;
import com.nagabolkka.backend.domain.piece.dto.CollectionResponse.Slot;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceCount;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final PieceRepository pieceRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;

    public CollectionResponse getCollection(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }

        // 내가 받은 조각 : 조각 id → 횟수 · 처음 받은 시각
        Map<Long, PieceCount> counts = pieceAcquisitionRepository.countByPiece(userId).stream()
                .collect(Collectors.toMap(PieceCount::pieceId, Function.identity()));

        // 조각 전체 (25칸 + 보너스) → 지역 칸 · 보너스로 나눔
        List<Piece> pieces = pieceRepository.findAll();
        Map<Long, List<Piece>> piecesByRegion = pieces.stream()
                .filter(piece -> piece.getRegion() != null)
                .collect(Collectors.groupingBy(piece -> piece.getRegion().getId()));

        // 지역 5개 (계단 순서) · 칸 1 ~ 5
        List<RegionPieces> regions = userRegionRepository.findByUserIdOrderByRegionStageNoAsc(userId).stream()
                .map(userRegion -> toRegionPieces(userRegion, piecesByRegion, counts))
                .toList();

        List<Slot> allSlots = regions.stream().flatMap(region -> region.slots().stream()).toList();
        int foundCount = (int) allSlots.stream().filter(Slot::found).count();

        // 보너스 : 받은 것만, 먼저 받은 순서
        List<BonusPiece> bonusPieces = pieces.stream()
                .filter(piece -> piece.getRegion() == null && counts.containsKey(piece.getId()))
                .map(piece -> BonusPiece.of(piece, counts.get(piece.getId())))
                .sorted(Comparator.comparing(BonusPiece::acquiredAt))
                .toList();

        return new CollectionResponse(foundCount, allSlots.size(), regions, bonusPieces);
    }

    private static RegionPieces toRegionPieces(UserRegion userRegion, Map<Long, List<Piece>> piecesByRegion,
                                               Map<Long, PieceCount> counts) {
        List<Slot> slots = piecesByRegion.getOrDefault(userRegion.getRegion().getId(), List.of()).stream()
                .sorted(Comparator.comparing(Piece::getSlotNo))
                .map(piece -> Slot.of(piece, counts.get(piece.getId())))
                .toList();
        return RegionPieces.of(userRegion, slots);
    }
}