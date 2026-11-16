package com.nagabolkka.backend.domain.plus.service;

import com.nagabolkka.backend.domain.piece.entity.AcquireSource;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.plus.dto.PlusInterestRequest;
import com.nagabolkka.backend.domain.plus.dto.PlusInterestResponse;
import com.nagabolkka.backend.domain.plus.entity.PlusInterest;
import com.nagabolkka.backend.domain.plus.entity.PriceOption;
import com.nagabolkka.backend.domain.plus.repository.PlusInterestRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PlusInterestServiceTest {

    private static final Long USER_ID = 1L;

    @Mock UserRepository userRepository;
    @Mock PlusInterestRepository plusInterestRepository;
    @Mock PieceRepository pieceRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;

    @InjectMocks PlusInterestService service;

    User user;
    Piece bonus;

    @BeforeEach
    void setUp() {
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        bonus = Piece.of(null, null, "첫 발견 조각", PieceType.BONUS, "복원 문구", "힌트");
        ReflectionTestUtils.setField(bonus, "id", 26L);

        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    }

    // 이미 응답했고 조각도 받은 상태
    private PlusInterest answered() {
        PlusInterest interest = PlusInterest.of(user, PriceOption.PRICE_1900, null);
        interest.markRewarded();
        given(plusInterestRepository.findByUserId(USER_ID)).willReturn(Optional.of(interest));
        given(plusInterestRepository.saveAndFlush(any(PlusInterest.class))).will(returnsFirstArg());
        return interest;
    }

    @Test
    @DisplayName("첫 응답 → 1행 저장 · 첫 발견 조각 NEW · reward_given true")
    void firstResponse() {
        given(plusInterestRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(pieceRepository.findFirstByType(PieceType.BONUS)).willReturn(Optional.of(bonus));
        given(pieceAcquisitionRepository.existsByUserIdAndPieceId(USER_ID, 26L)).willReturn(false);
        given(plusInterestRepository.saveAndFlush(any(PlusInterest.class))).will(returnsFirstArg());

        PlusInterestResponse res = service.respond(USER_ID, new PlusInterestRequest(PriceOption.PRICE_1900, null));

        assertThat(res.plusInterest().priceOption()).isEqualTo(PriceOption.PRICE_1900);
        assertThat(res.plusInterest().customPrice()).isNull();
        assertThat(res.rewardPiece().pieceId()).isEqualTo(26L);
        assertThat(res.rewardPiece().name()).isEqualTo("첫 발견 조각");
        assertThat(res.rewardPiece().type()).isEqualTo(PieceType.BONUS);
        assertThat(res.rewardPiece().isNew()).isTrue();

        ArgumentCaptor<PieceAcquisition> acquisition = ArgumentCaptor.forClass(PieceAcquisition.class);
        then(pieceAcquisitionRepository).should().save(acquisition.capture());
        assertThat(acquisition.getValue().getSource()).isEqualTo(AcquireSource.PLUS_INTEREST);

        ArgumentCaptor<PlusInterest> saved = ArgumentCaptor.forClass(PlusInterest.class);
        then(plusInterestRepository).should().saveAndFlush(saved.capture());
        assertThat(saved.getValue().isRewardGiven()).isTrue();
    }

    @Test
    @DisplayName("응답 바꾸기 → 같은 행 수정 · 조각 없음 (rewardPiece null)")
    void changeResponse() {
        PlusInterest interest = answered();

        PlusInterestResponse res = service.respond(USER_ID, new PlusInterestRequest(PriceOption.CUSTOM, 2500));

        assertThat(res.rewardPiece()).isNull();
        assertThat(interest.getPriceOption()).isEqualTo(PriceOption.CUSTOM);
        assertThat(interest.getCustomPrice()).isEqualTo(2500);
        then(pieceAcquisitionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("CUSTOM 인데 금액 없음 → 400 PLUS_CUSTOM_PRICE_REQUIRED · 저장 안 함")
    void custom_noPrice() {
        given(plusInterestRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.respond(USER_ID, new PlusInterestRequest(PriceOption.CUSTOM, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.PLUS_CUSTOM_PRICE_REQUIRED);
        then(plusInterestRepository).should(never()).saveAndFlush(any());
        then(pieceAcquisitionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("CUSTOM 50,001원 → 400 PLUS_CUSTOM_PRICE_REQUIRED · 기존 응답 그대로")
    void custom_overMax() {
        PlusInterest interest = PlusInterest.of(user, PriceOption.PRICE_1900, null);
        given(plusInterestRepository.findByUserId(USER_ID)).willReturn(Optional.of(interest));

        assertThatThrownBy(() -> service.respond(USER_ID,
                new PlusInterestRequest(PriceOption.CUSTOM, PlusInterest.CUSTOM_PRICE_MAX + 1)))
                .extracting("errorCode").isEqualTo(ErrorCode.PLUS_CUSTOM_PRICE_REQUIRED);
        assertThat(interest.getPriceOption()).isEqualTo(PriceOption.PRICE_1900);
    }

    @Test
    @DisplayName("PRICE_3900 + 금액 보냄 → 금액은 null 로 저장")
    void fixedOption_ignoresPrice() {
        PlusInterest interest = answered();

        service.respond(USER_ID, new PlusInterestRequest(PriceOption.PRICE_3900, 3000));

        assertThat(interest.getPriceOption()).isEqualTo(PriceOption.PRICE_3900);
        assertThat(interest.getCustomPrice()).isNull();
    }
}