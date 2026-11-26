# ✈️ 나가볼까? - Backend

> **나가볼까?** 의 백엔드 레포지토리입니다.
Spring Boot 로 만든 REST API 서버이며, 인증 · 데이터 저장 · 퀘스트 배정 · 외부 API 연동 · 푸시 알림 · 배치를 맡고, AI 기능은 내부 AI 서버 (`nagabolkka-ai`) 를 호출해 처리합니다. AWS EC2 에 Docker Compose 로 배포합니다.

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
| **AI 연동** | `AiClient` 로 내부 AI 서버(위기 감지 · 루미 한마디 · 연습 대화 · 연습 음성 입력 · 주간 엽서 · 동행 통화 임시 키)를 호출합니다. AI 호출은 트랜잭션 밖에서 해 DB 커넥션을 오래 잡지 않고, 실패하면 템플릿 문장 · 고정 대사로 대체합니다. |
| **안전장치** | 회고 · 연습 대화는 AI 위기 감지를 먼저 거치고, AI 서버가 응답하지 않아도 Spring 규칙(`CrisisFallback`)으로 다시 검사합니다. 위기면 회고를 저장하지 않고 `crisis_events` 만 기록합니다. |
| **날씨** | 기상청 단기예보 API 를 사용합니다. 받은 위경도를 기상청 격자(`nx, ny`)로 변환하고, 격자별로 1시간 캐시합니다. 위치가 없으면 가입한 구의 격자를 씁니다. |
| **장소** | 카카오 로컬 API 로 내 위치 주변의 공원 · 도서관 · 주민 공간 · 청년 공간을 검색해 DB 에 동기화하고, 거리순으로 내려줍니다. |
| **퀘스트 배정** | 사용자의 지역(섬) · 난이도 · 날씨 · 담아 둔 장소를 기준으로 오늘 퀘스트를 배정하고, 완료 기록에 따라 다음 날 난이도를 조정합니다. 수락 전에는 하루 한 번 더 쉽게(`EASIER`) · 더 어렵게(`HARDER`) 바꿀 수 있습니다. 다음 지역 제안 규칙은 `NextRegionPolicy` 로 분리해 홈과 완료 API 가 같이 씁니다. |
| **시작 체크 · 목표** | 시작 체크는 목표 없이도 시작할 수 있도록 `goalId` 를 선택값으로 받고, 목표는 나중에 내 정보의 목표 변경 API 로 정합니다. 고른 목표가 없으면 목표 조회는 `NOT_FOUND` 로 응답해 프론트가 빈 상태를 보여 줍니다. |
| **루미 동행 통화** | 오늘 퀘스트(제목 · 설명 · 장소)를 트랜잭션 안에서 읽어 AI 서버에 넘기고, AI 서버가 발급한 OpenAI Realtime 임시 키(60초)와 최대 통화 시간(10분)을 내려줍니다. 음성은 브라우저가 OpenAI 와 WebRTC 로 직접 주고받아 서버를 거치지 않습니다. 통화를 쓸 수 없으면 `CALL_UNAVAILABLE`(409)로 응답합니다. |
| **푸시 알림** | Firebase Admin SDK 로 FCM 알림을 보냅니다. 기기 토큰을 등록받아 오늘 퀘스트 알림과 관리자 공지 알림을 보내고, 만료된 토큰은 정리합니다. `FIREBASE_CREDENTIALS_BASE64` 가 비어 있으면 알림 발송만 건너뜁니다. |
| **도감** | 섬별 도장 칸마다 처음 받은 날과 그날 퀘스트 기분을 함께 내려줘, 프론트가 도장 잉크 색과 날짜를 그립니다. |
| **관리자** | `ADMIN` 권한 사용자만 사용자 목록(닉네임 마스킹) · 공지 등록 · 수정 · 삭제 · 공지 푸시를 쓸 수 있습니다. |
| **시드 데이터** | `SeedDataInitializer` 가 지역 · 조각 · 미션 · 목표 · 시나리오를 키로 확인해 없을 때만 넣으므로, 운영에서 다시 실행해도 중복되지 않습니다. |
| **배치** | 오늘 퀘스트 푸시(매일 09:00) · 주간 엽서 생성(월 10:00) · 만료 Refresh 토큰 정리(매일 04:00) · 6개월 지난 위치 이용 기록 삭제(매일 04:30), 모두 KST 기준입니다. |
| **API 문서** | springdoc-openapi 로 Swagger UI 를 제공하고, JWT Authorize 버튼으로 인증이 필요한 API 를 바로 테스트할 수 있습니다. |

<br>

## 🛠️ Tech Stack

**Language · Framework**

![Java](https://img.shields.io/badge/Java_21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)

**Database**

![MySQL](https://img.shields.io/badge/MySQL_8.4-4479A1?style=flat-square&logo=mysql&logoColor=white)
![H2](https://img.shields.io/badge/H2-09476B?style=flat-square)

**Auth**

![Kakao Login](https://img.shields.io/badge/Kakao_Login-FFCD00?style=flat-square&logo=kakao&logoColor=black)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)

**External API**

![Kakao Local](https://img.shields.io/badge/Kakao_Local-FFCD00?style=flat-square&logo=kakao&logoColor=black)
![기상청 단기예보](https://img.shields.io/badge/기상청_단기예보-0B57D0?style=flat-square)
![Firebase Cloud Messaging](https://img.shields.io/badge/Firebase_Cloud_Messaging-FFCA28?style=flat-square&logo=firebase&logoColor=black)

**Docs · Build**

![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)

**Infra**

![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![Caddy](https://img.shields.io/badge/Caddy-1F88C0?style=flat-square&logo=caddy&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

<br>

## 🏗️ Architecture

```
[ nagabolkka-fe (Vercel) ] ──(임시 키 · WebRTC, 동행 통화만)──▶ OpenAI Realtime
            │  HTTPS · JWT
            ▼
[ Caddy (HTTPS) ] → [ Spring Boot (app) ] → [ MySQL ]
                            │
                            ├─ 내부 네트워크 → [ nagabolkka-ai (FastAPI) ] → OpenAI
                            ├─ 외부 API → 카카오 로그인 · 카카오 로컬 · 기상청
                            └─ 푸시 → Firebase Cloud Messaging → 사용자 기기
```

- AI 서버는 외부에 포트를 열지 않고, Docker 내부 네트워크에서 내부 키로만 호출합니다.
- 동행 통화 음성은 서버를 거치지 않고, 프론트가 백엔드에서 받은 임시 키로 OpenAI 에 직접 연결합니다. OpenAI API 키는 AI 서버 밖으로 나가지 않습니다.

<br>

## 📁 Project Structure

```
src/main/java/com/nagabolkka/backend/
├── domain/              # 도메인별 controller · service · repository · entity · dto
│   ├── auth/            # 카카오 로그인 · 토큰 재발급 · 로그아웃
│   ├── user/            # 내 정보 · 탈퇴
│   ├── onboarding/      # 시작 체크
│   ├── home/            # 홈
│   ├── quest/           # 오늘 퀘스트 · 난이도 바꾸기 · 완료 · 동행 통화
│   ├── region/          # 지역 · 복원도 · 다음 지역 제안
│   ├── place/           # 동네 지도 · 담기
│   ├── practice/        # 연습실
│   ├── piece/           # 도감 (섬별 도장 · 처음 받은 날 · 기분)
│   ├── push/            # 기기 토큰 · 알림
│   ├── notice/          # 공지 · 공지 푸시
│   ├── admin/           # 관리자 사용자 목록
│   └── ...              # 기록 · 엽서 · 목표 · 안전 · 플러스 · 미션 · 위치 기록
└── global/              # config · security · exception · seed · batch · crypto · ai · kakao · weather · push 등 공통
src/main/resources/
├── application.yaml       # 공통 · 로컬(H2)
└── application-prod.yaml  # 운영(MySQL)
Dockerfile                 # 실행 전용 이미지 (app.jar 복사)
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
KAKAO_ADMIN_KEY=
KMA_SERVICE_KEY=
OPENAI_API_KEY=
JWT_SECRET=
HASH_SECRET=
AES_KEY=
AI_BASE_URL=http://localhost:8000
AI_INTERNAL_KEY=
FIREBASE_CREDENTIALS_BASE64=
APP_URL=http://localhost:5173
# 운영(MySQL)에서만
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
MYSQL_ROOT_PASSWORD=
```

| Key | Description |
|---|---|
| `KAKAO_REST_API_KEY` · `KAKAO_CLIENT_SECRET` | 카카오 로그인 · 카카오 로컬 API |
| `KAKAO_ADMIN_KEY` | 카카오 어드민 키 (탈퇴 시 연결 끊기) |
| `KMA_SERVICE_KEY` | 공공데이터포털 기상청 단기예보 서비스 키 |
| `OPENAI_API_KEY` | OpenAI API 키 |
| `JWT_SECRET` | JWT 서명 키 |
| `HASH_SECRET` | 카카오 회원번호 HMAC 키 (32자 이상) |
| `AES_KEY` | 회원번호 · 회고 암호화 키 (32바이트 base64) |
| `AI_BASE_URL` · `AI_INTERNAL_KEY` | AI 서버 주소 · 내부 호출 키 (AI 서버 `.env` 와 같은 값) |
| `FIREBASE_CREDENTIALS_BASE64` | Firebase 서비스 계정 JSON 을 base64 한 줄로 (비우면 알림 발송 안 함) |
| `APP_URL` | 알림을 누르면 열 프론트 주소 (운영 `https://nagabolkka.vercel.app`) |
| `DB_*` · `MYSQL_ROOT_PASSWORD` | 운영 MySQL 접속 정보 |

> ⚠️ `HASH_SECRET` · `AES_KEY` 는 운영에서 한 번 정하면 바꾸지 않습니다. 바꾸면 저장된 회원번호 · 회고를 읽을 수 없습니다.
> ⚠️ Firebase 서비스 계정 JSON 파일은 레포에 커밋하지 않고, base64 값만 서버 · 로컬 `.env` 에 넣습니다.

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
  - `main` push → GitHub 에서 `bootJar` 로 jar 빌드 → `scp` 로 `app.jar` 를 서버에 복사 → `origin/main` 동기화 → `docker compose up -d --build` (jar 복사만) → `/api/health` 헬스 체크
  - 서버(RAM 1GB)에서 Gradle 빌드를 하면 다른 컨테이너가 스왑으로 밀려나서, 빌드는 GitHub 에서 하고 서버는 실행만 합니다.
  - `develop` 은 배포하지 않습니다.
  - 배포하는 동안만 Actions 러너 IP 를 보안 그룹에 임시로 열고, 끝나면 닫습니다.
- `.env.example` 에 새 키가 추가된 PR 은 **main 머지 전에 서버 `.env` 부터 채웁니다.** (안 채우면 앱이 뜨지 않음)
- 프론트와 같이 바뀌는 API 는 **백엔드를 먼저** main 에 배포합니다. AI 서버를 새로 호출하는 API 는 **AI 서버 → 백엔드 → 프론트** 순서로 배포합니다.

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
배포 PR (develop → main) 은 `[DEPLOY] 내용`