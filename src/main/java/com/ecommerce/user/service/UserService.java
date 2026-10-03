package com.ecommerce.user.service;

import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UpdateUserStatusRequest;
import com.ecommerce.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface định nghĩa các nghiệp vụ quản lý tài khoản người dùng {@link com.ecommerce.user.entity.User}.
 * <p>
 * Đảm nhận các chức năng:
 * <ul>
 *     <li>Tra cứu thông tin người dùng theo ID, username, email.</li>
 *     <li>Phân trang và lọc danh sách người dùng cho quản trị viên.</li>
 *     <li>Cập nhật thông tin hồ sơ người dùng (email).</li>
 *     <li>Cập nhật trạng thái kích hoạt/khóa tài khoản người dùng.</li>
 * </ul>
 * Lưu ý: Quản lý mật khẩu và chứng thực thuộc trách nhiệm của Auth Service.
 */
public interface UserService {

    /**
     * Tra cứu thông tin chi tiết người dùng theo mã định danh ID.
     *
     * @param id mã định danh người dùng
     * @return đối tượng {@link UserResponse} chứa thông tin người dùng
     */
    UserResponse getUserById(Long id);

    /**
     * Tra cứu thông tin người dùng theo tên đăng nhập (username).
     *
     * @param username tên đăng nhập cần tìm
     * @return đối tượng {@link UserResponse} chứa thông tin người dùng
     */
    UserResponse getUserByUsername(String username);

    /**
     * Tra cứu thông tin người dùng theo địa chỉ email.
     *
     * @param email địa chỉ email cần tìm
     * @return đối tượng {@link UserResponse} chứa thông tin người dùng
     */
    UserResponse getUserByEmail(String email);

    /**
     * Lấy danh sách người dùng có phân trang và lọc theo trạng thái kích hoạt.
     *
     * @param enabled  trạng thái kích hoạt (null để lấy tất cả, true: kích hoạt, false: bị khóa)
     * @param pageable thông tin phân trang và sắp xếp
     * @return danh sách phân trang {@link PagedResponse} chứa {@link UserResponse}
     */
    PagedResponse<UserResponse> getUsers(Boolean enabled, Pageable pageable);

    /**
     * Cập nhật thông tin hồ sơ của người dùng (email).
     *
     * @param userId  mã định danh người dùng cần cập nhật
     * @param request dữ liệu yêu cầu cập nhật hồ sơ
     * @return thông tin người dùng sau khi cập nhật
     */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Cập nhật trạng thái kích hoạt hoặc khóa tài khoản người dùng (chỉ dành cho Admin).
     *
     * @param userId  mã định danh người dùng cần cập nhật trạng thái
     * @param request dữ liệu yêu cầu cập nhật trạng thái
     * @return thông tin người dùng sau khi cập nhật
     */
    UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);
}

