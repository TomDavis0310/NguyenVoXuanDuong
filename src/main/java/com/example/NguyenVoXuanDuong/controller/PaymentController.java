package com.example.NguyenVoXuanDuong.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    @GetMapping("/momo")
    public String momoLegacyRedirect() {
        return "redirect:/momo/create-payment";
    }

    @GetMapping("/vnpay")
    public String vnpayLegacyRedirect() {
        return "redirect:/vnpay/create-payment";
    }
}
