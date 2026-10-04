package com.example.checkout_rewards.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartNotFound(
            CartNotFoundException ex
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "CART_NOT_FOUND",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(
            ProductNotFoundException ex
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidCartStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCartState(
            InvalidCartStateException ex
    ) {
        return error(
                HttpStatus.CONFLICT,
                "INVALID_CART_STATE",
                ex.getMessage()
        );
    }
    @ExceptionHandler(InsufficientInventoryException.class)
public ResponseEntity<Map<String, Object>> handleInsufficientInventory(
        InsufficientInventoryException ex
) {
    return error(
            HttpStatus.CONFLICT,
            "INSUFFICIENT_INVENTORY",
            ex.getMessage()
    );
}

@ExceptionHandler(CouponNotFoundException.class)
public ResponseEntity<Map<String, Object>> handleCouponNotFound(
        CouponNotFoundException ex
) {
    return error(
            HttpStatus.NOT_FOUND,
            "COUPON_NOT_FOUND",
            ex.getMessage()
    );
}

@ExceptionHandler(CouponNotAvailableException.class)
public ResponseEntity<Map<String, Object>> handleCouponNotAvailable(
        CouponNotAvailableException ex
) {
    return error(
            HttpStatus.CONFLICT,
            "COUPON_NOT_AVAILABLE",
            ex.getMessage()
    );
}

@ExceptionHandler(IdempotencyConflictException.class)
public ResponseEntity<Map<String, Object>> handleIdempotencyConflict(
        IdempotencyConflictException ex
) {
    return error(
            HttpStatus.CONFLICT,
            "IDEMPOTENCY_CONFLICT",
            ex.getMessage()
    );
}
@ExceptionHandler(OrderNotFoundException.class)
public ResponseEntity<Map<String, Object>> handleOrderNotFound(
        OrderNotFoundException ex
) {
    return error(
            HttpStatus.NOT_FOUND,
            "ORDER_NOT_FOUND",
            ex.getMessage()
    );
}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .orElse("Invalid request");

        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message
        );
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String code,
            String message
    ) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("error", Map.of(
                "code", code,
                "message", message
        ));

        return ResponseEntity
                .status(status)
                .body(body);
    }
}
