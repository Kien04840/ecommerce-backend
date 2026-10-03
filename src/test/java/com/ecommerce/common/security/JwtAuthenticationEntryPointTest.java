package com.ecommerce.common.security;

import com.ecommerce.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử JwtAuthenticationEntryPoint")
class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    @Test
    @DisplayName("commence: trả về HTTP 401 Unauthorized kèm body JSON ApiErrorResponse chuẩn")
    void commence_ShouldReturn401StatusAndJsonErrorResponse() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Token invalid or expired"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertFalse(jsonNode.get("success").asBoolean());
        assertEquals(ErrorCode.UNAUTHORIZED.name(), jsonNode.get("code").asText());
        assertEquals(ErrorCode.UNAUTHORIZED.getDefaultMessage(), jsonNode.get("message").asText());
    }
}
