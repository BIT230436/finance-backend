package com.example.financebackend.dto;

import com.example.financebackend.entity.Achievement;

public class AchievementDto {
    private Long id;
    private Achievement.AchievementType type;
    private String name;
    private String description;
    private String icon;
    private Integer points;
    private String difficulty;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Achievement.AchievementType getType() {
        return type;
    }

    public void setType(Achievement.AchievementType type) {
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

