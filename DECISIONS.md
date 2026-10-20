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


## Running and Testing the API Locally

### Prerequisites

- Java 21 or compatible Java version configured for the project
- Maven 3.9+
- PowerShell on Windows
- No external database required; the application uses an in-memory H2 database.

### 1. Start the application

From the project root (the directory containing `pom.xml`), run:

```powershell
mvn clean test
mvn spring-boot:run
```

Wait for Spring Boot to finish starting. The API will be available at `http://localhost:8080`.

Keep this terminal running. Open a **second PowerShell terminal** for the following commands.

### 2. Configure the base URL

```powershell
$baseUrl = "http://localhost:8080"
```

### 3. Create a cart

```powershell
$cart = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts"

$cart | ConvertTo-Json -Depth 10

$cartId = $cart.cartId
Write-Host "Cart ID: $cartId"
```

Save the returned cart ID. The commands below use the IDs returned by previous requests rather than assuming that IDs start at a particular value.

### 4. Add products to the cart

The application seeds these products on startup:

| Product ID (fresh database) | Product | Unit price | Initial inventory |
|---|---|---:|---:|
| 1 | Wireless Mouse | 799.00 | 50 |
| 2 | Mechanical Keyboard | 2499.00 | 25 |
| 3 | USB-C Hub | 1499.00 | 30 |
| 4 | Webcam | 3299.00 | 10 |
| 5 | Premium Headphones | 8999.00 | 2 |

Add two wireless mice:

```powershell
$body = @{
    productId = 1
    quantity  = 2
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts/$cartId/items" `
  -ContentType "application/json" `
  -Body $body |
  ConvertTo-Json -Depth 10
```

Add one mechanical keyboard:

```powershell
$body = @{
    productId = 2
    quantity  = 1
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts/$cartId/items" `
  -ContentType "application/json" `
  -Body $body |
  ConvertTo-Json -Depth 10
```

### 5. Retrieve the cart

```powershell
Invoke-RestMethod -Method Get `
  -Uri "$baseUrl/carts/$cartId" |
  ConvertTo-Json -Depth 10
```

Expected cart total: **4097.00** (2 × 799.00 + 1 × 2499.00).

### 6. Update a cart item

Set the mouse quantity to three:

```powershell
$body = @{
    quantity = 3
} | ConvertTo-Json

Invoke-RestMethod -Method Patch `
  -Uri "$baseUrl/carts/$cartId/items/1" `
  -ContentType "application/json" `
  -Body $body |
  ConvertTo-Json -Depth 10
```

Here, `1` in the URL is the product ID, not the cart item ID. Use the actual product ID associated with the item you want to update.

### 7. Remove a cart item

Remove the keyboard from the cart:

```powershell
Invoke-RestMethod -Method Delete `
  -Uri "$baseUrl/carts/$cartId/items/2" |
  ConvertTo-Json -Depth 10
```

If you want to keep the original cart total for the checkout example, skip the update and removal steps or create a new cart.

### 8. Checkout

Checkout requires an `Idempotency-Key` header. The same key must be reused when retrying the same checkout request.

```powershell
$checkoutBody = @{
    couponId = $null
} | ConvertTo-Json

$order = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts/$cartId/checkout" `
  -Headers @{ "Idempotency-Key" = "checkout-demo-001" } `
  -ContentType "application/json" `
  -Body $checkoutBody

$order | ConvertTo-Json -Depth 10

$orderId = $order.orderId
Write-Host "Order ID: $orderId"
```

The response includes the order items, gross amount, discount, net amount, status, and creation timestamp.

### 9. Verify checkout idempotency

Repeat the same checkout using the **same cart, idempotency key, and request body**:

```powershell
$retry = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts/$cartId/checkout" `
  -Headers @{ "Idempotency-Key" = "checkout-demo-001" } `
  -ContentType "application/json" `
  -Body $checkoutBody

$retry | ConvertTo-Json -Depth 10

Write-Host "Original order ID: $orderId"
Write-Host "Retry order ID:    $($retry.orderId)"

if ($orderId -eq $retry.orderId) {
    Write-Host "PASS: Retry returned the original order."
} else {
    Write-Host "FAIL: Order IDs do not match."
}
```

Expected: the same order ID is returned, and inventory is not decremented a second time.

### 10. Retrieve an order

```powershell
Invoke-RestMethod -Method Get `
  -Uri "$baseUrl/orders/$orderId" |
  ConvertTo-Json -Depth 10
```

### 11. Generate a reward coupon

The default configuration awards a coupon for every five successful orders, with a 10% discount:

```properties
app.rewards.milestone=5
app.rewards.discount-percent=10
```

After the required number of successful orders has been reached, generate an eligible coupon:

```powershell
$coupon = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/admin/coupons/generate"

$coupon | ConvertTo-Json -Depth 10

$couponId = $coupon.couponId
Write-Host "Coupon ID: $couponId"
```

The endpoint generates one eligible milestone coupon per request. If no milestone has been reached, or all eligible milestone coupons have already been generated, the request will fail.

### 12. Redeem a coupon at checkout

Create another cart:

```powershell
$cart2 = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts"

$cartId2 = $cart2.cartId
Write-Host "New cart ID: $cartId2"
```

Add one USB-C Hub:

```powershell
$body = @{
    productId = 3
    quantity  = 1
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts/$cartId2/items" `
  -ContentType "application/json" `
  -Body $body |
  ConvertTo-Json -Depth 10
```

Checkout with the generated coupon:

```powershell
$checkoutWithCoupon = @{
    couponId = $couponId
} | ConvertTo-Json

$discountedOrder = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts/$cartId2/checkout" `
  -Headers @{ "Idempotency-Key" = "checkout-coupon-demo-001" } `
  -ContentType "application/json" `
  -Body $checkoutWithCoupon

$discountedOrder | ConvertTo-Json -Depth 10
```

Expected values for one USB-C Hub, assuming the seeded price and a 10% discount:

| Field | Expected value |
|---|---:|
| Gross amount | 1499.00 |
| Discount amount | 149.90 |
| Net amount | 1349.10 |

### 13. View the admin report

```powershell
$report = Invoke-RestMethod -Method Get `
  -Uri "$baseUrl/admin/report"

$report | ConvertTo-Json -Depth 10
```

The report includes product quantities sold, gross revenue, total discounts, net revenue, coupons generated, coupons available, coupons redeemed, and successful order count.

Check these reconciliation rules:

- `netRevenue = grossRevenue - totalDiscounts`
- `couponsGenerated = couponsAvailable + couponsRedeemed`

Revenue values are based on successful orders.

### 14. Test validation errors

Try adding a product with quantity zero:

```powershell
$badBody = @{
    productId = 1
    quantity  = 0
} | ConvertTo-Json

try {
    Invoke-RestMethod -Method Post `
      -Uri "$baseUrl/carts/$cartId/items" `
      -ContentType "application/json" `
      -Body $badBody
} catch {
    Write-Host "Expected validation error:"
    Write-Host $_.ErrorDetails.Message
}
```

Expected: a validation error because quantity must be positive.

### 15. Test an already checked-out cart

Use a **new idempotency key** with the cart that has already been checked out:

```powershell
try {
    Invoke-RestMethod -Method Post `
      -Uri "$baseUrl/carts/$cartId/checkout" `
      -Headers @{ "Idempotency-Key" = "checkout-demo-new-key" } `
      -ContentType "application/json" `
      -Body $checkoutBody
} catch {
    Write-Host "Expected checkout conflict:"
    Write-Host $_.ErrorDetails.Message
}
```

Expected: an `INVALID_CART_STATE` conflict. Using the original idempotency key instead should return the original order.

### 16. Test insufficient inventory

On a fresh database, create a cart and attempt to purchase more premium headphones than are available:

```powershell
$cart3 = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts"

$cartId3 = $cart3.cartId

$body = @{
    productId = 5
    quantity  = 3
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/carts/$cartId3/items" `
  -ContentType "application/json" `
  -Body $body

$checkoutBody3 = @{
    couponId = $null
} | ConvertTo-Json

try {
    Invoke-RestMethod -Method Post `
      -Uri "$baseUrl/carts/$cartId3/checkout" `
      -Headers @{ "Idempotency-Key" = "checkout-inventory-demo-001" } `
      -ContentType "application/json" `
      -Body $checkoutBody3
} catch {
    Write-Host "Expected inventory conflict:"
    Write-Host $_.ErrorDetails.Message
}
```

Expected: `INSUFFICIENT_INVENTORY`. Adding an item to a cart does not reserve stock; availability is enforced at checkout. This example assumes the initial inventory of two headphones has not already been consumed.

### API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/carts` | Create a cart |
| `GET` | `/carts/{cartId}` | Retrieve a cart |
| `POST` | `/carts/{cartId}/items` | Add a product |
| `PATCH` | `/carts/{cartId}/items/{productId}` | Update quantity |
| `DELETE` | `/carts/{cartId}/items/{productId}` | Remove a product |
| `POST` | `/carts/{cartId}/checkout` | Checkout with an optional coupon |
| `GET` | `/orders/{orderId}` | Retrieve an order |
| `POST` | `/admin/coupons/generate` | Generate an eligible coupon |
| `GET` | `/admin/report` | Retrieve the admin report |

### Notes

- The application uses an in-memory H2 database. Restarting it resets products, carts, orders, idempotency records, and coupons.
- Product IDs in the examples assume a fresh database with the seed data loaded.
- The application does not integrate with a real payment provider. A successful checkout represents successful checkout completion for this assignment.
- The admin endpoints are not protected by authentication because authentication is outside the assignment scope.
