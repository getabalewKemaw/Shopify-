package com.shopapplication.service;

import com.shopapplication.dto.AdminDashboardStats;
import com.shopapplication.dto.AuthRequest;
import com.shopapplication.dto.RegisterRequest;
import com.shopapplication.models.*;
import com.shopapplication.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final CartItemRepository cartItemRepository;

    public String registerAdmin(RegisterRequest request) {
        // Validate request
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        
        // Check if admin already exists
        if (adminRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Admin with this email already exists");
        }
        
        Admin admin = Admin.builder()
                .email(request.getEmail())
                .name(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        
        adminRepository.save(admin);
        return jwtService.generateToken(admin.getEmail());
    }

    public String loginAdmin(AuthRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return jwtService.generateToken(request.getEmail());
    }

    /**
     * Get comprehensive dashboard statistics for admin
     */
    public AdminDashboardStats getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.minusMonths(1);

        // User Statistics
        List<User> allUsers = userRepository.findAll();
        Long totalUsers = (long) allUsers.size();
        Long activeUsers = allUsers.stream()
                .filter(user -> orderRepository.findByUserOrderByCreatedAtDesc(user).size() > 0)
                .count();
        Long newUsersToday = allUsers.stream()
                .filter(user -> user.getCreatedAt().isAfter(startOfToday))
                .count();
        Long newUsersThisWeek = allUsers.stream()
                .filter(user -> user.getCreatedAt().isAfter(startOfWeek))
                .count();
        Long newUsersThisMonth = allUsers.stream()
                .filter(user -> user.getCreatedAt().isAfter(startOfMonth))
                .count();

        // Order Statistics
        List<Order> allOrders = orderRepository.findAll();
        Long totalOrders = (long) allOrders.size();
        Long pendingOrders = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CREATED || 
                               order.getStatus() == OrderStatus.PENDING_PAYMENT)
                .count();
        Long completedOrders = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .count();
        Long cancelledOrders = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                .count();
        Long ordersToday = allOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfToday))
                .count();
        Long ordersThisWeek = allOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfWeek))
                .count();
        Long ordersThisMonth = allOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .count();

        // Sales Statistics
        List<Order> paidOrders = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID || 
                               order.getStatus() == OrderStatus.SHIPPED || 
                               order.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());
        
        Double totalRevenue = paidOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();
        Double revenueToday = paidOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfToday))
                .mapToDouble(Order::getTotalAmount)
                .sum();
        Double revenueThisWeek = paidOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfWeek))
                .mapToDouble(Order::getTotalAmount)
                .sum();
        Double revenueThisMonth = paidOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .mapToDouble(Order::getTotalAmount)
                .sum();
        Double averageOrderValue = paidOrders.isEmpty() ? 0.0 : totalRevenue / paidOrders.size();

        // Product Statistics
        List<Product> allProducts = productRepository.findAll();
        Long totalProducts = (long) allProducts.size();
        Long lowStockProducts = allProducts.stream()
                .filter(product -> product.getStock() > 0 && product.getStock() < 10)
                .count();
        Long outOfStockProducts = allProducts.stream()
                .filter(product -> product.getStock() == 0)
                .count();

        // Payment Statistics
        List<Payment> allPayments = paymentRepository.findAll();
        Long totalPayments = (long) allPayments.size();
        Long successfulPayments = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
                .count();
        Long failedPayments = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.FAILED)
                .count();
        Double successRate = totalPayments > 0 ? (successfulPayments * 100.0 / totalPayments) : 0.0;

        // Review Statistics
        List<Review> allReviews = reviewRepository.findAll();
        Long totalReviews = (long) allReviews.size();
        Double averageRating = allReviews.isEmpty() ? 0.0 : 
                allReviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);
        Long reviewsThisMonth = allReviews.stream()
                .filter(review -> review.getCreatedAt().isAfter(startOfMonth))
                .count();

        // Order Status Distribution
        Map<String, Long> orderStatusDistribution = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = allOrders.stream()
                    .filter(order -> order.getStatus() == status)
                    .count();
            orderStatusDistribution.put(status.name(), count);
        }

        // Recent Activity
        List<Notification> allNotifications = notificationRepository.findAll();
        Long notificationsToday = allNotifications.stream()
                .filter(notification -> notification.getCreatedAt().isAfter(startOfToday))
                .count();
        Long cartItemsTotal = (long) cartItemRepository.findAll().size();

        return AdminDashboardStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .ordersToday(ordersToday)
                .ordersThisWeek(ordersThisWeek)
                .ordersThisMonth(ordersThisMonth)
                .totalRevenue(totalRevenue)
                .revenueToday(revenueToday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .averageOrderValue(averageOrderValue)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .outOfStockProducts(outOfStockProducts)
                .totalPayments(totalPayments)
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .successRate(successRate)
                .totalReviews(totalReviews)
                .averageRating(averageRating)
                .reviewsThisMonth(reviewsThisMonth)
                .orderStatusDistribution(orderStatusDistribution)
                .notificationsToday(notificationsToday)
                .cartItemsTotal(cartItemsTotal)
                .build();
    }
}
