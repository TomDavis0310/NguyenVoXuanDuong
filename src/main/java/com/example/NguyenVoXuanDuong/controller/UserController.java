package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.User;
import com.example.NguyenVoXuanDuong.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "users/login";
    }

    @GetMapping("/register")
    public String register(@NotNull Model model) {
        model.addAttribute("user", new User());
        return "users/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                           @NotNull BindingResult bindingResult,
                           Model model) {
        normalizeInput(user);

        List<String> errors = new ArrayList<>();
        if (bindingResult.hasErrors()) {
            errors.addAll(bindingResult.getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList());
        }

        if (userService.existsByUsername(user.getUsername())) {
            errors.add("Username da ton tai");
        }
        if (userService.existsByEmail(user.getEmail())) {
            errors.add("Email da duoc su dung");
        }
        if (userService.existsByPhone(user.getPhone())) {
            errors.add("So dien thoai da duoc su dung");
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return "users/register";
        }

        userService.save(user);
        userService.setDefaultRole(user.getUsername());
        return "redirect:/login?registered";
    }

    private void normalizeInput(User user) {
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
        }
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }
        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().trim());
            if (user.getPhone().isEmpty()) {
                user.setPhone(null);
            }
        }
    }
}
