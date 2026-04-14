package com.tbhatta.orderfront.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

//inbound OrderItem creation request
public record CreateOrderItemRequest(

        @NotBlank(message = "clientId is required")
        String clientId,

        @NotBlank(message = "asset is required")
        String asset,

        @NotBlank(message = "orderType is required")
        @Pattern(regexp = "^(bid|ask|BID|ASK|BUY|SELL|buy|sell)$",
                message = "orderType must be one of: bid, ask, buy, sell")
        String orderType,

        @NotBlank(message = "amount is required")
        @Pattern(regexp = "^\\d+(\\.\\d+)?$",
                message = "amount must be a valid positive number")
        String amount,

        @NotBlank(message = "volume is required")
        @Pattern(regexp = "^\\d+$",
                message = "volume must be a valid positive integer")
        String volume

) {
}
