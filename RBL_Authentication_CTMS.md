# BÁO CÁO RESEARCH BY LEARNING (RBL)
## UNIT TESTING CHỨC NĂNG AUTHENTICATION CHO HỆ THỐNG QUẢN LÝ CẢNG CONTAINER (CTMS)

* **Môn học:** SWT301 – Software Testing
* **Dự án nhóm:** Container Terminal Management System (CTMS) - Hệ thống Quản lý Cảng Container
* **Module kiểm thử:** Xác thực và Phân quyền người dùng (Authentication & Role-Based Authorization)
* **Người thực hiện / Sinh viên:** Cao Đức Tiến (MSSV: DE190608)

---

## 1. TỔNG QUAN HỆ THỐNG VÀ MODULE AUTHENTICATION

### 1.1 Bối cảnh hệ thống CTMS
Hệ thống **Container Terminal Management System (CTMS)** quản lý toàn bộ các hoạt động xếp dỡ, lưu kho bãi, cập cảng của tàu và giao nhận container tại cổng cảng. Vì tính chất an ninh cảng biển và vận tải đa phương thức, hệ thống yêu cầu kiểm soát truy cập nghiêm ngặt dựa trên vai trò (**Role-Based Access Control - RBAC**).

### 1.2 Các Actor và ma trận phân quyền chức năng
Chức năng Authentication chịu trách nhiệm xác minh danh tính và gán đúng quyền truy cập cho 4 nhóm đối tượng chính:

| Actor | Quyền truy cập sau khi xác thực thành công |
|---|---|
| **Manager** (Quản lý cảng) | • Authenticate User (Login / Logout)<br>• View Dashboard (thống kê tổng thể)<br>• Manage Shipping Lines (quản lý hãng tàu)<br>• Manage Vessels (quản lý đội tàu)<br>• Assign Vessel to Berth (chỉ định cầu bến) |
| **Terminal Operator** (Điều hành bãi & cổng) | • Authenticate User (Login / Logout)<br>• View Dashboard<br>• Manage Containers & Search/Filter<br>• View Movement History & View Yard<br>• Assign Container to Slot & Move Container<br>• View Gate Appointments<br>• Confirm Gate In & Confirm Gate Out |
| **Shipping Line Staff** (Nhân viên hãng tàu) | • Authenticate User (Login / Logout)<br>• View Shipping Line Information<br>• View Vessel Schedule<br>• Manage Containers & Search/Filter<br>• Book Import Drop-off Appointment<br>• Book Export Pickup Appointment<br>• View Gate Appointments |
| **Truck Driver** (Tài xế xe container) | • Authenticate User (Login / Logout)<br>• View Gate Appointments (xem lịch hẹn vào cổng) |

---

## 2. ĐẶC TẢ BUSINESS LOGIC (QUY TẮC NGHIỆP VỤ) CẦN KIỂM THỬ

Mọi quy tắc nghiệp vụ dưới đây phải được cài đặt trong `AuthService` và được kiểm thử toàn diện bằng Unit Test.

### 2.1 Nhóm quy tắc kiểm tra dữ liệu đầu vào (Input Validation)
* **BR-AUTH-01 (Null/Empty Input):** Username/Email và Password không được null, rỗng hoặc chỉ chứa khoảng trắng (`blank`). Nếu vi phạm $\rightarrow$ trả về `INVALID_INPUT` hoặc ném ngoại lệ tương ứng mà không cần truy vấn Database.
* **BR-AUTH-02 (Chuẩn hóa tài khoản):** Username/Email khi kiểm tra phải được chuẩn hóa về chữ thường (`toLowerCase(Locale.ROOT)`) để không phân biệt hoa thường khi đăng nhập. Mật khẩu phải phân biệt hoa thường (`case-sensitive`).

### 2.2 Nhóm quy tắc xác thực tài khoản & mật khẩu (Authentication & Security)
* **BR-AUTH-03 (Tài khoản không tồn tại):** Nếu Username/Email không có trong hệ thống $\rightarrow$ trả về mã lỗi `INVALID_CREDENTIALS` (Không thông báo "User không tồn tại" để phòng chống tấn công dò quét tài khoản - User Enumeration Attack).
* **BR-AUTH-04 (Tài khoản bị vô hiệu hóa):** Nếu tài khoản có trạng thái `DISABLED` / `INACTIVE` $\rightarrow$ chặn đăng nhập ngay lập tức, trả về `ACCOUNT_DISABLED`.
* **BR-AUTH-05 (Tài khoản đang bị khóa):** Nếu tài khoản đang có cờ `isLocked = true` $\rightarrow$ trả về `ACCOUNT_LOCKED`, **không** tăng bộ đếm số lần sai.
* **BR-AUTH-06 (Bảo vệ Brute-force / Khóa sau 5 lần sai):**
  * Mỗi lần nhập sai mật khẩu: tăng thuộc tính `failedAttempts` thêm 1 đơn vị.
  * Khi `failedAttempts < 5`: trả về `INVALID_CREDENTIALS`.
  * Khi `failedAttempts >= 5`: tự động kích hoạt cờ `isLocked = true` và trả về `ACCOUNT_LOCKED`.
* **BR-AUTH-07 (Đăng nhập thành công):** Khi nhập đúng thông tin:
  * Bộ đếm `failedAttempts` phải được reset về `0`.
  * Cập nhật thời gian đăng nhập mới nhất (`lastLoginAt = now()`).
  * Sinh mã xác thực JWT Access Token hợp lệ, trong Payload chứa đúng `userId`, `username`, và `role` tương ứng.

### 2.3 Nhóm quy tắc đăng xuất (Logout)
* **BR-AUTH-08 (Thu hồi phiên làm việc):** Đăng xuất hợp lệ khi cung cấp Token còn hạn $\rightarrow$ đưa Token vào danh sách thu hồi (Token Blacklist / Revoke Refresh Token) và trả về `LOGOUT_SUCCESS`.
* **BR-AUTH-09 (Token không hợp lệ khi logout):** Nếu Token rỗng hoặc hết hạn $\rightarrow$ trả về `INVALID_TOKEN`.

---

## 3. THIẾT KẾ TEST CASES VÀ MÔ TẢ PHƯƠNG THỨC KIỂM THỬ

Áp dụng các kỹ thuật kiểm thử hộp đen chuẩn quốc tế:
1. **Equivalence Partitioning (EP):** Phân vùng giá trị hợp lệ / không hợp lệ (null, rỗng, tài khoản đúng/sai, trạng thái tài khoản).
2. **Boundary Value Analysis (BVA):** Phân tích biên số lần đăng nhập sai (ngay dưới ngưỡng: 4 lần; đúng ngưỡng: 5 lần; trên ngưỡng: 6 lần).
3. **Decision Table Testing (Bảng quyết định):** Kết hợp các điều kiện kiểm tra của phương thức `login()`.

### 3.1 Bảng quyết định (Decision Table) cho `login()`

| Điều kiện / Quy tắc | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---|---|---|---|---|---|
| User tồn tại trong DB? | N | Y | Y | Y | Y | Y |
| Trạng thái DISABLED? | – | Y | N | N | N | N |
| Đang bị khóa (`isLocked`)? | – | – | Y | N | N | N |
| Mật khẩu trùng khớp? | – | – | – | N | N | Y |
| Đã sai 4 lần trước đó? | – | – | – | N | Y | – |
| **Kết quả mong đợi** | `INVALID_CREDENTIALS` | `ACCOUNT_DISABLED` | `ACCOUNT_LOCKED` | `INVALID_CREDENTIALS`<br>(counter + 1) | `ACCOUNT_LOCKED`<br>(counter = 5, lock) | `SUCCESS`<br>(counter = 0, trả Token) |

---

### 3.2 Danh sách chi tiết các Test Cases (Test Matrix)

| Mã TC | Tên phương thức Test | Tình huống kiểm thử | Dữ liệu đầu vào (Input) | Kết quả mong đợi (Expected) | Kỹ thuật áp dụng |
|---|---|---|---|---|---|
| **TC_AUTH_01** | `login_NullOrBlankUsername_ReturnsInvalidInput` | Username null hoặc rỗng | `username = ""` hoặc `null`, `pass = "Pass@123"` | `ResultCode.INVALID_INPUT` | EP (Hộp đen) |
| **TC_AUTH_02** | `login_NullOrBlankPassword_ReturnsInvalidInput` | Password null hoặc rỗng | `username = "manager_01"`, `pass = ""` | `ResultCode.INVALID_INPUT` | EP |
| **TC_AUTH_03** | `login_NonExistentUser_ReturnsInvalidCredentials` | Tài khoản không có trong DB | `username = "ghost_driver"`, `pass = "Pass@123"` | `ResultCode.INVALID_CREDENTIALS` | EP |
| **TC_AUTH_04** | `login_DisabledAccount_ReturnsAccountDisabled` | Tài khoản bị cấm hoạt động | User có status = `DISABLED` | `ResultCode.ACCOUNT_DISABLED` | Bảng quyết định R2 |
| **TC_AUTH_05** | `login_AlreadyLockedAccount_ReturnsAccountLocked` | Đang bị khóa nhập pass (đúng hoặc sai) | User có `isLocked = true` | `ResultCode.ACCOUNT_LOCKED`, counter giữ nguyên | Bảng quyết định R3 |
| **TC_AUTH_06** | `login_WrongPasswordUnderThreshold_IncrementsFailedAttempts` | Sai mật khẩu lần 1 đến 4 | Nhập sai pass 1..4 lần | `INVALID_CREDENTIALS`, `failedAttempts` tăng đúng số lần | BVA / State |
| **TC_AUTH_07** | `login_WrongPassword5thTime_LocksAccount` | Sai mật khẩu đúng lần thứ 5 | Nhập sai pass liên tiếp 5 lần | `ACCOUNT_LOCKED`, `isLocked = true`, `failedAttempts = 5` | BVA (Biên 5 lần) |
| **TC_AUTH_08** | `login_CorrectCredentials_ResetsFailedAttemptsAndReturnsSuccess` | Đăng nhập đúng sau khi từng sai | Đã sai 3 lần, lần 4 nhập đúng | `SUCCESS`, `failedAttempts = 0`, token != null | State Transition |
| **TC_AUTH_09** | `login_CaseInsensitiveUsername_ReturnsSuccess` | Nhập username chữ hoa/thường | `"OPERATOR_01"`, `"operator_01"` | Đều trả về `SUCCESS` | EP |
| **TC_AUTH_10** | `login_CaseSensitivePassword_WrongCaseReturnsError` | Mật khẩu sai chữ hoa/thường | Password đổi chữ hoa thành thường | `INVALID_CREDENTIALS` | EP |
| **TC_AUTH_11** | `login_RoleManager_TokenContainsManagerClaims` | Manager đăng nhập thành công | User có Role = `MANAGER` | Token có claim `role: "MANAGER"` | Role-based Auth |
| **TC_AUTH_12** | `login_RoleTerminalOperator_TokenContainsOperatorClaims` | Operator đăng nhập thành công | User có Role = `TERMINAL_OPERATOR` | Token có claim `role: "TERMINAL_OPERATOR"` | Role-based Auth |
| **TC_AUTH_13** | `login_RoleShippingLineStaff_TokenContainsStaffClaims` | Shipping Line Staff đăng nhập | User có Role = `SHIPPING_LINE_STAFF` | Token có claim `role: "SHIPPING_LINE_STAFF"` | Role-based Auth |
| **TC_AUTH_14** | `login_RoleTruckDriver_TokenContainsDriverClaims` | Driver đăng nhập | User có Role = `TRUCK_DRIVER` | Token có claim `role: "TRUCK_DRIVER"` | Role-based Auth |
| **TC_AUTH_15** | `logout_ValidToken_ReturnsSuccess` | Đăng xuất với token hợp lệ | Token đang hoạt động | `LOGOUT_SUCCESS`, token bị vô hiệu hóa | State Transition |
| **TC_AUTH_16** | `logout_NullOrExpiredToken_ReturnsInvalidToken` | Đăng xuất với token rỗng/hết hạn | `token = null` hoặc `expired` | `INVALID_TOKEN` | EP |

---

## 4. MÃ NGUỒN KIỂM THỬ MINH HỌA (THEO CÁC TEST FRAMEWORK PHỔ BIẾN)

### 4.1 Triển khai bằng Java (JUnit 5 + Mockito)
*Dành cho dự án Spring Boot / Java Console:*

```java
package ctms.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CTMS - Authentication Unit Tests (JUnit 5)")
class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @ParameterizedTest(name = "[{index}] username trống: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void login_NullOrBlankUsername_ReturnsInvalidInput(String username) {
        AuthResult result = authService.login(username, "Secret@123");
        assertEquals(ResultCode.INVALID_INPUT, result.getCode());
        verifyNoInteractions(userRepository); // Không tốn chi phí gọi DB
    }

    @Test
    @DisplayName("Sai mật khẩu liên tiếp 5 lần -> Khóa tài khoản")
    void login_WrongPassword5thTime_LocksAccount() {
        // Arrange
        User user = new User("driver_01", "driver@ctms.vn", "hashed_pass", Role.TRUCK_DRIVER);
        user.setFailedAttempts(4); // Đã sai 4 lần
        when(userRepository.findByUsername("driver_01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashed_pass")).thenReturn(false);

        // Act
        AuthResult result = authService.login("driver_01", "WrongPass");

        // Assert
        assertEquals(ResultCode.ACCOUNT_LOCKED, result.getCode());
        assertEquals(5, user.getFailedAttempts());
        assertTrue(user.isLocked());
        verify(userRepository).save(user); // Đảm bảo trạng thái khóa được lưu
    }

    @Test
    @DisplayName("Manager đăng nhập thành công -> Cấp Token chứa quyền MANAGER")
    void login_Manager_ReturnsSuccessWithManagerRoleClaim() {
        // Arrange
        User manager = new User("manager_steve", "manager@ctms.vn", "hashed_pass", Role.MANAGER);
        when(userRepository.findByUsername("manager_steve")).thenReturn(Optional.of(manager));
        when(passwordEncoder.matches("Valid@2026", "hashed_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken(manager)).thenReturn("jwt.token.manager");

        // Act
        AuthResult result = authService.login("manager_steve", "Valid@2026");

        // Assert
        assertEquals(ResultCode.SUCCESS, result.getCode());
        assertEquals("jwt.token.manager", result.getToken());
        assertEquals(0, manager.getFailedAttempts()); // Reset bộ đếm
    }
}
```

---

### 4.2 Triển khai bằng C# (.NET xUnit + Moq)
*Dành cho dự án ASP.NET Core Web API / Clean Architecture:*

```csharp
using Moq;
using Xunit;
using CTMS.Core.Entities;
using CTMS.Core.Interfaces;
using CTMS.Services;

namespace CTMS.Tests.UnitTests
{
    public class AuthServiceTests
    {
        private readonly Mock<IUserRepository> _userRepoMock;
        private readonly Mock<IPasswordHasher> _hasherMock;
        private readonly Mock<ITokenService> _tokenServiceMock;
        private readonly IAuthService _authService;

        public AuthServiceTests()
        {
            _userRepoMock = new Mock<IUserRepository>();
            _hasherMock = new Mock<IPasswordHasher>();
            _tokenServiceMock = new Mock<ITokenService>();
            _authService = new AuthService(_userRepoMock.Object, _hasherMock.Object, _tokenServiceMock.Object);
        }

        [Theory]
        [InlineData(null)]
        [InlineData("")]
        [InlineData("   ")]
        public async Task Login_EmptyUsername_ReturnsInvalidInput(string username)
        {
            var result = await _authService.LoginAsync(username, "Pass@123");
            Assert.Equal(AuthResultCode.InvalidInput, result.Code);
        }

        [Fact]
        public async Task Login_UserNotFound_ReturnsInvalidCredentials()
        {
            _userRepoMock.Setup(r => r.GetByUsernameAsync("ghost")).ReturnsAsync((User)null);

            var result = await _authService.LoginAsync("ghost", "Pass@123");
            Assert.Equal(AuthResultCode.InvalidCredentials, result.Code);
        }

        [Fact]
        public async Task Login_TerminalOperatorSuccess_ReturnsTokenWithCorrectRole()
        {
            var user = new User { Username = "operator_01", Role = UserRole.TerminalOperator, FailedAttempts = 2 };
            _userRepoMock.Setup(r => r.GetByUsernameAsync("operator_01")).ReturnsAsync(user);
            _hasherMock.Setup(h => h.VerifyPassword("Pass@123", user.PasswordHash)).Returns(true);
            _tokenServiceMock.Setup(t => t.CreateToken(user)).Returns("token_abc_xyz");

            var result = await _authService.LoginAsync("operator_01", "Pass@123");

            Assert.Equal(AuthResultCode.Success, result.Code);
            Assert.Equal(0, user.FailedAttempts); // Đã reset bộ đếm
            Assert.Equal("token_abc_xyz", result.AccessToken);
        }
    }
}
```

---

### 4.3 Triển khai bằng JavaScript / TypeScript (Jest)
*Dành cho dự án Node.js / Express / NestJS:*

```typescript
describe('CTMS AuthService - Login Unit Tests', () => {
    let authService: AuthService;
    let userRepository: any;
    let tokenService: any;

    beforeEach(() => {
        userRepository = {
            findByUsername: jest.fn(),
            save: jest.fn(),
        };
        tokenService = { generateToken: jest.fn() };
        authService = new AuthService(userRepository, tokenService);
    });

    test('Đăng nhập thất bại khi tài khoản đang bị khóa', async () => {
        userRepository.findByUsername.mockResolvedValue({
            username: 'driver_01',
            isLocked: true,
            failedAttempts: 5,
        });

        const result = await authService.login('driver_01', 'AnyPassword');
        expect(result.code).toBe('ACCOUNT_LOCKED');
    });

    test('Đăng nhập thành công với vai trò Shipping Line Staff', async () => {
        const staff = {
            username: 'maersk_staff',
            role: 'SHIPPING_LINE_STAFF',
            isLocked: false,
            failedAttempts: 0,
        };
        userRepository.findByUsername.mockResolvedValue(staff);
        tokenService.generateToken.mockReturnValue('valid_jwt_token');

        const result = await authService.login('maersk_staff', 'CorrectPass@123');
        expect(result.code).toBe('SUCCESS');
        expect(result.token).toBe('valid_jwt_token');
    });
});
```

---

## 5. ĐÁNH GIÁ CHẤT LƯỢNG VÀ KẾT QUẢ ĐO COVERAGE

* **Mục tiêu Code Coverage:**
  * **Line Coverage:** Đạt tối thiểu **85%** trên toàn bộ module xác thực.
  * **Branch Coverage:** Đạt tối thiểu **80%** (bao phủ toàn bộ các nhánh rẽ `if-else` của bảng quyết định).
* **Kết luận đạt được:**
  * Bộ test case độc lập, không phụ thuộc môi trường mạng hoặc Database thật nhờ kỹ thuật Mocking.
  * Đảm bảo tính toàn vẹn nghiệp vụ cảng biển: đúng vai trò mới được cấp quyền thao tác trên các màn hình tương ứng.
