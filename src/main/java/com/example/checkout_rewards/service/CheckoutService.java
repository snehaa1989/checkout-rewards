package com.example.checkout_rewards.service;

import com.example.checkout_rewards.dto.CheckoutRequest;
import com.example.checkout_rewards.dto.OrderItemResponse;
import com.example.checkout_rewards.dto.OrderResponse;
import com.example.checkout_rewards.entity.Cart;
import com.example.checkout_rewards.entity.CartStatus;
import com.example.checkout_rewards.entity.Coupon;
import com.example.checkout_rewards.entity.CouponStatus;
import com.example.checkout_rewards.entity.Order;
import com.example.checkout_rewards.entity.OrderItem;
import com.example.checkout_rewards.exception.CartNotFoundException;
import com.example.checkout_rewards.exception.CouponNotAvailableException;
import com.example.checkout_rewards.exception.CouponNotFoundException;
import com.example.checkout_rewards.exception.InsufficientInventoryException;
import com.example.checkout_rewards.exception.InvalidCartStateException;
import com.example.checkout_rewards.entity.IdempotencyRecord;
import com.example.checkout_rewards.repository.CartRepository;
import com.example.checkout_rewards.repository.CouponRepository;
import com.example.checkout_rewards.repository.IdempotencyRecordRepository;
import com.example.checkout_rewards.repository.OrderRepository;
import com.example.checkout_rewards.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    private final BigDecimal discountPercent;

    public CheckoutService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CouponRepository couponRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            @Value("${app.rewards.discount-percent}") BigDecimal discountPercent
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.discountPercent = discountPercent;
    }

    @Transactional
    public OrderResponse checkout(
            Long cartId,
            String idempotencyKey,
            CheckoutRequest request
    ) {

        // 1. Check whether this checkout was already completed
        var existingRecord =
                idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {
            Order existingOrder = orderRepository.findById(
                    existingRecord.get().getOrderId()
            ).orElseThrow(() ->
                    new IllegalStateException(
                            "Idempotency record references missing order"
                    )
            );

            return toResponseForController(existingOrder);
        }

        // 2. Load and lock the cart
Cart cart = cartRepository.findByIdForUpdate(cartId)
        .orElseThrow(() -> new CartNotFoundException(cartId));

// Another request using the same idempotency key may have
// completed while this request was waiting for the cart lock.
var recordAfterLock =
        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

if (recordAfterLock.isPresent()) {

    Order existingOrder = orderRepository.findById(
            recordAfterLock.get().getOrderId()
    ).orElseThrow(() ->
            new IllegalStateException(
                    "Idempotency record references missing order"
            )
    );

    return toResponseForController(existingOrder);
}

        // 3. Cart must still be open
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    "Cart " + cartId + " is already checked out"
            );
        }

        // 4. Cart must contain at least one item
        if (cart.getItems().isEmpty()) {
            throw new InvalidCartStateException(
                    "Cannot checkout an empty cart"
            );
        }

        // 5. Validate coupon if one was provided
        Coupon coupon = null;

        if (request.couponId() != null) {

            coupon = couponRepository.findByIdForUpdate(request.couponId())
        .orElseThrow(() ->
                new CouponNotFoundException(request.couponId()));

            if (coupon.getStatus() != CouponStatus.AVAILABLE) {
                throw new CouponNotAvailableException(request.couponId());
            }
        }

        // 6. Calculate gross amount using current product prices
        BigDecimal grossAmount = BigDecimal.ZERO;

        for (var cartItem : cart.getItems()) {

            BigDecimal unitPrice =
                    cartItem.getProduct().getUnitPrice();

            BigDecimal lineTotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(cartItem.getQuantity())
                    );

            grossAmount = grossAmount.add(lineTotal);
        }

        // 7. Calculate discount
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (coupon != null) {

            discountAmount = grossAmount
                    .multiply(discountPercent)
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        // 8. Calculate final amount
        BigDecimal netAmount =
                grossAmount.subtract(discountAmount);

        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            netAmount = BigDecimal.ZERO;
        }

        // 9. Atomically reserve inventory
        for (var cartItem : cart.getItems()) {

            int updatedRows = productRepository.decreaseInventory(
                    cartItem.getProduct().getId(),
                    cartItem.getQuantity()
            );

            if (updatedRows == 0) {
                throw new InsufficientInventoryException(
                        cartItem.getProduct().getId()
                );
            }
        }

        // 10. Create order
        Order order = new Order(
                cart.getId(),
                grossAmount,
                discountAmount,
                netAmount,
                coupon != null ? coupon.getId() : null
        );

        // 11. Create immutable order-item snapshots
        for (var cartItem : cart.getItems()) {

            var product = cartItem.getProduct();

            BigDecimal unitPrice = product.getUnitPrice();

            BigDecimal lineTotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(cartItem.getQuantity())
                    );

            OrderItem orderItem = new OrderItem(
                    product.getId(),
                    product.getName(),
                    cartItem.getQuantity(),
                    unitPrice,
                    lineTotal
            );

            order.addItem(orderItem);
        }

        // 12. Redeem coupon
        if (coupon != null) {
            coupon.redeem();
        }

        // 13. Mark cart as checked out
        cart.markCheckedOut();

        // 14. Save order
        Order savedOrder = orderRepository.save(order);

        // 15. Save idempotency record
        IdempotencyRecord idempotencyRecord =
                new IdempotencyRecord(
                        idempotencyKey,
                        savedOrder.getId()
                );

        idempotencyRecordRepository.save(idempotencyRecord);

        // 16. Return created order
        return toResponseForController(savedOrder);
    }

    public OrderResponse toResponseForController(Order order) {

        var items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCartId(),
                order.getStatus().name(),
                items,
                order.getGrossAmount(),
                order.getDiscountAmount(),
                order.getNetAmount(),
                order.getCouponId(),
                order.getCreatedAt()
        );
    }
}