package com.example.NguyenVoXuanDuong.service;

import com.example.NguyenVoXuanDuong.model.CartItem;
import com.example.NguyenVoXuanDuong.model.CustomerLoyalty;
import com.example.NguyenVoXuanDuong.model.Order;
import com.example.NguyenVoXuanDuong.model.OrderDetail;
import com.example.NguyenVoXuanDuong.model.Product;
import com.example.NguyenVoXuanDuong.repository.CustomerLoyaltyRepository;
import com.example.NguyenVoXuanDuong.repository.OrderDetailRepository;
import com.example.NguyenVoXuanDuong.repository.OrderRepository;
import com.example.NguyenVoXuanDuong.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
 private static final double SHIPPING_30K_THRESHOLD = 1_000_000d;
 private static final double FREE_SHIP_THRESHOLD = 2_000_000d;
 private static final double SHIPPING_FEE_30K = 30_000d;
 private static final double SHIPPING_FEE_15K = 15_000d;
 private static final int POINTS_TO_VND_RATIO = 2;
 private static final double DISCOUNT_PER_POINT_PAIR = 15_000d;

 private final OrderRepository orderRepository;
 private final OrderDetailRepository orderDetailRepository;
 private final ProductRepository productRepository;
 private final CustomerLoyaltyRepository customerLoyaltyRepository;
 private final VoucherService voucherService;
 private final CartService cartService;

 public int getAvailablePointsByPhone(String customerPhone) {
  String normalizedPhone = normalizePhone(customerPhone);
  if (normalizedPhone.isBlank()) {
   return 0;
  }
  return customerLoyaltyRepository.findByCustomerPhone(normalizedPhone)
	  .map(CustomerLoyalty::getPoints)
	  .orElse(0);
 }

 public OrderPricingSummary calculateSummary(String customerPhone, List<CartItem> cartItems, int requestedPoints,
                                             String voucherCode) {
  double subtotal = 0d;
  double promotionDiscount = 0d;
  int totalQuantity = 0;

  for (CartItem item : cartItems) {
   Product product = item.getProduct();
   int quantity = Math.max(0, item.getQuantity());
   totalQuantity += quantity;
   subtotal += product.getPrice() * quantity;

   int promoStock = product.getPromotionalStock() == null ? 0 : Math.max(0, product.getPromotionalStock());
   double promoPrice = product.getPromotionalPrice() == null ? product.getPrice() : product.getPromotionalPrice();

   if (product.isPromotional() && promoStock > 0 && promoPrice < product.getPrice()) {
    int promotionalQuantityUsed = Math.min(quantity, promoStock);
    promotionDiscount += (product.getPrice() - promoPrice) * promotionalQuantityUsed;
   }
  }

       double shippingFee;
       if (subtotal < SHIPPING_30K_THRESHOLD) {
        shippingFee = SHIPPING_FEE_30K;
       } else if (subtotal < FREE_SHIP_THRESHOLD) {
        shippingFee = SHIPPING_FEE_15K;
       } else {
        shippingFee = 0d;
       }

       double payableBeforeVoucherAndPoints = subtotal - promotionDiscount + shippingFee;
       VoucherService.VoucherValidation voucherValidation = voucherService.validateVoucher(customerPhone, voucherCode,
                     payableBeforeVoucherAndPoints);
       double voucherDiscount = voucherValidation.valid() ? voucherValidation.discount() : 0d;
       String appliedVoucherCode = voucherValidation.valid() ? voucherValidation.appliedCode() : "";
       String voucherMessage = voucherValidation.valid() ? "" : voucherValidation.message();

       int availablePoints = getAvailablePointsByPhone(customerPhone);
  int normalizedRequestedPoints = Math.max(0, requestedPoints);
  int usedPoints = Math.min(normalizedRequestedPoints, availablePoints);
  usedPoints = usedPoints - (usedPoints % POINTS_TO_VND_RATIO);

  double pointsDiscount = (usedPoints / (double) POINTS_TO_VND_RATIO) * DISCOUNT_PER_POINT_PAIR;
       double payableBeforePoints = payableBeforeVoucherAndPoints - voucherDiscount;
  if (pointsDiscount > payableBeforePoints) {
   int maxPointPairs = (int) (payableBeforePoints / DISCOUNT_PER_POINT_PAIR);
   usedPoints = maxPointPairs * POINTS_TO_VND_RATIO;
   pointsDiscount = maxPointPairs * DISCOUNT_PER_POINT_PAIR;
  }

  double finalTotal = payableBeforePoints - pointsDiscount;
  int earnedPoints = Math.max(0, totalQuantity);

  return new OrderPricingSummary(
	  subtotal,
	  promotionDiscount,
	  shippingFee,
         voucherDiscount,
	  pointsDiscount,
	  finalTotal,
	  totalQuantity,
	  availablePoints,
         appliedVoucherCode,
         voucherMessage,
	  usedPoints,
	  earnedPoints
  );
 }

 @Transactional
 public Order createOrder(String customerName, String customerPhone, List<CartItem> cartItems, int requestedPoints,
                          String paymentMethod, String paymentStatus, String voucherCode) {
  String normalizedPhone = normalizePhone(customerPhone);
  OrderPricingSummary summary = calculateSummary(normalizedPhone, cartItems, requestedPoints, voucherCode);

 Order order = new Order();
 order.setCustomerName(customerName == null || customerName.isBlank() ? "Guest" : customerName.trim());
 order.setCustomerPhone(normalizedPhone);
 order.setSubtotal(summary.getSubtotal());
 order.setPromotionDiscount(summary.getPromotionDiscount());
 order.setShippingFee(summary.getShippingFee());
 order.setVoucherDiscount(summary.getVoucherDiscount());
 order.setPointsDiscount(summary.getPointsDiscount());
 order.setFinalTotal(summary.getFinalTotal());
 order.setVoucherCode(summary.getAppliedVoucherCode());
 order.setUsedPoints(summary.getUsedPoints());
 order.setEarnedPoints(summary.getEarnedPoints());
 order.setPaymentMethod(paymentMethod);
 order.setPaymentStatus(paymentStatus);
 order = orderRepository.save(order);

 for (CartItem item : cartItems) {
 Product product = productRepository.findById(item.getProduct().getId())
	 .orElseThrow(() -> new IllegalStateException("Product not found: " + item.getProduct().getId()));

 int quantity = Math.max(0, item.getQuantity());
 int promoStock = product.getPromotionalStock() == null ? 0 : Math.max(0, product.getPromotionalStock());
 double promoPrice = product.getPromotionalPrice() == null ? product.getPrice() : product.getPromotionalPrice();

 int promotionalQuantityUsed = 0;
 double lineTotal;
 if (product.isPromotional() && promoStock > 0 && promoPrice < product.getPrice()) {
  promotionalQuantityUsed = Math.min(quantity, promoStock);
  lineTotal = (promotionalQuantityUsed * promoPrice) + ((quantity - promotionalQuantityUsed) * product.getPrice());
  product.setPromotionalStock(promoStock - promotionalQuantityUsed);
  productRepository.save(product);
 } else {
  lineTotal = quantity * product.getPrice();
 }

 OrderDetail detail = new OrderDetail();
 detail.setOrder(order);
 detail.setProduct(product);
 detail.setQuantity(quantity);
 detail.setPromotionalQuantityUsed(promotionalQuantityUsed);
 detail.setUnitPrice(product.getPrice());
 detail.setLineTotal(lineTotal);
 orderDetailRepository.save(detail);
 }

 if (!normalizedPhone.isBlank()) {
  CustomerLoyalty loyalty = customerLoyaltyRepository.findByCustomerPhone(normalizedPhone)
         .orElse(null);
  if (loyalty == null) {
   loyalty = new CustomerLoyalty();
   loyalty.setCustomerName(order.getCustomerName());
   loyalty.setCustomerPhone(normalizedPhone);
   loyalty.setPoints(0);
  }
  loyalty.setCustomerName(order.getCustomerName());
  int remainingPoints = Math.max(0, loyalty.getPoints() - summary.getUsedPoints());
  loyalty.setPoints(remainingPoints + summary.getEarnedPoints());
  customerLoyaltyRepository.save(loyalty);
 }

 if (summary.getAppliedVoucherCode() != null && !summary.getAppliedVoucherCode().isBlank()) {
  voucherService.markVoucherUsed(summary.getAppliedVoucherCode());
 }

 cartService.clearCart();
 return order;
 }

 private String normalizePhone(String customerPhone) {
  if (customerPhone == null) {
   return "";
  }
  return customerPhone.trim();
 }

 public Order getOrderById(Long orderId) {
        return orderRepository.findByIdWithDetails(orderId)
	  .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
 }

 public List<Order> getOrderHistory(String customerName) {
        if (customerName == null || customerName.isBlank()) {
         return List.of();
        }
        return orderRepository.findByCustomerNameIgnoreCaseOrderByIdDesc(customerName.trim());
 }
}
