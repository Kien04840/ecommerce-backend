package com.ecommerce.common.security;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service tích hợp với Spring Security để tải dữ liệu danh tính người dùng từ cơ sở dữ liệu.
 * <p>
 * Triển khai {@link UserDetailsService} phục vụ cơ chế xác thực thông qua {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}.
 * Hỗ trợ tra cứu linh hoạt bằng cả tên đăng nhập (username) và địa chỉ email.
 * <p>
 * Nạp sẵn (eager fetch) vai trò phân quyền {@link com.ecommerce.user.entity.Role} để triệt tiêu lỗi N+1 Query.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new UsernameNotFoundException("Tên đăng nhập hoặc email không được để trống");
        }

        String identifier = usernameOrEmail.trim();
        Optional<User> userOpt;

        if (identifier.contains("@")) {
            userOpt = userRepository.findWithRoleByEmail(identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsernameWithRole(identifier);
            }
        } else {
            userOpt = userRepository.findByUsernameWithRole(identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findWithRoleByEmail(identifier);
            }
        }

        User user = userOpt.orElseThrow(() -> {
            log.warn("Không tìm thấy người dùng trong hệ thống với định danh: '{}'", identifier);
            return new UsernameNotFoundException("Không tìm thấy người dùng với thông tin đăng nhập đã cung cấp");
        });

        return UserPrincipal.fromUser(user);
    }
}
