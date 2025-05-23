package com.tbhatta.matchingengine.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tbhatta.matchingengine.model.OrderItemModel;
import com.tbhatta.protos.me.OrderItemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @Autowired private OrderBook orderBook;

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(topics = "orderitem", groupId = "matching-engine")
    public void receiveOrderItemEvent(byte[] kafkaMessage) {
        try {
            OrderItemEvent orderItemEvent = OrderItemEvent.parseFrom(kafkaMessage);
            log.info("MatchingEngineService got OrderItem\n\t" +
                    "OrderItem.OrderId -> " + orderItemEvent.getOrderId() +
                    " OrderItem.OrderAmount -> " + orderItemEvent.getAmount() +
                    " OrderItem.OrderType -> " + orderItemEvent.getOrderType()
            );
            OrderItemModel orderItemModel = new OrderItemModel(orderItemEvent);
            log.info("MatchingEngineService made OrderItemModel\n\t" +
                    orderItemModel.toString()
                    );
            orderBook.enterOrderItem(orderItemModel);
            orderBook.printAskPQ();
            orderBook.printBidPQ();

        } catch (InvalidProtocolBufferException e) {
            log.error("Protocol related Exception in Matchingengine service, receiveOrderItemEven with error message :" + e.getMessage());
        } catch (Exception e) {
            log.error("Exception in Matchingengine service, receiveOrderItemEven with error message :" + e.getMessage());
        }



    }
}
