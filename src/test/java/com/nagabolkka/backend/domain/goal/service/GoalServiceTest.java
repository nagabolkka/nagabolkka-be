package com.nagabolkka.backend.domain.goal.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.dto.CheckGoalStepResponse;
import com.nagabolkka.backend.domain.goal.dto.MyGoalResponse;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.entity.GoalStep;
import com.nagabolkka.backend.domain.goal.entity.UserGoalStep;
import com.nagabolkka.backend.domain.goal.repository.GoalStepRepository;
import com.nagabolkka.backend.domain.goal.repository.UserGoalStepRepository;
import com.nagabolkka.backend.domain.piece.entity.AcquireSource;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long JOB_ID = 1L;

    @Mock UserRepository userRepository;
    @Mock GoalStepRepository goalStepRepository;
    @Mock UserGoalStepRepository userGoalStepRepository;
    @Mock PieceRepository pieceRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;

    GoalService service;
    User user;
    Goal job;
    List<GoalStep> steps;   // id 11 ~ 14
    GoalStep otherGoalStep; // 다른 목표 단계 id 31
    Piece hope;

    @BeforeEach
    void setUp() {
        service = new GoalService(userRepository, goalStepRepository, userGoalStepRepository,
                pieceRepository, pieceAcquisitionRepository);

        job = Goal.of("JOB", "취업");
        ReflectionTestUtils.setField(job, "id", JOB_ID);
        steps = IntStream.rangeClosed(1, 4)
                .mapToObj(n -> {
                    GoalStep step = GoalStep.of(job, n, "단계 " + n);
                    ReflectionTestUtils.setField(step, "id", 10L + n);
                    return step;
                })
                .toList();

        Goal meetup = Goal.of("MEETUP", "모임");
        ReflectionTestUtils.setField(meetup, "id", 3L);
        otherGoalStep = GoalStep.of(meetup, 1, "모임 단계 1");
        ReflectionTestUtils.setField(otherGoalStep, "id", 31L);

        Region stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        Region stage5 = Region.of(5, "새로운 세상", "새로운 곳", "희망의 조각", "#E8C06A", "a.png", "b.png");
        hope = Piece.of(stage5, 5, "희망 조각", PieceType.GOAL, "새로운 세상에 아침이 밝았어", "내 목표 준비 4단계 모두 완료하기");
        ReflectionTestUtils.setField(hope, "id", 25L);

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        District seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user.completeOnboarding(5, stage2, job, seongbuk);
    }

    // 체크 요청 공통 : 잠금 조회 · 단계 · 이미 체크한 단계 · 전체 4단계
    private void givenCheck(Long stepId, GoalStep step, List<Long> checkedIds) {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(goalStepRepository.findById(stepId)).willReturn(Optional.of(step));
        given(userGoalStepRepository.findCheckedStepIds(USER_ID, JOB_ID)).willReturn(checkedIds);
        given(goalStepRepository.countByGoalId(JOB_ID)).willReturn(4L);
    }

    @Test
    @DisplayName("내 목표 : 4단계 · 체크 상태 · 수 · 희망 조각 수령 여부")
    void getMyGoal() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(goalStepRepository.findByGoalOrderByStepNoAsc(job)).willReturn(steps);
        given(userGoalStepRepository.findCheckedStepIds(USER_ID, JOB_ID)).willReturn(List.of(11L, 13L));
        given(pieceAcquisitionRepository.existsByUserIdAndSourceAndGoalId(USER_ID, AcquireSource.GOAL, JOB_ID))
                .willReturn(false);

        MyGoalResponse res = service.getMyGoal(USER_ID);

        assertThat(res.goalId()).isEqualTo(JOB_ID);
        assertThat(res.name()).isEqualTo("취업");
        assertThat(res.steps()).extracting(MyGoalResponse.StepItem::checked)
                .containsExactly(true, false, true, false);
        assertThat(res.checked()).isEqualTo(2);
        assertThat(res.total()).isEqualTo(4);
        assertThat(res.hopePieceReceived()).isFalse();
    }

    @Test
    @DisplayName("시작 체크 전 → 409 ONBOARDING_REQUIRED")
    void getMyGoal_onboardingRequired() {
        User fresh = User.createKakaoUser("새 사용자");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(fresh));

        assertThatThrownBy(() -> service.getMyGoal(USER_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
    }

    @Test
    @DisplayName("3번째 체크 → 행 추가 · 3/4 · 희망 조각 없음")
    void check_notComplete() {
        givenCheck(13L, steps.get(2), List.of(11L, 12L));

        CheckGoalStepResponse res = service.checkStep(USER_ID, 13L, true);

        assertThat(res.checked()).isEqualTo(3);
        assertThat(res.total()).isEqualTo(4);
        assertThat(res.hopePiece()).isNull();
        then(userGoalStepRepository).should().save(any(UserGoalStep.class));
        then(pieceAcquisitionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이 목표 처음 4/4 → 희망 조각 (NEW) · 조각 기록에 목표 연결")
    void check_firstComplete() {
        givenCheck(14L, steps.get(3), List.of(11L, 12L, 13L));
        given(pieceAcquisitionRepository.existsByUserIdAndSourceAndGoalId(USER_ID, AcquireSource.GOAL, JOB_ID))
                .willReturn(false);
        given(pieceRepository.findFirstByType(PieceType.GOAL)).willReturn(Optional.of(hope));
        given(pieceAcquisitionRepository.existsByUserIdAndPieceId(USER_ID, 25L)).willReturn(false);

        CheckGoalStepResponse res = service.checkStep(USER_ID, 14L, true);

        assertThat(res.checked()).isEqualTo(4);
        assertThat(res.hopePiece().pieceId()).isEqualTo(25L);
        assertThat(res.hopePiece().name()).isEqualTo("희망 조각");
        assertThat(res.hopePiece().isNew()).isTrue();

        ArgumentCaptor<PieceAcquisition> captor = ArgumentCaptor.forClass(PieceAcquisition.class);
        then(pieceAcquisitionRepository).should().save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(AcquireSource.GOAL);
        assertThat(captor.getValue().getGoal()).isSameAs(job);
        assertThat(captor.getValue().getDailyQuest()).isNull();
    }

    @Test
    @DisplayName("이 목표로 이미 받음 (해제 후 다시 4/4) → 희망 조각 null · 추가 지급 없음")
    void check_alreadyReceived() {
        givenCheck(14L, steps.get(3), List.of(11L, 12L, 13L));
        given(pieceAcquisitionRepository.existsByUserIdAndSourceAndGoalId(USER_ID, AcquireSource.GOAL, JOB_ID))
                .willReturn(true);

        CheckGoalStepResponse res = service.checkStep(USER_ID, 14L, true);

        assertThat(res.checked()).isEqualTo(4);
        assertThat(res.hopePiece()).isNull();
        then(pieceAcquisitionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("다른 목표로 이미 희망 조각을 받음 → 이 목표 처음 4/4 면 지급 · NEW 아님")
    void check_otherGoalReceived() {
        givenCheck(14L, steps.get(3), List.of(11L, 12L, 13L));
        given(pieceAcquisitionRepository.existsByUserIdAndSourceAndGoalId(USER_ID, AcquireSource.GOAL, JOB_ID))
                .willReturn(false);
        given(pieceRepository.findFirstByType(PieceType.GOAL)).willReturn(Optional.of(hope));
        given(pieceAcquisitionRepository.existsByUserIdAndPieceId(USER_ID, 25L)).willReturn(true);

        CheckGoalStepResponse res = service.checkStep(USER_ID, 14L, true);

        assertThat(res.hopePiece().isNew()).isFalse();
        then(pieceAcquisitionRepository).should().save(any(PieceAcquisition.class));
    }

    @Test
    @DisplayName("해제 → 행 삭제 · 수 감소 · 희망 조각 없음")
    void uncheck() {
        givenCheck(12L, steps.get(1), List.of(11L, 12L, 13L, 14L));

        CheckGoalStepResponse res = service.checkStep(USER_ID, 12L, false);

        assertThat(res.checked()).isEqualTo(3);
        assertThat(res.hopePiece()).isNull();
        then(userGoalStepRepository).should().deleteByUserIdAndGoalStepId(USER_ID, 12L);
    }

    @Test
    @DisplayName("이미 체크된 단계에 다시 체크 → 아무것도 안 바꿈")
    void check_alreadyChecked() {
        givenCheck(11L, steps.get(0), List.of(11L));

        CheckGoalStepResponse res = service.checkStep(USER_ID, 11L, true);

        assertThat(res.checked()).isEqualTo(1);
        then(userGoalStepRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("현재 목표에 없는 단계 → 404 NOT_FOUND")
    void check_otherGoalStep() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(goalStepRepository.findById(31L)).willReturn(Optional.of(otherGoalStep));

        assertThatThrownBy(() -> service.checkStep(USER_ID, 31L, true))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
        then(userGoalStepRepository).should(never()).save(any());
    }
}