package com.ecommerce.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử đơn vị (Unit Test) cho {@link JwtService}.
 */
@DisplayName("Kiểm thử JwtService")
class JwtServiceTest {

    private static final String TEST_SECRET = "testSecretKeyForJwtUnitTestingMustBeAtLeast256BitsLong1234567890";
    private static final long TEST_VALIDITY_SECONDS = 900L; // 15 phút

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, TEST_VALIDITY_SECONDS);
    }

    @Nested
    @DisplayName("Nghiệp vụ sinh mã và trích xuất claims (Token Generation & Claims)")
    class TokenGenerationAndClaims {

        @Test
        @DisplayName("Sinh Access Token thành công và trích xuất đúng thông tin claims")
        void generateAccessToken_Success() {
            Long userId = 10L;
            String email = "customer@example.com";
            String role = "ROLE_CUSTOMER";

            String token = jwtService.generateAccessToken(userId, email, role);

            assertNotNull(token);
            assertFalse(token.isBlank());

            // Kiểm tra các claims được trích xuất
            assertEquals(userId, jwtService.extractUserId(token));
            assertEquals(email, jwtService.extractEmail(token));
            assertEquals(role, jwtService.extractRole(token));
            assertEquals("ecommerce-backend", jwtService.extractIssuer(token));

            // Kiểm tra thời hạn hết hạn trong tương lai
            Date expiration = jwtService.extractExpiration(token);
            assertNotNull(expiration);
            assertTrue(expiration.after(new Date()));
        }

        @Test
        @DisplayName("Lấy thời hạn hiệu lực của Access Token trả về đúng 900 giây (15 phút)")
        void getAccessTokenValiditySeconds_Returns900() {
            assertEquals(900L, jwtService.getAccessTokenValiditySeconds());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ kiểm tra tính hợp lệ của token (validateToken)")
    class TokenValidation {

        @Test
        @DisplayName("Token hợp lệ và chưa hết hạn trả về true")
        void validateToken_ValidToken_ReturnsTrue() {
            String token = jwtService.generateAccessToken(1L, "user@example.com", "ROLE_CUSTOMER");
            assertTrue(jwtService.validateToken(token));
            assertFalse(jwtService.isTokenExpired(token));
        }

        @Test
        @DisplayName("Token với định dạng không hợp lệ trả về false")
        void validateToken_MalformedToken_ReturnsFalse() {
            assertFalse(jwtService.validateToken("invalid.token.string"));
            assertFalse(jwtService.validateToken(""));
        }

        @Test
        @DisplayName("Token được ký bằng secret key khác trả về false")
        void validateToken_DifferentSecretKey_ReturnsFalse() {
            JwtService otherJwtService = new JwtService("anotherSecretKeyThatIsCompletelyDifferentAndAlsoLongEnough1234", 900L);
            String foreignToken = otherJwtService.generateAccessToken(1L, "user@example.com", "ROLE_CUSTOMER");

            assertFalse(jwtService.validateToken(foreignToken));
        }

        @Test
        @DisplayName("Token có issuer không khớp với cấu hình trả về false")
        void validateToken_DifferentIssuer_ReturnsFalse() {
            JwtService foreignIssuerService = new JwtService(TEST_SECRET, 900L, "untrusted-issuer");
            String foreignToken = foreignIssuerService.generateAccessToken(1L, "user@example.com", "ROLE_CUSTOMER");

            assertFalse(jwtService.validateToken(foreignToken));
        }

        @Test
        @DisplayName("Token đã hết hạn trả về false trong validateToken và true trong isTokenExpired")
        void validateToken_ExpiredToken_ReturnsFalse() {
            // Khởi tạo JwtService với thời hạn âm (tức hết hạn ngay)
            JwtService expiredJwtService = new JwtService(TEST_SECRET, -10L);
            String expiredToken = expiredJwtService.generateAccessToken(1L, "user@example.com", "ROLE_CUSTOMER");

            assertFalse(jwtService.validateToken(expiredToken));
            assertTrue(jwtService.isTokenExpired(expiredToken));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ kiểm tra tính hợp lệ của cấu hình JWT Secret (Fail-fast Configuration)")
    class SecretValidationTests {

        @Test
        @DisplayName("Ném IllegalStateException khi jwt.secret là null hoặc rỗng")
        void constructor_BlankSecret_ThrowsIllegalStateException() {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new JwtService(null, 900L));
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new JwtService("", 900L));
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new JwtService("   ", 900L));
        }

        @Test
        @DisplayName("Ném IllegalStateException khi jwt.secret ngắn hơn 32 bytes (< 256 bits)")
        void constructor_ShortSecret_ThrowsIllegalStateException() {
            String shortSecret = "too-short-secret-key-12345"; // < 32 bytes
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new JwtService(shortSecret, 900L));
        }
    }
}

