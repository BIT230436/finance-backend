package com.example.financebackend.service;

import com.example.financebackend.entity.Achievement;
import com.example.financebackend.entity.Notification;
import com.example.financebackend.entity.UserAchievement;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Achievement Service
 * Auto-unlock achievements khi user đạt milestones
 */
@Service
@Transactional
public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final NotificationRepository notificationRepository;

    public AchievementService(AchievementRepository achievementRepository,
                            UserAchievementRepository userAchievementRepository,
                            UserRepository userRepository,
                            TransactionRepository transactionRepository,
                            BudgetRepository budgetRepository,
                            CategoryRepository categoryRepository,
                            WalletRepository walletRepository,
                            FinancialGoalRepository financialGoalRepository,
                            NotificationRepository notificationRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Get all user achievements
     */
    public Map<String, Object> getUserAchievements(Long userId) {
        List<UserAchievement> unlocked = userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(userId);
        List<Achievement> allAchievements = achievementRepository.findAll();
        
        Set<Long> unlockedIds = unlocked.stream()
            .map(ua -> ua.getAchievement().getId())
            .collect(Collectors.toSet());
        
        List<Achievement> locked = allAchievements.stream()
            .filter(a -> !unlockedIds.contains(a.getId()))
            .collect(Collectors.toList());
        
        Integer totalPoints = userAchievementRepository.sumPointsByUserId(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("unlocked", unlocked);
        result.put("locked", locked);
        result.put("totalPoints", totalPoints != null ? totalPoints : 0);
        result.put("unlockedCount", unlocked.size());
        result.put("totalCount", allAchievements.size());
        result.put("completionPercentage", 
            allAchievements.isEmpty() ? 0 : (unlocked.size() * 100.0 / allAchievements.size()));
        
        return result;
    }

    /**
     * Check và unlock achievements sau action
     */
    public void checkAchievements(Long userId, Achievement.AchievementType triggerType) {
        try {
            switch (triggerType) {
                case FIRST_TRANSACTION:
                    checkFirstTransaction(userId);
                    break;
                case BUDGET_CHAMPION:
                    checkBudgetChampion(userId);
                    break;
                case CONSISTENT_TRACKER:
                case SEVEN_DAY_STREAK:
                case THIRTY_DAY_STREAK:
                    checkStreaks(userId);
                    break;
                case HUNDRED_TRANSACTIONS:
                    checkTransactionCount(userId);
                    break;
                case BUDGET_STARTER:
                    checkBudgetStarter(userId);
                    break;
                case CATEGORY_MASTER:
                    checkCategoryMaster(userId);
                    break;
                case WALLET_ORGANIZER:
                    checkWalletOrganizer(userId);
                    break;
                case GOAL_ACHIEVER:
                    checkGoalAchiever(userId);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("Error checking achievements for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private void checkFirstTransaction(Long userId) {
        long count = transactionRepository.findByUserId(userId).size();
        if (count >= 1) {
            unlockAchievement(userId, Achievement.AchievementType.FIRST_TRANSACTION);
        }
    }

    private void checkBudgetChampion(Long userId) {
        // Check if stayed within budget for 3 months straight
        // This is complex logic - simplified version
        List<com.example.financebackend.entity.Budget> budgets = budgetRepository.findByUserId(userId);
        // TODO: Implement full logic
    }

    private void checkStreaks(Long userId) {
        // TODO: Implement streak tracking
        // Need to track daily transaction logging
    }

    private void checkTransactionCount(Long userId) {
        long count = transactionRepository.findByUserId(userId).size();
        if (count >= 100) {
            unlockAchievement(userId, Achievement.AchievementType.HUNDRED_TRANSACTIONS);
        }
    }

    private void checkBudgetStarter(Long userId) {
        long count = budgetRepository.findByUserId(userId).size();
        if (count >= 1) {
            unlockAchievement(userId, Achievement.AchievementType.BUDGET_STARTER);
        }
    }

    private void checkCategoryMaster(Long userId) {
        long count = categoryRepository.findByUserId(userId).size();
        // Assuming first 15 are defaults, count only custom ones
        if (count >= 20) { // 15 defaults + 5 custom
            unlockAchievement(userId, Achievement.AchievementType.CATEGORY_MASTER);
        }
    }

    private void checkWalletOrganizer(Long userId) {
        long count = walletRepository.findByUserId(userId).size();
        if (count >= 3) {
            unlockAchievement(userId, Achievement.AchievementType.WALLET_ORGANIZER);
        }
    }

    private void checkGoalAchiever(Long userId) {
        // Check if any goal is completed
        List<com.example.financebackend.entity.FinancialGoal> goals = 
            financialGoalRepository.findByUserId(userId);
        
        boolean hasCompletedGoal = goals.stream()
            .anyMatch(g -> g.getCurrentAmount() != null && g.getTargetAmount() != null &&
                          g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0);
        
        if (hasCompletedGoal) {
            unlockAchievement(userId, Achievement.AchievementType.GOAL_ACHIEVER);
        }
    }

    /**
     * Unlock achievement nếu chưa unlock
     */
    private void unlockAchievement(Long userId, Achievement.AchievementType type) {
        // Check if already unlocked
        Optional<UserAchievement> existing = userAchievementRepository
            .findByUserIdAndAchievementType(userId, type);
        
        if (existing.isPresent()) {
            return; // Already unlocked
        }

        // Get achievement
        Optional<Achievement> achievementOpt = achievementRepository.findByType(type);
        if (!achievementOpt.isPresent()) {
            logger.warn("Achievement not found: {}", type);
            return;
        }

        Achievement achievement = achievementOpt.get();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        // Create user achievement
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUser(user);
        userAchievement.setAchievement(achievement);
        userAchievementRepository.save(userAchievement);

        // Create notification
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(Notification.NotificationType.SYSTEM);
        notification.setTitle("🎉 Thành tựu mới!");
        notification.setMessage(String.format(
            "Chúc mừng! Bạn đã mở khóa thành tựu '%s' %s (+%d điểm)",
            achievement.getName(),
            achievement.getIcon(),
            achievement.getPoints()
        ));
        notification.setRelatedEntityId(achievement.getId());
        notification.setRelatedEntityType("achievement");
        notificationRepository.save(notification);

        logger.info("Unlocked achievement {} for user {}", type, userId);
    }

    /**
     * Initialize default achievements (run once)
     */
    @Transactional
    public void initializeAchievements() {
        if (achievementRepository.count() > 0) {
            logger.info("Achievements already initialized");
            return;
        }

        List<Achievement> achievements = Arrays.asList(
            createAchievement(Achievement.AchievementType.FIRST_TRANSACTION,
                "Bước Đầu Tiên", "Tạo giao dịch đầu tiên của bạn", "🎉", 10, "EASY"),
            
            createAchievement(Achievement.AchievementType.BUDGET_CHAMPION,
                "Nhà Vô Địch Ngân Sách", "Tuân thủ ngân sách 3 tháng liên tiếp", "👑", 100, "HARD"),
            
            createAchievement(Achievement.AchievementType.SAVER,
                "Người Tiết Kiệm", "Tiết kiệm được 10 triệu trong 1 năm", "💰", 150, "HARD"),
            
            createAchievement(Achievement.AchievementType.CONSISTENT_TRACKER,
                "Người Kiên Trì", "Ghi giao dịch hàng ngày trong 30 ngày", "📝", 50, "MEDIUM"),
            
            createAchievement(Achievement.AchievementType.GOAL_ACHIEVER,
                "Đạt Mục Tiêu", "Hoàn thành mục tiêu tài chính đầu tiên", "🎯", 75, "MEDIUM"),
            
            createAchievement(Achievement.AchievementType.CATEGORY_MASTER,
                "Bậc Thầy Phân Loại", "Tạo 5 thể loại tùy chỉnh", "📁", 25, "EASY"),
            
            createAchievement(Achievement.AchievementType.WALLET_ORGANIZER,
                "Chuyên Gia Ví", "Tạo 3 ví khác nhau", "👛", 20, "EASY"),
            
            createAchievement(Achievement.AchievementType.BUDGET_STARTER,
                "Bắt Đầu Ngân Sách", "Tạo ngân sách đầu tiên", "💼", 15, "EASY"),
            
            createAchievement(Achievement.AchievementType.SEVEN_DAY_STREAK,
                "Chuỗi 7 Ngày", "Ghi giao dịch 7 ngày liên tiếp", "🔥", 30, "MEDIUM"),
            
            createAchievement(Achievement.AchievementType.THIRTY_DAY_STREAK,
                "Chuỗi 30 Ngày", "Ghi giao dịch 30 ngày liên tiếp", "🔥🔥", 100, "HARD"),
            
            createAchievement(Achievement.AchievementType.HUNDRED_TRANSACTIONS,
                "Thống Kê 100", "Ghi 100 giao dịch", "💯", 50, "MEDIUM"),
            
            createAchievement(Achievement.AchievementType.EARLY_BIRD,
                "Chim Sớm", "Ghi giao dịch trước 9 giờ sáng", "🌅", 10, "EASY"),
            
            createAchievement(Achievement.AchievementType.NIGHT_OWL,
                "Cú Đêm", "Ghi giao dịch sau 10 giờ tối", "🦉", 10, "EASY")
        );

        achievementRepository.saveAll(achievements);
        logger.info("Initialized {} achievements", achievements.size());
    }

    private Achievement createAchievement(Achievement.AchievementType type, String name,
                                        String description, String icon, int points, String difficulty) {
        Achievement achievement = new Achievement();
        achievement.setType(type);
        achievement.setName(name);
        achievement.setDescription(description);
        achievement.setIcon(icon);
        achievement.setPoints(points);
        achievement.setDifficulty(difficulty);
        return achievement;
    }
}

