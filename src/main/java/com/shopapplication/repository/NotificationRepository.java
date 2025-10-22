package com.shopapplication.repository;

import com.shopapplication.models.Admin;
import com.shopapplication.models.Notification;
import com.shopapplication.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, Boolean isRead);
    long countByUserAndIsRead(User user, Boolean isRead);
    List<Notification> findByAdminOrderByCreatedAtDesc(Admin admin);
}
