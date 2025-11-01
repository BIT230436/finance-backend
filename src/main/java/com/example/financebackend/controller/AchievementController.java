package com.example.financebackend.controller;

import com.example.financebackend.service.AchievementService;
import com.example.financebackend.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Achievement Controller
 * API endpoints cho gamification features
 */
@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    /**
     * Get user achievements (unlocked + locked + stats)
     */
    @GetMapping("/my")
    public Map<String, Object> getMyAchievements() {
        Long userId = AuthUtil.getCurrentUserId();
        return achievementService.getUserAchievements(userId);
    }
}

