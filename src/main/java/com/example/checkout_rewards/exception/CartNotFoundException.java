package com.example.checkout_rewards.exception;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(Long cartId) {
        super("Cart not found: " + cartId);
    }
}
