package com.example.checkout_rewards.dto;

import java.time.LocalDateTime;

public record CouponResponse(
        Long couponId,
        int milestoneOrderCount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime redeemedAt
) {
}