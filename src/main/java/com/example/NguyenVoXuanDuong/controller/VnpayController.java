package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.CartItem;
import com.example.NguyenVoXuanDuong.model.Order;
import com.example.NguyenVoXuanDuong.service.CartService;
import com.example.NguyenVoXuanDuong.service.OrderPricingSummary;
import com.example.NguyenVoXuanDuong.service.OrderService;
import com.example.NguyenVoXuanDuong.service.ProductService;
import com.example.NguyenVoXuanDuong.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vnpay")
@RequiredArgsConstructor
public class VnpayController {
    private static final String SESSION_PENDING_CUSTOMER = "pendingCustomerName";
    private static final String SESSION_PENDING_PHONE = "pendingCustomerPhone";
    private static final String SESSION_PENDING_POINTS = "pendingUsedPoints";
    private static final String SESSION_PENDING_VOUCHER = "pendingVoucherCode";
    private static final String SESSION_PENDING_CART_ITEMS = "pendingVnpayCartItems";
    private static final String SESSION_PENDING_TXN_REF = "pendingVnpayTxnRef";

    private final CartService cartService;
    private final OrderService orderService;
    private final VnpayService vnpayService;
    private final ProductService productService;

    @GetMapping("/create-payment")
    public String createPayment(HttpSession session, HttpServletRequest request) {
        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        String customerName = getPendingCustomer(session);
        String customerPhone = getPendingPhone(session);
        String voucherCode = getPendingVoucherCode(session);
        int usedPoints = getPendingUsedPoints(session);
        session.setAttribute(SESSION_PENDING_CART_ITEMS, toSnapshot(cartItems));

        OrderPricingSummary summary = orderService.calculateSummary(customerPhone, cartItems, usedPoints, voucherCode);
        long amount = Math.round(summary.getFinalTotal());

        if (!vnpayService.isConfigured()) {
            return "redirect:/order/checkout?customerName=" + encode(customerName)
                + "&customerPhone=" + encode(customerPhone)
                + "&voucherCode=" + encode(voucherCode)
                + "&checkoutError=" + encode("Thieu cau hinh VNPay (vnpay.tmn-code/hash-secret/return-url)");
        }

        VnpayService.VnpayCreateResponse response;
        try {
            response = vnpayService.createPaymentUrl(
                amount,
                "Thanh toan don hang NguyenVoXuanDuong",
                resolveClientIp(request),
                customerPhone
            );
        } catch (Exception ex) {
            return "redirect:/order/checkout?customerName=" + encode(customerName)
                + "&customerPhone=" + encode(customerPhone)
                + "&voucherCode=" + encode(voucherCode)
                + "&checkoutError=" + encode("Khong tao duoc giao dich VNPay: " + ex.getMessage());
        }

        session.setAttribute(SESSION_PENDING_TXN_REF, response.txnRef());
        return "redirect:" + response.paymentUrl();
    }

    @GetMapping("/return")
    public String returnFromVnpay(@RequestParam Map<String, String> params, HttpSession session) {
        boolean validSignature = vnpayService.isValidSignature(params);
        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "99");
        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        String pendingTxnRef = getPendingTxnRef(session);

        if (!"00".equals(responseCode) || !"00".equals(transactionStatus)) {
            clearPendingCheckout(session);
            return "redirect:/order/checkout?checkoutError=" + encode("Thanh toan VNPay that bai. responseCode=" + responseCode);
        }

        if (!validSignature || pendingTxnRef.isBlank() || !pendingTxnRef.equals(txnRef)) {
            clearPendingCheckout(session);
            return "redirect:/order/checkout?checkoutError=" + encode("Xac thuc callback VNPay khong hop le.");
        }

        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            cartItems = fromSnapshot(session);
        }
        if (cartItems.isEmpty()) {
            clearPendingCheckout(session);
            return "redirect:/order/checkout?checkoutError=" + encode("Khong tim thay gio hang de tao don sau thanh toan VNPay.");
        }

        String customerName = getPendingCustomer(session);
        String customerPhone = getPendingPhone(session);
        String voucherCode = getPendingVoucherCode(session);
        int usedPoints = getPendingUsedPoints(session);

        Order order = orderService.createOrder(customerName, customerPhone, cartItems, usedPoints, "VNPAY", "PAID", voucherCode);
        clearPendingCheckout(session);
        return "redirect:/order/receipt/" + order.getId();
    }

    @PostMapping("/ipn")
    public ResponseEntity<Map<String, Object>> ipn(@RequestBody Map<String, Object> payload) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            params.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }

        boolean valid = vnpayService.isValidSignature(params);
        Map<String, Object> result = new HashMap<>();
        if (!valid) {
            result.put("RspCode", "97");
            result.put("Message", "Invalid signature");
            return ResponseEntity.ok(result);
        }

        result.put("RspCode", "00");
        result.put("Message", "Confirm Success");
        return ResponseEntity.ok(result);
    }

    private String getPendingCustomer(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_CUSTOMER);
        return value == null ? "" : value.toString();
    }

    private String getPendingPhone(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_PHONE);
        return value == null ? "" : value.toString();
    }

    private int getPendingUsedPoints(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_POINTS);
        if (value instanceof Integer intValue) {
            return Math.max(0, intValue);
        }
        return 0;
    }

    private String getPendingVoucherCode(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_VOUCHER);
        return value == null ? "" : value.toString();
    }

    private String getPendingTxnRef(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_TXN_REF);
        return value == null ? "" : value.toString();
    }

    private void clearPendingCheckout(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_CUSTOMER);
        session.removeAttribute(SESSION_PENDING_PHONE);
        session.removeAttribute(SESSION_PENDING_POINTS);
        session.removeAttribute(SESSION_PENDING_VOUCHER);
        session.removeAttribute(SESSION_PENDING_CART_ITEMS);
        session.removeAttribute(SESSION_PENDING_TXN_REF);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "127.0.0.1" : request.getRemoteAddr();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private List<Map<String, Object>> toSnapshot(List<CartItem> cartItems) {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> row = new HashMap<>();
            row.put("productId", item.getProduct().getId());
            row.put("quantity", item.getQuantity());
            snapshot.add(row);
        }
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private List<CartItem> fromSnapshot(HttpSession session) {
        Object raw = session.getAttribute(SESSION_PENDING_CART_ITEMS);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }

        List<CartItem> rebuilt = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> row)) {
                continue;
            }
            Object productIdRaw = row.get("productId");
            Object quantityRaw = row.get("quantity");
            if (!(productIdRaw instanceof Number productIdNum) || !(quantityRaw instanceof Number quantityNum)) {
                continue;
            }
            long productId = productIdNum.longValue();
            int quantity = Math.max(1, quantityNum.intValue());
            productService.getProductById(productId).ifPresent(product -> rebuilt.add(new CartItem(product, quantity)));
        }
        return rebuilt;
    }
}
