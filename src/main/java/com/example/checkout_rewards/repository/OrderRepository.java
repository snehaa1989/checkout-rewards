package com.example.checkout_rewards.repository;

import com.example.checkout_rewards.entity.Order;
import com.example.checkout_rewards.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByStatus(OrderStatus status);

    @Query("""
            SELECT o
            FROM Order o
            WHERE o.status = :status
            """)
    List<Order> findAllByStatus(OrderStatus status);
}