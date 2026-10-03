package com.ecommerce.user.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.user.dto.RoleResponse;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UpdateUserStatusRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lớp triển khai các nghiệp vụ cho {@link UserService}.
 * <p>
 * Đảm bảo các bất biến và ranh giới nghiệp vụ:
 * <ul>
 *     <li>Kiểm tra không trùng lặp email khi cập nhật hồ sơ (loại trừ chính tài khoản hiện tại).</li>
 *     <li>Bảo vệ thông tin chứng thực: Tuyệt đối không để lộ mật khẩu trong response DTO hay log hệ thống.</li>
 *     <li>Tách biệt ranh giới: UserService chỉ quản lý thông tin hồ sơ người dùng. Mọi thao tác mật khẩu thuộc về Auth Module.</li>
 *     <li>Quản lý trạng thái kích hoạt/khóa tài khoản độc lập, không hard-delete tài khoản người dùng.</li>
 *     <li>Toàn bộ phương thức đọc sử dụng {@code readOnly = true} để tối ưu tài nguyên.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getUserById(Long id) {
        log.debug("Truy vấn thông tin người dùng theo ID: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));

        return mapToUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tên đăng nhập không được để trống");
        }

        String trimmedUsername = username.trim();
        log.debug("Truy vấn thông tin người dùng theo username: '{}'", trimmedUsername);

        User user = userRepository.findByUsername(trimmedUsername)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "username", trimmedUsername));

        return mapToUserResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Email không được để trống");
        }

        String trimmedEmail = email.trim();
        log.debug("Truy vấn thông tin người dùng theo email: '{}'", trimmedEmail);

        User user = userRepository.findByEmail(trimmedEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", trimmedEmail));

        return mapToUserResponse(user);
    }

    @Override
    public PagedResponse<UserResponse> getUsers(Boolean enabled, Pageable pageable) {
        Pageable effectivePageable = (pageable != null) ? pageable : PageRequest.of(0, 20);

        Page<User> userPage;
        if (enabled != null) {
            userPage = userRepository.findByEnabled(enabled, effectivePageable);
        } else {
            userPage = userRepository.findAll(effectivePageable);
        }

        List<UserResponse> items = userPage.getContent().stream()
            .map(this::mapToUserResponse)
            .toList();

        return PagedResponse.of(items, userPage);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Cập nhật thông tin hồ sơ cho người dùng ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));

        // Xử lý cập nhật Email nếu có truyền vào
        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim();

            // Chỉ kiểm tra trùng lặp nếu email mới khác với email hiện tại của người dùng
            if (!user.getEmail().equalsIgnoreCase(newEmail)) {
                userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        log.warn("Cập nhật hồ sơ thất bại: Email '{}' đã được sử dụng bởi người dùng khác", newEmail);
                        throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Email đã được sử dụng bởi tài khoản khác");
                    }
                });
                user.setEmail(newEmail);
                log.info("Đã cập nhật email mới cho người dùng ID: {}", userId);
            }
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        log.info("Cập nhật trạng thái tài khoản cho người dùng ID: {}, enabled: {}", userId, request.enabled());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));

        user.setEnabled(request.enabled());
        User updatedUser = userRepository.save(user);

        log.info("Cập nhật trạng thái kích hoạt tài khoản thành công cho người dùng ID: {}", userId);
        return mapToUserResponse(updatedUser);
    }

    /**
     * Ánh xạ an toàn từ thực thể {@link User} sang DTO {@link UserResponse}.
     * <p>
     * Tuyệt đối không để lộ mật khẩu, mã xác thực hay token nhạy cảm.
     */
    private UserResponse mapToUserResponse(User user) {
        Role role = user.getRole();
        RoleResponse roleResponse = (role != null)
            ? new RoleResponse(role.getId(), role.getName())
            : null;

        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getEnabled(),
            roleResponse,
            user.getCreatedAt()
        );
    }
}

