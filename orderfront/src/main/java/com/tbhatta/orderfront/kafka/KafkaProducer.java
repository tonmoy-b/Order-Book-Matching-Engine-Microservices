package com.tbhatta.orderfront.kafka;

import ch.qos.logback.core.encoder.EchoEncoder;
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
    private static final String dtPattern = "yyyy-MM-dd HH:mm:ss";

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendOrderItemCreatedEvent(OrderItem orderItem) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        OrderItemEvent orderItemEvent = OrderItemEvent.newBuilder()
                .setOrderId(orderItem.getOrderId().toString())
                .setClientId(orderItem.getClientId())
                .setAsset(orderItem.getAsset())
                .setOrderTime(orderItem.getOrderTime().format(formatter))
                .setOrderType(orderItem.getOrderType())
                .setAmount(orderItem.getAmount().toString())
                .setVolume(orderItem.getVolume().toString())
                .build();
        try {
            kafkaTemplate.send("orderitem", orderItemEvent.toByteArray());
        } catch (Exception e) {
            log.error("Error in sending OrderItemEvent :"+e.getMessage());
        }
    }



}
