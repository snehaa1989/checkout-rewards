package com.example.checkout_rewards.controller;

import com.example.checkout_rewards.dto.AddCartItemRequest;
import com.example.checkout_rewards.dto.CartResponse;
import com.example.checkout_rewards.dto.UpdateCartItemRequest;
import com.example.checkout_rewards.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse createCart() {
        return cartService.createCart();
    }

    @GetMapping("/{cartId}")
    public CartResponse getCart(@PathVariable Long cartId) {
        return cartService.getCart(cartId);
    }

    @PostMapping("/{cartId}/items")
    public CartResponse addItem(
            @PathVariable Long cartId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(cartId, request);
    }

    @PatchMapping("/{cartId}/items/{productId}")
    public CartResponse updateItem(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(
                cartId,
                productId,
                request
        );
    }

    @DeleteMapping("/{cartId}/items/{productId}")
    public CartResponse removeItem(
            @PathVariable Long cartId,
            @PathVariable Long productId
    ) {
        return cartService.removeItem(cartId, productId);
    }
}
