# Vạn Thư Các — Book Marketplace Backend

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-8-red)
![CI](https://github.com/minhdao-dev/vanthucac/actions/workflows/ci.yml/badge.svg)

Spring Boot backend cho hệ thống sàn giao dịch sách, hỗ trợ B2C, C2C với escrow flow và đấu giá sách real-time qua
WebSocket.

> Portfolio project — Rebuild từ đồ án tốt nghiệp JSP/Servlet thành Spring Boot marketplace theo hướng production-ready
> backend.

---

## Business Models

| Model   | Mô tả                                                          |
|---------|----------------------------------------------------------------|
| B2C     | Platform bán sách mới trực tiếp cho người mua                  |
| C2C     | User đăng ký seller, đăng bán sách cũ, giao dịch qua escrow    |
| Auction | Phiên đấu giá sách quý hiếm, real-time broadcast qua WebSocket |

---

## Tech Stack

| Layer           | Công nghệ                                            |
|-----------------|------------------------------------------------------|
| Language        | Java 21                                              |
| Framework       | Spring Boot 3.x                                      |
| Security        | Spring Security + OAuth2 Resource Server + JWT HS256 |
| ORM             | Spring Data JPA + Hibernate                          |
| Database        | MySQL 8.0                                            |
| Cache / Session | Redis 8                                              |
| Realtime        | Spring WebSocket STOMP                               |
| Storage         | AWS S3 Presigned URL                                 |
| Migration       | Flyway (17 migrations)                               |
| Docs            | SpringDoc OpenAPI / Swagger                          |
| Build           | Maven Wrapper                                        |
| Deploy          | Docker + Docker Compose                              |
| CI              | GitHub Actions                                       |

---

## Architecture

```
Client
  │
  ▼
Rate Limit Filter (Redis — atomic Lua script)
  │
  ▼
Spring Security Filter Chain
  │  JWT verify stateless — không gọi DB/Redis trong happy path
  ▼
Controller Layer
  │
  ▼
Service Layer
  │  Transaction boundary rõ ràng
  │  Outbox pattern cho async events
  │  Pessimistic locking cho stock checkout
  │  Pessimistic locking cho auction bid
  ▼
Repository Layer
  │
  ├── MySQL 8.0
  ├── Redis 8 (token store, rate limit, cache)
  ├── AWS S3 (file upload)
  └── WebSocket Broker (real-time bid broadcast)
```

**Package structure — Modular Monolith:**

```
com.vanthucac
├── auth/         JWT, refresh token rotation, reuse detection
├── user/         Profile, upgrade to seller
├── seller/       Seller profile, wallet, wallet transactions
├── catalog/      Book catalog, Google Books API integration
├── listing/      Book listings, admin review
├── cart/         Cart management
├── order/        Checkout, parent/sub-order, escrow flow
├── payment/      Payment, escrow release, commission
├── auction/      Auction session, bidding, scheduler
├── notification/ Email, in-app notification
├── audit/        Audit log
└── common/       Security, outbox, exception, config
```

---

## Key Technical Decisions

### Authentication

- JWT access token — TTL 15 phút, stateless verify, không gọi Redis
- Refresh token — opaque 256-bit, lưu SHA-256 hash trong Redis
- Token rotation — single-use, atomic Lua script tránh race condition
- Reuse detection — lưu used token hash, revoke cả token family khi phát hiện

### Concurrency

- Stock checkout — `SELECT FOR UPDATE` với deadlock avoidance (sort by listing ID trước khi lock)
- Auction bid — `SELECT FOR UPDATE`, `@Transactional(REQUIRES_NEW)` per session
- `StockService` annotated `@Transactional(propagation = MANDATORY)` — fail fast nếu gọi ngoài transaction

### Outbox Pattern

- Event được lưu trong cùng transaction với business data
- Processor retry với exponential backoff: 10s → 30s → 1m → 5m → 15m
- Mỗi event xử lý trong `@Transactional(REQUIRES_NEW)` riêng biệt

### Escrow Flow

```
Buyer pays
  └─► Escrow HOLDING
        └─► Buyer confirms receipt
              └─► Escrow RELEASING
                    └─► Commission deducted
                          └─► Net amount credited to Seller Wallet
                                └─► Escrow RELEASED
```

Idempotency check trước khi credit — không bị double-credit khi retry.

### WebSocket

- Broadcast sau `afterCommit()` — tránh client nhận data nhưng DB rollback

---

## Core Features

- JWT authentication với access token + refresh token rotation
- Role-based access: `BUYER`, `SELLER`, `ADMIN`
- Seller onboarding, seller wallet và wallet transaction ledger
- Book catalog với Google Books API ISBN auto-fill
- Listing management với admin approve/reject workflow
- Cart và multi-seller checkout
- Parent order + seller sub-orders
- Mock payment provider + escrow flow
- Commission calculation (configurable rate)
- Real-time auction bidding qua WebSocket
- Async email/notification qua Outbox pattern
- AWS S3 presigned URL upload
- Flyway database migration (17 migrations)
- Rate limiting trên Redis (atomic, production-safe)
- Audit log cho các thao tác quan trọng
- Actuator health check, metrics, Prometheus endpoint
- Docker Compose cho local dev và production

---

## Yêu cầu

- Java 21
- Docker + Docker Compose

---

## Chạy local (development)

### 1. Clone

```bash
git clone https://github.com/minhdao-dev/vanthucac.git
cd vanthucac
```

### 2. Cấu hình môi trường

```bash
cp .env.example .env
```

Điền các biến trong `.env`. Tạo JWT secret:

```bash
openssl rand -base64 48
```

### 3. Khởi động MySQL và Redis

```bash
docker compose up -d
docker compose ps
```

### 4. Chạy ứng dụng

```bash
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Chạy production (Docker)

### 1. Cấu hình môi trường

```bash
cp .env.prod.example .env.prod
```

Điền đầy đủ credentials trong `.env.prod`.

### 2. Build và deploy

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

### 3. Kiểm tra

```bash
docker compose -f docker-compose.prod.yml ps
curl http://localhost/actuator/health
```

App healthy khi trả về `{"status":"UP"}`.

> Swagger UI bị tắt trong prod. Để bật tạm thời cho demo, thêm `SPRINGDOC_SWAGGER_UI_ENABLED=true` và
`SPRINGDOC_API_DOCS_ENABLED=true` vào `.env.prod`.

---

## Tài khoản Admin mặc định

```
Email:    admin@vanthucac.com
Password: Admin@123
```

> Chỉ dùng cho development/demo. Đổi mật khẩu hoặc thay thế seed data trước khi deploy production thật.

---

## Chạy tests

Tests dùng Testcontainers — cần Docker đang chạy.

```bash
chmod +x mvnw
./mvnw test
```

Integration tests bao gồm:

- `CheckoutIntegrationTest` — happy path, stock validation, concurrent race condition
- `AuctionBiddingIntegrationTest` — valid bid, bid validation, concurrent same-amount bid

---

## Environment Variables

| Variable                    | Bắt buộc | Mô tả                                        |
|-----------------------------|:--------:|----------------------------------------------|
| `JWT_SECRET`                |    ✅     | Secret ký JWT, tối thiểu 32 ký tự ngẫu nhiên |
| `DB_USERNAME`               |    ✅     | MySQL application user                       |
| `DB_PASSWORD`               |    ✅     | MySQL application password                   |
| `REDIS_PASSWORD`            | ✅ (prod) | Redis auth password                          |
| `MAIL_USERNAME`             |    ✅     | Gmail address                                |
| `MAIL_PASSWORD`             |    ✅     | Gmail app password                           |
| `AWS_ACCESS_KEY`            |    ✅     | AWS access key                               |
| `AWS_SECRET_KEY`            |    ✅     | AWS secret key                               |
| `AWS_REGION`                |    ✅     | AWS region (vd: `ap-southeast-1`)            |
| `AWS_S3_BUCKET`             |    ✅     | S3 bucket name                               |
| `GOOGLE_BOOKS_API_KEY`      |  Không   | Thiếu thì seller nhập sách thủ công          |
| `WEBSOCKET_ALLOWED_ORIGINS` | ✅ (prod) | Domain frontend được phép kết nối WebSocket  |

---

## WebSocket — Đấu giá real-time

```
Endpoint:  ws://localhost:8080/ws
Subscribe: /topic/auction/{itemId}
```

Khi bid được chấp nhận và transaction commit, server broadcast `BidBroadcastMessage` tới toàn bộ client đang subscribe
vào item đó.

---

## API Documentation

```
Swagger UI:   http://localhost:8080/swagger-ui.html   (dev only)
OpenAPI JSON: http://localhost:8080/v3/api-docs        (dev only)
```

---

## CI/CD

GitHub Actions tự động build và test khi push hoặc mở pull request vào `main` và `develop`.

```
.github/workflows/ci.yml
```

---

## Branching Strategy

```
main        stable, demo-ready
develop     integration branch
feature/*   tính năng mới
fix/*       bug fixes
refactor/*  refactoring nội bộ
chore/*     tooling, config, CI
```

Không commit trực tiếp vào `main`. Mọi thay đổi đi qua pull request từ feature/fix vào `develop`, sau đó merge
`develop` → `main` khi ổn định.

---

## Known Limitations

- Payment là mock — chưa tích hợp VNPay, MoMo hoặc provider thật
- Admin seed trong Flyway chỉ dùng cho development/demo

---

## License

Personal portfolio project.