package com.shopapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private Long orderId;
    private String paymentMethod; // e.g., "CREDIT_CARD", "DEBIT_CARD", "PAYPAL"
    private String idempotencyKey; // Client-generated unique key to prevent duplicate payments
}
