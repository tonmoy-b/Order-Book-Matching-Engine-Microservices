package com.tbhatta.matchingengine.controller;

import com.tbhatta.matchingengine.service.KafkaConsumer;
import com.tbhatta.matchingengine.service.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

@RestController
//@RequestMapping("/check")
public class MatchingEngineController {
    private static final Logger log = LoggerFactory.getLogger(MatchingEngineController.class);
    private KafkaConsumer kafkaConsumer;
    private OrderBook orderBook;

    public MatchingEngineController(KafkaConsumer kafkaConsumer, OrderBook orderBook) {
        this.kafkaConsumer = kafkaConsumer;
        this.orderBook = orderBook;
    }

    @GetMapping("/hello")
    public String sayHello() {
        //kafkaConsumer.receiveOrderItemEvent();
        return "Hello, World!";
    }

    @GetMapping("/bids")
    public String getBids() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(orderBook.bidPQ);
        } catch (Exception e) {
            log.error("Error in getting ME-Bids : "+e.getMessage());
            return "Error in getting Bids";
        }
    }

    @GetMapping("/asks")
    public String getAsks() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(orderBook.askPQ);
        } catch (Exception e) {
            log.error("Error in getting ME-Asks : "+e.getMessage());
            return "Error in getting Asks";
        }
    }
}
