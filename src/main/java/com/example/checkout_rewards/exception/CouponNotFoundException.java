package com.example.checkout_rewards.exception;


public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(Long couponId) {
        super("Coupon not found: " + couponId);
    }
}
