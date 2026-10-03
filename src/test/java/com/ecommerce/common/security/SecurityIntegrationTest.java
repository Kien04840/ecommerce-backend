package com.ecommerce.common.security;

import com.ecommerce.auth.service.JwtService;
import com.ecommerce.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("Kiểm thử tích hợp Spring Security, JWT và Phân quyền RBAC")
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("Endpoint công khai: truy cập không cần token trả về HTTP 200 OK")
    void publicEndpoint_WithoutToken_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/test/public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is("Dữ liệu công khai")));
    }

    @Test
    @DisplayName("Endpoint yêu cầu xác thực: truy cập không có token trả về HTTP 401 Unauthorized kèm JSON ApiErrorResponse")
    void protectedEndpoint_WithoutToken_ShouldReturn401JsonError() throws Exception {
        mockMvc.perform(get("/api/v1/test/authenticated"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is(ErrorCode.UNAUTHORIZED.name())))
            .andExpect(jsonPath("$.message", is(ErrorCode.UNAUTHORIZED.getDefaultMessage())));
    }

    @Test
    @DisplayName("Endpoint yêu cầu xác thực: token sai định dạng/chữ ký giả mạo trả về HTTP 401 Unauthorized")
    void protectedEndpoint_WithInvalidToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/test/authenticated")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.signature.token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is(ErrorCode.UNAUTHORIZED.name())));
    }

    @Test
    @DisplayName("Endpoint yêu cầu xác thực: token hợp lệ trả về HTTP 200 OK")
    void protectedEndpoint_WithValidToken_ShouldReturn200() throws Exception {
        String token = jwtService.generateAccessToken(100L, "customer@example.com", "ROLE_CUSTOMER");

        mockMvc.perform(get("/api/v1/test/authenticated")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is("Dữ liệu bảo mật")));
    }

    @Test
    @DisplayName("RBAC: Khách hàng (ROLE_CUSTOMER) truy cập endpoint Admin trả về HTTP 403 Forbidden kèm JSON ApiErrorResponse")
    void adminEndpoint_WithCustomerToken_ShouldReturn403Forbidden() throws Exception {
        String customerToken = jwtService.generateAccessToken(101L, "customer@example.com", "ROLE_CUSTOMER");

        // Kiểm tra qua URL pattern quy định trong SecurityConfig
        mockMvc.perform(get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
            .andExpect(status().isForbidden())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is(ErrorCode.FORBIDDEN.name())))
            .andExpect(jsonPath("$.message", is(ErrorCode.FORBIDDEN.getDefaultMessage())));
    }

    @Test
    @DisplayName("RBAC Method Security: Khách hàng (ROLE_CUSTOMER) gọi endpoint @PreAuthorize('hasRole(ADMIN)') trả về HTTP 403")
    void methodSecurity_WithCustomerTokenOnAdminEndpoint_ShouldReturn403() throws Exception {
        String customerToken = jwtService.generateAccessToken(102L, "customer@example.com", "ROLE_CUSTOMER");

        mockMvc.perform(get("/api/v1/test/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is(ErrorCode.FORBIDDEN.name())));
    }

    @Test
    @DisplayName("RBAC Method Security: Quản trị viên (ROLE_ADMIN) truy cập endpoint Admin thành công trả về HTTP 200 OK")
    void methodSecurity_WithAdminTokenOnAdminEndpoint_ShouldReturn200() throws Exception {
        String adminToken = jwtService.generateAccessToken(1L, "admin@example.com", "ROLE_ADMIN");

        mockMvc.perform(get("/api/v1/test/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is("Dữ liệu quản trị")));
    }

    @Test
    @DisplayName("RBAC Method Security: Nhân viên (ROLE_STAFF) truy cập endpoint Staff thành công")
    void methodSecurity_WithStaffTokenOnStaffEndpoint_ShouldReturn200() throws Exception {
        String staffToken = jwtService.generateAccessToken(200L, "staff@example.com", "ROLE_STAFF");

        mockMvc.perform(get("/api/v1/test/staff")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is("Dữ liệu nhân viên")));
    }

    @Test
    @DisplayName("RBAC Method Security: Nhân viên (ROLE_STAFF) truy cập endpoint Admin-only bị từ chối 403 Forbidden")
    void methodSecurity_WithStaffTokenOnAdminOnlyEndpoint_ShouldReturn403() throws Exception {
        String staffToken = jwtService.generateAccessToken(201L, "staff@example.com", "ROLE_STAFF");

        mockMvc.perform(get("/api/v1/test/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is(ErrorCode.FORBIDDEN.name())));
    }

    @Test
    @DisplayName("Trích xuất danh tính: SecurityUtils lấy chính xác User ID và Role từ JWT trong SecurityContextHolder")
    void identityEndpoint_WithValidToken_ShouldExtractCorrectPrincipal() throws Exception {
        String token = jwtService.generateAccessToken(888L, "shopper888@example.com", "ROLE_CUSTOMER");

        mockMvc.perform(get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is("User ID: 888, Role: ROLE_CUSTOMER")));
    }

    @Test
    @DisplayName("CORS Preflight: Request OPTIONS từ Origin được cấu hình trả về 200 OK và header Access-Control-Allow-Origin")
    void corsPreflight_WithAllowedOrigin_ShouldReturnOkAndHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/test/public")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
