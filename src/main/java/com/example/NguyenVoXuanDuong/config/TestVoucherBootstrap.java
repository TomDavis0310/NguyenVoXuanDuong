package com.example.NguyenVoXuanDuong.config;

import com.example.NguyenVoXuanDuong.model.CustomerLoyalty;
import com.example.NguyenVoXuanDuong.model.User;
import com.example.NguyenVoXuanDuong.model.Voucher;
import com.example.NguyenVoXuanDuong.repository.CustomerLoyaltyRepository;
import com.example.NguyenVoXuanDuong.repository.UserRepository;
import com.example.NguyenVoXuanDuong.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestVoucherBootstrap implements ApplicationRunner {
    private static final long TARGET_USER_ID = 3L;
    private static final String FALLBACK_PHONE = "0900000003";

    private final UserRepository userRepository;
    private final CustomerLoyaltyRepository customerLoyaltyRepository;
    private final VoucherRepository voucherRepository;

    @Override
    public void run(ApplicationArguments args) {
        User user = userRepository.findById(TARGET_USER_ID).orElse(null);
        if (user == null) {
            log.warn("[TEST-SEED] User id={} not found. Skip loyalty/voucher seed.", TARGET_USER_ID);
            return;
        }

        String phone = normalizePhone(user.getPhone());
        if (!phone.matches("^[0-9]{10}$")) {
            phone = FALLBACK_PHONE;
            if (userRepository.existsByPhone(phone)) {
                log.warn("[TEST-SEED] Fallback phone {} already used. Skip seed for user id={}", phone, TARGET_USER_ID);
                return;
            }
            user.setPhone(phone);
            userRepository.save(user);
        }

        final String finalPhone = phone;

        CustomerLoyalty loyalty = customerLoyaltyRepository.findByCustomerPhone(finalPhone)
            .orElseGet(() -> {
                CustomerLoyalty created = new CustomerLoyalty();
                created.setCustomerPhone(finalPhone);
                created.setCustomerName(user.getUsername() == null ? "user3" : user.getUsername());
                return created;
            });
        loyalty.setCustomerName(user.getUsername() == null ? loyalty.getCustomerName() : user.getUsername());
        loyalty.setPoints(50);
        customerLoyaltyRepository.save(loyalty);

        upsertVoucher("TEST30K_U3", finalPhone, 30_000d, 100_000d);
        upsertVoucher("TEST50K_U3", finalPhone, 50_000d, 200_000d);
        upsertVoucher("TEST100K_U3", finalPhone, 100_000d, 300_000d);

        log.info("[TEST-SEED] User id={} phone={} seeded with 50 points and 3 vouchers", TARGET_USER_ID, finalPhone);
    }

    private void upsertVoucher(String code, String phone, double discountAmount, double minOrderValue) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
            .orElseGet(Voucher::new);

        voucher.setCode(code);
        voucher.setCustomerPhone(phone);
        voucher.setPointsCost(0);
        voucher.setDiscountAmount(discountAmount);
        voucher.setMinOrderValue(minOrderValue);
        voucher.setUsed(false);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setExpiresAt(LocalDateTime.now().plusDays(30));
        voucher.setUsedAt(null);
        voucherRepository.save(voucher);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.trim();
    }
}
