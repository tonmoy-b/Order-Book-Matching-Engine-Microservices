package com.tbhatta.orderfront.service;

import com.tbhatta.orderfront.dto.OrderItemDTO;
import com.tbhatta.orderfront.entity.OrderItem;
import com.tbhatta.orderfront.mapper.OrderItemMapper;
import com.tbhatta.orderfront.repository.OrderItemRedisRepo;
import com.tbhatta.orderfront.repository.OrderItemRepo;
import org.hibernate.query.Order;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemService {
    private OrderItemRepo orderItemRepo;
    private OrderItemRedisRepo orderItemRedisRepo;

    public OrderItemService(OrderItemRepo orderItemRepo) {
        this.orderItemRepo = orderItemRepo;
    }

//    public OrderItemService(OrderItemRedisRepo orderItemRedisRepo) {
//        this.orderItemRedisRepo = orderItemRedisRepo;
//    }

    public List<OrderItemDTO> getAllOrders() {
        List<OrderItem> allOrders = orderItemRepo.findAll();
        List<OrderItemDTO> allOrdersDTO = allOrders.stream()
                .map(OrderItemMapper::orderItemToDTO)
                .toList();
        return allOrdersDTO;
    }

    public OrderItemDTO createOrderItem(OrderItemDTO orderItemDTO) {
        OrderItem orderItem =  orderItemRepo.save(OrderItemMapper.toModel(orderItemDTO));
        return OrderItemMapper.orderItemToDTO(orderItem);
    }
}
