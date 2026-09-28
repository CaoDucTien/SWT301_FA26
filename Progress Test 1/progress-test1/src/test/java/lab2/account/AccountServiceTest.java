package lab2.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountService")
class AccountServiceTest {

    private AccountService service;

    private static final String VALID_USER = "alice_01";
    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_PASS = "Secret@123";
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
            assertEquals("alice@example.com", acc.getEmail()); // lowercase email
            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            assertNotNull(acc.getSalt());
            assertNotEquals(VALID_PASS, acc.getCurrentPasswordHash());
            assertTrue(PasswordHasher.matches(acc.getSalt(), VALID_PASS, acc.getCurrentPasswordHash()));
        }

        @Test
        @DisplayName("ÄÄƒng kÃ½ thÃ nh cÃ´ng vá»›i phone null hoáº·c empty (phone tÃ¹y chá»n)")
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
                "18, 0, SUCCESS",       // Ä‘Ãºng 18 tuá»•i
                "18, 1, UNDERAGE",      // 18 tuá»•i thiáº¿u 1 ngÃ y (17 tuá»•i)
                "0, 1, INVALID_INPUT"   // ngÃ y sinh á»Ÿ tÆ°Æ¡ng lai
        })
        void register_AgeBoundary_RelativeDates(int yearsAgo, int plusDays, ResultCode expected) {
            LocalDate dob = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);
            String user = "user_" + yearsAgo + "_" + plusDays;
            String email = user + "@example.com";
            ResultCode result = service.register(user, email, VALID_PASS, VALID_PASS, dob, null);
            assertEquals(expected, result);
        }

        @ParameterizedTest(name = "[{index}] trÃ¹ng username khÃ´ng phÃ¢n biá»‡t hoa thÆ°á»ng: {0}")
        @ValueSource(strings = {"alice_01", "ALICE_01", "Alice_01"})
        void register_DuplicateUsername_CaseInsensitive(String duplicateUser) {
            ResultCode res1 = service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.SUCCESS, res1);

            ResultCode res2 = service.register(duplicateUser, "other@example.com", VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.DUPLICATE_USERNAME, res2);
        }

        @ParameterizedTest(name = "[{index}] trÃ¹ng email khÃ´ng phÃ¢n biá»‡t hoa thÆ°á»ng: {0}")
        @ValueSource(strings = {"alice@example.com", "ALICE@EXAMPLE.COM", "Alice@Example.Com"})
        void register_DuplicateEmail_CaseInsensitive(String duplicateEmail) {
            ResultCode res1 = service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.SUCCESS, res1);

            ResultCode res2 = service.register("other_user", duplicateEmail, VALID_PASS, VALID_PASS, VALID_DOB, null);
            assertEquals(ResultCode.DUPLICATE_EMAIL, res2);
        }
    }

    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                // ÄÆ¡n láº» tá»«ng quy táº¯c
                Arguments.of("Username khÃ´ng há»£p lá»‡", "1alice", VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("Email khÃ´ng há»£p lá»‡", VALID_USER, "bad_email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("Máº­t kháº©u yáº¿u", VALID_USER, VALID_EMAIL, "weakpass", "weakpass", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD),
                Arguments.of("XÃ¡c nháº­n máº­t kháº©u lá»‡ch", VALID_USER, VALID_EMAIL, VALID_PASS, "Mismatch@123", VALID_DOB, VALID_PHONE, ResultCode.PASSWORD_MISMATCH),
                Arguments.of("Sá»‘ Ä‘iá»‡n thoáº¡i khÃ´ng há»£p lá»‡", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, "0123456789", ResultCode.INVALID_PHONE),
                Arguments.of("Sá»‘ Ä‘iá»‡n thoáº¡i lÃ  chuá»—i khoáº£ng tráº¯ng", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, "   ", ResultCode.INVALID_PHONE),

                // >= 3 test chá»©ng minh THá»¨ Tá»° Æ¯U TIÃŠN KIá»‚M TRA (Priority Order)
                Arguments.of("Æ¯u tiÃªn: Username sai + Email sai -> INVALID_USERNAME", "1alice", "bad_email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("Æ¯u tiÃªn: Email sai + Password yáº¿u -> INVALID_EMAIL", VALID_USER, "bad_email", "weakpass", "weakpass", VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("Æ¯u tiÃªn: Password yáº¿u + Confirm lá»‡ch -> WEAK_PASSWORD", VALID_USER, VALID_EMAIL, "weakpass", "otherpass", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD)
        );
    }
}