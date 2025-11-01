package com.example.financebackend.repository;

import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.TransactionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionTemplateRepository extends JpaRepository<TransactionTemplate, Long> {
    @Query("SELECT t FROM TransactionTemplate t WHERE t.user.id = :userId ORDER BY t.usageCount DESC, t.lastUsedAt DESC")
    List<TransactionTemplate> findByUserIdOrderByUsageCountDesc(@Param("userId") Long userId);

    @Query("SELECT t FROM TransactionTemplate t WHERE t.id = :id AND t.user.id = :userId")
    Optional<TransactionTemplate> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COUNT(t) > 0 FROM TransactionTemplate t WHERE t.user.id = :userId AND LOWER(t.name) = LOWER(:name)")
    boolean existsByUserIdAndNameIgnoreCase(@Param("userId") Long userId, @Param("name") String name);
}

