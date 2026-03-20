package com.example.NguyenVoXuanDuong.service;

import com.example.NguyenVoXuanDuong.config.VnpayProperties;
import com.example.NguyenVoXuanDuong.service.VnpayService;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VnpayIntegrationTest {

    @Test
    void createAndVerifySignedUrl() throws Exception {
        VnpayProperties props = new VnpayProperties();
        props.setTmnCode("TESTTMN");
        props.setHashSecret("SECRETKEY1234567890");
        props.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        props.setReturnUrl("http://localhost:8080/vnpay/return");

        VnpayService svc = new VnpayService(props);
        VnpayService.VnpayCreateResponse resp = svc.createPaymentUrl(123000L, "Test order", "127.0.0.1", "0901234567");

        // Print the generated payment URL so the developer can inspect it in test output
        System.out.println("Generated VNPay URL: " + resp.paymentUrl());

        // Parse query string into map and verify signature
        String query = resp.paymentUrl().split("\\?", 2)[1];
        Map<String, String> params = new HashMap<>();
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(k, v);
        }

        boolean valid = svc.isValidSignature(params);
        assertTrue(valid, "VNPay signature should be valid for generated URL");
    }
}
