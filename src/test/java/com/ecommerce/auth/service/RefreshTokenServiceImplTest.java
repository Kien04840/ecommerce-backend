package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * Kiểm thử đơn vị (Unit Test) cho {@link RefreshTokenServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử RefreshTokenServiceImpl")
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private RefreshToken validRefreshToken;

    @BeforeEach
    void setUp() {
        Role testRole = Role.builder()
            .name("ROLE_CUSTOMER")
            .build();
        ReflectionTestUtils.setField(testRole, "id", 1L);

        testUser = User.builder()
            .username("johndoe")
            .email("john@example.com")
            .password("hashed_password")
            .enabled(true)
            .role(testRole)
            .build();
        ReflectionTestUtils.setField(testUser, "id", 10L);

        validRefreshToken = RefreshToken.builder()
            .user(testUser)
            .token("valid_refresh_token_string_example_123456789")
            .expiryDate(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .build();
        ReflectionTestUtils.setField(validRefreshToken, "id", 100L);
    }

    @Nested
    @DisplayName("Nghiệp vụ tạo mới Refresh Token (createRefreshToken)")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Tạo mới Refresh Token thành công với thời hạn 7 ngày và trạng thái chưa bị thu hồi")
        void createRefreshToken_Success() {
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
                RefreshToken token = invocation.getArgument(0);
                ReflectionTestUtils.setField(token, "id", 101L);
                return token;
            });

            RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser);

            assertNotNull(createdToken);
            assertEquals(101L, createdToken.getId());
            assertEquals(testUser, createdToken.getUser());
            assertNotNull(createdToken.getToken());
            assertFalse(createdToken.getRevoked());
            assertTrue(createdToken.getExpiryDate().isAfter(LocalDateTime.now()));

            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ kiểm tra tính hợp lệ của Refresh Token (verifyRefreshToken)")
    class VerifyRefreshTokenTests {

        @Test
        @DisplayName("Xác thực thành công khi token tồn tại, chưa bị thu hồi và còn hạn")
        void verifyRefreshToken_Success() {
            when(refreshTokenRepository.findByToken("valid_refresh_token_string_example_123456789"))
                .thenReturn(Optional.of(validRefreshToken));

            RefreshToken result = refreshTokenService.verifyRefreshToken("valid_refresh_token_string_example_123456789");

            assertNotNull(result);
            assertEquals(validRefreshToken.getToken(), result.getToken());
            assertFalse(result.getRevoked());
        }

        @Test
        @DisplayName("Ném BusinessException BAD_REQUEST khi token gửi lên là null hoặc khoảng trắng")
        void verifyRefreshToken_BlankToken_ThrowsBadRequest() {
            BusinessException ex1 = assertThrows(BusinessException.class,
                () -> refreshTokenService.verifyRefreshToken(null));
            assertEquals(ErrorCode.BAD_REQUEST, ex1.getErrorCode());

            BusinessException ex2 = assertThrows(BusinessException.class,
                () -> refreshTokenService.verifyRefreshToken("   "));
            assertEquals(ErrorCode.BAD_REQUEST, ex2.getErrorCode());
        }

        @Test
        @DisplayName("Ném BusinessException UNAUTHORIZED khi token không tồn tại trong database")
        void verifyRefreshToken_TokenNotFound_ThrowsUnauthorized() {
            when(refreshTokenRepository.findByToken("non_existent_token"))
                .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.verifyRefreshToken("non_existent_token"));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Ném BusinessException UNAUTHORIZED khi token đã bị thu hồi (revoked = true)")
        void verifyRefreshToken_TokenRevoked_ThrowsUnauthorized() {
            validRefreshToken.setRevoked(true);
            when(refreshTokenRepository.findByToken("valid_refresh_token_string_example_123456789"))
                .thenReturn(Optional.of(validRefreshToken));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.verifyRefreshToken("valid_refresh_token_string_example_123456789"));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("đã bị vô hiệu hóa"));
        }

        @Test
        @DisplayName("Ném BusinessException UNAUTHORIZED khi token đã quá hạn sử dụng")
        void verifyRefreshToken_TokenExpired_ThrowsUnauthorized() {
            validRefreshToken.setExpiryDate(LocalDateTime.now().minusDays(1)); // Đã hết hạn
            when(refreshTokenRepository.findByToken("valid_refresh_token_string_example_123456789"))
                .thenReturn(Optional.of(validRefreshToken));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.verifyRefreshToken("valid_refresh_token_string_example_123456789"));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("đã hết hạn"));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ xoay vòng Refresh Token (rotateRefreshToken - RTR)")
    class RotateRefreshTokenTests {

        @Test
        @DisplayName("Xoay vòng token thành công: thu hồi token cũ và phát hành token mới")
        void rotateRefreshToken_Success() {
            String oldTokenStr = "valid_refresh_token_string_example_123456789";
            when(refreshTokenRepository.findByTokenForUpdate(oldTokenStr))
                .thenReturn(Optional.of(validRefreshToken));

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RefreshToken newToken = refreshTokenService.rotateRefreshToken(oldTokenStr);

            assertNotNull(newToken);
            assertTrue(validRefreshToken.getRevoked()); // Token cũ đã bị revoked
            assertFalse(newToken.getRevoked()); // Token mới chưa bị revoked
            assertEquals(testUser, newToken.getUser());

            // Lưu token cũ (thu hồi) và lưu token mới
            verify(refreshTokenRepository).save(validRefreshToken);
        }

        @Test
        @DisplayName("Phát hiện nguy cơ Token Reuse Attack: thu hồi toàn bộ token của User khi token cũ đã bị revoked")
        void rotateRefreshToken_TokenReuseAttack_RevokesAllTokensAndThrowsUnauthorized() {
            String stolenTokenStr = "stolen_already_revoked_token";
            RefreshToken revokedToken = RefreshToken.builder()
                .user(testUser)
                .token(stolenTokenStr)
                .expiryDate(LocalDateTime.now().plusDays(5))
                .revoked(true) // Đã bị thu hồi trước đó
                .build();

            when(refreshTokenRepository.findByTokenForUpdate(stolenTokenStr))
                .thenReturn(Optional.of(revokedToken));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.rotateRefreshToken(stolenTokenStr));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("nghi ngờ vi phạm bảo mật"));

            // Đảm bảo hệ thống đã thu hồi toàn bộ token của user để bảo vệ tài khoản
            verify(refreshTokenRepository).revokeAllUserTokens(testUser.getId());
        }

        @Test
        @DisplayName("Ném BusinessException UNAUTHORIZED khi token cũ đã hết hạn sử dụng")
        void rotateRefreshToken_TokenExpired_ThrowsUnauthorized() {
            String expiredTokenStr = "expired_token";
            RefreshToken expiredToken = RefreshToken.builder()
                .user(testUser)
                .token(expiredTokenStr)
                .expiryDate(LocalDateTime.now().minusDays(1)) // Đã quá hạn
                .revoked(false)
                .build();

            when(refreshTokenRepository.findByTokenForUpdate(expiredTokenStr))
                .thenReturn(Optional.of(expiredToken));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.rotateRefreshToken(expiredTokenStr));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("đã hết hạn"));
        }

        @Test
        @DisplayName("Ném BusinessException FORBIDDEN khi tài khoản của người dùng đã bị khóa (enabled = false)")
        void rotateRefreshToken_UserDisabled_ThrowsForbidden() {
            String tokenStr = "valid_token_but_user_disabled";
            testUser.setEnabled(false); // Bị khóa
            RefreshToken tokenOfDisabledUser = RefreshToken.builder()
                .user(testUser)
                .token(tokenStr)
                .expiryDate(LocalDateTime.now().plusDays(5))
                .revoked(false)
                .build();

            when(refreshTokenRepository.findByTokenForUpdate(tokenStr))
                .thenReturn(Optional.of(tokenOfDisabledUser));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.rotateRefreshToken(tokenStr));

            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("đã bị khóa hoặc chưa được kích hoạt"));
        }

        @Test
        @DisplayName("Ném BusinessException UNAUTHORIZED khi token cũ không tồn tại trong hệ thống")
        void rotateRefreshToken_TokenNotFound_ThrowsUnauthorized() {
            when(refreshTokenRepository.findByTokenForUpdate("ghost_token"))
                .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.rotateRefreshToken("ghost_token"));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ thu hồi token (Revocation)")
    class RevocationTests {

        @Test
        @DisplayName("Thu hồi một token cụ thể thành công")
        void revokeToken_Success() {
            when(refreshTokenRepository.findByToken("token_to_revoke"))
                .thenReturn(Optional.of(validRefreshToken));

            refreshTokenService.revokeToken("token_to_revoke");

            assertTrue(validRefreshToken.getRevoked());
            verify(refreshTokenRepository).save(validRefreshToken);
        }

        @Test
        @DisplayName("Bỏ qua an toàn khi thu hồi token là null hoặc khoảng trắng")
        void revokeToken_BlankToken_DoesNothing() {
            refreshTokenService.revokeToken(null);
            refreshTokenService.revokeToken("   ");

            verify(refreshTokenRepository, never()).findByToken(anyString());
        }

        @Test
        @DisplayName("Thu hồi toàn bộ token của người dùng thành công")
        void revokeAllUserTokens_Success() {
            when(refreshTokenRepository.revokeAllUserTokens(10L)).thenReturn(3);

            refreshTokenService.revokeAllUserTokens(10L);

            verify(refreshTokenRepository).revokeAllUserTokens(10L);
        }
    }
}

