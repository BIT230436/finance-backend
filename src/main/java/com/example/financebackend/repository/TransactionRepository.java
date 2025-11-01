package com.example.financebackend.repository;

import com.example.financebackend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId")
    List<Transaction> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.user.id = :userId")
    Optional<Transaction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId")
    List<Transaction> findByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:walletId IS NULL OR t.wallet.id = :walletId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:startDate IS NULL OR t.occurredAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.occurredAt <= :endDate) " +
           "AND (:keyword IS NULL OR LOWER(t.note) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY t.occurredAt DESC")
    List<Transaction> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("walletId") Long walletId,
            @Param("type") Transaction.TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword
    );

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.category.id = :categoryId " +
           "AND t.occurredAt >= :startDate AND t.occurredAt <= :endDate " +
           "ORDER BY t.occurredAt DESC")
    List<Transaction> findByBudget(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.wallet.id = :walletId")
    long countByWalletId(@Param("walletId") Long walletId);
}
