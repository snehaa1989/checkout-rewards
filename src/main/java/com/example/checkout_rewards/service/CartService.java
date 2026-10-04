package com.example.checkout_rewards.service;

import com.example.checkout_rewards.dto.AddCartItemRequest;
import com.example.checkout_rewards.dto.CartItemResponse;
import com.example.checkout_rewards.dto.CartResponse;
import com.example.checkout_rewards.dto.UpdateCartItemRequest;
import com.example.checkout_rewards.entity.Cart;
import com.example.checkout_rewards.entity.CartItem;
import com.example.checkout_rewards.entity.CartStatus;
import com.example.checkout_rewards.entity.Product;
import com.example.checkout_rewards.exception.CartNotFoundException;
import com.example.checkout_rewards.exception.ProductNotFoundException;
import com.example.checkout_rewards.exception.InvalidCartStateException;
import com.example.checkout_rewards.repository.CartItemRepository;
import com.example.checkout_rewards.repository.CartRepository;
import com.example.checkout_rewards.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartResponse createCart() {
        Cart cart = new Cart();
        Cart savedCart = cartRepository.save(cart);

        return toResponse(savedCart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long cartId) {
        Cart cart = findCart(cartId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long cartId, AddCartItemRequest request) {

        Cart cart = findCart(cartId);
        ensureCartIsOpen(cart);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() ->
                        new ProductNotFoundException(request.productId()));

        CartItem existingItem = cart.getItems()
                .stream()
                .filter(item ->
                        item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(
                    existingItem.getQuantity() + request.quantity()
            );
        } else {
            CartItem item = new CartItem(product, request.quantity());
            cart.addItem(item);
        }

        cartRepository.save(cart);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(
            Long cartId,
            Long productId,
            UpdateCartItemRequest request
    ) {

        Cart cart = findCart(cartId);
        ensureCartIsOpen(cart);

        CartItem item = findCartItem(cart, productId);

        item.setQuantity(request.quantity());

        cartRepository.save(cart);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long cartId, Long productId) {

        Cart cart = findCart(cartId);
        ensureCartIsOpen(cart);

        CartItem item = findCartItem(cart, productId);

        cart.removeItem(item);

        cartRepository.save(cart);

        return toResponse(cart);
    }

    private Cart findCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new CartNotFoundException(cartId));
    }

    private CartItem findCartItem(Cart cart, Long productId) {

        return cart.getItems()
                .stream()
                .filter(item ->
                        item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new ProductNotFoundException(productId));
    }

    private void ensureCartIsOpen(Cart cart) {

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    "Cart " + cart.getId() + " is already checked out"
            );
        }
    }

    private CartResponse toResponse(Cart cart) {

        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(item -> {

                    BigDecimal lineTotal =
                            item.getProduct()
                                    .getUnitPrice()
                                    .multiply(
                                            BigDecimal.valueOf(item.getQuantity())
                                    );

                    return new CartItemResponse(
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getProduct().getUnitPrice(),
                            lineTotal
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId(),
                cart.getStatus().name(),
                items,
                total
        );
    }
}
