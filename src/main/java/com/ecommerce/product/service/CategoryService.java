package com.ecommerce.product.service;

import com.ecommerce.product.dto.CategoryRequest;
import com.ecommerce.product.dto.CategoryResponse;

import java.util.List;

/**
 * Service interface định nghĩa các nghiệp vụ quản lý Danh mục sản phẩm (Category).
 * <p>
 * Cung cấp các thao tác CRUD danh mục phục vụ phân loại sản phẩm trên sàn thương mại điện tử.
 */
public interface CategoryService {

    /**
     * Tạo mới một danh mục sản phẩm.
     *
     * @param request dữ liệu yêu cầu tạo mới danh mục
     * @return thông tin chi tiết danh mục vừa tạo
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Lấy danh sách toàn bộ danh mục sản phẩm trong hệ thống.
     *
     * @return danh sách các danh mục sản phẩm
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Lấy thông tin chi tiết một danh mục sản phẩm theo mã định danh.
     *
     * @param id mã định danh danh mục
     * @return thông tin chi tiết danh mục
     */
    CategoryResponse getCategoryById(Long id);

    /**
     * Cập nhật thông tin chi tiết một danh mục sản phẩm.
     *
     * @param id      mã định danh danh mục cần cập nhật
     * @param request dữ liệu yêu cầu cập nhật
     * @return thông tin danh mục sau khi cập nhật thành công
     */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /**
     * Xóa một danh mục sản phẩm (chỉ cho phép khi danh mục không chứa bất kỳ sản phẩm nào).
     *
     * @param id mã định danh danh mục cần xóa
     */
    void deleteCategory(Long id);
}
