package com.ecommerce.common.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử khả năng tuần tự hóa JSON (JSON Serialization) của các đối tượng phản hồi dùng chung.
 * <p>
 * Đảm bảo định dạng JSON xuất ra tuân thủ chính xác các ví dụ tại docs/api.md.
 */
class ResponseSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("ApiResponse - Tuần tự hóa JSON thành công với cấu trúc {success, message, data}")
    void shouldSerializeApiResponseCorrectly() throws Exception {
        record SampleData(Long id, String name) {}
        SampleData data = new SampleData(1L, "Sản phẩm A");
        ApiResponse<SampleData> response = ApiResponse.success("Lấy thông tin thành công", data);

        String json = objectMapper.writeValueAsString(response);
        JsonNode rootNode = objectMapper.readTree(json);

        assertThat(rootNode.get("success").asBoolean()).isTrue();
        assertThat(rootNode.get("message").asText()).isEqualTo("Lấy thông tin thành công");
        assertThat(rootNode.get("data").get("id").asLong()).isEqualTo(1L);
        assertThat(rootNode.get("data").get("name").asText()).isEqualTo("Sản phẩm A");
    }

    @Test
    @DisplayName("PagedResponse - Tuần tự hóa JSON chuẩn khớp với docs/api.md Mục 4.2")
    void shouldSerializePagedResponseCorrectly() throws Exception {
        List<String> items = List.of("Item 1", "Item 2");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 20), 142);

        PagedResponse<String> pagedResponse = PagedResponse.from(page);
        String json = objectMapper.writeValueAsString(pagedResponse);
        JsonNode rootNode = objectMapper.readTree(json);

        assertThat(rootNode.get("items")).hasSize(2);
        JsonNode paginationNode = rootNode.get("pagination");
        assertThat(paginationNode.get("page").asInt()).isEqualTo(0);
        assertThat(paginationNode.get("size").asInt()).isEqualTo(20);
        assertThat(paginationNode.get("totalElements").asLong()).isEqualTo(142L);
        assertThat(paginationNode.get("totalPages").asInt()).isEqualTo(8);
        assertThat(paginationNode.get("isFirst").asBoolean()).isTrue();
        assertThat(paginationNode.get("isLast").asBoolean()).isFalse();
        assertThat(paginationNode.get("hasNext").asBoolean()).isTrue();
        assertThat(paginationNode.get("hasPrevious").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("ApiErrorResponse - Không hiển thị trường 'errors' khi không có lỗi validation")
    void shouldOmitErrorsFieldWhenNull() throws Exception {
        ApiErrorResponse errorResponse = ApiErrorResponse.of("RESOURCE_NOT_FOUND", "Không tìm thấy tài nguyên");

        String json = objectMapper.writeValueAsString(errorResponse);
        JsonNode rootNode = objectMapper.readTree(json);

        assertThat(rootNode.get("success").asBoolean()).isFalse();
        assertThat(rootNode.get("code").asText()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(rootNode.get("message").asText()).isEqualTo("Không tìm thấy tài nguyên");
        assertThat(rootNode.has("errors")).isFalse();
    }

    @Test
    @DisplayName("ApiErrorResponse - Hiển thị đầy đủ trường 'errors' khi có lỗi validation")
    void shouldIncludeErrorsFieldWhenPresent() throws Exception {
        List<ValidationErrorDetail> errorDetails = List.of(
            new ValidationErrorDetail("price", -50000, "Giá sản phẩm phải lớn hơn 0")
        );
        ApiErrorResponse errorResponse = ApiErrorResponse.of("VALIDATION_FAILED", "Dữ liệu đầu vào không hợp lệ", errorDetails);

        String json = objectMapper.writeValueAsString(errorResponse);
        JsonNode rootNode = objectMapper.readTree(json);

        assertThat(rootNode.get("success").asBoolean()).isFalse();
        assertThat(rootNode.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(rootNode.get("errors")).hasSize(1);
        assertThat(rootNode.get("errors").get(0).get("field").asText()).isEqualTo("price");
        assertThat(rootNode.get("errors").get(0).get("rejectedValue").asInt()).isEqualTo(-50000);
        assertThat(rootNode.get("errors").get(0).get("message").asText()).isEqualTo("Giá sản phẩm phải lớn hơn 0");
    }
}

