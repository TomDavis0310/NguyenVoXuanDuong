package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.Product;
import com.example.NguyenVoXuanDuong.service.CategoryService;
import com.example.NguyenVoXuanDuong.service.FileStorageService;
import com.example.NguyenVoXuanDuong.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @GetMapping("/products")
    public String showProductList(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                  Model model) {
        if (categoryId != null) {
            model.addAttribute("products", productService.getProductsByCategoryId(categoryId));
            model.addAttribute("selectedCategoryId", categoryId);
            categoryService.getCategoryById(categoryId)
                    .ifPresent(category -> model.addAttribute("selectedCategoryName", category.getName()));
        } else {
            model.addAttribute("products", productService.getAllProducts());
        }
        return "/products/products-list";
    }

    @GetMapping("/products/{id}")
    public String showProductDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        model.addAttribute("product", product);
        return "/products/product-detail";
    }

    // For adding a new product
    @GetMapping("/products/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/products/add-product";
    }
    // Process the form for adding a new product
    @PostMapping("/products/add")
    public String addProduct(@ModelAttribute("product") @Valid Product product, BindingResult result,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             @RequestParam(value = "category", required = false) Long categoryId,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "/products/add-product";
        }
        if (categoryId != null) {
            categoryService.getCategoryById(categoryId).ifPresent(product::setCategory);
        }

        String imagePath = fileStorageService.storeImage(imageFile, "images");
        if (imagePath != null) {
            product.setImage(imagePath);
            logger.debug("Stored product image at {}", imagePath);
        }

        productService.addProduct(product);
        return "redirect:/products";
    }

    @GetMapping("/products/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/products/update-product";
    }
    // Process the form for updating a product
    @PostMapping("/products/update/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute("product") @Valid Product product,
                                BindingResult result,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(value = "category", required = false) Long categoryId,
                                Model model) {
        if (result.hasErrors()) {
            product.setId(id);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "/products/update-product";
        }
        product.setId(id);
        if (categoryId != null) {
            categoryService.getCategoryById(categoryId).ifPresent(product::setCategory);
        }
        Product existing = productService.getProductById(id).orElse(null);
        if (existing != null && (imageFile == null || imageFile.isEmpty())) {
            product.setImage(existing.getImage());
        }

        String imagePath = fileStorageService.storeImage(imageFile, "images");
        if (imagePath != null) {
            product.setImage(imagePath);
            logger.debug("Updated product image at {}", imagePath);
        }

        productService.updateProduct(product);
        return "redirect:/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProductById(id);
        return "redirect:/products";
    }
}
