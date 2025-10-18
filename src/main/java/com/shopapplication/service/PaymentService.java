package com.shopapplication.service;

import com.shopapplication.dto.PaymentRequest;
import com.shopapplication.dto.PaymentResponse;
import com.shopapplication.models.*;
import com.shopapplication.repository.OrderRepository;
import com.shopapplication.repository.PaymentRepository;
import com.shopapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Random random = new Random();

    /**
     * Process payment with idempotency support
     * - Checks if payment already exists with the same idempotency key
     * - Validates order belongs to current user
     * - Simulates payment processing (90% success rate)
     * - Updates order status based on payment result
     * - Sends notifications
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        User user = getCurrentUser();

        // Validate idempotency key
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            throw new RuntimeException("Idempotency key is required");
        }

        // Check if payment already exists with this idempotency key (idempotency check)
        return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(this::convertToPaymentResponse)
                .orElseGet(() -> createNewPayment(request, user));
    }

    /**
     * Create new payment transaction
     */
    private PaymentResponse createNewPayment(PaymentRequest request, User user) {
        // Validate order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + request.getOrderId()));

        // Ensure order belongs to current user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to pay for this order");
        }

        // Check if order is already paid
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED || 
            order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Order is already paid");
        }

        // Check if order is cancelled
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot pay for a cancelled order");
        }

        // Validate payment method
        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new RuntimeException("Payment method is required");
        }

        // Create payment record with PENDING status
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Simulate payment processing (90% success rate)
        boolean paymentSuccessful = simulatePaymentProcessing();

        if (paymentSuccessful) {
            // Update payment status to SUCCEEDED
            savedPayment.setStatus(PaymentStatus.SUCCEEDED);
            paymentRepository.save(savedPayment);

            // Update order status to PAID
            order.setStatus(OrderStatus.PAID);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Notify user about successful payment
            notificationService.createNotification(
                user,
                "Payment Successful",
                String.format("Payment of $%.2f for order #%d was successful. Your order will be shipped soon.",
                    savedPayment.getAmount(), order.getId())
            );
        } else {
            // Update payment status to FAILED
            savedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(savedPayment);

            // Notify user about failed payment
            notificationService.createNotification(
                user,
                "Payment Failed",
                String.format("Payment of $%.2f for order #%d failed. Please try again.",
                    savedPayment.getAmount(), order.getId())
            );
        }

        return convertToPaymentResponse(savedPayment);
    }

    /**
     * Simulate payment processing
     * Returns true for success (90% probability), false for failure (10% probability)
     */
    private boolean simulatePaymentProcessing() {
        // Simulate processing delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 90% success rate
        return random.nextInt(100) < 90;
    }

    /**
     * Get payment by order ID
     */
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        User user = getCurrentUser();

        // Validate order belongs to user
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to access this payment");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));

        return convertToPaymentResponse(payment);
    }

    /**
     * Get payment history for current user
     */
    public List<PaymentResponse> getPaymentHistory() {
        User user = getCurrentUser();

        // Get all orders for user
        List<Order> userOrders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        List<Long> orderIds = userOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());

        // Get all payments for user's orders
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(payment -> orderIds.contains(payment.getOrder().getId()))
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());

        return payments.stream()
                .map(this::convertToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments (Admin only)
     */
    public List<PaymentResponse> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        payments.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
        
        return payments.stream()
                .map(this::convertToPaymentResponse)
                .collect(Collectors.toList());
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private PaymentResponse convertToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod())
                .idempotencyKey(payment.getIdempotencyKey())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
