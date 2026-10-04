package com.example.checkout_rewards.controller;


import com.example.checkout_rewards.dto.CheckoutRequest;
import com.example.checkout_rewards.dto.OrderResponse;
import com.example.checkout_rewards.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/{cartId}/checkout")
    public OrderResponse checkout(
            @PathVariable Long cartId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return checkoutService.checkout(
                cartId,
                idempotencyKey,
                request
        );
    }
}
