package com.example.financebackend.repository;

import com.example.financebackend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    
    @Query("SELECT p FROM PasswordResetToken p WHERE p.token = :token AND p.used = false")
    Optional<PasswordResetToken> findByTokenAndUsedFalse(@Param("token") String token);
    
    @Query("SELECT p FROM PasswordResetToken p WHERE p.user.id = :userId AND p.used = false")
    List<PasswordResetToken> findByUserIdAndUsedFalse(@Param("userId") Long userId);
}
