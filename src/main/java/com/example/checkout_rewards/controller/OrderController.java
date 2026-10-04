package com.example.checkout_rewards.controller;

import com.example.checkout_rewards.dto.OrderResponse;
import com.example.checkout_rewards.entity.Order;
import com.example.checkout_rewards.exception.OrderNotFoundException;
import com.example.checkout_rewards.repository.OrderRepository;
import com.example.checkout_rewards.service.CheckoutService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final CheckoutService checkoutService;

    public OrderController(
            OrderRepository orderRepository,
            CheckoutService checkoutService
    ) {
        this.orderRepository = orderRepository;
        this.checkoutService = checkoutService;
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(orderId)
                );

        return checkoutService.toResponseForController(order);
    }
}