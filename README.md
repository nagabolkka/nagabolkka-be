# ✨ 나가볼까? — Backend
> 잃어버린 세상의 조각들 — 방 밖으로 한 걸음씩, 루미와 함께하는 모험
>
**나가볼까?** 는 고립·은둔 청년이 작은 일상 퀘스트를 하나씩 해내며 바깥세상과 다시 연결되도록 돕는 RPG형 모바일 웹 서비스입니다.

## ✨ 주요 기능 
| 기능 | 설명                                                                     |
| --- |--------------------------------------------------------------------------|
| 🔐 인증 | 카카오 로그인, JWT 발급                                                  |
| 🧭 온보딩 | 시작 체크 점수 기반 출발 지역 배정, 목표 저장                            |
| 🗺️ 오늘의 퀘스트 | 담아둔 장소 · 날씨 · 난이도 기반 퀘스트 배정, 쪼개기 · 바꾸기 · 건너뛰기 |
| 📈 난이도 조절 | 연속 완료 횟수와 기분 기록에 따라 난이도 자동 조절                       |
| 💎 조각·복원도 | 퀘스트 완료 시 조각 획득, 지역 복원도 계산, 다음 지역 제안               |
| 📍 동네 장소 | 장소 목록 · 필터 조회, 미션으로 담기                                     |
| 🌦️ 날씨 | 기상청 단기예보 연동                                                     |
| 💬 연습실 | OpenAI 기반 NPC 롤플레이 대화 및 피드백                                  |
| 🆘 위기 감지 | 키워드 + Moderation API로 위기 표현 감지                                 |
| 📖 기록·리포트 | 날짜별 기록 통계, 주간 리포트 (루미의 편지) |

## 🛠️ 기술 스택
| 구분 |  기술 스택 | 사용 목적 |
| --- | --- | --- |
| Language | Java 21 | 서버 개발 |
| Framework | Spring Boot 4 | REST API 서버 |
| Build Tool | Gradle | 의존성 관리 및 빌드 |
| ORM | Spring Data JPA | DB 접근 |
| Database | H2 (개발) → MySQL | 데이터 저장 |
| Validation | Spring Validation | 요청 값 검증 |
| Library | Lombok | 반복 코드 감소 |
| External API | 카카오, 기상청, OpenAI | 로그인·지도, 날씨, AI 대화 |
| Version Control | Git, GitHub | 버전 관리 |

## 📁 프로젝트 구조
```
src/main/java/com/nagabolkka/backend/
├── BackendApplication.java   # 서버 시작점
├── domain/                   # 기능별 패키지
│   └── health/               # 서버 상태 확인
│       # 각 도메인은 controller · service · repository · entity · dto로 구성
└── global/                   # 공통 설정
├── common/               # 공통 응답 형식
├── config/               # 설정 (CORS, Security 등)
└── exception/            # 예외 처리
```

## 🚀 실행 방법
### 1. 요구 사항
- JDK 21
### 2. 환경 변수 설정
`.env.example` 을 복사해 프로젝트 루트에 `.env` 파일을 만들고 값을 채워주세요.
```
KAKAO_REST_API_KEY=
KAKAO_CLIENT_SECRET=
KMA_SERVICE_KEY=
OPENAI_API_KEY=
 ```
### 3. 실행
```aiignore
git clone <repository-url>
cd <project-folder>
./gradlew bootRun
```
### 4. 확인
```aiignore
GET <http://localhost:8080/api/health>
→ {"status":"ok","service":"나가볼까"}
```
> 개발 단계에서는 H2 메모리 DB를 사용합니다. H2 콘솔: `http://localhost:8080/h2-console`

## 🎯 Git Convention
| Type | Description |
| --- | --- |
| 🎉 Start | 프로젝트 시작 |
| ✨ Feat | 새로운 기능 추가 |
| 🐛 Fix | 버그 수정 |
| ♻️ Refactor | 코드 리팩토링 |
| 🔧 Settings | 설정 파일 수정 |
| ✅ Test | 테스트 코드 추가 및 수정 |
| ➕ Dependency/Plugin | 라이브러리 추가 |
| 📝 Docs | 문서 수정 |
| 🚚 Rename | 파일 및 폴더 이름 수정 |
| 🔥 Remove | 파일 삭제 |

## 🌲 Branch Convention
| Branch | Description |
| --- | --- |
| main | 배포 가능한 브랜치 |
| develop | 개발 브랜치 |
| feat/#이슈번호/명칭 | 새로운 기능 개발 |
| fix/#이슈번호/명칭 | 버그 수정 |
| refactor/#이슈번호/명칭 | 리팩토링 작업 |
