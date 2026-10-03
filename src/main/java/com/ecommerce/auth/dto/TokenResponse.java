package com.ecommerce.auth.dto;

/**
 * DTO phản hồi chứa cặp Access Token và Refresh Token sau khi xác thực thành công.
 *
 * @param accessToken  Chuỗi JWT Access Token (hiệu lực 15 phút)
 * @param refreshToken Chuỗi Refresh Token ngẫu nhiên (hiệu lực 7 ngày)
 * @param tokenType    Loại mã xác thực (mặc định là "Bearer")
 * @param expiresIn    Thời hạn hiệu lực của Access Token tính bằng giây (900s)
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
    /**
     * Khởi tạo đối tượng phản hồi token với tiền tố mặc định Bearer.
     *
     * @param accessToken  chuỗi JWT Access Token
     * @param refreshToken chuỗi Refresh Token
     * @param expiresIn    thời hạn tính bằng giây
     * @return đối tượng {@link TokenResponse}
     */
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}

