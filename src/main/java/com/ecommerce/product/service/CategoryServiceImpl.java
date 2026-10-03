package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.CategoryRequest;
import com.ecommerce.product.dto.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lớp triển khai các nghiệp vụ quản lý Danh mục sản phẩm {@link CategoryService}.
 * <p>
 * Đảm bảo các bất biến:
 * <ul>
 *     <li>Tên danh mục là duy nhất trong hệ thống.</li>
 *     <li>Không cho phép xóa danh mục khi đang có sản phẩm liên kết.</li>
 *     <li>Toàn bộ phương thức đọc sử dụng {@code readOnly = true} để tối ưu hiệu năng.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Bắt đầu tạo danh mục mới với tên: '{}'", request.name());

        String trimmedName = request.name().trim();
        if (categoryRepository.existsByName(trimmedName)) {
            log.warn("Tạo danh mục thất bại: Tên '{}' đã tồn tại", trimmedName);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Tên danh mục đã tồn tại trong hệ thống");
        }

        Category category = Category.builder()
            .name(trimmedName)
            .description(request.description())
            .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Tạo danh mục thành công với ID: {}", savedCategory.getId());

        return mapToCategoryResponse(savedCategory);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        log.debug("Lấy toàn bộ danh sách danh mục sản phẩm");

        return categoryRepository.findAll().stream()
            .map(this::mapToCategoryResponse)
            .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Tra cứu chi tiết danh mục theo ID: {}", id);

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Cập nhật thông tin danh mục ID: {}", id);

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        String trimmedName = request.name().trim();
        if (!category.getName().equalsIgnoreCase(trimmedName) && categoryRepository.existsByName(trimmedName)) {
            log.warn("Cập nhật danh mục thất bại: Tên '{}' đã tồn tại", trimmedName);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Tên danh mục đã tồn tại trong hệ thống");
        }

        category.setName(trimmedName);
        category.setDescription(request.description());

        Category updatedCategory = categoryRepository.save(category);
        log.info("Cập nhật danh mục ID: {} thành công", updatedCategory.getId());

        return mapToCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Yêu cầu xóa danh mục ID: {}", id);

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        // Kiểm tra xem danh mục có đang chứa sản phẩm nào không
        boolean hasProducts = productRepository.findByCategoryId(id, PageRequest.of(0, 1)).hasContent();
        if (hasProducts) {
            log.warn("Không thể xóa danh mục ID: {} vì vẫn còn sản phẩm đang liên kết", id);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể xóa danh mục đang có sản phẩm liên kết");
        }

        categoryRepository.delete(category);
        log.info("Xóa danh mục ID: {} thành công", id);
    }

    /**
     * Chuyển đổi thực thể {@link Category} sang DTO phản hồi {@link CategoryResponse}.
     */
    private CategoryResponse mapToCategoryResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getCreatedAt()
        );
    }
}
