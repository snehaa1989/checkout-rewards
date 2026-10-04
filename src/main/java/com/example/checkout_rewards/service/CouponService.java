package com.example.checkout_rewards.service;

import com.example.checkout_rewards.dto.CouponResponse;
import com.example.checkout_rewards.entity.Coupon;
import com.example.checkout_rewards.entity.CouponStatus;
import com.example.checkout_rewards.entity.OrderStatus;
import com.example.checkout_rewards.repository.CouponRepository;
import com.example.checkout_rewards.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final int milestone;

    public CouponService(
            CouponRepository couponRepository,
            OrderRepository orderRepository,
            @Value("${app.rewards.milestone}") int milestone
    ) {
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.milestone = milestone;
    }

    @Transactional
    public CouponResponse generateCoupon() {

        long successfulOrders =
                orderRepository.countByStatus(OrderStatus.SUCCESS);

        int eligibleMilestone =
                (int) (successfulOrders / milestone) * milestone;

        if (eligibleMilestone == 0) {
            throw new IllegalStateException(
                    "No reward milestone has been reached yet"
            );
        }

        for (int currentMilestone = milestone;
             currentMilestone <= eligibleMilestone;
             currentMilestone += milestone) {

            boolean alreadyGenerated =
        couponRepository
                .findByMilestoneOrderCount(currentMilestone)
                .isPresent();

            if (!alreadyGenerated) {
                Coupon coupon = new Coupon(currentMilestone);
                Coupon savedCoupon = couponRepository.save(coupon);

                return toResponse(savedCoupon);
            }
        }

        throw new IllegalStateException(
                "All eligible milestone coupons have already been generated"
        );
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getMilestoneOrderCount(),
                coupon.getStatus().name(),
                coupon.getCreatedAt(),
                coupon.getRedeemedAt()
        );
    }
}