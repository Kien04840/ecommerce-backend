package com.ecommerce.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Service quản lý vòng đời và xử lý các thao tác liên quan đến mã xác thực JSON Web Token (JWT).
 * <p>
 * Tuân thủ quy định tại docs/business-rules.md:
 * <ul>
 *     <li>Thuật toán ký: HMAC-SHA256 với độ dài khóa tối thiểu 256 bits.</li>
 *     <li>Thời hạn hiệu lực Access Token: chính xác 15 phút (900 giây).</li>
 *     <li>Cơ chế phi trạng thái (Stateless): không lưu Access Token vào cơ sở dữ liệu.</li>
 *     <li>Các Claims tiêu chuẩn: {@code sub} (ID người dùng), {@code email}, {@code role}, {@code iat}, {@code exp}.</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    private static final long DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS = 900L; // 15 phút
    private static final String DEFAULT_ISSUER = "ecommerce-backend";

    private final SecretKey secretKey;
    private final long accessTokenValiditySeconds;
    private final String issuer;

    @Autowired
    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiration:900}") long accessTokenValiditySeconds,
        @Value("${jwt.issuer:ecommerce-backend}") String issuer
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Cấu hình 'jwt.secret' không được để trống. Vui lòng thiết lập biến môi trường JWT_SECRET.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("Khóa bí mật JWT ('jwt.secret') phải có độ dài tối thiểu 256 bits (32 bytes) để đảm bảo an toàn thuật toán HMAC-SHA256.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValiditySeconds = accessTokenValiditySeconds != 0
            ? accessTokenValiditySeconds
            : DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS;
        this.issuer = (issuer != null && !issuer.isBlank()) ? issuer : DEFAULT_ISSUER;
    }

    public JwtService(String secret, long accessTokenValiditySeconds) {
        this(secret, accessTokenValiditySeconds, DEFAULT_ISSUER);
    }

    /**
     * Tạo mới Access Token JWT với các thông tin người dùng được mã hóa trong claims.
     *
     * @param userId mã định danh ID người dùng
     * @param email  địa chỉ email người dùng
     * @param role   tên vai trò phân quyền (ví dụ: ROLE_CUSTOMER)
     * @return chuỗi JWT Access Token hoàn chỉnh
     */
    public String generateAccessToken(Long userId, String email, String role) {
        long nowMillis = System.currentTimeMillis();
        Date issuedAt = new Date(nowMillis);
        Date expiration = new Date(nowMillis + (accessTokenValiditySeconds * 1000));

        return Jwts.builder()
            .issuer(issuer)
            .subject(String.valueOf(userId))
            .claim("email", email)
            .claim("role", role)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Trích xuất đơn vị phát hành (issuer) từ claims của JWT.
     *
     * @param token chuỗi JWT
     * @return tên đơn vị phát hành
     */
    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    /**
     * Trích xuất ID người dùng từ claim {@code sub} trong JWT.
     *
     * @param token chuỗi JWT
     * @return ID người dùng kiểu {@link Long}
     */
    public Long extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return Long.valueOf(subject);
    }

    /**
     * Trích xuất địa chỉ email từ claims của JWT.
     *
     * @param token chuỗi JWT
     * @return địa chỉ email người dùng
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Trích xuất vai trò phân quyền (role) từ claims của JWT.
     *
     * @param token chuỗi JWT
     * @return tên vai trò (ví dụ: ROLE_CUSTOMER)
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Trích xuất thời điểm hết hạn của JWT.
     *
     * @param token chuỗi JWT
     * @return mốc thời gian {@link Date} hết hạn
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Kiểm tra tính hợp lệ về mặt chữ ký số, đơn vị phát hành (issuer) và thời hạn của JWT.
     *
     * @param token chuỗi JWT cần kiểm tra
     * @return {@code true} nếu token hợp lệ và còn hạn; ngược lại {@code false}
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Mã xác thực JWT không hợp lệ hoặc đã hết hạn: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra xem token đã quá thời hạn sử dụng hay chưa.
     *
     * @param token chuỗi JWT
     * @return {@code true} nếu đã hết hạn, ngược lại {@code false}
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Lấy thời hạn hiệu lực của Access Token tính theo đơn vị giây.
     *
     * @return số giây hiệu lực (mặc định 900s = 15 phút)
     */
    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    /**
     * Lấy tên đơn vị phát hành token được cấu hình.
     *
     * @return chuỗi tên đơn vị phát hành (issuer)
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Trích xuất claim bất kỳ từ token bằng hàm chuyển đổi.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Giải mã và đọc toàn bộ Claims payload từ token đã ký.
     * Xác thực cả chữ ký HMAC-SHA256 lẫn đơn vị phát hành token (issuer).
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
