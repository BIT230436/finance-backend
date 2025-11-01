package com.example.financebackend.repository;

import com.example.financebackend.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    @Query("SELECT r FROM RecurringTransaction r WHERE r.user.id = :userId")
    List<RecurringTransaction> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r FROM RecurringTransaction r WHERE r.id = :id AND r.user.id = :userId")
    java.util.Optional<RecurringTransaction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    @Query("SELECT r FROM RecurringTransaction r WHERE r.active = true AND r.nextRunDate <= :date")
    List<RecurringTransaction> findByActiveTrueAndNextRunDateLessThanEqual(@Param("date") LocalDate date);
}
