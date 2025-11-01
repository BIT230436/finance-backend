package com.example.financebackend.repository;

import com.example.financebackend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    List<Wallet> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT w FROM Wallet w WHERE w.id = :id AND w.user.id = :userId")
    Optional<Wallet> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(w) > 0 FROM Wallet w WHERE w.user.id = :userId AND LOWER(w.name) = LOWER(:name)")
    boolean existsByUserIdAndNameIgnoreCase(@Param("userId") Long userId, @Param("name") String name);
}
