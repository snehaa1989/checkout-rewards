package com.example.checkout_rewards.exception;


public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(Long productId) {
        super("Insufficient inventory for product: " + productId);
    }
}
