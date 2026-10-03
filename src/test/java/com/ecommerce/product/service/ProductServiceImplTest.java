package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSearchCriteria;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.product.dto.UpdateStockRequest;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Tag;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho {@link ProductServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử ProductServiceImpl")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
            .name("Thiết bị điện tử")
            .description("Các thiết bị công nghệ hiện đại")
            .build();
        ReflectionTestUtils.setField(testCategory, "id", 1L);

        testProduct = Product.builder()
            .category(testCategory)
            .name("Bàn phím cơ không dây")
            .description("Bàn phím cơ Bluetooth RGB")
            .price(new BigDecimal("1500000.00"))
            .stockQuantity(50)
            .status(ProductStatus.ACTIVE)
            .imageUrl("https://example.com/keyboard.jpg")
            .build();
        ReflectionTestUtils.setField(testProduct, "id", 100L);
    }

    @Nested
    @DisplayName("Nghiệp vụ tạo mới sản phẩm (createProduct)")
    class CreateProductTests {

        @Test
        @DisplayName("Tạo sản phẩm thành công khi tồn kho > 0: khởi tạo trạng thái INACTIVE theo State Machine")
        void createProduct_WithStock_ShouldBeInactive() {
            CreateProductRequest request = new CreateProductRequest(
                1L,
                "Chuột gaming không dây",
                "Chuột gaming công thái học",
                new BigDecimal("800000.00"),
                20,
                "https://example.com/mouse.jpg",
                Set.of("gaming", "wireless")
            );

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.existsByName("Chuột gaming không dây")).thenReturn(false);
            when(tagRepository.findByName("gaming")).thenReturn(Optional.of(Tag.builder().name("gaming").build()));
            when(tagRepository.findByName("wireless")).thenReturn(Optional.empty());
            when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 101L);
                return p;
            });

            ProductResponse response = productService.createProduct(request);

            assertNotNull(response);
            assertEquals(101L, response.id());
            assertEquals("Chuột gaming không dây", response.name());
            assertEquals(ProductStatus.INACTIVE, response.status());
            assertEquals(20, response.stockQuantity());
            assertEquals(1L, response.category().id());
            assertEquals(1, response.images().size());
            assertEquals("https://example.com/mouse.jpg", response.images().getFirst().url());
            assertTrue(response.images().getFirst().isPrimary());
            assertEquals(2, response.tags().size());

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Tạo sản phẩm thành công khi tồn kho = 0: khởi tạo trạng thái INACTIVE theo State Machine")
        void createProduct_WithZeroStock_ShouldBeInactive() {
            CreateProductRequest request = new CreateProductRequest(
                1L,
                "Sản phẩm hết hàng",
                "Mô tả",
                new BigDecimal("500000.00"),
                0,
                null,
                null
            );

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.existsByName(anyString())).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 102L);
                return p;
            });

            ProductResponse response = productService.createProduct(request);

            assertNotNull(response);
            assertEquals(ProductStatus.INACTIVE, response.status());
            assertEquals(0, response.stockQuantity());
        }

        @Test
        @DisplayName("Tạo sản phẩm thất bại khi danh mục không tồn tại")
        void createProduct_CategoryNotFound_ShouldThrowException() {
            CreateProductRequest request = new CreateProductRequest(
                999L,
                "Chuột gaming",
                "Mô tả",
                new BigDecimal("800000.00"),
                10,
                null,
                null
            );

            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> productService.createProduct(request));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Tạo sản phẩm thất bại khi trùng tên sản phẩm")
        void createProduct_DuplicateName_ShouldThrowException() {
            CreateProductRequest request = new CreateProductRequest(
                1L,
                "Bàn phím cơ không dây",
                "Mô tả",
                new BigDecimal("1500000.00"),
                10,
                null,
                null
            );

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.existsByName("Bàn phím cơ không dây")).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.createProduct(request));

            assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Tạo sản phẩm thất bại khi giá bán nhỏ hơn hoặc bằng 0")
        void createProduct_InvalidPrice_ShouldThrowException() {
            CreateProductRequest request = new CreateProductRequest(
                1L,
                "Sản phẩm giá lỗi",
                "Mô tả",
                BigDecimal.ZERO,
                10,
                null,
                null
            );

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.existsByName(anyString())).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.createProduct(request));

            assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
            assertEquals("Giá sản phẩm phải lớn hơn 0", exception.getMessage());
        }

        @Test
        @DisplayName("Tạo sản phẩm thất bại khi số lượng tồn kho âm")
        void createProduct_NegativeStock_ShouldThrowException() {
            CreateProductRequest request = new CreateProductRequest(
                1L,
                "Sản phẩm tồn kho lỗi",
                "Mô tả",
                new BigDecimal("100000.00"),
                -5,
                null,
                null
            );

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.existsByName(anyString())).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.createProduct(request));

            assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
            assertEquals("Số lượng tồn kho không được âm", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ truy vấn chi tiết sản phẩm (getProductById)")
    class GetProductByIdTests {

        @Test
        @DisplayName("Truy vấn thành công khi ID sản phẩm tồn tại")
        void getProductById_Success() {
            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));

            ProductResponse response = productService.getProductById(100L);

            assertNotNull(response);
            assertEquals(100L, response.id());
            assertEquals("Bàn phím cơ không dây", response.name());
            assertEquals(new BigDecimal("1500000.00"), response.price());
            verify(productRepository).findByIdWithDetails(100L);
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi ID sản phẩm không tồn tại")
        void getProductById_NotFound_ShouldThrowException() {
            when(productRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(999L));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ tìm kiếm và phân trang sản phẩm (searchProducts)")
    class SearchProductsTests {

        @Test
        @DisplayName("Tìm kiếm và phân trang thành công với các tiêu chí lọc")
        void searchProducts_WithCriteria_Success() {
            ProductSearchCriteria criteria = new ProductSearchCriteria(
                "bàn phím",
                1L,
                ProductStatus.ACTIVE,
                new BigDecimal("1000000.00"),
                new BigDecimal("2000000.00")
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

            when(productRepository.searchAndFilterProducts(
                eq("bàn phím"),
                eq(1L),
                eq(ProductStatus.ACTIVE),
                eq(new BigDecimal("1000000.00")),
                eq(new BigDecimal("2000000.00")),
                eq(pageable)
            )).thenReturn(page);

            PagedResponse<ProductResponse> response = productService.searchProducts(criteria, pageable);

            assertNotNull(response);
            assertEquals(1, response.items().size());
            assertEquals(1, response.pagination().totalElements());
            assertEquals(1, response.pagination().totalPages());
            assertEquals(0, response.pagination().page());
            assertEquals(10, response.pagination().size());
        }

        @Test
        @DisplayName("Tìm kiếm với criteria rỗng vẫn hoạt động bình thường")
        void searchProducts_NullCriteria_Success() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

            when(productRepository.searchAndFilterProducts(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(pageable)
            )).thenReturn(page);

            PagedResponse<ProductResponse> response = productService.searchProducts(null, pageable);

            assertNotNull(response);
            assertEquals(1, response.items().size());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ cập nhật sản phẩm (updateProduct)")
    class UpdateProductTests {

        @Test
        @DisplayName("Cập nhật sản phẩm thành công")
        void updateProduct_Success() {
            UpdateProductRequest request = new UpdateProductRequest(
                1L,
                "Bàn phím cơ Custom cao cấp",
                "Mô tả mới",
                new BigDecimal("1800000.00"),
                ProductStatus.ACTIVE,
                "https://example.com/new.jpg",
                Set.of("custom")
            );

            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));
            when(productRepository.existsByName("Bàn phím cơ Custom cao cấp")).thenReturn(false);
            when(tagRepository.findByName("custom")).thenReturn(Optional.of(Tag.builder().name("custom").build()));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductResponse response = productService.updateProduct(100L, request);

            assertNotNull(response);
            assertEquals("Bàn phím cơ Custom cao cấp", response.name());
            assertEquals(new BigDecimal("1800000.00"), response.price());
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("Cập nhật thất bại khi đổi tên trùng với sản phẩm khác")
        void updateProduct_DuplicateName_ShouldThrowException() {
            UpdateProductRequest request = new UpdateProductRequest(
                1L,
                "Tên sản phẩm trùng lặp",
                "Mô tả",
                new BigDecimal("1800000.00"),
                ProductStatus.ACTIVE,
                null,
                null
            );

            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));
            when(productRepository.existsByName("Tên sản phẩm trùng lặp")).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.updateProduct(100L, request));

            assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ cập nhật tồn kho (updateStock)")
    class UpdateStockTests {

        @Test
        @DisplayName("Cập nhật tồn kho thành công")
        void updateStock_Success() {
            UpdateStockRequest request = new UpdateStockRequest(80);

            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductResponse response = productService.updateStock(100L, request);

            assertNotNull(response);
            assertEquals(80, response.stockQuantity());
            assertEquals(ProductStatus.ACTIVE, response.status());
        }

        @Test
        @DisplayName("Cập nhật tồn kho về 0 tự động chuyển trạng thái sang OUT_OF_STOCK")
        void updateStock_ToZero_ShouldChangeToOutOfStock() {
            UpdateStockRequest request = new UpdateStockRequest(0);

            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductResponse response = productService.updateStock(100L, request);

            assertEquals(0, response.stockQuantity());
            assertEquals(ProductStatus.OUT_OF_STOCK, response.status());
        }

        @Test
        @DisplayName("Cập nhật tồn kho từ 0 lên dương khi đang OUT_OF_STOCK tự động kích hoạt lại ACTIVE")
        void updateStock_FromZeroToPositive_ShouldChangeToActive() {
            testProduct.setStatus(ProductStatus.OUT_OF_STOCK);
            testProduct.setStockQuantity(0);

            UpdateStockRequest request = new UpdateStockRequest(25);

            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductResponse response = productService.updateStock(100L, request);

            assertEquals(25, response.stockQuantity());
            assertEquals(ProductStatus.ACTIVE, response.status());
        }

        @Test
        @DisplayName("Cập nhật tồn kho thất bại khi số lượng âm")
        void updateStock_Negative_ShouldThrowException() {
            UpdateStockRequest request = new UpdateStockRequest(-10);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.updateStock(100L, request));

            assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ thay đổi trạng thái và xóa mềm (Status & Soft Delete)")
    class StatusAndDeletionTests {

        @Test
        @DisplayName("Thay đổi trạng thái thành công sang INACTIVE")
        void changeProductStatus_Success() {
            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductResponse response = productService.changeProductStatus(100L, ProductStatus.INACTIVE);

            assertEquals(ProductStatus.INACTIVE, response.status());
        }

        @Test
        @DisplayName("Không cho phép kích hoạt ACTIVE khi tồn kho bằng 0")
        void changeProductStatus_ActiveWhenStockZero_ShouldThrowException() {
            testProduct.setStockQuantity(0);
            when(productRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testProduct));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.changeProductStatus(100L, ProductStatus.ACTIVE));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        }

        @Test
        @DisplayName("Xóa mềm sản phẩm thành công bằng cách chuyển trạng thái sang INACTIVE")
        void deleteProduct_SoftDelete_Success() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            productService.deleteProduct(100L);

            assertEquals(ProductStatus.INACTIVE, testProduct.getStatus());
            verify(productRepository).save(testProduct);
            verify(productRepository, never()).delete(any(Product.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ hỗ trợ giao tiếp liên module (Inter-module Stock Operations)")
    class InterModuleOperationsTests {

        @Test
        @DisplayName("Kiểm tra sản phẩm có được phép mua (isProductPurchasable)")
        void isProductPurchasable_Scenarios() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            // Đủ điều kiện: ACTIVE và tồn kho 50 >= 10
            assertTrue(productService.isProductPurchasable(100L, 10));

            // Không đủ điều kiện: Mua 60 > 50
            assertFalse(productService.isProductPurchasable(100L, 60));

            // Không đủ điều kiện: Trạng thái OUT_OF_STOCK
            testProduct.setStatus(ProductStatus.OUT_OF_STOCK);
            assertFalse(productService.isProductPurchasable(100L, 5));

            // Số lượng mua âm hoặc bằng 0
            assertFalse(productService.isProductPurchasable(100L, 0));
        }

        @Test
        @DisplayName("Trừ tồn kho nguyên tử thành công")
        void deductStock_Success() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            productService.deductStock(100L, 20);

            assertEquals(30, testProduct.getStockQuantity());
            assertEquals(ProductStatus.ACTIVE, testProduct.getStatus());
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("Trừ hết sạch tồn kho tự động chuyển trạng thái sang OUT_OF_STOCK")
        void deductStock_UntilZero_ShouldChangeToOutOfStock() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            productService.deductStock(100L, 50);

            assertEquals(0, testProduct.getStockQuantity());
            assertEquals(ProductStatus.OUT_OF_STOCK, testProduct.getStatus());
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("Trừ tồn kho thất bại và ném InsufficientStockException khi không đủ hàng")
        void deductStock_InsufficientStock_ShouldThrowException() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> productService.deductStock(100L, 51));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Trừ tồn kho thất bại khi sản phẩm không ở trạng thái ACTIVE")
        void deductStock_NotActive_ShouldThrowException() {
            testProduct.setStatus(ProductStatus.INACTIVE);
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.deductStock(100L, 5));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        }

        @Test
        @DisplayName("Hoàn trả tồn kho thành công và tự động kích hoạt lại trạng thái ACTIVE nếu trước đó OUT_OF_STOCK")
        void restoreStock_Success() {
            testProduct.setStatus(ProductStatus.OUT_OF_STOCK);
            testProduct.setStockQuantity(0);

            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            productService.restoreStock(100L, 15);

            assertEquals(15, testProduct.getStockQuantity());
            assertEquals(ProductStatus.ACTIVE, testProduct.getStatus());
            verify(productRepository).save(testProduct);
        }
    }
}
