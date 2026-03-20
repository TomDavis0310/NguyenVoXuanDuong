package com.example.NguyenVoXuanDuong.controller;

import com.example.NguyenVoXuanDuong.model.CartItem;
import com.example.NguyenVoXuanDuong.model.Order;
import com.example.NguyenVoXuanDuong.model.User;
import com.example.NguyenVoXuanDuong.service.CartService;
import com.example.NguyenVoXuanDuong.service.OrderPricingSummary;
import com.example.NguyenVoXuanDuong.service.OrderService;
import com.example.NguyenVoXuanDuong.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
 private static final String SESSION_PENDING_CUSTOMER = "pendingCustomerName";
 private static final String SESSION_PENDING_PHONE = "pendingCustomerPhone";
 private static final String SESSION_PENDING_POINTS = "pendingUsedPoints";
 private static final String SESSION_PENDING_VOUCHER = "pendingVoucherCode";

 private final OrderService orderService;
 private final CartService cartService;
 private final UserService userService;

 @GetMapping("/checkout")
 public String checkout(@RequestParam(value = "customerName", required = false) String customerName,
								@RequestParam(value = "customerPhone", required = false) String customerPhone,
								@RequestParam(value = "voucherCode", required = false) String voucherCode,
								@RequestParam(value = "momoError", required = false) String momoError,
								@RequestParam(value = "checkoutError", required = false) String checkoutError,
												Model model,
												Principal principal) {
 List<CartItem> cartItems = cartService.getCartItems();
 String effectiveCustomerName = customerName == null ? "" : customerName.trim();
 String effectiveCustomerPhone = customerPhone == null ? "" : customerPhone.trim();

 if ((effectiveCustomerName.isEmpty() || effectiveCustomerPhone.isEmpty()) && principal != null) {
 	User currentUser = userService.findByUsername(principal.getName()).orElse(null);
 	if (currentUser != null) {
 		if (effectiveCustomerName.isEmpty()) {
 			effectiveCustomerName = currentUser.getUsername() == null ? "" : currentUser.getUsername();
 		}
 		if (effectiveCustomerPhone.isEmpty()) {
 			effectiveCustomerPhone = currentUser.getPhone() == null ? "" : currentUser.getPhone();
 		}
 	}
 }

 OrderPricingSummary summary = orderService.calculateSummary(effectiveCustomerPhone, cartItems, 0, voucherCode);
 model.addAttribute("cartItems", cartItems);
 model.addAttribute("summary", summary);
 model.addAttribute("customerName", effectiveCustomerName);
 model.addAttribute("customerPhone", effectiveCustomerPhone);
 model.addAttribute("voucherCode", voucherCode == null ? "" : voucherCode);
 model.addAttribute("momoError", momoError);
 model.addAttribute("checkoutError", checkoutError);
 return "/cart/checkout";
 }

 @PostMapping("/submit")
 public String submitOrder(@RequestParam String customerName,
								 @RequestParam String customerPhone,
								 @RequestParam(required = false) String voucherCode,
													 @RequestParam(defaultValue = "0") int usedPoints,
													 @RequestParam(defaultValue = "COD") String paymentMethod,
													 HttpSession session,
													 Model model) {
 List<CartItem> cartItems = cartService.getCartItems();
 if (cartItems.isEmpty()) {
 return "redirect:/cart"; // Redirect if cart is empty
 }

 String normalizedPhone = customerPhone == null ? "" : customerPhone.trim();
 if (!normalizedPhone.matches("^[0-9]{10}$")) {
	return "redirect:/order/checkout?customerName=" + encode(customerName) + "&customerPhone=" + encode(normalizedPhone)
			+ "&checkoutError=" + encode("So dien thoai phai gom 10 chu so");
 }

 if ("MOMO".equalsIgnoreCase(paymentMethod) || "VNPAY".equalsIgnoreCase(paymentMethod)) {
	session.setAttribute(SESSION_PENDING_CUSTOMER, customerName);
	session.setAttribute(SESSION_PENDING_PHONE, normalizedPhone);
	session.setAttribute(SESSION_PENDING_POINTS, Math.max(0, usedPoints));
	session.setAttribute(SESSION_PENDING_VOUCHER, voucherCode == null ? "" : voucherCode.trim());
	if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
	 return "redirect:/vnpay/create-payment";
	}
	return "redirect:/momo/create-payment";
 }

 Order order = orderService.createOrder(customerName, normalizedPhone, cartItems, usedPoints, "COD", "UNPAID", voucherCode);
 return "redirect:/order/receipt/" + order.getId();
 }

 @GetMapping("/confirmation")
 public String orderConfirmationFallback() {
 return "redirect:/";
 }

 @GetMapping("/confirmation/{id}")
 public String orderConfirmation(@PathVariable Long id, Model model) {
	Order order = orderService.getOrderById(id);
	model.addAttribute("order", order);
	model.addAttribute("message", "Đặt hàng thành công.");
	return "cart/order-confirmation";
 }

 @GetMapping("/receipt/{id}")
 public String receipt(@PathVariable Long id, Model model) {
  Order order = orderService.getOrderById(id);
  model.addAttribute("order", order);
  return "cart/order-receipt";
 }

 @GetMapping("/history")
 public String orderHistory(@RequestParam(value = "customerName", required = false) String customerName,
							Model model) {
  model.addAttribute("customerName", customerName == null ? "" : customerName);
  model.addAttribute("orders", orderService.getOrderHistory(customerName));
  return "cart/order-history";
 }

 @GetMapping("/points")
 @ResponseBody
 public ResponseEntity<Map<String, Object>> getCustomerPoints(@RequestParam(value = "customerPhone", required = false) String customerPhone) {
  int points = orderService.getAvailablePointsByPhone(customerPhone);
  return ResponseEntity.ok(Map.of(
	  "customerPhone", customerPhone == null ? "" : customerPhone,
	  "points", points
  ));
 }

 public String getPendingCustomer(HttpSession session) {
	Object value = session.getAttribute(SESSION_PENDING_CUSTOMER);
	return value == null ? "" : value.toString();
 }

 public int getPendingUsedPoints(HttpSession session) {
	Object value = session.getAttribute(SESSION_PENDING_POINTS);
	if (value instanceof Integer intValue) {
	 return Math.max(0, intValue);
	}
	return 0;
 }

 public void clearPendingCheckout(HttpSession session) {
	session.removeAttribute(SESSION_PENDING_CUSTOMER);
	session.removeAttribute(SESSION_PENDING_PHONE);
	session.removeAttribute(SESSION_PENDING_POINTS);
	session.removeAttribute(SESSION_PENDING_VOUCHER);
 }

 public String getPendingPhone(HttpSession session) {
	Object value = session.getAttribute(SESSION_PENDING_PHONE);
	return value == null ? "" : value.toString();
 }

 private String encode(String value) {
  return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
 }

 public String getPendingVoucher(HttpSession session) {
	Object value = session.getAttribute(SESSION_PENDING_VOUCHER);
	return value == null ? "" : value.toString();
 }
}
