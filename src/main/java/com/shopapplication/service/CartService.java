package com.shopapplication.service;

import com.shopapplication.dto.CartItemRequest;
import com.shopapplication.dto.CartItemResponse;
import com.shopapplication.dto.CartResponse;
import com.shopapplication.models.Cart;
import com.shopapplication.models.CartItem;
import com.shopapplication.models.Product;
import com.shopapplication.models.User;
import com.shopapplication.repository.CartItemRepository;
import com.shopapplication.repository.CartRepository;
import com.shopapplication.repository.ProductRepository;
import com.shopapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get or create cart for current user
     */
    @Transactional
    public Cart getOrCreateCart(User user) {
        Optional<Cart> existingCart = cartRepository.findByUser(user);
        
        if (existingCart.isPresent()) {
            return existingCart.get();
        }
        
        // Create new cart
        Cart cart = Cart.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        
        return cartRepository.save(cart);
    }

    /**
     * Add item to cart
     */
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        // Validate request
        if (request.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }
        
        // Check if product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));
        
        // Check stock availability
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStock());
        }
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
        
        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            
            // Check stock for new quantity
            if (product.getStock() < newQuantity) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStock());
            }
            
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // Add new item
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            
            cartItemRepository.save(cartItem);
        }
        
        return getCart();
    }

    /**
     * Update cart item quantity
     */
    @Transactional
    public CartResponse updateCartItem(Long productId, Integer quantity) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        // Validate
        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        if (quantity == null || quantity < 0) {
            throw new RuntimeException("Quantity must be 0 or greater");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        if (quantity == 0) {
            // Remove item if quantity is 0
            cartItemRepository.delete(cartItem);
        } else {
            // Check stock
            if (product.getStock() < quantity) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStock());
            }
            
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }
        
        return getCart();
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public CartResponse removeFromCart(Long productId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        cartItemRepository.delete(cartItem);
        
        return getCart();
    }

    /**
     * Get current user's cart
     */
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        
        List<CartItemResponse> items = cartItems.stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());
        
        double totalAmount = items.stream()
                .mapToDouble(CartItemResponse::getSubtotal)
                .sum();
        
        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();
        
        return CartResponse.builder()
                .id(cart.getId())
                .userId(user.getId())
                .items(items)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .createdAt(cart.getCreatedAt())
                .build();
    }

    /**
     * Clear cart
     */
    @Transactional
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is already empty");
        }
        
        cartItemRepository.deleteAll(cartItems);
    }

    /**
     * Get cart item count
     */
    public int getCartItemCount() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private CartItemResponse convertToCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        double subtotal = product.getPrice() * cartItem.getQuantity();
        
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImageUrl())
                .unitPrice(product.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
