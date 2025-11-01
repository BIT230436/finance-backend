package com.example.financebackend.config;

import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final com.example.financebackend.service.AchievementService achievementService;

    public DataInitializer(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          CategoryRepository categoryRepository,
                          WalletRepository walletRepository,
                          com.example.financebackend.service.AchievementService achievementService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.achievementService = achievementService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize achievements first
        achievementService.initializeAchievements();
        
        // Then create default users
        createDefaultUsers();
    }

    private void createDefaultUsers() {
        // Admin user
        createUserIfNotExists(
            "admin@finance.com",
            "admin123",
            "Administrator",
            User.Role.ADMIN
        );

        // Regular user
        createUserIfNotExists(
            "user@finance.com",
            "user123",
            "Regular User",
            User.Role.USER
        );

        // Viewer user
        createUserIfNotExists(
            "viewer@finance.com",
            "viewer123",
            "Viewer User",
            User.Role.VIEWER
        );

        logger.info("Default users initialized (if not existed)");
    }

    private void createUserIfNotExists(String email, String password, String fullName, User.Role role) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setRole(role);
            user.setEnabled(true);
            user.setTwoFactorEnabled(false);

            User saved = userRepository.save(user);

            // Tạo default categories và wallet cho user mới
            createDefaultCategories(saved);
            createDefaultWallet(saved);

            logger.info("Created default {} user: {} ({})", role, email, fullName);
        } else {
            logger.debug("User {} already exists, skipping", email);
        }
    }

    private void createDefaultCategories(User user) {
        List<CategoryData> defaultCategories = Arrays.asList(
            // Income categories
            new CategoryData("Lương", Category.CategoryType.INCOME, "#4CAF50"),
            new CategoryData("Thưởng", Category.CategoryType.INCOME, "#8BC34A"),
            new CategoryData("Đầu tư", Category.CategoryType.INCOME, "#CDDC39"),
            new CategoryData("Kinh doanh", Category.CategoryType.INCOME, "#FFEB3B"),
            new CategoryData("Khác", Category.CategoryType.INCOME, "#9E9E9E"),
            
            // Expense categories
            new CategoryData("Ăn uống", Category.CategoryType.EXPENSE, "#FF5722"),
            new CategoryData("Mua sắm", Category.CategoryType.EXPENSE, "#E91E63"),
            new CategoryData("Di chuyển", Category.CategoryType.EXPENSE, "#3F51B5"),
            new CategoryData("Giải trí", Category.CategoryType.EXPENSE, "#9C27B0"),
            new CategoryData("Y tế", Category.CategoryType.EXPENSE, "#F44336"),
            new CategoryData("Giáo dục", Category.CategoryType.EXPENSE, "#2196F3"),
            new CategoryData("Hóa đơn", Category.CategoryType.EXPENSE, "#009688"),
            new CategoryData("Nhà ở", Category.CategoryType.EXPENSE, "#795548"),
            new CategoryData("Gia đình", Category.CategoryType.EXPENSE, "#607D8B"),
            new CategoryData("Khác", Category.CategoryType.EXPENSE, "#9E9E9E")
        );

        for (CategoryData catData : defaultCategories) {
            Category category = new Category();
            category.setUser(user);
            category.setName(catData.name);
            category.setType(catData.type);
            category.setColor(catData.color);
            categoryRepository.save(category);
        }
    }

    private void createDefaultWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setName("Ví chính");
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("VND");
        wallet.setDefault(true);
        walletRepository.save(wallet);
    }

    private static class CategoryData {
        String name;
        Category.CategoryType type;
        String color;

        CategoryData(String name, Category.CategoryType type, String color) {
            this.name = name;
            this.type = type;
            this.color = color;
        }
    }
}

