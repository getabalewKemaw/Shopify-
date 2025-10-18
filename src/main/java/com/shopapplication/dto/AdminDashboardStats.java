package com.shopapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStats {
    // User Statistics
    private Long totalUsers;
    private Long activeUsers; // Users who have placed orders
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;
    
    // Order Statistics
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long ordersToday;
    private Long ordersThisWeek;
    private Long ordersThisMonth;
    
    // Sales Statistics
    private Double totalRevenue;
    private Double revenueToday;
    private Double revenueThisWeek;
    private Double revenueThisMonth;
    private Double averageOrderValue;
    
    // Product Statistics
    private Long totalProducts;
    private Long lowStockProducts; // Products with stock < 10
    private Long outOfStockProducts; // Products with stock = 0
    
    // Payment Statistics
    private Long totalPayments;
    private Long successfulPayments;
    private Long failedPayments;
    private Double successRate; // Percentage
    
    // Review Statistics
    private Long totalReviews;
    private Double averageRating;
    private Long reviewsThisMonth;
    
    // Order Status Distribution
    private Map<String, Long> orderStatusDistribution;
    
    // Recent Activity
    private Long notificationsToday;
    private Long cartItemsTotal;
}
