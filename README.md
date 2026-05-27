# Vạn Thư Các — Book Marketplace Backend

Spring Boot backend cho hệ thống sàn giao dịch sách: B2C, C2C và đấu giá sách quý hiếm.

> Portfolio project — Rebuild từ đồ án tốt nghiệp JSP/Servlet thành Spring Boot marketplace.

---

## Business Models

| Model   | Mô tả                                               |
|---------|-----------------------------------------------------|
| B2C     | Platform bán sách mới trực tiếp                     |
| C2C     | User đăng ký seller, đăng bán sách cũ — escrow flow |
| Auction | Phiên đấu giá hàng tháng — real-time qua WebSocket  |

---

## Tech Stack

| Layer     | Công nghệ                                            |
|-----------|------------------------------------------------------|
| Language  | Java 21 (Virtual Threads)                            |
| Framework | Spring Boot 3.x                                      |
| Security  | Spring Security + OAuth2 Resource Server (JWT HS256) |
| ORM       | Spring Data JPA + Hibernate                          |
| Database  | MySQL 8.0                                            |
| Cache     | Redis 8                                              |
| Realtime  | Spring WebSocket (STOMP)                             |
| Storage   | AWS S3 Presigned URL                                 |
| Migration | Flyway                                               |
| Docs      | SpringDoc OpenAPI (Swagger)                          |
| Deploy    | Docker + Docker Compose                              |

---

## Yêu cầu

- Java 21
- Docker & Docker Compose

---

## Chạy local

### 1. Clone và cấu hình môi trường

```bash
git clone https://github.com/your-username/vanthucac.git
cd vanthucac
cp .env.example .env
# Mở .env, điền JWT_SECRET, MAIL_*, GOOGLE_BOOKS_API_KEY, AWS_*
```

### 2. Khởi động MySQL và Redis

```bash
docker compose up -d
docker compose ps   # chờ MySQL healthy (~30 giây)
```

### 3. Chạy ứng dụng

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Truy cập Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### 5. Tài khoản Admin mặc định

```
Email:    admin@vanthucac.com
Password: Admin@123
```

> ⚠️ Đổi mật khẩu admin ngay sau khi chạy lần đầu trong môi trường thực tế.

---

## Chạy Tests

```bash
./mvnw test
```

Tests dùng H2 in-memory — không cần MySQL hay Redis đang chạy.

---

## Environment Variables

| Variable                    | Bắt buộc  | Mô tả                                |
|-----------------------------|-----------|--------------------------------------|
| `JWT_SECRET`                | ✅         | Tối thiểu 32 ký tự ngẫu nhiên        |
| `DB_USERNAME`               | ✅         | MySQL user (không phải root)         |
| `DB_PASSWORD`               | ✅         | MySQL password                       |
| `MAIL_USERNAME`             | ✅         | Gmail address                        |
| `MAIL_PASSWORD`             | ✅         | Gmail App Password                   |
| `GOOGLE_BOOKS_API_KEY`      | ⚠️        | Optional — thiếu thì seller nhập tay |
| `AWS_ACCESS_KEY`            | ✅         | AWS credentials cho S3               |
| `AWS_SECRET_KEY`            | ✅         | AWS credentials cho S3               |
| `WEBSOCKET_ALLOWED_ORIGINS` | Prod only | Domain frontend — dev mặc định `*`   |

Generate JWT_SECRET:

```bash
openssl rand -base64 32
```

---

## WebSocket — Đấu giá real-time

```
Endpoint:  ws://localhost:8080/ws
Subscribe: /topic/auction/{itemId}
```

Khi bid được chấp nhận, server broadcast `BidBroadcastMessage` tới toàn bộ client đang subscribe.

---

## Known Limitations

- **Payment là mock** — `Payment.createMock()` tự set `COMPLETED`. Chưa tích hợp VNPay hay MoMo.
- **Rate limiter là in-memory** — Không hoạt động đúng khi scale nhiều instance. Production cần Redis-based (Bucket4j +
  Redis).
- **Admin mặc định** trong `V4__insert_admin_user.sql` chỉ nên chạy ở dev. Cần đổi password trước khi deploy production.
- **Email async** — Email đang pending sẽ mất nếu server restart giữa chừng.