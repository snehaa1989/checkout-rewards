package com.example.checkout_rewards.dto;

public record ProductSalesResponse(
        Long productId,
        String productName,
        long quantitySold
) {
}