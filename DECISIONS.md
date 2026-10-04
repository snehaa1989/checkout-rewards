# Engineering Decisions

## 1. Overview

This project implements a reliable checkout and rewards service using Java 21, Spring Boot, Spring Data JPA, and H2.

The primary goal was correctness under retries and concurrent requests rather than maximizing feature breadth.

The implementation focuses on the following invariants:

1. A cart can be successfully checked out at most once.
2. Inventory can never become negative.
3. Concurrent checkouts cannot oversell inventory.
4. A successful checkout creates exactly one order.
5. Repeating a checkout with the same idempotency key returns the same order.
6. A coupon can be redeemed at most once.
7. A failed checkout does not consume inventory or a coupon.
8. Order item pricing and product details are immutable snapshots.
9. Admin reporting is based on successful orders.
10. Monetary calculations use `BigDecimal`, never floating-point arithmetic.

---

# 2. Business Semantics

## 2.1 Inventory is reserved at checkout

Adding an item to a cart does not reserve inventory.

Inventory is validated and atomically decremented during checkout.

### Reason

Reserving inventory when items are added to a cart could allow abandoned carts to hold stock indefinitely. That would require additional reservation expiration and cleanup logic.

### Trade-off

A product can be available when it is added to a cart but unavailable when checkout occurs.

In that case, checkout fails with an `INSUFFICIENT_INVENTORY` conflict.

---

## 2.2 Product price is evaluated at checkout

The cart references the product rather than storing a price snapshot.

Therefore, the current product price is used when checkout occurs.

For example, if a product costs ₹100 when added to the cart and its price changes to ₹120 before checkout, the checkout uses ₹120.

### Reason

This keeps the implementation simple and avoids introducing price-locking or quote-expiration behavior that was outside the assignment scope.

The final order stores the actual unit price used at checkout, so historical orders remain unchanged if the product price changes later.

---

## 2.3 Order items are immutable snapshots

Each `OrderItem` stores:

- Product ID
- Product name
- Quantity
- Unit price
- Line total

Historical orders therefore do not depend on mutable product information.

Changing a product's name or price after checkout does not change the order or historical reporting.

---

# 3. Inventory Concurrency

Inventory is protected using an atomic conditional database update:

```sql
UPDATE products
SET available_inventory = available_inventory - :quantity
WHERE id = :productId
  AND available_inventory >= :quantity