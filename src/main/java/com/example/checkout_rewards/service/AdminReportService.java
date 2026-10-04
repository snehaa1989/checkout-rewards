package com.example.checkout_rewards.service;

import com.example.checkout_rewards.dto.AdminReportResponse;
import com.example.checkout_rewards.dto.ProductSalesResponse;
import com.example.checkout_rewards.entity.Order;
import com.example.checkout_rewards.entity.OrderItem;
import com.example.checkout_rewards.entity.OrderStatus;
import com.example.checkout_rewards.entity.CouponStatus;
import com.example.checkout_rewards.repository.CouponRepository;
import com.example.checkout_rewards.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportService {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;

    public AdminReportService(
            OrderRepository orderRepository,
            CouponRepository couponRepository
    ) {
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    public AdminReportResponse generateReport() {

        List<Order> successfulOrders =
                orderRepository.findAllByStatus(OrderStatus.SUCCESS);

        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;

        Map<Long, ProductSalesAccumulator> productSales =
                new LinkedHashMap<>();

        for (Order order : successfulOrders) {

            grossRevenue = grossRevenue.add(order.getGrossAmount());
            totalDiscounts =
                    totalDiscounts.add(order.getDiscountAmount());
            netRevenue = netRevenue.add(order.getNetAmount());

            for (OrderItem item : order.getItems()) {

                ProductSalesAccumulator accumulator =
                        productSales.computeIfAbsent(
                                item.getProductId(),
                                id -> new ProductSalesAccumulator(
                                        item.getProductName()
                                )
                        );

                accumulator.quantitySold += item.getQuantity();
            }
        }

        List<ProductSalesResponse> productSalesResponse =
                new ArrayList<>();

        for (Map.Entry<Long, ProductSalesAccumulator> entry
                : productSales.entrySet()) {

            productSalesResponse.add(
                    new ProductSalesResponse(
                            entry.getKey(),
                            entry.getValue().productName,
                            entry.getValue().quantitySold
                    )
            );
        }

        long couponsGenerated =
                couponRepository.count();

        long couponsAvailable =
                couponRepository.countByStatus(
                        CouponStatus.AVAILABLE
                );

        long couponsRedeemed =
                couponRepository.countByStatus(
                        CouponStatus.REDEEMED
                );

        return new AdminReportResponse(
                productSalesResponse,
                grossRevenue,
                totalDiscounts,
                netRevenue,
                couponsGenerated,
                couponsAvailable,
                couponsRedeemed,
                successfulOrders.size()
        );
    }

    private static class ProductSalesAccumulator {

        private final String productName;
        private long quantitySold;

        private ProductSalesAccumulator(String productName) {
            this.productName = productName;
        }
    }
}