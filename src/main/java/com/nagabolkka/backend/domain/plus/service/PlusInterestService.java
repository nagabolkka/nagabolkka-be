package com.nagabolkka.backend.domain.plus.service;

import com.nagabolkka.backend.domain.piece.entity.AcquireSource;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.plus.dto.PlusInterestRequest;
import com.nagabolkka.backend.domain.plus.dto.PlusInterestResponse;
import com.nagabolkka.backend.domain.plus.dto.PlusInterestResponse.RewardPiece;
import com.nagabolkka.backend.domain.plus.entity.PlusInterest;
import com.nagabolkka.backend.domain.plus.repository.PlusInterestRepository;
import com.nagabolkka.backend.domain.user.dto.MyInfoResponse.PlusInterestInfo;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlusInterestService {

    private final UserRepository userRepository;
    private final PlusInterestRepository plusInterestRepository;
    private final PieceRepository pieceRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;

    // 설문 응답 · 수정 (사용자 당 1행 upsert) → 첫 응답만 첫 발견 조각
    @Transactional
    public PlusInterestResponse respond(Long userId, PlusInterestRequest request) {
        // "관심 있어요" 두 번 눌러도 행 · 조각 1번 → 사용자 행 잠금
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // CUSTOM 금액 검증은 엔티티 (of · change) 에서 → 400 PLUS_CUSTOM_PRICE_REQUIRED
        PlusInterest interest = plusInterestRepository.findByUserId(userId)
                .map(existing -> {
                    existing.change(request.priceOption(), request.customPrice());
                    return existing;
                })
                .orElseGet(() -> PlusInterest.of(user, request.priceOption(), request.customPrice()));

        RewardPiece reward = interest.isRewardGiven() ? null : grantBonusPiece(user, interest);

        // flush 로 created_at · updated_at 을 지금 채움 → 응답 respondedAt 이 방금 시각
        PlusInterest saved = plusInterestRepository.saveAndFlush(interest);
        return new PlusInterestResponse(PlusInterestInfo.from(saved), reward);
    }

    // 도감 25칸 밖 · 복원도 미반영 · 사용자당 1번 (reward_given)
    private RewardPiece grantBonusPiece(User user, PlusInterest interest) {
        Piece piece = pieceRepository.findFirstByType(PieceType.BONUS)
                .orElseThrow(() -> new IllegalStateException("보너스 조각 시드가 없어요")); // 데이터 오류 → 500
        boolean isNew = !pieceAcquisitionRepository.existsByUserIdAndPieceId(user.getId(), piece.getId());
        pieceAcquisitionRepository.save(PieceAcquisition.of(user, piece, AcquireSource.PLUS_INTEREST, null));
        interest.markRewarded();
        return new RewardPiece(piece.getId(), piece.getName(), piece.getType(), isNew);
    }
}