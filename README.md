# Coupon Publish

Spring Boot, Redis, Kafka, Apache Flink를 사용한 쿠폰 발급 및 주문 관리 프로젝트입니다.

이 프로젝트는 두 도메인을 분리해서 다룹니다.

- 쿠폰 발급: Redis Lua Script로 수량 차감과 중복 발급 방지를 atomic 하게 처리하고, Kafka/Flink로 발급 집계를 갱신합니다.
- 주문 관리: Redis에 주문 원본과 이벤트 로그를 저장하고, Kafka/Flink로 주문 상태별 수량, 오늘 매출, 최근 이벤트 피드를 집계합니다.

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

## 아키텍처

### 공통 구성

```text
Spring Boot API
  ├─ Redis: 원본 데이터, 이벤트 로그, 집계 데이터 저장
  ├─ Kafka: 도메인 이벤트 발행
  └─ Flink: Kafka 이벤트 소비 후 Redis 집계 키 갱신
```

API 서버와 Flink job은 별도 프로세스로 실행합니다. API 서버는 요청 처리와 Kafka 발행을 담당하고, Flink job은 Kafka topic을 계속 구독하면서 Redis 집계 키만 갱신합니다.

### 쿠폰 발급 흐름

```text
POST /api/coupons/{couponId}/issues
  → Redis 쿠폰 메타데이터 조회
  → Redis Lua Script로 중복 발급/남은 수량 확인 및 차감
  → Redis 발급 이력 저장
  → Kafka coupon-issued 이벤트 발행
  → Flink 쿠폰 집계 job이 Redis coupon:{couponId}:statistics 갱신
```

쿠폰 발급은 트래픽이 몰리는 순간의 정합성이 중요하므로 Redis Lua Script가 핵심입니다. Lua Script 안에서 사용자 발급 여부 확인, 남은 수량 확인, 남은 수량 감소, 발급 사용자 Set 등록을 한 번에 수행합니다.

### 주문 관리 흐름

```text
POST /api/orders 또는 PATCH /api/orders/{orderId}/status
  → Redis 주문 원본 저장
  → Redis 주문 이벤트 로그 저장
  → Kafka order-events 이벤트 발행
  → Flink 주문 집계 job이 Redis order:statistics:* 키 갱신
```

주문 API는 Redis에 현재 주문 상태를 저장합니다. 주문 생성/상태 변경 이벤트는 Kafka로 발행되고, Flink가 상태별 주문 수, 오늘 매출, 최근 이벤트 피드를 Redis 집계 키에 반영합니다.

## 프로젝트 구조

```text
src/main/java/com/example/couponpublish
├── CouponPublishApplication.java
├── coupon
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── event
│   ├── flink
│   ├── repository
│   └── service
└── order
    ├── config
    ├── controller
    ├── dto
    ├── entity
    ├── event
    ├── flink
    ├── repository
    └── service
```

## 로컬 실행

### 인프라 실행

Redis, Redis UI, Kafka, Kafka UI는 `docker-compose.yml`로 실행합니다.

```bash
docker compose up -d
```

### 애플리케이션 실행

macOS/Linux:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

기본 API 주소는 `http://localhost:8080`입니다.

### Flink Job 실행

쿠폰과 주문은 topic, 이벤트 스키마, 집계 로직이 달라서 Flink job을 따로 실행합니다.

쿠폰 발급 집계:

```bash
./gradlew runCouponStatisticsFlinkJob
```

Windows PowerShell:

```powershell
.\gradlew.bat runCouponStatisticsFlinkJob
```

주문 관리 집계:

```bash
./gradlew runOrderStatisticsFlinkJob
```

Windows PowerShell:

```powershell
.\gradlew.bat runOrderStatisticsFlinkJob
```

기본값은 다음과 같습니다.

```text
Kafka bootstrap servers: localhost:9092
Coupon topic: coupon-issued
Order topic: order-events
Coupon Flink consumer group: coupon-flink-statistics
Order Flink consumer group: order-flink-statistics
Redis: localhost:6379
```

다른 주소를 쓰려면 system property로 넘길 수 있습니다.

```powershell
.\gradlew.bat -Dcoupon.flink.kafka.bootstrap-servers=localhost:9092 -Dcoupon.flink.redis.host=localhost -Dcoupon.flink.redis.port=6379 runCouponStatisticsFlinkJob
```

```powershell
.\gradlew.bat -Dorder.flink.kafka.bootstrap-servers=localhost:9092 -Dorder.flink.redis.host=localhost -Dorder.flink.redis.port=6379 runOrderStatisticsFlinkJob
```

### 관리 UI

```text
Kafka UI: http://localhost:8081
Redis UI: http://localhost:8082
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

`coupon.kafka.enabled=false`로 설정하면 쿠폰 이벤트 publisher가 no-op으로 동작합니다. `order.kafka.enabled=false`로 설정하면 주문 이벤트 publisher가 no-op으로 동작합니다.

## 쿠폰 도메인

### 주요 기능

- 쿠폰 캠페인 생성, 조회, 삭제
- 쿠폰별 발급 기간 관리
- 쿠폰별 최대 발급 수량 제한
- 동일 사용자 중복 발급 방지
- Redis Lua Script 기반 atomic 발급 처리
- 쿠폰 발급 취소
- Kafka `coupon-issued` 이벤트 발행
- Flink 기반 쿠폰별 발급 집계

### Kafka 이벤트

쿠폰 발급 성공 시 `coupon-issued` topic으로 이벤트를 발행합니다.

```json
{
  "couponIssueId": 1,
  "couponId": 1,
  "userId": "user-1",
  "status": "ISSUED",
  "issuedAt": "2026-05-06T16:30:00"
}
```

topic 메시지 확인:

```bash
docker exec -it coupon-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic coupon-issued --from-beginning
```

### Redis 집계 키

Flink 쿠폰 집계 job은 `coupon-issued` 이벤트를 처리해 다음 Redis 키를 갱신합니다.

```text
coupon:{couponId}:statistics
coupon:{couponId}:statistics-events
```

### API

쿠폰 캠페인 생성:

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

쿠폰 캠페인 조회:

```http
GET /api/coupons/1
```

쿠폰 캠페인 목록 조회:

```http
GET /api/coupons
```

쿠폰 캠페인 삭제:

```http
DELETE /api/coupons/1
```

쿠폰 발급:

```http
POST /api/coupons/1/issues
Content-Type: application/json

{
  "userId": "user-1"
}
```

쿠폰 발급 내역 단건 조회:

```http
GET /api/coupons/1/issues/user-1
```

쿠폰 발급 내역 목록 조회:

```http
GET /api/coupons/1/issues?status=ISSUED&page=0&size=20
```

쿠폰 남은 수량 조회:

```http
GET /api/coupons/1/remaining
```

쿠폰 발급 집계 조회:

```http
GET /api/coupons/1/statistics
```

쿠폰 발급 취소:

```http
DELETE /api/coupons/1/issues/user-1
```

## 주문 도메인

### 주요 기능

- 주문 생성, 목록 조회, 상세 조회
- 주문 상태 변경
- Redis 주문 원본 저장
- Redis 주문 이벤트 로그 저장
- Kafka `order-events` 이벤트 발행
- Flink 기반 상태별 주문 수, 오늘 매출, 최근 이벤트 집계

### Kafka 이벤트

주문 생성 또는 상태 변경 시 `order-events` topic으로 이벤트를 발행합니다.

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

topic 메시지 확인:

```bash
docker exec -it coupon-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

### Redis 집계 키

Flink 주문 집계 job은 `order-events` 이벤트를 처리해 다음 Redis 키를 갱신합니다.

```text
order:statistics:summary
order:statistics:recent-events
order:statistics:events
```

API 서버는 주문 요약과 최근 이벤트 조회 시 Flink 집계 키를 먼저 읽습니다. Flink job이 아직 실행되지 않았거나 집계 키가 없으면 Redis 주문 원본과 `order:events` 로그를 기준으로 응답합니다.

### 주문 상태

- `PAYMENT_CONFIRMED`: 결제 확인
- `PREPARING`: 상품 준비
- `SHIPPING`: 배송중
- `COMPLETED`: 완료
- `ON_HOLD`: 보류

### 결제 수단

- `CARD`: 카드
- `EASY_PAY`: 간편결제
- `BANK_TRANSFER`: 계좌이체

### API

주문 생성:

```http
POST /api/orders
Content-Type: application/json

{
  "customerName": "김민준",
  "productName": "스타벅스 아메리카노 쿠폰 10매",
  "paymentMethod": "EASY_PAY",
  "amount": 45000,
  "couponCode": "WELCOME-15",
  "address": "서울 강남구"
}
```

주문 목록 조회:

```http
GET /api/orders?status=PAYMENT_CONFIRMED&query=김민준&page=0&size=20
```

주문 상세 조회:

```http
GET /api/orders/ORD-0001
```

주문 상태 변경:

```http
PATCH /api/orders/ORD-0001/status
Content-Type: application/json

{
  "status": "PREPARING"
}
```

주문 요약 지표 조회:

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

최근 주문 이벤트 조회:

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

## 장애 시나리오

### 쿠폰 Redis 처리 실패

Redis가 장애 상태라면 쿠폰 발급을 진행하지 않습니다. Redis가 쿠폰 상태, 발급 이력, 빠른 수량 차감, 1차 중복 방어를 맡고 있기 때문입니다.

Redis에서 수량 차감과 사용자 등록이 성공했지만 발급 이력 저장이 실패하면 `CouponService`가 `couponRedisRepository.rollbackIssue(couponId, userId)`를 호출해 차감 내용을 되돌립니다.

### Kafka 장애

Kafka 브로커가 내려가 있으면 Kafka publisher를 사용하는 API는 실패할 수 있습니다. 운영 환경에서는 이벤트 유실을 더 강하게 막기 위해 Redis Streams나 별도 outbox 저장소를 두고 relay가 Kafka로 재시도 발행하는 구조를 고려할 수 있습니다.

### Flink Job 미실행

Flink job이 실행되지 않으면 Kafka 이벤트는 topic에 쌓이지만 Redis 집계 키는 갱신되지 않습니다. 쿠폰 집계 API는 집계 키가 없으면 0 또는 빈 집계로 응답하고, 주문 요약/최근 이벤트 API는 Redis 원본 데이터와 이벤트 로그로 fallback합니다.

## 에러 응답

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

동시성 통합 테스트는 Testcontainers로 Redis를 실행하므로 Docker가 필요합니다. Docker를 사용할 수 없는 환경에서는 해당 통합 테스트가 자동으로 스킵됩니다.

## 로컬 인프라 종료

```bash
docker compose down
```

볼륨까지 삭제하려면 다음 명령을 사용합니다.

```bash
docker compose down -v
```
