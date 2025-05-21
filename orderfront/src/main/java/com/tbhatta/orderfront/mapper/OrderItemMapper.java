package com.tbhatta.orderfront.mapper;

import com.tbhatta.orderfront.dto.OrderItemDTO;
import com.tbhatta.orderfront.entity.OrderItem;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class OrderItemMapper {

    private static final String dtPattern = "yyyy-MM-dd HH:mm:ss";

    public static OrderItemDTO orderItemToDTO(OrderItem orderItem) {
        OrderItemDTO orderItemDTO = new OrderItemDTO();
        orderItemDTO.setClientId(orderItem.getClientId());
        orderItemDTO.setOrderId(orderItem.getOrderId().toString());
        orderItemDTO.setAsset(orderItem.getAsset());
        orderItemDTO.setOrderTime(orderItem.getOrderTime().format(DateTimeFormatter.ofPattern(dtPattern)));
        orderItemDTO.setOrderType(orderItem.getOrderType());
        orderItemDTO.setAmount(orderItem.getAmount().toString().strip());
        orderItemDTO.setVolume(orderItem.getVolume().toString().strip());
        return orderItemDTO;
    }

    public static OrderItem toModel(OrderItemDTO orderItemDTO) {
        OrderItem orderItem = new OrderItem();
        orderItem.setClientId(orderItemDTO.getClientId());
        orderItem.setAsset(orderItemDTO.getAsset());
        orderItem.setOrderTime(LocalDateTime.now());
        orderItem.setOrderType(orderItemDTO.getOrderType());
        orderItem.setAmount(new BigDecimal(orderItemDTO.getAmount()));
        orderItem.setVolume(new BigInteger(orderItemDTO.getVolume()));
        return orderItem;
    }
}
