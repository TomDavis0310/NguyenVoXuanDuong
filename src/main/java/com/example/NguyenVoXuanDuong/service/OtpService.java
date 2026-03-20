package com.example.NguyenVoXuanDuong.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {
    private static final String REDEEM_PURPOSE = "REDEEM_VOUCHER";
    private static final String CHANNEL_PHONE = "phone";
    private static final String CHANNEL_EMAIL = "email";
    private static final Duration OTP_EXPIRE = Duration.ofMinutes(3);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(45);
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpSession> sessions = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${otp.email.enabled:false}")
    private boolean otpEmailEnabled;

    @Value("${otp.email.from:}")
    private String otpEmailFrom;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public OtpRequestResult requestRedeemOtp(String customerPhone) {
        return requestRedeemOtp(customerPhone, CHANNEL_PHONE, null);
    }

    public OtpRequestResult requestRedeemOtp(String customerPhone, String channel, String email) {
        String normalizedPhone = normalizePhone(customerPhone);
        if (!normalizedPhone.matches("^[0-9]{10}$")) {
            return OtpRequestResult.fail("So dien thoai khong hop le", null);
        }

        String normalizedChannel = normalizeChannel(channel);
        String destination;
        if (CHANNEL_EMAIL.equals(normalizedChannel)) {
            String normalizedEmail = normalizeEmail(email);
            if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return OtpRequestResult.fail("Email khong hop le", null);
            }
            destination = normalizedEmail;
        } else {
            destination = normalizedPhone;
        }

        String key = keyFor(REDEEM_PURPOSE, normalizedPhone);
        OtpSession current = sessions.get(key);
        LocalDateTime now = LocalDateTime.now();

        if (current != null && current.nextRequestAt().isAfter(now)) {
            long seconds = Duration.between(now, current.nextRequestAt()).toSeconds();
            return OtpRequestResult.fail("Vui long cho " + Math.max(seconds, 1) + " giay de gui lai OTP", null);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpSession next = new OtpSession(
            code,
            now.plus(OTP_EXPIRE),
            now.plus(RESEND_COOLDOWN),
            0
        );
        sessions.put(key, next);

        // Demo assignment mode: log OTP instead of integrating SMS provider.
        log.info("[OTP-DEMO] purpose={} channel={} destination={} phone={} otp={} expiresAt={}",
            REDEEM_PURPOSE,
            normalizedChannel,
            destination,
            normalizedPhone,
            code,
            next.expiresAt());

        if (CHANNEL_EMAIL.equals(normalizedChannel)) {
            if (sendEmailOtp(destination, code)) {
                return OtpRequestResult.success("OTP da duoc gui qua email.", null);
            }
            return OtpRequestResult.success(
                "He thong chua gui duoc email OTP. Tam thoi dung OTP demo trong thong bao/log server.",
                code
            );
        }

        String message = "OTP da duoc gui qua so dien thoai (demo). Vui long kiem tra log server de lay ma OTP.";

        return OtpRequestResult.success(message, code);
    }

    private boolean sendEmailOtp(String toEmail, String otpCode) {
        if (!otpEmailEnabled || mailSender == null) {
            return false;
        }

        String fromEmail = hasText(otpEmailFrom) ? otpEmailFrom.trim() : safe(smtpUsername);
        if (!hasText(fromEmail)) {
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Ma OTP doi voucher");
            message.setText("Ma OTP cua ban la: " + otpCode + "\nMa co hieu luc trong 3 phut.");
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Send OTP email failed to {}: {}", toEmail, ex.getMessage());
            return false;
        }
    }

    public OtpVerifyResult verifyRedeemOtp(String customerPhone, String otp) {
        String normalizedPhone = normalizePhone(customerPhone);
        String normalizedOtp = otp == null ? "" : otp.trim();

        if (!normalizedPhone.matches("^[0-9]{10}$")) {
            return OtpVerifyResult.fail("So dien thoai khong hop le");
        }
        if (!normalizedOtp.matches("^[0-9]{6}$")) {
            return OtpVerifyResult.fail("OTP phai gom 6 chu so");
        }

        String key = keyFor(REDEEM_PURPOSE, normalizedPhone);
        OtpSession session = sessions.get(key);
        if (session == null) {
            return OtpVerifyResult.fail("Ban chua yeu cau OTP hoac OTP da het han");
        }

        LocalDateTime now = LocalDateTime.now();
        if (session.expiresAt().isBefore(now)) {
            sessions.remove(key);
            return OtpVerifyResult.fail("OTP da het han");
        }

        if (session.failedAttempts() >= MAX_VERIFY_ATTEMPTS) {
            sessions.remove(key);
            return OtpVerifyResult.fail("Ban da nhap sai OTP qua nhieu lan");
        }

        if (!session.code().equals(normalizedOtp)) {
            sessions.put(key, new OtpSession(
                session.code(),
                session.expiresAt(),
                session.nextRequestAt(),
                session.failedAttempts() + 1
            ));
            return OtpVerifyResult.fail("OTP khong dung");
        }

        sessions.remove(key);
        return OtpVerifyResult.verified();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            return CHANNEL_PHONE;
        }
        String value = channel.trim().toLowerCase();
        if (CHANNEL_EMAIL.equals(value)) {
            return CHANNEL_EMAIL;
        }
        return CHANNEL_PHONE;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String keyFor(String purpose, String customerPhone) {
        return purpose + ":" + customerPhone;
    }

    private record OtpSession(String code, LocalDateTime expiresAt, LocalDateTime nextRequestAt, int failedAttempts) {
    }

    public record OtpRequestResult(boolean success, String message, String demoOtp) {
        public static OtpRequestResult success(String message, String demoOtp) {
            return new OtpRequestResult(true, message, demoOtp);
        }

        public static OtpRequestResult fail(String message, String demoOtp) {
            return new OtpRequestResult(false, message, demoOtp);
        }
    }

    public record OtpVerifyResult(boolean success, String message) {
        public static OtpVerifyResult verified() {
            return new OtpVerifyResult(true, "Xac thuc OTP thanh cong");
        }

        public static OtpVerifyResult fail(String message) {
            return new OtpVerifyResult(false, message);
        }
    }
}
