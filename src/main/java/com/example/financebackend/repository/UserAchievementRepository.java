package com.example.financebackend.repository;

import com.example.financebackend.entity.Achievement;
import com.example.financebackend.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    
    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(Long userId);
    
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.achievement.type = :type")
    Optional<UserAchievement> findByUserIdAndAchievementType(@Param("userId") Long userId, 
                                                               @Param("type") Achievement.AchievementType type);
    
    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(ua.achievement.points) FROM UserAchievement ua WHERE ua.user.id = :userId")
    Integer sumPointsByUserId(@Param("userId") Long userId);
}

