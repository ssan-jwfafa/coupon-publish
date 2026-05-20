# Coupon Publish

Spring Boot, Redis, MySQL, Kafka, Apache Flink를 사용한 쿠폰 발급 및 주문 관리 프로젝트입니다.

이 프로젝트는 두 도메인을 분리해서 다룹니다.

- 쿠폰 발급: Redis Lua Script로 수량 차감과 중복 발급 방지를 atomic 하게 처리하고, MySQL outbox/Kafka/Flink로 발급 집계를 갱신합니다.
- 주문 관리: Redis에 주문 원본과 이벤트 로그를 저장하고, MySQL outbox/Kafka/Flink로 주문 상태별 수량, 오늘 매출, 최근 이벤트 피드를 집계합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.6
- Gradle
- Spring Web
- Spring Data Redis
- Spring JDBC
- Spring Kafka
- Apache Flink
- Redis 7.4
- MySQL 8.4
- Apache Kafka 3.8.1
- Docker Compose

## 아키텍처

### 공통 구성

```text
Spring Boot API
  ├─ Redis: 원본 데이터, 이벤트 로그, 집계 데이터 저장
  ├─ MySQL outbox_events: Kafka 발행 대기 이벤트 저장
  ├─ Outbox Relay: outbox 이벤트를 Kafka로 발행
  └─ Flink: Kafka 이벤트 소비 후 Redis 집계 키 갱신
```

API 서버와 Flink job은 별도 프로세스로 실행합니다. API 서버는 요청 처리와 MySQL outbox 저장을 담당하고, outbox relay가 Kafka 발행을 재시도합니다. Flink job은 Kafka topic을 계속 구독하면서 Redis 집계 키만 갱신합니다.

### 쿠폰 발급 흐름

```text
[Client]
   ↓
[Spring Boot Coupon API]
   ↓
[Redis Coupon Data]
   ↓
[Redis Lua Script]
   ↓
[Redis Issue History]
   ↓
[MySQL Outbox: outbox_events]
   ↓
[Outbox Relay]
   ↓
[Kafka: coupon-issued]
   ↓
[Flink: CouponStatisticsFlinkJob]
   ↓
[Redis Statistics: coupon:{couponId}:statistics]
```

```text
POST /api/coupons/{couponId}/issues
  → Redis 쿠폰 메타데이터 조회
  → Redis Lua Script로 중복 발급/남은 수량 확인 및 차감
  → Redis 발급 이력 저장
  → MySQL outbox_events에 coupon-issued 이벤트 저장
  → Outbox Relay가 Kafka coupon-issued 이벤트 발행
  → Flink 쿠폰 집계 job이 Redis coupon:{couponId}:statistics 갱신
```

쿠폰 발급은 트래픽이 몰리는 순간의 정합성이 중요하므로 Redis Lua Script가 핵심입니다. Lua Script 안에서 사용자 발급 여부 확인, 남은 수량 확인, 남은 수량 감소, 발급 사용자 Set 등록을 한 번에 수행합니다.

### 주문 관리 흐름

```text
[Client]
   ↓
[Spring Boot Order API]
   ↓
[Redis Order Data]
   ↓
[Redis Order Event Log]
   ↓
[MySQL Outbox: outbox_events]
   ↓
[Outbox Relay]
   ↓
[Kafka: order-events]
   ↓
[Flink: OrderStatisticsFlinkJob]
   ↓
[Redis Statistics: order:statistics:*]
```

```text
POST /api/orders 또는 PATCH /api/orders/{orderId}/status
  → Redis 주문 원본 저장
  → Redis 주문 이벤트 로그 저장
  → MySQL outbox_events에 order-events 이벤트 저장
  → Outbox Relay가 Kafka order-events 이벤트 발행
  → Flink 주문 집계 job이 Redis order:statistics:* 키 갱신
```

주문 API는 Redis에 현재 주문 상태를 저장합니다. 주문 생성/상태 변경 이벤트는 MySQL outbox에 저장되고, relay가 Kafka로 발행합니다. Flink가 상태별 주문 수, 오늘 매출, 최근 이벤트 피드를 Redis 집계 키에 반영합니다.

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
├── outbox
│   ├── OutboxEventRepository.java
│   ├── OutboxEventPublisher.java
│   └── OutboxRelay.java
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

MySQL, Redis, Redis UI, Kafka, Kafka UI는 `docker-compose.yml`로 실행합니다.

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
MySQL: localhost:3306/coupon_publish
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
  datasource:
    url: jdbc:mysql://localhost:3306/coupon_publish?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: coupon
    password: coupon
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

outbox:
  relay:
    enabled: true
    batch-size: 50
    max-attempts: 10
    fixed-delay: 1000
```

`coupon.kafka.enabled=false`로 설정하면 쿠폰 이벤트 publisher가 no-op으로 동작합니다. `order.kafka.enabled=false`로 설정하면 주문 이벤트 publisher가 no-op으로 동작합니다.

`outbox.relay.enabled=false`로 설정하면 outbox relay가 Kafka 발행을 멈춥니다. 이 경우 이벤트는 MySQL `outbox_events`에 남아 있고, relay를 다시 켜면 재발행 대상이 됩니다.

## 쿠폰 도메인

### 주요 기능

- 쿠폰 캠페인 생성, 조회, 삭제
- 쿠폰별 발급 기간 관리
- 쿠폰별 최대 발급 수량 제한
- 동일 사용자 중복 발급 방지
- Redis Lua Script 기반 atomic 발급 처리
- 쿠폰 발급 취소
- MySQL outbox 기반 `coupon-issued` 이벤트 발행
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
- MySQL outbox 기반 `order-events` 이벤트 발행
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

## 설계 의사결정 FAQ

### 왜 Redis Lua를 선택했는가?

쿠폰 발급은 동시에 많은 요청이 들어와도 남은 수량 차감과 사용자 중복 발급 체크가 반드시 한 번에 처리되어야 합니다.

Redis Lua Script는 Redis 서버 안에서 단일 스크립트를 atomic 하게 실행합니다. 이 프로젝트는 쿠폰 발급 시 다음 작업을 Lua Script 안에서 함께 수행합니다.

```text
1. 이미 발급한 userId인지 확인
2. 남은 수량이 있는지 확인
3. 남은 수량 1 감소
4. 발급 사용자 Set에 userId 추가
```

이렇게 하면 애플리케이션 서버가 여러 대이거나 요청이 동시에 몰려도 Redis 기준으로 중간 상태가 끼어들지 않습니다.

### 왜 DB unique만 안 썼는가?

DB unique 제약만으로도 동일 사용자 중복 발급은 막을 수 있습니다. 하지만 이 프로젝트의 핵심 문제는 중복 발급뿐 아니라 제한 수량 차감까지 함께 맞추는 것입니다.

DB unique만 사용하면 요청이 몰릴 때 다음 문제가 남습니다.

- 재고 확인과 재고 차감 사이에 경쟁 조건이 생길 수 있습니다.
- 중복 요청이 DB insert까지 도달하므로 DB write 부하가 커집니다.
- 수량 초과 상황을 막기 위해 row lock, pessimistic lock, serializable isolation 같은 추가 전략이 필요합니다.

그래서 이 프로젝트는 Redis Lua로 발급 순간의 1차 정합성과 부하 흡수를 처리합니다. 이벤트 발행 안정성은 MySQL `outbox_events`에 이벤트를 먼저 저장하고 relay가 Kafka로 재시도 발행하는 방식으로 보완합니다.

### 왜 Kafka를 넣었는가?

API 요청 처리와 후속 집계를 분리하기 위해 Kafka를 사용합니다.

쿠폰 발급이나 주문 상태 변경이 일어날 때마다 집계까지 API 요청 안에서 모두 처리하면 응답 경로가 길어지고 장애 영향도 커집니다. Kafka를 사이에 두면 API는 원본 상태 변경과 outbox 저장까지만 담당하고, relay와 Flink가 비동기로 발행/집계를 처리할 수 있습니다.

현재 topic은 다음처럼 나뉩니다.

```text
coupon-issued: 쿠폰 발급 성공 이벤트
order-events: 주문 생성/상태 변경 이벤트
```

### 왜 Flink가 필요한가?

Kafka는 이벤트를 전달하고 보관하는 역할이고, Flink는 이벤트를 계속 소비하면서 집계 상태를 만드는 역할입니다.

이 프로젝트에서는 Flink가 다음 일을 담당합니다.

- `coupon-issued`를 소비해 쿠폰별 발급 수와 마지막 발급 시각 갱신
- `order-events`를 소비해 상태별 주문 수, 오늘 매출, 최근 이벤트 피드 갱신
- 이미 처리한 이벤트를 Redis Set에 기록해 중복 집계 방지

단순 consumer로도 구현할 수 있지만, Flink를 사용하면 스트림 처리 구조를 명확히 분리할 수 있고 이후 window 집계, 지연 이벤트 처리, 복수 sink 확장 같은 방향으로 키우기 쉽습니다.

### 왜 Redis 집계를 했는가?

조회 API가 빠르게 응답하도록 집계 결과를 Redis에 저장합니다.

쿠폰 발급 수나 주문 요약 지표를 매번 원본 전체에서 계산하면 데이터가 늘수록 조회 비용이 커집니다. Flink가 이벤트를 받을 때마다 Redis 집계 키를 갱신해두면 API는 이미 계산된 값을 읽기만 하면 됩니다.

현재 주요 집계 키는 다음과 같습니다.

```text
coupon:{couponId}:statistics
order:statistics:summary
order:statistics:recent-events
```

Redis 집계는 빠른 대시보드/조회용 read model입니다. 영구 원장이 필요한 운영 환경에서는 RDB, object storage, compacted topic 같은 별도 원장을 함께 두는 편이 좋습니다.

### 왜 MySQL outbox를 추가했는가?

Redis 상태 변경 후 Kafka에 직접 발행하면, 상태 저장은 성공했지만 Kafka 발행 직전에 API 서버가 죽는 경우 이벤트가 유실될 수 있습니다.

outbox 패턴은 이벤트를 먼저 MySQL `outbox_events`에 저장하고, 별도 relay가 Kafka 발행을 맡습니다. Kafka 발행에 실패해도 outbox row가 `FAILED` 상태로 남고, relay가 다시 처리할 수 있습니다.

```text
API
  → Redis business state 저장
  → MySQL outbox_events 저장
  → Outbox Relay가 Kafka 발행
  → 발행 성공 시 outbox_events.status = PUBLISHED
```

현재 구현은 Redis와 MySQL을 하나의 분산 트랜잭션으로 묶지는 않습니다. 다만 Kafka 직접 발행보다 이벤트 유실 가능성을 줄이고, 재시도 가능한 발행 기준점을 MySQL에 남깁니다.

### 장애 시 정합성은 어떻게 맞추는가?

쿠폰 발급의 1차 정합성은 Redis Lua Script가 담당합니다. Redis Lua가 성공하면 남은 수량 차감과 사용자 등록은 Redis 기준으로 함께 반영됩니다.

발급 이력 저장이 실패하면 `CouponService`가 Redis 차감을 rollback합니다.

```text
Redis Lua 발급 성공
  → 발급 이력 저장 실패
  → couponRedisRepository.rollbackIssue(couponId, userId)
  → 사용자 Set 제거 및 남은 수량 복구
```

Kafka 또는 Flink 장애가 발생하면 원본 상태와 집계 상태 사이에 지연이 생길 수 있습니다.

- Kafka 발행 실패: outbox relay가 실패 횟수와 에러를 저장하고 다음 주기에 재시도합니다.
- Flink 미실행: Kafka topic에는 이벤트가 쌓이지만 Redis 집계 키는 갱신되지 않습니다.
- Flink 재시작: consumer group offset 기준으로 이어서 처리합니다.
- 이벤트 중복 처리: Flink sink가 Redis Set에 eventId를 기록해 같은 이벤트의 중복 집계를 방지합니다.

주문 API는 Flink 집계가 없을 때 Redis 주문 원본과 이벤트 로그로 fallback합니다. 쿠폰 집계는 Redis 집계 키 기준으로 조회하므로 Flink가 멈춘 동안에는 집계 값이 늦게 반영될 수 있습니다.

더 강한 운영 정합성이 필요하면 비즈니스 원장도 MySQL에 저장하고, 원장 변경과 outbox insert를 같은 DB transaction으로 묶는 구조가 적합합니다.

## 장애 시나리오

### 쿠폰 Redis 처리 실패

Redis가 장애 상태라면 쿠폰 발급을 진행하지 않습니다. Redis가 쿠폰 상태, 발급 이력, 빠른 수량 차감, 1차 중복 방어를 맡고 있기 때문입니다.

Redis에서 수량 차감과 사용자 등록이 성공했지만 발급 이력 저장이 실패하면 `CouponService`가 `couponRedisRepository.rollbackIssue(couponId, userId)`를 호출해 차감 내용을 되돌립니다.

### Kafka 장애

Kafka 브로커가 내려가 있으면 outbox relay가 발행에 실패한 이벤트를 `FAILED` 상태로 남기고 다음 주기에 재시도합니다. API 요청은 Kafka 브로커 상태와 직접 연결되지 않고, MySQL outbox 저장까지만 수행합니다.

### MySQL 장애

MySQL이 내려가 있으면 outbox 이벤트를 저장할 수 없으므로 Kafka로 전달되어야 하는 쿠폰/주문 이벤트 API는 실패할 수 있습니다. 이 경우 Redis에만 상태가 반영되지 않도록, 쿠폰 발급은 예외 발생 시 Redis 차감을 rollback합니다.

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
