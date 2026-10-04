package com.example.checkout_rewards;
import com.example.checkout_rewards.exception.InsufficientInventoryException;
import com.example.checkout_rewards.dto.AdminReportResponse;
import com.example.checkout_rewards.service.AdminReportService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.example.checkout_rewards.entity.Coupon;
import com.example.checkout_rewards.entity.CouponStatus;
import com.example.checkout_rewards.repository.CouponRepository;
import com.example.checkout_rewards.repository.IdempotencyRecordRepository;
import com.example.checkout_rewards.repository.OrderRepository;
import com.example.checkout_rewards.exception.CouponNotAvailableException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.example.checkout_rewards.dto.AddCartItemRequest;
import com.example.checkout_rewards.dto.CheckoutRequest;
import com.example.checkout_rewards.dto.OrderResponse;
import com.example.checkout_rewards.entity.Cart;
import com.example.checkout_rewards.entity.Product;
import com.example.checkout_rewards.repository.CartRepository;
import com.example.checkout_rewards.repository.ProductRepository;
import com.example.checkout_rewards.service.CartService;
import com.example.checkout_rewards.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutRewardsApplicationTests {

    @Autowired
    private CartRepository cartRepository;
@Autowired
private CouponRepository couponRepository;
    @Autowired
    private ProductRepository productRepository;
@Autowired
private OrderRepository orderRepository;

@Autowired
private IdempotencyRecordRepository idempotencyRecordRepository;
    @Autowired
    private CartService cartService;
@Autowired
private AdminReportService adminReportService;
    @Autowired
    private CheckoutService checkoutService;
    private Long testProductId;
    
    @BeforeEach
void setUp() {

    idempotencyRecordRepository.deleteAll();
    couponRepository.deleteAll();
    orderRepository.deleteAll();
    cartRepository.deleteAll();
    productRepository.deleteAll();

    Product product = productRepository.save(
            new Product(
                    "Test Mouse",
                    new BigDecimal("100.00"),
                    10
            )
    );

    testProductId = product.getId();
}

    @Test
    void repeatedCheckoutWithSameIdempotencyKeyReturnsSameOrder() {

        Cart cart = cartRepository.save(new Cart());

        cartService.addItem(
                cart.getId(),
                new AddCartItemRequest(testProductId, 2)
        );

        CheckoutRequest request = new CheckoutRequest(null);

        OrderResponse first =
                checkoutService.checkout(
                        cart.getId(),
                        "test-key-001",
                        request
                );

        OrderResponse second =
                checkoutService.checkout(
                        cart.getId(),
                        "test-key-001",
                        request
                );

        assertEquals(
                first.orderId(),
                second.orderId()
        );

        assertEquals(
                new BigDecimal("200.00"),
                first.grossAmount()
        );

        assertEquals(
                new BigDecimal("200.00"),
                second.grossAmount()
        );

        Product product =
                productRepository.findById(testProductId).orElseThrow();

        assertEquals(
                8,
                product.getAvailableInventory()
        );
    }
	@Test
void concurrentCheckoutsCannotOversellInventory() throws Exception {

    // Create a product with only ONE unit available
    Product product = productRepository.save(
            new Product(
                    "Limited Product",
                    new BigDecimal("500.00"),
                    1
            )
    );

    // Create two separate carts
    Cart cart1 = cartRepository.save(new Cart());
    Cart cart2 = cartRepository.save(new Cart());

    cartService.addItem(
            cart1.getId(),
            new AddCartItemRequest(product.getId(), 1)
    );

    cartService.addItem(
            cart2.getId(),
            new AddCartItemRequest(product.getId(), 1)
    );

    CheckoutRequest request = new CheckoutRequest(null);

    ExecutorService executor = Executors.newFixedThreadPool(2);

    CountDownLatch start = new CountDownLatch(1);

    Future<OrderResponse> checkout1 = executor.submit(() -> {
        start.await();

        return checkoutService.checkout(
                cart1.getId(),
                "concurrent-key-1",
                request
        );
    });

    Future<OrderResponse> checkout2 = executor.submit(() -> {
        start.await();

        return checkoutService.checkout(
                cart2.getId(),
                "concurrent-key-2",
                request
        );
    });

    // Start both checkouts at approximately the same time
    start.countDown();

    int successfulCheckouts = 0;
    int failedCheckouts = 0;

    try {
        checkout1.get();
        successfulCheckouts++;
    } catch (Exception ex) {
        failedCheckouts++;
    }

    try {
        checkout2.get();
        successfulCheckouts++;
    } catch (Exception ex) {
        failedCheckouts++;
    }

    executor.shutdown();

    // Exactly one checkout must succeed
    assertEquals(1, successfulCheckouts);

    // Exactly one checkout must fail
    assertEquals(1, failedCheckouts);

    // Inventory must never become negative
    Product updatedProduct =
            productRepository.findById(product.getId())
                    .orElseThrow();

    assertEquals(
            0,
            updatedProduct.getAvailableInventory()
    );
}
@Test
void concurrentCheckoutOfSameCartCreatesOnlyOneOrder() throws Exception {

    Product product = productRepository.save(
            new Product(
                    "Test Keyboard",
                    new BigDecimal("1000.00"),
                    10
            )
    );

    Cart cart = cartRepository.save(new Cart());

    cartService.addItem(
            cart.getId(),
            new AddCartItemRequest(product.getId(), 1)
    );

    CheckoutRequest request = new CheckoutRequest(null);

    ExecutorService executor = Executors.newFixedThreadPool(2);

    CountDownLatch start = new CountDownLatch(1);

    Future<OrderResponse> checkout1 = executor.submit(() -> {
        start.await();

        return checkoutService.checkout(
                cart.getId(),
                "same-cart-key-1",
                request
        );
    });

    Future<OrderResponse> checkout2 = executor.submit(() -> {
        start.await();

        return checkoutService.checkout(
                cart.getId(),
                "same-cart-key-2",
                request
        );
    });

    start.countDown();

    int successfulCheckouts = 0;
    int failedCheckouts = 0;

    try {
        checkout1.get();
        successfulCheckouts++;
    } catch (Exception ex) {
        failedCheckouts++;
    }

    try {
        checkout2.get();
        successfulCheckouts++;
    } catch (Exception ex) {
        failedCheckouts++;
    }

    executor.shutdown();

    // Exactly one checkout should succeed
    assertEquals(1, successfulCheckouts);

    // Exactly one should fail because the cart is already checked out
    assertEquals(1, failedCheckouts);

    // Inventory should have decreased only once
    Product updatedProduct =
            productRepository.findById(product.getId())
                    .orElseThrow();

    assertEquals(
            9,
            updatedProduct.getAvailableInventory()
    );
}
@Test
void failedCheckoutDoesNotConsumeCouponOrInventory() {

    Product product = productRepository.save(
            new Product(
                    "Rollback Product",
                    new BigDecimal("1000.00"),
                    1
            )
    );

    Coupon coupon = couponRepository.save(
            new Coupon(5)
    );

    Cart cart = cartRepository.save(new Cart());

    cartService.addItem(
            cart.getId(),
            new AddCartItemRequest(product.getId(), 2)
    );

    CheckoutRequest request =
            new CheckoutRequest(coupon.getId());

    assertThrows(
            InsufficientInventoryException.class,
            () -> checkoutService.checkout(
                    cart.getId(),
                    "rollback-test-key",
                    request
            )
    );

    // Inventory must remain unchanged
    Product updatedProduct =
            productRepository.findById(product.getId())
                    .orElseThrow();

    assertEquals(
            1,
            updatedProduct.getAvailableInventory()
    );

    // Coupon must remain available
    Coupon updatedCoupon =
            couponRepository.findById(coupon.getId())
                    .orElseThrow();

    assertEquals(
            CouponStatus.AVAILABLE,
            updatedCoupon.getStatus()
    );

    // Cart must remain open
    Cart updatedCart =
            cartRepository.findById(cart.getId())
                    .orElseThrow();

    assertEquals(
            "OPEN",
            updatedCart.getStatus().name()
    );
}
@Test
void concurrentCheckoutsCannotRedeemSameCouponTwice() throws Exception {

    Product product = productRepository.save(
            new Product(
                    "Coupon Product",
                    new BigDecimal("1000.00"),
                    10
            )
    );

    Coupon coupon = couponRepository.save(
            new Coupon(5)
    );

    Cart cart1 = cartRepository.save(new Cart());
    Cart cart2 = cartRepository.save(new Cart());

    cartService.addItem(
            cart1.getId(),
            new AddCartItemRequest(product.getId(), 1)
    );

    cartService.addItem(
            cart2.getId(),
            new AddCartItemRequest(product.getId(), 1)
    );

    CheckoutRequest request =
            new CheckoutRequest(coupon.getId());

    ExecutorService executor =
            Executors.newFixedThreadPool(2);

    CountDownLatch start =
            new CountDownLatch(1);

    Future<OrderResponse> checkout1 =
            executor.submit(() -> {
                start.await();

                return checkoutService.checkout(
                        cart1.getId(),
                        "coupon-concurrent-key-1",
                        request
                );
            });

    Future<OrderResponse> checkout2 =
            executor.submit(() -> {
                start.await();

                return checkoutService.checkout(
                        cart2.getId(),
                        "coupon-concurrent-key-2",
                        request
                );
            });

    start.countDown();

    int successfulCheckouts = 0;
    int failedCheckouts = 0;

    try {
        checkout1.get();
        successfulCheckouts++;
    } catch (Exception ex) {
        failedCheckouts++;
    }

    try {
        checkout2.get();
        successfulCheckouts++;
    } catch (Exception ex) {
        failedCheckouts++;
    }

    executor.shutdown();

    // Only one checkout can consume the coupon
    assertEquals(1, successfulCheckouts);

    // The other checkout must fail
    assertEquals(1, failedCheckouts);

    Coupon updatedCoupon =
            couponRepository.findById(coupon.getId())
                    .orElseThrow();

    assertEquals(
            CouponStatus.REDEEMED,
            updatedCoupon.getStatus()
    );

    assertEquals(
            1,
            couponRepository.countByStatus(CouponStatus.REDEEMED)
    );
}
@Test
void adminReportReconcilesSuccessfulOrders() {

    Product mouse = productRepository.save(
            new Product(
                    "Report Mouse",
                    new BigDecimal("100.00"),
                    10
            )
    );

    Product keyboard = productRepository.save(
            new Product(
                    "Report Keyboard",
                    new BigDecimal("200.00"),
                    10
            )
    );

    // First successful order:
    // 2 mice = 200
    Cart cart1 = cartRepository.save(new Cart());

    cartService.addItem(
            cart1.getId(),
            new AddCartItemRequest(mouse.getId(), 2)
    );

    OrderResponse order1 =
            checkoutService.checkout(
                    cart1.getId(),
                    "report-key-1",
                    new CheckoutRequest(null)
            );

    // Second successful order:
    // 1 keyboard = 200
    Cart cart2 = cartRepository.save(new Cart());

    cartService.addItem(
            cart2.getId(),
            new AddCartItemRequest(keyboard.getId(), 1)
    );

    OrderResponse order2 =
            checkoutService.checkout(
                    cart2.getId(),
                    "report-key-2",
                    new CheckoutRequest(null)
            );

    AdminReportResponse report =
            adminReportService.generateReport();

    assertEquals(
            2,
            report.successfulOrders()
    );

    assertEquals(
            new BigDecimal("400.00"),
            report.grossRevenue()
    );

    assertEquals(
            new BigDecimal("0.00"),
            report.totalDiscounts()
    );

    assertEquals(
            new BigDecimal("400.00"),
            report.netRevenue()
    );

    assertEquals(
            0,
            report.couponsGenerated()
    );

    assertEquals(
            0,
            report.couponsAvailable()
    );

    assertEquals(
            0,
            report.couponsRedeemed()
    );

    assertEquals(
            2,
            report.productSales().size()
    );
}
@Test
void concurrentRequestsWithSameIdempotencyKeyReturnSameOrder()
        throws Exception {

    Product product = productRepository.save(
            new Product(
                    "Idempotency Product",
                    new BigDecimal("500.00"),
                    10
            )
    );

    Cart cart = cartRepository.save(new Cart());

    cartService.addItem(
            cart.getId(),
            new AddCartItemRequest(product.getId(), 1)
    );

    CheckoutRequest request = new CheckoutRequest(null);

    ExecutorService executor =
            Executors.newFixedThreadPool(2);

    CountDownLatch start =
            new CountDownLatch(1);

    String idempotencyKey =
            "same-concurrent-key";

    Future<OrderResponse> checkout1 =
            executor.submit(() -> {
                start.await();

                return checkoutService.checkout(
                        cart.getId(),
                        idempotencyKey,
                        request
                );
            });

    Future<OrderResponse> checkout2 =
            executor.submit(() -> {
                start.await();

                return checkoutService.checkout(
                        cart.getId(),
                        idempotencyKey,
                        request
                );
            });

    start.countDown();

    OrderResponse order1 = checkout1.get();
    OrderResponse order2 = checkout2.get();

    executor.shutdown();

    assertEquals(
            order1.orderId(),
            order2.orderId()
    );

    assertEquals(
            1,
            orderRepository.count()
    );

    Product updatedProduct =
            productRepository.findById(product.getId())
                    .orElseThrow();

    assertEquals(
            9,
            updatedProduct.getAvailableInventory()
    );
}
}