package ctms.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CTMS - Authentication Unit Tests (JUnit 5 & Mockito)")
class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    private static final String DEFAULT_USER = "operator_01";
    private static final String DEFAULT_PASS = "Secret@123";
    private static final String DEFAULT_HASH = "hashed_secret_123";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtTokenProvider);
    }

    // ==========================================
    // 1. INPUT VALIDATION TESTS (BR-AUTH-01)
    // ==========================================
    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @ParameterizedTest(name = "[{index}] username không hợp lệ: \"{0}\" -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "  \t\n "})
        void login_NullOrBlankUsername_ReturnsInvalidInput(String username) {
            AuthResult result = authService.login(username, DEFAULT_PASS);

            assertEquals(AuthResultCode.INVALID_INPUT, result.getCode());
            verifyNoInteractions(userRepository); // Không tốn chi phí gọi Database
        }

        @ParameterizedTest(name = "[{index}] password không hợp lệ: \"{0}\" -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void login_NullOrBlankPassword_ReturnsInvalidInput(String password) {
            AuthResult result = authService.login(DEFAULT_USER, password);

            assertEquals(AuthResultCode.INVALID_INPUT, result.getCode());
            verifyNoInteractions(userRepository);
        }
    }

    // ==========================================
    // 2. DECISION TABLE & BRUTE-FORCE TESTS
    // ==========================================
    @Nested
    @DisplayName("Login Decision Table & Brute-force Tests")
    class LoginDecisionTableTests {

        @Test
        @DisplayName("R1: Tài khoản không tồn tại trong hệ thống -> INVALID_CREDENTIALS")
        void login_NonExistentUser_ReturnsInvalidCredentials() {
            when(userRepository.findByUsername("ghost_user")).thenReturn(Optional.empty());

            AuthResult result = authService.login("ghost_user", DEFAULT_PASS);

            assertEquals(AuthResultCode.INVALID_CREDENTIALS, result.getCode());
            verify(userRepository).findByUsername("ghost_user");
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("R2: Tài khoản bị vô hiệu hóa (DISABLED) -> Chặn đăng nhập")
        void login_DisabledAccount_ReturnsAccountDisabled() {
            User disabledUser = new User("u1", DEFAULT_USER, "op@ctms.vn", DEFAULT_HASH, Role.TERMINAL_OPERATOR);
            disabledUser.setStatus(UserStatus.DISABLED);
            when(userRepository.findByUsername(DEFAULT_USER)).thenReturn(Optional.of(disabledUser));

            AuthResult result = authService.login(DEFAULT_USER, DEFAULT_PASS);

            assertEquals(AuthResultCode.ACCOUNT_DISABLED, result.getCode());
            verifyNoInteractions(passwordEncoder);
        }

        @ParameterizedTest(name = "[{index}] Tài khoản đang bị khóa, nhập pass đúng={0} -> ACCOUNT_LOCKED, counter không tăng")
        @ValueSource(booleans = {true, false})
        @DisplayName("R3: Tài khoản đang bị khóa (isLocked=true) -> Không tăng bộ đếm")
        void login_AlreadyLockedAccount_ReturnsAccountLockedWithoutIncrementingCounter(boolean passwordCorrect) {
            User lockedUser = new User("u1", DEFAULT_USER, "op@ctms.vn", DEFAULT_HASH, Role.TERMINAL_OPERATOR);
            lockedUser.lock();
            lockedUser.setFailedAttempts(5);
            when(userRepository.findByUsername(DEFAULT_USER)).thenReturn(Optional.of(lockedUser));

            AuthResult result = authService.login(DEFAULT_USER, passwordCorrect ? DEFAULT_PASS : "wrong");

            assertEquals(AuthResultCode.ACCOUNT_LOCKED, result.getCode());
            assertEquals(5, lockedUser.getFailedAttempts());
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder);
        }

        @ParameterizedTest(name = "[{index}] Sai mật khẩu lần thứ {0} -> INVALID_CREDENTIALS, counter={0}")
        @ValueSource(ints = {1, 2, 3, 4})
        @DisplayName("R4: Sai mật khẩu dưới ngưỡng 5 lần -> Tăng failedAttempts và báo lỗi")
        void login_WrongPasswordUnderThreshold_IncrementsFailedAttempts(int attemptNumber) {
            User user = new User("u1", DEFAULT_USER, "op@ctms.vn", DEFAULT_HASH, Role.TERMINAL_OPERATOR);
            user.setFailedAttempts(attemptNumber - 1);
            when(userRepository.findByUsername(DEFAULT_USER)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong_pass", DEFAULT_HASH)).thenReturn(false);

            AuthResult result = authService.login(DEFAULT_USER, "wrong_pass");

            assertEquals(AuthResultCode.INVALID_CREDENTIALS, result.getCode());
            assertEquals(attemptNumber, user.getFailedAttempts());
            assertFalse(user.isLocked());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("R5: Sai mật khẩu đúng lần thứ 5 -> Tự động KHÓA tài khoản (ACCOUNT_LOCKED)")
        void login_WrongPassword5thTime_LocksAccount() {
            User user = new User("u1", DEFAULT_USER, "op@ctms.vn", DEFAULT_HASH, Role.TERMINAL_OPERATOR);
            user.setFailedAttempts(4); // Đã sai 4 lần trước đó
            when(userRepository.findByUsername(DEFAULT_USER)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong_pass", DEFAULT_HASH)).thenReturn(false);

            AuthResult result = authService.login(DEFAULT_USER, "wrong_pass");

            assertEquals(AuthResultCode.ACCOUNT_LOCKED, result.getCode());
            assertEquals(5, user.getFailedAttempts());
            assertTrue(user.isLocked());
            assertEquals(UserStatus.LOCKED, user.getStatus());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("R6: Đăng nhập thành công -> Reset bộ đếm số lần sai về 0 và cấp Token")
        void login_CorrectCredentials_ResetsFailedAttemptsAndReturnsSuccess() {
            User user = new User("u1", DEFAULT_USER, "op@ctms.vn", DEFAULT_HASH, Role.TERMINAL_OPERATOR);
            user.setFailedAttempts(3); // Từng nhập sai 3 lần
            when(userRepository.findByUsername(DEFAULT_USER)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(DEFAULT_PASS, DEFAULT_HASH)).thenReturn(true);
            when(jwtTokenProvider.generateToken(user)).thenReturn("jwt.token.operator");

            AuthResult result = authService.login(DEFAULT_USER, DEFAULT_PASS);

            assertEquals(AuthResultCode.SUCCESS, result.getCode());
            assertEquals("jwt.token.operator", result.getToken());
            assertEquals(Role.TERMINAL_OPERATOR, result.getRole());
            assertEquals(0, user.getFailedAttempts()); // Reset về 0
            assertNotNull(user.getLastLoginAt());
            verify(userRepository).save(user);
        }
    }

    // ==========================================
    // 3. CASE SENSITIVITY TESTS (BR-AUTH-02)
    // ==========================================
    @Nested
    @DisplayName("Case Sensitivity Tests")
    class CaseSensitivityTests {

        @ParameterizedTest(name = "[{index}] username hoa thường: \"{0}\" đều hợp lệ")
        @ValueSource(strings = {"operator_01", "OPERATOR_01", "Operator_01", "  OperAtor_01  "})
        void login_UsernameCaseInsensitive_ReturnsSuccess(String inputUsername) {
            User user = new User("u1", "operator_01", "op@ctms.vn", DEFAULT_HASH, Role.TERMINAL_OPERATOR);
            when(userRepository.findByUsername("operator_01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(DEFAULT_PASS, DEFAULT_HASH)).thenReturn(true);
            when(jwtTokenProvider.generateToken(user)).thenReturn("jwt.token.valid");

            AuthResult result = authService.login(inputUsername, DEFAULT_PASS);

            assertEquals(AuthResultCode.SUCCESS, result.getCode());
            verify(userRepository).findByUsername("operator_01");
        }

        @Test
        @DisplayName("Mật khẩu phân biệt hoa thường -> Sai hoa thường bị coi là sai mật khẩu")
        void login_PasswordCaseSensitive_WrongCaseReturnsError() {
            User user = new User("u1", DEFAULT_USER, "op@ctms.vn", DEFAULT_HASH, Role.TERMINAL_OPERATOR);
            when(userRepository.findByUsername(DEFAULT_USER)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret@123", DEFAULT_HASH)).thenReturn(false); // Viết thường

            AuthResult result = authService.login(DEFAULT_USER, "secret@123");

            assertEquals(AuthResultCode.INVALID_CREDENTIALS, result.getCode());
        }
    }

    // ==========================================
    // 4. ROLE-BASED ACCESS CONTROL (4 ACTORS)
    // ==========================================
    @Nested
    @DisplayName("Role-Based Token Claims for 4 Actors")
    class RoleBasedAuthorizationTests {

        @ParameterizedTest(name = "[{index}] Actor: {0} -> Cấp Token đúng Role")
        @CsvSource({
                "manager_01, MANAGER, token.manager.hash",
                "operator_01, TERMINAL_OPERATOR, token.operator.hash",
                "shipping_line_01, SHIPPING_LINE_STAFF, token.shipping.hash",
                "driver_01, TRUCK_DRIVER, token.driver.hash"
        })
        void login_DifferentActors_ReturnsTokenWithCorrectRole(String username, Role expectedRole, String mockToken) {
            User user = new User("uid", username, username + "@ctms.vn", DEFAULT_HASH, expectedRole);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(DEFAULT_PASS, DEFAULT_HASH)).thenReturn(true);
            when(jwtTokenProvider.generateToken(user)).thenReturn(mockToken);

            AuthResult result = authService.login(username, DEFAULT_PASS);

            assertEquals(AuthResultCode.SUCCESS, result.getCode());
            assertEquals(expectedRole, result.getRole());
            assertEquals(mockToken, result.getToken());
        }
    }

    // ==========================================
    // 5. LOGOUT TESTS (BR-AUTH-08, BR-AUTH-09)
    // ==========================================
    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Đăng xuất thành công với Token hợp lệ")
        void logout_ValidToken_ReturnsSuccess() {
            String token = "valid.jwt.token";
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);

            AuthResult result = authService.logout(token);

            assertEquals(AuthResultCode.LOGOUT_SUCCESS, result.getCode());
            verify(jwtTokenProvider).invalidateToken(token);
        }

        @ParameterizedTest(name = "[{index}] Token rỗng: \"{0}\" -> INVALID_TOKEN")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void logout_NullOrBlankToken_ReturnsInvalidToken(String token) {
            AuthResult result = authService.logout(token);

            assertEquals(AuthResultCode.INVALID_TOKEN, result.getCode());
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }

        @Test
        @DisplayName("Đăng xuất với Token đã hết hạn hoặc không hợp lệ -> INVALID_TOKEN")
        void logout_ExpiredOrInvalidToken_ReturnsInvalidToken() {
            String expiredToken = "expired.jwt.token";
            when(jwtTokenProvider.validateToken(expiredToken)).thenReturn(false);

            AuthResult result = authService.logout(expiredToken);

            assertEquals(AuthResultCode.INVALID_TOKEN, result.getCode());
            verify(jwtTokenProvider, never()).invalidateToken(anyString());
        }
    }
}
