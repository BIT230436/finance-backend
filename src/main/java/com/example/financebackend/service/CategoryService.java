package com.example.financebackend.service;

import com.example.financebackend.dto.CategoryDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.BudgetRepository;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository,
                          TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    public List<CategoryDto> findAllByUserId(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> findByUserIdAndType(Long userId, Category.CategoryType type) {
        return categoryRepository.findByUserIdAndType(userId, type).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto findByIdAndUserId(Long id, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
        return toDto(category);
    }

    public CategoryDto create(CategoryDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, dto.getName(), dto.getType())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại cho loại này");
        }

        Category category = new Category();
        category.setUser(user);
        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setColor(dto.getColor());

        Category saved = categoryRepository.save(category);
        return toDto(saved);
    }

    public CategoryDto update(Long id, CategoryDto dto, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        if (!category.getName().equalsIgnoreCase(dto.getName()) 
                && categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, dto.getName(), dto.getType())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại cho loại này");
        }

        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setColor(dto.getColor());

        Category saved = categoryRepository.save(category);
        return toDto(saved);
    }

    public void delete(Long id, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
        
        // Kiểm tra xem category có đang được sử dụng trong transactions không
        long transactionCount = transactionRepository.countByCategoryId(id);
        if (transactionCount > 0) {
            throw new IllegalStateException(
                String.format("Không thể xóa danh mục này vì đang có %d giao dịch đang sử dụng. " +
                            "Vui lòng xóa hoặc chuyển các giao dịch sang danh mục khác trước.", 
                            transactionCount));
        }
        
        // Kiểm tra xem category có đang được sử dụng trong budgets không
        long budgetCount = budgetRepository.countByCategoryId(id);
        if (budgetCount > 0) {
            throw new IllegalStateException(
                String.format("Không thể xóa danh mục này vì đang có %d ngân sách đang sử dụng. " +
                            "Vui lòng xóa các ngân sách trước.", 
                            budgetCount));
        }
        
        categoryRepository.delete(category);
    }

    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setType(category.getType());
        dto.setColor(category.getColor());
        return dto;
    }
}
