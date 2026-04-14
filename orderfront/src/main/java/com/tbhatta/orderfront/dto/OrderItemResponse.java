package com.tbhatta.orderfront.dto;

// outbound confirmation of OrderItem creation
public record OrderItemResponse (
        String orderId,
        String clientId,
        String asset,
        String orderTime,
        String orderType,
        String amount,
        String volume
) {
}
