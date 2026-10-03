package com.ecommerce.product.exception;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;

/**
 * Ngoại lệ ném ra khi số lượng tồn kho của sản phẩm không đủ để đáp ứng yêu cầu đặt hàng.
 * <p>
 * Kế thừa {@link BusinessException} và mặc định mang mã lỗi {@link ErrorCode#BAD_REQUEST}.
 */
public class InsufficientStockException extends BusinessException {

    /**
     * Khởi tạo ngoại lệ với thông điệp cụ thể.
     *
     * @param message thông điệp mô tả chi tiết lỗi tồn kho bằng tiếng Việt
     */
    public InsufficientStockException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    /**
     * Khởi tạo ngoại lệ với chi tiết mã sản phẩm, số lượng yêu cầu và số lượng tồn kho hiện tại.
     *
     * @param productId         mã định danh sản phẩm
     * @param requestedQuantity số lượng khách hàng yêu cầu mua
     * @param currentStock      số lượng tồn kho thực tế còn lại
     */
    public InsufficientStockException(Long productId, int requestedQuantity, int currentStock) {
        super(ErrorCode.BAD_REQUEST,
            String.format("Số lượng sản phẩm trong kho không đủ để đáp ứng (Mã sản phẩm: %d, yêu cầu: %d, tồn kho hiện tại: %d)",
                productId, requestedQuantity, currentStock));
    }
}
