package com.example.financebackend.service;

import com.example.financebackend.dto.WalletShareDto;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.entity.WalletShare;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import com.example.financebackend.repository.WalletShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletShareService {

    private static final Logger logger = LoggerFactory.getLogger(WalletShareService.class);
    
    private final WalletShareRepository walletShareRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletShareService(WalletShareRepository walletShareRepository,
                              WalletRepository walletRepository,
                              UserRepository userRepository) {
        this.walletShareRepository = walletShareRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WalletShareDto> getSharedWallets(Long userId) {
        return walletShareRepository.findBySharedWithUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WalletShareDto> getWalletShares(Long walletId, Long ownerId) {
        // Verify ownership
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví hoặc bạn không có quyền truy cập"));

        return walletShareRepository.findByWalletId(walletId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public WalletShareDto shareWallet(Long walletId, WalletShareDto.CreateWalletShareRequest request, Long ownerId) {
        try {
            logger.info("Sharing wallet {} with email {} by user {}", 
                       walletId, request.getSharedWithUserEmail(), ownerId);
            
            Wallet wallet = walletRepository.findByIdAndUserId(walletId, ownerId)
                    .orElseThrow(() -> {
                        logger.error("Wallet not found or no access: walletId={}, userId={}", walletId, ownerId);
                        return new IllegalArgumentException("Không tìm thấy ví hoặc bạn không có quyền truy cập");
                    });

            String email = request.getSharedWithUserEmail() != null ? 
                          request.getSharedWithUserEmail().trim().toLowerCase() : null;
            if (email == null || email.isEmpty()) {
                logger.error("Email is empty");
                throw new IllegalArgumentException("Email không được để trống");
            }

            User sharedWithUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found with email: {}", email);
                        return new IllegalArgumentException("Không tìm thấy người dùng với email: " + email);
                    });

            // Cannot share with yourself
            if (sharedWithUser.getId().equals(ownerId)) {
                logger.warn("Attempt to share wallet with yourself: walletId={}, userId={}", walletId, ownerId);
                throw new IllegalArgumentException("Không thể chia sẻ ví với chính mình");
            }

            // Check if already shared
            if (walletShareRepository.findByWalletIdAndSharedWithUserId(walletId, sharedWithUser.getId()).isPresent()) {
                logger.warn("Wallet already shared with user: walletId={}, userId={}", walletId, sharedWithUser.getId());
                throw new IllegalArgumentException("Ví đã được chia sẻ với người dùng này");
            }

            WalletShare walletShare = new WalletShare();
            walletShare.setWallet(wallet);
            walletShare.setSharedWithUser(sharedWithUser);
            walletShare.setPermission(request.getPermission() != null ? request.getPermission() : WalletShare.Permission.VIEWER);

            WalletShare saved = walletShareRepository.save(walletShare);
            logger.info("Wallet shared successfully: walletShareId={}, walletId={}, sharedWithUserId={}", 
                       saved.getId(), walletId, sharedWithUser.getId());
            return toDto(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error sharing wallet: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sharing wallet: walletId={}, email={}, userId={}", 
                        walletId, request.getSharedWithUserEmail(), ownerId, e);
            throw new IllegalArgumentException("Đã xảy ra lỗi không mong muốn khi chia sẻ ví: " + e.getMessage());
        }
    }

    public WalletShareDto updatePermission(Long shareId, WalletShare.Permission permission, Long ownerId) {
        WalletShare walletShare = walletShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi chia sẻ"));

        // Verify ownership
        if (!walletShare.getWallet().getUser().getId().equals(ownerId)) {
            throw new IllegalArgumentException("Chỉ chủ sở hữu ví mới có quyền thay đổi quyền truy cập");
        }

        walletShare.setPermission(permission);
        WalletShare saved = walletShareRepository.save(walletShare);
        return toDto(saved);
    }

    public void unshareWallet(Long shareId, Long ownerId) {
        WalletShare walletShare = walletShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi chia sẻ"));

        // Verify ownership
        if (!walletShare.getWallet().getUser().getId().equals(ownerId)) {
            throw new IllegalArgumentException("Chỉ chủ sở hữu ví mới có quyền xóa chia sẻ");
        }

        walletShareRepository.delete(walletShare);
    }

    private WalletShareDto toDto(WalletShare walletShare) {
        WalletShareDto dto = new WalletShareDto();
        dto.setId(walletShare.getId());
        dto.setWalletId(walletShare.getWallet().getId());
        dto.setWalletName(walletShare.getWallet().getName());
        dto.setSharedWithUserId(walletShare.getSharedWithUser().getId());
        dto.setSharedWithUserEmail(walletShare.getSharedWithUser().getEmail());
        dto.setSharedWithUserFullName(walletShare.getSharedWithUser().getFullName());
        dto.setPermission(walletShare.getPermission());
        dto.setCreatedAt(walletShare.getCreatedAt());
        return dto;
    }
}

