package com.tbhatta.matchingengine.service;

import com.tbhatta.matchingengine.entity.OrderItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.TreeMap;

import com.tbhatta.matchingengine.model.comparator.AskComparator;
import com.tbhatta.matchingengine.model.comparator.BidComparator;
import com.tbhatta.matchingengine.model.OrderItemModel;

@Service
public class OrderBook {
    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);
    public PriorityQueue<OrderItemModel> askPQ, bidPQ;

    public OrderBook() {
        askPQ = new PriorityQueue<>(new AskComparator());
        bidPQ = new PriorityQueue<>(new BidComparator());
    }

    public void enterOrderItem(OrderItemModel orderItemModel) {
        if (orderItemModel.getOrderType().toLowerCase().strip().equals("bid")) {
            bidPQ.add(orderItemModel);
        } else if (orderItemModel.getOrderType().toLowerCase().strip().equals("ask")){
            askPQ.add(orderItemModel);
        } else {
            log.error("Invalid Order Item OrderType of {}", orderItemModel.getOrderType());
        }
    }

    public void printAskPQ() {
        String s = askPQ.toString();
        log.info("AskPQ is \n\t {}", s);
    }

    public void printBidPQ() {
        String s = bidPQ.toString();
        log.info("AskPQ is \n\t {}", s);
    }
}
