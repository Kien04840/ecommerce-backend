package com.ecommerce.common.security;

import com.ecommerce.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử CustomAccessDeniedHandler")
class CustomAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomAccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler();

    @Test
    @DisplayName("handle: trả về HTTP 403 Forbidden kèm body JSON ApiErrorResponse chuẩn")
    void handle_ShouldReturn403StatusAndJsonErrorResponse() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access is denied"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertFalse(jsonNode.get("success").asBoolean());
        assertEquals(ErrorCode.FORBIDDEN.name(), jsonNode.get("code").asText());
        assertEquals(ErrorCode.FORBIDDEN.getDefaultMessage(), jsonNode.get("message").asText());
    }
}
