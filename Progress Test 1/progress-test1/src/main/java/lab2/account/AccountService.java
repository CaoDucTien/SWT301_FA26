package lab2.account;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AccountService {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int PASSWORD_HISTORY_SIZE = 3;
    public static final int MIN_AGE = 18;

    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, String> emailToUsername = new HashMap<>();

    public AccountService() {
    }

    public ResultCode register(String username, String email, String password,
                               String confirmPassword, LocalDate dateOfBirth, String phone) {
        LocalDate today = LocalDate.now();

        // BR-REG-01: Kiá»ƒm tra rá»—ng/null vÃ  ngÃ y sinh tÆ°Æ¡ng lai
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword)
                || dateOfBirth == null || dateOfBirth.isAfter(today)) {
            return ResultCode.INVALID_INPUT;
        }

        // BR-REG-02: Äá»‹nh dáº¡ng username
        if (!AccountValidator.isValidUsername(username)) {
            return ResultCode.INVALID_USERNAME;
        }

        // BR-REG-04: Äá»‹nh dáº¡ng email
        if (!AccountValidator.isValidEmail(email)) {
            return ResultCode.INVALID_EMAIL;
        }

        // BR-REG-06: Máº­t kháº©u yáº¿u
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }

        // BR-REG-07: XÃ¡c nháº­n máº­t kháº©u khÃ´ng khá»›p
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        // BR-REG-08: Äá»™ tuá»•i tá»‘i thiá»ƒu
        if (AccountValidator.calculateAge(dateOfBirth, today) < MIN_AGE) {
            return ResultCode.UNDERAGE;
        }

        // BR-REG-09: Äá»‹nh dáº¡ng sá»‘ Ä‘iá»‡n thoáº¡i (tÃ¹y chá»n)
        if (phone != null && !phone.isEmpty()) {
            if (phone.isBlank() || !AccountValidator.isValidPhone(phone)) {
                return ResultCode.INVALID_PHONE;
            }
        }

        String userKey = key(username);
        String emailKey = key(email);

        // BR-REG-03: TrÃ¹ng láº·p username
        if (accounts.containsKey(userKey)) {
            return ResultCode.DUPLICATE_USERNAME;
        }

        // BR-REG-05: TrÃ¹ng láº·p email
        if (emailToUsername.containsKey(emailKey)) {
            return ResultCode.DUPLICATE_EMAIL;
        }

        // BR-REG-10: ThÃ nh cÃ´ng
        String salt = PasswordHasher.generateSalt();
        String passwordHash = PasswordHasher.hash(salt, password);
        String savedPhone = (phone != null && !phone.isBlank()) ? phone : null;

        Account account = new Account(username, emailKey, dateOfBirth, savedPhone, salt, passwordHash);
        accounts.put(userKey, account);
        emailToUsername.put(emailKey, userKey);

        return ResultCode.SUCCESS;
    }

    public ResultCode login(String username, String password) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode disableAccount(String username) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode unlockAccount(String username) {
        throw new UnsupportedOperationException("TODO");
    }

    public Optional<Account> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(accounts.get(key(username)));
    }

    public boolean isLocked(String username) {
        return findByUsername(username).map(Account::isLocked).orElse(false);
    }

    public ResultCode changePassword(String username, String oldPassword, String newPassword) {
        throw new UnsupportedOperationException("TODO");
    }

    public TokenResult requestPasswordReset(String email) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode resetPassword(String token, String newPassword) {
        throw new UnsupportedOperationException("TODO");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String key(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }
}