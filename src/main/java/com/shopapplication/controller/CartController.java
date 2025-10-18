package com.shopapplication.controller;

import com.shopapplication.dto.CartItemRequest;
import com.shopapplication.dto.CartResponse;
import com.shopapplication.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart() {
        try {
            CartResponse cart = cartService.getCart();
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody CartItemRequest request) {
        try {
            CartResponse cart = cartService.addToCart(request);
            return ResponseEntity.ok().body(Map.of(
                "message", "Item added to cart successfully",
                "cart", cart
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long productId, @RequestBody Map<String, Integer> body) {
        try {
            Integer quantity = body.get("quantity");
            CartResponse cart = cartService.updateCartItem(productId, quantity);
            return ResponseEntity.ok().body(Map.of(
                "message", "Cart updated successfully",
                "cart", cart
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long productId) {
        try {
            CartResponse cart = cartService.removeFromCart(productId);
            return ResponseEntity.ok().body(Map.of(
                "message", "Item removed from cart successfully",
                "cart", cart
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart() {
        try {
            cartService.clearCart();
            return ResponseEntity.ok().body(Map.of("message", "Cart cleared successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCartItemCount() {
        try {
            int count = cartService.getCartItemCount();
            return ResponseEntity.ok().body(Map.of("count", count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
