package com.example.financebackend.service;

import com.example.financebackend.dto.WalletDto;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WalletService
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.example.financebackend.repository.WalletShareRepository walletShareRepository;

    @Mock
    private com.example.financebackend.repository.TransactionRepository transactionRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, userRepository, 
                                         walletShareRepository, transactionRepository);
    }

    @Test
    void findAllByUserId_ShouldReturnAllUserWallets() {
        // Arrange
        Long userId = 1L;
        Wallet wallet1 = createWallet(1L, "Ví tiền mặt", Wallet.WalletType.CASH, new BigDecimal("1000000"));
        Wallet wallet2 = createWallet(2L, "Ví ngân hàng", Wallet.WalletType.BANK, new BigDecimal("5000000"));

        when(walletRepository.findByUserId(userId)).thenReturn(Arrays.asList(wallet1, wallet2));

        // Act
        List<WalletDto> result = walletService.findAllByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Ví tiền mặt", result.get(0).getName());
        assertEquals("Ví ngân hàng", result.get(1).getName());
        verify(walletRepository, times(1)).findByUserId(userId);
    }

    @Test
    void create_WithValidData_ShouldCreateWallet() {
        // Arrange
        Long userId = 1L;
        WalletDto dto = new WalletDto();
        dto.setName("Ví mới");
        dto.setType(Wallet.WalletType.E_WALLET);
        dto.setCurrency("VND");
        dto.setBalance(BigDecimal.ZERO);
        dto.setIsDefault(false);

        User user = new User();
        user.setId(userId);

        Wallet savedWallet = createWallet(1L, "Ví mới", Wallet.WalletType.E_WALLET, BigDecimal.ZERO);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Act
        WalletDto result = walletService.create(dto, userId);

        // Assert
        assertNotNull(result);
        assertEquals("Ví mới", result.getName());
        assertEquals(Wallet.WalletType.E_WALLET, result.getType());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void create_WithInvalidUserId_ShouldThrowException() {
        // Arrange
        Long userId = 999L;
        WalletDto dto = new WalletDto();
        dto.setName("Ví mới");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.create(dto, userId)
        );

        assertEquals("Không tìm thấy người dùng", exception.getMessage());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void delete_WithValidWallet_ShouldDeleteWallet() {
        // Arrange
        Long walletId = 1L;
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Wallet wallet = createWallet(walletId, "Ví cũ", Wallet.WalletType.CASH, BigDecimal.ZERO);
        wallet.setUser(user);

        when(walletRepository.findByIdAndUserId(walletId, userId)).thenReturn(Optional.of(wallet));

        // Act
        walletService.delete(walletId, userId);

        // Assert
        verify(walletRepository, times(1)).delete(wallet);
    }

    @Test
    void delete_WithNonExistentWallet_ShouldThrowException() {
        // Arrange
        Long walletId = 999L;
        Long userId = 1L;

        when(walletRepository.findByIdAndUserId(walletId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.delete(walletId, userId)
        );

        assertEquals("Không tìm thấy ví", exception.getMessage());
        verify(walletRepository, never()).delete(any(Wallet.class));
    }

    private Wallet createWallet(Long id, String name, Wallet.WalletType type, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setId(id);
        wallet.setName(name);
        wallet.setType(type);
        wallet.setCurrency("VND");
        wallet.setBalance(balance);
        wallet.setDefault(false);
        return wallet;
    }
}

