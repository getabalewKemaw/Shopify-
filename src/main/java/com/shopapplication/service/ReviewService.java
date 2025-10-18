package com.shopapplication.service;

import com.shopapplication.dto.ReviewRequest;
import com.shopapplication.dto.ReviewResponse;
import com.shopapplication.models.*;
import com.shopapplication.repository.*;
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
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /**
     * Create a review for a purchased product
     * - Validates user has purchased the product
     * - Ensures user hasn't already reviewed this product
     * - Validates rating is between 1-5
     */
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User user = getCurrentUser();

        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

        // Check if user has already reviewed this product
        if (reviewRepository.findByUserIdAndProductId(user.getId(), request.getProductId()).isPresent()) {
            throw new RuntimeException("You have already reviewed this product");
        }

        // Validate user has purchased this product
        if (!hasUserPurchasedProduct(user.getId(), request.getProductId())) {
            throw new RuntimeException("You can only review products you have purchased");
        }

        // Validate rating
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        // Create review
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        return convertToReviewResponse(savedReview);
    }

    /**
     * Update an existing review
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        User user = getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        // Ensure review belongs to current user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to update this review");
        }

        // Validate rating
        if (request.getRating() != null) {
            if (request.getRating() < 1 || request.getRating() > 5) {
                throw new RuntimeException("Rating must be between 1 and 5");
            }
            review.setRating(request.getRating());
        }

        // Update comment
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        Review updatedReview = reviewRepository.save(review);

        return convertToReviewResponse(updatedReview);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        User user = getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        // Ensure review belongs to current user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to delete this review");
        }

        reviewRepository.delete(review);
    }

    /**
     * Get all reviews for a product
     */
    public List<ReviewResponse> getProductReviews(Long productId) {
        // Validate product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        List<Review> reviews = reviewRepository.findByProductId(productId);
        
        return reviews.stream()
                .map(this::convertToReviewResponse)
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Get user's own reviews
     */
    public List<ReviewResponse> getUserReviews() {
        User user = getCurrentUser();

        List<Review> reviews = reviewRepository.findAll().stream()
                .filter(review -> review.getUser().getId().equals(user.getId()))
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .collect(Collectors.toList());

        return reviews.stream()
                .map(this::convertToReviewResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get review by ID
     */
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        return convertToReviewResponse(review);
    }

    /**
     * Get average rating for a product
     */
    public Double getProductAverageRating(Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        
        if (reviews.isEmpty()) {
            return 0.0;
        }

        double sum = reviews.stream()
                .mapToInt(Review::getRating)
                .sum();

        return sum / reviews.size();
    }

    // Helper methods

    /**
     * Check if user has purchased a product
     */
    private boolean hasUserPurchasedProduct(Long userId, Long productId) {
        List<Order> userOrders = orderRepository.findByUserOrderByCreatedAtDesc(
                userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );

        // Check if any order contains the product and is paid/shipped/delivered
        for (Order order : userOrders) {
            if (order.getStatus() == OrderStatus.PAID || 
                order.getStatus() == OrderStatus.SHIPPED || 
                order.getStatus() == OrderStatus.DELIVERED) {
                
                boolean hasProduct = order.getOrderItems().stream()
                        .anyMatch(item -> item.getProduct().getId().equals(productId));
                
                if (hasProduct) {
                    return true;
                }
            }
        }

        return false;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ReviewResponse convertToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getUsername())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
