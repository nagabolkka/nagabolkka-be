package com.nagabolkka.backend.global.seed;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.district.repository.DistrictRepository;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.entity.GoalStep;
import com.nagabolkka.backend.domain.goal.repository.GoalRepository;
import com.nagabolkka.backend.domain.goal.repository.GoalStepRepository;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.MissionStep;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.mission.repository.MissionStepRepository;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.repository.RegionRepository;
import com.nagabolkka.backend.global.common.Difficulty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static com.nagabolkka.backend.domain.piece.entity.PieceType.CHALLENGE;
import static com.nagabolkka.backend.domain.piece.entity.PieceType.EASY;
import static com.nagabolkka.backend.domain.piece.entity.PieceType.GOAL;
import static com.nagabolkka.backend.domain.piece.entity.PieceType.NORMAL;
import static com.nagabolkka.backend.domain.piece.entity.PieceType.PRACTICE;
import static com.nagabolkka.backend.domain.piece.entity.PieceType.RAINY;
import static com.nagabolkka.backend.domain.piece.entity.PieceType.REFLECTION;
import static com.nagabolkka.backend.domain.piece.entity.PieceType.SAVED_PLACE;

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

    private record PieceSeed(int stageNo, int slotNo, String name, PieceType type,
                             String restoreText, String hint) {
    }

    // slotNo = 보상 조각 칸 (지역 · 칸으로 조각을 찾는다)
    private record MissionSeed(int stageNo, int slotNo, String title, String description,
                               Difficulty difficulty, boolean indoor, boolean rainyOk,
                               String placeName, int estMinutes, List<String> steps) {
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

    // 도감 25칸 (이름 · 복원 문구 · 힌트 = 프론트 mocks/missions.js 와 같게)
    private static final List<PieceSeed> PIECES = List.of(
            // 1 · 시작의 방
            new PieceSeed(1, 1, "햇살 조각", EASY, "하늘에 색이 돌아왔어", "창문 열고 바깥 공기 3분 마시기"),
            new PieceSeed(1, 2, "아침 조각", NORMAL, "창밖에 아침 햇살이 들어왔어", "오전 10시 전에 일어나 물 한 잔"),
            new PieceSeed(1, 3, "문턱 조각", CHALLENGE, "방문 틈으로 빛이 새어 들어와", "현관문 열고 햇빛 10분 쬐기"),
            new PieceSeed(1, 4, "빗소리 조각", RAINY, "창가에 빗소리가 맴돌아", "비 오는 날 창가에서 빗소리 듣기"),
            new PieceSeed(1, 5, "쉼의 조각", REFLECTION, "방 안에 따뜻한 불이 켜졌어", "시작의 방 미션을 마치고 한 줄 회고 남기기"),
            // 2 · 바람의 숲
            new PieceSeed(2, 1, "첫걸음 조각", EASY, "숲길에 발자국이 생겼어", "집 앞 골목 한 바퀴, 10분 산책"),
            new PieceSeed(2, 2, "바람 조각", NORMAL, "나뭇잎이 바람에 흔들려", "편의점에서 음료 하나 사오기"),
            new PieceSeed(2, 3, "향기 조각", CHALLENGE, "숲에 꽃향기가 퍼졌어", "동네 빵집에서 빵 하나 고르기"),
            new PieceSeed(2, 4, "빗방울 조각", RAINY, "잎사귀에 빗방울이 반짝여", "비 오는 날 우산 쓰고 현관 앞까지"),
            new PieceSeed(2, 5, "초록 조각", SAVED_PLACE, "나무가 다시 초록빛이 됐어", "동네 지도에서 담은 장소 다녀오기"),
            // 3 · 발견의 마을
            new PieceSeed(3, 1, "자연 조각", EASY, "마을 공원에 꽃이 피었어", "공원 벤치에 15분 앉아 있기"),
            new PieceSeed(3, 2, "이야기 조각", NORMAL, "마을 도서관에 불이 켜졌어", "도서관에서 책 한 권 빌려보기"),
            new PieceSeed(3, 3, "따뜻함 조각", CHALLENGE, "카페 굴뚝에 연기가 피어올라", "카페에서 음료 주문해 20분 머물기"),
            new PieceSeed(3, 4, "고요 조각", RAINY, "비 오는 마을이 고요해졌어", "비 오는 날 도서관 열람실에서 머물기"),
            new PieceSeed(3, 5, "탐험 조각", SAVED_PLACE, "골목 끝에 새 길이 보여", "동네 지도에서 담은 장소 다녀오기"),
            // 4 · 만남의 정원
            new PieceSeed(4, 1, "귀기울임 조각", EASY, "정원에 작은 새가 노래해", "북토크 뒷자리에서 듣기만 하기"),
            new PieceSeed(4, 2, "용기 조각", NORMAL, "정원 문이 활짝 열렸어", "원데이 클래스 하나 신청하기"),
            new PieceSeed(4, 3, "인사 조각", CHALLENGE, "벤치에서 누군가 손을 흔들어", "프로그램에서 옆 사람에게 인사 한 번"),
            new PieceSeed(4, 4, "다정 조각", RAINY, "정원에 무지개가 떴어", "비 오는 날 실내 원데이 클래스 둘러보기"),
            new PieceSeed(4, 5, "연습 조각", PRACTICE, "정원에 등불 하나가 켜졌어", "연습실 '첫 방문' 끝까지 해보기"),
            // 5 · 새로운 세상
            new PieceSeed(5, 1, "발걸음 조각", EASY, "다리 위로 길이 이어졌어", "청년센터 라운지 들러보기"),
            new PieceSeed(5, 2, "나눔 조각", NORMAL, "거리에 꽃 화분이 놓였어", "봉사활동 1회 신청하기"),
            new PieceSeed(5, 3, "도전 조각", CHALLENGE, "높은 탑에 불이 켜졌어", "알바 공고에 문의 전화 걸기"),
            new PieceSeed(5, 4, "자신감 조각", PRACTICE, "하늘에 별 하나가 떠올랐어", "연습실 '첫 전화' 또는 '모의 면접' 완료"),
            new PieceSeed(5, 5, "희망 조각", GOAL, "새로운 세상에 아침이 밝았어", "내 목표 준비 4단계 모두 완료하기")
    );

    // 오늘 퀘스트로 배정되는 미션 19개 (칸 1~3 · 1~4계단 칸 4 빗소리). 칸 5 · 연습 · 목표 조각은 다른 API 에서 지급
    private static final List<MissionSeed> MISSIONS = List.of(
            // 1 · 시작의 방
            new MissionSeed(1, 1, "창문 열고 바깥 공기 3분 마시기",
                    "창문을 활짝 열고 바깥 소리를 들어봐요. 오늘 공기가 어떤지 느껴보는 것만으로 충분해요.",
                    Difficulty.EASY, true, false, "내 방 창가", 3,
                    List.of("커튼 걷기", "창문 반만 열기", "숨 세 번 크게 쉬기")),
            new MissionSeed(1, 2, "오전 10시 전에 일어나 물 한 잔",
                    "알람 하나만 맞춰둬요. 일어나서 부엌까지 걸어가 물 한 잔 마시면 퀘스트 끝이에요.",
                    Difficulty.NORMAL, true, false, "부엌", 5,
                    List.of("알람 하나 맞춰두기", "알람이 울리면 일어나 앉기", "부엌까지 걸어가 물 마시기")),
            new MissionSeed(1, 3, "현관문 열고 햇빛 10분 쬐기",
                    "신발을 신고 현관문을 열어 문 앞에 서 있어 봐요. 멀리 가지 않아도 괜찮아요.",
                    Difficulty.CHALLENGE, false, false, "현관 앞", 10,
                    List.of("신발 신어보기", "현관문 열어보기", "문 앞에 1분 서 있기")),
            new MissionSeed(1, 4, "창가에서 빗소리 5분 듣기",
                    "비 오는 날엔 창문을 살짝만 열고 빗소리를 들어봐요. 비 오는 날에만 찾을 수 있는 조각이에요.",
                    Difficulty.EASY, true, true, "내 방 창가", 5,
                    List.of("커튼 걷기", "창문 살짝 열기", "눈 감고 빗소리 듣기")),
            // 2 · 바람의 숲
            new MissionSeed(2, 1, "집 앞 골목 한 바퀴, 10분 산책",
                    "정해진 길로 골목 끝까지 갔다 와요. 이어폰을 챙기면 조금 더 편해요.",
                    Difficulty.EASY, false, false, "집 앞 골목", 10,
                    List.of("신발 신고 현관 나서기", "골목 입구까지 가보기", "골목 끝 찍고 돌아오기")),
            new MissionSeed(2, 2, "편의점에서 음료 하나 사오기",
                    "좋아하는 음료 하나만 골라 계산하고 와요. 셀프 계산대가 있는 곳이에요.",
                    Difficulty.NORMAL, false, false, "가까운 편의점", 15,
                    List.of("편의점 위치 확인하기", "편의점 앞까지 가보기", "음료 하나 골라 계산하기")),
            new MissionSeed(2, 3, "동네 빵집에서 빵 하나 고르기",
                    "오전 11시쯤이 가장 한산해요. 빵 하나 고르고, 계산하고, 돌아오면 돼요.",
                    Difficulty.CHALLENGE, false, false, "모퉁이 빵집", 20,
                    List.of("빵집 앞 지나가 보기", "문 열고 들어가 둘러보기", "빵 하나 골라 계산하기")),
            new MissionSeed(2, 4, "우산 쓰고 현관 앞까지 나가보기",
                    "비 오는 날엔 멀리 가지 않아도 돼요. 우산을 펴고 현관 앞에서 빗소리를 들어봐요.",
                    Difficulty.EASY, false, true, "현관 앞", 5,
                    List.of("우산 꺼내두기", "신발 신고 현관 열기", "우산 쓰고 1분 서 있기")),
            // 3 · 발견의 마을
            new MissionSeed(3, 1, "공원 벤치에 15분 앉아 있기",
                    "사람이 적은 벤치를 골라 앉아요. 아무것도 안 해도 돼요.",
                    Difficulty.EASY, false, false, "개운산 근린공원 입구", 30,
                    List.of("공원 입구까지 가보기", "빈 벤치 찾아보기", "5분만 앉아 있기")),
            new MissionSeed(3, 2, "도서관에서 책 한 권 빌려보기",
                    "종암동 주민센터 4·5층에 있는 작은 도서관이에요. 아무 책이나 한 권이면 충분해요.",
                    Difficulty.NORMAL, false, false, "종암동새날도서관", 40,
                    List.of("도서관 입구까지 가보기", "서가 한 줄 둘러보기", "책 한 권 빌리기")),
            new MissionSeed(3, 3, "카페에서 음료 주문해 20분 머물기",
                    "창가 자리가 있는 작은 카페예요. 주문은 키오스크로도 할 수 있어요.",
                    Difficulty.CHALLENGE, false, false, "조용한 동네 카페", 40,
                    List.of("카페 메뉴 미리 보기", "키오스크로 주문하기", "10분만 앉아 있기")),
            new MissionSeed(3, 4, "도서관 열람실에서 30분 머물기",
                    "비 오는 날엔 조용한 실내가 좋아요. 열람실 창가에서 빗소리를 들으며 쉬다 와요.",
                    Difficulty.NORMAL, false, true, "종암동새날도서관 열람실", 40,
                    List.of("우산 챙겨 나가기", "열람실 빈자리 찾기", "30분 머물러 보기")),
            // 4 · 만남의 정원
            new MissionSeed(4, 1, "북토크 뒷자리에서 듣기만 하기",
                    "말하지 않아도 되는 북토크예요. 뒷자리에 앉아 듣기만 해도 충분해요.",
                    Difficulty.EASY, false, false, "성북정보도서관", 90,
                    List.of("북토크 일정 확인하기", "강연실 입구까지 가보기", "뒷자리에서 20분 듣기")),
            new MissionSeed(4, 2, "원데이 클래스 하나 신청하기",
                    "신청까지만 해요. 클래스에 가는 건 다음 퀘스트예요.",
                    Difficulty.NORMAL, false, false, "서울청년센터 성북", 10,
                    List.of("클래스 목록 둘러보기", "마음에 드는 것 하나 고르기", "신청 버튼 누르기")),
            new MissionSeed(4, 3, "프로그램에서 옆 사람에게 인사 한 번",
                    "“안녕하세요” 한 마디면 돼요. 연습실에서 먼저 연습해봐도 좋아요.",
                    Difficulty.CHALLENGE, false, false, "서울청년센터 성북", 120,
                    List.of("연습실에서 인사 연습하기", "프로그램 자리에 앉아보기", "눈인사 한 번 하기")),
            new MissionSeed(4, 4, "실내 원데이 클래스 둘러보기",
                    "비 오는 날엔 실내 프로그램을 구경만 해도 괜찮아요. 참여하지 않아도 돼요.",
                    Difficulty.NORMAL, false, true, "서울청년센터 성북", 30,
                    List.of("클래스 일정 확인하기", "센터 2층까지 가보기", "클래스 하나 구경하기")),
            // 5 · 새로운 세상 (비 오는 날 = 도전 미션, 실내)
            new MissionSeed(5, 1, "청년센터 라운지 들러보기",
                    "만 19–39세 청년 누구나 예약 없이 쓰는 라운지예요. 1층에서 청년정책 안내 책자 하나 챙겨 오면 완료예요.",
                    Difficulty.EASY, false, false, "서울청년센터 성북 2층 오픈라운지", 30,
                    List.of("운영 시간 확인하기", "센터 입구까지 가보기", "안내 책자 하나 챙기기")),
            new MissionSeed(5, 2, "봉사활동 1회 신청하기",
                    "동네에서 하는 2시간짜리 봉사를 골라 신청까지만 해요. 봉사 시간도 인정돼요.",
                    Difficulty.NORMAL, true, false, "성북구자원봉사센터 (온라인 신청)", 15,
                    List.of("봉사 공고 읽어보기", "날짜 하나 골라보기", "신청서 제출하기")),
            new MissionSeed(5, 3, "알바 공고에 문의 전화 걸기",
                    "연습실에서 ‘첫 전화’를 먼저 연습해봐도 좋아요.",
                    Difficulty.CHALLENGE, true, true, "동네 카페 공고", 5,
                    List.of("물어볼 말 한 줄 적기", "연습실에서 한 번 연습하기", "전화 걸기"))
    );

    private final DistrictRepository districtRepository;
    private final RegionRepository regionRepository;
    private final GoalRepository goalRepository;
    private final GoalStepRepository goalStepRepository;
    private final PieceRepository pieceRepository;
    private final MissionRepository missionRepository;
    private final MissionStepRepository missionStepRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDistricts();
        seedRegions();
        seedGoals();
        seedPieces();
        seedMissions();
        log.info("시드 데이터 확인 완료 (구 {} · 지역 {} · 목표 {} · 조각 {} · 미션 {})",
                districtRepository.count(), regionRepository.count(), goalRepository.count(),
                pieceRepository.count(), missionRepository.count());
    }

    // 성북구 1행 (MVP)
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

    // 지역 · 칸이 이미 있으면 건너뜀
    private void seedPieces() {
        for (PieceSeed seed : PIECES) {
            Region region = findRegion(seed.stageNo());
            if (pieceRepository.findByRegionIdAndSlotNo(region.getId(), seed.slotNo()).isPresent()) {
                continue;
            }
            pieceRepository.save(Piece.of(region, seed.slotNo(), seed.name(), seed.type(),
                    seed.restoreText(), seed.hint()));
        }
    }

    // 보상 조각에 이미 미션이 있으면 건너뜀
    private void seedMissions() {
        for (MissionSeed seed : MISSIONS) {
            Region region = findRegion(seed.stageNo());
            Piece piece = pieceRepository.findByRegionIdAndSlotNo(region.getId(), seed.slotNo())
                    .orElseThrow(() -> new IllegalStateException(
                            "조각 시드가 없어요: " + seed.stageNo() + "계단 " + seed.slotNo() + "칸"));
            if (missionRepository.existsByRewardPieceId(piece.getId())) {
                continue;
            }
            Mission mission = missionRepository.save(Mission.of(region, piece, seed.title(), seed.description(),
                    seed.difficulty(), seed.indoor(), seed.rainyOk(), seed.placeName(), seed.estMinutes()));
            missionStepRepository.saveAll(IntStream.range(0, seed.steps().size())
                    .mapToObj(i -> MissionStep.of(mission, i + 1, seed.steps().get(i)))
                    .toList());
        }
    }

    private Region findRegion(int stageNo) {
        return regionRepository.findByStageNo(stageNo)
                .orElseThrow(() -> new IllegalStateException("지역 시드가 없어요: " + stageNo));
    }
}