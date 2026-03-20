package com.example.NguyenVoXuanDuong.service;

import com.example.NguyenVoXuanDuong.model.CustomerLoyalty;
import com.example.NguyenVoXuanDuong.model.Voucher;
import com.example.NguyenVoXuanDuong.repository.CustomerLoyaltyRepository;
import com.example.NguyenVoXuanDuong.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {
    public static final int REDEEM_POINTS_COST = 20;
    public static final double REDEEM_DISCOUNT_AMOUNT = 50_000d;
    public static final double REDEEM_MIN_ORDER = 300_000d;

    private final VoucherRepository voucherRepository;
    private final CustomerLoyaltyRepository customerLoyaltyRepository;

    public RedeemResult redeemByPhone(String customerPhone) {
        String normalizedPhone = normalizePhone(customerPhone);
        if (!normalizedPhone.matches("^[0-9]{10}$")) {
            return RedeemResult.fail("So dien thoai khong hop le");
        }

        Optional<CustomerLoyalty> loyaltyOptional = customerLoyaltyRepository.findByCustomerPhone(normalizedPhone);
        if (loyaltyOptional.isEmpty()) {
            return RedeemResult.fail("Khong tim thay thong tin diem cho so dien thoai nay");
        }

        CustomerLoyalty loyalty = loyaltyOptional.get();
        if (loyalty.getPoints() < REDEEM_POINTS_COST) {
            return RedeemResult.fail("Khong du diem de doi voucher (can " + REDEEM_POINTS_COST + " diem)");
        }

        loyalty.setPoints(loyalty.getPoints() - REDEEM_POINTS_COST);
        customerLoyaltyRepository.save(loyalty);

        Voucher voucher = new Voucher();
        voucher.setCode(generateCode());
        voucher.setCustomerPhone(normalizedPhone);
        voucher.setPointsCost(REDEEM_POINTS_COST);
        voucher.setDiscountAmount(REDEEM_DISCOUNT_AMOUNT);
        voucher.setMinOrderValue(REDEEM_MIN_ORDER);
        voucher.setUsed(false);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setExpiresAt(LocalDateTime.now().plusDays(30));
        voucherRepository.save(voucher);

        return RedeemResult.success(voucher, loyalty.getPoints());
    }

    @Transactional(readOnly = true)
    public List<Voucher> getAvailableVouchers(String customerPhone) {
        String normalizedPhone = normalizePhone(customerPhone);
        if (!normalizedPhone.matches("^[0-9]{10}$")) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findByCustomerPhoneAndUsedFalseOrderByCreatedAtDesc(normalizedPhone)
            .stream()
            .filter(v -> v.getExpiresAt() != null && v.getExpiresAt().isAfter(now))
            .toList();
    }

    @Transactional(readOnly = true)
    public VoucherValidation validateVoucher(String customerPhone, String voucherCode, double orderValue) {
        String normalizedPhone = normalizePhone(customerPhone);
        String normalizedCode = normalizeCode(voucherCode);

        if (normalizedCode.isBlank()) {
            return VoucherValidation.none();
        }

        Optional<Voucher> optionalVoucher = voucherRepository.findByCodeIgnoreCase(normalizedCode);
        if (optionalVoucher.isEmpty()) {
            return VoucherValidation.invalid("Voucher khong ton tai");
        }

        Voucher voucher = optionalVoucher.get();
        if (voucher.isUsed()) {
            return VoucherValidation.invalid("Voucher da duoc su dung");
        }
        if (!voucher.getCustomerPhone().equals(normalizedPhone)) {
            return VoucherValidation.invalid("Voucher khong thuoc so dien thoai nay");
        }
        if (voucher.getExpiresAt() == null || !voucher.getExpiresAt().isAfter(LocalDateTime.now())) {
            return VoucherValidation.invalid("Voucher da het han");
        }
        if (orderValue < voucher.getMinOrderValue()) {
            return VoucherValidation.invalid("Don hang chua dat gia tri toi thieu de dung voucher");
        }

        return VoucherValidation.valid(voucher, Math.min(voucher.getDiscountAmount(), orderValue));
    }

    public void markVoucherUsed(String voucherCode) {
        String normalizedCode = normalizeCode(voucherCode);
        if (normalizedCode.isBlank()) {
            return;
        }
        voucherRepository.findByCodeIgnoreCase(normalizedCode).ifPresent(voucher -> {
            voucher.setUsed(true);
            voucher.setUsedAt(LocalDateTime.now());
            voucherRepository.save(voucher);
        });
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.trim();
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String generateCode() {
        return "VC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    public record RedeemResult(boolean success, String message, Voucher voucher, int remainingPoints) {
        public static RedeemResult success(Voucher voucher, int remainingPoints) {
            return new RedeemResult(true, "Doi voucher thanh cong", voucher, remainingPoints);
        }

        public static RedeemResult fail(String message) {
            return new RedeemResult(false, message, null, 0);
        }
    }

    public record VoucherValidation(boolean valid, String message, String appliedCode, double discount) {
        public static VoucherValidation none() {
            return new VoucherValidation(false, "", "", 0d);
        }

        public static VoucherValidation invalid(String message) {
            return new VoucherValidation(false, message, "", 0d);
        }

        public static VoucherValidation valid(Voucher voucher, double discount) {
            return new VoucherValidation(true, "", voucher.getCode(), discount);
        }
    }
}
