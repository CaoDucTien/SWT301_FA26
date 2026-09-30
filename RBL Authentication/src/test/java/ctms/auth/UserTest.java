package ctms.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Entity Tests")
class UserTest {

    @Test
    void user_GettersAndSetters_WorkCorrectly() {
        User user = new User("u1", "test_user", "test@ctms.vn", "hash123", Role.TRUCK_DRIVER);

        assertEquals("u1", user.getUserId());
        assertEquals("test_user", user.getUsername());
        assertEquals("test@ctms.vn", user.getEmail());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals(Role.TRUCK_DRIVER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedAttempts());
        assertFalse(user.isLocked());
        assertNull(user.getLastLoginAt());

        user.setPasswordHash("new_hash");
        assertEquals("new_hash", user.getPasswordHash());

        user.setRole(Role.MANAGER);
        assertEquals(Role.MANAGER, user.getRole());

        user.setStatus(UserStatus.DISABLED);
        assertEquals(UserStatus.DISABLED, user.getStatus());

        user.setFailedAttempts(2);
        assertEquals(2, user.getFailedAttempts());

        user.setLocked(true);
        assertTrue(user.isLocked());

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        assertEquals(now, user.getLastLoginAt());

        user.unlock();
        assertFalse(user.isLocked());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedAttempts());
    }
}