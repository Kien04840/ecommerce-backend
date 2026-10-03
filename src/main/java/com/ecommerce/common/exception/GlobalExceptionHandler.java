package com.ecommerce.common.exception;

import com.ecommerce.common.response.ApiErrorResponse;
import com.ecommerce.common.response.ValidationErrorDetail;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

/**
 * Bộ xử lý ngoại lệ tập trung toàn hệ thống (Global Exception Handler).
 * <p>
 * Đánh chặn toàn bộ các ngoại lệ phát sinh từ tầng Controller và Service,
 * chuyển đổi thành cấu trúc phản hồi lỗi {@link ApiErrorResponse} chuẩn mực
 * theo đặc tả tại docs/api.md.
 * <p>
 * Bảo đảm tuyệt đối không để lộ thông tin kỹ thuật nhạy cảm (stack trace, SQL query,
 * database structure) ra ngoài phía client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý các ngoại lệ nghiệp vụ phát sinh từ tầng Service.
     *
     * @param ex ngoại lệ {@link BusinessException}
     * @return phản hồi lỗi tương ứng với trạng thái HTTP của mã lỗi
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Ngoại lệ nghiệp vụ phát sinh [{}]: {}", errorCode.name(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(errorCode.name(), ex.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Xử lý lỗi kiểm tra tính hợp lệ dữ liệu đầu vào của Request Body (@Valid).
     * <p>
     * Trích xuất chi tiết từng trường bị vi phạm kèm giá trị bị từ chối
     * và thông điệp lỗi tiếng Việt thân thiện với người dùng.
     *
     * @param ex ngoại lệ {@link MethodArgumentNotValidException}
     * @return phản hồi HTTP 400 Bad Request kèm danh sách lỗi trường
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<ValidationErrorDetail> validationErrors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ValidationErrorDetail(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
            ));
        }

        log.warn("Kiểm tra dữ liệu đầu vào thất bại: {} lỗi vi phạm", validationErrors.size());

        ApiErrorResponse response = ApiErrorResponse.of(
            ErrorCode.VALIDATION_FAILED.name(),
            ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
            validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi vi phạm ràng buộc validation mức tham số (ConstraintViolationException).
     *
     * @param ex ngoại lệ {@link ConstraintViolationException}
     * @return phản hồi HTTP 400 Bad Request kèm danh sách lỗi
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        List<ValidationErrorDetail> validationErrors = ex.getConstraintViolations().stream()
            .map(violation -> new ValidationErrorDetail(
                violation.getPropertyPath().toString(),
                violation.getInvalidValue(),
                violation.getMessage()
            ))
            .toList();

        log.warn("Vi phạm ràng buộc dữ liệu: {} lỗi", validationErrors.size());

        ApiErrorResponse response = ApiErrorResponse.of(
            ErrorCode.VALIDATION_FAILED.name(),
            ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
            validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi sai định dạng JSON hoặc không thể đọc HTTP request body.
     *
     * @param ex ngoại lệ {@link HttpMessageNotReadableException}
     * @return phản hồi HTTP 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Dữ liệu HTTP request body không đọc được hoặc sai cú pháp JSON: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
            ErrorCode.BAD_REQUEST.name(),
            "Định dạng dữ liệu yêu cầu không hợp lệ hoặc cú pháp JSON sai"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi sai kiểu dữ liệu của tham số trên URL (Path variable hoặc Request parameter).
     *
     * @param ex ngoại lệ {@link MethodArgumentTypeMismatchException}
     * @return phản hồi HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Tham số '%s' có giá trị không hợp lệ: '%s'", ex.getName(), ex.getValue());
        log.warn("Sai kiểu dữ liệu tham số yêu cầu: {}", message);

        ApiErrorResponse response = ApiErrorResponse.of(ErrorCode.BAD_REQUEST.name(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi phương thức HTTP không được hỗ trợ trên endpoint gọi đến.
     *
     * @param ex ngoại lệ {@link HttpRequestMethodNotSupportedException}
     * @return phản hồi HTTP 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("Phương thức HTTP '%s' không được hỗ trợ cho đường dẫn này", ex.getMethod());
        log.warn("Gọi phương thức HTTP không hỗ trợ: {}", message);

        ApiErrorResponse response = ApiErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED.name(), message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Xử lý ngoại lệ truyền đối số không hợp lệ trong luồng xử lý.
     *
     * @param ex ngoại lệ {@link IllegalArgumentException}
     * @return phản hồi HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Tham số truyền vào không hợp lệ: {}", ex.getMessage());

        String message = ex.getMessage() != null ? ex.getMessage() : ErrorCode.BAD_REQUEST.getDefaultMessage();
        ApiErrorResponse response = ApiErrorResponse.of(ErrorCode.BAD_REQUEST.name(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý bắt toàn bộ các lỗi ngoại lệ chưa được phân loại khác (Fallback Handler).
     * <p>
     * Luôn ghi log chi tiết mức độ ERROR cùng stack trace hoàn chỉnh phía server
     * để phục vụ truy vết sự cố, nhưng chỉ trả về thông điệp chung chung an toàn cho client.
     *
     * @param ex ngoại lệ không mong muốn
     * @return phản hồi HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Lỗi hệ thống nội bộ không mong muốn: ", ex);

        ApiErrorResponse response = ApiErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR.name(),
            ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

