package com.example.NguyenVoXuanDuong.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerName;
    private String customerPhone;
    private double subtotal;
    private double promotionDiscount;
    private double shippingFee;
    private double voucherDiscount;
    private double pointsDiscount;
    private double finalTotal;
    private String voucherCode;
    private int usedPoints;
    private int earnedPoints;
    private String paymentMethod;
    private String paymentStatus;
    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;
}
