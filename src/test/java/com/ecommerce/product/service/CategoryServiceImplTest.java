package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.CategoryRequest;
import com.ecommerce.product.dto.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho {@link CategoryServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử CategoryServiceImpl")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
            .name("Gia dụng")
            .description("Đồ dùng trong nhà")
            .build();
        ReflectionTestUtils.setField(testCategory, "id", 10L);
    }

    @Nested
    @DisplayName("Nghiệp vụ tạo danh mục (createCategory)")
    class CreateCategoryTests {

        @Test
        @DisplayName("Tạo danh mục thành công khi dữ liệu hợp lệ")
        void createCategory_Success() {
            CategoryRequest request = new CategoryRequest("Thời trang nam", "Quần áo thời trang nam");

            when(categoryRepository.existsByName("Thời trang nam")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
                Category c = invocation.getArgument(0);
                ReflectionTestUtils.setField(c, "id", 20L);
                return c;
            });

            CategoryResponse response = categoryService.createCategory(request);

            assertNotNull(response);
            assertEquals(20L, response.id());
            assertEquals("Thời trang nam", response.name());
            assertEquals("Quần áo thời trang nam", response.description());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Tạo danh mục thất bại khi trùng tên danh mục")
        void createCategory_DuplicateName_ShouldThrowException() {
            CategoryRequest request = new CategoryRequest("Gia dụng", "Mô tả mới");

            when(categoryRepository.existsByName("Gia dụng")).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.createCategory(request));

            assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ tra cứu danh mục (getAllCategories, getCategoryById)")
    class QueryCategoryTests {

        @Test
        @DisplayName("Lấy danh sách tất cả danh mục thành công")
        void getAllCategories_Success() {
            when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

            List<CategoryResponse> list = categoryService.getAllCategories();

            assertEquals(1, list.size());
            assertEquals("Gia dụng", list.getFirst().name());
        }

        @Test
        @DisplayName("Lấy chi tiết danh mục theo ID thành công")
        void getCategoryById_Success() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));

            CategoryResponse response = categoryService.getCategoryById(10L);

            assertNotNull(response);
            assertEquals(10L, response.id());
            assertEquals("Gia dụng", response.name());
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi ID danh mục không tồn tại")
        void getCategoryById_NotFound_ShouldThrowException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(999L));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ cập nhật danh mục (updateCategory)")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Cập nhật danh mục thành công")
        void updateCategory_Success() {
            CategoryRequest request = new CategoryRequest("Gia dụng thông minh", "Thiết bị nhà cửa tiện ích");

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByName("Gia dụng thông minh")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CategoryResponse response = categoryService.updateCategory(10L, request);

            assertNotNull(response);
            assertEquals("Gia dụng thông minh", response.name());
            assertEquals("Thiết bị nhà cửa tiện ích", response.description());
        }

        @Test
        @DisplayName("Cập nhật thất bại khi đổi tên trùng với danh mục khác")
        void updateCategory_DuplicateName_ShouldThrowException() {
            CategoryRequest request = new CategoryRequest("Điện máy", "Mô tả");

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByName("Điện máy")).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.updateCategory(10L, request));

            assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ xóa danh mục (deleteCategory)")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Xóa danh mục thành công khi không có sản phẩm liên kết")
        void deleteCategory_Success() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
            when(productRepository.findByCategoryId(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

            categoryService.deleteCategory(10L);

            verify(categoryRepository).delete(testCategory);
        }

        @Test
        @DisplayName("Không cho phép xóa danh mục khi vẫn còn sản phẩm đang thuộc danh mục")
        void deleteCategory_HasProducts_ShouldThrowException() {
            Product product = Product.builder().category(testCategory).name("Nồi cơm").build();

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
            when(productRepository.findByCategoryId(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(10L));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            assertEquals("Không thể xóa danh mục đang có sản phẩm liên kết", exception.getMessage());
            verify(categoryRepository, never()).delete(any(Category.class));
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi xóa danh mục không tồn tại")
        void deleteCategory_NotFound_ShouldThrowException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(999L));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
            verify(categoryRepository, never()).delete(any(Category.class));
        }
    }
}
