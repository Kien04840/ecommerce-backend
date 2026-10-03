package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.ChangePasswordRequest;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RefreshTokenRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.TokenResponse;
import com.ecommerce.user.dto.UserResponse;

/**
 * Service interface định nghĩa các nghiệp vụ xác thực (Authentication), cấp phát token
 * và quản lý chứng thực cho toàn bộ hệ thống E-Commerce.
 * <p>
 * Đảm nhận các chức năng:
 * <ul>
 *     <li>Đăng ký tài khoản khách hàng mới với mật khẩu được mã hóa BCrypt (strength = 12).</li>
 *     <li>Đăng nhập tài khoản bằng username hoặc email, cấp phát cặp Access Token (JWT) và Refresh Token.</li>
 *     <li>Làm mới phiên làm việc bằng cơ chế xoay vòng Refresh Token (Refresh Token Rotation - RTR).</li>
 *     <li>Đăng xuất và thu hồi Refresh Token hiện tại.</li>
 *     <li>Thay đổi mật khẩu tài khoản và tự động thu hồi toàn bộ phiên đăng nhập đang hoạt động.</li>
 * </ul>
 */
public interface AuthService {

    /**
     * Đăng ký tài khoản khách hàng mới trong hệ thống.
     * <p>
     * Mặc định gán vai trò {@code ROLE_CUSTOMER}. Mật khẩu được mã hóa an toàn bằng BCrypt.
     *
     * @param request dữ liệu yêu cầu đăng ký
     * @return thông tin người dùng {@link UserResponse} đã tạo thành công
     */
    UserResponse register(RegisterRequest request);

    /**
     * Đăng nhập tài khoản và cấp phát cặp token xác thực.
     * <p>
     * Hỗ trợ đăng nhập linh hoạt bằng username hoặc email.
     *
     * @param request dữ liệu yêu cầu đăng nhập
     * @return {@link TokenResponse} chứa Access Token (15 phút) và Refresh Token (7 ngày)
     */
    TokenResponse login(LoginRequest request);

    /**
     * Cấp phát cặp Access Token và Refresh Token mới thông qua Refresh Token hợp lệ (RTR).
     *
     * @param request dữ liệu yêu cầu làm mới token
     * @return {@link TokenResponse} chứa cặp token mới
     */
    TokenResponse refreshToken(RefreshTokenRequest request);

    /**
     * Đăng xuất tài khoản người dùng bằng cách thu hồi Refresh Token hiện tại.
     *
     * @param refreshToken chuỗi Refresh Token cần thu hồi
     */
    void logout(String refreshToken);

    /**
     * Thay đổi mật khẩu người dùng và thu hồi toàn bộ Refresh Token của tài khoản.
     *
     * @param userId  mã định danh người dùng yêu cầu đổi mật khẩu
     * @param request dữ liệu chứa mật khẩu hiện tại và mật khẩu mới
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}

