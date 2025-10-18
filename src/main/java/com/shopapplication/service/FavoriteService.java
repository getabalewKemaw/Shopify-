package com.shopapplication.service;

import com.shopapplication.dto.FavoriteRequest;
import com.shopapplication.dto.FavoriteResponse;
import com.shopapplication.dto.ProductResponse;
import com.shopapplication.models.Favorite;
import com.shopapplication.models.Product;
import com.shopapplication.models.User;
import com.shopapplication.repository.FavoriteRepository;
import com.shopapplication.repository.ProductRepository;
import com.shopapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Add a product to user's favorites
     * Validates: user exists, product exists, not already favorited
     */
    @Transactional
    public FavoriteResponse addToFavorites(FavoriteRequest request) {
        User user = getCurrentUser();
        
        // Validate product ID
        if (request.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        // Check if product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));
        
        // Check if already favorited
        if (favoriteRepository.existsByUserAndProduct(user, product)) {
            throw new RuntimeException("Product is already in your favorites");
        }
        
        // Create favorite
        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
        
        Favorite savedFavorite = favoriteRepository.save(favorite);
        return convertToResponse(savedFavorite);
    }

    /**
     * Remove a product from user's favorites
     * Validates: user exists, favorite exists
     */
    @Transactional
    public void removeFromFavorites(Long productId) {
        User user = getCurrentUser();
        
        // Validate product ID
        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        // Check if product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        // Check if favorite exists
        Favorite favorite = favoriteRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("Product is not in your favorites"));
        
        favoriteRepository.delete(favorite);
    }

    /**
     * Get all favorites for current user
     */
    public List<FavoriteResponse> getUserFavorites() {
        User user = getCurrentUser();
        List<Favorite> favorites = favoriteRepository.findByUser(user);
        return favorites.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all favorites for a specific user (by user ID)
     * Can be used by admins or for public profiles
     */
    public List<FavoriteResponse> getFavoritesByUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("User ID is required");
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if a product is in user's favorites
     */
    public boolean isProductFavorited(Long productId) {
        User user = getCurrentUser();
        
        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        // Check if product exists
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found with id: " + productId);
        }
        
        return favoriteRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    /**
     * Get count of favorites for current user
     */
    public long getFavoritesCount() {
        User user = getCurrentUser();
        return favoriteRepository.countByUserId(user.getId());
    }

    /**
     * Clear all favorites for current user
     */
    @Transactional
    public void clearAllFavorites() {
        User user = getCurrentUser();
        List<Favorite> favorites = favoriteRepository.findByUser(user);
        
        if (favorites.isEmpty()) {
            throw new RuntimeException("You have no favorites to clear");
        }
        
        favoriteRepository.deleteAll(favorites);
    }

    /**
     * Get a specific favorite by ID
     */
    public FavoriteResponse getFavoriteById(Long favoriteId) {
        User user = getCurrentUser();
        
        if (favoriteId == null) {
            throw new RuntimeException("Favorite ID is required");
        }
        
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new RuntimeException("Favorite not found with id: " + favoriteId));
        
        // Ensure the favorite belongs to the current user
        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to access this favorite");
        }
        
        return convertToResponse(favorite);
    }

    /**
     * Toggle favorite status (add if not exists, remove if exists)
     */
    @Transactional
    public FavoriteResponse toggleFavorite(Long productId) {
        User user = getCurrentUser();
        
        if (productId == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        // Check if already favorited
        if (favoriteRepository.existsByUserAndProduct(user, product)) {
            // Remove from favorites
            Favorite favorite = favoriteRepository.findByUserAndProduct(user, product).get();
            favoriteRepository.delete(favorite);
            return null; // Indicates removed
        } else {
            // Add to favorites
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .product(product)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Favorite savedFavorite = favoriteRepository.save(favorite);
            return convertToResponse(savedFavorite);
        }
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private FavoriteResponse convertToResponse(Favorite favorite) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .userId(favorite.getUser().getId())
                .userEmail(favorite.getUser().getEmail())
                .product(convertProductToResponse(favorite.getProduct()))
                .createdAt(favorite.getCreatedAt())
                .build();
    }

    private ProductResponse convertProductToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .rating(product.getRating())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
