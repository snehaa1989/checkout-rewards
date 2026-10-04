package com.example.checkout_rewards.config;

import com.example.checkout_rewards.entity.Product;
import com.example.checkout_rewards.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepository) {
        return args -> {

            System.out.println("=== SEEDING PRODUCTS ===");

            productRepository.save(
                    new Product(
                            "Wireless Mouse",
                            new BigDecimal("799.00"),
                            50
                    )
            );

            productRepository.save(
                    new Product(
                            "Mechanical Keyboard",
                            new BigDecimal("2499.00"),
                            25
                    )
            );

            productRepository.save(
                    new Product(
                            "USB-C Hub",
                            new BigDecimal("1499.00"),
                            30
                    )
            );

            productRepository.save(
                    new Product(
                            "Webcam",
                            new BigDecimal("3299.00"),
                            10
                    )
            );

            productRepository.save(
                    new Product(
                            "Premium Headphones",
                            new BigDecimal("8999.00"),
                            2
                    )
            );

            System.out.println(
                    "=== SEEDED " +
                    productRepository.count() +
                    " PRODUCTS ==="
            );
        };
    }
}