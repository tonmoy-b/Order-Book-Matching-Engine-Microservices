package com.tbhatta.orderfront.mapper;

import com.tbhatta.orderfront.OrderfrontConstants;
import com.tbhatta.orderfront.dto.CreateOrderItemRequest;
import com.tbhatta.orderfront.dto.OrderItemDTO;
import com.tbhatta.orderfront.dto.OrderItemResponse;
import com.tbhatta.orderfront.entity.OrderItem;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class OrderItemMapper {

    public OrderItemMapper() {
    }

    public static OrderItemResponse toResponse(OrderItem entity) {
        return new OrderItemResponse(
                entity.getOrderId().toString(),
                entity.getClientId(),
                entity.getAsset(),
                entity.getOrderTime().format(OrderfrontConstants.DATETIME_FORMATTER),
                entity.getOrderType(),
                entity.getAmount().stripTrailingZeros().toPlainString(),
                entity.getVolume().toString()
        );
    }

    public static OrderItem toEntity(CreateOrderItemRequest request, LocalDateTime orderTime) {
        return new OrderItem(
                null,   // skip DB gen. ID
                request.clientId(),
                request.asset(),
                orderTime,
                request.orderType().toUpperCase(),
                new BigDecimal(request.amount()),
                new BigInteger(request.volume())
        );
    }


}
