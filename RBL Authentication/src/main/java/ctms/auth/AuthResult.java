package ctms.auth;

import java.util.Objects;

public class AuthResult {
    private final AuthResultCode code;
    private final String token;
    private final Role role;
    private final String message;

    public AuthResult(AuthResultCode code, String token, Role role, String message) {
        this.code = code;
        this.token = token;
        this.role = role;
        this.message = message;
    }

    public static AuthResult success(String token, Role role) {
        return new AuthResult(AuthResultCode.SUCCESS, token, role, "Login successful");
    }

    public static AuthResult failed(AuthResultCode code, String message) {
        return new AuthResult(code, null, null, message);
    }

    public AuthResultCode getCode() {
        return code;
    }

    public String getToken() {
        return token;
    }

    public Role getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthResult that = (AuthResult) o;
        return code == that.code && Objects.equals(token, that.token) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, token, role);
    }
}
