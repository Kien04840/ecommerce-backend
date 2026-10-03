package com.ecommerce.common.validation;

import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.UpdateStockRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử tính đúng đắn của các ràng buộc Jakarta Bean Validation trên các DTO Request.
 * <p>
 * Đảm bảo các thông điệp vi phạm validation hiển thị bằng tiếng Việt chuẩn có dấu
 * và các quy tắc nghiệp vụ bất biến (giá > 0, tồn kho >= 0,...) được chặn ngay tại DTO.
 */
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("RegisterRequest - Phát hiện lỗi khi thông tin đăng ký không hợp lệ")
    void shouldValidateRegisterRequestFailures() {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(4); // username blank, username size, invalid email, password size
        List<String> messages = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(messages).contains("Định dạng email không hợp lệ");
        assertThat(messages).contains("Mật khẩu phải có độ dài từ 8 đến 100 ký tự");
    }

    @Test
    @DisplayName("RegisterRequest - Hợp lệ khi cung cấp đầy đủ thông tin đúng quy chuẩn")
    void shouldValidateRegisterRequestSuccess() {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "SecurePassword123!");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CreateProductRequest - Chặn giá bán <= 0 và số lượng tồn kho âm")
    void shouldRejectInvalidProductPriceAndStock() {
        CreateProductRequest request = new CreateProductRequest(
            null,
            "",
            "Mô tả",
            BigDecimal.ZERO, // Giá = 0 vi phạm @DecimalMin("0.01")
            -5,              // Tồn kho âm vi phạm @Min(0)
            null,
            null
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);

        List<String> messages = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(messages).contains("Danh mục sản phẩm không được để trống");
        assertThat(messages).contains("Tên sản phẩm không được để trống");
        assertThat(messages).contains("Giá sản phẩm phải lớn hơn 0");
        assertThat(messages).contains("Số lượng tồn kho không được âm");
    }

    @Test
    @DisplayName("CreateProductRequest - Hợp lệ khi giá > 0 và tồn kho >= 0")
    void shouldAcceptValidProductCreation() {
        CreateProductRequest request = new CreateProductRequest(
            1L,
            "Bàn phím cơ",
            "Mô tả chi tiết",
            new BigDecimal("1500000.00"),
            10,
            "http://example.com/img.png",
            Set.of("gaming", "keyboard")
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CreateOrderRequest - Chặn đơn hàng không có sản phẩm hoặc số lượng = 0")
    void shouldRejectOrderWithEmptyItemsOrZeroQuantity() {
        CreateOrderRequest emptyOrderRequest = new CreateOrderRequest(Collections.emptyList());
        Set<ConstraintViolation<CreateOrderRequest>> emptyViolations = validator.validate(emptyOrderRequest);
        assertThat(emptyViolations).hasSize(1);
        assertThat(emptyViolations.iterator().next().getMessage()).isEqualTo("Đơn hàng phải có ít nhất một mặt hàng");

        CreateOrderRequest invalidItemOrder = new CreateOrderRequest(List.of(
            new OrderItemRequest(null, 0)
        ));
        Set<ConstraintViolation<CreateOrderRequest>> itemViolations = validator.validate(invalidItemOrder);
        List<String> messages = itemViolations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(messages).contains("Mã sản phẩm không được để trống");
        assertThat(messages).contains("Số lượng mua tối thiểu là 1");
    }

    @Test
    @DisplayName("UpdateStockRequest - Chặn điều chỉnh số lượng tồn kho âm")
    void shouldRejectNegativeStockUpdate() {
        UpdateStockRequest request = new UpdateStockRequest(-10);

        Set<ConstraintViolation<UpdateStockRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Số lượng tồn kho không được âm");
    }
}

