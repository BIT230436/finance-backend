package com.example.financebackend.entity;

import jakarta.persistence.*;

/**
 * Achievement definitions
 * Các thành tựu có thể unlock trong hệ thống
 */
@Entity
@Table(name = "achievements")
public class Achievement {

    public enum AchievementType {
        FIRST_TRANSACTION,      // Giao dịch đầu tiên
        BUDGET_CHAMPION,        // Stay within budget 3 months straight
        SAVER,                  // Save 10M in 1 year
        CONSISTENT_TRACKER,     // Log transactions daily for 30 days
        GOAL_ACHIEVER,          // Complete first financial goal
        CATEGORY_MASTER,        // Create 5 custom categories
        WALLET_ORGANIZER,       // Create 3 wallets
        BUDGET_STARTER,         // Create first budget
        SEVEN_DAY_STREAK,       // 7 days logging streak
        THIRTY_DAY_STREAK,      // 30 days logging streak
        HUNDRED_TRANSACTIONS,   // Log 100 transactions
        EARLY_BIRD,             // Log transaction before 9 AM
        NIGHT_OWL              // Log transaction after 10 PM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private AchievementType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String icon;  // Emoji hoặc icon name

    @Column(nullable = false)
    private Integer points;  // Points awarded

    @Column(nullable = false, length = 20)
    private String difficulty;  // EASY, MEDIUM, HARD

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AchievementType getType() {
        return type;
    }

    public void setType(AchievementType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}

