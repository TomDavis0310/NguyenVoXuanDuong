package com.example.NguyenVoXuanDuong.config;

import com.example.NguyenVoXuanDuong.service.CategoryService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import com.example.NguyenVoXuanDuong.model.Category;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CategoryService categoryService;

    public GlobalModelAttributes(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @ModelAttribute("headerCategories")
    public List<Category> headerCategories() {
        return categoryService.getRootCategoriesWithChildren();
    }
}
