package com.example.checkout_rewards.repository;



import com.example.checkout_rewards.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("""
            UPDATE Product p
            SET p.availableInventory = p.availableInventory - :quantity
            WHERE p.id = :productId
              AND p.availableInventory >= :quantity
            """)
    int decreaseInventory(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}