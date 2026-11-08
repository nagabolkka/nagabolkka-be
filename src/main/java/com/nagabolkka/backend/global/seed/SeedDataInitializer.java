package com.nagabolkka.backend.global.seed;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.district.repository.DistrictRepository;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.entity.GoalStep;
import com.nagabolkka.backend.domain.goal.repository.GoalRepository;
import com.nagabolkka.backend.domain.goal.repository.GoalStepRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    private static final String IMAGE_BASE = "/images/regions/";

    private record RegionSeed(int stageNo, String name, String realLabel, String pieceFamily,
                              String color, String imageName) {
    }

    private record GoalSeed(String code, String name, List<String> steps) {
    }

    private static final List<RegionSeed> REGIONS = List.of(
            new RegionSeed(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "1-start-room"),
            new RegionSeed(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "2-wind-forest"),
            new RegionSeed(3, "발견의 마을", "동네", "발견의 조각", "#7FCDE6", "3-discovery-village"),
            new RegionSeed(4, "만남의 정원", "사람 곁", "마음의 조각", "#F2A7C3", "4-meeting-garden"),
            new RegionSeed(5, "새로운 세상", "사회 연결", "희망의 조각", "#BFB2F2", "5-new-world")
    );

    private static final List<GoalSeed> GOALS = List.of(
            new GoalSeed("JOB", "취업", List.of(
                    "이력서 양식 하나 내려받기",
                    "이력서에 이름 · 연락처 채워 보기",
                    "채용 공고 3개 둘러보기",
                    "관심 가는 공고 하나 저장하기")),
            new GoalSeed("PART_TIME", "알바", List.of(
                    "알바 공고 앱 둘러보기",
                    "집 근처 공고 3개 읽어 보기",
                    "해볼 만한 공고 하나 저장하기",
                    "지원 문의 문장 한 줄 써 보기")),
            new GoalSeed("MEETUP", "모임", List.of(
                    "관심 있는 취미 하나 적어 보기",
                    "동네 모임 · 소모임 둘러보기",
                    "구경만 해도 되는 모임 찾기",
                    "모임 참여 신청해 보기")),
            new GoalSeed("VOLUNTEER", "봉사", List.of(
                    "1365 자원봉사포털 둘러보기",
                    "혼자 할 수 있는 봉사 찾기",
                    "가능한 날짜 하나 골라 보기",
                    "봉사 신청서 제출하기")),
            new GoalSeed("SCHOOL", "학업 복귀", List.of(
                    "다시 배우고 싶은 것 적어 보기",
                    "학교 · 강의 정보 찾아보기",
                    "입학 · 복학 일정 확인하기",
                    "상담 문의 한 번 해 보기"))
    );

    private final DistrictRepository districtRepository;
    private final RegionRepository regionRepository;
    private final GoalRepository goalRepository;
    private final GoalStepRepository goalStepRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDistricts();
        seedRegions();
        seedGoals();
        log.info("시드 데이터 확인 완료 (구 {} · 지역 {} · 목표 {})",
                districtRepository.count(), regionRepository.count(), goalRepository.count());
    }

    // 성북구 1행 (MVP). 구를 늘릴 때는 여기에 추가
    private void seedDistricts() {
        if (districtRepository.count() > 0) {
            return;
        }
        districtRepository.save(District.of("성북구",
                new BigDecimal("37.5894000"), new BigDecimal("127.0167000"), 61, 127));
    }

    private void seedRegions() {
        for (RegionSeed seed : REGIONS) {
            if (regionRepository.existsByStageNo(seed.stageNo())) {
                continue;
            }
            regionRepository.save(Region.of(
                    seed.stageNo(), seed.name(), seed.realLabel(), seed.pieceFamily(), seed.color(),
                    IMAGE_BASE + seed.imageName() + ".png",
                    IMAGE_BASE + seed.imageName() + "-rain.png"));
        }
    }

    private void seedGoals() {
        for (GoalSeed seed : GOALS) {
            if (goalRepository.existsByCode(seed.code())) {
                continue;
            }
            Goal goal = goalRepository.save(Goal.of(seed.code(), seed.name()));
            goalStepRepository.saveAll(IntStream.range(0, seed.steps().size())
                    .mapToObj(i -> GoalStep.of(goal, i + 1, seed.steps().get(i)))
                    .toList());
        }
    }
}