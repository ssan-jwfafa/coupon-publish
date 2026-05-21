# Coupon Publish

Spring Boot, Redis, MySQL, Kafka, Apache Flink 기반의 쿠폰 발급 및 주문 관리 프로젝트입니다.

쿠폰 발급은 Redis Lua Script로 선착순 수량 차감과 중복 발급 방지를 atomic 하게 처리하고, 주문 관리는 Redis에 주문 원본과 이벤트 로그를 저장합니다. 두 도메인 모두 MySQL outbox, Kafka, Flink를 거쳐 Redis 조회용 집계를 비동기로 갱신합니다.

## Technologies

![Coupon Publish 기술 스택](docs/image/coupon-publish-tech-stack.svg)

## Flow

### Coupon Issuance

![쿠폰 발급 Flow](docs/image/coupon-issue-flow.svg)

```text
POST /api/coupons/{couponId}/issues
  -> Redis Lua Script로 중복 발급/남은 수량 확인 및 차감
  -> 쿠폰 발급 이력 저장
  -> MySQL outbox_events 저장
  -> OutboxRelay가 Kafka coupon-issued 발행
  -> Flink가 Redis coupon:{couponId}:statistics 갱신
```

### Order Lifecycle

![주문 관리 Flow](docs/image/order-lifecycle-flow.svg)

```text
POST /api/orders 또는 PATCH /api/orders/{orderId}/status
  -> Redis 주문 원본 저장
  -> Redis 주문 이벤트 로그 저장
  -> MySQL outbox_events 저장
  -> OutboxRelay가 Kafka order-events 발행
  -> Flink가 Redis order:statistics:* 갱신
```

## Architecture

![System Architecture](docs/image/system-architecture.svg)

```text
Spring Boot API
  ├─ coupon: 쿠폰 캠페인, 발급 이력, Redis Lua 기반 발급 처리
  ├─ order: 주문 원본, 주문 이벤트 로그, 주문 조회/요약
  ├─ outbox: 이벤트 저장 및 Kafka 재시도 발행
  └─ flink: Kafka 이벤트 소비 후 Redis 집계 갱신
```

API 서버와 Flink job은 별도 프로세스로 실행합니다. API는 요청 처리와 outbox 저장까지만 담당하고, Kafka/Flink 경로가 조회용 집계를 비동기로 만듭니다.

## Project Structure

```text
src/main/java/com/example/couponpublish
├── coupon
│   ├── controller
│   ├── service
│   ├── repository
│   ├── event
│   └── flink
├── order
│   ├── controller
│   ├── service
│   ├── repository
│   ├── event
│   └── flink
└── outbox
    ├── OutboxEventRepository.java
    ├── OutboxEventPublisher.java
    └── OutboxRelay.java
```

## Local Run

### Docker Compose

MySQL, Redis, Kafka, Kafka UI, Redis UI를 실행합니다.

```powershell
docker compose up -d
```

API 서버:

```powershell
.\gradlew.bat bootRun
```

Flink 집계 job:

```powershell
.\gradlew.bat runCouponStatisticsFlinkJob
.\gradlew.bat runOrderStatisticsFlinkJob
```

접속 주소:

```text
API:      http://localhost:8080
Kafka UI: http://localhost:8081
Redis UI: http://localhost:8082
```

종료:

```powershell
docker compose down
```

## Docker Desktop Kubernetes

Docker Compose 대신 Docker Desktop Kubernetes로도 실행할 수 있습니다.

```powershell
kubectl config use-context docker-desktop
docker compose down

docker build -t coupon-publish:local .
kubectl apply -k k8s

kubectl -n coupon-publish get pods -w
```

`get pods -w`는 상태 감시 명령이라 자동으로 끝나지 않습니다. Pod들이 `Running`이 되면 `Ctrl + C`로 종료하면 됩니다.

코드 변경 후 재배포:

```powershell
docker build -t coupon-publish:local .
kubectl -n coupon-publish rollout restart deploy/coupon-publish-api deploy/coupon-statistics-flink deploy/order-statistics-flink
```

삭제:

```powershell
kubectl delete -k k8s
```

Docker Desktop Kubernetes가 `kind` 방식이면 로컬 이미지가 노드에서 보이지 않아 `ErrImageNeverPull`이 발생할 수 있습니다. 로컬 개발에서는 `kubeadm` 방식으로 클러스터를 만들거나, 이미지를 registry에 push한 뒤 매니페스트의 image 값을 registry 주소로 변경합니다.

## GitHub Actions

Workflow는 두 개입니다.

- `.github/workflows/build-and-push-ghcr.yml`: 테스트 후 GHCR 이미지 빌드/푸시
- `.github/workflows/docker-desktop-k8s.yml`: self-hosted runner에서 Docker Desktop Kubernetes 배포

Docker Desktop Kubernetes는 로컬 PC 안에 있으므로 GitHub-hosted runner가 직접 접근할 수 없습니다. push만으로 로컬 클러스터에 배포하려면 GitHub repository의 `Settings > Actions > Runners`에서 Windows self-hosted runner를 설치하고 실행해야 합니다.

## 주요 API

### Coupon

```http
POST   /api/coupons
GET    /api/coupons
GET    /api/coupons/{couponId}
DELETE /api/coupons/{couponId}

POST   /api/coupons/{couponId}/issues
GET    /api/coupons/{couponId}/issues/{userId}
GET    /api/coupons/{couponId}/issues
GET    /api/coupons/{couponId}/remaining
GET    /api/coupons/{couponId}/statistics
DELETE /api/coupons/{couponId}/issues/{userId}
```

### Order

```http
POST  /api/orders
GET   /api/orders
GET   /api/orders/{orderId}
PATCH /api/orders/{orderId}/status
GET   /api/orders/summary
GET   /api/orders/events
```

## 주요 설정

기본 설정 파일은 `src/main/resources/application.yml`입니다.

```text
Redis: localhost:6379
MySQL: localhost:3306/coupon_publish
Kafka: localhost:9092

Coupon topic: coupon-issued
Order topic: order-events
Outbox relay: enabled
```

Kubernetes에서는 매니페스트의 환경 변수로 `redis`, `mysql`, `kafka` 서비스 주소를 주입합니다.

## Test

```powershell
.\gradlew.bat test
```

전체 빌드:

```powershell
.\gradlew.bat build
```

동시성 통합 테스트는 Testcontainers로 Redis를 실행하므로 Docker가 필요합니다.
