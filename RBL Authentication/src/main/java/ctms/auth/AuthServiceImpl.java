package ctms.auth;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResult login(String username, String password) {
        // BR-AUTH-01: Kiểm tra null hoặc chuỗi khoảng trắng
        if (isBlank(username) || isBlank(password)) {
            return AuthResult.failed(AuthResultCode.INVALID_INPUT, "Username and password must not be blank");
        }

        // BR-AUTH-02: Chuẩn hóa username không phân biệt hoa thường
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);

        Optional<User> userOpt = userRepository.findByUsername(normalizedUsername);

        // BR-AUTH-03: Tài khoản không tồn tại (Chống User Enumeration Attack)
        if (userOpt.isEmpty()) {
            return AuthResult.failed(AuthResultCode.INVALID_CREDENTIALS, "Invalid username or password");
        }

        User user = userOpt.get();

        // BR-AUTH-04: Tài khoản bị vô hiệu hóa
        if (user.getStatus() == UserStatus.DISABLED) {
            return AuthResult.failed(AuthResultCode.ACCOUNT_DISABLED, "Account is disabled. Please contact administrator");
        }

        // BR-AUTH-05: Tài khoản đang bị khóa (Không tăng bộ đếm)
        if (user.isLocked()) {
            return AuthResult.failed(AuthResultCode.ACCOUNT_LOCKED, "Account is locked due to multiple failed login attempts");
        }

        // Kiểm tra mật khẩu (Phân biệt hoa thường)
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.incrementFailedAttempts();

            // BR-AUTH-06: Khóa tài khoản sau 5 lần sai liên tiếp
            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lock();
                userRepository.save(user);
                return AuthResult.failed(AuthResultCode.ACCOUNT_LOCKED, "Account has been locked after 5 failed attempts");
            }

            userRepository.save(user);
            return AuthResult.failed(AuthResultCode.INVALID_CREDENTIALS, "Invalid username or password");
        }

        // BR-AUTH-07: Đăng nhập thành công -> Reset số lần sai và cấp Token
        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return AuthResult.success(token, user.getRole());
    }

    @Override
    public AuthResult logout(String token) {
        // BR-AUTH-09: Token null hoặc rỗng
        if (isBlank(token)) {
            return AuthResult.failed(AuthResultCode.INVALID_TOKEN, "Token must not be blank");
        }

        // BR-AUTH-08: Thu hồi token hợp lệ
        if (!jwtTokenProvider.validateToken(token)) {
            return AuthResult.failed(AuthResultCode.INVALID_TOKEN, "Invalid or expired token");
        }

        jwtTokenProvider.invalidateToken(token);
        return new AuthResult(AuthResultCode.LOGOUT_SUCCESS, null, null, "Logout successful");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
