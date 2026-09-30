package ctms.auth;

public interface JwtTokenProvider {
    String generateToken(User user);
    boolean validateToken(String token);
    void invalidateToken(String token);
}
