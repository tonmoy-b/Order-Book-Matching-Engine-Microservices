package com.tbhatta.orderfront.kafka;

import ch.qos.logback.core.encoder.EchoEncoder;
import com.tbhatta.orderfront.OrderfrontConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.tbhatta.orderfront.entity.OrderItem;
import com.tbhatta.protos.of.OrderItemEvent;

import java.time.format.DateTimeFormatter;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final Counter kafkaPublishSuccessCounter;
    private final Counter kafkaPublishFailureCounter;
    private final Timer kafkaPublishTimer;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaPublishSuccessCounter = Counter.builder("orderfront.kafka.publish")
                .tag("status", "success")
                .description("Successful Kafka publishes")
                .register(meterRegistry);
        this.kafkaPublishFailureCounter = Counter.builder("orderfront.kafka.publish")
                .tag("status", "error")
                .description("Failed Kafka publishes")
                .register(meterRegistry);
        this.kafkaPublishTimer = Timer.builder("orderfront.kafka.publish.duration")
                .description("Time taken in publishing to Kafka")
                .register(meterRegistry);
    }

    public void sendOrderItemCreatedEvent(OrderItem orderItem) {
        OrderItemEvent event = OrderItemEvent.newBuilder()
                .setOrderId(orderItem.getOrderId().toString())
                .setClientId(orderItem.getClientId())
                .setAsset(orderItem.getAsset())
                .setOrderTime(orderItem.getOrderTime().format(OrderfrontConstants.DATETIME_FORMATTER))
                .setOrderType(orderItem.getOrderType())
                .setAmount(orderItem.getAmount().toPlainString())
                .setVolume(orderItem.getVolume().toString())
                .build();
        // time the kafka publishing process
        Timer.Sample sample = Timer.start();
        kafkaTemplate.send(OrderfrontConstants.KAFKA_TOPIC_ORDER_ITEM, event.toByteArray())
                .whenComplete((result, e) -> {
                    sample.stop(kafkaPublishTimer);
                    if (e != null) {
                        kafkaPublishFailureCounter.increment();
                        log.error("Failed to publish order event orderId={}, clientId={}: {}",
                                orderItem.getOrderId(), orderItem.getClientId(), e.getMessage(), e);
                    } else {
                        kafkaPublishSuccessCounter.increment();
                        log.info("Published order event orderId={}, clientId={}, topic={}, offset={}",
                                orderItem.getOrderId(), orderItem.getClientId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().offset());
                    }
                });


    }


}
