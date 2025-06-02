package com.tbhatta.matchingengine.service;

import com.tbhatta.matchingengine.model.comparator.AskComparatorPriceBigDecimal;
import com.tbhatta.matchingengine.model.comparator.BidComparatorPriceBigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.PriorityQueue;
import java.util.TreeMap;

import com.tbhatta.matchingengine.model.comparator.AskComparatorPrice;
import com.tbhatta.matchingengine.model.comparator.BidComparatorPrice;
import com.tbhatta.matchingengine.model.OrderItemModel;

@Service
public class OrderBook {
    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);
    public PriorityQueue<OrderItemModel> askPQ, bidPQ;
    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;

    public OrderBook() {
        askPQ = new PriorityQueue<>(new AskComparatorPrice());
        bidPQ = new PriorityQueue<>(new BidComparatorPrice());
        bidTreeMap = new TreeMap<>(new BidComparatorPriceBigDecimal());
        askTreeMap = new TreeMap<>(new AskComparatorPriceBigDecimal());
    }

    public void enterOrderItem0(OrderItemModel orderItemModel) {
        if (orderItemModel.getOrderType().toLowerCase().strip().equals("bid")) {
            bidPQ.add(orderItemModel);
        } else if (orderItemModel.getOrderType().toLowerCase().strip().equals("ask")){
            askPQ.add(orderItemModel);
        } else {
            log.error("Invalid Order Item OrderType of {}", orderItemModel.getOrderType());
        }
    }

    public void enterOrderItem(OrderItemModel orderItemModel) {
        if (orderItemModel.getOrderType().toLowerCase().strip().equals("bid")) {
            BigDecimal price = orderItemModel.getAmount();
            if (bidTreeMap.containsKey(price)) {
                PriorityQueue<OrderItemModel> pQ = bidTreeMap.get(price);
                pQ.add(orderItemModel);
            } else {
                //bidTreeMap.put(price, new PriorityQueue<OrderItemModel>(new BidComparatorPriceBigDecimal()))
            }

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
