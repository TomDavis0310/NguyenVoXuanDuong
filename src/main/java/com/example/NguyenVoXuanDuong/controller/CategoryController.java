package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.Category;
import com.example.NguyenVoXuanDuong.service.CategoryService;
import com.example.NguyenVoXuanDuong.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @GetMapping("/categories/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("parentCategories", categoryService.getParentCategoryOptions());
        return "/categories/add-category";
    }

    @PostMapping("/categories/add")
    public String addCategory(@Valid Category category, BindingResult result, Model model,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", categoryService.getParentCategoryOptions());
            return "/categories/add-category";
        }

        String imagePath = fileStorageService.storeImage(imageFile, "images/categories");
        if (imagePath != null) {
            category.setImage(imagePath);
        }

        categoryService.addCategory(category);
        return "redirect:/categories";
    }

    // Hiển thị danh sách danh mục
    @GetMapping("/categories")
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("rootCategories", categoryService.getRootCategoriesWithChildren());
        model.addAttribute("categories", categories);
        return "/categories/categories-list";
    }

    @GetMapping("/categories/edit/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:"
                        + id));
        model.addAttribute("category", category);
        model.addAttribute("parentCategories", categoryService.getParentCategoryOptions().stream()
            .filter(parent -> !parent.getId().equals(id))
            .toList());
        return "/categories/update-category";
    }
    // POST request to update category
    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable("id") Long id, @Valid Category category,
                                 BindingResult result, Model model,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        if (result.hasErrors()) {
            category.setId(id);
            model.addAttribute("parentCategories", categoryService.getParentCategoryOptions().stream()
                    .filter(parent -> !parent.getId().equals(id))
                    .toList());
            return "/categories/update-category";
        }
        
        // Get existing category to preserve the image if not updating
        Category existingCategory = categoryService.getCategoryById(id).orElse(null);
        String imagePath = fileStorageService.storeImage(imageFile, "images/categories");
        if (imagePath != null) {
            category.setImage(imagePath);
        } else if (existingCategory != null) {
            category.setImage(existingCategory.getImage());
        }

        if (category.getParentCategory() != null && category.getParentCategory().getId() != null
                && category.getParentCategory().getId().equals(id)) {
            category.setParentCategory(null);
        }

        categoryService.updateCategory(category);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "redirect:/categories";
    }
    // GET request for deleting category
    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategoryById(id);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/categories";
    }

}
