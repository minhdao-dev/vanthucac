# Vạn Thư Các — Book Marketplace Backend

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Database](https://img.shields.io/badge/Database-MySQL%208-blue)
![Cache](https://img.shields.io/badge/Cache-Redis-red)

Spring Boot backend cho hệ thống sàn giao dịch sách, hỗ trợ B2C, C2C, escrow flow và đấu giá sách theo thời gian thực.

> Portfolio project — Rebuild từ đồ án tốt nghiệp JSP/Servlet thành Spring Boot marketplace theo hướng production-inspired backend.

---

## Business Models

| Model   | Mô tả                                                               |
|---------|---------------------------------------------------------------------|
| B2C     | Platform bán sách mới trực tiếp                                     |
| C2C     | User đăng ký seller, đăng bán sách cũ và xử lý giao dịch qua escrow |
| Auction | Phiên đấu giá sách quý hiếm theo thời gian thực qua WebSocket       |

---

## Core Features

- Authentication với JWT access token và refresh token.
- Refresh token rotation, Redis session store và reuse detection.
- Role-based access control cho user, seller và admin.
- Seller onboarding và seller wallet.
- Book catalog và listing management.
- Admin approve/reject listing.
- Cart và checkout flow.
- Parent order, sub-order theo seller và escrow transaction.
- Commission calculation và release tiền về seller wallet.
- Auction session, bid, winner selection và real-time broadcast qua WebSocket.
- Notification và email async.
- AWS S3 presigned URL upload.
- Flyway database migration.
- Docker Compose cho local development.
- Swagger/OpenAPI documentation.

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
| Migration       | Flyway                                               |
| Docs            | SpringDoc OpenAPI                                    |
| Build Tool      | Maven Wrapper                                        |
| Deploy          | Docker + Docker Compose                              |

---

## Architecture Overview

```text
Client
  |
  v
Spring Security / JWT Filter
  |
  v
Controller Layer
  |
  v
Service Layer / Domain Use Cases
  |
  v
Repository Layer
  |
  +--> MySQL
  +--> Redis
  +--> AWS S3
  +--> Mail Provider
  +--> WebSocket Broker
```

---

## Main Business Flow

```text
User registers / logs in
  |
  v
User becomes seller
  |
  v
Seller creates book listing
  |
  v
Admin approves listing
  |
  v
Buyer adds listing to cart
  |
  v
Buyer checks out
  |
  v
System creates parent order and seller sub-orders
  |
  v
Payment is created
  |
  v
Escrow holds money for each seller sub-order
  |
  v
Order is completed
  |
  v
System releases escrow, deducts commission and credits seller wallet
```

---

## Yêu cầu

- Java 21
- Docker
- Docker Compose

---

## Chạy local

### 1. Clone project

```bash
git clone https://github.com/minhdao-dev/vanthucac.git
cd vanthucac
```

### 2. Cấu hình môi trường

```bash
cp .env.example .env
```

Mở file `.env` và cấu hình các biến cần thiết:

```env
JWT_SECRET=
DB_USERNAME=
DB_PASSWORD=
MAIL_USERNAME=
MAIL_PASSWORD=
GOOGLE_BOOKS_API_KEY=
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_REGION=
AWS_S3_BUCKET=
WEBSOCKET_ALLOWED_ORIGINS=
```

Tạo `JWT_SECRET`:

```bash
openssl rand -base64 32
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

### 5. Truy cập Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

---

## Tài khoản Admin mặc định

```text
Email:    admin@vanthucac.com
Password: Admin@123
```

Tài khoản admin mặc định chỉ phục vụ môi trường development/demo. Khi triển khai môi trường thật, cần đổi mật khẩu hoặc thay thế cơ chế seed admin.

---

## Chạy tests

```bash
chmod +x mvnw
./mvnw test
```

Tests hiện dùng H2 in-memory, không yêu cầu MySQL hoặc Redis đang chạy.

---

## CI

Project dùng GitHub Actions để tự động build và chạy test khi push hoặc mở pull request vào các nhánh:

```text
main
develop
```

Workflow chính:

```text
.github/workflows/ci.yml
```

---

## Environment Variables

| Variable                    |   Bắt buộc | Mô tả                                                     |
|-----------------------------|-----------:|-----------------------------------------------------------|
| `JWT_SECRET`                |         Có | Secret dùng để ký JWT, tối thiểu 32 ký tự ngẫu nhiên      |
| `DB_USERNAME`               |         Có | MySQL application user                                    |
| `DB_PASSWORD`               |         Có | MySQL application password                                |
| `MAIL_USERNAME`             |         Có | Email username                                            |
| `MAIL_PASSWORD`             |         Có | Email app password                                        |
| `GOOGLE_BOOKS_API_KEY`      |      Không | Google Books API key, thiếu thì seller nhập sách thủ công |
| `AWS_ACCESS_KEY`            |         Có | AWS access key                                            |
| `AWS_SECRET_KEY`            |         Có | AWS secret key                                            |
| `AWS_REGION`                |         Có | AWS region                                                |
| `AWS_S3_BUCKET`             |         Có | S3 bucket name                                            |
| `WEBSOCKET_ALLOWED_ORIGINS` | Production | Domain frontend được phép kết nối WebSocket               |

---

## WebSocket — Đấu giá real-time

```text
Endpoint:  ws://localhost:8080/ws
Subscribe: /topic/auction/{itemId}
```

Khi bid được chấp nhận, server broadcast `BidBroadcastMessage` tới toàn bộ client đang subscribe vào auction item tương ứng.

---

## API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

---

## Branching Strategy

Project dùng flow:

```text
main        stable/demo-ready branch
develop     integration branch
feature/*   feature development
fix/*       bug fixes
chore/*     tooling, docs, CI, config
refactor/*  internal refactoring
test/*      test coverage improvements
```

Không commit trực tiếp vào `main`. Các thay đổi nên đi qua pull request từ nhánh feature/chore/fix vào `develop`, sau đó merge `develop` vào `main` khi ổn định.

---

## Known Limitations

- Payment hiện là mock. `Payment.createMock()` tự set trạng thái `COMPLETED`, chưa tích hợp VNPay, MoMo hoặc provider thật.
- Rate limiter hiện là in-memory, chưa phù hợp khi scale nhiều instance. Production cần Redis-based rate limiter.
- Admin mặc định trong migration chỉ nên dùng cho development/demo.
- Email async hiện chưa có persistent queue/outbox, email pending có thể mất nếu server restart giữa chừng.
- Tests hiện chưa bao phủ đầy đủ các luồng critical như concurrent bidding, payment callback, escrow idempotency và order state transition.

---

## Production Improvement Roadmap

### Phase 1 — Repository polish and CI

- Sửa README, clone URL, badges và architecture overview.
- Thêm GitHub Actions CI.
- Đảm bảo `./mvnw test` pass trên GitHub Actions.
- Thêm branch strategy rõ ràng.

### Phase 2 — Critical test coverage

- Test refresh token rotation và reuse detection.
- Test checkout flow với một seller và nhiều seller.
- Test listing out of stock.
- Test cancel order hoàn stock.
- Test escrow release idempotency.
- Test auction bid validation và concurrent bidding.
- Test authorization cho admin/seller/user.

### Phase 3 — Payment provider abstraction

- Tách `PaymentProvider` interface.
- Tạo `MockPaymentProvider`.
- Đổi checkout flow sang `PENDING_PAYMENT`.
- Thêm payment callback/webhook mock endpoint.
- Chuẩn bị tích hợp VNPay hoặc MoMo.

### Phase 4 — Order state machine

- Chuẩn hóa order status transition.
- Không set order status trực tiếp trong service.
- Thêm test cho từng transition hợp lệ và không hợp lệ.

### Phase 5 — Wallet ledger

- Thêm bảng `wallet_transactions`.
- Lưu `balance_before`, `balance_after`, `reference_type`, `reference_id`.
- Mọi thay đổi số dư ví phải có transaction log.
- Escrow release phải idempotent.

### Phase 6 — Redis-based rate limiter

- Chuyển rate limiter từ in-memory sang Redis.
- Áp dụng cho login, register, refresh token, bid và upload presigned URL.

### Phase 7 — Outbox pattern

- Thêm bảng `outbox_events`.
- Lưu event cùng transaction nghiệp vụ.
- Worker xử lý email/notification có retry.
- Không mất event khi server restart.

### Phase 8 — Observability

- Thêm Spring Boot Actuator.
- Thêm health check, metrics và Prometheus endpoint.
- Thêm request id/correlation id vào log.
- Ghi log cho các flow quan trọng như checkout, payment, escrow, auction.

### Phase 9 — Production deployment

- Thêm `docker-compose.prod.yml`.
- Thêm Nginx reverse proxy.
- Thêm healthcheck cho app.
- Chuẩn hóa production environment variables.
- Viết tài liệu deploy VPS.

---

## License

This project is currently maintained as a personal portfolio project.