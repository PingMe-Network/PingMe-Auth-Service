# PingMe Auth Service

Backend chuyên trách xác thực và quản lý tài khoản cho hệ sinh thái PingMe, phụ trách đăng ký, đăng nhập, refresh token,
OTP verification, current user session, current user profile và một số luồng quản trị người dùng.

## Overview

`PingMe-Auth-Service` cung cấp các nghiệp vụ chính cho nền tảng PingMe, bao gồm:

- Đăng ký, đăng nhập và refresh session bằng JWT
- Quản lý refresh token và device session với Redis
- OTP verification cho quên mật khẩu, kích hoạt tài khoản và admin verification
- Quản lý hồ sơ người dùng hiện tại, đổi mật khẩu và cập nhật avatar
- Admin login và quản lý người dùng cơ bản

## Kiến trúc kỹ thuật

| Thành phần                               | Vai trò                                         |
|------------------------------------------|-------------------------------------------------|
| Spring Boot 4                            | Nền tảng backend chính                          |
| Spring Security + OAuth2 Resource Server | Bảo vệ API, xử lý JWT                           |
| MariaDB + Spring Data JPA                | Dữ liệu người dùng, role, permission            |
| Redis                                    | Lưu refresh token, session metadata, OTP, cache |
| OpenFeign                                | Gọi Mail Service và Cloudflare Turnstile        |
| AWS S3                                   | Lưu avatar người dùng                           |
| Spring Validation                        | Validate request payload                        |
| Spring Actuator                          | Health check và thông tin vận hành cơ bản       |

## Công nghệ sử dụng

- Java 21
- Spring Boot `4.0.3`
- Spring Cloud OpenFeign `2025.1.0`
- MariaDB
- Redis
- AWS SDK S3
- Maven
- Jib

## Yêu cầu môi trường

- JDK `21`
- Maven `3.9+`
- MariaDB
- Redis
- Mail Service đang chạy nếu cần flow OTP thực tế
- Tài khoản AWS hoặc credentials hợp lệ nếu cần upload avatar lên S3

## Cấu hình môi trường

Ứng dụng đọc cấu hình từ `src/main/resources/application.properties` và biến môi trường.

Biến môi trường tối thiểu:

```env
SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/pingme
SPRING_DATASOURCE_USERNAME=pingme
SPRING_DATASOURCE_PASSWORD=secret

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_SECRET=change-me
CORS_ALLOWED_ORIGINS=http://localhost:3000

AWS_ACCESS_KEY=...
AWS_SECERT_KEY=...
AWS_REGION=ap-southeast-1
AWS_S3_BUCKET_NAME=...
AWS_S3_DOMAIN=...

MAIL_SERVICE_URL=http://localhost:8082
MAIL_DEFAULT_OTP=000000

CLOUDFLARE_SECRET_KEY=...
APP_INTERNAL_SECRET=...
```

## Chạy local nhanh

### 1. Build source

```bash
./mvnw -DskipTests package
```

### 2. Chạy với profile `dev`

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Hoặc chạy file jar

```bash
java -jar target/pingme-auth-service-1.0.0.jar
```

Mặc định service chạy tại:

- App: `http://localhost:8080`
- Health check: `GET /actuator/health`

## Docker image với Jib

```bash
./mvnw -DskipTests clean compile jib:build \
  -Djib.to.auth.username=YOUR_DOCKER_USERNAME \
  -Djib.to.auth.password=YOUR_DOCKER_PASSWORD
```

Image mặc định được đẩy theo cấu hình Maven:

```text
{YOUR_DOCKER_USERNAME}/pingme-auth-service
```

## Gợi ý quy trình local

1. Khởi động MariaDB và Redis.
2. Cấu hình đầy đủ biến môi trường bắt buộc.
3. Chạy Mail Service nếu cần test flow OTP thật.
4. Chạy service với profile `dev`.
5. Kiểm tra `GET /actuator/health`.
6. Test các flow `/auth`, `/otp`, `/users/me` và `/users`.

## Cấu trúc nghiệp vụ nổi bật trong mã nguồn

```text
src/main/java/org/ping_me
├── controller      # API endpoints cho auth, otp, profile, session, admin user
├── service         # Business logic cho authentication, mail, user, authorization
├── repository      # JPA repositories
├── config          # Security, auth, Redis, S3, Swagger, Feign
├── client          # Feign clients gọi service ngoài
├── dto             # Request/response models
├── model           # Entity, constant, common models
└── utils           # Mapper, otp, crypto helper
```

## Observability và vận hành

- Port mặc định: `8080`
- Actuator exposure: `health`, `info`
- Virtual threads được bật
- Redis được dùng cho session và OTP
- Swagger/OpenAPI dependency có trong project nhưng đang tắt ở cấu hình mặc định
- CORS, cookie security và JWT expiration được cấu hình qua properties

## Repository liên quan

- Core Service: https://github.com/PingMe-Network/PingMe-Core-Service
- Auth Service: https://github.com/PingMe-Network/PingMe-Auth-Service
- Music Service: https://github.com/PingMe-Network/PingMe-Music-Service
- Utility Service: https://github.com/PingMe-Network/PingMe-Utility-Service

## Tác giả

- Huỳnh Đức Phú
- Lê Trần Gia Huy
- Đặng Nguyễn Tiến Phát
- Phạm Ngọc Hùng
