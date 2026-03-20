package com.example.NguyenVoXuanDuong.service;

import com.example.NguyenVoXuanDuong.config.MomoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MomoService {
    private final MomoProperties momoProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public MomoService(MomoProperties momoProperties) {
        this.momoProperties = momoProperties;
        this.httpClient = HttpClient.newHttpClient();
    }

    public MomoCreateResponse createPayment(long amount, String orderInfo, String extraData) {
        if (!momoProperties.isConfigured()) {
            throw new IllegalStateException("MoMo configuration is missing. Please set momo.partnerCode/accessKey/secretKey/endpoint/redirectUrl/ipnUrl");
        }

        String requestId = UUID.randomUUID().toString();
        String orderId = "MOMO_" + System.currentTimeMillis() + "_" + requestId.substring(0, 8);

        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + safe(extraData)
                + "&ipnUrl=" + momoProperties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + momoProperties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + momoProperties.getRequestType();

        String signature = hmacSha256(rawSignature, momoProperties.getSecretKey());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", momoProperties.getPartnerCode());
        payload.put("partnerName", "NguyenVoXuanDuong");
        payload.put("storeId", "NguyenVoXuanDuongStore");
        payload.put("requestId", requestId);
        payload.put("amount", String.valueOf(amount));
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", momoProperties.getRedirectUrl());
        payload.put("ipnUrl", momoProperties.getIpnUrl());
        payload.put("lang", "vi");
        payload.put("requestType", momoProperties.getRequestType());
        payload.put("autoCapture", true);
        payload.put("extraData", safe(extraData));
        payload.put("signature", signature);

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(momoProperties.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());

            return new MomoCreateResponse(
                    root.path("resultCode").asInt(-1),
                    root.path("message").asText("Unknown"),
                    root.path("payUrl").asText(""),
                    orderId,
                    requestId
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create MoMo payment: " + ex.getMessage(), ex);
        }
    }

    public boolean isConfigured() {
        return momoProperties.isConfigured();
    }

    public boolean isValidSignature(Map<String, String> params) {
        if (!momoProperties.isConfigured()) {
            return false;
        }
        String providedSignature = params.get("signature");
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }

        String rawSignature = buildResponseRawSignature(params);
        if (rawSignature.isBlank()) {
            return false;
        }

        String expected = hmacSha256(rawSignature, momoProperties.getSecretKey());
        return expected.equals(providedSignature);
    }

    public String hmacSha256(String data, String key) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] bytes = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign MoMo data", ex);
        }
    }

    private String buildResponseRawSignature(Map<String, String> params) {
        List<String> orderedKeys = List.of(
                "amount",
                "extraData",
                "message",
                "orderId",
                "orderInfo",
                "orderType",
                "partnerCode",
                "payType",
                "requestId",
                "responseTime",
                "resultCode",
                "transId"
        );

        StringBuilder raw = new StringBuilder();
        for (String key : orderedKeys) {
            if (!params.containsKey(key)) {
                continue;
            }
            if (!raw.isEmpty()) {
                raw.append("&");
            }
            raw.append(key).append("=").append(safe(params.get(key)));
        }
        return raw.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record MomoCreateResponse(int resultCode, String message, String payUrl, String orderId, String requestId) {
    }
}
