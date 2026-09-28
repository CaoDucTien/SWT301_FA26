package lab2.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountService")
class AccountServiceTest {

    private AccountService service;

    private static final String VALID_USER = "alice_01";
    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_PASS = "Secret@123";
    private static final String WRONG_PASS = "Wrong@999";
    private static final LocalDate VALID_DOB = LocalDate.now().minusYears(20);
    private static final String VALID_PHONE = "0912345678";

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    // ---------- REGISTER TESTS (TODO-5) ----------
    @Nested
    @DisplayName("Register")
    class Register {

        @Test
        @DisplayName("ÄÄƒng kÃ½ thÃ nh cÃ´ng: assert SUCCESS vÃ  tráº¡ng thÃ¡i tÃ i khoáº£n")
        void register_Success_CreatesActiveAccountWithCorrectState() {
            ResultCode result = service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);

            assertEquals(ResultCode.SUCCESS, result);
            Optional<Account> opt = service.findByUsername(VALID_USER);
            assertTrue(opt.isPresent());

            Account acc = opt.get();
            assertEquals(VALID_USER, acc.getUsername());
            assertEquals("alice@example.com", acc.getEmail());
            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            assertNotNull(acc.getSalt());
            assertNotEquals(VALID_PASS, acc.getCurrentPasswordHash());
            assertTrue(PasswordHasher.matches(acc.getSalt(), VALID_PASS, acc.getCurrentPasswordHash()));
        }

        @Test
        @DisplayName("ÄÄƒng kÃ½ thÃ nh cÃ´ng vá»›i phone null hoáº·c empty")
        void register_Success_OptionalPhone() {
            ResultCode res1 = service.register("bob_01", "bob@example.com", VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.SUCCESS, res1);

            ResultCode res2 = service.register("charlie_01", "charlie@example.com", VALID_PASS, VALID_PASS, VALID_DOB, "");
            assertEquals(ResultCode.SUCCESS, res2);
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("lab2.account.AccountServiceTest#invalidRegisterInputs")
        void register_InvalidInputs_ReturnsExpectedCode(String desc, String u, String e, String p, String c,
                                                        LocalDate dob, String phone, ResultCode expected) {
            ResultCode result = service.register(u, e, p, c, dob, phone);
            assertEquals(expected, result);
            if (u != null) {
                assertTrue(service.findByUsername(u).isEmpty());
            }
        }

        @ParameterizedTest(name = "[{index}] username null/empty/blank: \"{0}\" -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_NullEmptyBlankUsername_ReturnsInvalidInput(String username) {
            ResultCode res = service.register(username, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.INVALID_INPUT, res);
        }

        @ParameterizedTest(name = "[{index}] email null/empty/blank: \"{0}\" -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_NullEmptyBlankEmail_ReturnsInvalidInput(String email) {
            ResultCode res = service.register(VALID_USER, email, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.INVALID_INPUT, res);
        }

        @ParameterizedTest(name = "[{index}] password null/empty/blank: \"{0}\" -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_NullEmptyBlankPassword_ReturnsInvalidInput(String pass) {
            ResultCode res = service.register(VALID_USER, VALID_EMAIL, pass, pass, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.INVALID_INPUT, res);
        }

        @ParameterizedTest(name = "[{index}] ngÃ y sinh: hÃ´m nay - {0} nÄƒm + {1} ngÃ y -> {2}")
        @CsvSource({
                "18, 0, SUCCESS",
                "18, 1, UNDERAGE",
                "0, 1, INVALID_INPUT"
        })
        void register_AgeBoundary_RelativeDates(int yearsAgo, int plusDays, ResultCode expected) {
            LocalDate dob = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);
            String user = "user_" + yearsAgo + "_" + plusDays;
            String email = user + "@example.com";
            ResultCode result = service.register(user, email, VALID_PASS, VALID_PASS, dob, null);
            assertEquals(expected, result);
        }

        @ParameterizedTest(name = "[{index}] trÃ¹ng username: {0}")
        @ValueSource(strings = {"alice_01", "ALICE_01", "Alice_01"})
        void register_DuplicateUsername_CaseInsensitive(String duplicateUser) {
            ResultCode res1 = service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.SUCCESS, res1);

            ResultCode res2 = service.register(duplicateUser, "other@example.com", VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.DUPLICATE_USERNAME, res2);
        }

        @ParameterizedTest(name = "[{index}] trÃ¹ng email: {0}")
        @ValueSource(strings = {"alice@example.com", "ALICE@EXAMPLE.COM", "Alice@Example.Com"})
        void register_DuplicateEmail_CaseInsensitive(String duplicateEmail) {
            ResultCode res1 = service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.SUCCESS, res1);

            ResultCode res2 = service.register("other_user", duplicateEmail, VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.DUPLICATE_EMAIL, res2);
        }
    }

    // ---------- LOGIN TESTS (TODO-7) ----------
    @Nested
    @DisplayName("Login")
    class Login {

        @BeforeEach
        void registerDefaultUser() {
            service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
        }

        private void failLogin(int times) {
            for (int i = 0; i < times; i++) {
                service.login(VALID_USER, WRONG_PASS);
            }
        }

        @Test
        @DisplayName("Rule 6: ÄÄƒng nháº­p thÃ nh cÃ´ng, reset failedAttempts vá» 0")
        void login_CorrectCredentials_ReturnsSuccessAndResetsCounter() {
            failLogin(2);
            assertEquals(2, service.findByUsername(VALID_USER).get().getFailedAttempts());

            ResultCode result = service.login(VALID_USER, VALID_PASS);
            assertEquals(ResultCode.SUCCESS, result);
            assertEquals(0, service.findByUsername(VALID_USER).get().getFailedAttempts());
            assertFalse(service.isLocked(VALID_USER));
        }

        @Test
        @DisplayName("Rule 1: User khÃ´ng tá»“n táº¡i -> INVALID_CREDENTIALS")
        void login_NonExistentUser_ReturnsInvalidCredentials() {
            ResultCode result = service.login("unknown_user", VALID_PASS);
            assertEquals(ResultCode.INVALID_CREDENTIALS, result);
        }

        @ParameterizedTest(name = "[{index}] Disabled user vá»›i máº­t kháº©u: {0}")
        @ValueSource(strings = {VALID_PASS, WRONG_PASS})
        @DisplayName("Rule 2: TÃ i khoáº£n DISABLED nháº­p pass Ä‘Ãºng hay sai Ä‘á»u -> ACCOUNT_DISABLED")
        void login_DisabledAccount_ReturnsAccountDisabled(String password) {
            service.disableAccount(VALID_USER);
            ResultCode result = service.login(VALID_USER, password);
            assertEquals(ResultCode.ACCOUNT_DISABLED, result);
        }

        @ParameterizedTest(name = "[{index}] Sai láº§n {0} -> INVALID_CREDENTIALS, chÆ°a bá»‹ khÃ³a")
        @ValueSource(ints = {1, 2, 3, 4})
        @DisplayName("Rule 4: ÄÄƒng nháº­p sai tá»« 1 Ä‘áº¿n 4 láº§n -> INVALID_CREDENTIALS, bá»™ Ä‘áº¿m tÄƒng")
        void login_WrongPasswordUnderThreshold_IncrementsCounter(int attempts) {
            failLogin(attempts - 1);
            ResultCode result = service.login(VALID_USER, WRONG_PASS);

            assertEquals(ResultCode.INVALID_CREDENTIALS, result);
            Account acc = service.findByUsername(VALID_USER).get();
            assertEquals(attempts, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            assertFalse(service.isLocked(VALID_USER));
        }

        @Test
        @DisplayName("Rule 5: Sai láº§n thá»© 5 -> KhÃ³a tÃ i khoáº£n (ACCOUNT_LOCKED)")
        void login_WrongPassword5thTime_LocksAccount() {
            failLogin(4);
            ResultCode result = service.login(VALID_USER, WRONG_PASS);

            assertEquals(ResultCode.ACCOUNT_LOCKED, result);
            Account acc = service.findByUsername(VALID_USER).get();
            assertEquals(5, acc.getFailedAttempts());
            assertTrue(acc.isLocked());
            assertTrue(service.isLocked(VALID_USER));
        }

        @ParameterizedTest(name = "[{index}] Äang khÃ³a, nháº­p pass: {0} -> ACCOUNT_LOCKED, bá»™ Ä‘áº¿m khÃ´ng Ä‘á»•i")
        @ValueSource(strings = {VALID_PASS, WRONG_PASS})
        @DisplayName("Rule 3: Äang khÃ³a nháº­p pass Ä‘Ãºng hay sai Ä‘á»u -> ACCOUNT_LOCKED, bá»™ Ä‘áº¿m khÃ´ng tÄƒng")
        void login_LockedAccount_DoesNotIncrementCounter(String password) {
            failLogin(5);
            assertTrue(service.isLocked(VALID_USER));

            ResultCode result = service.login(VALID_USER, password);
            assertEquals(ResultCode.ACCOUNT_LOCKED, result);
            assertEquals(5, service.findByUsername(VALID_USER).get().getFailedAttempts());
        }

        @ParameterizedTest(name = "[{index}] {0} láº§n sai, sau Ä‘Ã³ nháº­p Ä‘Ãºng -> {1}, locked={2}")
        @CsvSource({
                "4, SUCCESS, false",
                "5, ACCOUNT_LOCKED, true",
                "6, ACCOUNT_LOCKED, true"
        })
        @DisplayName("BiÃªn sá»‘ láº§n sai trÆ°á»›c khi nháº­p Ä‘Ãºng máº­t kháº©u")
        void login_CorrectPasswordAfterFailures(int failures, ResultCode expected, boolean locked) {
            failLogin(failures);
            ResultCode result = service.login(VALID_USER, VALID_PASS);

            assertEquals(expected, result);
            assertEquals(locked, service.isLocked(VALID_USER));
        }

        @ParameterizedTest(name = "[{index}] username hoa thÆ°á»ng: {0} -> SUCCESS")
        @ValueSource(strings = {"alice_01", "ALICE_01", "Alice_01"})
        void login_UsernameCaseInsensitive(String inputUser) {
            ResultCode result = service.login(inputUser, VALID_PASS);
            assertEquals(ResultCode.SUCCESS, result);
        }

        @Test
        @DisplayName("Máº­t kháº©u phÃ¢n biá»‡t hoa thÆ°á»ng")
        void login_PasswordCaseSensitive() {
            ResultCode result = service.login(VALID_USER, VALID_PASS.toLowerCase(Locale.ROOT));
            assertEquals(ResultCode.INVALID_CREDENTIALS, result);
        }

        @ParameterizedTest(name = "[{index}] input null/empty: \"{0}\" -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void login_NullEmptyBlank_ReturnsInvalidInput(String input) {
            assertEquals(ResultCode.INVALID_INPUT, service.login(input, VALID_PASS));
            assertEquals(ResultCode.INVALID_INPUT, service.login(VALID_USER, input));
        }
    }

    // ---------- ADMIN TESTS ----------
    @Nested
    @DisplayName("Admin")
    class Admin {

        @BeforeEach
        void registerDefaultUser() {
            service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
        }

        @Test
        @DisplayName("Admin má»Ÿ khÃ³a tÃ i khoáº£n: reset failedAttempts vá» 0 vÃ  Ä‘Äƒng nháº­p láº¡i Ä‘Æ°á»£c")
        void unlockAccount_Success_ResetsCounterAndAllowsLogin() {
            for (int i = 0; i < 5; i++) {
                service.login(VALID_USER, WRONG_PASS);
            }
            assertTrue(service.isLocked(VALID_USER));

            ResultCode unlockRes = service.unlockAccount(VALID_USER);
            assertEquals(ResultCode.SUCCESS, unlockRes);
            assertFalse(service.isLocked(VALID_USER));

            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(VALID_USER, WRONG_PASS));
            assertEquals(1, service.findByUsername(VALID_USER).get().getFailedAttempts());

            assertEquals(ResultCode.SUCCESS, service.login(VALID_USER, VALID_PASS));
        }

        @Test
        @DisplayName("Má»Ÿ khÃ³a user khÃ´ng tá»“n táº¡i hoáº·c rá»—ng -> USER_NOT_FOUND")
        void unlockAccount_InvalidUser_ReturnsUserNotFound() {
            assertEquals(ResultCode.USER_NOT_FOUND, service.unlockAccount("ghost"));
            assertEquals(ResultCode.USER_NOT_FOUND, service.unlockAccount(""));
            assertEquals(ResultCode.USER_NOT_FOUND, service.unlockAccount(null));
        }

        @Test
        @DisplayName("VÃ´ hiá»‡u hÃ³a user khÃ´ng tá»“n táº¡i hoáº·c rá»—ng -> USER_NOT_FOUND")
        void disableAccount_InvalidUser_ReturnsUserNotFound() {
            assertEquals(ResultCode.USER_NOT_FOUND, service.disableAccount("ghost"));
            assertEquals(ResultCode.USER_NOT_FOUND, service.disableAccount(""));
            assertEquals(ResultCode.USER_NOT_FOUND, service.disableAccount(null));
        }

        @Test
        @DisplayName("isLocked tráº£ vá» false khi user khÃ´ng tá»“n táº¡i hoáº·c null")
        void isLocked_NonExistentOrNull_ReturnsFalse() {
            assertFalse(service.isLocked("ghost"));
            assertFalse(service.isLocked(null));
        }
    }

    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                Arguments.of("Username khÃ´ng há»£p lá»‡", "1alice", VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("Email khÃ´ng há»£p lá»‡", VALID_USER, "bad_email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("Máº­t kháº©u yáº¿u", VALID_USER, VALID_EMAIL, "weakpass", "weakpass", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD),
                Arguments.of("XÃ¡c nháº­n máº­t kháº©u lá»‡ch", VALID_USER, VALID_EMAIL, VALID_PASS, "Mismatch@123", VALID_DOB, VALID_PHONE, ResultCode.PASSWORD_MISMATCH),
                Arguments.of("Sá»‘ Ä‘iá»‡n thoáº¡i khÃ´ng há»£p lá»‡", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, "0123456789", ResultCode.INVALID_PHONE),
                Arguments.of("Sá»‘ Ä‘iá»‡n thoáº¡i lÃ  chuá»—i khoáº£ng tráº¯ng", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, "   ", ResultCode.INVALID_PHONE),

                Arguments.of("Æ¯u tiÃªn: Username sai + Email sai -> INVALID_USERNAME", "1alice", "bad_email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("Æ¯u tiÃªn: Email sai + Password yáº¿u -> INVALID_EMAIL", VALID_USER, "bad_email", "weakpass", "weakpass", VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("Æ¯u tiÃªn: Password yáº¿u + Confirm lá»‡ch -> WEAK_PASSWORD", VALID_USER, VALID_EMAIL, "weakpass", "otherpass", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD)
        );
    }
}
