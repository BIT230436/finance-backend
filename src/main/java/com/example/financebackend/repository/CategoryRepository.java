package com.example.financebackend.repository;

import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId")
    List<Category> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.type = :type")
    List<Category> findByUserIdAndType(@Param("userId") Long userId, @Param("type") CategoryType type);
    
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.user.id = :userId")
    Optional<Category> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.user.id = :userId AND LOWER(c.name) = LOWER(:name) AND c.type = :type")
    boolean existsByUserIdAndNameIgnoreCaseAndType(@Param("userId") Long userId, @Param("name") String name, @Param("type") CategoryType type);
}

