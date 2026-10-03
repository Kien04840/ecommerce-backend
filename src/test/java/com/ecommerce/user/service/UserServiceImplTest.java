package com.ecommerce.user.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PagedResponse;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UpdateUserStatusRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho {@link UserServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private Role testRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        testRole = Role.builder()
            .name("ROLE_CUSTOMER")
            .build();
        ReflectionTestUtils.setField(testRole, "id", 1L);

        testUser = User.builder()
            .username("nguyenvana")
            .email("nguyenvana@example.com")
            .password("$2a$12$hashedPasswordPlaceholder")
            .enabled(true)
            .role(testRole)
            .build();
        ReflectionTestUtils.setField(testUser, "id", 10L);
        ReflectionTestUtils.setField(testUser, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("Nghiệp vụ lấy thông tin người dùng theo ID (getUserById)")
    class GetUserByIdTests {

        @Test
        @DisplayName("Lấy thông tin thành công khi User tồn tại và ánh xạ Role chính xác")
        void getUserById_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

            UserResponse response = userService.getUserById(10L);

            assertNotNull(response);
            assertEquals(10L, response.id());
            assertEquals("nguyenvana", response.username());
            assertEquals("nguyenvana@example.com", response.email());
            assertTrue(response.enabled());
            assertNotNull(response.role());
            assertEquals(1L, response.role().id());
            assertEquals("ROLE_CUSTOMER", response.role().name());
            assertNotNull(response.createdAt());

            verify(userRepository).findById(10L);
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi ID người dùng không tồn tại")
        void getUserById_NotFound_ShouldThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(999L));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
            verify(userRepository).findById(999L);
        }

        @Test
        @DisplayName("Bảo vệ credential: Mật khẩu không xuất hiện trong UserResponse")
        void getUserById_CredentialProtection() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

            UserResponse response = userService.getUserById(10L);

            assertNotNull(response);
            assertFalse(response.toString().contains("$2a$12$hashedPasswordPlaceholder"));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ tra cứu người dùng (Lookup by Username / Email)")
    class LookupUserTests {

        @Test
        @DisplayName("Tra cứu theo username thành công")
        void getUserByUsername_Success() {
            when(userRepository.findByUsername("nguyenvana")).thenReturn(Optional.of(testUser));

            UserResponse response = userService.getUserByUsername("nguyenvana");

            assertNotNull(response);
            assertEquals("nguyenvana", response.username());
            assertEquals(10L, response.id());
            verify(userRepository).findByUsername("nguyenvana");
        }

        @Test
        @DisplayName("Tra cứu theo username thất bại khi không tìm thấy người dùng")
        void getUserByUsername_NotFound_ShouldThrowException() {
            when(userRepository.findByUsername("user_khong_ton_tai")).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername("user_khong_ton_tai"));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Tra cứu theo username thất bại khi username null hoặc rỗng")
        void getUserByUsername_Blank_ShouldThrowException() {
            BusinessException exception1 = assertThrows(BusinessException.class,
                () -> userService.getUserByUsername(null));
            assertEquals(ErrorCode.BAD_REQUEST, exception1.getErrorCode());

            BusinessException exception2 = assertThrows(BusinessException.class,
                () -> userService.getUserByUsername("   "));
            assertEquals(ErrorCode.BAD_REQUEST, exception2.getErrorCode());

            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("Tra cứu theo email thành công")
        void getUserByEmail_Success() {
            when(userRepository.findByEmail("nguyenvana@example.com")).thenReturn(Optional.of(testUser));

            UserResponse response = userService.getUserByEmail("nguyenvana@example.com");

            assertNotNull(response);
            assertEquals("nguyenvana@example.com", response.email());
            assertEquals(10L, response.id());
            verify(userRepository).findByEmail("nguyenvana@example.com");
        }

        @Test
        @DisplayName("Tra cứu theo email thất bại khi không tìm thấy email")
        void getUserByEmail_NotFound_ShouldThrowException() {
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByEmail("notfound@example.com"));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Tra cứu theo email thất bại khi email null hoặc rỗng")
        void getUserByEmail_Blank_ShouldThrowException() {
            BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getUserByEmail("  "));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
            verify(userRepository, never()).findByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ lấy danh sách người dùng phân trang (getUsers)")
    class GetUsersPaginationTests {

        @Test
        @DisplayName("Lấy danh sách người dùng có lọc theo trạng thái enabled")
        void getUsers_FilteredByEnabled_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findByEnabled(true, pageable)).thenReturn(page);

            PagedResponse<UserResponse> response = userService.getUsers(true, pageable);

            assertNotNull(response);
            assertEquals(1, response.items().size());
            assertEquals(1, response.pagination().totalElements());
            assertEquals(0, response.pagination().page());
            assertEquals(10, response.pagination().size());
            verify(userRepository).findByEnabled(true, pageable);
        }

        @Test
        @DisplayName("Lấy tất cả người dùng khi enabled null")
        void getUsers_All_Success() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> page = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(page);

            PagedResponse<UserResponse> response = userService.getUsers(null, pageable);

            assertNotNull(response);
            assertEquals(1, response.items().size());
            verify(userRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ cập nhật hồ sơ cá nhân (updateProfile)")
    class UpdateProfileTests {

        @Test
        @DisplayName("Cập nhật email thành công khi email mới chưa ai sử dụng")
        void updateProfile_Success_NewEmail() {
            UpdateProfileRequest request = new UpdateProfileRequest("newemail@example.com");

            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.updateProfile(10L, request);

            assertNotNull(response);
            assertEquals("newemail@example.com", response.email());
            assertEquals("nguyenvana", response.username());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Cập nhật hồ sơ thành công khi giữ nguyên email của chính mình (không bị coi là trùng lặp)")
        void updateProfile_Success_SameEmail() {
            UpdateProfileRequest request = new UpdateProfileRequest("nguyenvana@example.com");

            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.updateProfile(10L, request);

            assertNotNull(response);
            assertEquals("nguyenvana@example.com", response.email());
            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Cập nhật thất bại khi email mới trùng với tài khoản người dùng khác")
        void updateProfile_DuplicateEmail_ShouldThrowException() {
            UpdateProfileRequest request = new UpdateProfileRequest("duplicate@example.com");

            User anotherUser = User.builder()
                .username("nguyenvanb")
                .email("duplicate@example.com")
                .build();
            ReflectionTestUtils.setField(anotherUser, "id", 20L);

            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("duplicate@example.com")).thenReturn(Optional.of(anotherUser));

            BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateProfile(10L, request));

            assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
            assertEquals("Email đã được sử dụng bởi tài khoản khác", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Cập nhật thất bại khi không tìm thấy người dùng")
        void updateProfile_UserNotFound_ShouldThrowException() {
            UpdateProfileRequest request = new UpdateProfileRequest("any@example.com");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.updateProfile(999L, request));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Các trường bất biến (username, role, password) không bị ảnh hưởng")
        void updateProfile_ImmutableFieldsProtected() {
            UpdateProfileRequest request = new UpdateProfileRequest("updated@example.com");

            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.updateProfile(10L, request);

            assertEquals("nguyenvana", response.username());
            assertEquals("ROLE_CUSTOMER", response.role().name());
            assertEquals(1L, response.role().id());
            assertEquals("$2a$12$hashedPasswordPlaceholder", testUser.getPassword());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ cập nhật trạng thái tài khoản (updateUserStatus)")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Khóa tài khoản người dùng thành công (enabled = false)")
        void updateUserStatus_DisableAccount_Success() {
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.updateUserStatus(10L, request);

            assertNotNull(response);
            assertFalse(response.enabled());
            assertFalse(testUser.getEnabled());
            verify(userRepository).save(testUser);
            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        @DisplayName("Kích hoạt mở khóa lại tài khoản người dùng thành công (enabled = true)")
        void updateUserStatus_EnableAccount_Success() {
            testUser.setEnabled(false);
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(true);

            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.updateUserStatus(10L, request);

            assertNotNull(response);
            assertTrue(response.enabled());
            assertTrue(testUser.getEnabled());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Cập nhật trạng thái thất bại khi không tìm thấy người dùng")
        void updateUserStatus_UserNotFound_ShouldThrowException() {
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserStatus(999L, request));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
            verify(userRepository, never()).save(any(User.class));
        }
    }
}

