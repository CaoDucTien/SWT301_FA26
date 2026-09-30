package ctms.auth;

import java.time.LocalDateTime;

public class User {
    private final String userId;
    private final String username;
    private final String email;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private int failedAttempts;
    private boolean locked;
    private LocalDateTime lastLoginAt;

    public User(String userId, String username, String email, String passwordHash, Role role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.failedAttempts = 0;
        this.locked = false;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public void lock() {
        this.locked = true;
        this.status = UserStatus.LOCKED;
    }

    public void unlock() {
        this.locked = false;
        this.status = UserStatus.ACTIVE;
        this.failedAttempts = 0;
    }
}
