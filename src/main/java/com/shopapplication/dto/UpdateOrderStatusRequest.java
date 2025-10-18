package com.shopapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    private String status; // PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED
    private String message; // Optional message to send to user
}
