package com.ecommerce.order.service;

import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface định nghĩa các nghiệp vụ cốt lõi cho module Đơn hàng (Order).
 * <p>
 * Cung cấp các thao tác trong quy trình Checkout, đóng băng snapshot giá,
 * tự động tính toán tổng tiền, kiểm soát State Machine vòng đời đơn hàng,
 * phân quyền sở hữu đơn hàng (Ownership Authorization) và hoàn trả tồn kho khi hủy đơn.
 */
public interface OrderService {

    /**
     * Khởi tạo đơn hàng mới (quy trình Checkout).
     * <p>
     * Thực hiện kiểm tra tính hợp lệ của người dùng, sản phẩm, trừ tồn kho nguyên tử,
     * đóng băng snapshot giá và tính toán tổng tiền trong cùng một transaction.
     *
     * @param userId  mã định danh người dùng đặt hàng
     * @param request dữ liệu yêu cầu tạo đơn hàng chứa danh sách mặt hàng
     * @return thông tin chi tiết đơn hàng vừa khởi tạo
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * Lấy thông tin chi tiết đơn hàng của người dùng (có kiểm tra quyền sở hữu).
     *
     * @param orderId mã định danh đơn hàng
     * @param userId  mã định danh người dùng yêu cầu
     * @return thông tin chi tiết đơn hàng kèm danh sách mặt hàng snapshot
     */
    OrderResponse getOrderById(Long orderId, Long userId);

    /**
     * Lấy thông tin chi tiết đơn hàng dành cho Quản trị viên / Nhân viên (không kiểm tra quyền sở hữu người dùng).
     *
     * @param orderId mã định danh đơn hàng
     * @return thông tin chi tiết đơn hàng đầy đủ
     */
    OrderResponse getOrderDetails(Long orderId);

    /**
     * Lấy danh sách lịch sử đơn hàng của người dùng có phân trang và lọc theo trạng thái.
     *
     * @param userId   mã định danh người dùng
     * @param status   trạng thái đơn hàng cần lọc (có thể null để lấy tất cả)
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách đơn hàng của người dùng
     */
    PagedResponse<OrderResponse> getUserOrders(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Lấy danh sách tất cả đơn hàng trong hệ thống có phân trang và lọc theo trạng thái (dành cho Admin/Staff).
     *
     * @param status   trạng thái đơn hàng cần lọc (có thể null để lấy tất cả)
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang danh sách tất cả đơn hàng
     */
    PagedResponse<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable);

    /**
     * Cập nhật trạng thái tiến độ của đơn hàng theo State Machine (dành cho Admin/Staff).
     *
     * @param orderId mã định danh đơn hàng
     * @param request yêu cầu cập nhật trạng thái mới
     * @return thông tin đơn hàng sau khi cập nhật trạng thái
     */
    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    /**
     * Hủy đơn hàng và tự động hoàn trả tồn kho nguyên tử.
     * <p>
     * Chỉ cho phép hủy khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED.
     * Kiểm tra quyền sở hữu nếu người gọi là khách hàng (userId != null).
     *
     * @param orderId mã định danh đơn hàng cần hủy
     * @param userId  mã định danh người dùng yêu cầu hủy (có thể null nếu là Admin thao tác)
     * @return thông tin đơn hàng sau khi đã chuyển sang trạng thái CANCELLED
     */
    OrderResponse cancelOrder(Long orderId, Long userId);
}

