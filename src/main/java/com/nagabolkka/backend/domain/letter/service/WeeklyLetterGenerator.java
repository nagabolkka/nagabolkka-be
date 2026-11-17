package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.letter.entity.WeeklyLetter;
import com.nagabolkka.backend.domain.letter.repository.WeeklyLetterRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.repository.QuestRecordRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyLetterGenerator {

    static final int RECOMMEND_COUNT = 2;
    static final int RECOMMEND_POOL = 6; // 가까운 6곳 안에서 주마다 돌아가며 2곳

    private final UserRepository userRepository;
    private final WeeklyLetterRepository weeklyLetterRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final QuestRecordRepository questRecordRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;
    private final UserRegionRepository userRegionRepository;
    private final PlaceRepository placeRepository;
    private final AiClient aiClient;
    private final Clock clock;

    public record Result(LocalDate weekStart, int created, int skipped, int failed) {
    }

    // 트랜잭션 없음 : 사용자마다 따로 저장 (한 명 실패해도 다음 사람 · AI 기다리는 동안 잠금 없음)
    public Result generate(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<User> targets = userRepository.findLetterTargets(Role.USER, weekEnd.plusDays(1).atStartOfDay());

        int created = 0;
        int skipped = 0;
        int failed = 0;
        for (User user : targets) {
            try {
                if (weeklyLetterRepository.existsByUserIdAndWeekStart(user.getId(), weekStart)) {
                    skipped++;
                    continue;
                }
                LetterStats stats = stats(user, weekStart, weekEnd);
                weeklyLetterRepository.save(WeeklyLetter.of(user, weekStart, stats, body(user, stats),
                        recommend(user, weekStart)));
                created++;
            } catch (DataIntegrityViolationException e) {
                skipped++; // 동시에 돈 다른 실행이 먼저 저장 (유니크 제약)
            } catch (RuntimeException e) {
                failed++;
                log.warn("편지 생성 실패 user={} : {}", user.getId(), e.getClass().getSimpleName());
            }
        }
        log.info("주간 편지 {} : 생성 {} · 건너뜀 {} · 실패 {}", weekStart, created, skipped, failed);
        return new Result(weekStart, created, skipped, failed);
    }

    LetterStats stats(User user, LocalDate from, LocalDate to) {
        Long userId = user.getId();
        int outing = (int) dailyQuestRepository.countByUserIdAndStatusAndQuestDateBetween(
                userId, QuestStatus.COMPLETED, from, to);
        int split = (int) dailyQuestRepository.countByUserIdAndStatusAndSplitTrueAndQuestDateBetween(
                userId, QuestStatus.COMPLETED, from, to);
        Double mood = outing == 0 ? null : questRecordRepository.averageMood(userId, from, to);

        Region region = user.getCurrentRegion();
        int restoration = userRegionRepository.findByUserIdOrderByRegionStageNoAsc(userId).stream()
                .filter(ur -> Objects.equals(ur.getRegion().getId(), region.getId()))
                .findFirst()
                .map(UserRegion::getRestoration)
                .orElse(0);

        return new LetterStats(outing, split,
                (int) pieceAcquisitionRepository.countByUserId(userId),
                (int) pieceAcquisitionRepository.countFoundSlots(userId),
                region.getName(), restoration, mood);
    }

    // 쉬어간 주는 템플릿 (AI 비용 아낌) · 아니면 AI → 실패하면 템플릿
    String body(User user, LetterStats stats) {
        if (stats.outingCount() == 0) {
            return LetterTemplate.of(stats);
        }
        return aiClient.weeklyLetter(new AiClient.LetterRequest(user.getNickname(), stats.outingCount(),
                        stats.splitCount(), stats.pieceCount(), stats.foundSlots(), stats.regionName(),
                        stats.restoration(), stats.moodAverage()))
                .map(AiClient.LetterResult::body)
                .filter(body -> body != null && !body.isBlank())
                .map(String::strip)
                .orElseGet(() -> LetterTemplate.of(stats));
    }

    // 내 구 · 지금 열린 · 혼자 OK 장소 → 구 중심에서 가까운 6곳 중 주마다 돌아가며 2곳
    List<Long> recommend(User user, LocalDate weekStart) {
        District district = user.getDistrict();
        if (district == null) {
            return List.of();
        }
        GeoPoint center = GeoPoint.of(district);
        LocalDate today = LocalDate.now(clock);
        List<Place> pool = placeRepository.findActiveByDistrictId(district.getId()).stream()
                .filter(place -> place.isOpenOn(today))
                .filter(Place::isSoloOk)
                .sorted(Comparator.comparingInt((Place place) -> center.distanceTo(place.point()))
                        .thenComparing(Place::getId))
                .limit(RECOMMEND_POOL)
                .toList();
        if (pool.isEmpty()) {
            return List.of();
        }
        int offset = (int) ((weekStart.toEpochDay() / 7 * RECOMMEND_COUNT) % pool.size());
        return IntStream.range(0, Math.min(RECOMMEND_COUNT, pool.size()))
                .mapToObj(i -> pool.get((offset + i) % pool.size()).getId())
                .toList();
    }
}