package com.example.checkout_rewards.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupon_milestone",
                        columnNames = "milestoneOrderCount"
                )
        }
)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int milestoneOrderCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime redeemedAt;

    protected Coupon() {
    }

    public Coupon(int milestoneOrderCount) {
        this.milestoneOrderCount = milestoneOrderCount;
        this.status = CouponStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public int getMilestoneOrderCount() {
        return milestoneOrderCount;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void redeem() {
        if (status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("Coupon is not available");
        }

        status = CouponStatus.REDEEMED;
        redeemedAt = LocalDateTime.now();
    }
}
