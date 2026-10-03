package com.ecommerce.common.exception;

import com.ecommerce.common.response.ApiErrorResponse;
import com.ecommerce.common.response.ValidationErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị cho {@link GlobalExceptionHandler}.
 * <p>
 * Kiểm tra các tình huống xử lý ngoại lệ: BusinessException, ResourceNotFoundException,
 * ValidationException, IllegalArgumentException và Exception không mong muốn.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleBusinessException - Trả về HTTP status và mã lỗi tương ứng của ErrorCode")
    void shouldHandleBusinessExceptionCorrectly() {
        BusinessException exception = new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Email đã được sử dụng");

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleBusinessException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().success()).isFalse();
        assertThat(responseEntity.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(responseEntity.getBody().message()).isEqualTo("Email đã được sử dụng");
        assertThat(responseEntity.getBody().errors()).isNull();
    }

    @Test
    @DisplayName("handleResourceNotFoundException - Trả về HTTP 404 và mã lỗi RESOURCE_NOT_FOUND")
    void shouldHandleResourceNotFoundExceptionCorrectly() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Sản phẩm", "id", 100L);

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleBusinessException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().success()).isFalse();
        assertThat(responseEntity.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(responseEntity.getBody().message()).isEqualTo("Không tìm thấy Sản phẩm với id: '100'");
    }

    @Test
    @DisplayName("handleValidationException - Trích xuất đầy đủ danh sách lỗi trường và trả về HTTP 400")
    void shouldHandleMethodArgumentNotValidExceptionCorrectly() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("productRequest", "name", "", false, null, null, "Tên không được để trống");
        FieldError fieldError2 = new FieldError("productRequest", "price", -10, false, null, null, "Giá phải lớn hơn 0");

        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleValidationException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().success()).isFalse();
        assertThat(responseEntity.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(responseEntity.getBody().message()).isEqualTo("Dữ liệu đầu vào không hợp lệ");
        assertThat(responseEntity.getBody().errors()).hasSize(2);

        ValidationErrorDetail errorDetail1 = responseEntity.getBody().errors().get(0);
        assertThat(errorDetail1.field()).isEqualTo("name");
        assertThat(errorDetail1.rejectedValue()).isEqualTo("");
        assertThat(errorDetail1.message()).isEqualTo("Tên không được để trống");

        ValidationErrorDetail errorDetail2 = responseEntity.getBody().errors().get(1);
        assertThat(errorDetail2.field()).isEqualTo("price");
        assertThat(errorDetail2.rejectedValue()).isEqualTo(-10);
        assertThat(errorDetail2.message()).isEqualTo("Giá phải lớn hơn 0");
    }

    @Test
    @DisplayName("handleIllegalArgumentException - Trả về HTTP 400 BAD_REQUEST kèm thông điệp")
    void shouldHandleIllegalArgumentExceptionCorrectly() {
        IllegalArgumentException exception = new IllegalArgumentException("Tham số trang không được âm");

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleIllegalArgumentException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().success()).isFalse();
        assertThat(responseEntity.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(responseEntity.getBody().message()).isEqualTo("Tham số trang không được âm");
    }

    @Test
    @DisplayName("handleHttpMessageNotReadableException - Trả về HTTP 400 khi JSON sai định dạng")
    void shouldHandleHttpMessageNotReadableExceptionCorrectly() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleHttpMessageNotReadableException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().success()).isFalse();
        assertThat(responseEntity.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(responseEntity.getBody().message()).contains("Định dạng dữ liệu yêu cầu không hợp lệ hoặc cú pháp JSON sai");
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatchException - Trả về HTTP 400 khi sai kiểu tham số URL")
    void shouldHandleMethodArgumentTypeMismatchExceptionCorrectly() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");
        when(exception.getValue()).thenReturn("abc");

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleMethodArgumentTypeMismatchException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(responseEntity.getBody().message()).contains("Tham số 'id' có giá trị không hợp lệ: 'abc'");
    }

    @Test
    @DisplayName("handleHttpRequestMethodNotSupportedException - Trả về HTTP 405 METHOD_NOT_ALLOWED")
    void shouldHandleHttpRequestMethodNotSupportedExceptionCorrectly() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleHttpRequestMethodNotSupportedException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(responseEntity.getBody().message()).contains("Phương thức HTTP 'DELETE' không được hỗ trợ");
    }

    @Test
    @DisplayName("handleGenericException - Trả về HTTP 500 và thông điệp an toàn, không leak stack trace")
    void shouldHandleGenericExceptionSafelyWithoutLeakingInternalDetails() {
        Exception exception = new RuntimeException("SQL syntax error near 'SELECT * FROM secret_table'");

        ResponseEntity<ApiErrorResponse> responseEntity = exceptionHandler.handleGenericException(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().success()).isFalse();
        assertThat(responseEntity.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(responseEntity.getBody().message()).isEqualTo("Đã xảy ra lỗi hệ thống nội bộ, vui lòng thử lại sau");
        assertThat(responseEntity.getBody().message()).doesNotContain("secret_table");
        assertThat(responseEntity.getBody().message()).doesNotContain("SQL");
    }
}
