package com.ecommerce.common.security;

import com.ecommerce.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller phục vụ riêng cho việc kiểm thử bảo mật (Security Integration Tests).
 * <p>
 * Nằm trong thư mục {@code src/test/java}, chỉ tồn tại trong vòng đời test và tuyệt đối không
 * bị đóng gói vào sản phẩm production hay làm thay đổi API contract chính thức.
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestSecurityController {

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<String>> publicEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Endpoint công khai", "Dữ liệu công khai"));
    }

    @GetMapping("/authenticated")
    public ResponseEntity<ApiResponse<String>> authenticatedEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Endpoint yêu cầu đăng nhập", "Dữ liệu bảo mật"));
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> customerOnlyEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Chỉ dành cho Customer", "Dữ liệu khách hàng"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminOnlyEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Chỉ dành cho Admin", "Dữ liệu quản trị"));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<String>> staffOnlyEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Chỉ dành cho Staff", "Dữ liệu nhân viên"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<String>> identityEndpoint() {
        Long userId = SecurityUtils.getCurrentUserId().orElse(-1L);
        String role = SecurityUtils.getCurrentUserRole().orElse("NONE");
        return ResponseEntity.ok(ApiResponse.success("Định danh thành công", "User ID: " + userId + ", Role: " + role));
    }
}
