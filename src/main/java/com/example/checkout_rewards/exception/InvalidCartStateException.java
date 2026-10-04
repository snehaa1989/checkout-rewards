package com.example.checkout_rewards.exception;
public class InvalidCartStateException extends RuntimeException {

    public InvalidCartStateException(String message) {
        super(message);
    }
}
