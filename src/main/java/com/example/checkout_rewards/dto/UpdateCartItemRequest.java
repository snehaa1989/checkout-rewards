package com.example.checkout_rewards.dto;

import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(
        @Positive
        int quantity
) {
}
