package com.example.financebackend.repository;

import com.example.financebackend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
