package com.tbhatta.orderfront.service;

import com.tbhatta.orderfront.dto.CreateOrderItemRequest;
import com.tbhatta.orderfront.dto.OrderItemDTO;
import com.tbhatta.orderfront.dto.OrderItemResponse;
import com.tbhatta.orderfront.entity.OrderItem;
import com.tbhatta.orderfront.exception.OrderCreationException;
import com.tbhatta.orderfront.mapper.OrderItemMapper;
import com.tbhatta.orderfront.repository.OrderItemRedisRepo;
import com.tbhatta.orderfront.repository.OrderItemRepo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.tbhatta.orderfront.kafka.KafkaProducer;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderItemService {
    private OrderItemRepo orderItemRepo;
    private KafkaProducer kafkaProducer;
    private Counter ordersCreatedCounter;
    private Counter ordersFailedCounter;
    private Timer orderCreationTimer;
    private static final Logger log = LoggerFactory.getLogger(OrderItemService.class);

    public OrderItemService(OrderItemRepo orderItemRepo, KafkaProducer kafkaProducer, MeterRegistry meterRegistry) {
        this.orderItemRepo = orderItemRepo;
        this.kafkaProducer = kafkaProducer;
        this.ordersCreatedCounter = Counter.builder("orderfront.orders.created")
                .description("Total orders succesfully created")
                .register(meterRegistry);
        this.ordersFailedCounter = Counter.builder("orderfront.orders.failed")
                .description("Total orders failed while creating")
                .register(meterRegistry);
        this.orderCreationTimer = Timer.builder("orderfront.orders.creation.duration")
                .description("Time taken in creating and persisting orders")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public Page<OrderItemResponse> getAllOrders(Pageable pageable) {
        return orderItemRepo.findAll(pageable).map(OrderItemMapper::toResponse);
    }

    public OrderItemResponse createOrderItem(CreateOrderItemRequest orderRequest) {
        return orderCreationTimer.record(() -> {
            try {
                LocalDateTime localDateTimeNow = LocalDateTime.now();
                OrderItem entity = OrderItemMapper.toEntity(orderRequest, localDateTimeNow);
                OrderItem saved = orderItemRepo.save(entity);
                log.info("Order persisted orderId={}, clientId={}, asset={}, orderType={}",
                        saved.getOrderId(), saved.getClientId(),
                        saved.getAsset(), saved.getOrderType());
                kafkaProducer.sendOrderItemCreatedEvent(saved);
                ordersCreatedCounter.increment();
                return OrderItemMapper.toResponse(saved);
            } catch (Exception e) {
                ordersFailedCounter.increment();
                log.error("Order creation failed for clientId={}, asset={}: {}",
                        orderRequest.clientId(), orderRequest.asset(), e.getMessage(), e);
                throw new OrderCreationException(
                        "Failed to create order for client " + orderRequest.clientId(), e);
            }
        });

    }


}
