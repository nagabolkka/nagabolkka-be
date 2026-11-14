package com.nagabolkka.backend.domain.piece.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.piece.dto.CollectionResponse;
import com.nagabolkka.backend.domain.piece.dto.CollectionResponse.RegionPieces;
import com.nagabolkka.backend.domain.piece.dto.CollectionResponse.Slot;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceCount;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 15, 0);

    @Mock UserRepository userRepository;
    @Mock UserRegionRepository userRegionRepository;
    @Mock PieceRepository pieceRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;

    @InjectMocks
    CollectionService collectionService;

    User user;
    List<UserRegion> userRegions;
    List<Piece> pieces;

    @BeforeEach
    void setUp() {
        Region stage1 = region(1L, 1, "시작의 방");
        Region stage2 = region(2L, 2, "바람의 숲");

        // 조각 : 1계단 1 · 2 · 5칸, 2계단 1칸, 보너스 1개 (순서 섞어서 → 정렬 확인)
        pieces = List.of(
                piece(5L, stage1, 5, "쉼의 조각", PieceType.REFLECTION),
                piece(1L, stage1, 1, "햇살 조각", PieceType.EASY),
                piece(6L, stage2, 1, "첫걸음 조각", PieceType.EASY),
                piece(2L, stage1, 2, "아침 조각", PieceType.NORMAL),
                piece(26L, null, null, "첫 발견 조각", PieceType.BONUS));

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.completeOnboarding(3, stage1, Goal.of("JOB", "취업"),
                District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127));

        UserRegion active = UserRegion.create(user, stage1, RegionStatus.ACTIVE, NOW);
        ReflectionTestUtils.setField(active, "completedCount", 2); // 복원도 40
        userRegions = List.of(active, UserRegion.create(user, stage2, RegionStatus.LOCKED, null));
    }

    private Region region(Long id, int stageNo, String name) {
        Region region = Region.of(stageNo, name, "라벨", "조각", "#E8C06A", "a.png", "b.png");
        ReflectionTestUtils.setField(region, "id", id);
        return region;
    }

    private Piece piece(Long id, Region region, Integer slotNo, String name, PieceType type) {
        Piece piece = Piece.of(region, slotNo, name, type, name + " 복원 문구", name + " 힌트");
        ReflectionTestUtils.setField(piece, "id", id);
        return piece;
    }

    private void givenCollection(List<PieceCount> counts) {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(pieceAcquisitionRepository.countByPiece(USER_ID)).willReturn(counts);
        given(pieceRepository.findAll()).willReturn(pieces);
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(userRegions);
    }

    @Test
    @DisplayName("찾은 칸 : 이름 · 복원 문구 · 횟수 / 못 찾은 칸 : 힌트만 · 칸 순서 · 복원도")
    void collection() {
        givenCollection(List.of(new PieceCount(1L, 2L, NOW), new PieceCount(5L, 1L, NOW)));

        CollectionResponse response = collectionService.getCollection(USER_ID);

        assertThat(response.foundCount()).isEqualTo(2);
        assertThat(response.total()).isEqualTo(4); // 보너스 제외
        assertThat(response.regions()).extracting(RegionPieces::stageNo).containsExactly(1, 2);

        RegionPieces stage1 = response.regions().get(0);
        assertThat(stage1.restoration()).isEqualTo(40);
        assertThat(stage1.slots()).extracting(Slot::slotNo).containsExactly(1, 2, 5);

        Slot sun = stage1.slots().get(0);
        assertThat(sun.found()).isTrue();
        assertThat(sun.name()).isEqualTo("햇살 조각");
        assertThat(sun.count()).isEqualTo(2);
        assertThat(sun.restoreText()).isEqualTo("햇살 조각 복원 문구");
        assertThat(sun.hint()).isNull();

        Slot morning = stage1.slots().get(1);
        assertThat(morning.found()).isFalse();
        assertThat(morning.name()).isNull();
        assertThat(morning.restoreText()).isNull();
        assertThat(morning.hint()).isEqualTo("아침 조각 힌트");
        assertThat(morning.count()).isZero();

        assertThat(response.regions().get(1).restoration()).isZero();
        assertThat(response.bonusPieces()).isEmpty();
    }

    @Test
    @DisplayName("보너스 조각 : 받은 것만 · 도감 칸 수 · 찾은 수에는 안 들어감")
    void bonusPiece() {
        givenCollection(List.of(new PieceCount(26L, 1L, NOW)));

        CollectionResponse response = collectionService.getCollection(USER_ID);

        assertThat(response.bonusPieces()).hasSize(1);
        assertThat(response.bonusPieces().get(0).name()).isEqualTo("첫 발견 조각");
        assertThat(response.bonusPieces().get(0).acquiredAt()).isEqualTo(NOW);
        assertThat(response.foundCount()).isZero();
        assertThat(response.total()).isEqualTo(4);
    }

    @Test
    @DisplayName("시작 체크 전 → 409 ONBOARDING_REQUIRED (조각 조회 안 함)")
    void beforeOnboarding() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.createKakaoUser("동진")));

        assertThatThrownBy(() -> collectionService.getCollection(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
        then(pieceRepository).shouldHaveNoInteractions();
    }
}