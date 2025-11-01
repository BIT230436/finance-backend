package com.example.financebackend.service;

import com.example.financebackend.dto.WalletDto;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.entity.WalletShare;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import com.example.financebackend.repository.WalletShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletShareRepository walletShareRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository, 
                        WalletShareRepository walletShareRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.walletShareRepository = walletShareRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<WalletDto> findAllByUserId(Long userId) {
        // Get owned wallets
        List<WalletDto> ownedWallets = walletRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        // Get shared wallets with at least VIEWER permission
        List<WalletShare> sharedWallets = walletShareRepository.findBySharedWithUserId(userId);
        List<WalletDto> sharedWalletsDto = sharedWallets.stream()
                .map(ws -> toDto(ws.getWallet()))
                .collect(Collectors.toList());
        
        // Combine and return (avoid duplicates by wallet ID)
        java.util.Map<Long, WalletDto> walletMap = new java.util.HashMap<>();
        ownedWallets.forEach(w -> walletMap.put(w.getId(), w));
        sharedWalletsDto.forEach(w -> walletMap.putIfAbsent(w.getId(), w));
        
        return new java.util.ArrayList<>(walletMap.values());
    }

    public WalletDto findByIdAndUserId(Long id, Long userId) {
        // Check if user owns the wallet
        Optional<Wallet> ownedWallet = walletRepository.findById(id)
                .filter(w -> w.getUser().getId().equals(userId));
        
        if (ownedWallet.isPresent()) {
            return toDto(ownedWallet.get());
        }
        
        // Check if wallet is shared with user
        Optional<WalletShare> sharedWallet = walletShareRepository.findByWalletIdAndSharedWithUserId(id, userId);
        if (sharedWallet.isPresent()) {
            return toDto(sharedWallet.get().getWallet());
        }
        
        throw new IllegalArgumentException("Không tìm thấy ví hoặc bạn không có quyền truy cập");
    }
    
    public boolean hasWalletAccess(Long walletId, Long userId, WalletShare.Permission requiredPermission) {
        // Check ownership
        Optional<Wallet> wallet = walletRepository.findById(walletId);
        if (wallet.isPresent() && wallet.get().getUser().getId().equals(userId)) {
            return true; // Owner has all permissions
        }
        
        // Check shared permission
        Optional<WalletShare> share = walletShareRepository.findByWalletIdAndSharedWithUserId(walletId, userId);
        if (share.isPresent()) {
            WalletShare.Permission userPermission = share.get().getPermission();
            // OWNER > EDITOR > VIEWER
            if (requiredPermission == WalletShare.Permission.VIEWER) {
                return true; // Anyone with share can view
            } else if (requiredPermission == WalletShare.Permission.EDITOR) {
                return userPermission == WalletShare.Permission.EDITOR || userPermission == WalletShare.Permission.OWNER;
            } else if (requiredPermission == WalletShare.Permission.OWNER) {
                return userPermission == WalletShare.Permission.OWNER;
            }
        }
        
        return false;
    }

    public WalletDto create(WalletDto dto, Long userId) {
        logger.info("Creating wallet for user {}: {}", userId, dto.getName());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId);
                    return new IllegalArgumentException("Không tìm thấy người dùng");
                });

        if (walletRepository.existsByUserIdAndNameIgnoreCase(userId, dto.getName())) {
            logger.warn("Wallet name already exists for user {}: {}", userId, dto.getName());
            throw new IllegalArgumentException("Tên ví đã tồn tại");
        }

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            List<Wallet> userWallets = walletRepository.findByUserId(userId);
            userWallets.forEach(w -> w.setDefault(false));
            walletRepository.saveAll(userWallets);
            logger.debug("Reset default flag for {} existing wallets", userWallets.size());
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        apply(dto, wallet);
        Wallet saved = walletRepository.save(wallet);
        logger.info("Wallet created successfully: id={}, name={}, userId={}", saved.getId(), saved.getName(), userId);
        return toDto(saved);
    }

    public WalletDto update(Long id, WalletDto dto, Long userId) {
        Wallet wallet = walletRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));

        if (!wallet.getName().equalsIgnoreCase(dto.getName()) 
                && walletRepository.existsByUserIdAndNameIgnoreCase(userId, dto.getName())) {
            throw new IllegalArgumentException("Tên ví đã tồn tại");
        }

        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(wallet.getDefault())) {
            List<Wallet> userWallets = walletRepository.findByUserId(userId);
            userWallets.forEach(w -> w.setDefault(false));
            walletRepository.saveAll(userWallets);
        }

        apply(dto, wallet);
        Wallet saved = walletRepository.save(wallet);
        return toDto(saved);
    }

    public void delete(Long id, Long userId) {
        Wallet wallet = walletRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));
        
        // Kiểm tra xem wallet có đang được sử dụng trong transactions không
        long transactionCount = transactionRepository.countByWalletId(id);
        if (transactionCount > 0) {
            throw new IllegalStateException(
                String.format("Không thể xóa ví này vì đang có %d giao dịch. " +
                            "Vui lòng xóa hoặc chuyển các giao dịch sang ví khác trước.", 
                            transactionCount));
        }
        
        // Kiểm tra xem wallet có đang được chia sẻ không
        long shareCount = walletShareRepository.countByWalletId(id);
        if (shareCount > 0) {
            throw new IllegalStateException(
                String.format("Không thể xóa ví này vì đang được chia sẻ với %d người dùng. " +
                            "Vui lòng xóa các chia sẻ trước.", 
                            shareCount));
        }
        
        // Kiểm tra nếu là default wallet và có ví khác, không cho phép xóa
        if (Boolean.TRUE.equals(wallet.getDefault())) {
            List<Wallet> otherWallets = walletRepository.findByUserId(userId).stream()
                    .filter(w -> !w.getId().equals(id))
                    .toList();
            if (!otherWallets.isEmpty()) {
                throw new IllegalStateException(
                    "Không thể xóa ví mặc định. Vui lòng đặt ví khác làm mặc định trước.");
            }
        }
        
        walletRepository.delete(wallet);
    }

    private WalletDto toDto(Wallet wallet) {
        WalletDto dto = new WalletDto();
        dto.setId(wallet.getId());
        dto.setName(wallet.getName());
        dto.setType(wallet.getType());
        dto.setCurrency(wallet.getCurrency());
        dto.setBalance(wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO);
        dto.setIsDefault(wallet.getDefault());
        return dto;
    }

    private void apply(WalletDto dto, Wallet wallet) {
        wallet.setName(dto.getName());
        wallet.setType(dto.getType());
        wallet.setCurrency(dto.getCurrency());
        wallet.setBalance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        wallet.setDefault(Boolean.TRUE.equals(dto.getIsDefault()));
    }
}
