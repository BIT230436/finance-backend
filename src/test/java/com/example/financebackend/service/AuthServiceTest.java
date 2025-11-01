package com.example.financebackend.service;

import com.example.financebackend.dto.AuthResponse;
import com.example.financebackend.dto.LoginRequest;
import com.example.financebackend.dto.RegisterRequest;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.PasswordResetTokenRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import com.example.financebackend.util.JwtUtil;
import com.example.financebackend.util.TotpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * 
 * Testing strategy:
 * - Test happy paths (successful operations)
 * - Test error cases (validation failures)
 * - Test edge cases (null values, empty strings)
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private TotpUtil totpUtil;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtUtil,
                passwordResetTokenRepository,
                totpUtil,
                categoryRepository,
                walletRepository,
                emailService
        );
    }

    @Test
    void register_WithValidData_ShouldCreateUser() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("test@example.com");
        savedUser.setFullName("Test User");
        savedUser.setRole(User.Role.USER);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Mock category and wallet saves
        when(categoryRepository.save(any(com.example.financebackend.entity.Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(com.example.financebackend.entity.Wallet.class)))
                .thenAnswer(invocation -> {
                    com.example.financebackend.entity.Wallet wallet = invocation.getArgument(0);
                    wallet.setId(1L); // Set ID to avoid NullPointerException
                    return wallet;
                });
        
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyLong(), anyString(), anyLong()))
                .thenReturn("refresh_token");

        // Mock email service (don't throw exceptions)
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(1L, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Email đã tồn tại", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded_password");
        user.setFullName("Test User");
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setTokenVersion(0L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyLong(), anyString(), anyLong()))
                .thenReturn("refresh_token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(1L, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void login_WithInvalidEmail_ShouldThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Email hoặc mật khẩu không đúng", exception.getMessage());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong_password");

        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded_password");
        user.setEnabled(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Email hoặc mật khẩu không đúng", exception.getMessage());
    }

    @Test
    void login_WithDisabledUser_ShouldThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded_password");
        user.setEnabled(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Tài khoản đã bị vô hiệu hóa", exception.getMessage());
    }

    @Test
    void logoutAllDevices_ShouldIncrementTokenVersion() {
        // Arrange
        Long userId = 1L;
        String password = "password123";

        User user = new User();
        user.setId(userId);
        user.setPasswordHash("encoded_password");
        user.setTokenVersion(0L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "encoded_password")).thenReturn(true);

        // Act
        authService.logoutAllDevices(userId, password);

        // Assert
        assertEquals(1L, user.getTokenVersion());
        verify(userRepository, times(1)).save(user);
    }
}

