# Coupon Publish

Spring Boot, Gradle, Redis, Kafka, Apache Flink를 사용한 쿠폰 발급 및 주문 관리 프로젝트입니다.

동시에 여러 사용자가 쿠폰 발급을 요청해도 Redis Lua Script를 이용해 발급 수량 차감과 사용자 중복 발급 체크를 atomic 하게 처리합니다. 발급 성공 후에는 Kafka로 쿠폰 발급 이벤트를 발행하고, Flink가 이벤트를 소비해 Redis에 쿠폰별 집계 현황을 갱신합니다.

주문 관리는 Redis에 주문 원본과 이벤트 로그를 저장하고, 주문 생성/상태 변경 이벤트를 Kafka로 발행합니다. 별도 Flink job이 주문 이벤트를 소비해 상태별 주문 수, 오늘 매출, 최근 이벤트 피드를 Redis 집계 키로 갱신합니다.

이 프로젝트는 Redis, Kafka, Flink가 다음 책임을 나눕니다.

- Redis: 트래픽이 몰리는 발급 순간에 빠르게 남은 수량을 차감하고 중복 요청을 1차로 차단합니다.
- Redis: 쿠폰 메타데이터, 발급 이력, 남은 수량, 주문 원본, 이벤트 로그, 집계 현황을 저장합니다.
- Kafka: 쿠폰 발급 성공 이벤트와 주문 라이프사이클 이벤트를 비동기로 전달합니다.
- Flink: Kafka 이벤트를 스트림으로 처리해 Redis 집계 키를 갱신합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.6
- Gradle
- Spring Web
- Spring Data Redis
- Spring Kafka
- Apache Flink
- Redis 7.4
- Apache Kafka 3.8.1
- Docker Compose

## 주요 기능

- 쿠폰 캠페인 생성, 조회, 삭제
- 쿠폰별 발급 기간 관리
- 쿠폰별 발급
- 쿠폰별 발급 내역 단건/목록 조회
- 쿠폰별 남은 수량 조회
- 쿠폰 발급 취소
- 쿠폰별 최대 발급 수량 제한
- 쿠폰별 동일 사용자 중복 발급 방지
- Redis Lua Script 기반 atomic 발급 처리
- 쿠폰 발급 성공 이벤트 Kafka 발행
- Flink Kafka source를 통한 쿠폰별 발급 집계
- Redis 기반 쿠폰별 집계 현황 조회
- 주문 생성, 조회, 상태 변경
- 주문 생성/상태 변경 이벤트 Kafka 발행
- Flink Kafka source를 통한 주문 상태별 수량, 오늘 매출, 최근 이벤트 집계

## 프로젝트 구조

```text
src/main/java/com/example/couponpublish
├── CouponPublishApplication.java
└── coupon
    ├── config
    │   ├── CouponConfig.java
    │   └── KafkaTopicProperties.java
    ├── controller
    │   └── CouponController.java
    ├── dto
    │   ├── CouponCreateRequest.java
    │   ├── CouponIssuePageResponse.java
    │   ├── CouponIssueRequest.java
    │   ├── CouponIssueResponse.java
    │   ├── CouponIssueStatisticsResponse.java
    │   ├── CouponRemainingResponse.java
    │   ├── CouponResponse.java
    │   └── ErrorResponse.java
    ├── entity
    │   ├── Coupon.java
    │   ├── CouponIssue.java
    │   ├── CouponIssueStatistics.java
    │   └── CouponStatus.java
    ├── exception
    │   ├── CouponException.java
    │   └── GlobalExceptionHandler.java
    ├── event
    │   ├── CouponEventPublisher.java
    │   ├── CouponIssuedEvent.java
    │   ├── KafkaCouponEventPublisher.java
    │   └── NoOpCouponEventPublisher.java
    ├── flink
    │   ├── CouponIssuedEventDeserializationSchema.java
    │   ├── CouponStatisticsFlinkJob.java
    │   └── RedisCouponStatisticsSink.java
    ├── repository
    │   ├── CouponRepository.java
    │   ├── CouponIssueRepository.java
    │   ├── CouponIssueStatisticsRepository.java
    │   └── CouponRedisRepository.java
    └── service
        ├── CouponIssueStatisticsService.java
        ├── CouponRedisInitializer.java
        └── CouponService.java
```

주문 관리는 `order` 패키지 아래에 controller, dto, entity, repository, service를 두고, Kafka publisher는 `order.event`, Flink 집계 job과 Redis sink는 `order.flink`에 둡니다.

## 동시성 처리 방식

쿠폰 발급 시 Redis Lua Script 안에서 아래 작업을 한 번에 수행합니다.

```text
1. 사용자 발급 여부 확인
2. 남은 쿠폰 수량 확인
3. 남은 수량 1 감소
4. 발급 사용자 Set에 userId 추가
```

Redis는 Lua Script 실행 중 다른 명령이 끼어들지 않으므로, 위 작업은 Redis 기준으로 atomic 하게 처리됩니다.

발급 성공 후 발급 이력도 Redis에 저장합니다. 쿠폰별 중복 발급 방지와 남은 수량 차감은 Lua Script에서 함께 처리되므로 Redis 기준으로 한 번에 성공하거나 실패합니다.

발급 이력이 Redis에 저장되면 `CouponIssuedEvent`를 Kafka `coupon-issued` topic으로 발행합니다. Flink job은 이 topic을 읽고 Redis `coupon:{couponId}:statistics`에 발급 수와 마지막 발급 시각을 반영합니다.

Spring Boot API 서버와 Flink job은 별도 프로세스로 실행합니다. API 서버는 요청 처리와 Kafka 발행까지만 담당하고, Flink job은 Kafka topic을 계속 구독하면서 Redis 집계만 담당합니다.

전체 발급 흐름은 다음과 같습니다.

```text
1. POST /api/coupons/{couponId}/issues 요청
2. Redis에서 couponId 기준 쿠폰 캠페인과 발급 기간 확인
3. Redis Lua Script로 쿠폰별 중복 발급과 남은 수량 확인
4. Redis에서 쿠폰별 남은 수량 차감 및 발급 사용자 등록
5. Redis에 신규 발급 또는 취소 이력 재발급 저장
6. 이력 저장 실패 시 Redis 차감 롤백
7. Kafka coupon-issued 이벤트 발행
8. 발급 결과 응답 반환
9. Flink가 coupon-issued 이벤트를 소비해 Redis 집계 현황 갱신
```

동시성 테스트는 다음 조건을 검증합니다.

- 1,000명의 사용자가 동시에 요청해도 정확히 100명만 발급 성공
- 동일 `userId`가 동시에 100번 요청해도 정확히 1번만 발급 성공
- 성공한 발급 수와 Redis `ISSUED` 이력 수, Redis 남은 수량이 서로 일치

## 장애 시나리오

### Redis 발급 성공 후 이력 저장 실패

Redis에서 수량 차감과 사용자 등록이 성공했지만 발급 이력 저장이 실패할 수 있습니다.

이 경우 `CouponService`는 `couponRedisRepository.rollbackIssue(couponId, userId)`를 호출해 Redis의 사용자 Set 등록과 남은 수량 차감을 되돌립니다.

### Redis 장애

Redis가 장애 상태라면 발급을 진행하지 않습니다. Redis가 쿠폰 상태, 발급 이력, 빠른 수량 차감과 1차 중복 방어를 맡고 있기 때문입니다.

따라서 Redis 발급 스크립트 실행이 실패하면 요청을 실패 처리합니다.

### 서버 재시작

서버가 재시작되더라도 Redis 데이터가 유지되면 쿠폰, 발급 이력, 남은 수량, 집계 현황을 그대로 조회할 수 있습니다. Redis 데이터를 초기화하면 쿠폰/발급 상태도 함께 초기화됩니다.

### Kafka 장애

현재 Kafka 이벤트는 Redis 발급 이력 저장 이후 발행됩니다. Flink job은 발급 이벤트를 소비한 뒤 Redis 집계 키의 쿠폰별 발급 수와 마지막 발급 시각을 갱신합니다. Kafka 브로커가 내려가 있으면 발급 API는 실패할 수 있습니다.

운영 환경에서는 이벤트 유실을 더 강하게 막기 위해 Redis Streams나 별도 outbox 저장소를 두고, relay가 Kafka로 재시도 발행하는 구조를 고려할 수 있습니다. 현재 구현은 로컬 학습과 기본 이벤트 연동을 위한 단순 producer 구조입니다.

## 실행 전 준비

Docker가 설치되어 있어야 합니다.

Redis, Redis UI, Kafka, Kafka UI는 `docker-compose.yml`로 실행합니다.

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

## Flink 집계 Job 실행

쿠폰 발급 집계와 주문 관리 집계가 필요하면 API 서버와 별도로 Flink job을 실행합니다.

현재 쿠폰과 주문은 토픽, 이벤트 스키마, 집계 로직이 달라서 Flink job을 따로 실행합니다.

### 쿠폰 발급 집계

macOS/Linux:

```bash
./gradlew runCouponStatisticsFlinkJob
```

Windows PowerShell:

```powershell
.\gradlew.bat runCouponStatisticsFlinkJob
```

로컬 기본값은 다음과 같습니다.

```text
Kafka bootstrap servers: localhost:9092
Kafka topic: coupon-issued
Flink consumer group: coupon-flink-statistics
Redis: localhost:6379
```

다른 주소를 쓰려면 system property로 넘길 수 있습니다.

```powershell
.\gradlew.bat -Dcoupon.flink.kafka.bootstrap-servers=localhost:9092 -Dcoupon.flink.redis.host=localhost -Dcoupon.flink.redis.port=6379 runCouponStatisticsFlinkJob
```

### 주문 관리 집계

macOS/Linux:

```bash
./gradlew runOrderStatisticsFlinkJob
```

Windows PowerShell:

```powershell
.\gradlew.bat runOrderStatisticsFlinkJob
```

로컬 기본값은 다음과 같습니다.

```text
Kafka bootstrap servers: localhost:9092
Kafka topic: order-events
Flink consumer group: order-flink-statistics
Redis: localhost:6379
```

다른 주소를 쓰려면 system property로 넘길 수 있습니다.

```powershell
.\gradlew.bat -Dorder.flink.kafka.bootstrap-servers=localhost:9092 -Dorder.flink.redis.host=localhost -Dorder.flink.redis.port=6379 runOrderStatisticsFlinkJob
```

Kafka topic과 메시지는 Kafka UI에서 확인할 수 있습니다.

```text
http://localhost:8081
```

Redis key와 value는 Redis UI에서 확인할 수 있습니다.

```text
http://localhost:8082
```

## 설정

기본 설정 파일은 `src/main/resources/application.yml`입니다.

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092

coupon:
  kafka:
    enabled: true
    topics:
      coupon-issued: coupon-issued

order:
  kafka:
    enabled: true
    topics:
      events: order-events
```

`coupon.kafka.enabled=false`로 설정하면 Kafka producer bean 대신 no-op publisher를 사용합니다. 테스트에서는 Kafka 없이 실행되도록 이 값을 `false`로 둡니다.

`order.kafka.enabled=false`로 설정하면 주문 이벤트도 Kafka producer bean 대신 no-op publisher를 사용합니다.

## Kafka 이벤트

쿠폰 발급 성공 시 `coupon-issued` topic으로 다음 형태의 이벤트를 발행합니다.

```json
{
  "couponIssueId": 1,
  "couponId": 1,
  "userId": "user-1",
  "status": "ISSUED",
  "issuedAt": "2026-05-06T16:30:00"
}
```

주문 생성 또는 상태 변경 시 `order-events` topic으로 다음 형태의 이벤트를 발행합니다.

```json
{
  "eventId": 1,
  "orderId": "ORD-0001",
  "type": "CREATED",
  "message": "ORD-0001 주문이 접수되었습니다.",
  "previousStatus": null,
  "status": "PAYMENT_CONFIRMED",
  "paymentMethod": "EASY_PAY",
  "amount": 45000,
  "occurredAt": "2026-05-20T11:10:00"
}
```

로컬에서 topic 메시지를 직접 확인하려면 Kafka 컨테이너가 실행 중인 상태에서 다음 명령을 사용할 수 있습니다.

```bash
docker exec -it coupon-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic coupon-issued --from-beginning
```

주문 이벤트는 다음 명령으로 확인할 수 있습니다.

```bash
docker exec -it coupon-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

## API

### 쿠폰 캠페인 생성

```http
POST /api/coupons
Content-Type: application/json

{
  "name": "웰컴 쿠폰",
  "maxCount": 100,
  "startAt": "2026-05-06T10:00:00",
  "endAt": "2026-05-31T23:59:59"
}
```

응답 예시:

```json
{
  "couponId": 1,
  "name": "웰컴 쿠폰",
  "maxCount": 100,
  "startAt": "2026-05-06T10:00:00",
  "endAt": "2026-05-31T23:59:59"
}
```

### 쿠폰 캠페인 조회

```http
GET /api/coupons/1
```

응답 예시:

```json
{
  "couponId": 1,
  "name": "웰컴 쿠폰",
  "maxCount": 100,
  "startAt": "2026-05-06T10:00:00",
  "endAt": "2026-05-31T23:59:59"
}
```

### 쿠폰 캠페인 목록 조회

최신 생성 쿠폰이 먼저 오도록 `couponId` 내림차순으로 반환합니다.

```http
GET /api/coupons
```

응답 예시:

```json
[
  {
    "couponId": 2,
    "name": "신규 쿠폰",
    "maxCount": 50,
    "startAt": "2026-05-18T10:00:00",
    "endAt": "2026-12-31T23:59:59"
  },
  {
    "couponId": 1,
    "name": "웰컴 쿠폰",
    "maxCount": 100,
    "startAt": "2026-05-06T10:00:00",
    "endAt": "2026-05-31T23:59:59"
  }
]
```

### 쿠폰 캠페인 삭제

쿠폰 메타데이터, 남은 수량, 발급 사용자 Set, 발급 이력, 집계 현황을 Redis에서 함께 삭제합니다.

```http
DELETE /api/coupons/1
```

응답:

```http
204 No Content
```

### 쿠폰 발급

```http
POST /api/coupons/1/issues
Content-Type: application/json

{
  "userId": "user-1"
}
```

응답 예시:

```json
{
  "couponIssueId": 1,
  "couponId": 1,
  "userId": "user-1",
  "status": "ISSUED",
  "issuedAt": "2026-05-06T16:30:00",
  "canceledAt": null
}
```

### 쿠폰 발급 내역 단건 조회

```http
GET /api/coupons/1/issues/user-1
```

응답 예시:

```json
{
  "couponIssueId": 1,
  "couponId": 1,
  "userId": "user-1",
  "status": "ISSUED",
  "issuedAt": "2026-05-06T16:30:00",
  "canceledAt": null
}
```

### 쿠폰 발급 내역 목록 조회

```http
GET /api/coupons/1/issues?status=ISSUED&page=0&size=20
```

응답 예시:

```json
{
  "contents": [
    {
      "couponIssueId": 1,
      "couponId": 1,
      "userId": "user-1",
      "status": "ISSUED",
      "issuedAt": "2026-05-06T16:30:00",
      "canceledAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 쿠폰 남은 수량 조회

```http
GET /api/coupons/1/remaining
```

응답 예시:

```json
{
  "remainingCount": 99
}
```

### 쿠폰 발급 집계 조회

Flink가 Kafka 발급 이벤트를 처리해 Redis에 반영한 집계입니다.

```http
GET /api/coupons/1/statistics
```

응답 예시:

```json
{
  "couponId": 1,
  "issuedCount": 1,
  "lastIssuedAt": "2026-05-06T16:30:00",
  "updatedAt": "2026-05-06T16:30:01"
}
```

### 쿠폰 발급 취소

```http
DELETE /api/coupons/1/issues/user-1
```

응답 예시:

```json
{
  "couponIssueId": 1,
  "couponId": 1,
  "userId": "user-1",
  "status": "CANCELED",
  "issuedAt": "2026-05-06T16:30:00",
  "canceledAt": "2026-05-06T16:35:00"
}
```

## 실시간 주문관리 API

프론트 `실시간 주문 관리` 화면에서 필요한 기본 API입니다.

### 주문 생성

```http
POST /api/orders
```

요청 예시:

```json
{
  "customerName": "김민준",
  "productName": "스타벅스 아메리카노 쿠폰 10매",
  "paymentMethod": "EASY_PAY",
  "amount": 45000,
  "couponCode": "WELCOME-15",
  "address": "서울 강남구"
}
```

응답 예시:

```json
{
  "orderId": "ORD-0001",
  "customerName": "김민준",
  "productName": "스타벅스 아메리카노 쿠폰 10매",
  "status": "PAYMENT_CONFIRMED",
  "paymentMethod": "EASY_PAY",
  "amount": 45000,
  "couponCode": "WELCOME-15",
  "address": "서울 강남구",
  "risk": "NORMAL",
  "createdAt": "2026-05-20T11:10:00",
  "updatedAt": "2026-05-20T11:10:00"
}
```

### 주문 목록 조회

```http
GET /api/orders?status=PAYMENT_CONFIRMED&query=김민준&page=0&size=20
```

`status`와 `query`는 선택 값입니다. 최신 주문이 먼저 반환됩니다.

### 주문 상세 조회

```http
GET /api/orders/ORD-0001
```

### 주문 상태 변경

```http
PATCH /api/orders/ORD-0001/status
```

요청 예시:

```json
{
  "status": "PREPARING"
}
```

사용 가능한 주문 상태:

- `PAYMENT_CONFIRMED`: 결제 확인
- `PREPARING`: 상품 준비
- `SHIPPING`: 배송중
- `COMPLETED`: 완료
- `ON_HOLD`: 보류

사용 가능한 결제 수단:

- `CARD`: 카드
- `EASY_PAY`: 간편결제
- `BANK_TRANSFER`: 계좌이체

### 주문 요약 지표 조회

Flink가 `order-events` topic을 처리해 Redis `order:statistics:summary`에 반영한 집계입니다. 주문 Flink job이 아직 실행되지 않았거나 집계 키가 없으면 API 서버가 Redis 주문 원본을 기준으로 계산해 응답합니다.

```http
GET /api/orders/summary
```

응답 예시:

```json
{
  "activeOrderCount": 3,
  "paymentConfirmedCount": 1,
  "preparingCount": 1,
  "shippingCount": 1,
  "completedCount": 2,
  "onHoldCount": 0,
  "todayRevenue": 153000
}
```

### 실시간 주문 이벤트 조회

Flink가 Redis `order:statistics:recent-events`에 반영한 최근 이벤트 피드입니다. 주문 Flink job이 아직 실행되지 않았거나 집계 이벤트가 없으면 API 서버가 Redis `order:events`에 저장한 이벤트 로그를 기준으로 응답합니다.

```http
GET /api/orders/events?limit=20
```

응답 예시:

```json
[
  {
    "eventId": 2,
    "orderId": "ORD-0001",
    "type": "STATUS_CHANGED",
    "message": "ORD-0001 상태가 PAYMENT_CONFIRMED에서 PREPARING로 변경되었습니다.",
    "occurredAt": "2026-05-20T11:12:00"
  }
]
```

## 에러 응답

이미 발급된 사용자, 쿠폰 소진, 존재하지 않는 발급 내역, 존재하지 않는 주문 등은 다음 형태로 응답합니다.

```json
{
  "message": "이미 발급된 사용자입니다.",
  "timestamp": "2026-05-06T16:30:00"
}
```

주요 상태 코드:

- `400 Bad Request`: 요청 값 검증 실패
- `404 Not Found`: 쿠폰, 발급 내역 또는 주문 없음
- `409 Conflict`: 중복 발급, 쿠폰 소진, 발급 기간 아님, 이미 취소된 쿠폰

## 테스트

동시성 통합 테스트는 Testcontainers로 Redis를 실행하므로 Docker가 필요합니다. Docker를 사용할 수 없는 환경에서는 해당 통합 테스트가 자동으로 스킵됩니다.

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
