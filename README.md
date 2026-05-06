# Coupon Publish

Spring Boot, Gradle, MySQL, Redis를 사용한 쿠폰 발급 프로젝트입니다.

동시에 여러 사용자가 쿠폰 발급을 요청해도 Redis Lua Script를 이용해 발급 수량 차감과 사용자 중복 발급 체크를 atomic 하게 처리합니다. MySQL에는 발급 내역을 저장하고, `user_id` 유니크 제약을 통해 데이터베이스 레벨에서도 중복 발급을 방지합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.6
- Gradle
- Spring Web
- Spring Data JPA
- Spring Data Redis
- MySQL 8.4
- Redis 7.4
- Docker Compose

## 주요 기능

- 쿠폰 발급
- 쿠폰 발급 내역 조회
- 쿠폰 남은 수량 조회
- 쿠폰 발급 취소
- 최대 발급 수량 100장 제한
- 동일 사용자 중복 발급 방지
- Redis Lua Script 기반 atomic 발급 처리
- 애플리케이션 시작 시 MySQL의 활성 발급 내역 기준으로 Redis 상태 재구성

## 프로젝트 구조

```text
src/main/java/com/example/couponpublish
├── CouponPublishApplication.java
└── coupon
    ├── config
    │   ├── CouponConfig.java
    │   └── CouponProperties.java
    ├── controller
    │   └── CouponController.java
    ├── dto
    │   ├── CouponIssueRequest.java
    │   ├── CouponIssueResponse.java
    │   ├── CouponRemainingResponse.java
    │   └── ErrorResponse.java
    ├── entity
    │   ├── CouponIssue.java
    │   └── CouponStatus.java
    ├── exception
    │   ├── CouponException.java
    │   └── GlobalExceptionHandler.java
    ├── repository
    │   ├── CouponIssueRepository.java
    │   └── CouponRedisRepository.java
    └── service
        ├── CouponRedisInitializer.java
        └── CouponService.java
```

## 동시성 처리 방식

쿠폰 발급 시 Redis Lua Script 안에서 아래 작업을 한 번에 수행합니다.

```text
1. 사용자 발급 여부 확인
2. 남은 쿠폰 수량 확인
3. 남은 수량 1 감소
4. 발급 사용자 Set에 userId 추가
```

Redis는 Lua Script 실행 중 다른 명령이 끼어들지 않으므로, 위 작업은 Redis 기준으로 atomic 하게 처리됩니다.

추가로 MySQL의 `coupon_issue.user_id` 컬럼에 유니크 제약을 걸어 영속 저장소에서도 중복 발급을 방지합니다.

## 실행 전 준비

Docker가 설치되어 있어야 합니다.

MySQL과 Redis는 `docker-compose.yml`로 실행합니다.

```bash
docker compose up -d
```

Windows PowerShell에서도 동일하게 실행할 수 있습니다.

```powershell
docker compose up -d
```

## 애플리케이션 실행

macOS/Linux:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

기본 실행 주소는 다음과 같습니다.

```text
http://localhost:8080
```

## 설정

기본 설정 파일은 `src/main/resources/application.yml`입니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/coupon_publish
    username: coupon
    password: coupon
  data:
    redis:
      host: localhost
      port: 6379

coupon:
  max-count: 100
```

## API

### 쿠폰 발급

```http
POST /api/coupons/issues
Content-Type: application/json

{
  "userId": "user-1"
}
```

응답 예시:

```json
{
  "couponIssueId": 1,
  "userId": "user-1",
  "status": "ISSUED",
  "issuedAt": "2026-05-06T16:30:00",
  "canceledAt": null
}
```

### 쿠폰 발급 내역 조회

```http
GET /api/coupons/issues/user-1
```

응답 예시:

```json
{
  "couponIssueId": 1,
  "userId": "user-1",
  "status": "ISSUED",
  "issuedAt": "2026-05-06T16:30:00",
  "canceledAt": null
}
```

### 쿠폰 남은 수량 조회

```http
GET /api/coupons/remaining
```

응답 예시:

```json
{
  "remainingCount": 99
}
```

### 쿠폰 발급 취소

```http
DELETE /api/coupons/issues/user-1
```

응답 예시:

```json
{
  "couponIssueId": 1,
  "userId": "user-1",
  "status": "CANCELED",
  "issuedAt": "2026-05-06T16:30:00",
  "canceledAt": "2026-05-06T16:35:00"
}
```

## 에러 응답

이미 발급된 사용자, 쿠폰 소진, 존재하지 않는 발급 내역 등은 다음 형태로 응답합니다.

```json
{
  "message": "이미 발급된 사용자입니다.",
  "timestamp": "2026-05-06T16:30:00"
}
```

주요 상태 코드:

- `400 Bad Request`: 요청 값 검증 실패
- `404 Not Found`: 발급 내역 없음
- `409 Conflict`: 중복 발급, 쿠폰 소진, 이미 취소된 쿠폰

## 테스트

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

전체 빌드:

```bash
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

## 로컬 인프라 종료

```bash
docker compose down
```

볼륨까지 삭제하려면 다음 명령을 사용합니다.

```bash
docker compose down -v
```
