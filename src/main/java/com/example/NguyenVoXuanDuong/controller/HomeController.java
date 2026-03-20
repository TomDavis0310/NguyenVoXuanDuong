package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.Product;
import com.example.NguyenVoXuanDuong.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        List<Product> allProducts = productService.getAllProducts();
        model.addAttribute("promotionalProducts", allProducts.stream().filter(Product::isPromotional).toList());
        model.addAttribute("normalProducts", allProducts.stream().filter(product -> !product.isPromotional()).toList());
        return "home";
    }
}
