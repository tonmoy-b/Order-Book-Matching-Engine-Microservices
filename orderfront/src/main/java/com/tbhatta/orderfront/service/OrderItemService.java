package com.tbhatta.orderfront.service;

import com.tbhatta.orderfront.repository.OrderItemRedisRepo;
import com.tbhatta.orderfront.repository.OrderItemRepo;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService {
    private OrderItemRepo orderItemRepo;
    private OrderItemRedisRepo orderItemRedisRepo;

    public OrderItemService(OrderItemRepo orderItemRepo) {
        this.orderItemRepo = orderItemRepo;
    }

    public OrderItemService(OrderItemRedisRepo orderItemRedisRepo) {
        this.orderItemRedisRepo = orderItemRedisRepo;
    }
}
