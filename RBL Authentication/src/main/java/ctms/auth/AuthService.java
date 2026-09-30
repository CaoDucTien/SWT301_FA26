package ctms.auth;

public interface AuthService {
    int MAX_FAILED_ATTEMPTS = 5;

    AuthResult login(String username, String password);
    AuthResult logout(String token);
}
