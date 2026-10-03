package com.ecommerce.product.service;

import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSearchCriteria;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.product.dto.UpdateStockRequest;
import com.ecommerce.product.entity.ProductStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface định nghĩa các nghiệp vụ cốt lõi cho module Sản phẩm (Product).
 * <p>
 * Cung cấp các thao tác quản lý vòng đời sản phẩm, tìm kiếm lọc đa tiêu chí,
 * cập nhật kho hàng và các phương thức giao tiếp liên module (Inter-module Communication)
 * phục vụ quy trình Checkout/Order.
 */
public interface ProductService {

    /**
     * Tạo mới một sản phẩm trong hệ thống kèm hình ảnh và thẻ phân loại ban đầu.
     *
     * @param request dữ liệu yêu cầu tạo mới sản phẩm
     * @return thông tin chi tiết của sản phẩm vừa được tạo
     */
    ProductResponse createProduct(CreateProductRequest request);

    /**
     * Lấy thông tin chi tiết đầy đủ của sản phẩm theo mã định danh.
     *
     * @param id mã định danh sản phẩm
     * @return thông tin chi tiết sản phẩm kèm danh mục, hình ảnh và thẻ phân loại
     */
    ProductResponse getProductById(Long id);

    /**
     * Tìm kiếm và lọc sản phẩm đa điều kiện kết hợp phân trang.
     *
     * @param criteria tiêu chí tìm kiếm và bộ lọc (từ khóa, danh mục, trạng thái, khoảng giá)
     * @param pageable thông tin phân trang và sắp xếp kết quả
     * @return danh sách sản phẩm phân trang kèm siêu dữ liệu (metadata)
     */
    PagedResponse<ProductResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable);

    /**
     * Cập nhật thông tin chi tiết của sản phẩm.
     *
     * @param id      mã định danh sản phẩm cần cập nhật
     * @param request dữ liệu yêu cầu cập nhật thông tin sản phẩm
     * @return thông tin sản phẩm sau khi cập nhật thành công
     */
    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    /**
     * Điều chỉnh số lượng tồn kho của một sản phẩm.
     *
     * @param id      mã định danh sản phẩm
     * @param request dữ liệu yêu cầu cập nhật tồn kho mới
     * @return thông tin sản phẩm với số lượng tồn kho đã được cập nhật
     */
    ProductResponse updateStock(Long id, UpdateStockRequest request);

    /**
     * Thay đổi trạng thái kinh doanh của sản phẩm (ACTIVE, INACTIVE, OUT_OF_STOCK).
     *
     * @param id     mã định danh sản phẩm
     * @param status trạng thái kinh doanh mới cần chuyển đổi
     * @return thông tin sản phẩm với trạng thái mới
     */
    ProductResponse changeProductStatus(Long id, ProductStatus status);

    /**
     * Xóa mềm sản phẩm khỏi hệ thống (chuyển trạng thái sang INACTIVE).
     *
     * @param id mã định danh sản phẩm cần xóa mềm
     */
    void deleteProduct(Long id);

    /**
     * Kiểm tra điều kiện sản phẩm có được phép mua hay không (dùng cho quy trình Đặt hàng).
     * <p>
     * Điều kiện mua: Sản phẩm tồn tại, trạng thái là ACTIVE, số lượng mua hợp lệ (&gt; 0)
     * và số lượng tồn kho hiện tại đủ đáp ứng.
     *
     * @param productId mã định danh sản phẩm
     * @param quantity  số lượng khách hàng dự định mua
     * @return {@code true} nếu sản phẩm đủ điều kiện đặt mua, ngược lại {@code false}
     */
    boolean isProductPurchasable(Long productId, int quantity);

    /**
     * Trừ tồn kho sản phẩm một cách nguyên tử phục vụ quy trình Checkout tạo đơn hàng.
     *
     * @param productId mã định danh sản phẩm cần trừ kho
     * @param quantity  số lượng sản phẩm cần trừ (bắt buộc &gt; 0)
     */
    void deductStock(Long productId, int quantity);

    /**
     * Cộng hoàn trả lại số lượng tồn kho khi đơn hàng bị hủy bỏ.
     *
     * @param productId mã định danh sản phẩm cần hoàn trả kho
     * @param quantity  số lượng sản phẩm cần hoàn trả (bắt buộc &gt; 0)
     */
    void restoreStock(Long productId, int quantity);
}
