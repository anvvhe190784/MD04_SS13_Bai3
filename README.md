# Session 13: Xây dựng hệ thống xác thực Microservices với JWT

Hệ thống bao gồm các dịch vụ:
- `identity-service`: Quản lý người dùng, băm mật khẩu BCrypt, cấp phát và xác thực JWT.
- `gateway-service`: API Gateway (Spring Cloud Gateway WebFlux) định tuyến, chặn bắt và kiểm tra tính hợp lệ của JWT, trích xuất và chuyển tiếp User Context.
- `product-service`: Dịch vụ đích tiếp nhận request và phân quyền dựa trên User Context (`X-User-Role`).

---

## [Bài tập 1] Khởi tạo Identity Service và Bảo mật mật khẩu
- Quản lý entity `User` (id: UUID, username, password, role).
- Băm mật khẩu an toàn với `BCryptPasswordEncoder`.
- Cấu hình Spring Security tắt CSRF và cho phép truy cập công khai endpoint đăng ký.
- API: `POST /api/auth/register` tiếp nhận thông tin người dùng và lưu vào database PostgreSQL `identity_db`.

---

## [Bài tập 2] Xây dựng bộ công cụ cấp phát JSON Web Token (JWT)
- Tích hợp thư viện `io.jsonwebtoken:jjwt-api:0.11.5`.
- Cấu hình secret 256-bit và expiration 3600000ms trong `application.yml`.
- `JwtUtil`: Sinh token với Claims `sub` (username), custom claim `role`, ký HMAC-SHA256.
- API: `GET /api/auth/test-token?username={name}` tìm người dùng trong DB và cấp token.
- Kiểm tra tính hợp lệ của Payload trên [jwt.io](https://jwt.io).

---

## [Bài tập 3] Triển khai Stateless Authentication Flow với Spring Security
- Cấu hình `SessionCreationPolicy.STATELESS` (vô hiệu hóa HttpSession và cookie JSESSIONID).
- Đăng ký `BCryptPasswordEncoder` bean để Spring Security so khớp mật khẩu qua `passwordEncoder.matches()`.
- API: `POST /api/auth/login` tiếp nhận `LoginRequest` (username, password):
  - Thành công: Trả về HTTP 200 kèm JWT access token.
  - Thất bại: Trả về HTTP 401 Unauthorized kèm thông điệp chung `Bad credentials` để bảo mật.
- Kiểm thử toàn diện qua JUnit 5 Mockito cho các kịch bản thành công và thất bại.
