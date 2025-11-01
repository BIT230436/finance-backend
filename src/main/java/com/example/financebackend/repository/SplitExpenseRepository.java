package com.example.financebackend.repository;

import com.example.financebackend.entity.SplitExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SplitExpenseRepository extends JpaRepository<SplitExpense, Long> {
    
    @Query("SELECT se FROM SplitExpense se WHERE se.createdBy.id = :userId ORDER BY se.createdAt DESC")
    List<SplitExpense> findByCreatedByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT se FROM SplitExpense se " +
           "JOIN SplitExpenseParticipant sep ON sep.splitExpense.id = se.id " +
           "WHERE sep.user.id = :userId OR se.createdBy.id = :userId " +
           "ORDER BY se.createdAt DESC")
    List<SplitExpense> findByUserIdAsParticipantOrCreator(@Param("userId") Long userId);
}

