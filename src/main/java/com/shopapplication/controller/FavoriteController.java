package com.shopapplication.controller;

import com.shopapplication.dto.FavoriteRequest;
import com.shopapplication.dto.FavoriteResponse;
import com.shopapplication.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * Add a product to favorites
     * POST /api/favorites
     */
    @PostMapping
    public ResponseEntity<?> addToFavorites(@RequestBody FavoriteRequest request) {
        try {
            FavoriteResponse favorite = favoriteService.addToFavorites(request);
            return ResponseEntity.ok().body(Map.of(
                "message", "Product added to favorites successfully",
                "favorite", favorite
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove a product from favorites
     * DELETE /api/favorites/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromFavorites(@PathVariable Long productId) {
        try {
            favoriteService.removeFromFavorites(productId);
            return ResponseEntity.ok().body(Map.of(
                "message", "Product removed from favorites successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all favorites for current user
     * GET /api/favorites
     */
    @GetMapping
    public ResponseEntity<?> getUserFavorites() {
        try {
            List<FavoriteResponse> favorites = favoriteService.getUserFavorites();
            return ResponseEntity.ok(favorites);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get favorites by user ID
     * GET /api/favorites/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getFavoritesByUserId(@PathVariable Long userId) {
        try {
            List<FavoriteResponse> favorites = favoriteService.getFavoritesByUserId(userId);
            return ResponseEntity.ok(favorites);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if a product is favorited
     * GET /api/favorites/check/{productId}
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<?> isProductFavorited(@PathVariable Long productId) {
        try {
            boolean isFavorited = favoriteService.isProductFavorited(productId);
            return ResponseEntity.ok().body(Map.of(
                "productId", productId,
                "isFavorited", isFavorited
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get favorites count for current user
     * GET /api/favorites/count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getFavoritesCount() {
        try {
            long count = favoriteService.getFavoritesCount();
            return ResponseEntity.ok().body(Map.of("count", count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Clear all favorites for current user
     * DELETE /api/favorites
     */
    @DeleteMapping
    public ResponseEntity<?> clearAllFavorites() {
        try {
            favoriteService.clearAllFavorites();
            return ResponseEntity.ok().body(Map.of(
                "message", "All favorites cleared successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get a specific favorite by ID
     * GET /api/favorites/detail/{favoriteId}
     */
    @GetMapping("/detail/{favoriteId}")
    public ResponseEntity<?> getFavoriteById(@PathVariable Long favoriteId) {
        try {
            FavoriteResponse favorite = favoriteService.getFavoriteById(favoriteId);
            return ResponseEntity.ok(favorite);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle favorite status (add if not exists, remove if exists)
     * POST /api/favorites/toggle/{productId}
     */
    @PostMapping("/toggle/{productId}")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long productId) {
        try {
            FavoriteResponse favorite = favoriteService.toggleFavorite(productId);
            if (favorite == null) {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Product removed from favorites",
                    "action", "removed"
                ));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Product added to favorites",
                    "action", "added",
                    "favorite", favorite
                ));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
