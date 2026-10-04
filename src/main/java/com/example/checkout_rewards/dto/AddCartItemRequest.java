package com.example.checkout_rewards.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull
        Long productId,

        @Positive
        int quantity
) {
}