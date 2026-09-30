package ctms.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthResult DTO Tests")
class AuthResultTest {

    @Test
    void authResult_SuccessAndFailed_WorkCorrectly() {
        AuthResult success = AuthResult.success("jwt.token", Role.MANAGER);
        assertEquals(AuthResultCode.SUCCESS, success.getCode());
        assertEquals("jwt.token", success.getToken());
        assertEquals(Role.MANAGER, success.getRole());
        assertEquals("Login successful", success.getMessage());

        AuthResult failed = AuthResult.failed(AuthResultCode.INVALID_INPUT, "Error");
        assertEquals(AuthResultCode.INVALID_INPUT, failed.getCode());
        assertNull(failed.getToken());
        assertNull(failed.getRole());
        assertEquals("Error", failed.getMessage());

        AuthResult success2 = AuthResult.success("jwt.token", Role.MANAGER);
        assertEquals(success, success2);
        assertEquals(success.hashCode(), success2.hashCode());
        assertNotEquals(success, failed);
        assertNotEquals(success, null);
        assertNotEquals(success, "other");
    }
}