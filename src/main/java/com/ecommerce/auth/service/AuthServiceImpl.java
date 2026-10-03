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
import com.ecommerce.user.dto.RoleResponse;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Lớp triển khai các nghiệp vụ cho {@link AuthService}.
 * <p>
 * Đảm bảo các bất biến và ranh giới bảo mật:
 * <ul>
 *     <li>Bảo vệ thông tin chứng thực: Không lưu trữ hay ghi log mật khẩu dạng plaintext.</li>
 *     <li>Mã hóa mật khẩu bằng BCrypt với chi phí tính toán an toàn (rounds = 12).</li>
 *     <li>Mặc định tài khoản đăng ký mới được gán vai trò {@code ROLE_CUSTOMER} và trạng thái kích hoạt {@code enabled = true}.</li>
 *     <li>Hỗ trợ đăng nhập linh hoạt bằng cả tên đăng nhập (username) và địa chỉ email.</li>
 *     <li>Ngăn chặn đăng nhập vào các tài khoản đã bị vô hiệu hóa hoặc khóa tạm thời.</li>
 *     <li>Áp dụng cơ chế xoay vòng Refresh Token Rotation (RTR) an toàn.</li>
 *     <li>Thu hồi toàn bộ Refresh Token của tài khoản khi thay đổi mật khẩu thành công.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE_NAME = "ROLE_CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String trimmedUsername = request.username().trim();
        String trimmedEmail = request.email().trim();

        log.info("Bắt đầu xử lý đăng ký tài khoản mới cho username: '{}', email: '{}'", trimmedUsername, trimmedEmail);

        // 1. Kiểm tra tính duy nhất của tên đăng nhập
        if (userRepository.existsByUsername(trimmedUsername)) {
            log.warn("Đăng ký thất bại: Tên đăng nhập '{}' đã tồn tại", trimmedUsername);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Tên đăng nhập đã tồn tại trong hệ thống");
        }

        // 2. Kiểm tra tính duy nhất của email
        if (userRepository.existsByEmail(trimmedEmail)) {
            log.warn("Đăng ký thất bại: Email '{}' đã tồn tại", trimmedEmail);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Email đã tồn tại trong hệ thống");
        }

        // 3. Tra cứu vai trò khách hàng mặc định (ROLE_CUSTOMER)
        Role customerRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
            .orElseThrow(() -> new ResourceNotFoundException("Vai trò", "name", DEFAULT_ROLE_NAME));

        // 4. Mã hóa mật khẩu an toàn bằng BCrypt (không bao giờ lưu plaintext)
        String encodedPassword = passwordEncoder.encode(request.password());

        // 5. Khởi tạo và lưu trữ thực thể User mới
        User newUser = User.builder()
            .username(trimmedUsername)
            .email(trimmedEmail)
            .password(encodedPassword)
            .enabled(true)
            .role(customerRole)
            .build();

        User savedUser = userRepository.save(newUser);
        log.info("Đăng ký tài khoản thành công cho người dùng ID: {}, username: '{}'", savedUser.getId(), savedUser.getUsername());

        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        String loginIdentifier = request.username().trim();
        log.info("Xử lý yêu cầu đăng nhập cho tài khoản: '{}'", loginIdentifier);

        // 1. Tìm kiếm tài khoản theo username hoặc email
        Optional<User> userOpt;
        if (loginIdentifier.contains("@")) {
            userOpt = userRepository.findWithRoleByEmail(loginIdentifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsernameWithRole(loginIdentifier);
            }
        } else {
            userOpt = userRepository.findByUsernameWithRole(loginIdentifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findWithRoleByEmail(loginIdentifier);
            }
        }

        if (userOpt.isEmpty()) {
            log.warn("Đăng nhập thất bại: Không tìm thấy tài khoản '{}'", loginIdentifier);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        User user = userOpt.get();

        // 2. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Đăng nhập thất bại: Sai mật khẩu cho tài khoản '{}'", loginIdentifier);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        // 3. Kiểm tra trạng thái tài khoản
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("Đăng nhập thất bại: Tài khoản '{}' đã bị vô hiệu hóa", loginIdentifier);
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt");
        }

        // 4. Cấp phát Access Token (JWT - 15 phút) và Refresh Token (7 ngày)
        String roleName = (user.getRole() != null) ? user.getRole().getName() : DEFAULT_ROLE_NAME;
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleName);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Đăng nhập thành công cho người dùng ID: {}, cấp phát cặp token mới", user.getId());

        return TokenResponse.of(accessToken, refreshToken.getToken(), jwtService.getAccessTokenValiditySeconds());
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Xử lý yêu cầu làm mới phiên đăng nhập (RTR)");

        // 1. Thực hiện xoay vòng Refresh Token (hủy token cũ, tạo token mới)
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(request.refreshToken());
        User user = newRefreshToken.getUser();

        // 2. Cấp phát Access Token mới
        String roleName = (user.getRole() != null) ? user.getRole().getName() : DEFAULT_ROLE_NAME;
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleName);

        log.info("Xoay vòng token thành công cho người dùng ID: {}", user.getId());

        return TokenResponse.of(newAccessToken, newRefreshToken.getToken(), jwtService.getAccessTokenValiditySeconds());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.debug("Xử lý yêu cầu đăng xuất");
        refreshTokenService.revokeToken(refreshToken);
        log.info("Đăng xuất thành công, Refresh Token đã được thu hồi");
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Xử lý yêu cầu đổi mật khẩu cho người dùng ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));

        // 1. Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("Đổi mật khẩu thất bại cho User ID {}: Mật khẩu hiện tại không khớp", userId);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mật khẩu hiện tại không chính xác");
        }

        // 2. Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            log.warn("Đổi mật khẩu thất bại cho User ID {}: Mật khẩu mới trùng với mật khẩu cũ", userId);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        // 3. Mã hóa và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Đã cập nhật mật khẩu mới thành công cho người dùng ID: {}", userId);

        // 4. Thu hồi toàn bộ Refresh Token của tài khoản để đăng xuất khỏi mọi thiết bị
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("Đã thu hồi toàn bộ phiên đăng nhập cũ của người dùng ID: {} sau khi đổi mật khẩu", userId);
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

