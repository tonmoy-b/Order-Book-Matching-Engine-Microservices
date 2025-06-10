package com.tbhatta.matchingengine.service;

import com.tbhatta.matchingengine.model.comparator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.tbhatta.matchingengine.model.OrderItemModel;

@Service
public class OrderBook {
    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);
    public static final String ASK = "ask";
    public static final String BID = "bid";
    //public PriorityQueue<OrderItemModel> askPQ, bidPQ;
    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;
    public HashMap<String, HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>>> assetHashMap;

    public OrderBook() {
        //askPQ = new PriorityQueue<>(new AskComparatorPrice());
        //bidPQ = new PriorityQueue<>(new BidComparatorPrice());
        bidTreeMap = new TreeMap<>(new BidComparatorPriceBigDecimal());
        askTreeMap = new TreeMap<>(new AskComparatorPriceBigDecimal());
        assetHashMap = new HashMap<>();
    }

    public void enterOrderItem_(OrderItemModel orderItemModel) {
        try {
            String orderAsset = orderItemModel.getAsset().strip().toLowerCase();
            //check if ask-bid trees exist for that asset
            //if they do not exist, create hashmap
            if (!assetHashMap.containsKey(orderAsset)) {
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;
                bidTreeMap = new TreeMap<>();
                askTreeMap = new TreeMap<>();
                HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> treeMaps = new HashMap<>();
                treeMaps.put(OrderBook.BID, bidTreeMap);
                treeMaps.put(OrderBook.ASK, askTreeMap);
                assetHashMap.put(orderItemModel.getAsset(), treeMaps);
            }
            HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetTreeMap = assetHashMap.get(orderAsset.toLowerCase().strip());
            //if they exist, check if bid-tree exists in cse of ask order or vice versa
            BigDecimal orderPrice = orderItemModel.getAmount();
            BigInteger ordervolume = orderItemModel.getVolume();
            if (orderItemModel.getOrderType().strip().equalsIgnoreCase(OrderBook.ASK)) {
                //get highest bid price for the asset
                bidTreeMap = assetTreeMap.get(OrderBook.BID);
                Iterator<BigDecimal> keyPricesBidTreeMap = bidTreeMap.keySet().iterator();
                log.info("ME: Iterator of Bid Prices is {}", iteratorToString(keyPricesBidTreeMap));
                while (keyPricesBidTreeMap.hasNext()) {
                    BigDecimal keyPrice = keyPricesBidTreeMap.next();
                    if (keyPrice.compareTo(orderPrice) >= 0)  {
                        try {
                            OrderItemModel bidOrder = bidTreeMap.get(keyPrice).peek();
                            bidOrder = bidTreeMap.get(keyPrice).poll();
                            break;
                        } catch (Exception e) {
                            log.info("ME Enginge Exception in fetching PQ {}", e.toString());
                        }
                    }
                }
            } else if (orderItemModel.getOrderType().strip().equalsIgnoreCase(OrderBook.BID)) {
                //get highest bid price for the asset
                askTreeMap = assetTreeMap.get(OrderBook.ASK);
                Iterator<BigDecimal> keyPricesAskTreeMap = askTreeMap.keySet().iterator();
                log.info("ME: Iterator of Ask Prices is {}", iteratorToString(keyPricesAskTreeMap));


            } else {
                //illegal OrderType - must be bid or ask, but is neither
                throw new Exception("OrderType neither bid nor ask");
            }
            //if they exist, check if for bid-order, the price exceeds lowest-ask in tree and vice versa : spreadCrossed
            //

        } catch (Exception e) {
            log.error("Error in enterOrderItem with {} :\n {} ", e.getMessage(), e.toString());
        }

    }

    private String iteratorToString(Iterator<BigDecimal> iterator) {
        String s = "";
        for (Iterator<BigDecimal> it = iterator; it.hasNext(); ) {
            BigDecimal bigDecimal = it.next();
            s += bigDecimal.toString() + "; ";
        }
        return s + ".";

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
