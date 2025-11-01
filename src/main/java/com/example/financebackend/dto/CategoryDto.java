package com.example.financebackend.dto;

import com.example.financebackend.entity.Category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryDto {

    private Long id;

    @NotBlank(message = "Tên danh mục là bắt buộc")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Loại danh mục là bắt buộc")
    private CategoryType type;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu sắc phải ở định dạng hex (#RRGGBB)")
    private String color;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
