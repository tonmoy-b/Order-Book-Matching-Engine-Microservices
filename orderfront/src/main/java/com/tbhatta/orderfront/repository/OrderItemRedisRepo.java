package com.tbhatta.orderfront.repository;

import com.tbhatta.orderfront.entity.OrderItem;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OrderItemRedisRepo extends CrudRepository<OrderItem, UUID> {
}
