package com.example.financebackend.repository;

import com.example.financebackend.entity.WalletShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletShareRepository extends JpaRepository<WalletShare, Long> {
    @Query("SELECT ws FROM WalletShare ws WHERE ws.wallet.id = :walletId")
    List<WalletShare> findByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT ws FROM WalletShare ws WHERE ws.sharedWithUser.id = :userId")
    List<WalletShare> findBySharedWithUserId(@Param("userId") Long userId);

    @Query("SELECT ws FROM WalletShare ws WHERE ws.wallet.id = :walletId AND ws.sharedWithUser.id = :userId")
    Optional<WalletShare> findByWalletIdAndSharedWithUserId(@Param("walletId") Long walletId, @Param("userId") Long userId);

    @Query("SELECT ws FROM WalletShare ws WHERE ws.wallet.id = :walletId AND ws.wallet.user.id = :ownerId")
    List<WalletShare> findByWalletIdAndOwnerId(@Param("walletId") Long walletId, @Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(ws) FROM WalletShare ws WHERE ws.wallet.id = :walletId")
    long countByWalletId(@Param("walletId") Long walletId);
}

