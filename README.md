# 🗺️ 나가볼까? - Backend

> **나가볼까?** 의 백엔드 레포지토리입니다.
Spring Boot 로 만든 REST API 서버이며, 인증 · 데이터 저장 · 퀘스트 배정 · 외부 API 연동 · 배치를 맡고, AI 기능은 내부 AI 서버 (`nagabolkka-ai`) 를 호출해 처리합니다. AWS EC2 에 Docker Compose 로 배포합니다.

**🔗 API 서버 → https://api.nagabolkka.shop**
**📄 API 문서 (Swagger) → https://api.nagabolkka.shop/swagger-ui/index.html**

<br />

## 🧩 Implementation

| 구분 | 구현 방식 |
|---|---|
| **인증** | 카카오 인가 코드로 토큰을 교환해 가입 · 로그인합니다. Access 토큰(30분) + Refresh 토큰(14일)을 발급하고, Refresh 는 SHA-256 해시로만 저장하며 재발급할 때마다 새 토큰으로 교체(rotation)합니다. |
| **개인정보 보호** | 카카오 회원번호는 HMAC-SHA256 해시(로그인 조회용)와 AES-256-GCM 암호문(탈퇴 시 연결 끊기용)으로 분리 보관합니다. 한 줄 회고는 AES 로 암호화해 저장하고, 관리자 응답의 닉네임은 마스킹합니다. 사용자 위치 좌표는 요청 처리에만 쓰고 저장하지 않습니다. |
| **공통 Audit** | 모든 엔티티가 `BaseEntity` 를 상속해 `created_at` · `updated_at` · `created_by` · `updated_by` 를 JPA Auditing 으로 자동 기록합니다. |
| **에러 처리** | `ErrorCode` + `GlobalExceptionHandler` 로 모든 실패 응답을 `{ code, message }` 형식으로 통일하고, 프론트는 `code` 로 분기합니다. |
| **동시성** | 하루 한 개 퀘스트는 `(user_id, quest_date)` 유니크 제약 + 사용자 행 비관적 잠금(`PESSIMISTIC_WRITE`)으로 중복 요청을 막습니다. |
| **AI 연동** | `AiClient` 로 내부 AI 서버(위기 감지 · 루미 한마디 · 연습 대화 · 주간 편지)를 호출합니다. AI 호출은 트랜잭션 밖에서 해 DB 커넥션을 오래 잡지 않고, 실패하면 템플릿 문장 · 고정 대사로 대체합니다. |
| **안전장치** | 회고 · 연습 대화는 AI 위기 감지를 먼저 거치고, AI 서버가 응답하지 않아도 Spring 규칙(`CrisisFallback`)으로 다시 검사합니다. 위기면 회고를 저장하지 않고 `crisis_events` 만 기록합니다. |
| **날씨** | 기상청 단기예보 API 를 사용합니다. 받은 위경도를 기상청 격자(`nx, ny`)로 변환하고, 격자별로 1시간 캐시합니다. 위치가 없으면 가입한 구의 격자를 씁니다. |
| **장소** | 카카오 로컬 API 로 내 위치 주변의 공원 · 도서관 · 주민 공간 · 청년 공간을 검색해 DB 에 동기화하고, 거리순으로 내려줍니다. |
| **퀘스트 배정** | 사용자의 계단(지역) · 난이도 · 날씨 · 담아 둔 장소를 기준으로 오늘 퀘스트를 배정하고, 완료 기록에 따라 다음 날 난이도를 조정합니다. 다음 지역 제안 규칙은 `NextRegionPolicy` 로 분리해 홈과 완료 API 가 같이 씁니다. |
| **시드 데이터** | `SeedDataInitializer` 가 지역 · 조각 · 미션 · 목표 · 시나리오를 키로 확인해 없을 때만 넣으므로, 운영에서 다시 실행해도 중복되지 않습니다. |
| **배치** | 주간 편지 생성(월 03:00) · 만료 Refresh 토큰 정리(매일 04:00) · 6개월 지난 위치 이용 기록 삭제(매일 04:30), 모두 KST 기준입니다. |
| **API 문서** | springdoc-openapi 로 Swagger UI 를 제공하고, JWT Authorize 버튼으로 인증이 필요한 API 를 바로 테스트할 수 있습니다. |

<br>

## 🛠️ Tech Stack

| Category | Stack |
|---|---|
| Language | ![Java](https://img.shields.io/badge/Java_21-007396?style=for-the-badge&logo=openjdk&logoColor=white) |
| Framework | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white) |
| Database | ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white) ![H2](https://img.shields.io/badge/H2-09476B?style=for-the-badge&logoColor=white) |
| Auth | ![Kakao Login](https://img.shields.io/badge/Kakao_Login-FFCD00?style=for-the-badge&logo=kakao&logoColor=black) ![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white) |
| External API | ![Kakao Local](https://img.shields.io/badge/Kakao_Local-FFCD00?style=for-the-badge&logo=kakao&logoColor=black) ![KMA](https://img.shields.io/badge/기상청_단기예보-0072BC?style=for-the-badge&logoColor=white) |
| Docs | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black) |
| Build | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) |
| Infra | ![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white) ![Caddy](https://img.shields.io/badge/Caddy-1F88C0?style=for-the-badge&logo=caddy&logoColor=white) |
| CI/CD | ![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white) |

<br>

## 🏗️ Architecture

```
[ nagabolkka-fe (Vercel) ]
            │  HTTPS · JWT
            ▼
[ Caddy (HTTPS) ] → [ Spring Boot (app) ] → [ MySQL ]
                            │
                            ├─ 내부 네트워크 → [ nagabolkka-ai (FastAPI) ] → OpenAI
                            └─ 외부 API → 카카오 로그인 · 카카오 로컬 · 기상청
```

- AI 서버는 외부에 포트를 열지 않고, Docker 내부 네트워크에서 내부 키로만 호출합니다.

<br>

## 📁 Project Structure

```
src/main/java/com/nagabolkka/backend/
├── domain/              # 도메인별 controller · service · repository · entity · dto
│   ├── auth/            # 카카오 로그인 · 토큰 재발급 · 로그아웃
│   ├── user/            # 내 정보 · 탈퇴
│   ├── home/            # 홈
│   ├── quest/           # 오늘 퀘스트 · 완료
│   ├── region/          # 지역 · 복원도 · 다음 지역 제안
│   ├── place/           # 동네 지도 · 담기
│   ├── practice/        # 연습실
│   └── ...              # 도감 · 기록 · 편지 · 목표 · 안전 · 공지 · 관리자 · 플러스
└── global/              # config · security · error · seed · 외부 API 클라이언트 등 공통
src/main/resources/
├── application.yaml       # 공통 · 로컬(H2)
└── application-prod.yaml  # 운영(MySQL)
docker-compose.yml         # mysql + app + caddy
Caddyfile
```

<br>

## 🚀 Getting Started

### 📦 Install

```bash
git clone https://github.com/nagabolkka/nagabolkka-be.git
cd nagabolkka-be
```

### 🔑 Environment Variables

`.env.example` 을 복사해 `.env` 를 만들고 값을 채웁니다. (커밋하지 않음)

```env
KAKAO_REST_API_KEY=
KAKAO_CLIENT_SECRET=
KMA_SERVICE_KEY=
OPENAI_API_KEY=
JWT_SECRET=
HASH_SECRET=
AES_KEY=
AI_BASE_URL=http://localhost:8000
AI_INTERNAL_KEY=
# 운영(MySQL)에서만
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
MYSQL_ROOT_PASSWORD=
```

| Key | Description |
|---|---|
| `KAKAO_REST_API_KEY` · `KAKAO_CLIENT_SECRET` | 카카오 로그인 · 카카오 로컬 API |
| `KMA_SERVICE_KEY` | 공공데이터포털 기상청 단기예보 서비스 키 |
| `OPENAI_API_KEY` | OpenAI API 키 |
| `JWT_SECRET` | JWT 서명 키 |
| `HASH_SECRET` | 카카오 회원번호 HMAC 키 (32자 이상) |
| `AES_KEY` | 회원번호 · 회고 암호화 키 (32바이트 base64) |
| `AI_BASE_URL` · `AI_INTERNAL_KEY` | AI 서버 주소 · 내부 호출 키 (AI 서버 `.env` 와 같은 값) |
| `DB_*` · `MYSQL_ROOT_PASSWORD` | 운영 MySQL 접속 정보 |

> ⚠️ `HASH_SECRET` · `AES_KEY` 는 운영에서 한 번 정하면 바꾸지 않습니다. 바꾸면 저장된 회원번호 · 회고를 읽을 수 없습니다.

### ▶️ Run

```bash
./gradlew bootRun   # http://localhost:8080 (로컬은 H2)
./gradlew test      # 테스트
```

- Swagger → http://localhost:8080/swagger-ui/index.html
- 헬스 체크 → http://localhost:8080/api/health
- AI 기능까지 확인하려면 `nagabolkka-ai` 를 로컬에서 함께 실행합니다. (꺼져 있으면 템플릿 응답으로 대체)

<br>

## 🌐 Deployment

- AWS EC2 에서 Docker Compose 로 `mysql` · `app` · `caddy` 컨테이너를 실행하고, Caddy 가 HTTPS 인증서를 자동으로 발급합니다.
- GitHub Actions (`deploy.yml`)
    - `main` push → EC2 에 접속해 `origin/main` 동기화 → `docker compose up -d --build` → `/api/health` 헬스 체크
    - `develop` 은 배포하지 않습니다.
    - 배포하는 동안만 Actions 러너 IP 를 보안 그룹에 임시로 열고, 끝나면 닫습니다.
- `.env.example` 에 새 키가 추가된 PR 은 **main 머지 전에 서버 `.env` 부터 채웁니다.** (안 채우면 앱이 뜨지 않음)

<br>

## 🤝 Convention

### 🌿 Branch

`타입/#이슈번호/명칭` — `develop` 에서 생성 (예: `feat/#10/mypage`)

| Branch | Usage |
|---|---|
| `main` | 운영 배포 |
| `develop` | 개발 통합 |
| `feat` · `fix` · `refactor` · `chore` | 작업 브랜치 |

### 💬 Commit

`이모지 Type: 내용` (이슈 번호 없음)

| Type | Usage |
|---|---|
| ✨ Feat | 새 기능 |
| 🐛 Fix | 버그 수정 |
| ♻️ Refactor | 리팩토링 |
| ✅ Test | 테스트 |
| 🔧 Settings | 설정 파일 · 의존성 |
| 🧹 Chore | 기능 변화 없는 정리 |
| 🚀 Deploy | 배포 · CI/CD |
| 📝 Docs | 문서 |

### 🔀 Pull Request

`[타입/#이슈번호] 내용` — 예: `[FEAT/#10] 내 정보 조회 · 목표 변경 API 구현`
배포 PR (develop → main) 은 `[RELEASE] 내용`