package com.shopapplication.service;

import com.shopapplication.dto.CreateOrderRequest;
import com.shopapplication.dto.OrderItemResponse;
import com.shopapplication.dto.OrderResponse;
import com.shopapplication.dto.UpdateOrderStatusRequest;
import com.shopapplication.models.*;
import com.shopapplication.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final NotificationService notificationService;

    /**
     * Create order from cart or direct items
     * - If items provided in request, use them directly (for external API products)
     * - Otherwise, use cart items (for database products)
     * - Validates items are not empty
     * - Checks stock availability for database products only
     * - Creates order and order items
     * - Reduces product stock for database products
     * - Clears cart if used
     * - Notifies admins about new order
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = getCurrentUser();
        
        // Validate shipping address
        if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
            throw new RuntimeException("Shipping address is required");
        }
        
        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;
        boolean useCart = false;
        
        // Check if items are provided directly in the request
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Create order from provided items (supports external API products)
            for (CreateOrderRequest.OrderItemDto itemDto : request.getItems()) {
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElse(null);
                
                // For database products, validate stock
                if (product != null) {
                    if (product.getStock() < itemDto.getQuantity()) {
                        throw new RuntimeException(String.format(
                            "Insufficient stock for product '%s'. Available: %d, Requested: %d",
                            product.getName(), product.getStock(), itemDto.getQuantity()
                        ));
                    }
                }
                
                // Use provided price (important for external API products)
                double unitPrice = itemDto.getPrice() != null ? itemDto.getPrice() : 
                                  (product != null ? product.getPrice() : 0.0);
                double subtotal = unitPrice * itemDto.getQuantity();
                totalAmount += subtotal;
                
                // Create a placeholder order item (will set order later)
                OrderItem orderItem = OrderItem.builder()
                        .product(product) // May be null for external products
                        .quantity(itemDto.getQuantity())
                        .unitPrice(unitPrice)
                        .subtotal(subtotal)
                        .build();
                
                orderItems.add(orderItem);
            }
        } else {
            // Use cart items (original behavior)
            useCart = true;
            Cart cart = cartRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Cart not found"));
            
            List<CartItem> cartItems = cartItemRepository.findByCart(cart);
            
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Cart is empty. Add items before creating an order");
            }
            
            // Validate stock availability for all items
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                if (product.getStock() < cartItem.getQuantity()) {
                    throw new RuntimeException(String.format(
                        "Insufficient stock for product '%s'. Available: %d, Requested: %d",
                        product.getName(), product.getStock(), cartItem.getQuantity()
                    ));
                }
            }
            
            // Calculate total amount and create order items
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                double subtotal = product.getPrice() * cartItem.getQuantity();
                totalAmount += subtotal;
                
                OrderItem orderItem = OrderItem.builder()
                        .product(product)
                        .quantity(cartItem.getQuantity())
                        .unitPrice(product.getPrice())
                        .subtotal(subtotal)
                        .build();
                
                orderItems.add(orderItem);
            }
        }
        
        if (orderItems.isEmpty()) {
            throw new RuntimeException("No items to order");
        }
        
        // Create order
        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .shippingAddress(request.getShippingAddress())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // Set order reference and reduce stock for database products
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(savedOrder);
            
            // Reduce stock only for database products
            if (orderItem.getProduct() != null) {
                Product product = orderItem.getProduct();
                product.setStock(product.getStock() - orderItem.getQuantity());
                productRepository.save(product);
            }
        }
        
        savedOrder.setOrderItems(orderItems);
        orderRepository.save(savedOrder);
        
        // Clear cart if it was used
        if (useCart) {
            Cart cart = cartRepository.findByUser(user).orElse(null);
            if (cart != null) {
                List<CartItem> cartItems = cartItemRepository.findByCart(cart);
                cartItemRepository.deleteAll(cartItems);
            }
        }
        
        // Notify user
        notificationService.createNotification(
            user,
            "Order Created Successfully",
            String.format("Your order #%d has been created successfully. Total: $%.2f", 
                savedOrder.getId(), totalAmount)
        );
        
        // Notify admins about new order
        notificationService.notifyAdminsAboutNewOrder(savedOrder.getId(), user.getEmail(), totalAmount);
        
        return convertToOrderResponse(savedOrder);
    }

    /**
     * Get all orders for current user
     */
    public List<OrderResponse> getUserOrders() {
        User user = getCurrentUser();
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get order by ID (user can only access their own orders)
     */
    public OrderResponse getOrderById(Long orderId) {
        User user = getCurrentUser();
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Ensure order belongs to current user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to access this order");
        }
        
        return convertToOrderResponse(order);
    }

    /**
     * Cancel order (only if status is CREATED or PENDING_PAYMENT)
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Ensure order belongs to current user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to cancel this order");
        }
        
        // Check if order can be cancelled
        if (order.getStatus() == OrderStatus.SHIPPED || 
            order.getStatus() == OrderStatus.DELIVERED || 
            order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }
        
        // Restore product stock
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.setStock(product.getStock() + orderItem.getQuantity());
            productRepository.save(product);
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        
        // Notify user
        notificationService.createNotification(
            user,
            "Order Cancelled",
            String.format("Your order #%d has been cancelled successfully", orderId)
        );
        
        return convertToOrderResponse(updatedOrder);
    }

    /**
     * Get all orders (Admin only)
     */
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update order status (Admin only)
     * Sends notification to user about status change
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Validate status
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + request.getStatus());
        }
        
        // Update order status
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        
        // Notify user about status change
        notificationService.notifyUserAboutOrderStatus(
            order.getUser(),
            orderId,
            newStatus.name(),
            request.getMessage()
        );
        
        return convertToOrderResponse(updatedOrder);
    }

    /**
     * Get orders by status (Admin only)
     */
    public List<OrderResponse> getOrdersByStatus(String status) {
        OrderStatus orderStatus;
        try {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
        
        List<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus);
        return orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private OrderResponse convertToOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList());
        
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse convertToOrderItemResponse(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : "External Product")
                .productImage(product != null ? product.getImageUrl() : null)
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .subtotal(orderItem.getSubtotal())
                .build();
    }
}
