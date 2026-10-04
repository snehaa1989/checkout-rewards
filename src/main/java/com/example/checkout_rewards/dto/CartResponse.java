package com.example.checkout_rewards.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        String status,
        List<CartItemResponse> items,
        BigDecimal total
) {
}
