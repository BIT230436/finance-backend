package com.example.financebackend.service;

import com.example.financebackend.dto.CategoryDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategorySuggestionService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    // Keyword mapping cho category suggestions (có thể extend thêm)
    private static final Map<String, String[]> CATEGORY_KEYWORDS = createCategoryKeywordsMap();
    
    private static Map<String, String[]> createCategoryKeywordsMap() {
        Map<String, String[]> map = new HashMap<>();
        map.put("Ăn uống", new String[]{"ăn", "uống", "cà phê", "starbucks", "restaurant", "nhà hàng", "food", "meal", "lunch", "dinner", "breakfast", "đồ ăn", "nước", "bánh", "pizza", "burger", "noodle", "phở", "bún", "cơm"});
        map.put("Mua sắm", new String[]{"mua", "shop", "shopping", "mall", "store", "cửa hàng", "siêu thị", "vinmart", "coopmart", "market", "mua sắm", "đồ dùng", "hàng hóa"});
        map.put("Di chuyển", new String[]{"xe", "taxi", "uber", "grab", "xe buýt", "bus", "xăng", "dầu", "fuel", "parking", "đỗ xe", "bãi đỗ", "di chuyển", "transport", "travel"});
        map.put("Giải trí", new String[]{"game", "phim", "movie", "cinema", "karaoke", "club", "bar", "nhạc", "music", "concert", "show", "giải trí", "entertainment", "netflix", "spotify"});
        map.put("Y tế", new String[]{"bệnh viện", "hospital", "phòng khám", "clinic", "bác sĩ", "doctor", "thuốc", "medicine", "pharmacy", "nhà thuốc", "y tế", "health", "medical"});
        map.put("Giáo dục", new String[]{"học", "school", "university", "book", "sách", "course", "khóa học", "education", "study", "giáo dục"});
        map.put("Hóa đơn", new String[]{"hóa đơn", "bill", "invoice", "điện", "nước", "internet", "wifi", "phone", "điện thoại", "utility"});
        map.put("Nhà ở", new String[]{"nhà", "house", "rent", "thuê", "apartment", "chung cư", "tiền nhà", "rental"});
        map.put("Gia đình", new String[]{"gia đình", "family", "con", "child", "trẻ em", "quà", "gift", "tặng"});
        map.put("Lương", new String[]{"lương", "salary", "wage", "income", "thu nhập"});
        map.put("Thưởng", new String[]{"thưởng", "bonus", "reward"});
        map.put("Đầu tư", new String[]{"đầu tư", "investment", "invest", "cổ phiếu", "stock", "chứng khoán"});
        map.put("Kinh doanh", new String[]{"kinh doanh", "business", "sale", "bán hàng", "doanh thu"});
        return Collections.unmodifiableMap(map);
    }

    public CategorySuggestionService(CategoryRepository categoryRepository,
                                    TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Suggest categories dựa trên note/description
     */
    public List<CategorySuggestionDto> suggestCategories(String note, Transaction.TransactionType type, Long userId) {
        List<CategorySuggestionDto> suggestions = new ArrayList<>();

        if (note == null || note.trim().isEmpty()) {
            // Nếu không có note, trả về most used categories
            return getMostUsedCategories(type, userId, 3);
        }

        String lowerNote = note.toLowerCase().trim();
        
        // Get all categories của user
        List<Category> userCategories = categoryRepository.findByUserIdAndType(userId, 
                type == Transaction.TransactionType.INCOME ? Category.CategoryType.INCOME : Category.CategoryType.EXPENSE);

        // Score categories dựa trên keyword matching
        Map<Category, Integer> categoryScores = new HashMap<>();
        
        for (Category category : userCategories) {
            int score = calculateKeywordScore(category.getName(), lowerNote);
            if (score > 0) {
                categoryScores.put(category, score);
            }
        }

        // Sort by score (descending) và lấy top 3
        List<Map.Entry<Category, Integer>> sortedEntries = categoryScores.entrySet().stream()
                .sorted(Map.Entry.<Category, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Convert to DTOs
        for (Map.Entry<Category, Integer> entry : sortedEntries) {
            CategorySuggestionDto dto = new CategorySuggestionDto();
            dto.setCategoryId(entry.getKey().getId());
            dto.setCategoryName(entry.getKey().getName());
            dto.setScore(entry.getValue());
            dto.setConfidence(calculateConfidence(entry.getValue()));
            suggestions.add(dto);
        }

        // Nếu không có suggestions từ keywords, fallback to most used categories
        if (suggestions.isEmpty()) {
            suggestions = getMostUsedCategories(type, userId, 3);
        }

        return suggestions;
    }

    /**
     * Suggest amount dựa trên category history
     */
    public List<BigDecimal> suggestAmounts(Long categoryId, Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .sorted(Comparator.comparing(Transaction::getOccurredAt).reversed())
                .limit(10)
                .collect(Collectors.toList());

        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate average, most common, và last amount
        BigDecimal sum = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(transactions.size()), 2, java.math.RoundingMode.HALF_UP);

        // Most common amount (simplified - lấy mode)
        BigDecimal mostCommon = transactions.stream()
                .map(Transaction::getAmount)
                .collect(Collectors.groupingBy(amount -> amount, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Last amount
        BigDecimal lastAmount = transactions.get(0).getAmount();

        List<BigDecimal> suggestions = new ArrayList<>();
        if (mostCommon != null && !suggestions.contains(mostCommon)) {
            suggestions.add(mostCommon);
        }
        if (!suggestions.contains(average)) {
            suggestions.add(average);
        }
        if (!suggestions.contains(lastAmount)) {
            suggestions.add(lastAmount);
        }

        return suggestions.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get most used categories
     */
    private List<CategorySuggestionDto> getMostUsedCategories(Transaction.TransactionType type, Long userId, int limit) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getType() == type)
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.toList());

        // Count transactions per category
        Map<Long, Long> categoryCounts = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getId(),
                        Collectors.counting()
                ));

        // Sort by count và lấy top categories
        List<Map.Entry<Long, Long>> sortedEntries = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        List<CategorySuggestionDto> suggestions = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : sortedEntries) {
            Category category = categoryRepository.findById(entry.getKey()).orElse(null);
            if (category != null) {
                CategorySuggestionDto dto = new CategorySuggestionDto();
                dto.setCategoryId(category.getId());
                dto.setCategoryName(category.getName());
                dto.setScore((int) entry.getValue().longValue());
                dto.setConfidence("medium");
                suggestions.add(dto);
            }
        }

        return suggestions;
    }

    /**
     * Calculate keyword score cho category matching
     */
    private int calculateKeywordScore(String categoryName, String note) {
        int score = 0;
        
        // Check if category name appears in note
        if (note.contains(categoryName.toLowerCase())) {
            score += 10;
        }

        // Check keyword mapping
        String[] keywords = CATEGORY_KEYWORDS.get(categoryName);
        if (keywords != null) {
            for (String keyword : keywords) {
                if (note.contains(keyword.toLowerCase())) {
                    score += 5;
                }
            }
        }

        return score;
    }

    /**
     * Calculate confidence level
     */
    private String calculateConfidence(int score) {
        if (score >= 15) {
            return "high";
        } else if (score >= 5) {
            return "medium";
        } else {
            return "low";
        }
    }

    /**
     * DTO for category suggestions
     */
    public static class CategorySuggestionDto {
        private Long categoryId;
        private String categoryName;
        private Integer score;
        private String confidence;

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }
    }
}

