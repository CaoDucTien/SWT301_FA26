package ctms.auth;

public interface PasswordEncoder {
    boolean matches(String rawPassword, String encodedPassword);
    String encode(String rawPassword);
}
