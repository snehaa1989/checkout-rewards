package com.example.checkout_rewards.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long cartId,
        String status,
        List<OrderItemResponse> items,
        BigDecimal grossAmount,
        BigDecimal discountAmount,
        BigDecimal netAmount,
        Long couponId,
        LocalDateTime createdAt
) {
}
