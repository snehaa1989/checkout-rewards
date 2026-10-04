package com.example.checkout_rewards.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}