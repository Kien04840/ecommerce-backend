package com.ecommerce.order.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử tích hợp (Integration Test) cho {@link OrderService} trên cơ sở dữ liệu thật.
 * <p>
 * Kiểm chứng các bất biến nghiệp vụ then chốt:
 * <ul>
 *     <li>Tính toàn vẹn giao dịch và Rollback khi một sản phẩm trong đơn checkout bị thiếu hàng.</li>
 *     <li>Cơ chế Pessimistic Write Lock ngăn chặn bán âm / bán quá tồn kho (Overselling) khi có 2 transaction đồng thời.</li>
 *     <li>Hoàn trả tồn kho nguyên tử và tính Idempotent khi hủy đơn hàng.</li>
 * </ul>
 */
@SpringBootTest
@DisplayName("Kiểm thử tích hợp OrderService trên Database thực tế")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Category testCategory;
    private Product product1;
    private Product product2;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Đảm bảo có Role trong database
        Role role = roleRepository.findByName("ROLE_CUSTOMER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").build()));

        // Tạo User duy nhất cho phiên test
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testUser = userRepository.save(User.builder()
            .username("buyer_" + uniqueSuffix)
            .email("buyer_" + uniqueSuffix + "@example.com")
            .password("EncodedPassword123!")
            .enabled(true)
            .role(role)
            .build());

        // Tạo Category cho phiên test
        testCategory = categoryRepository.save(Category.builder()
            .name("Danh mục Test " + uniqueSuffix)
            .description("Mô tả danh mục test")
            .build());

        // Tạo 2 sản phẩm phục vụ kiểm thử
        product1 = productRepository.save(Product.builder()
            .category(testCategory)
            .name("Sản phẩm Rollback A " + uniqueSuffix)
            .price(new BigDecimal("100000.00"))
            .stockQuantity(5)
            .status(ProductStatus.ACTIVE)
            .build());
        createdProductIds.add(product1.getId());

        product2 = productRepository.save(Product.builder()
            .category(testCategory)
            .name("Sản phẩm Rollback B " + uniqueSuffix)
            .price(new BigDecimal("200000.00"))
            .stockQuantity(1)
            .status(ProductStatus.ACTIVE)
            .build());
        createdProductIds.add(product2.getId());
    }

    @AfterEach
    void tearDown() {
        // Dọn dẹp dữ liệu theo đúng thứ tự ràng buộc khóa ngoại (FK): Order & OrderItems -> Product -> Category -> User
        for (Long orderId : createdOrderIds) {
            try {
                orderRepository.deleteById(orderId);
            } catch (Exception ignored) {
            }
        }
        for (Long productId : createdProductIds) {
            try {
                productRepository.deleteById(productId);
            } catch (Exception ignored) {
            }
        }
        try {
            if (testCategory != null) {
                categoryRepository.deleteById(testCategory.getId());
            }
        } catch (Exception ignored) {
        }
        try {
            if (testUser != null) {
                userRepository.deleteById(testUser.getId());
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Transaction Rollback: Sản phẩm 2 thiếu hàng phải rollback toàn bộ, tồn kho sản phẩm 1 được giữ nguyên")
    void createOrder_RollbackOnFailure_ShouldKeepStockAndNoOrderPersisted() {
        // Đặt mua: Product 1 (mua 2 / tồn 5), Product 2 (mua 2 / tồn 1 -> THIẾU HÀNG)
        CreateOrderRequest request = new CreateOrderRequest(List.of(
            new OrderItemRequest(product1.getId(), 2),
            new OrderItemRequest(product2.getId(), 2)
        ));

        // Thực thi checkout và kỳ vọng ném InsufficientStockException
        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(testUser.getId(), request));

        // Kiểm chứng tính nguyên tử: Tồn kho của product1 trong database KHÔNG BỊ TRỪ (vẫn nguyên là 5)
        Product reloadedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        assertEquals(5, reloadedProduct1.getStockQuantity(), "Tồn kho của Product 1 phải được rollback về 5");

        // Tồn kho của product2 trong database vẫn là 1
        Product reloadedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
        assertEquals(1, reloadedProduct2.getStockQuantity(), "Tồn kho của Product 2 phải giữ nguyên là 1");

        // Không có đơn hàng nào được tạo cho user
        long orderCount = orderRepository.findByUserId(testUser.getId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        assertEquals(0, orderCount, "Không được có đơn hàng nào được tạo trong DB khi transaction rollback");
    }

    @Test
    @DisplayName("Pessimistic Lock: 2 Transaction đồng thời mua sản phẩm tồn kho 1 thì đúng 1 thành công và 1 thất bại")
    void createOrder_ConcurrentCheckout_OnlyOneShouldSucceedAndZeroStock() throws InterruptedException, ExecutionException {
        // Tạo sản phẩm chỉ còn duy nhất 1 món trong kho
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Product concurrentProduct = productRepository.save(Product.builder()
            .category(testCategory)
            .name("Sản phẩm Concurrency " + uniqueSuffix)
            .price(new BigDecimal("500000.00"))
            .stockQuantity(1)
            .status(ProductStatus.ACTIVE)
            .build());
        createdProductIds.add(concurrentProduct.getId());

        CreateOrderRequest request = new CreateOrderRequest(List.of(
            new OrderItemRequest(concurrentProduct.getId(), 1)
        ));

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Callable<OrderResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            tasks.add(() -> {
                readyLatch.countDown();
                // Chờ hiệu lệnh đồng thời bắt đầu
                startLatch.await();
                try {
                    OrderResponse response = orderService.createOrder(testUser.getId(), request);
                    successCount.incrementAndGet();
                    synchronized (createdOrderIds) {
                        createdOrderIds.add(response.id());
                    }
                    return response;
                } catch (BusinessException ex) {
                    failureCount.incrementAndGet();
                    throw ex;
                }
            });
        }

        List<Future<OrderResponse>> futures = new ArrayList<>();
        for (var task : tasks) {
            futures.add(executorService.submit(task));
        }

        // Chờ 2 thread cùng sẵn sàng
        readyLatch.await(5, TimeUnit.SECONDS);
        // Phát lệnh cho 2 thread cùng lao vào checkout
        startLatch.countDown();

        // Chờ kết quả của cả 2 thread
        for (Future<OrderResponse> future : futures) {
            try {
                future.get();
            } catch (ExecutionException ignored) {
                // Kỳ vọng 1 thread ném ngoại lệ
            }
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Kiểm chứng kết quả Concurrency: Đúng 1 thành công, đúng 1 thất bại
        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 checkout được phép thành công");
        assertEquals(1, failureCount.get(), "Chỉ duy nhất 1 checkout phải thất bại do hết tồn kho");

        // Kiểm tra tồn kho cuối cùng trong database = 0 và trạng thái OUT_OF_STOCK
        Product finalProduct = productRepository.findById(concurrentProduct.getId()).orElseThrow();
        assertEquals(0, finalProduct.getStockQuantity(), "Tồn kho trong DB phải bằng 0 sau khi bán hết");
        assertEquals(ProductStatus.OUT_OF_STOCK, finalProduct.getStatus(), "Sản phẩm phải tự động chuyển sang OUT_OF_STOCK");
    }

    @Test
    @DisplayName("Hủy đơn hàng và Hoàn trả tồn kho: Tồn kho trong DB được phục hồi chính xác")
    void cancelOrder_Integration_ShouldRestoreStockInDatabase() {
        // 1. Tạo đơn hàng thành công mua 2 chiếc product1 (tồn kho ban đầu là 5)
        CreateOrderRequest request = new CreateOrderRequest(List.of(
            new OrderItemRequest(product1.getId(), 2)
        ));

        OrderResponse orderResponse = orderService.createOrder(testUser.getId(), request);
        createdOrderIds.add(orderResponse.id());

        // Kiểm tra tồn kho sau khi mua giảm xuống 3
        Product productAfterBuy = productRepository.findById(product1.getId()).orElseThrow();
        assertEquals(3, productAfterBuy.getStockQuantity());

        // 2. Thực hiện hủy đơn hàng
        OrderResponse cancelledResponse = orderService.cancelOrder(orderResponse.id(), testUser.getId());
        assertEquals(OrderStatus.CANCELLED, cancelledResponse.status());

        // 3. Kiểm chứng tồn kho trong database được phục hồi từ 3 lên 5
        Product productAfterCancel = productRepository.findById(product1.getId()).orElseThrow();
        assertEquals(5, productAfterCancel.getStockQuantity(), "Tồn kho trong DB phải được hoàn trả từ 3 về 5");
    }

    @Test
    @DisplayName("Tính Idempotent: Hủy lại đơn đã CANCELLED phải ném lỗi và tuyệt đối không hoàn kho lần 2")
    void cancelOrder_Idempotency_SecondCancelShouldFailWithoutRestoringStock() {
        // Tạo đơn mua 2 món (tồn kho ban đầu 5 -> còn 3)
        CreateOrderRequest request = new CreateOrderRequest(List.of(
            new OrderItemRequest(product1.getId(), 2)
        ));
        OrderResponse orderResponse = orderService.createOrder(testUser.getId(), request);
        createdOrderIds.add(orderResponse.id());

        // Hủy lần 1 thành công (tồn kho 3 -> 5)
        orderService.cancelOrder(orderResponse.id(), testUser.getId());
        Product productAfterFirstCancel = productRepository.findById(product1.getId()).orElseThrow();
        assertEquals(5, productAfterFirstCancel.getStockQuantity());

        // Hủy lần 2 phải bị từ chối
        BusinessException exception = assertThrows(BusinessException.class,
            () -> orderService.cancelOrder(orderResponse.id(), testUser.getId()));
        assertEquals("Đơn hàng đã bị hủy trước đó", exception.getMessage());

        // Tồn kho trong DB vẫn phải là 5, không được phép tăng lên 7!
        Product productAfterSecondCancel = productRepository.findById(product1.getId()).orElseThrow();
        assertEquals(5, productAfterSecondCancel.getStockQuantity(), "Tồn kho trong DB không được tăng thêm lần 2");
    }
}

