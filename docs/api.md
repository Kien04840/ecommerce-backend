# REST API Standards & Endpoint Catalog

> **Protocol**: HTTP/1.1 & HTTP/2 over TLS (HTTPS)  
> **Base URL Path**: `/api/v1`  
> **Content-Type**: `application/json; charset=UTF-8`  
> **Authentication**: Bearer JWT (`Authorization: Bearer <token>`)

---

## 1. REST API Design Conventions

1. **Resource-Oriented URIs**:
   - Use **plural nouns** in lowercase to name collections: `/api/v1/products`, `/api/v1/orders`.
   - Use path variables for single entity access: `/api/v1/products/{id}`.
   - Use hyphen-delimited lowercase (`kebab-case`) for multi-word paths: `/api/v1/refresh-tokens`, `/api/v1/order-items`.
   - Use sub-resources for nested aggregates: `/api/v1/products/{productId}/images`.
2. **Action Endpoints (RPC-style on Resources)**:
   - When an operation is an explicit business lifecycle transition rather than a direct entity property edit, use a sub-resource verb:
     - `POST /api/v1/orders/{id}/cancel`
     - `PATCH /api/v1/notifications/{id}/read`
3. **HTTP Methods & Semantics**:
   - `GET`: Safe, idempotent retrieval. Must not alter database state.
   - `POST`: Non-idempotent creation or execution of business actions.
   - `PUT`: Idempotent full replacement of an existing resource.
   - `PATCH`: Partial update of specific fields of a resource.
   - `DELETE`: Idempotent removal (or soft-delete).

---

## 2. HTTP Status Codes

| Code | Status | Meaning & Standard Usage |
|---|---|---|
| **200** | `OK` | Standard response for successful `GET`, `PUT`, or `PATCH` operations. |
| **201** | `Created` | Returned for successful `POST` operations creating a resource. Includes created body. |
| **204** | `No Content` | Successful request where no body is returned (e.g. `DELETE` or logout). |
| **400** | `Bad Request` | Malformed JSON syntax, invalid query parameters, or missing headers. |
| **401** | `Unauthorized` | Missing, expired, or invalid JWT access token. |
| **402** | `Payment Required` | Payment processing failed or payment balance insufficient. |
| **403** | `Forbidden` | Authenticated user lacks required role/permission (e.g. customer accessing admin route). |
| **404** | `Not Found` | The requested resource ID does not exist in the database. |
| **409** | `Conflict` | Business duplicate conflict (e.g. duplicate email, unique SKU collision). |
| **422** | `Unprocessable Entity` | JSON is well-formed but violates Bean Validation or business invariants. |
| **500** | `Internal Server Error`| Unexpected runtime server exception. Details logged internally; sanitized message returned. |

---

## 3. Standard Response Envelopes

Every JSON response emitted by the backend is wrapped in a consistent structure.

### 3.1 Standard Success Envelope: `ApiResponse<T>`

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 101,
    "name": "Wireless Mechanical Keyboard",
    "sku": "KB-WL-RGB-01",
    "basePrice": 89.99,
    "stock": 50,
    "status": "ACTIVE"
  },
  "timestamp": "2026-10-02T12:00:00Z"
}
```

### 3.2 Standard Error Envelope: `ApiErrorResponse`

```json
{
  "success": false,
  "errorCode": "VALIDATION_FAILED",
  "message": "Input validation failed for one or more fields",
  "details": [
    {
      "field": "basePrice",
      "rejectedValue": -10.0,
      "message": "Price must be strictly positive"
    },
    {
      "field": "sku",
      "rejectedValue": "",
      "message": "SKU cannot be blank"
    }
  ],
  "timestamp": "2026-10-02T12:00:00Z",
  "path": "/api/v1/products"
}
```

Common standard error codes:
- `VALIDATION_FAILED` (422)
- `RESOURCE_NOT_FOUND` (404)
- `DUPLICATE_RESOURCE` (409)
- `UNAUTHORIZED_ACCESS` (401)
- `ACCESS_DENIED` (403)
- `INSUFFICIENT_STOCK` (422)
- `INVALID_ORDER_STATE` (422)
- `INTERNAL_SERVER_ERROR` (500)

---

## 4. Pagination, Sorting & Filtering Format

All collection queries support standardized pagination and sorting via query parameters.

### 4.1 Request Query Parameters
- `page`: Page index (0-based integer, default: `0`).
- `size`: Elements per page (integer, default: `20`, maximum: `100`).
- `sort`: Sort field and direction in format `<property>,<asc|desc>` (e.g., `createdAt,desc` or `basePrice,asc`). Multiple sort parameters are permitted.

**Example Request**:  
`GET /api/v1/products?page=0&size=20&sort=createdAt,desc&categoryId=3&minPrice=50&maxPrice=150`

### 4.2 Standard Paginated Envelope: `PagedResponse<T>`

```json
{
  "success": true,
  "message": "Product list retrieved successfully",
  "data": {
    "items": [
      {
        "id": 101,
        "name": "Wireless Mechanical Keyboard",
        "sku": "KB-WL-RGB-01",
        "basePrice": 89.99,
        "salePrice": 79.99,
        "stock": 50,
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
  },
  "timestamp": "2026-10-02T12:00:00Z"
}
```

---

## 5. Complete API Route Catalog

### 5.1 Authentication Module (`/api/v1/auth`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register customer account (email, password, name) |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user, returns Access Token (15m) + Refresh Token (7d) |
| `POST` | `/api/v1/auth/refresh` | Public | Exchange valid Refresh Token for new Access & rotated Refresh Token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke user's active refresh token |

### 5.2 User Management Module (`/api/v1/users`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Authenticated | Retrieve authenticated user's profile details |
| `PUT` | `/api/v1/users/me` | Authenticated | Update authenticated user's profile |
| `GET` | `/api/v1/admin/users` | Admin | Search & paginate all users |
| `GET` | `/api/v1/admin/users/{id}` | Admin | Get full user details including assigned roles |
| `PATCH` | `/api/v1/admin/users/{id}/roles` | Admin | Update user roles (assign/revoke roles) |
| `PATCH` | `/api/v1/admin/users/{id}/status`| Admin | Suspend or reactivate user account |

### 5.3 Product & Category Module (`/api/v1/products`, `/api/v1/categories`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/categories` | Public | Get hierarchical tree of active categories (Redis cached) |
| `POST` | `/api/v1/categories` | Admin | Create new category (evicts category cache) |
| `PUT` | `/api/v1/categories/{id}` | Admin | Update category details (evicts cache) |
| `DELETE` | `/api/v1/categories/{id}` | Admin | Soft delete category |
| `GET` | `/api/v1/products` | Public | Dynamic search, filter & paginate active products |
| `GET` | `/api/v1/products/{id}` | Public | Get single product details by ID (Redis cached) |
| `GET` | `/api/v1/products/slug/{slug}`| Public | Get single product details by URL slug |
| `POST` | `/api/v1/admin/products` | Admin | Create product with initial inventory |
| `PUT` | `/api/v1/admin/products/{id}` | Admin | Update product details & price (evicts Redis cache) |
| `PATCH`| `/api/v1/admin/products/{id}/stock`| Admin/Staff | Adjust inventory quantity |
| `DELETE`| `/api/v1/admin/products/{id}`| Admin | Soft delete product (`is_deleted = true`) |

### 5.4 Order & Checkout Module (`/api/v1/orders`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/orders` | Customer | Checkout: create order, deduct stock atomically, emit event |
| `GET` | `/api/v1/orders` | Customer | Paginated list of current user's past orders |
| `GET` | `/api/v1/orders/{id}` | Customer | Get order detail with item snapshots (must belong to user) |
| `POST` | `/api/v1/orders/{id}/cancel` | Customer | Cancel order (only if `PENDING` or `CONFIRMED`), restores stock |
| `GET` | `/api/v1/admin/orders` | Admin/Staff | Paginated order list with multi-status filters |
| `GET` | `/api/v1/admin/orders/{id}` | Admin/Staff | Get full order details for administrative inspection |
| `PATCH`| `/api/v1/admin/orders/{id}/status`| Admin/Staff | Advance order status (`CONFIRMED` -> `PROCESSING`, etc.) |

### 5.5 Logistics & Shipping Module (`/api/v1/shipments`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/shipments/order/{orderId}` | Customer | Get shipping status and tracking code for an order |
| `POST` | `/api/v1/shipments/webhook` | External/Public | Webhook callback from shipping carrier updating delivery status |
| `PATCH`| `/api/v1/admin/shipments/{id}/status` | Admin/Staff | Manually update carrier tracking status |

### 5.6 Notification Module (`/api/v1/notifications`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Customer | Get paginated notification list for current user |
| `GET` | `/api/v1/notifications/unread-count`| Customer | Get total count of unread notifications |
| `PATCH`| `/api/v1/notifications/{id}/read` | Customer | Mark notification as `READ` |
| `POST` | `/api/v1/notifications/read-all` | Customer | Mark all notifications of current user as `READ` |
