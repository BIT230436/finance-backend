package com.example.financebackend.repository;

import com.example.financebackend.entity.SplitExpenseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SplitExpenseParticipantRepository extends JpaRepository<SplitExpenseParticipant, Long> {
    
    List<SplitExpenseParticipant> findBySplitExpenseId(Long splitExpenseId);
    
    @Query("SELECT sep FROM SplitExpenseParticipant sep WHERE sep.user.id = :userId AND sep.paymentStatus = 'PENDING'")
    List<SplitExpenseParticipant> findPendingByUserId(@Param("userId") Long userId);
    
    @Query("SELECT sep FROM SplitExpenseParticipant sep WHERE sep.splitExpense.id = :splitExpenseId AND sep.user.id = :userId")
    SplitExpenseParticipant findBySplitExpenseIdAndUserId(@Param("splitExpenseId") Long splitExpenseId, 
                                                          @Param("userId") Long userId);
}

