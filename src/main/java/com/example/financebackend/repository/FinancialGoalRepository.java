package com.example.financebackend.repository;

import com.example.financebackend.entity.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {
    @Query("SELECT f FROM FinancialGoal f WHERE f.user.id = :userId")
    List<FinancialGoal> findByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM FinancialGoal f WHERE f.id = :id AND f.user.id = :userId")
    Optional<FinancialGoal> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT f FROM FinancialGoal f WHERE f.user.id = :userId AND f.active = true")
    List<FinancialGoal> findByUserIdAndActiveTrue(@Param("userId") Long userId);
}

