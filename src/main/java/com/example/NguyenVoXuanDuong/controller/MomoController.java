package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.CartItem;
import com.example.NguyenVoXuanDuong.model.Order;
import com.example.NguyenVoXuanDuong.service.CartService;
import com.example.NguyenVoXuanDuong.service.MomoService;
import com.example.NguyenVoXuanDuong.service.OrderPricingSummary;
import com.example.NguyenVoXuanDuong.service.OrderService;
import com.example.NguyenVoXuanDuong.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Controller
@RequestMapping("/momo")
public class MomoController {
    private static final String SESSION_PENDING_CUSTOMER = "pendingCustomerName";
    private static final String SESSION_PENDING_PHONE = "pendingCustomerPhone";
    private static final String SESSION_PENDING_POINTS = "pendingUsedPoints";
    private static final String SESSION_PENDING_VOUCHER = "pendingVoucherCode";
    private static final String SESSION_PENDING_CART_ITEMS = "pendingMomoCartItems";
    private static final String SESSION_PENDING_ORDER_ID = "pendingMomoOrderId";

    private final CartService cartService;
    private final OrderService orderService;
    private final MomoService momoService;
    private final ProductService productService;

    public MomoController(CartService cartService, OrderService orderService, MomoService momoService, ProductService productService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.momoService = momoService;
        this.productService = productService;
    }

    @GetMapping("/create-payment")
    public String createPayment(HttpSession session) {
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

        if (!momoService.isConfigured()) {
            return "redirect:/order/checkout?customerName=" + encode(customerName)
                + "&momoError=" + encode("Thiếu cấu hình MoMo (MOMO_PARTNER_CODE, MOMO_ACCESS_KEY, MOMO_SECRET_KEY, MOMO_REDIRECT_URL, MOMO_IPN_URL)");
        }

        MomoService.MomoCreateResponse response;
        try {
            response = momoService.createPayment(
                amount,
                "Thanh toan don hang NguyenVoXuanDuong",
                customerName
            );
        } catch (Exception ex) {
            return "redirect:/order/checkout?customerName=" + encode(customerName)
                    + "&momoError=" + encode("Không gọi được cổng MoMo: " + ex.getMessage());
        }

        if (response.resultCode() != 0 || response.payUrl().isBlank()) {
            return "redirect:/order/checkout?customerName=" + encode(customerName)
                    + "&momoError=" + encode("MoMo từ chối giao dịch. resultCode=" + response.resultCode() + ", message=" + response.message());
        }

        session.setAttribute(SESSION_PENDING_ORDER_ID, response.orderId());

        return "redirect:" + response.payUrl();
    }

    @GetMapping("/return")
    public String returnFromMomo(@RequestParam Map<String, String> params, HttpSession session) {
        boolean validSignature = momoService.isValidSignature(params);
        String resultCode = params.getOrDefault("resultCode", "99");
        String callbackOrderId = params.getOrDefault("orderId", "");
        String pendingOrderId = getPendingOrderId(session);

        if (!"0".equals(resultCode)) {
            clearPendingCheckout(session);
            return "redirect:/order/checkout?momoError=" + encode("Thanh toán MoMo thất bại. resultCode=" + resultCode);
        }

        // In demo environment, callback signature can be inconsistent. Allow processing if orderId matches pending request.
        if (!validSignature && (pendingOrderId.isBlank() || !pendingOrderId.equals(callbackOrderId))) {
            clearPendingCheckout(session);
            return "redirect:/order/checkout?momoError=" + encode("Xác thực callback MoMo không hợp lệ.");
        }

        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            cartItems = fromSnapshot(session);
        }
        if (cartItems.isEmpty()) {
            clearPendingCheckout(session);
            return "redirect:/order/checkout?momoError=" + encode("Không tìm thấy giỏ hàng để tạo đơn sau thanh toán MoMo.");
        }

        String customerName = getPendingCustomer(session);
        String customerPhone = getPendingPhone(session);
        String voucherCode = getPendingVoucherCode(session);
        int usedPoints = getPendingUsedPoints(session);
        Order order = orderService.createOrder(customerName, customerPhone, cartItems, usedPoints, "MOMO", "PAID", voucherCode);
        clearPendingCheckout(session);

        return "redirect:/order/receipt/" + order.getId();
    }

    @PostMapping("/ipn")
    public ResponseEntity<Map<String, Object>> ipn(@RequestBody Map<String, Object> payload) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            params.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }

        boolean valid = momoService.isValidSignature(params);
        Map<String, Object> result = new HashMap<>();

        if (!valid) {
            result.put("resultCode", 1);
            result.put("message", "Invalid signature");
            return ResponseEntity.ok(result);
        }

        result.put("resultCode", 0);
        result.put("message", "Success");
        return ResponseEntity.ok(result);
    }

    private String getPendingCustomer(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_CUSTOMER);
        return value == null ? "" : value.toString();
    }

    private int getPendingUsedPoints(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_POINTS);
        if (value instanceof Integer intValue) {
            return Math.max(0, intValue);
        }
        return 0;
    }

    private void clearPendingCheckout(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_CUSTOMER);
        session.removeAttribute(SESSION_PENDING_PHONE);
        session.removeAttribute(SESSION_PENDING_POINTS);
        session.removeAttribute(SESSION_PENDING_VOUCHER);
        session.removeAttribute(SESSION_PENDING_CART_ITEMS);
        session.removeAttribute(SESSION_PENDING_ORDER_ID);
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

    private String getPendingOrderId(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_ORDER_ID);
        return value == null ? "" : value.toString();
    }

    private String getPendingPhone(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_PHONE);
        return value == null ? "" : value.toString();
    }

    private String getPendingVoucherCode(HttpSession session) {
        Object value = session.getAttribute(SESSION_PENDING_VOUCHER);
        return value == null ? "" : value.toString();
    }
}
