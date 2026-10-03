package com.ecommerce.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình bảo mật trung tâm Spring Security cho hệ thống E-Commerce Backend.
 * <p>
 * Áp dụng các nguyên tắc kiến trúc và tiêu chuẩn bảo mật nghiêm ngặt:
 * <ul>
 *     <li><b>Xác thực Phi trạng thái (Stateless Authentication)</b>: Vô hiệu hóa HTTP Session, chỉ sử dụng JWT Access Token.</li>
 *     <li><b>Vô hiệu hóa CSRF có chủ đích</b>: Backend là REST API thuần túy, mọi yêu cầu xác thực truyền qua header {@code Authorization: Bearer <token>} và không phụ thuộc vào Cookie session trên trình duyệt.</li>
 *     <li><b>Cấu hình CORS an toàn</b>: Cho phép danh sách nguồn (Origins) được chỉ định trong cấu hình, không sử dụng ký tự đại diện wildcard khi bật credentials.</li>
 *     <li><b>Phân quyền theo vai trò (RBAC)</b>: Thiết lập ranh giới quyền hạn theo nguyên tắc đặc quyền tối thiểu (Principle of Least Privilege).</li>
 *     <li><b>Bật Method Security</b>: Cho phép sử dụng các chú thích {@code @PreAuthorize} trên các method Service hoặc Controller khi cần.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private List<String> allowedOrigins;

    /**
     * Cấu hình chuỗi lọc bảo mật Spring Security (Security Filter Chain).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Tắt CSRF: Do hệ thống là REST API phi trạng thái (Stateless), sử dụng token JWT qua Authorization Bearer header
            // và không sử dụng Cookie session phía trình duyệt nên hoàn toàn miễn nhiễm với tấn công CSRF.
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Cấu hình CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. Chính sách quản lý phiên: Hoàn toàn phi trạng thái (Stateless), không tạo HttpSession
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. Xử lý ngoại lệ phân quyền và xác thực: Trả về JSON ApiErrorResponse chuẩn
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )

            // 5. Cấu hình quy tắc kiểm soát truy cập (Authorization Rules)
            .authorizeHttpRequests(auth -> auth
                // Endpoint công khai của module Auth
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh"
                ).permitAll()

                // Endpoint đọc công khai của module Catalog (Sản phẩm & Danh mục)
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()

                // Endpoint thao tác ghi Danh mục: Chỉ dành cho Quản trị viên (ROLE_ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")

                // Webhook tiếp nhận thông báo từ đối tác giao vận
                .requestMatchers("/api/v1/shipments/webhook").permitAll()

                // Endpoint kiểm tra công khai phục vụ kiểm thử bảo mật
                .requestMatchers("/api/v1/test/public").permitAll()

                // Endpoint quản trị người dùng: Chỉ dành cho Quản trị viên (ROLE_ADMIN)
                .requestMatchers("/api/v1/admin/users/**").hasRole("ADMIN")

                // Cập nhật tồn kho sản phẩm: Dành cho Quản trị viên và Nhân viên (ROLE_ADMIN, ROLE_STAFF)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/products/*/stock").hasAnyRole("ADMIN", "STAFF")
                // Các thao tác quản trị sản phẩm khác (tạo, cập nhật, xóa mềm): Chỉ dành cho Quản trị viên
                .requestMatchers("/api/v1/admin/products/**").hasRole("ADMIN")

                // Endpoint quản trị đơn hàng và vận chuyển: Dành cho Quản trị viên và Nhân viên (ROLE_ADMIN, ROLE_STAFF)
                .requestMatchers("/api/v1/admin/orders/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/v1/admin/shipments/**").hasAnyRole("ADMIN", "STAFF")

                // Tất cả các endpoint quản trị /admin còn lại: Mặc định yêu cầu quyền ADMIN
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Tất cả các endpoint nghiệp vụ khác: Yêu cầu người dùng phải được xác thực danh tính
                .anyRequest().authenticated()
            )

            // 6. Cấu hình AuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // 7. Đặt JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Khởi tạo Bean {@link AuthenticationProvider} sử dụng {@link DaoAuthenticationProvider}
     * kết hợp với {@link CustomUserDetailsService} và {@link PasswordEncoder} an toàn.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Khởi tạo Bean {@link AuthenticationManager} từ cấu hình của Spring Security.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Cấu hình CORS (Cross-Origin Resource Sharing) chuẩn mực cho REST API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
