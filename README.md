# Coupon Publish

Spring Boot, Gradle, MySQL, Redis를 사용한 쿠폰 발급 프로젝트입니다.

동시에 여러 사용자가 쿠폰 발급을 요청해도 Redis Lua Script를 이용해 발급 수량 차감과 사용자 중복 발급 체크를 atomic 하게 처리합니다. MySQL에는 최종 발급 이력을 저장하고, `user_id` 유니크 제약을 통해 데이터베이스 레벨에서도 중복 발급을 방지합니다.

이 프로젝트는 Redis와 DB를 둘 다 사용합니다. 둘은 같은 일을 중복해서 하는 것이 아니라 서로 다른 책임을 나눕니다.

- Redis: 트래픽이 몰리는 발급 순간에 빠르게 남은 수량을 차감하고 중복 요청을 1차로 차단합니다.
- DB: 최종 발급 이력과 쿠폰 상태를 영속화하고, 취소/재발급 같은 비즈니스 정합성을 보장합니다.
- Unique 제약: 애플리케이션이나 Redis 방어를 통과한 예외 상황에서도 동일 `userId` 중복 발급을 막는 마지막 방어선입니다.

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

DB는 Redis 성공 이후 실제 발급 내역을 저장합니다. 이때 MySQL의 `coupon_issue.user_id` 컬럼에 유니크 제약을 걸어 영속 저장소에서도 중복 발급을 방지합니다. 즉, Redis는 빠른 수량 차감과 1차 중복 방어를 맡고, DB는 최종 발급 이력과 정합성의 기준점이 됩니다.

동시성 테스트는 다음 조건을 검증합니다.

- 1,000명의 사용자가 동시에 요청해도 정확히 100명만 발급 성공
- 동일 `userId`가 동시에 100번 요청해도 정확히 1번만 발급 성공
- 성공한 발급 수와 DB의 `ISSUED` 이력 수, Redis 남은 수량이 서로 일치

## 장애 시나리오

### Redis 성공 후 DB 저장 실패

Redis에서 수량 차감과 사용자 등록이 성공했지만 DB 저장이 실패할 수 있습니다. 예를 들어 DB 유니크 제약 충돌, 일시적인 DB 오류 등이 발생할 수 있습니다.

이 경우 Redis에만 발급된 것처럼 남으면 실제 DB 이력과 Redis 수량이 어긋납니다. 그래서 `CouponService`는 DB 저장 단계에서 `CouponException` 또는 `DataIntegrityViolationException`이 발생하면 `couponRedisRepository.rollbackIssue(userId)`를 호출해 Redis의 사용자 Set 등록과 남은 수량 차감을 되돌립니다.

### Redis 장애

Redis가 장애 상태라면 발급을 진행하지 않습니다. Redis가 빠른 수량 차감과 1차 중복 방어를 맡고 있기 때문에, Redis 없이 DB만으로 발급을 계속하면 순간 트래픽에서 초과 발급 위험이 커집니다.

따라서 Redis 발급 스크립트 실행이 실패하면 DB 저장 단계로 넘어가지 않고 요청을 실패 처리합니다.

### 서버 재시작

서버가 재시작되거나 Redis 데이터가 초기화되면 Redis 상태는 DB의 최종 발급 이력을 기준으로 복구합니다.

애플리케이션 시작 시 `CouponRedisInitializer`가 DB에서 `ISSUED` 상태의 발급 내역을 조회하고, 해당 사용자 목록으로 Redis 발급 사용자 Set과 남은 수량을 재구성합니다. 이때 `CANCELED` 상태는 활성 발급으로 보지 않으므로 남은 수량 계산에서 제외됩니다.

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

동시성 통합 테스트는 Testcontainers로 MySQL과 Redis를 실행하므로 Docker가 필요합니다. Docker를 사용할 수 없는 환경에서는 해당 통합 테스트가 자동으로 스킵됩니다.

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
