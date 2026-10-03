package com.ecommerce.order.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.service.ProductService;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ OrderServiceImpl")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private User otherUser;
    private UserResponse testUserResponse;
    private Product testProduct1;
    private Product testProduct2;
    private ProductResponse testProductResponse1;
    private ProductResponse testProductResponse2;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .username("buyer01")
            .email("buyer01@example.com")
            .password("Password123!")
            .enabled(true)
            .build();
        testUser.setId(1L);

        otherUser = User.builder()
            .username("buyer02")
            .email("buyer02@example.com")
            .password("Password123!")
            .enabled(true)
            .build();
        otherUser.setId(2L);

        testUserResponse = new UserResponse(
            1L,
            "buyer01",
            "buyer01@example.com",
            true,
            null,
            LocalDateTime.now()
        );

        testProduct1 = Product.builder()
            .name("Bàn phím cơ không dây")
            .price(new BigDecimal("1500000.00"))
            .stockQuantity(10)
            .status(ProductStatus.ACTIVE)
            .build();
        testProduct1.setId(10L);

        testProduct2 = Product.builder()
            .name("Chuột công thái học")
            .price(new BigDecimal("800000.00"))
            .stockQuantity(5)
            .status(ProductStatus.ACTIVE)
            .build();
        testProduct2.setId(20L);

        testProductResponse1 = new ProductResponse(
            10L,
            "Bàn phím cơ không dây",
            "Mô tả bàn phím",
            new BigDecimal("1500000.00"),
            10,
            ProductStatus.ACTIVE,
            "https://example.com/keyboard.png",
            null,
            Collections.emptyList(),
            Collections.emptySet(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        testProductResponse2 = new ProductResponse(
            20L,
            "Chuột công thái học",
            "Mô tả chuột",
            new BigDecimal("800000.00"),
            5,
            ProductStatus.ACTIVE,
            "https://example.com/mouse.png",
            null,
            Collections.emptyList(),
            Collections.emptySet(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        testOrder = Order.builder()
            .user(testUser)
            .totalAmount(new BigDecimal("2300000.00"))
            .status(OrderStatus.PENDING)
            .paymentStatus(PaymentStatus.UNPAID)
            .build();
        testOrder.setId(100L);

        OrderItem item1 = OrderItem.builder()
            .order(testOrder)
            .product(testProduct1)
            .quantity(1)
            .unitPrice(new BigDecimal("1500000.00"))
            .subtotal(new BigDecimal("1500000.00"))
            .build();
        item1.setId(1001L);

        OrderItem item2 = OrderItem.builder()
            .order(testOrder)
            .product(testProduct2)
            .quantity(1)
            .unitPrice(new BigDecimal("800000.00"))
            .subtotal(new BigDecimal("800000.00"))
            .build();
        item2.setId(1002L);

        testOrder.addOrderItem(item1);
        testOrder.addOrderItem(item2);
    }

    @Nested
    @DisplayName("Nghiệp vụ khởi tạo đơn hàng (createOrder - Checkout)")
    class CreateOrderTests {

        @Test
        @DisplayName("Tạo đơn hàng thành công với nhiều sản phẩm và tính toán chính xác tổng tiền")
        void createOrder_Success() {
            CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(20L, 1), // Product 20
                new OrderItemRequest(10L, 2)  // Product 10 (thứ tự gửi không theo ID)
            ));

            when(userService.getUserById(1L)).thenReturn(testUserResponse);
            when(productService.getProductById(10L)).thenReturn(testProductResponse1);
            when(productService.getProductById(20L)).thenReturn(testProductResponse2);
            when(entityManager.getReference(User.class, 1L)).thenReturn(testUser);
            when(entityManager.getReference(Product.class, 10L)).thenReturn(testProduct1);
            when(entityManager.getReference(Product.class, 20L)).thenReturn(testProduct2);

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(100L);
                long itemId = 1001L;
                for (OrderItem item : order.getOrderItems()) {
                    item.setId(itemId++);
                }
                return order;
            });

            OrderResponse response = orderService.createOrder(1L, request);

            assertNotNull(response);
            assertEquals(100L, response.id());
            assertEquals(1L, response.userId());
            assertEquals(OrderStatus.PENDING, response.status());
            assertEquals(PaymentStatus.UNPAID, response.paymentStatus());

            // Tổng tiền: 2 * 1,500,000 + 1 * 800,000 = 3,800,000
            BigDecimal expectedTotal = new BigDecimal("3800000.00");
            assertEquals(expectedTotal, response.totalAmount());
            assertEquals(2, response.orderItems().size());

            // Đảm bảo thứ tự lock và deduct kho theo productId tăng dần: 10L trước, 20L sau (ngăn Deadlock)
            var inOrderVerifier = inOrder(productService);
            inOrderVerifier.verify(productService).deductStock(10L, 2);
            inOrderVerifier.verify(productService).deductStock(20L, 1);

            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Gom nhóm số lượng khi khách hàng gửi trùng productId trong danh sách items")
        void createOrder_DuplicateProductIds_ShouldMergeQuantities() {
            CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(10L, 1),
                new OrderItemRequest(10L, 2)
            ));

            when(userService.getUserById(1L)).thenReturn(testUserResponse);
            when(productService.getProductById(10L)).thenReturn(testProductResponse1);
            when(entityManager.getReference(User.class, 1L)).thenReturn(testUser);
            when(entityManager.getReference(Product.class, 10L)).thenReturn(testProduct1);

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(100L);
                return order;
            });

            OrderResponse response = orderService.createOrder(1L, request);

            assertNotNull(response);
            // Chỉ deduct 1 lần với tổng số lượng 1 + 2 = 3
            verify(productService, times(1)).deductStock(10L, 3);
            assertEquals(new BigDecimal("4500000.00"), response.totalAmount());
        }

        @Test
        @DisplayName("Ném lỗi khi userId là null")
        void createOrder_NullUserId_ShouldThrowException() {
            CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(10L, 1)));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.createOrder(null, request));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Ném lỗi khi danh sách items trong đơn hàng rỗng hoặc null")
        void createOrder_EmptyItems_ShouldThrowException() {
            CreateOrderRequest request = new CreateOrderRequest(Collections.emptyList());

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, request));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Ném lỗi khi số lượng đặt mua không hợp lệ (nhỏ hơn hoặc bằng 0)")
        void createOrder_InvalidQuantity_ShouldThrowException() {
            CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(10L, 0)));
            when(userService.getUserById(1L)).thenReturn(testUserResponse);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, request));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(productService, never()).deductStock(any(), any(Integer.class));
        }

        @Test
        @DisplayName("Ném lỗi khi sản phẩm không ở trạng thái ACTIVE")
        void createOrder_ProductNotActive_ShouldThrowException() {
            CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(10L, 1)));

            ProductResponse inactiveProduct = new ProductResponse(
                10L, "Sản phẩm ẩn", "Mô tả", new BigDecimal("100000.00"),
                10, ProductStatus.INACTIVE, null, null,
                Collections.emptyList(), Collections.emptySet(), LocalDateTime.now(), LocalDateTime.now()
            );

            when(userService.getUserById(1L)).thenReturn(testUserResponse);
            when(productService.getProductById(10L)).thenReturn(inactiveProduct);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, request));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(productService, never()).deductStock(any(), any(Integer.class));
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Ném InsufficientStockException khi số lượng tồn kho không đủ")
        void createOrder_InsufficientStock_ShouldThrowException() {
            CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(10L, 100)));

            when(userService.getUserById(1L)).thenReturn(testUserResponse);
            when(productService.getProductById(10L)).thenReturn(testProductResponse1); // Tồn kho chỉ có 10

            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> orderService.createOrder(1L, request));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(productService, never()).deductStock(any(), any(Integer.class));
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Lan truyền ngoại lệ khi người dùng không tồn tại")
        void createOrder_UserNotFound_ShouldPropagateException() {
            CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(10L, 1)));
            when(userService.getUserById(999L)).thenThrow(new ResourceNotFoundException("Người dùng", "id", 999L));

            assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(999L, request));

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ tra cứu chi tiết đơn hàng (getOrderById & getOrderDetails)")
    class GetOrderTests {

        @Test
        @DisplayName("Khách hàng tra cứu thành công đơn hàng thuộc quyền sở hữu của mình")
        void getOrderById_Success_WhenUserIsOwner() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            OrderResponse response = orderService.getOrderById(100L, 1L);

            assertNotNull(response);
            assertEquals(100L, response.id());
            assertEquals(1L, response.userId());
            assertEquals(2, response.orderItems().size());
            verify(orderRepository).findByIdWithDetails(100L);
        }

        @Test
        @DisplayName("Từ chối truy cập và ném FORBIDDEN khi khách hàng xem đơn hàng của người khác")
        void getOrderById_Forbidden_WhenNotOwner() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.getOrderById(100L, 2L)); // testOrder thuộc user 1L, user 2L gọi

            assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi ID đơn hàng không tồn tại")
        void getOrderById_NotFound_ShouldThrowException() {
            when(orderRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(999L, 1L));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Quản trị viên xem chi tiết đơn hàng bất kỳ thành công không bị chặn quyền")
        void getOrderDetails_Admin_Success() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            OrderResponse response = orderService.getOrderDetails(100L);

            assertNotNull(response);
            assertEquals(100L, response.id());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ phân trang danh sách đơn hàng (getUserOrders & getAllOrders)")
    class OrderPaginationTests {

        @Test
        @DisplayName("Lấy danh sách đơn hàng của người dùng không lọc trạng thái thành công")
        void getUserOrders_WithoutStatus_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);

            when(userService.getUserById(1L)).thenReturn(testUserResponse);
            when(orderRepository.findByUserIdWithUser(1L, pageable)).thenReturn(orderPage);

            PagedResponse<OrderResponse> response = orderService.getUserOrders(1L, null, pageable);

            assertNotNull(response);
            assertEquals(1, response.items().size());
            assertEquals(1, response.pagination().totalElements());
        }

        @Test
        @DisplayName("Lấy danh sách đơn hàng của người dùng có lọc theo trạng thái PENDING thành công")
        void getUserOrders_WithStatus_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);

            when(userService.getUserById(1L)).thenReturn(testUserResponse);
            when(orderRepository.findByUserIdAndStatusWithUser(1L, OrderStatus.PENDING, pageable)).thenReturn(orderPage);

            PagedResponse<OrderResponse> response = orderService.getUserOrders(1L, OrderStatus.PENDING, pageable);

            assertNotNull(response);
            assertEquals(1, response.items().size());
            assertEquals(OrderStatus.PENDING, response.items().get(0).status());
        }

        @Test
        @DisplayName("Quản trị viên lấy danh sách toàn bộ đơn hàng có phân trang thành công")
        void getAllOrders_Success() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);

            when(orderRepository.findAllWithUserByStatus(null, pageable)).thenReturn(orderPage);

            PagedResponse<OrderResponse> response = orderService.getAllOrders(null, pageable);

            assertNotNull(response);
            assertEquals(1, response.items().size());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ cập nhật trạng thái đơn hàng (updateOrderStatus - State Machine)")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Chuyển trạng thái hợp lệ: PENDING -> CONFIRMED")
        void updateOrderStatus_PendingToConfirmed_Success() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED));

            assertEquals(OrderStatus.CONFIRMED, response.status());
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Chuyển trạng thái hợp lệ: CONFIRMED -> PROCESSING")
        void updateOrderStatus_ConfirmedToProcessing_Success() {
            testOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.PROCESSING));

            assertEquals(OrderStatus.PROCESSING, response.status());
        }

        @Test
        @DisplayName("Chuyển trạng thái hợp lệ: PROCESSING -> SHIPPED")
        void updateOrderStatus_ProcessingToShipped_Success() {
            testOrder.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.SHIPPED));

            assertEquals(OrderStatus.SHIPPED, response.status());
        }

        @Test
        @DisplayName("Chuyển trạng thái hợp lệ: SHIPPED -> COMPLETED và tự động cập nhật thanh toán PAID")
        void updateOrderStatus_ShippedToCompleted_ShouldUpdatePaymentStatusToPaid() {
            testOrder.setStatus(OrderStatus.SHIPPED);
            testOrder.setPaymentStatus(PaymentStatus.UNPAID);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.COMPLETED));

            assertEquals(OrderStatus.COMPLETED, response.status());
            assertEquals(PaymentStatus.PAID, response.paymentStatus());
        }

        @Test
        @DisplayName("Chuyển trạng thái hợp lệ sang CANCELLED tự động hoàn trả tồn kho đầy đủ")
        void updateOrderStatus_ConfirmedToCancelled_ShouldRestoreStock() {
            testOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

            assertEquals(OrderStatus.CANCELLED, response.status());
            // Hoàn trả tồn kho cho product 10 (quantity 1) và product 20 (quantity 1)
            verify(productService).restoreStock(10L, 1);
            verify(productService).restoreStock(20L, 1);
        }

        @Test
        @DisplayName("Chuyển trạng thái bất hợp lệ: PENDING -> PROCESSING ném BusinessException")
        void updateOrderStatus_InvalidTransition_ShouldThrowException() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.PROCESSING)));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Chuyển trạng thái bất hợp lệ: COMPLETED -> CANCELLED ném BusinessException")
        void updateOrderStatus_CompletedToCancelled_ShouldThrowException() {
            testOrder.setStatus(OrderStatus.COMPLETED);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.CANCELLED)));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(orderRepository, never()).save(any(Order.class));
            verify(productService, never()).restoreStock(any(), any(Integer.class));
        }

        @Test
        @DisplayName("Chuyển trạng thái bất hợp lệ: Trùng trạng thái hiện tại (PENDING -> PENDING) ném BusinessException")
        void updateOrderStatus_SameStatus_ShouldThrowException() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateOrderStatus(100L, new UpdateOrderStatusRequest(OrderStatus.PENDING)));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ hủy đơn hàng (cancelOrder)")
    class CancelOrderTests {

        @Test
        @DisplayName("Khách hàng hủy đơn hàng ở trạng thái PENDING thành công và hoàn trả tồn kho")
        void cancelOrder_Pending_Success() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.cancelOrder(100L, 1L);

            assertEquals(OrderStatus.CANCELLED, response.status());
            verify(productService).restoreStock(10L, 1);
            verify(productService).restoreStock(20L, 1);
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Khách hàng hủy đơn hàng CONFIRMED đã thanh toán PAID thành công, chuyển sang REFUNDED")
        void cancelOrder_ConfirmedAndPaid_ShouldRefund() {
            testOrder.setStatus(OrderStatus.CONFIRMED);
            testOrder.setPaymentStatus(PaymentStatus.PAID);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.cancelOrder(100L, 1L);

            assertEquals(OrderStatus.CANCELLED, response.status());
            assertEquals(PaymentStatus.REFUNDED, response.paymentStatus());
            verify(productService).restoreStock(10L, 1);
            verify(productService).restoreStock(20L, 1);
        }

        @Test
        @DisplayName("Quản trị viên hủy đơn hàng (userId == null) thành công không bị chặn quyền")
        void cancelOrder_AdminCancel_Success() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.cancelOrder(100L, null);

            assertEquals(OrderStatus.CANCELLED, response.status());
            verify(productService).restoreStock(10L, 1);
            verify(productService).restoreStock(20L, 1);
        }

        @Test
        @DisplayName("Từ chối hủy đơn và ném FORBIDDEN khi người hủy không phải chủ sở hữu")
        void cancelOrder_Forbidden_WhenNotOwner() {
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(100L, 2L)); // Đơn của 1L, 2L gửi yêu cầu hủy

            assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
            verify(productService, never()).restoreStock(any(), any(Integer.class));
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Không cho phép hủy khi đơn hàng đang ở trạng thái PROCESSING")
        void cancelOrder_WhenProcessing_ShouldThrowException() {
            testOrder.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(100L, 1L));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(productService, never()).restoreStock(any(), any(Integer.class));
        }

        @Test
        @DisplayName("Không cho phép hủy khi đơn hàng đã COMPLETED")
        void cancelOrder_WhenCompleted_ShouldThrowException() {
            testOrder.setStatus(OrderStatus.COMPLETED);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(100L, 1L));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(productService, never()).restoreStock(any(), any(Integer.class));
        }

        @Test
        @DisplayName("Không cho phép hủy khi đơn hàng đã bị CANCELLED trước đó")
        void cancelOrder_WhenAlreadyCancelled_ShouldThrowException() {
            testOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testOrder));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(100L, 1L));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(productService, never()).restoreStock(any(), any(Integer.class));
        }
    }
}
