package com.tbhatta.orderfront.mapper;

import com.tbhatta.orderfront.dto.OrderItemDTO;
import com.tbhatta.orderfront.entity.OrderItem;

import java.time.format.DateTimeFormatter;

public class OrderItemMapper {

    private static final String dtPattern = "yyyy-MM-dd HH:mm:ss.SSS";

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
}
