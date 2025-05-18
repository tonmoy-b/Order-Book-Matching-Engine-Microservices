package com.tbhatta.orderfront.dto;

import jakarta.validation.constraints.NotBlank;

public class OrderItemResponseDTO {
    @NotBlank private String orderId;
    @NotBlank private String clientId;
    @NotBlank private String asset;
    private String orderTime;
    @NotBlank private String orderType;
    @NotBlank private String amount;
    @NotBlank private String volume;

    public OrderItemResponseDTO() {
    }
}
