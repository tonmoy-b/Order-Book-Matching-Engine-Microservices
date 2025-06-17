package com.tbhatta.matchingengine.controller;

import com.tbhatta.matchingengine.model.OrderItemModel;
import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.order_records.service.OrderRecordService;
import com.tbhatta.matchingengine.order_records.service.TransactionRecordService;
import com.tbhatta.matchingengine.service.KafkaConsumer;
import com.tbhatta.matchingengine.service.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

@RestController
@CrossOrigin
public class MatchingEngineController {
    private static final Logger log = LoggerFactory.getLogger(MatchingEngineController.class);
    private KafkaConsumer kafkaConsumer;
    private OrderBook orderBook;
    private OrderRecordService orderRecordService;
    private TransactionRecordService transactionRecordService;

//    public MatchingEngineController(KafkaConsumer kafkaConsumer, OrderBook orderBook, OrderRecordService orderRecordService) {
//        this.kafkaConsumer = kafkaConsumer;
//        this.orderBook = orderBook;
//        this.orderRecordService = orderRecordService;
//    }

    public MatchingEngineController(KafkaConsumer kafkaConsumer, OrderBook orderBook, OrderRecordService orderRecordService, TransactionRecordService transactionRecordService) {
        this.kafkaConsumer = kafkaConsumer;
        this.orderBook = orderBook;
        this.orderRecordService = orderRecordService;
        this.transactionRecordService = transactionRecordService;
    }

    @GetMapping("/hello")
    public String sayHello() {
        //kafkaConsumer.receiveOrderItemEvent();
        return "Hello, World!";
    }

//    @GetMapping("/bids")
//    public String getBids() {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            objectMapper.registerModule(new JavaTimeModule());
//            return objectMapper.writeValueAsString(orderBook.bidPQ);
//        } catch (Exception e) {
//            log.error("Error in getting ME-Bids : "+e.getMessage());
//            return "Error in getting Bids";
//        }
//    }

//    @GetMapping("/asks")
//    public String getAsks() {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            objectMapper.registerModule(new JavaTimeModule());
//            return objectMapper.writeValueAsString(orderBook.askPQ);
//        } catch (Exception e) {
//            log.error("Error in getting ME-Asks : "+e.getMessage());
//            return "Error in getting Asks";
//        }
//    }

    @GetMapping("/ask-treemap")
    public String getAskTreeMap() {
        //return orderBook.printAskTreeMap();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(orderBook.getAskTreeMap());
        } catch (Exception e) {
            return "Error is gathering info";
        }
    }

    @GetMapping("/bid-treemap")
    public String getBidTreeMap() {
        //return orderBook.printBidTreeMap();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(orderBook.getBidTreeMap());
        } catch (Exception e) {
            return "Error is gathering info";
        }
    }

    @GetMapping("/bid-treemap/{asset}")
    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getBidTreeMapByAsset(@PathVariable("asset") String asset) {
        return orderBook.getBidTreeByAsset(asset);
    }

    @GetMapping("/ask-treemap/{asset}")
    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getAskTreeMapByAsset(@PathVariable("asset") String asset) {
        return orderBook.getAskTreeByAsset(asset);
    }

    @GetMapping("/mong/{name}/{detail}")
    public String saveMong(@PathVariable("name") String name, @PathVariable("detail") String detail) {
        orderRecordService.saveP(name, detail);
        return "done";

    }

    @GetMapping("/tranmong/{name}/{detail}")
    public String saveTran(@PathVariable("name") String name, @PathVariable("detail") String detail) {
        transactionRecordService.setTransaction_(name, detail);
        return "done";
    }

    //getRecordsByClientID
    @GetMapping("/transactions/clientId/{clientID}")
    public List<TransactionItemModel> getTransactionsByClient(@PathVariable("clientID") String clientID) {
        return transactionRecordService.getRecordsByClientID(clientID);
    }

    //getRecordsByClientID
    @GetMapping("/transactions/transactionId/{transactionId}")
    public List<TransactionItemModel> getTransactionsByTransactionId(@PathVariable("transactionId") String transactionId) {
        return transactionRecordService.getRecordsByTransactionID(transactionId);
    }

    @GetMapping("/transactions/mainClientOrderId/{mainClientOrderId}")
    public List<TransactionItemModel> getTransactionsByMainclientOrderID(@PathVariable("mainClientOrderId") String mainClientOrderId) {
        return transactionRecordService.getRecordsByMainclientOrderID(mainClientOrderId);
    }

}
