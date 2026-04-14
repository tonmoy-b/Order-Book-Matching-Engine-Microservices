package com.tbhatta.orderfront.kafka;


import static org.junit.jupiter.api.Assertions.*;

import com.tbhatta.orderfront.entity.OrderItem;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {
    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private KafkaProducer kafkaProducer;
    private MeterRegistry meterRegistry;
    private OrderItem sampleOrder;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        kafkaProducer = new KafkaProducer(kafkaTemplate, meterRegistry);
        sampleOrder = new OrderItem(
                UUID.randomUUID(), "CLIENT-001", "AAPL",
                LocalDateTime.of(2025, 5, 20, 10, 30, 0),
                "BUY", new BigDecimal("1500.50"), new BigInteger("100")
        );
    }

    @Test
    void send_publishesToCorrectTopicWithProtobufPayload() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("orderitem", 0), 0, 0, 0L, 0, 0);
        SendResult<String, byte[]> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        kafkaProducer.sendOrderItemCreatedEvent(sampleOrder);
        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(eq("orderitem"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isNotEmpty();
    }

    @Test
    void send_success_incrementsSuccessCounter() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("orderitem", 0), 0, 0, 0L, 0, 0);
        SendResult<String, byte[]> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        kafkaProducer.sendOrderItemCreatedEvent(sampleOrder);
        double successCount = meterRegistry.counter(
                "orderfront.kafka.publish", "status", "success").count();
        assertThat(successCount).isEqualTo(1.0);
    }

    @Test
    void send_failure_incrementsErrorCounter() {
        CompletableFuture<SendResult<String, byte[]>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker down"));
        when(kafkaTemplate.send(anyString(), any(byte[].class))).thenReturn(failedFuture);
        kafkaProducer.sendOrderItemCreatedEvent(sampleOrder);
        double errorCount = meterRegistry.counter(
                "orderfront.kafka.publish", "status", "error").count();
        assertThat(errorCount).isEqualTo(1.0);
    }

    @Test
    void send_failure_doesNotPropagateException() {
        CompletableFuture<SendResult<String, byte[]>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker down"));
        when(kafkaTemplate.send(anyString(), any(byte[].class))).thenReturn(failedFuture);
        assertThatCode(() -> kafkaProducer.sendOrderItemCreatedEvent(sampleOrder))
                .doesNotThrowAnyException();
    }


}