package com.ecommerce.common.security;

import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Tải thông tin người dùng bằng username thành công")
    void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
        Role role = Role.builder().name("ROLE_CUSTOMER").build();
        User user = User.builder()
            .username("johndoe")
            .email("john@example.com")
            .password("EncodedPassword")
            .enabled(true)
            .role(role)
            .build();

        when(userRepository.findByUsernameWithRole("johndoe")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("johndoe");

        assertNotNull(userDetails);
        assertEquals("johndoe", userDetails.getUsername());
        assertEquals("EncodedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertEquals(1, userDetails.getAuthorities().size());
        assertEquals("ROLE_CUSTOMER", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Tải thông tin người dùng bằng email thành công")
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        Role role = Role.builder().name("ROLE_ADMIN").build();
        User user = User.builder()
            .username("admin_user")
            .email("admin@example.com")
            .password("EncodedAdminPassword")
            .enabled(true)
            .role(role)
            .build();

        when(userRepository.findWithRoleByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@example.com");

        assertNotNull(userDetails);
        assertEquals("admin_user", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
        assertEquals("ROLE_ADMIN", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Tải người dùng bị vô hiệu hóa: isEnabled phải trả về false")
    void loadUserByUsername_DisabledUser_ShouldReturnUserDetailsWithDisabledStatus() {
        Role role = Role.builder().name("ROLE_CUSTOMER").build();
        User user = User.builder()
            .username("disabled_user")
            .email("disabled@example.com")
            .password("EncodedPassword")
            .enabled(false)
            .role(role)
            .build();

        when(userRepository.findByUsernameWithRole("disabled_user")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled_user");

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled(), "Tài khoản bị khóa thì isEnabled phải bằng false");
    }

    @Test
    @DisplayName("Không tìm thấy người dùng: ném UsernameNotFoundException")
    void loadUserByUsername_UserNotFound_ShouldThrowException() {
        when(userRepository.findByUsernameWithRole("unknown")).thenReturn(Optional.empty());
        when(userRepository.findWithRoleByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown"));
    }

    @Test
    @DisplayName("Định danh null hoặc rỗng: ném UsernameNotFoundException")
    void loadUserByUsername_NullOrBlank_ShouldThrowException() {
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(null));
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("   "));
    }

    @Test
    @DisplayName("DaoAuthenticationProvider chặn tài khoản bị vô hiệu hóa: ném DisabledException khi xác thực bằng mật khẩu")
    void authenticate_DisabledUserWithDaoAuthenticationProvider_ShouldThrowDisabledException() {
        Role role = Role.builder().name("ROLE_CUSTOMER").build();
        org.springframework.security.crypto.password.PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String rawPassword = "ValidPassword123!";
        String encodedPassword = encoder.encode(rawPassword);

        User disabledUser = User.builder()
            .username("locked_buyer")
            .email("locked@example.com")
            .password(encodedPassword)
            .enabled(false)
            .role(role)
            .build();

        when(userRepository.findByUsernameWithRole("locked_buyer")).thenReturn(Optional.of(disabledUser));

        org.springframework.security.authentication.dao.DaoAuthenticationProvider provider =
            new org.springframework.security.authentication.dao.DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authRequest =
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("locked_buyer", rawPassword);

        assertThrows(org.springframework.security.authentication.DisabledException.class,
            () -> provider.authenticate(authRequest));
    }
}
