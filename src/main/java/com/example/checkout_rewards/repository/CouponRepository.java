package com.example.checkout_rewards.repository;

import com.example.checkout_rewards.entity.Coupon;
import com.example.checkout_rewards.entity.CouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByStatusAndMilestoneOrderCount(
            CouponStatus status,
            int milestoneOrderCount
    );

    Optional<Coupon> findByMilestoneOrderCount(
            int milestoneOrderCount
    );

    long countByStatus(CouponStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :couponId")
    Optional<Coupon> findByIdForUpdate(
            @Param("couponId") Long couponId
    );
}