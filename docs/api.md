# Tiêu chuẩn Thiết kế REST API & Danh mục Endpoint (API Standards)

> **Giao thức**: HTTP/1.1 & HTTP/2 qua TLS (HTTPS)  
> **Đường dẫn Gốc (Base URL)**: `/api/v1`  
> **Định dạng Dữ liệu (Content-Type)**: `application/json; charset=UTF-8`  
> **Cơ chế Xác thực**: Bearer JWT (`Authorization: Bearer <access_token>`)

---

## 1. Quy chuẩn Thiết kế REST API

1. **Định dạng Đường dẫn Hướng Tài nguyên (Resource-Oriented URI)**:
   - Sử dụng **danh từ số nhiều**, viết thường toàn bộ để định danh tập hợp tài nguyên: `/api/v1/products`, `/api/v1/orders`.
   - Sử dụng biến đường dẫn (Path Variable) để truy cập tài nguyên cụ thể: `/api/v1/products/{id}`.
   - Sử dụng dấu gạch nối (`kebab-case`) cho đường dẫn gồm nhiều từ ghép: `/api/v1/refresh-tokens`, `/api/v1/order-items`.
   - Sử dụng đường dẫn con cho tài nguyên phụ thuộc phân cấp: `/api/v1/products/{productId}/images`.
2. **Quy ước cho các Endpoint Thực hiện Hành động Nghiệp vụ**:
   - Khi một thao tác đại diện cho một bước chuyển đổi trạng thái vòng đời đặc thù thay vì thao tác cập nhật trường thông thường, sử dụng động từ ở đuôi:
     - `POST /api/v1/orders/{id}/cancel` (Hủy đơn hàng)
     - `PATCH /api/v1/notifications/{id}/read` (Đánh dấu đã đọc)
3. **Phương thức HTTP & Ngữ nghĩa**:
   - `GET`: Đọc tài nguyên an toàn và có tính lũy kế (idempotent), tuyệt đối không làm biến đổi dữ liệu trong database.
   - `POST`: Tạo mới tài nguyên hoặc kích hoạt một luồng nghiệp vụ.
   - `PUT`: Cập nhật thay thế toàn bộ tài nguyên.
   - `PATCH`: Cập nhật một phần các trường của tài nguyên.
   - `DELETE`: Xóa hoặc kích hoạt xóa mềm tài nguyên.

---

## 2. Các Mã Trạng thái HTTP Chuẩn (HTTP Status Codes)

| Mã HTTP | Tên Trạng thái | Ngữ nghĩa & Trường hợp Sử dụng |
|---|---|---|
| **200** | `OK` | Phản hồi chuẩn cho các yêu cầu `GET`, `PUT`, hoặc `PATCH` thành công. |
| **201** | `Created` | Trả về khi yêu cầu `POST` tạo mới một tài nguyên thành công, kèm dữ liệu vừa tạo. |
| **204** | `No Content` | Thao tác thành công nhưng không có nội dung body trả về (ví dụ: `DELETE` hoặc đăng xuất). |
| **400** | `Bad Request` | Cú pháp JSON sai, tham số truy vấn không hợp lệ hoặc thiếu header bắt buộc. |
| **401** | `Unauthorized` | Không có token, token hết hạn hoặc chữ ký JWT không hợp lệ. |
| **403** | `Forbidden` | Đã xác thực nhưng người dùng không có quyền hạn truy cập tài nguyên này (ví dụ: khách hàng gọi API admin). |
| **404** | `Not Found` | Không tìm thấy tài nguyên theo ID yêu cầu trong hệ thống. |
| **409** | `Conflict` | Xung đột nghiệp vụ dữ liệu duy nhất (ví dụ: trùng email, trùng tên tài khoản, trùng SKU). |
| **422** | `Unprocessable Entity` | Dữ liệu đúng cấu trúc JSON nhưng vi phạm validation hoặc quy tắc bất biến của nghiệp vụ. |
| **500** | `Internal Server Error`| Lỗi hệ thống ngoài ý muốn trong quá trình runtime; chi tiết lỗi được ghi log nội bộ và trả về thông báo chung an toàn. |

---

## 3. Cấu trúc Phản hồi Chuẩn (Standard Response Wrappers)

Mọi phản hồi JSON phát sinh từ backend đều được chuẩn hóa cấu trúc để tầng frontend dễ dàng tích hợp và hiển thị thông báo tiếng Việt.

### 3.1 Phản hồi Thành công Chuẩn: `ApiResponse<T>`

```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": {
    "id": 1,
    "name": "Bàn phím Cơ Không dây Gaming RGB",
    "price": 1250000.00,
    "stockQuantity": 50,
    "status": "ACTIVE"
  }
}
```

### 3.2 Phản hồi Lỗi Chuẩn: `ApiErrorResponse`

```json
{
  "success": false,
  "message": "Sản phẩm không tồn tại",
  "code": "PRODUCT_NOT_FOUND"
}
```

Trong trường hợp lỗi do **Kiểm tra Hợp lệ Dữ liệu Đầu vào (Validation Failed)**, phản hồi sẽ bổ sung chi tiết từng trường bị lỗi:

```json
{
  "success": false,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "code": "VALIDATION_FAILED",
  "errors": [
    {
      "field": "price",
      "rejectedValue": -50000,
      "message": "Giá sản phẩm phải lớn hơn 0"
    },
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Tên sản phẩm không được để trống"
    }
  ]
}
```

### Các Mã Lỗi Hệ thống Thường Gặp (Error Codes):
- `VALIDATION_FAILED`: Dữ liệu đầu vào vi phạm validation.
- `RESOURCE_NOT_FOUND`: Không tìm thấy tài nguyên yêu cầu.
- `DUPLICATE_RESOURCE`: Dữ liệu đã tồn tại (trùng email, username).
- `UNAUTHORIZED_ACCESS`: Chưa đăng nhập hoặc token không hợp lệ.
- `ACCESS_DENIED`: Không đủ quyền hạn truy cập tài nguyên.
- `INSUFFICIENT_STOCK`: Số lượng tồn kho không đủ để đáp ứng đơn hàng.
- `INVALID_ORDER_STATE`: Chuyển trạng thái đơn hàng không hợp lệ.
- `INTERNAL_SERVER_ERROR`: Lỗi hệ thống nội bộ máy chủ.

---

## 4. Định dạng Phân trang & Sắp xếp (Pagination & Sorting)

Tất cả các API lấy danh sách nhiều bản ghi đều hỗ trợ cơ chế phân trang và sắp xếp dữ liệu thông qua query parameters.

### 4.1 Tham số Truy vấn Phân trang (Request Query Parameters)
- `page`: Chỉ số trang cần lấy (đếm từ `0`, mặc định: `0`).
- `size`: Số lượng phần tử trên mỗi trang (số nguyên, mặc định: `20`, tối đa: `100`).
- `sort`: Trường cần sắp xếp và chiều sắp xếp theo cú pháp `<tên_trường>,<asc|desc>` (ví dụ: `createdAt,desc` hoặc `price,asc`). Có thể truyền nhiều tham số sắp xếp.

**Ví dụ Request**:  
`GET /api/v1/products?page=0&size=20&sort=createdAt,desc&categoryId=2&minPrice=500000&maxPrice=2000000`

### 4.2 Cấu trúc Phản hồi Phân trang Chuẩn: `PagedResponse<T>`

```json
{
  "success": true,
  "message": "Lấy danh sách sản phẩm thành công",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "Bàn phím Cơ Không dây Gaming RGB",
        "price": 1250000.00,
        "stockQuantity": 50,
        "status": "ACTIVE"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 142,
      "totalPages": 8,
      "isFirst": true,
      "isLast": false,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

---

## 5. Danh mục Endpoint Toàn diện theo Module

### 5.1 Module Xác thực & Phân quyền (`/api/v1/auth`)

| Phương thức | Đường dẫn Endpoint | Quyền hạn | Mô tả Chức năng |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Công khai | Đăng ký tài khoản khách hàng mới (mặc định vai trò `ROLE_CUSTOMER`) |
| `POST` | `/api/v1/auth/login` | Công khai | Đăng nhập tài khoản, trả về Access Token (15m) và Refresh Token (7d) |
| `POST` | `/api/v1/auth/refresh` | Công khai | Đổi Refresh Token hợp lệ lấy Access Token mới và Refresh Token mới (RTR) |
| `POST` | `/api/v1/auth/logout` | Đã đăng nhập | Thu hồi và vô hiệu hóa Refresh Token hiện tại của người dùng |

### 5.2 Module Quản lý Người dùng (`/api/v1/users`)

| Phương thức | Đường dẫn Endpoint | Quyền hạn | Mô tả Chức năng |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Đã đăng nhập | Xem thông tin hồ sơ của tài khoản đang đăng nhập |
| `PUT` | `/api/v1/users/me` | Đã đăng nhập | Cập nhật thông tin hồ sơ cá nhân |
| `GET` | `/api/v1/admin/users` | Admin | Tìm kiếm, lọc và phân trang toàn bộ danh sách người dùng trong hệ thống |
| `GET` | `/api/v1/admin/users/{id}` | Admin | Xem thông tin chi tiết một người dùng kèm vai trò phân quyền |
| `PATCH` | `/api/v1/admin/users/{id}/status`| Admin | Khóa tài khoản hoặc mở khóa kích hoạt lại tài khoản người dùng |

### 5.3 Module Danh mục & Sản phẩm (`/api/v1/products`, `/api/v1/categories`)

| Phương thức | Đường dẫn Endpoint | Quyền hạn | Mô tả Chức năng |
|---|---|---|---|
| `GET` | `/api/v1/categories` | Công khai | Lấy danh sách toàn bộ danh mục sản phẩm (được lưu cache Redis) |
| `POST` | `/api/v1/categories` | Admin | Tạo danh mục sản phẩm mới (tự động xóa cache danh mục) |
| `PUT` | `/api/v1/categories/{id}` | Admin | Chỉnh sửa danh mục sản phẩm (xóa cache danh mục) |
| `GET` | `/api/v1/products` | Công khai | Tìm kiếm, lọc đa tiêu chí và phân trang sản phẩm đang bán (`ACTIVE`) |
| `GET` | `/api/v1/products/{id}` | Công khai | Xem chi tiết sản phẩm theo ID (được lưu cache Redis) |
| `POST` | `/api/v1/admin/products` | Admin | Tạo sản phẩm mới kèm số lượng tồn kho ban đầu |
| `PUT` | `/api/v1/admin/products/{id}` | Admin | Cập nhật thông tin và giá sản phẩm (tự động xóa cache sản phẩm) |
| `PATCH`| `/api/v1/admin/products/{id}/stock`| Admin/Staff | Cập nhật điều chỉnh số lượng tồn kho của sản phẩm |
| `DELETE`| `/api/v1/admin/products/{id}`| Admin | Xóa mềm sản phẩm (chuyển trạng thái sang `INACTIVE`) |

### 5.4 Module Đơn hàng & Thanh toán (`/api/v1/orders`)

| Phương thức | Đường dẫn Endpoint | Quyền hạn | Mô tả Chức năng |
|---|---|---|---|
| `POST` | `/api/v1/orders` | Khách hàng | Đặt hàng (Checkout): trừ tồn kho, chụp snapshot giá, lưu đơn, phát sinh sự kiện |
| `GET` | `/api/v1/orders` | Khách hàng | Lấy danh sách lịch sử đơn hàng của người dùng hiện tại (có phân trang) |
| `GET` | `/api/v1/orders/{id}` | Khách hàng | Xem chi tiết đơn hàng kèm các mục sản phẩm snapshot (chỉ xem được đơn của mình) |
| `POST` | `/api/v1/orders/{id}/cancel` | Khách hàng | Hủy đơn hàng (chỉ khi `PENDING` hoặc `CONFIRMED`), tự động hoàn trả tồn kho |
| `GET` | `/api/v1/admin/orders` | Admin/Staff | Xem danh sách toàn bộ đơn hàng của hệ thống có phân trang và lọc trạng thái |
| `PATCH`| `/api/v1/admin/orders/{id}/status`| Admin/Staff | Cập nhật tiến độ trạng thái đơn hàng (`CONFIRMED` $\rightarrow$ `PROCESSING` $\rightarrow$ `SHIPPED` $\rightarrow$ `COMPLETED`) |

### 5.5 Module Vận chuyển (`/api/v1/shipments`)

| Phương thức | Đường dẫn Endpoint | Quyền hạn | Mô tả Chức năng |
|---|---|---|---|
| `GET` | `/api/v1/shipments/order/{orderId}`| Khách hàng | Tra cứu thông tin vận đơn và mã theo dõi đơn hàng của khách |
| `POST` | `/api/v1/shipments/webhook` | Đối tác/Public | Webhook tiếp nhận thông báo trạng thái giao vận từ đối tác vận chuyển |
| `PATCH`| `/api/v1/admin/shipments/{id}/status`| Admin/Staff | Cập nhật thủ công trạng thái vận đơn trong trường hợp cần đối soát |

### 5.6 Module Hộp thư Thông báo (`/api/v1/notifications`)

| Phương thức | Đường dẫn Endpoint | Quyền hạn | Mô tả Chức năng |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Khách hàng | Lấy danh sách thông báo của tài khoản đang đăng nhập (có phân trang) |
| `GET` | `/api/v1/notifications/unread-count`| Khách hàng | Đếm số lượng thông báo chưa đọc của tài khoản |
| `PATCH`| `/api/v1/notifications/{id}/read` | Khách hàng | Đánh dấu một thông báo cụ thể là đã đọc |
| `POST` | `/api/v1/notifications/read-all` | Khách hàng | Đánh dấu tất cả thông báo trong hộp thư là đã đọc |
