package com.example.checkout_rewards.entity;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    private Long couponId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(
            Long cartId,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal netAmount,
            Long couponId
    ) {
        this.cartId = cartId;
        this.status = OrderStatus.SUCCESS;
        this.grossAmount = grossAmount;
        this.discountAmount = discountAmount;
        this.netAmount = netAmount;
        this.couponId = couponId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCartId() {
        return cartId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public Long getCouponId() {
        return couponId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
