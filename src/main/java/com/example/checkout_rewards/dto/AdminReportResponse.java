package com.example.checkout_rewards.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminReportResponse(
        List<ProductSalesResponse> productSales,
        BigDecimal grossRevenue,
        BigDecimal totalDiscounts,
        BigDecimal netRevenue,
        long couponsGenerated,
        long couponsAvailable,
        long couponsRedeemed,
        long successfulOrders
) {
}