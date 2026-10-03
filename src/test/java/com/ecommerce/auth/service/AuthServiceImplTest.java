package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.ChangePasswordRequest;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RefreshTokenRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.TokenResponse;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho {@link AuthServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử AuthServiceImpl")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role customerRole;
    private User activeUser;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder()
            .name("ROLE_CUSTOMER")
            .build();
        ReflectionTestUtils.setField(customerRole, "id", 1L);

        activeUser = User.builder()
            .username("johndoe")
            .email("john@example.com")
            .password("encoded_hashed_password")
            .enabled(true)
            .role(customerRole)
            .build();
        ReflectionTestUtils.setField(activeUser, "id", 10L);
    }

    @Nested
    @DisplayName("Nghiệp vụ đăng ký tài khoản (register)")
    class RegisterTests {

        private RegisterRequest validRequest;

        @BeforeEach
        void setUpRegister() {
            validRequest = new RegisterRequest("newuser", "newuser@example.com", "Password@123");
        }

        @Test
        @DisplayName("Đăng ký tài khoản thành công: mật khẩu được mã hóa BCrypt, gán ROLE_CUSTOMER")
        void register_Success() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode("Password@123")).thenReturn("encoded_bcrypt_pwd");

            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                ReflectionTestUtils.setField(user, "id", 20L);
                return user;
            });

            UserResponse response = authService.register(validRequest);

            assertNotNull(response);
            assertEquals(20L, response.id());
            assertEquals("newuser", response.username());
            assertEquals("newuser@example.com", response.email());
            assertTrue(response.enabled());
            assertEquals("ROLE_CUSTOMER", response.role().name());

            verify(passwordEncoder).encode("Password@123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Ném BusinessException DUPLICATE_RESOURCE khi tên đăng nhập đã tồn tại")
        void register_DuplicateUsername_ThrowsDuplicateResource() {
            when(userRepository.existsByUsername("newuser")).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(validRequest));

            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Tên đăng nhập đã tồn tại"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Ném BusinessException DUPLICATE_RESOURCE khi email đã tồn tại")
        void register_DuplicateEmail_ThrowsDuplicateResource() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(validRequest));

            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Email đã tồn tại"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi không tìm thấy vai trò ROLE_CUSTOMER trong database")
        void register_RoleNotFound_ThrowsResourceNotFoundException() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> authService.register(validRequest));

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ đăng nhập tài khoản (login)")
    class LoginTests {

        private LoginRequest loginByUsername;
        private LoginRequest loginByEmail;
        private RefreshToken testRefreshToken;

        @BeforeEach
        void setUpLogin() {
            loginByUsername = new LoginRequest("johndoe", "Password@123");
            loginByEmail = new LoginRequest("john@example.com", "Password@123");

            testRefreshToken = RefreshToken.builder()
                .user(activeUser)
                .token("sample_refresh_token_string")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        }

        @Test
        @DisplayName("Đăng nhập thành công bằng username: trả về cặp Access Token và Refresh Token")
        void login_ByUsername_Success() {
            when(userRepository.findByUsernameWithRole("johndoe")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("Password@123", activeUser.getPassword())).thenReturn(true);
            when(jwtService.generateAccessToken(10L, "john@example.com", "ROLE_CUSTOMER")).thenReturn("jwt_access_token");
            when(jwtService.getAccessTokenValiditySeconds()).thenReturn(900L);
            when(refreshTokenService.createRefreshToken(activeUser)).thenReturn(testRefreshToken);

            TokenResponse response = authService.login(loginByUsername);

            assertNotNull(response);
            assertEquals("jwt_access_token", response.accessToken());
            assertEquals("sample_refresh_token_string", response.refreshToken());
            assertEquals("Bearer", response.tokenType());
            assertEquals(900L, response.expiresIn());
        }

        @Test
        @DisplayName("Đăng nhập thành công bằng email: trả về cặp Access Token và Refresh Token")
        void login_ByEmail_Success() {
            when(userRepository.findWithRoleByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("Password@123", activeUser.getPassword())).thenReturn(true);
            when(jwtService.generateAccessToken(10L, "john@example.com", "ROLE_CUSTOMER")).thenReturn("jwt_access_token");
            when(jwtService.getAccessTokenValiditySeconds()).thenReturn(900L);
            when(refreshTokenService.createRefreshToken(activeUser)).thenReturn(testRefreshToken);

            TokenResponse response = authService.login(loginByEmail);

            assertNotNull(response);
            assertEquals("jwt_access_token", response.accessToken());
            assertEquals("sample_refresh_token_string", response.refreshToken());
        }

        @Test
        @DisplayName("Ném BusinessException UNAUTHORIZED khi không tìm thấy tài khoản")
        void login_UserNotFound_ThrowsUnauthorized() {
            when(userRepository.findByUsernameWithRole("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("nonexistent", "Password@123")));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Tên đăng nhập hoặc mật khẩu không chính xác"));
        }

        @Test
        @DisplayName("Ném BusinessException UNAUTHORIZED khi nhập sai mật khẩu")
        void login_WrongPassword_ThrowsUnauthorized() {
            when(userRepository.findByUsernameWithRole("johndoe")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("WrongPassword", activeUser.getPassword())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("johndoe", "WrongPassword")));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Tên đăng nhập hoặc mật khẩu không chính xác"));
        }

        @Test
        @DisplayName("Ném BusinessException FORBIDDEN khi tài khoản đã bị vô hiệu hóa (enabled = false)")
        void login_AccountDisabled_ThrowsForbidden() {
            activeUser.setEnabled(false);
            when(userRepository.findByUsernameWithRole("johndoe")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("Password@123", activeUser.getPassword())).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginByUsername));

            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("đã bị khóa hoặc chưa được kích hoạt"));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ làm mới token (refreshToken - RTR)")
    class RefreshTokenTests {

        @Test
        @DisplayName("Làm mới token thành công: xoay vòng Refresh Token và cấp phát Access Token mới")
        void refreshToken_Success() {
            String oldTokenStr = "old_refresh_token";
            RefreshToken rotatedToken = RefreshToken.builder()
                .user(activeUser)
                .token("new_rotated_refresh_token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

            when(refreshTokenService.rotateRefreshToken(oldTokenStr)).thenReturn(rotatedToken);
            when(jwtService.generateAccessToken(10L, "john@example.com", "ROLE_CUSTOMER")).thenReturn("new_jwt_access_token");
            when(jwtService.getAccessTokenValiditySeconds()).thenReturn(900L);

            TokenResponse response = authService.refreshToken(new RefreshTokenRequest(oldTokenStr));

            assertNotNull(response);
            assertEquals("new_jwt_access_token", response.accessToken());
            assertEquals("new_rotated_refresh_token", response.refreshToken());
            assertEquals(900L, response.expiresIn());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ đăng xuất (logout)")
    class LogoutTests {

        @Test
        @DisplayName("Đăng xuất thành công: thu hồi Refresh Token")
        void logout_Success() {
            String token = "refresh_token_to_logout";

            authService.logout(token);

            verify(refreshTokenService).revokeToken(token);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ đổi mật khẩu (changePassword)")
    class ChangePasswordTests {

        private ChangePasswordRequest changeRequest;

        @BeforeEach
        void setUpChangePassword() {
            changeRequest = new ChangePasswordRequest("OldPassword@123", "NewPassword@456");
        }

        @Test
        @DisplayName("Đổi mật khẩu thành công: mã hóa mật khẩu mới, lưu user và thu hồi toàn bộ Refresh Token")
        void changePassword_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("OldPassword@123", activeUser.getPassword())).thenReturn(true);
            when(passwordEncoder.matches("NewPassword@456", activeUser.getPassword())).thenReturn(false);
            when(passwordEncoder.encode("NewPassword@456")).thenReturn("encoded_new_hashed_pwd");

            authService.changePassword(10L, changeRequest);

            assertEquals("encoded_new_hashed_pwd", activeUser.getPassword());
            verify(userRepository).save(activeUser);
            verify(refreshTokenService).revokeAllUserTokens(10L);
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi không tìm thấy người dùng theo ID")
        void changePassword_UserNotFound_ThrowsResourceNotFoundException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> authService.changePassword(999L, changeRequest));

            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("Ném BusinessException BAD_REQUEST khi mật khẩu hiện tại không chính xác")
        void changePassword_WrongCurrentPassword_ThrowsBadRequest() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("OldPassword@123", activeUser.getPassword())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(10L, changeRequest));

            assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Mật khẩu hiện tại không chính xác"));

            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("Ném BusinessException BAD_REQUEST khi mật khẩu mới trùng mật khẩu hiện tại")
        void changePassword_NewPasswordSameAsOld_ThrowsBadRequest() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("OldPassword@123", activeUser.getPassword())).thenReturn(true);
            when(passwordEncoder.matches("NewPassword@456", activeUser.getPassword())).thenReturn(true); // Trùng nhau

            BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(10L, changeRequest));

            assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("không được trùng"));

            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any());
        }
    }
}

