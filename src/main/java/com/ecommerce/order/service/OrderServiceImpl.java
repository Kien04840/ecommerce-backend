package com.ecommerce.order.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderItemResponse;
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
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp triển khai các nghiệp vụ cho {@link OrderService}.
 * <p>
 * Thực thi các nguyên tắc cốt lõi:
 * <ul>
 *     <li>Quy trình Checkout nguyên tử trong cùng một transaction.</li>
 *     <li>Đóng băng snapshot giá và tính toán tổng tiền bằng {@link BigDecimal}.</li>
 *     <li>Chống Race Condition và Deadlock khi trừ tồn kho đồng thời nhiều sản phẩm.</li>
 *     <li>Tuân thủ nghiêm ngặt State Machine chuyển đổi trạng thái đơn hàng.</li>
 *     <li>Xác thực quyền sở hữu đơn hàng (Ownership Authorization).</li>
 *     <li>Tự động hoàn trả tồn kho nguyên tử khi đơn hàng bị hủy bỏ.</li>
 *     <li>Không làm lộ thực thể JPA ra ngoài tầng Service.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final UserService userService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("Bắt đầu quy trình tạo đơn hàng (Checkout) cho người dùng ID: {}", userId);

        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã người dùng không được để trống");
        }

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Đơn hàng phải có ít nhất một mặt hàng");
        }

        // Kiểm tra người dùng tồn tại và hợp lệ trong hệ thống
        userService.getUserById(userId);

        // Gom nhóm số lượng theo productId để xử lý trường hợp cùng sản phẩm xuất hiện nhiều lần trong giỏ hàng
        Map<Long, Integer> itemQuantities = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            if (item.productId() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã sản phẩm không được để trống");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Số lượng đặt mua phải lớn hơn 0");
            }
            itemQuantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        // Sắp xếp danh sách productId tăng dần trước khi thao tác khóa và trừ tồn kho để ngăn chặn Deadlock giữa các transaction đồng thời
        List<Long> sortedProductIds = itemQuantities.keySet().stream().sorted().toList();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItemSnapshot> snapshots = new ArrayList<>();
        Map<Long, String> productNames = new HashMap<>();

        for (Long productId : sortedProductIds) {
            int requestedQuantity = itemQuantities.get(productId);

            // Truy vấn thông tin sản phẩm từ ProductService
            ProductResponse product = productService.getProductById(productId);

            // Kiểm tra trạng thái sản phẩm phải là ACTIVE mới được phép mua
            if (product.status() != ProductStatus.ACTIVE) {
                log.warn("Đặt hàng thất bại: Sản phẩm ID: {} ('{}') không ở trạng thái ACTIVE (Trạng thái: {})",
                    productId, product.name(), product.status());
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                    String.format("Sản phẩm '%s' hiện không mở bán để đặt mua", product.name()));
            }

            // Kiểm tra tồn kho trước khi trừ
            if (product.stockQuantity() < requestedQuantity) {
                log.warn("Đặt hàng thất bại: Sản phẩm ID: {} chỉ còn {}, yêu cầu {}",
                    productId, product.stockQuantity(), requestedQuantity);
                throw new InsufficientStockException(productId, requestedQuantity, product.stockQuantity());
            }

            // Trừ tồn kho nguyên tử có Pessimistic Write Lock (SELECT ... FOR UPDATE)
            productService.deductStock(productId, requestedQuantity);

            // Đóng băng snapshot giá tại thời điểm đặt hàng và tính toán thành tiền
            BigDecimal unitPrice = product.price();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(requestedQuantity)).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal);

            snapshots.add(new OrderItemSnapshot(productId, product.name(), requestedQuantity, unitPrice, subtotal));
            productNames.put(productId, product.name());
        }

        // Kiểm tra tổng tiền đơn hàng phải lớn hơn 0
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tổng giá trị đơn hàng phải lớn hơn 0");
        }

        // Tạo thực thể Order liên kết với User reference (không cần query thêm User)
        User userRef = entityManager.getReference(User.class, userId);
        Order order = Order.builder()
            .user(userRef)
            .totalAmount(totalAmount)
            .status(OrderStatus.PENDING)
            .paymentStatus(PaymentStatus.UNPAID)
            .build();

        // Tạo các dòng OrderItem snapshot gắn vào Order
        for (OrderItemSnapshot snapshot : snapshots) {
            Product productRef = entityManager.getReference(Product.class, snapshot.productId());
            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(productRef)
                .quantity(snapshot.quantity())
                .unitPrice(snapshot.unitPrice())
                .subtotal(snapshot.subtotal())
                .build();
            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Khởi tạo đơn hàng thành công với ID: {}, Tổng tiền: {}", savedOrder.getId(), savedOrder.getTotalAmount());

        // Ánh xạ sang OrderResponse kèm tên sản phẩm snapshot
        List<OrderItemResponse> itemResponses = savedOrder.getOrderItems().stream()
            .map(item -> new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                productNames.getOrDefault(item.getProduct().getId(), "Sản phẩm #" + item.getProduct().getId()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
            ))
            .toList();

        return new OrderResponse(
            savedOrder.getId(),
            userId,
            savedOrder.getTotalAmount(),
            savedOrder.getStatus(),
            savedOrder.getPaymentStatus(),
            savedOrder.getOrderDate(),
            savedOrder.getUpdatedAt(),
            itemResponses
        );
    }

    @Override
    public OrderResponse getOrderById(Long orderId, Long userId) {
        log.debug("Truy vấn chi tiết đơn hàng ID: {} của người dùng ID: {}", orderId, userId);

        Order order = orderRepository.findByIdWithDetails(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        // Kiểm tra quyền sở hữu đơn hàng (Ownership Authorization)
        if (userId != null && !order.getUser().getId().equals(userId)) {
            log.warn("Từ chối truy cập: Người dùng ID: {} không có quyền xem đơn hàng ID: {}", userId, orderId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderDetails(Long orderId) {
        log.debug("Quản trị viên truy vấn chi tiết đơn hàng ID: {}", orderId);

        Order order = orderRepository.findByIdWithDetails(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        return mapToOrderResponse(order);
    }

    @Override
    public PagedResponse<OrderResponse> getUserOrders(Long userId, OrderStatus status, Pageable pageable) {
        log.debug("Lấy danh sách đơn hàng của người dùng ID: {}, trạng thái: {}", userId, status);

        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã người dùng không được để trống");
        }

        // Xác thực người dùng tồn tại
        userService.getUserById(userId);

        Pageable effectivePageable = (pageable != null) ? pageable : PageRequest.of(0, 20);

        Page<Order> orderPage = (status != null)
            ? orderRepository.findByUserIdAndStatusWithUser(userId, status, effectivePageable)
            : orderRepository.findByUserIdWithUser(userId, effectivePageable);

        List<OrderResponse> content = orderPage.getContent().stream()
            .map(this::mapToOrderResponse)
            .toList();

        return PagedResponse.of(content, orderPage);
    }

    @Override
    public PagedResponse<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        log.debug("Quản trị viên truy vấn danh sách toàn bộ đơn hàng, lọc theo trạng thái: {}", status);

        Pageable effectivePageable = (pageable != null) ? pageable : PageRequest.of(0, 20);

        Page<Order> orderPage = orderRepository.findAllWithUserByStatus(status, effectivePageable);

        List<OrderResponse> content = orderPage.getContent().stream()
            .map(this::mapToOrderResponse)
            .toList();

        return PagedResponse.of(content, orderPage);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.info("Cập nhật trạng thái đơn hàng ID: {} sang {}", orderId, request != null ? request.status() : null);

        if (request == null || request.status() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Trạng thái đơn hàng không được để trống");
        }

        Order order = orderRepository.findByIdWithDetails(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.status();

        // Kiểm tra tính hợp lệ của bước chuyển trạng thái theo State Machine
        validateStatusTransition(currentStatus, targetStatus);

        // Nếu chuyển sang CANCELLED, tự động hoàn trả tồn kho nguyên tử cho các sản phẩm và hoàn tiền nếu đã thanh toán
        if (targetStatus == OrderStatus.CANCELLED) {
            restoreStockForOrder(order);
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Đơn hàng ID: {} đã thanh toán trước đó, chuyển paymentStatus sang REFUNDED", orderId);
            }
        }

        // Nếu chuyển sang COMPLETED và chưa thanh toán, cập nhật trạng thái thanh toán sang PAID
        if (targetStatus == OrderStatus.COMPLETED && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            log.info("Đơn hàng ID: {} đã hoàn tất giao hàng, tự động cập nhật paymentStatus sang PAID", orderId);
        }

        order.setStatus(targetStatus);
        Order updatedOrder = orderRepository.save(order);
        log.info("Cập nhật trạng thái đơn hàng ID: {} thành công từ {} sang {}", orderId, currentStatus, targetStatus);

        return mapToOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        log.info("Xử lý yêu cầu hủy đơn hàng ID: {} từ người dùng ID: {}", orderId, userId);

        Order order = orderRepository.findByIdWithDetails(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        // Kiểm tra quyền sở hữu nếu là khách hàng gọi API hủy
        if (userId != null && !order.getUser().getId().equals(userId)) {
            log.warn("Từ chối hủy đơn: Người dùng ID: {} không phải chủ đơn hàng ID: {}", userId, orderId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng này");
        }

        // Quy tắc nghiệp vụ: Chỉ cho phép hủy đơn khi trạng thái là PENDING hoặc CONFIRMED
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            if (currentStatus == OrderStatus.CANCELLED) {
                log.warn("Đơn hàng ID: {} đã bị hủy trước đó", orderId);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Đơn hàng đã bị hủy trước đó");
            } else if (currentStatus == OrderStatus.COMPLETED) {
                log.warn("Đơn hàng ID: {} đã hoàn tất, không được phép hủy", orderId);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể hủy đơn hàng đã hoàn tất");
            } else {
                log.warn("Đơn hàng ID: {} đang ở trạng thái {}, không thể hủy qua kênh thông thường", orderId, currentStatus);
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Đơn hàng đang trong quá trình đóng gói hoặc vận chuyển, không thể tự hủy");
            }
        }

        // Hoàn trả tồn kho nguyên tử cho từng sản phẩm
        restoreStockForOrder(order);

        // Cập nhật trạng thái đơn hàng sang CANCELLED
        order.setStatus(OrderStatus.CANCELLED);

        // Nếu đơn hàng đã thanh toán, chuyển sang REFUNDED
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            log.info("Đơn hàng ID: {} đã thanh toán trước đó, chuyển paymentStatus sang REFUNDED", orderId);
        }

        Order cancelledOrder = orderRepository.save(order);
        log.info("Hủy đơn hàng ID: {} thành công, đã hoàn trả tồn kho đầy đủ", orderId);

        return mapToOrderResponse(cancelledOrder);
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH RIÊNG (PRIVATE HELPERS)
    // ==========================================

    /**
     * Kiểm tra tính hợp lệ của chuyển đổi trạng thái đơn hàng theo State Machine.
     * <pre>
     * PENDING    -> CONFIRMED, CANCELLED
     * CONFIRMED  -> PROCESSING, CANCELLED
     * PROCESSING -> SHIPPED
     * SHIPPED    -> COMPLETED
     * COMPLETED  -> (Terminal - không chuyển tiếp)
     * CANCELLED  -> (Terminal - không chuyển tiếp)
     * </pre>
     */
    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Đơn hàng hiện tại đã ở trạng thái " + to);
        }

        boolean isValid = switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPED;
            case SHIPPED -> to == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!isValid) {
            log.warn("Chuyển trạng thái đơn hàng không hợp lệ: từ {} sang {}", from, to);
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                String.format("Không thể chuyển trạng thái đơn hàng từ %s sang %s", from, to));
        }
    }

    /**
     * Hoàn trả tồn kho nguyên tử cho toàn bộ các mục sản phẩm trong đơn hàng.
     * Sắp xếp theo productId tăng dần để phòng chống Deadlock khi hoàn trả.
     */
    private void restoreStockForOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        List<OrderItem> sortedItems = order.getOrderItems().stream()
            .sorted(Comparator.comparing(item -> item.getProduct().getId()))
            .toList();

        for (OrderItem item : sortedItems) {
            Long productId = item.getProduct().getId();
            int quantity = item.getQuantity();
            log.info("Hoàn trả tồn kho cho sản phẩm ID: {}, số lượng: {} từ đơn hàng ID: {}",
                productId, quantity, order.getId());
            productService.restoreStock(productId, quantity);
        }
    }

    /**
     * Ánh xạ thực thể {@link Order} sang DTO phản hồi {@link OrderResponse}.
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = (order.getOrderItems() != null)
            ? order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                    item.getId(),
                    item.getProduct() != null ? item.getProduct().getId() : null,
                    item.getProduct() != null ? item.getProduct().getName() : null,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()
                ))
                .toList()
            : Collections.emptyList();

        return new OrderResponse(
            order.getId(),
            order.getUser() != null ? order.getUser().getId() : null,
            order.getTotalAmount(),
            order.getStatus(),
            order.getPaymentStatus(),
            order.getOrderDate(),
            order.getUpdatedAt(),
            itemResponses
        );
    }

    /**
     * Record nội bộ lưu giữ thông tin snapshot tạm thời trong quá trình checkout.
     */
    private record OrderItemSnapshot(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}
}

