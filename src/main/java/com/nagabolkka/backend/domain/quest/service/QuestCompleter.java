package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.piece.entity.AcquireSource;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.NextRegion;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.PieceItem;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.Restoration;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestRecord;
import com.nagabolkka.backend.domain.quest.repository.QuestRecordRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.region.service.NextRegionPolicy;
import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import com.nagabolkka.backend.domain.safety.entity.CrisisSource;
import com.nagabolkka.backend.domain.safety.repository.CrisisEventRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.crypto.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 퀘스트 완료의 DB 부분 (AI 호출은 QuestCompleteService 가 트랜잭션 밖에서)
@Component
@RequiredArgsConstructor
public class QuestCompleter {

    // 쉼의 조각 = 1계단 미션을 완료하면서 처음 회고를 남겼을 때
    static final int REST_PIECE_STAGE = 1;

    private final QuestService questService;
    private final QuestRecordRepository questRecordRepository;
    private final CrisisEventRepository crisisEventRepository;
    private final PieceRepository pieceRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;
    private final UserRegionRepository userRegionRepository;
    private final NextRegionPolicy nextRegionPolicy;
    private final AesUtil aesUtil;
    private final Clock clock;

    // 응답 재료 + 루미 한마디 재료
    public record Result(
            Long recordId,
            List<PieceItem> pieces,
            Restoration restoration,
            Difficulty nextDifficulty,
            NextRegion nextRegion,
            String nickname,
            String missionTitle,
            String pieceName,
            String restoreText
    ) {
    }

    @Transactional
    public Result complete(Long userId, int mood, int doneSteps, String reflection, boolean crisis) {
        // 1) 사용자 잠금 + 시작 체크 확인 → 버튼 연타 두 번째는 아래 complete 에서 409
        User user = questService.lockOnboardedUser(userId);
        LocalDateTime now = LocalDateTime.now(clock);

        // 2) 오늘 퀘스트 (없으면 배정 — 상세를 안 거치고 바로 와도 같은 퀘스트)
        DailyQuest quest = questService.findOrAssign(user, now.toLocalDate());
        quest.complete(doneSteps, now);

        // 3) 기록 : 위기면 회고 저장 안 함 + 위기 기록 (시각만)
        String savedReflection = crisis ? null : reflection;
        byte[] reflectionEnc = savedReflection == null ? null : aesUtil.encrypt(savedReflection);
        QuestRecord record = questRecordRepository.save(QuestRecord.of(quest, mood, reflectionEnc));
        if (crisis) {
            crisisEventRepository.save(CrisisEvent.of(user, CrisisSource.REFLECTION));
        }

        // 4) 조각 : 미션 보상 + (1계단 · 회고 있음 · 위기 아님 · 처음) 쉼의 조각
        List<PieceItem> pieces = new ArrayList<>();
        Piece reward = quest.getMission().getRewardPiece();
        pieces.add(acquire(user, reward, AcquireSource.QUEST, quest));

        if (savedReflection != null && quest.getRegion().getStageNo() == REST_PIECE_STAGE) {
            pieceRepository.findFirstByType(PieceType.REFLECTION)
                    .filter(rest -> !pieceAcquisitionRepository.existsByUserIdAndPieceId(userId, rest.getId()))
                    .ifPresent(rest -> pieces.add(acquire(user, rest, AcquireSource.REFLECTION, quest)));
        }

        // 5) 복원도 : 퀘스트 지역 (지나온 계단 미션이면 그 계단) +1번
        List<UserRegion> userRegions = userRegionRepository.findByUserIdOrderByRegionStageNoAsc(userId);
        Region questRegion = quest.getRegion();
        UserRegion target = findUserRegion(userRegions, questRegion.getId());
        int before = target.getRestoration();
        target.addCompletion();
        Restoration restoration = new Restoration(questRegion.getStageNo(), questRegion.getName(),
                before, target.getRestoration());

        // 6) 다음 지역 제안 : 현재 지역 기준 (홈과 같은 규칙)
        UserRegion current = findUserRegion(userRegions, user.getCurrentRegion().getId());
        Region next = nextRegionPolicy.suggest(current, userRegions, now);

        return new Result(
                record.getId(),
                pieces,
                restoration,
                user.getDifficulty(), // TODO TDD : 다음 날 난이도 계산
                next == null ? null : new NextRegion(next.getStageNo(), next.getName()),
                user.getNickname(),
                quest.getMission().getTitle(),
                reward.getName(),
                reward.getRestoreText()
        );
    }

    @Transactional
    public void saveLumiMessage(Long recordId, String message) {
        questRecordRepository.findById(recordId)
                .ifPresent(record -> record.saveLumiMessage(message));
    }

    // NEW 여부는 저장 "전에" 확인 (저장 후 확인하면 항상 false)
    private PieceItem acquire(User user, Piece piece, AcquireSource source, DailyQuest quest) {
        boolean isNew = !pieceAcquisitionRepository.existsByUserIdAndPieceId(user.getId(), piece.getId());
        pieceAcquisitionRepository.save(PieceAcquisition.of(user, piece, source, quest));
        return new PieceItem(piece.getId(), piece.getName(), piece.getSlotNo(), isNew);
    }

    private static UserRegion findUserRegion(List<UserRegion> userRegions, Long regionId) {
        return userRegions.stream()
                .filter(ur -> Objects.equals(ur.getRegion().getId(), regionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지역 행이 없어요: region " + regionId)); // 데이터 오류 → 500
    }
}