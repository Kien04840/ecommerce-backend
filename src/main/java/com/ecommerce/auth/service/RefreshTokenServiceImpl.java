package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lớp triển khai các nghiệp vụ cho {@link RefreshTokenService}.
 * <p>
 * Đảm bảo các bất biến và ranh giới bảo mật:
 * <ul>
 *     <li>Thời hạn hiệu lực mặc định: 7 ngày (604,800 giây).</li>
 *     <li>Cơ chế RTR (Refresh Token Rotation): mỗi token chỉ được phép dùng đúng một lần.</li>
 *     <li>Bảo vệ chống tấn công Token Reuse: thu hồi toàn bộ token nếu phát hiện hành vi dùng lại token đã thu hồi.</li>
 *     <li>Hủy toàn bộ phiên đăng nhập của người dùng khi đổi mật khẩu.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final long DEFAULT_REFRESH_TOKEN_VALIDITY_SECONDS = 7L * 24 * 60 * 60; // 7 ngày = 604800s

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenValiditySeconds = DEFAULT_REFRESH_TOKEN_VALIDITY_SECONDS;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        log.debug("Tạo mới Refresh Token cho người dùng ID: {}", user.getId());

        String tokenString = generateSecureRandomToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds);

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(tokenString)
            .expiryDate(expiryDate)
            .revoked(false)
            .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Đã phát hành Refresh Token mới cho người dùng ID: {}, hết hạn lúc: {}", user.getId(), expiryDate);
        return savedToken;
    }

    @Override
    public RefreshToken verifyRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Refresh token không được để trống");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token.trim())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Mã làm mới (Refresh Token) không tồn tại"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            log.warn("Kiểm tra Refresh Token thất bại: Token đã bị thu hồi trước đó");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Mã làm mới (Refresh Token) đã bị vô hiệu hóa");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Kiểm tra Refresh Token thất bại: Token đã hết hạn lúc {}", refreshToken.getExpiryDate());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Mã làm mới (Refresh Token) đã hết hạn sử dụng");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String oldToken) {
        if (oldToken == null || oldToken.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Refresh token không được để trống");
        }

        String trimmedToken = oldToken.trim();
        LocalDateTime now = LocalDateTime.now();

        // 1. Khóa và đọc bản ghi Refresh Token theo token string (Pessimistic Lock - SELECT ... FOR UPDATE)
        // Ngăn chặn triệt để race condition giữa các request đồng thời cùng mang 1 token cũ
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenForUpdate(trimmedToken);

        if (tokenOpt.isEmpty()) {
            log.warn("Yêu cầu xoay vòng token thất bại: Refresh Token không tồn tại");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Mã làm mới (Refresh Token) không hợp lệ");
        }

        RefreshToken refreshToken = tokenOpt.get();
        User user = refreshToken.getUser();

        // 2. Token Reuse Detection: Nếu token đã bị thu hồi trước đó -> CẢNH BÁO TẤN CÔNG ĐÁNH CẮP TOKEN!
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            Long compromisedUserId = user.getId();
            log.warn("CẢNH BÁO BẢO MẬT: Phát hiện hành vi sử dụng lại Refresh Token đã bị thu hồi! "
                + "User ID: {}. Tiến hành thu hồi toàn bộ token của tài khoản để ngăn chặn truy cập trái phép.", compromisedUserId);
            refreshTokenRepository.revokeAllUserTokens(compromisedUserId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Mã làm mới đã bị vô hiệu hóa do nghi ngờ vi phạm bảo mật");
        }

        // 3. Kiểm tra hạn sử dụng của token
        if (refreshToken.getExpiryDate().isBefore(now)) {
            log.warn("Yêu cầu xoay vòng token thất bại: Refresh Token của User ID {} đã hết hạn sử dụng", user.getId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Mã làm mới (Refresh Token) đã hết hạn sử dụng");
        }

        // 4. Kiểm tra trạng thái tài khoản người dùng: Không cho phép tài khoản bị khóa/vô hiệu hóa refresh token
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("Yêu cầu xoay vòng token thất bại: Tài khoản User ID {} đã bị vô hiệu hóa hoặc khóa", user.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt");
        }

        // 5. Thu hồi token cũ (Refresh Token Rotation - chỉ dùng 1 lần)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        log.info("Đã thu hồi Refresh Token cũ của User ID: {} trong chu kỳ RTR", user.getId());

        // 6. Phát hành Refresh Token mới cho người dùng
        return createRefreshToken(user);
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        refreshTokenRepository.findByToken(token.trim()).ifPresent(refreshToken -> {
            if (!Boolean.TRUE.equals(refreshToken.getRevoked())) {
                refreshToken.setRevoked(true);
                refreshTokenRepository.save(refreshToken);
                log.info("Đã thu hồi Refresh Token cho User ID: {}", refreshToken.getUser().getId());
            }
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        log.info("Thu hồi toàn bộ Refresh Token của người dùng ID: {}", userId);
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("Đã vô hiệu hóa {} Refresh Token của người dùng ID: {}", revokedCount, userId);
    }

    @Override
    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    /**
     * Sinh chuỗi mã token ngẫu nhiên bảo mật 64 ký tự không đoán được.
     */
    private String generateSecureRandomToken() {
        return (UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "")).toLowerCase();
    }
}

