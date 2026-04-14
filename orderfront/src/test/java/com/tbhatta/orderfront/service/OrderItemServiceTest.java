package com.tbhatta.orderfront.service;

import static org.junit.jupiter.api.Assertions.*;

import com.tbhatta.orderfront.dto.CreateOrderItemRequest;
import com.tbhatta.orderfront.dto.OrderItemResponse;
import com.tbhatta.orderfront.entity.OrderItem;
import com.tbhatta.orderfront.exception.OrderCreationException;
import com.tbhatta.orderfront.kafka.KafkaProducer;
import com.tbhatta.orderfront.repository.OrderItemRepo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {
    @Mock
    private OrderItemRepo orderItemRepo;

    @Mock
    private KafkaProducer kafkaProducer;

    private OrderItemService orderItemService;
    private MeterRegistry meterRegistry;
    private OrderItem sampleEntity;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderItemService = new OrderItemService(orderItemRepo, kafkaProducer, meterRegistry);

        sampleId = UUID.fromString("d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f90");
        sampleEntity = new OrderItem(
                sampleId, "CLIENT-001", "AAPL",
                LocalDateTime.of(2025, 5, 20, 10, 30, 0),
                "BUY", new BigDecimal("1500.50"), new BigInteger("100")
        );
    }

    @Test
    void getAllOrders_returnsPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderItem> entityPage = new PageImpl<>(List.of(sampleEntity), pageable, 1);
        when(orderItemRepo.findAll(pageable)).thenReturn(entityPage);
        Page<OrderItemResponse> result = orderItemService.getAllOrders(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).orderId()).isEqualTo(sampleId.toString());
        assertThat(result.getContent().get(0).clientId()).isEqualTo("CLIENT-001");
        assertThat(result.getContent().get(0).asset()).isEqualTo("AAPL");
        verify(orderItemRepo).findAll(pageable);
    }

    @Test
    void getAllOrders_emptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(orderItemRepo.findAll(pageable)).thenReturn(Page.empty(pageable));
        Page<OrderItemResponse> result = orderItemService.getAllOrders(pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void createOrderItem_persistsAndPublishesEvent() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-002", "TSLA", "SELL", "2500.00", "50"
        );
        when(orderItemRepo.save(any(OrderItem.class))).thenReturn(sampleEntity);
        OrderItemResponse result = orderItemService.createOrderItem(request);
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(sampleId.toString());
        verify(orderItemRepo).save(any(OrderItem.class));
        verify(kafkaProducer).sendOrderItemCreatedEvent(sampleEntity);
    }

    @Test
    void createOrderItem_incrementsSuccessCounter() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-003", "GOOG", "BUY", "3000.00", "10"
        );
        when(orderItemRepo.save(any(OrderItem.class))).thenReturn(sampleEntity);
        orderItemService.createOrderItem(request);
        double count = meterRegistry.counter("orderfront.orders.created").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void createOrderItem_repoFailure_throwsOrderCreationException() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-004", "AMZN", "BUY", "500.00", "5"
        );
        when(orderItemRepo.save(any(OrderItem.class)))
                .thenThrow(new RuntimeException("DB unavailable"));
        assertThatThrownBy(() -> orderItemService.createOrderItem(request))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("CLIENT-004")
                .hasCauseInstanceOf(RuntimeException.class);
        verify(kafkaProducer, never()).sendOrderItemCreatedEvent(any());
    }

    @Test
    void createOrderItem_repoFailure_incrementsFailedCounter() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-005", "META", "SELL", "200.00", "15"
        );
        when(orderItemRepo.save(any(OrderItem.class)))
                .thenThrow(new RuntimeException("connection refused"));
        try {
            orderItemService.createOrderItem(request);
        } catch (OrderCreationException e) {
            // exp.
        }
        double count = meterRegistry.counter("orderfront.orders.failed").count();
        assertThat(count).isEqualTo(1.0);
    }


}