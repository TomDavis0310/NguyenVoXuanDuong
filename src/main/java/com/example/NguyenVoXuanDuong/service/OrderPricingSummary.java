package com.example.NguyenVoXuanDuong.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderPricingSummary {
    private final double subtotal;
    private final double promotionDiscount;
    private final double shippingFee;
    private final double voucherDiscount;
    private final double pointsDiscount;
    private final double finalTotal;
    private final int totalQuantity;
    private final int availablePoints;
    private final String appliedVoucherCode;
    private final String voucherMessage;
    private final int usedPoints;
    private final int earnedPoints;
}
