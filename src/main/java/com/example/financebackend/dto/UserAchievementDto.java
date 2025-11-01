package com.example.financebackend.dto;

import java.time.LocalDateTime;

public class UserAchievementDto {
    private Long id;
    private AchievementDto achievement;
    private LocalDateTime unlockedAt;
    private Boolean notified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AchievementDto getAchievement() {
        return achievement;
    }

    public void setAchievement(AchievementDto achievement) {
        this.achievement = achievement;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public Boolean getNotified() {
        return notified;
    }

    public void setNotified(Boolean notified) {
        this.notified = notified;
    }
}

