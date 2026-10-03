package com.ecommerce.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cấu hình mã hóa mật khẩu cho toàn bộ hệ thống E-Commerce.
 * <p>
 * Sử dụng thuật toán BCrypt với độ dài muối (strength / log rounds) là 12,
 * tuân thủ tuyệt đối quy định an toàn thông tin tại AGENTS.md và docs/business-rules.md.
 */
@Configuration
public class PasswordEncoderConfig {

    private static final int BCRYPT_STRENGTH = 12;

    /**
     * Khởi tạo Bean {@link PasswordEncoder} với chi phí băm BCrypt = 12.
     *
     * @return phiên bản {@link BCryptPasswordEncoder} an toàn
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}

