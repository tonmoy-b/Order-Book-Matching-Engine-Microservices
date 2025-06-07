package com.tbhatta.matchingengine.service;

import com.tbhatta.matchingengine.model.comparator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.TreeMap;

import com.tbhatta.matchingengine.model.OrderItemModel;

@Service
public class OrderBook {
    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);
    //public PriorityQueue<OrderItemModel> askPQ, bidPQ;
    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;
    public HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetHashMap;

    public OrderBook() {
        //askPQ = new PriorityQueue<>(new AskComparatorPrice());
        //bidPQ = new PriorityQueue<>(new BidComparatorPrice());
        bidTreeMap = new TreeMap<>(new BidComparatorPriceBigDecimal());
        askTreeMap = new TreeMap<>(new AskComparatorPriceBigDecimal());
    }


    public void enterOrderItem(OrderItemModel orderItemModel) {
        BigDecimal price = orderItemModel.getAmount();
        if (orderItemModel.getOrderType().toLowerCase().strip().equals("bid")) {
            if (bidTreeMap.containsKey(price)) {
                PriorityQueue<OrderItemModel> pQ = bidTreeMap.get(price);
                pQ.add(orderItemModel);
            } else {
                PriorityQueue<OrderItemModel> bidPriorityQueue = new PriorityQueue<OrderItemModel>(new BidComparatorOrderTime());
                bidPriorityQueue.add(orderItemModel);
                bidTreeMap.put(price, bidPriorityQueue);
            }
        } else if (orderItemModel.getOrderType().toLowerCase().strip().equals("ask")){
            if (askTreeMap.containsKey(price)) {
                PriorityQueue<OrderItemModel> pQ = askTreeMap.get(price);
                pQ.add(orderItemModel);
            } else {
                PriorityQueue<OrderItemModel> askPriorityQueue = new PriorityQueue<OrderItemModel>(new AskComparatorOrderTime());
                askPriorityQueue.add(orderItemModel);
                askTreeMap.put(price, askPriorityQueue);
            }
        } else {
            log.error("Invalid Order Item OrderType of {}", orderItemModel.getOrderType());
        }
    }

    @Scheduled(fixedRate = 3000)
    public void runMatchingEngine() {
        try {
            getHighBid();
            log.info("ME: Scheduled runMatchingEngine Executed");
        } catch (Exception e) {
            log.error("ME: Scheduled runMatchingEngine Exception {}", e.getMessage());
        }

    }

    private void getHighBid() {
        BigDecimal highestBid = bidTreeMap.firstKey();
        BigDecimal lowestAsk = askTreeMap.firstKey();
        log.info("bid {}", bidTreeMap.get(highestBid).toString());
        log.info("ask {}", askTreeMap.get(lowestAsk).toString());
        log.info("The highest bid is {} and the lowest ask is {}.", highestBid, lowestAsk);
    }

    public String printAskTreeMap() {
        String s = askTreeMap.toString();
        log.info("AskTreeMap is \n\t {}", s);
        return s;
    }

    public String printBidTreeMap() {
        String s = bidTreeMap.toString();
        log.info("BidTreeMap is \n\t {}", s);
        return s;
    }

    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getBidTreeMap() {
        return bidTreeMap;
    }

    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getAskTreeMap() {
        return askTreeMap;
    }

    public String printBidTreePQueue(BigDecimal price) {
        if (bidTreeMap.containsKey(price)) {
            return bidTreeMap.get(price).toString();
        } else {
            return "PriorityQueue for this Price does not exist";
        }
    }

    public String printAskTreePQueue(BigDecimal price) {
        if (askTreeMap.containsKey(price)) {
            return askTreeMap.get(price).toString();
        } else {
            return "PriorityQueue for this Price does not exist";
        }
    }
}
