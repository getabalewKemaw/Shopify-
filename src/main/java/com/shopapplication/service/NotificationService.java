package com.shopapplication.service;

import com.shopapplication.dto.NotificationResponse;
import com.shopapplication.models.Admin;
import com.shopapplication.models.Notification;
import com.shopapplication.models.User;
import com.shopapplication.repository.AdminRepository;
import com.shopapplication.repository.NotificationRepository;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    /**
     * Create notification for a specific user
     */
    @Transactional
    public NotificationResponse createNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Notification saved = notificationRepository.save(notification);
        return convertToResponse(saved);
    }

    /**
     * Create notification for a user by email
     */
    @Transactional
    public NotificationResponse createNotificationByEmail(String userEmail, String title, String message) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));
        
        return createNotification(user, title, message);
    }

    /**
     * Notify all admins about new order
     */
    @Transactional
    public void notifyAdminsAboutNewOrder(Long orderId, String userEmail, Double totalAmount) {
        List<Admin> admins = adminRepository.findAll();
        
        String title = "New Order Received";
        String message = String.format("New order #%d from %s. Total: $%.2f", 
                orderId, userEmail, totalAmount);
        
        for (Admin admin : admins) {
            // Create notification for each admin (if they have user accounts)
            // In a real system, you might have a separate admin notification table
            // For now, we'll skip this or you can extend Admin to have notifications
        }
    }

    /**
     * Notify user about order status change
     */
    @Transactional
    public void notifyUserAboutOrderStatus(User user, Long orderId, String status, String customMessage) {
        String title = "Order Status Updated";
        String message = customMessage != null && !customMessage.isEmpty() 
                ? customMessage 
                : String.format("Your order #%d status has been updated to: %s", orderId, status);
        
        createNotification(user, title, message);
    }

    /**
     * Get all notifications for current user
     */
    public List<NotificationResponse> getUserNotifications() {
        User user = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        return notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for current user
     */
    public List<NotificationResponse> getUnreadNotifications() {
        User user = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        return notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        User user = getCurrentUser();
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        // Ensure notification belongs to current user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to access this notification");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for current user
     */
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        User user = getCurrentUser();
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        // Ensure notification belongs to current user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to delete this notification");
        }
        
        notificationRepository.delete(notification);
    }

    /**
     * Get notification count for current user
     */
    public long getUnreadCount() {
        User user = getCurrentUser();
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
