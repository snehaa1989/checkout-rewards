package com.example.checkout_rewards.exception;


public class CouponNotAvailableException extends RuntimeException {

    public CouponNotAvailableException(Long couponId) {
        super("Coupon is no longer available: " + couponId);
    }
}
