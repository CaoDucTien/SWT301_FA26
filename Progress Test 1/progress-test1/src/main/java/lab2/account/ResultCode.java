package lab2.account;

public enum ResultCode {
    SUCCESS,
    INVALID_INPUT,
    // ÄÄƒng kÃ½
    INVALID_USERNAME,
    DUPLICATE_USERNAME,
    INVALID_EMAIL,
    DUPLICATE_EMAIL,
    WEAK_PASSWORD,
    PASSWORD_MISMATCH,
    UNDERAGE,
    INVALID_PHONE,
    // ÄÄƒng nháº­p
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,
    // Äá»•i / Ä‘áº·t láº¡i máº­t kháº©u
    USER_NOT_FOUND,
    OLD_PASSWORD_INCORRECT,
    SAME_AS_OLD_PASSWORD,
    PASSWORD_REUSED,
    INVALID_TOKEN;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}