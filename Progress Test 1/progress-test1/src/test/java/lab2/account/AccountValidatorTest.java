package lab2.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountValidatorTest {

    // --- USERNAME TESTS ---

    @ParameterizedTest(name = "[{index}] username há»£p lá»‡: {0}")
    @ValueSource(strings = {"alice", "Alice_01", "Z____", "User123456789012345"})
    void isValidUsername_ValidUsernames_ReturnsTrue(String username) {
        assertTrue(AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] username khÃ´ng há»£p lá»‡: {0}")
    @ValueSource(strings = {"ab_1", "1alice", "_alice", "ali ce", "alice!", "alice-01"})
    void isValidUsername_InvalidUsernames_ReturnsFalse(String username) {
        assertFalse(AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] username null hoáº·c rá»—ng")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void isValidUsername_NullAndEmpty_ReturnsFalse(String username) {
        assertFalse(AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] biÃªn Ä‘á»™ dÃ i username {0} -> {1}")
    @MethodSource("usernameLengths")
    void isValidUsername_BoundaryLengths(int length, boolean expected) {
        String username = "a" + "0".repeat(length - 1);
        assertEquals(expected, AccountValidator.isValidUsername(username));
    }

    static Stream<Arguments> usernameLengths() {
        return Stream.of(
                Arguments.of(4, false),
                Arguments.of(5, true),
                Arguments.of(6, true),
                Arguments.of(19, true),
                Arguments.of(20, true),
                Arguments.of(21, false)
        );
    }

    // --- EMAIL TESTS ---

    @ParameterizedTest(name = "[{index}] email há»£p lá»‡: {0}")
    @ValueSource(strings = {"user@gmail.com", "alice.bob@company.org", "test_user+tag@domain.co.uk"})
    void isValidEmail_ValidEmails_ReturnsTrue(String email) {
        assertTrue(AccountValidator.isValidEmail(email));
    }

    @ParameterizedTest(name = "[{index}] email khÃ´ng há»£p lá»‡: {0}")
    @ValueSource(strings = {"plainaddress", "@missinglocal.com", "user@.com", "user@domain.c", "user@domain..com"})
    void isValidEmail_InvalidEmails_ReturnsFalse(String email) {
        assertFalse(AccountValidator.isValidEmail(email));
    }

    @ParameterizedTest(name = "[{index}] email null hoáº·c rá»—ng")
    @NullAndEmptySource
    void isValidEmail_NullAndEmpty_ReturnsFalse(String email) {
        assertFalse(AccountValidator.isValidEmail(email));
    }

    @ParameterizedTest(name = "[{index}] biÃªn Ä‘á»™ dÃ i email {0} kÃ½ tá»± -> {1}")
    @CsvSource({
            "99, true",
            "100, true",
            "101, false"
    })
    void isValidEmail_BoundaryLengths(int totalLength, boolean expected) {
        String suffix = "@domain.com"; // 11 chars
        String local = "a".repeat(totalLength - suffix.length());
        String email = local + suffix;
        assertEquals(expected, AccountValidator.isValidEmail(email));
    }

    // --- PASSWORD TESTS ---

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvSource(delimiter = '|', value = {
            "Secret@123    | alice_01 | true  | Há»£p lá»‡ Ä‘áº§y Ä‘á»§ 4 nhÃ³m",
            "secret@123    | alice_01 | false | Thiáº¿u chá»¯ hoa",
            "SECRET@123    | alice_01 | false | Thiáº¿u chá»¯ thÆ°á»ng",
            "Secret@@@@    | alice_01 | false | Thiáº¿u chá»¯ sá»‘",
            "Secret1234    | alice_01 | false | Thiáº¿u kÃ½ tá»± Ä‘áº·c biá»‡t",
            "'Secret @123' | alice_01 | false | Chá»©a khoáº£ng tráº¯ng",
            "Secret#123~   | alice_01 | false | Chá»©a kÃ½ tá»± ngoÃ i danh sÃ¡ch",
            "Xalice_01@1   | alice_01 | false | Chá»©a username",
            "Xalice_01@1   |          | true  | Username rá»—ng/null thÃ¬ bá» qua kiá»ƒm tra username",
            "Pass@12       | alice_01 | false | BiÃªn dÆ°á»›i: 7 kÃ½ tá»±",
            "Passwd@1      | alice_01 | true  | BiÃªn min: 8 kÃ½ tá»±",
            "P@ss1234567890123456789012345678 | alice_01 | true  | BiÃªn max: 32 kÃ½ tá»±",
            "P@ss12345678901234567890123456789 | alice_01 | false | VÆ°á»£t biÃªn max: 33 kÃ½ tá»±"
    })
    void isValidPassword_EquivalencePartitions(String password, String username, boolean expected, String desc) {
        assertEquals(expected, AccountValidator.isValidPassword(password, username));
    }

    @ParameterizedTest(name = "[{index}] password null hoáº·c empty")
    @NullAndEmptySource
    void isValidPassword_NullAndEmpty_ReturnsFalse(String password) {
        assertFalse(AccountValidator.isValidPassword(password, "alice"));
    }

    // --- PHONE TESTS ---

    @ParameterizedTest(name = "[{index}] Ä‘áº§u sá»‘ Ä‘iá»‡n thoáº¡i há»£p lá»‡: {0}")
    @ValueSource(strings = {"0321234567", "0561234567", "0771234567", "0881234567", "0901234567"})
    void isValidPhone_ValidPrefixes_ReturnsTrue(String phone) {
        assertTrue(AccountValidator.isValidPhone(phone));
    }

    @ParameterizedTest(name = "[{index}] sá»‘ Ä‘iá»‡n thoáº¡i khÃ´ng há»£p lá»‡: {0}")
    @ValueSource(strings = {"0123456789", "091234567", "09123456789", "090123456a", "1234567890"})
    void isValidPhone_InvalidPhones_ReturnsFalse(String phone) {
        assertFalse(AccountValidator.isValidPhone(phone));
    }

    @ParameterizedTest(name = "[{index}] phone null")
    @NullSource
    void isValidPhone_Null_ReturnsFalse(String phone) {
        assertFalse(AccountValidator.isValidPhone(phone));
    }

    // --- AGE TESTS ---

    @ParameterizedTest(name = "[{index}] sinh ngÃ y {0}, hÃ´m nay {1} -> {2} tuá»•i")
    @CsvSource({
            "2008-09-28, 2026-09-28, 18", // ÄÃºng ngÃ y sinh nháº­t 18
            "2008-09-29, 2026-09-28, 17", // 18 tuá»•i thiáº¿u 1 ngÃ y
            "2008-02-29, 2026-02-28, 17", // NÄƒm nhuáº­n chÆ°a tá»›i ngÃ y
            "2008-02-29, 2026-03-01, 18"  // NÄƒm nhuáº­n qua ngÃ y
    })
    void calculateAge_Boundaries(LocalDate dob, LocalDate today, int expected) {
        assertEquals(expected, AccountValidator.calculateAge(dob, today));
    }
}