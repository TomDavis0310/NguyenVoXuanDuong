package com.example.NguyenVoXuanDuong.service;

import com.example.NguyenVoXuanDuong.model.CartItem;
import com.example.NguyenVoXuanDuong.model.Product;
import com.example.NguyenVoXuanDuong.repository.CustomerLoyaltyRepository;
import com.example.NguyenVoXuanDuong.repository.OrderDetailRepository;
import com.example.NguyenVoXuanDuong.repository.OrderRepository;
import com.example.NguyenVoXuanDuong.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServicePricingTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerLoyaltyRepository customerLoyaltyRepository;
    @Mock
    private VoucherService voucherService;
    @Mock
    private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderRepository,
            orderDetailRepository,
            productRepository,
            customerLoyaltyRepository,
            voucherService,
            cartService
        );

        when(customerLoyaltyRepository.findByCustomerPhone(anyString())).thenReturn(Optional.empty());
        when(voucherService.validateVoucher(anyString(), anyString(), anyDouble()))
            .thenReturn(VoucherService.VoucherValidation.none());
    }

    @Test
    void shippingFee_shouldBe30k_whenSubtotalBelow1M() {
        List<CartItem> cartItems = List.of(new CartItem(createProduct(900_000d), 1));

        OrderPricingSummary summary = orderService.calculateSummary("0901234567", cartItems, 0, "");

        assertEquals(30_000d, summary.getShippingFee());
    }

    @Test
    void shippingFee_shouldBe15k_whenSubtotalFrom1MToBelow2M() {
        List<CartItem> cartItems = List.of(new CartItem(createProduct(1_500_000d), 1));

        OrderPricingSummary summary = orderService.calculateSummary("0901234567", cartItems, 0, "");

        assertEquals(15_000d, summary.getShippingFee());
    }

    @Test
    void shippingFee_shouldBeFree_whenSubtotalAtLeast2M() {
        List<CartItem> cartItems = List.of(new CartItem(createProduct(2_000_000d), 1));

        OrderPricingSummary summary = orderService.calculateSummary("0901234567", cartItems, 0, "");

        assertEquals(0d, summary.getShippingFee());
    }

    private Product createProduct(double price) {
        Product product = new Product();
        product.setPrice(price);
        product.setPromotional(false);
        product.setPromotionalStock(0);
        return product;
    }
}
