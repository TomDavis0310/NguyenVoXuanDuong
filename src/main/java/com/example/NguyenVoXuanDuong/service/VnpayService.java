package com.example.NguyenVoXuanDuong.service;

import com.example.NguyenVoXuanDuong.config.VnpayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VnpayService {
    private static final DateTimeFormatter VNP_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties vnpayProperties;

    public boolean isConfigured() {
        return vnpayProperties.isConfigured();
    }

    public VnpayCreateResponse createPaymentUrl(long amount, String orderInfo, String clientIp, String customerPhone) {
        if (!vnpayProperties.isConfigured()) {
            throw new IllegalStateException("VNPay configuration is missing. Please set vnpay.tmn-code/hash-secret/return-url.");
        }

        String txnRef = "VNP" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        String createDate = VNP_DATE_TIME.format(now);
        String expireDate = VNP_DATE_TIME.format(now.plusMinutes(15));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(Math.max(0, amount) * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", safe(orderInfo));
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        params.put("vnp_IpAddr", safe(clientIp));
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);
        params.put("vnp_BankCode", "NCB");
        if (customerPhone != null && !customerPhone.isBlank()) {
            params.put("vnp_Bill_Mobile", customerPhone.trim());
        }

        String hashData = toQueryString(params, true);
        String secureHash = hmacSha512(vnpayProperties.getHashSecret(), hashData);

        String query = toQueryString(params, true)
            + "&vnp_SecureHash=" + encode(secureHash)
            + "&vnp_SecureHashType=HmacSHA512";

        String paymentUrl = vnpayProperties.getPayUrl() + "?" + query;
        return new VnpayCreateResponse(paymentUrl, txnRef);
    }

    public boolean isValidSignature(Map<String, String> params) {
        if (!vnpayProperties.isConfigured()) {
            return false;
        }

        String receivedHash = params.getOrDefault("vnp_SecureHash", "");
        if (receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> filtered = new HashMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        String hashData = toQueryString(filtered, true);
        String expected = hmacSha512(vnpayProperties.getHashSecret(), hashData);
        return expected.equalsIgnoreCase(receivedHash);
    }

    private String toQueryString(Map<String, String> params, boolean urlEncodeValues) {
        List<String> keys = new ArrayList<>(params.keySet());
        keys.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key);
            if (value == null || value.isBlank()) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(key));
            sb.append('=');
            sb.append(urlEncodeValues ? encode(value) : value);
        }
        return sb.toString();
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign VNPay data", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record VnpayCreateResponse(String paymentUrl, String txnRef) {
    }
}
