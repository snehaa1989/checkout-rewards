package com.example.checkout_rewards.entity;


import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int availableInventory;

    @Version
    private Long version;

    protected Product() {
    }

    public Product(
            String name,
            BigDecimal unitPrice,
            int availableInventory
    ) {
        this.name = name;
        this.unitPrice = unitPrice;
        this.availableInventory = availableInventory;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getAvailableInventory() {
        return availableInventory;
    }

    public Long getVersion() {
        return version;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setAvailableInventory(int availableInventory) {
        this.availableInventory = availableInventory;
    }
}
