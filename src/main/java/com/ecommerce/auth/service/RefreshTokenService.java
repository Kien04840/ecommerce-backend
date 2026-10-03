package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.user.entity.User;

/**
 * Service interface quản lý vòng đời và cơ chế làm mới phiên đăng nhập {@link RefreshToken}.
 * <p>
 * Tuân thủ quy định tại docs/business-rules.md:
 * <ul>
 *     <li>Thời hạn hiệu lực: 7 ngày (604,800 giây).</li>
 *     <li>Lưu trữ trạng thái an toàn trong database, hỗ trợ thu hồi tức thì.</li>
 *     <li>Cơ chế xoay vòng Refresh Token (Refresh Token Rotation - RTR): mỗi token chỉ được dùng 1 lần.</li>
 *     <li>Phát hiện nguy cơ tấn công sử dụng lại token (Token Reuse Detection) để bảo vệ tài khoản.</li>
 *     <li>Thu hồi toàn bộ token khi đổi mật khẩu hoặc đăng xuất.</li>
 * </ul>
 */
public interface RefreshTokenService {

    /**
     * Tạo mới và lưu trữ Refresh Token ngẫu nhiên cho người dùng với thời hạn 7 ngày.
     *
     * @param user thực thể {@link User} nhận token
     * @return thực thể {@link RefreshToken} đã được lưu vào cơ sở dữ liệu
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Kiểm tra tính hợp lệ của Refresh Token (chưa bị thu hồi và chưa hết hạn).
     *
     * @param token chuỗi token cần kiểm tra
     * @return thực thể {@link RefreshToken} hợp lệ kèm thông tin người dùng
     * @throws com.ecommerce.common.exception.BusinessException nếu token không tồn tại, đã bị thu hồi hoặc hết hạn
     */
    RefreshToken verifyRefreshToken(String token);

    /**
     * Thực hiện cơ chế xoay vòng Refresh Token (Refresh Token Rotation - RTR).
     * <p>
     * Vô hiệu hóa token cũ và sinh ra một cặp token hoàn toàn mới cho cùng người dùng.
     * Nếu phát hiện token cũ đã bị thu hồi trước đó (nguy cơ Token Reuse Attack),
     * hệ thống sẽ thu hồi toàn bộ token đang hoạt động của người dùng để bảo vệ tài khoản.
     *
     * @param oldToken chuỗi token cũ cần đổi
     * @return thực thể {@link RefreshToken} mới được cấp phát
     */
    RefreshToken rotateRefreshToken(String oldToken);

    /**
     * Thu hồi một Refresh Token cụ thể (sử dụng khi đăng xuất).
     *
     * @param token chuỗi token cần thu hồi
     */
    void revokeToken(String token);

    /**
     * Thu hồi toàn bộ Refresh Token của một người dùng (sử dụng khi đổi mật khẩu hoặc khóa tài khoản).
     *
     * @param userId mã định danh ID của người dùng
     */
    void revokeAllUserTokens(Long userId);

    /**
     * Lấy thời hạn hiệu lực của Refresh Token tính theo đơn vị giây.
     *
     * @return số giây hiệu lực (mặc định 604800s = 7 ngày)
     */
    long getRefreshTokenValiditySeconds();
}

