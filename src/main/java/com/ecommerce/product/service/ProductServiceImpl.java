package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.product.dto.CategoryResponse;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductImageResponse;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSearchCriteria;
import com.ecommerce.product.dto.TagResponse;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.product.dto.UpdateStockRequest;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Tag;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lớp triển khai các nghiệp vụ cho {@link ProductService}.
 * <p>
 * Đảm bảo các bất biến nghiệp vụ cốt lõi:
 * <ul>
 *     <li>Giá sản phẩm luôn dương (price &gt; 0).</li>
 *     <li>Tồn kho không bao giờ âm (stockQuantity &ge; 0).</li>
 *     <li>Đồng bộ vòng đời trạng thái kinh doanh theo số lượng tồn kho.</li>
 *     <li>Trừ và hoàn trả tồn kho nguyên tử hỗ trợ quy trình Checkout/Order.</li>
 *     <li>Toàn bộ phương thức đọc sử dụng {@code readOnly = true} để tối ưu tài nguyên.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Bắt đầu tạo mới sản phẩm với tên: '{}', danh mục ID: {}", request.name(), request.categoryId());

        // Kiểm tra danh mục tồn tại
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", request.categoryId()));

        // Kiểm tra trùng lặp tên sản phẩm
        String trimmedName = request.name().trim();
        if (productRepository.existsByName(trimmedName)) {
            log.warn("Tạo sản phẩm thất bại do trùng tên: '{}'", trimmedName);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Tên sản phẩm đã tồn tại trong hệ thống");
        }

        // Kiểm tra phòng thủ tính hợp lệ của giá và tồn kho
        validatePrice(request.price());
        validateStockQuantity(request.stockQuantity());

        // Theo State Machine (docs/business-rules.md), sản phẩm tạo mới luôn khởi tạo ở trạng thái INACTIVE (chờ Admin duyệt mở bán)
        ProductStatus initialStatus = ProductStatus.INACTIVE;

        Product product = Product.builder()
            .category(category)
            .name(trimmedName)
            .description(request.description())
            .price(request.price())
            .stockQuantity(request.stockQuantity())
            .status(initialStatus)
            .imageUrl(request.imageUrl())
            .build();

        // Gắn ảnh đại diện ban đầu vào danh sách hình ảnh nếu có
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            ProductImage primaryImage = ProductImage.builder()
                .product(product)
                .url(request.imageUrl().trim())
                .isPrimary(true)
                .build();
            product.addImage(primaryImage);
        }

        // Xử lý gắn thẻ phân loại (Tags)
        if (request.tagNames() != null && !request.tagNames().isEmpty()) {
            for (String tagName : request.tagNames()) {
                if (tagName != null && !tagName.isBlank()) {
                    String trimmedTagName = tagName.trim();
                    Tag tag = tagRepository.findByName(trimmedTagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(trimmedTagName).build()));
                    product.addTag(tag);
                }
            }
        }

        Product savedProduct = productRepository.save(product);
        log.info("Tạo mới sản phẩm thành công với ID: {}", savedProduct.getId());

        return mapToProductResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        log.debug("Truy vấn chi tiết sản phẩm theo ID: {}", id);

        Product product = productRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));

        return mapToProductResponse(product);
    }

    @Override
    public PagedResponse<ProductResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        Pageable effectivePageable = (pageable != null) ? pageable : PageRequest.of(0, 20);

        String keyword = null;
        Long categoryId = null;
        ProductStatus status = null;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        if (criteria != null) {
            keyword = (criteria.keyword() != null && !criteria.keyword().isBlank())
                ? criteria.keyword().trim()
                : null;
            categoryId = criteria.categoryId();
            status = criteria.status();
            minPrice = criteria.minPrice();
            maxPrice = criteria.maxPrice();
        }

        log.debug("Tìm kiếm sản phẩm với keyword: '{}', categoryId: {}, status: {}, page: {}",
            keyword, categoryId, status, effectivePageable.getPageNumber());

        Page<Product> productPage = productRepository.searchAndFilterProducts(
            keyword, categoryId, status, minPrice, maxPrice, effectivePageable
        );

        List<ProductResponse> content = productPage.getContent().stream()
            .map(this::mapToProductResponse)
            .toList();

        return PagedResponse.of(content, productPage);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Cập nhật thông tin sản phẩm ID: {}", id);

        Product product = productRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));

        // Kiểm tra danh mục mới nếu có thay đổi
        if (!product.getCategory().getId().equals(request.categoryId())) {
            Category newCategory = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", request.categoryId()));
            product.setCategory(newCategory);
        }

        // Kiểm tra tên sản phẩm mới có bị trùng lặp với sản phẩm khác không
        String trimmedNewName = request.name().trim();
        if (!product.getName().equalsIgnoreCase(trimmedNewName) && productRepository.existsByName(trimmedNewName)) {
            log.warn("Cập nhật sản phẩm thất bại do trùng tên: '{}'", trimmedNewName);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Tên sản phẩm đã tồn tại trong hệ thống");
        }

        validatePrice(request.price());

        product.setName(trimmedNewName);
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStatus(request.status());
        product.setImageUrl(request.imageUrl());

        // Cập nhật lại danh sách thẻ phân loại (Tags)
        if (request.tagNames() != null) {
            Set<Tag> existingTags = new HashSet<>(product.getTags());
            existingTags.forEach(product::removeTag);

            for (String tagName : request.tagNames()) {
                if (tagName != null && !tagName.isBlank()) {
                    String trimmedTagName = tagName.trim();
                    Tag tag = tagRepository.findByName(trimmedTagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(trimmedTagName).build()));
                    product.addTag(tag);
                }
            }
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Cập nhật thông tin sản phẩm ID: {} thành công", updatedProduct.getId());

        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, UpdateStockRequest request) {
        log.info("Cập nhật tồn kho sản phẩm ID: {}, số lượng mới: {}", id, request.stockQuantity());

        validateStockQuantity(request.stockQuantity());

        Product product = productRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));

        product.setStockQuantity(request.stockQuantity());

        // Tự động chuyển trạng thái theo quy tắc State Machine:
        // - Nếu số lượng về 0 và đang ACTIVE -> chuyển sang OUT_OF_STOCK
        // - Nếu số lượng > 0 và đang OUT_OF_STOCK -> chuyển lại sang ACTIVE
        if (request.stockQuantity() == 0 && product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            log.info("Sản phẩm ID: {} đã hết hàng, tự động chuyển trạng thái sang OUT_OF_STOCK", id);
        } else if (request.stockQuantity() > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
            log.info("Sản phẩm ID: {} đã có hàng trở lại, tự động chuyển trạng thái sang ACTIVE", id);
        }

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse changeProductStatus(Long id, ProductStatus status) {
        log.info("Thay đổi trạng thái sản phẩm ID: {} sang {}", id, status);

        if (status == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Trạng thái sản phẩm không được để trống");
        }

        Product product = productRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));

        // Không cho phép kích hoạt mở bán sản phẩm khi tồn kho = 0
        if (status == ProductStatus.ACTIVE && product.getStockQuantity() == 0) {
            log.warn("Không thể kích hoạt sản phẩm ID: {} vì số lượng tồn kho bằng 0", id);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể kích hoạt sản phẩm khi số lượng tồn kho bằng 0");
        }

        product.setStatus(status);
        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Xóa mềm sản phẩm ID: {}", id);

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));

        // Áp dụng xóa mềm: Chuyển trạng thái sang INACTIVE để bảo toàn toàn vẹn dữ liệu đơn hàng lịch sử
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);

        log.info("Sản phẩm ID: {} đã được chuyển trạng thái sang INACTIVE (xóa mềm thành công)", id);
    }

    @Override
    public boolean isProductPurchasable(Long productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return false;
        }

        return productRepository.findById(productId)
            .map(product -> product.getStatus() == ProductStatus.ACTIVE && product.getStockQuantity() >= quantity)
            .orElse(false);
    }

    @Override
    @Transactional
    public void deductStock(Long productId, int quantity) {
        log.info("Thực hiện trừ tồn kho nguyên tử cho sản phẩm ID: {}, số lượng: {}", productId, quantity);

        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Số lượng cần trừ kho phải lớn hơn 0");
        }

        // Xóa sạch First-Level Cache (Persistence Context) để đảm bảo câu lệnh Pessimistic Lock tải lại dữ liệu mới nhất từ database
        entityManager.clear();

        // Khóa dòng sản phẩm bằng Pessimistic Write Lock (SELECT ... FOR UPDATE) để chống Race Condition khi nhiều khách hàng cùng checkout
        Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", productId));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            log.warn("Trừ tồn kho thất bại: Sản phẩm ID: {} hiện không ở trạng thái ACTIVE (Trạng thái: {})",
                productId, product.getStatus());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Sản phẩm hiện không mở bán để đặt mua");
        }

        if (product.getStockQuantity() < quantity) {
            log.warn("Trừ tồn kho thất bại: Sản phẩm ID: {} chỉ còn {}, yêu cầu trừ {}",
                productId, product.getStockQuantity(), quantity);
            throw new InsufficientStockException(productId, quantity, product.getStockQuantity());
        }

        int remainingStock = product.getStockQuantity() - quantity;
        product.setStockQuantity(remainingStock);

        // Nếu trừ hết hàng thì tự động đổi trạng thái sang OUT_OF_STOCK
        if (remainingStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            log.info("Sản phẩm ID: {} đã hết sạch hàng, tự động chuyển sang OUT_OF_STOCK", productId);
        }

        productRepository.save(product);
        log.info("Trừ tồn kho thành công cho sản phẩm ID: {}. Tồn kho còn lại: {}", productId, remainingStock);
    }

    @Override
    @Transactional
    public void restoreStock(Long productId, int quantity) {
        log.info("Thực hiện hoàn trả tồn kho cho sản phẩm ID: {}, số lượng: {}", productId, quantity);

        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Số lượng cần hoàn trả phải lớn hơn 0");
        }

        // Xóa sạch First-Level Cache trước khi khóa dòng hoàn trả kho
        entityManager.clear();

        // Khóa dòng sản phẩm bằng Pessimistic Write Lock khi hoàn trả tồn kho
        Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", productId));

        int newStock = product.getStockQuantity() + quantity;
        product.setStockQuantity(newStock);

        // Nếu sản phẩm trước đó bị OUT_OF_STOCK, khi hoàn trả kho > 0 tự động kích hoạt lại sang ACTIVE
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
            product.setStatus(ProductStatus.ACTIVE);
            log.info("Sản phẩm ID: {} được hoàn kho từ 0 lên {}, tự động kích hoạt lại sang ACTIVE", productId, newStock);
        }

        productRepository.save(product);
        log.info("Hoàn trả tồn kho thành công cho sản phẩm ID: {}. Tồn kho mới: {}", productId, newStock);
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH RIÊNG (PRIVATE HELPERS)
    // ==========================================

    /**
     * Kiểm tra tính hợp lệ của giá bán sản phẩm.
     */
    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Giá sản phẩm phải lớn hơn 0");
        }
    }

    /**
     * Kiểm tra tính hợp lệ của số lượng tồn kho.
     */
    private void validateStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Số lượng tồn kho không được âm");
        }
    }

    /**
     * Ánh xạ thực thể {@link Product} sang DTO phản hồi {@link ProductResponse}.
     */
    private ProductResponse mapToProductResponse(Product product) {
        Category category = product.getCategory();
        CategoryResponse categoryResponse = (category != null)
            ? new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.getCreatedAt())
            : null;

        List<ProductImageResponse> imageResponses = (product.getImages() != null)
            ? product.getImages().stream()
                .map(img -> new ProductImageResponse(img.getId(), img.getUrl(), img.getIsPrimary()))
                .toList()
            : Collections.emptyList();

        Set<TagResponse> tagResponses = (product.getTags() != null)
            ? product.getTags().stream()
                .map(t -> new TagResponse(t.getId(), t.getName()))
                .collect(Collectors.toSet())
            : Collections.emptySet();

        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getStatus(),
            product.getImageUrl(),
            categoryResponse,
            imageResponses,
            tagResponses,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
