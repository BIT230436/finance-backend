package com.example.financebackend.repository;

import com.example.financebackend.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId")
    List<Budget> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Budget b WHERE b.id = :id AND b.user.id = :userId")
    Optional<Budget> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM Budget b WHERE b.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
}

