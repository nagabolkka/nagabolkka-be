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
                    List.of("물어볼 말 한 줄 적기", "연습실에서 한 번 연습하기", "전화 걸기")),
            // 1 · 시작의 방 (추가)
            new MissionSeed(1, 1, "이불 개고 침대 위 정리하기",
                                    "이불을 한 번 털어서 접어 두기만 해요. 방이 조금 넓어 보일 거예요.",
                            Difficulty.EASY, true, false, "내 방", 5,
                            List.of("베개 제자리에 두기", "이불 펼쳐 털기", "반으로 접어 두기")),
            new MissionSeed(1, 1, "좋아하는 노래 한 곡 끝까지 듣기",
                                    "편한 자리에 앉아 노래 한 곡만 끝까지 들어봐요. 다른 건 안 해도 돼요.",
                            Difficulty.EASY, true, false, "내 방", 4,
                            List.of("노래 한 곡 고르기", "편한 자리에 앉기", "끝까지 들어보기")),
            new MissionSeed(1, 1, "세수하고 로션 바르기",
                                    "미지근한 물로 세수만 해도 얼굴이 가벼워져요.",
                            Difficulty.EASY, true, false, "욕실", 5,
                            List.of("욕실 불 켜기", "미지근한 물로 세수하기", "로션 바르기")),
            new MissionSeed(1, 1, "창밖 풍경 사진 한 장 찍기",
                                    "창밖 하늘이나 나무를 한 장 찍어 둬요. 오늘의 바깥을 기록하는 거예요.",
                            Difficulty.EASY, true, false, "내 방 창가", 3,
                            List.of("휴대폰 카메라 켜기", "창밖 바라보기", "사진 한 장 찍기")),
            new MissionSeed(1, 2, "샤워하고 새 옷으로 갈아입기",
                                    "씻고 깨끗한 옷으로 갈아입는 것까지예요. 외출복이 아니어도 괜찮아요.",
                            Difficulty.NORMAL, true, false, "욕실", 20,
                            List.of("갈아입을 옷 꺼내두기", "샤워하기", "새 옷으로 갈아입기")),
            new MissionSeed(1, 2, "간단한 아침 차려 먹기",
                                    "빵이나 시리얼처럼 간단한 것도 좋아요. 식탁에 앉아서 먹어봐요.",
                            Difficulty.NORMAL, true, false, "부엌", 15,
                            List.of("먹을 것 하나 고르기", "그릇에 담기", "식탁에 앉아 먹기")),
            new MissionSeed(1, 2, "방 바닥 10분 정리하기",
                                    "타이머 10분만 맞추고, 울리면 거기서 멈춰도 돼요.",
                            Difficulty.NORMAL, true, false, "내 방", 10,
                            List.of("타이머 10분 맞추기", "바닥 물건 한곳에 모으기", "쓰레기 버리기")),
            new MissionSeed(1, 2, "가까운 사람에게 안부 메시지 하나 보내기",
                                    "“잘 지내?” 한 줄이면 충분해요. 답장은 기다리지 않아도 돼요.",
                            Difficulty.NORMAL, true, false, "내 방", 5,
                            List.of("보낼 사람 한 명 떠올리기", "한 줄 적어보기", "보내기 누르기")),
            new MissionSeed(1, 3, "쓰레기 들고 현관 밖까지 나가기",
                                    "쓰레기 봉투 하나 들고 현관 밖 버리는 곳까지만 가요.",
                            Difficulty.CHALLENGE, false, false, "현관 밖", 5,
                            List.of("쓰레기 봉투 묶기", "신발 신기", "버리는 곳에 두고 오기")),
            new MissionSeed(1, 3, "1층 우편함 확인하고 오기",
                                    "엘리베이터나 계단으로 1층까지 내려가 우편함만 열어보고 와요.",
                            Difficulty.CHALLENGE, false, false, "1층 우편함", 5,
                            List.of("신발 신기", "1층까지 내려가기", "우편함 열어보기")),
            new MissionSeed(1, 3, "외출복 입고 현관에서 신발 신어보기",
                                    "나가지 않아도 돼요. 외출할 때처럼 입고 신발까지 신어보는 연습이에요.",
                            Difficulty.CHALLENGE, true, false, "현관", 5,
                            List.of("외출복으로 갈아입기", "현관까지 가기", "신발 신고 1분 서 있기")),
            new MissionSeed(1, 3, "복도 끝까지 걸어갔다 오기",
                                    "현관문을 열고 복도 끝까지만 갔다 와요. 아무도 없을 때 해도 괜찮아요.",
                            Difficulty.CHALLENGE, false, false, "우리 집 복도", 5,
                            List.of("신발 신기", "현관문 열기", "복도 끝 찍고 돌아오기")),
            // 2 · 바람의 숲 (추가)
            new MissionSeed(2, 1, "집 앞에서 하늘 사진 한 장 찍기",
                                    "문 밖에 나가 하늘을 올려다보고 한 장 찍어 와요.",
                            Difficulty.EASY, false, false, "집 앞", 5,
                            List.of("신발 신고 나가기", "하늘 올려다보기", "사진 한 장 찍기")),
            new MissionSeed(2, 1, "우리 건물 한 바퀴 돌기",
                                    "건물 둘레를 천천히 한 바퀴만 걸어요. 길을 잃을 걱정이 없어요.",
                            Difficulty.EASY, false, false, "우리 건물 주변", 10,
                            List.of("현관 나서기", "건물 모퉁이까지 가기", "한 바퀴 돌아오기")),
            new MissionSeed(2, 1, "집 앞 쉼터 벤치에 5분 앉기",
                                    "놀이터나 쉼터 벤치에 앉아 바람을 느껴봐요.",
                            Difficulty.EASY, false, false, "집 앞 쉼터", 10,
                            List.of("벤치 위치 떠올리기", "벤치까지 가기", "5분 앉아 있기")),
            new MissionSeed(2, 1, "분리수거 하고 한 바퀴 돌아오기",
                                    "분리수거를 버린 김에 건물 주변을 한 바퀴 걸어요.",
                            Difficulty.EASY, false, false, "분리수거장", 10,
                            List.of("분리수거 모아 들기", "분리수거장까지 가기", "한 바퀴 돌아오기")),
            new MissionSeed(2, 2, "생활용품점에서 필요한 것 하나 사기",
                                    "휴지나 칫솔처럼 필요한 것 하나만 정해서 사 와요.",
                            Difficulty.NORMAL, false, false, "동네 생활용품점", 20,
                            List.of("살 것 하나 정하기", "가게 앞까지 가기", "골라서 계산하기")),
            new MissionSeed(2, 2, "버스 정류장까지 걸어갔다 오기",
                                    "버스는 안 타도 돼요. 정류장 노선표만 한 번 보고 돌아와요.",
                            Difficulty.NORMAL, false, false, "가까운 버스 정류장", 15,
                            List.of("정류장 위치 확인하기", "정류장까지 걷기", "노선표 보고 돌아오기")),
            new MissionSeed(2, 2, "동네 골목 20분 산책",
                                    "갈 때와 다른 길로 돌아오면 새로운 골목을 볼 수 있어요.",
                            Difficulty.NORMAL, false, false, "동네 골목", 20,
                            List.of("갈 방향 정하기", "10분 걸어가기", "다른 길로 돌아오기")),
            new MissionSeed(2, 2, "무인 가게에서 간식 하나 사기",
                                    "말하지 않아도 되는 무인 가게예요. 간식 하나 골라 계산하고 와요.",
                            Difficulty.NORMAL, false, false, "동네 무인 가게", 15,
                            List.of("가게 위치 확인하기", "들어가서 둘러보기", "하나 골라 계산하기")),
            new MissionSeed(2, 3, "편의점 카운터에서 직접 계산하기",
                                    "이번엔 셀프 계산대 말고 카운터에서 계산해봐요. “봉투는 괜찮아요” 한 마디면 돼요.",
                            Difficulty.CHALLENGE, false, false, "가까운 편의점", 15,
                            List.of("살 것 하나 고르기", "카운터에 올려두기", "한 마디 하고 계산하기")),
            new MissionSeed(2, 3, "분식집에서 포장 주문하기",
                                    "메뉴를 미리 정해 가면 “○○ 포장이요” 한 마디로 끝나요.",
                            Difficulty.CHALLENGE, false, false, "동네 분식집", 20,
                            List.of("메뉴 미리 정하기", "가게 들어가기", "포장 주문하기")),
            new MissionSeed(2, 3, "동네 시장 한 줄 걸어보기",
                                    "사람이 많지 않은 오전에 가 봐요. 과일 하나 사 오면 완료예요.",
                            Difficulty.CHALLENGE, false, false, "동네 시장", 30,
                            List.of("시장 입구까지 가기", "한 줄 따라 걸어보기", "과일 하나 사기")),
            new MissionSeed(2, 3, "카페에서 음료 테이크아웃하기",
                                    "머물지 않고 음료만 받아 나와요. 메뉴를 미리 보고 가면 편해요.",
                            Difficulty.CHALLENGE, false, false, "가까운 카페", 15,
                            List.of("메뉴 미리 보기", "주문하기", "음료 받아 나오기")),
            // 3 · 발견의 마을 (추가)
            new MissionSeed(3, 1, "동네 산책로 20분 걷기",
                                    "정해진 산책로를 따라 걸으면 길 걱정이 없어요.",
                            Difficulty.EASY, false, false, "동네 산책로", 30,
                            List.of("산책로 입구 찾기", "10분 걸어가기", "돌아오기")),
            new MissionSeed(3, 1, "공원에서 꽃이나 나무 사진 세 장 찍기",
                                    "마음에 드는 것 세 가지만 찾아 찍어 와요.",
                            Difficulty.EASY, false, false, "가까운 공원", 30,
                            List.of("공원까지 가기", "한 바퀴 걸으며 찾아보기", "사진 세 장 찍기")),
            new MissionSeed(3, 1, "도서관 입구 게시판 보고 오기",
                                    "들어가지 않아도 돼요. 입구 게시판에 어떤 프로그램이 있는지만 보고 와요.",
                            Difficulty.EASY, false, false, "동네 도서관", 20,
                            List.of("도서관까지 가기", "입구 게시판 보기", "하나 사진 찍기")),
            new MissionSeed(3, 1, "처음 가보는 골목 걸어보기",
                                    "늘 가던 길 옆 골목으로 한 번 들어가 봐요.",
                            Difficulty.EASY, false, false, "동네 골목", 20,
                            List.of("지도에서 골목 하나 고르기", "골목 입구까지 가기", "끝까지 걸어보기")),
            new MissionSeed(3, 2, "도서관에서 30분 책 읽기",
                                    "빌리지 않아도 돼요. 열람실 자리에 앉아 30분 읽다 와요.",
                            Difficulty.NORMAL, false, false, "동네 도서관", 45,
                            List.of("도서관 들어가기", "읽을 책 하나 고르기", "30분 읽기")),
            new MissionSeed(3, 2, "동네 서점이나 문구점 둘러보기",
                                    "마음에 드는 것 하나를 찾아 사진만 찍어 와도 돼요.",
                            Difficulty.NORMAL, false, false, "동네 서점", 30,
                            List.of("가게 위치 확인하기", "들어가서 둘러보기", "마음에 드는 것 사진 찍기")),
            new MissionSeed(3, 2, "혼밥하기 좋은 식당에서 한 끼 먹기",
                                    "1인석이 있거나 키오스크로 주문하는 곳을 골라봐요.",
                            Difficulty.NORMAL, false, false, "동네 식당", 40,
                            List.of("1인석 있는 곳 찾기", "들어가 주문하기", "다 먹고 나오기")),
            new MissionSeed(3, 2, "주민센터 게시판에서 프로그램 하나 찾기",
                                    "동네에서 하는 무료 강좌나 행사를 하나 찾아 메모해 와요.",
                            Difficulty.NORMAL, false, false, "동네 주민센터", 30,
                            List.of("주민센터 위치 확인하기", "게시판 보기", "관심 가는 것 하나 메모하기")),
            new MissionSeed(3, 3, "도서관 사서에게 책 위치 물어보기",
                                    "“이 책 어디 있어요?” 한 마디면 돼요. 찾을 책을 미리 정해 가요.",
                            Difficulty.CHALLENGE, false, false, "동네 도서관", 40,
                            List.of("찾을 책 정하기", "안내데스크 가기", "책 위치 물어보기")),
            new MissionSeed(3, 3, "카페에서 1시간 머물며 할 일 하기",
                                    "책이나 노트 하나 챙겨 가서 1시간 머물러 봐요.",
                            Difficulty.CHALLENGE, false, false, "동네 카페", 70,
                            List.of("할 일 하나 챙기기", "주문하고 자리 잡기", "1시간 머물기")),
            new MissionSeed(3, 3, "공공 체육센터 안내 받아오기",
                                    "안내데스크에서 프로그램 안내지만 받아 와요. 등록은 안 해도 돼요.",
                            Difficulty.CHALLENGE, false, false, "동네 체육센터", 40,
                            List.of("운영 시간 확인하기", "안내데스크까지 가기", "프로그램 안내 받아오기")),
            new MissionSeed(3, 3, "동네 미용실 다녀오기",
                                    "예약은 앱으로 해도 돼요. “다듬어 주세요” 한 마디면 충분해요.",
                            Difficulty.CHALLENGE, false, false, "동네 미용실", 60,
                            List.of("예약하기", "미용실 가기", "원하는 것 한 마디 말하기")),
            // 4 · 만남의 정원 (추가)
            new MissionSeed(4, 1, "온라인 강연 하나 끝까지 보기",
                                    "집에서 보는 강연이에요. 관심 있는 주제 하나면 충분해요.",
                            Difficulty.EASY, true, false, "집 (온라인)", 30,
                            List.of("강연 하나 고르기", "처음 10분 보기", "끝까지 보기")),
            new MissionSeed(4, 1, "동네 행사 · 플리마켓 구경하기",
                                    "사지 않아도 돼요. 한 바퀴 구경만 하고 와요.",
                            Difficulty.EASY, false, false, "동네 행사장", 40,
                            List.of("행사 일정 확인하기", "행사장까지 가기", "한 바퀴 구경하기")),
            new MissionSeed(4, 1, "청년 공간 라운지에서 30분 머물기",
                                    "예약 없이 쓰는 라운지예요. 앉아서 쉬기만 해도 돼요.",
                            Difficulty.EASY, false, false, "청년 공간 라운지", 40,
                            List.of("운영 시간 확인하기", "라운지까지 가기", "30분 머물기")),
            new MissionSeed(4, 1, "소모임 공지 하나 끝까지 읽어보기",
                                    "신청은 안 해도 돼요. 어떤 모임들이 있는지 구경만 해요.",
                            Difficulty.EASY, true, false, "집 (온라인)", 10,
                            List.of("소모임 게시판 열기", "관심 분야 찾기", "공지 하나 끝까지 읽기")),
            new MissionSeed(4, 2, "원데이 클래스 참여하기",
                                    "신청해 둔 클래스에 가서 끝까지 있어 봐요. 잘하지 않아도 괜찮아요.",
                            Difficulty.NORMAL, false, false, "청년 공간", 90,
                            List.of("준비물 확인하기", "시간 맞춰 도착하기", "끝까지 참여하기")),
            new MissionSeed(4, 2, "취미 소모임 하나 신청하기",
                                    "독서 · 산책 · 그림처럼 말이 적은 모임부터 골라봐요.",
                            Difficulty.NORMAL, true, false, "집 (온라인)", 10,
                            List.of("모임 목록 둘러보기", "하나 고르기", "신청하기")),
            new MissionSeed(4, 2, "도서관 프로그램 현장 접수하기",
                                    "안내데스크에서 접수만 하고 와요.",
                            Difficulty.NORMAL, false, false, "동네 도서관", 30,
                            List.of("프로그램 하나 고르기", "안내데스크 가기", "접수하기")),
            new MissionSeed(4, 2, "단체 운동 프로그램 한 번 참여하기",
                                    "맨 뒤에서 따라 하기만 해도 돼요.",
                            Difficulty.NORMAL, false, false, "동네 체육센터", 60,
                            List.of("프로그램 시간 확인하기", "일찍 도착해 자리 잡기", "끝까지 따라 하기")),
            new MissionSeed(4, 3, "모임에서 자기소개 한 문장 하기",
                                    "이름과 한 문장이면 충분해요. 미리 적어 가면 편해요.",
                            Difficulty.CHALLENGE, false, false, "소모임 장소", 90,
                            List.of("자기소개 한 문장 적기", "모임 참석하기", "이름과 한 문장 말하기")),
            new MissionSeed(4, 3, "프로그램 끝나고 진행자에게 질문 하나 하기",
                                    "궁금한 것 하나를 미리 적어 가요. 끝나고 짧게 물어보면 돼요.",
                            Difficulty.CHALLENGE, false, false, "프로그램 장소", 90,
                            List.of("질문 하나 적어두기", "끝까지 참여하기", "진행자에게 물어보기")),
            new MissionSeed(4, 3, "옆 사람에게 질문 하나 건네기",
                                    "“여기 자주 오세요?” 같은 가벼운 질문 하나면 돼요.",
                            Difficulty.CHALLENGE, false, false, "프로그램 장소", 90,
                            List.of("건넬 질문 정하기", "프로그램 참여하기", "쉬는 시간에 말 건네기")),
            new MissionSeed(4, 3, "같은 프로그램 두 번째로 참여하기",
                                    "한 번 가 본 곳은 두 번째가 훨씬 편해요. 아는 얼굴이 있으면 눈인사해요.",
                            Difficulty.CHALLENGE, false, false, "프로그램 장소", 120,
                            List.of("지난 프로그램 다시 찾기", "신청하기", "참여해서 눈인사하기")),
            // 5 · 새로운 세상 (추가)
            new MissionSeed(5, 1, "청년정책 하나 찾아 읽어보기",
                                    "나에게 맞는 지원이 있는지 하나만 끝까지 읽어봐요.",
                            Difficulty.EASY, true, false, "집 (온라인)", 15,
                            List.of("청년정책 사이트 열기", "관심 분야 고르기", "정책 하나 끝까지 읽기")),
            new MissionSeed(5, 1, "구인 공고 세 개 읽어보기",
                                    "지원하지 않아도 돼요. 어떤 일이 있는지 구경만 해요.",
                            Difficulty.EASY, true, false, "집 (온라인)", 15,
                            List.of("구인 사이트 열기", "관심 분야 고르기", "공고 세 개 읽기")),
            new MissionSeed(5, 1, "관심 있는 강좌나 자격증 하나 찾아보기",
                                    "무료 강좌도 많아요. 하나만 찾아서 저장해 둬요.",
                            Difficulty.EASY, true, false, "집 (온라인)", 15,
                            List.of("관심 분야 떠올리기", "강좌 검색하기", "하나 저장해 두기")),
            new MissionSeed(5, 1, "청년 상담 프로그램 찾아보기",
                                    "진로 · 취업 상담을 무료로 해 주는 곳이 있어요. 어디서 하는지만 알아봐요.",
                            Difficulty.EASY, true, false, "집 (온라인)", 15,
                            List.of("청년센터 홈페이지 열기", "상담 메뉴 찾기", "신청 방법 읽어보기")),
            new MissionSeed(5, 2, "봉사활동 1회 다녀오기",
                                    "신청해 둔 봉사에 가서 2시간 함께해요.",
                            Difficulty.NORMAL, false, false, "봉사 장소", 120,
                            List.of("장소 · 시간 다시 확인하기", "일찍 도착하기", "끝까지 함께하기")),
            new MissionSeed(5, 2, "청년 상담 예약하기",
                                    "진로나 취업 상담을 예약만 해 둬요.",
                            Difficulty.NORMAL, true, false, "집 (온라인)", 10,
                            List.of("상담 종류 고르기", "날짜 고르기", "예약하기")),
            new MissionSeed(5, 2, "무료 강좌나 스터디 하나 신청하기",
                                    "온라인으로 듣는 강좌도 괜찮아요. 신청까지만 해요.",
                            Difficulty.NORMAL, true, false, "집 (온라인)", 10,
                            List.of("강좌 목록 둘러보기", "하나 고르기", "신청하기")),
            new MissionSeed(5, 2, "안 쓰는 물건 하나 나눔 올리기",
                                    "쓰지 않는 물건 하나를 필요한 사람에게 나눠봐요.",
                            Difficulty.NORMAL, true, false, "집 (온라인)", 20,
                            List.of("나눌 물건 고르기", "사진 찍기", "나눔 글 올리기")),
            new MissionSeed(5, 3, "예약한 청년 상담 다녀오기",
                                    "묻고 싶은 것 한 가지만 적어 가면 충분해요.",
                            Difficulty.CHALLENGE, false, false, "청년센터", 60,
                            List.of("물어볼 것 하나 적기", "상담 장소 가기", "상담 받기")),
            new MissionSeed(5, 3, "단기 알바나 일일 봉사에 지원서 내기",
                                    "하루짜리부터 시작해봐요. 지원서 제출까지만 해요.",
                            Difficulty.CHALLENGE, true, true, "집 (온라인)", 20,
                            List.of("공고 하나 고르기", "지원서 작성하기", "제출하기")),
            new MissionSeed(5, 3, "관심 있는 곳에 이메일로 문의하기",
                                    "궁금한 것 하나만 짧게 물어봐요. 답장은 기다리기만 하면 돼요.",
                            Difficulty.CHALLENGE, true, true, "집 (온라인)", 20,
                            List.of("문의할 곳 고르기", "질문 두세 줄 쓰기", "이메일 보내기")),
            new MissionSeed(5, 3, "강좌나 자격증 시험 등록하기",
                                    "찾아 둔 것 하나를 실제로 등록해요. 날짜가 생기면 한 걸음 가까워져요.",
                            Difficulty.CHALLENGE, true, true, "집 (온라인)", 20,
                            List.of("등록할 것 다시 확인하기", "일정 고르기", "등록 마치기"))
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

    // 같은 제목 미션이 이미 있으면 건너뜀 (보상 조각 하나에 미션 여러 개)
    private void seedMissions() {
        for (MissionSeed seed : MISSIONS) {
            if (missionRepository.existsByTitle(seed.title())) {
                continue;
            }
            Region region = findRegion(seed.stageNo());
            Piece piece = pieceRepository.findByRegionIdAndSlotNo(region.getId(), seed.slotNo())
                    .orElseThrow(() -> new IllegalStateException(
                            "조각 시드가 없어요: " + seed.stageNo() + "계단 " + seed.slotNo() + "칸"));
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