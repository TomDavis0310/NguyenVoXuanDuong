package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.Voucher;
import com.example.NguyenVoXuanDuong.service.OtpService;
import com.example.NguyenVoXuanDuong.service.OrderService;
import com.example.NguyenVoXuanDuong.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {
    private final OrderService orderService;
    private final VoucherService voucherService;
    private final OtpService otpService;

    @GetMapping("/lookup")
    public String lookupPage() {
        return "loyalty/lookup";
    }

    @GetMapping("/points")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> points(@RequestParam(value = "customerPhone", required = false) String customerPhone) {
        int points = orderService.getAvailablePointsByPhone(customerPhone);
        return ResponseEntity.ok(Map.of(
            "customerPhone", customerPhone == null ? "" : customerPhone,
            "points", points
        ));
    }

    @PostMapping("/redeem/request-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestRedeemOtp(
        @RequestParam("customerPhone") String customerPhone,
        @RequestParam(value = "channel", defaultValue = "phone") String channel,
        @RequestParam(value = "email", required = false) String email
    ) {
        OtpService.OtpRequestResult result = otpService.requestRedeemOtp(customerPhone, channel, email);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", result.message()
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", result.message(),
            "channel", channel,
            "demoOtp", result.demoOtp() == null ? "" : result.demoOtp()
        ));
    }

    @PostMapping("/redeem/confirm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> redeem(
        @RequestParam("customerPhone") String customerPhone,
        @RequestParam("otp") String otp
    ) {
        OtpService.OtpVerifyResult otpResult = otpService.verifyRedeemOtp(customerPhone, otp);
        if (!otpResult.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", otpResult.message()
            ));
        }

        VoucherService.RedeemResult result = voucherService.redeemByPhone(customerPhone);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", result.message()
            ));
        }

        Voucher voucher = result.voucher();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Doi voucher thanh cong",
            "code", voucher.getCode(),
            "discountAmount", voucher.getDiscountAmount(),
            "minOrderValue", voucher.getMinOrderValue(),
            "expiresAt", voucher.getExpiresAt().toString(),
            "remainingPoints", result.remainingPoints()
        ));
    }

    @GetMapping("/vouchers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> vouchers(@RequestParam("customerPhone") String customerPhone) {
        List<Map<String, Object>> payload = voucherService.getAvailableVouchers(customerPhone)
            .stream()
            .map(v -> Map.<String, Object>of(
                "code", v.getCode(),
                "discountAmount", v.getDiscountAmount(),
                "minOrderValue", v.getMinOrderValue(),
                "expiresAt", v.getExpiresAt().toString()
            ))
            .toList();
        return ResponseEntity.ok(payload);
    }
}
