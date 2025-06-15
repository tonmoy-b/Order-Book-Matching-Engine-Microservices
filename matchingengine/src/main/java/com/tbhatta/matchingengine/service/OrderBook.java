package com.tbhatta.matchingengine.service;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.model.comparator.*;
import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.bson.Document;
import org.bson.types.ObjectId;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.math.MathContext;
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
//            TransactionItemModel mainPartyTransaction = new TransactionItemModel();
//            TransactionItemModel counterPartyTransaction = new TransactionItemModel();
//            mainPartyTransaction.setTransactionID(UUID.randomUUID());
//            mainPartyTransaction.setMainClientID(orderItemModel.getClientId());
//            tranItemRepo.insert(mainPartyTransaction);//pendingVolume.compareTo(bidOrder.getVolume()) ==
            //if they do not exist, create hashmap
            if (!assetHashMap.containsKey(orderAsset)) {
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;
                bidTreeMap = new TreeMap<>(new BidComparatorPriceBigDecimal());
                askTreeMap = new TreeMap<>(new AskComparatorPriceBigDecimal());
                HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> treeMaps = new HashMap<>();
                treeMaps.put(OrderBook.BID, bidTreeMap);
                treeMaps.put(OrderBook.ASK, askTreeMap);
                assetHashMap.put(orderItemModel.getAsset(), treeMaps);
            }
            HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetTreeMap = assetHashMap.get(orderAsset.toLowerCase().strip());
            //if they exist, check if bid-tree exists in cse of ask order or vice versa
            BigDecimal orderPrice = orderItemModel.getAmount();
            List<OrderItemModel> transactions = new ArrayList<>();
            if (orderItemModel.getOrderType().strip().equalsIgnoreCase(OrderBook.ASK)) {
                //Order coming in from the Kafka queue is of an Ask-type
                bidTreeMap = assetTreeMap.get(OrderBook.BID);
                Iterator<BigDecimal> keyPricesBidTreeMap = bidTreeMap.keySet().iterator();
                log.info("ME: Iterator of Bid Prices is {}", iteratorToString(keyPricesBidTreeMap));
                BigInteger pendingVolume = orderItemModel.getVolume();//Volume to be sold to asker
                while (keyPricesBidTreeMap.hasNext() && pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    BigDecimal keyPrice = keyPricesBidTreeMap.next();
                    if (keyPrice.compareTo(orderPrice) >= 0)  {
                        //
                        try {
                            //OrderItemModel bidOrder = bidTreeMap.get(keyPrice).peek();
                            //bidOrder = bidTreeMap.get(keyPrice).poll();
                            //break;
                            PriorityQueue<OrderItemModel> priorityQueue = bidTreeMap.get(keyPrice);
                            //iterate through OrderItemModels in the PriorityQueue at this Price point
                            for (int i = 0; i < priorityQueue.size(); i++) {
                                OrderItemModel bidOrder = bidTreeMap.get(keyPrice).poll();
                                if (bidOrder == null) {
                                    continue;
                                }
                                //doTransaction
                                OrderItemModel transaction = new OrderItemModel();
                                transaction.setAsset(orderItemModel.getAsset());
                                transaction.setClientId(orderItemModel.getClientId());
                                if (pendingVolume.compareTo(bidOrder.getVolume()) < 0) {
                                    //orderItem is asking for less than the bid - sale possible
                                    transaction.setVolume(pendingVolume);
                                    pendingVolume = BigInteger.valueOf(0);
                                    bidOrder.setVolume(bidOrder.getVolume().subtract(transaction.getVolume()));
                                    //log.info()
//                                    TransactionItemModel mainPartyTransaction = new TransactionItemModel();
//                                    TransactionItemModel counterPartyTransaction = new TransactionItemModel();
//                                    mainPartyTransaction.setTransactionID(UUID.randomUUID());
//                                    mainPartyTransaction.setMainClientID(bidOrder.getClientId());
//                                    tranItemRepo.insert(mainPartyTransaction);
                                } else if (pendingVolume.compareTo(bidOrder.getVolume()) > 0) {


                                } else {


                                }
                            }
                        } catch (Exception e) {
                            log.info("ME Enginge Exception in fetching PQ {}", e.toString());
                        }
                    } else {

                    }
                } //end of loop through all current BID-TreeMaps in the asset's Hashmap
                if (pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    //theres still some volume that has not been met
                    //so create an ASK for the remaining volume
                    askTreeMap = assetTreeMap.get(OrderBook.ASK);
                    orderItemModel.setVolume(pendingVolume);
                    if (askTreeMap.containsKey(orderItemModel.getAmount())) {
                        PriorityQueue<OrderItemModel> priorityQueue = askTreeMap.get(orderItemModel.getAmount());
                        priorityQueue.add(orderItemModel);
                    } else {
                        PriorityQueue<OrderItemModel> priorityQueue = new PriorityQueue<OrderItemModel>(new AskComparatorOrderTime());
                        priorityQueue.add(orderItemModel);
                        askTreeMap.put(orderItemModel.getAmount(), priorityQueue);
                    }
                }
            } else if (orderItemModel.getOrderType().strip().equalsIgnoreCase(OrderBook.BID)) {
                //Order coming in from the Kafka queue is of an Bid-type
                //iterate through all the Ask-Price keyed TreeMaps in the Asset's HashMap
                askTreeMap = assetTreeMap.get(OrderBook.ASK);
                Iterator<BigDecimal> keyPricesAskTreeMap = askTreeMap.keySet().iterator();//keys are the Ask prices
                log.info("ME: Iterator of Ask Prices is {}", iteratorToString(keyPricesAskTreeMap));
                BigInteger pendingVolume = orderItemModel.getVolume();//volume of sale required in the Bid-Order
                while (keyPricesAskTreeMap.hasNext() && pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    BigDecimal keyPrice = keyPricesAskTreeMap.next();
                    if (keyPrice.compareTo(orderPrice) >= 0) {
                        //The Key (price) of the Asks in this node of the TreeMap is
                        //greater than or equalling the Kafka-originated Order's Bid price
                        //thus a sale is possible
                        try {
                            //get the priority-queue of all ask-orders set as value for the key-price
                            //in the TreeMap, the priority-queue orders are arranged according to time
                            PriorityQueue<OrderItemModel> priorityQueue = askTreeMap.get(keyPrice);
                            for (int i = 0; i < priorityQueue.size(); i++) {
                                OrderItemModel askOrder = askTreeMap.get(keyPrice).peek();//glance without removing
                                if (askOrder == null) {
                                    continue;
                                }
                                //doTransaction
                                OrderItemModel transaction = new OrderItemModel();
                                transaction.setAsset(orderItemModel.getAsset());
                                transaction.setClientId(orderItemModel.getClientId());
                                if (pendingVolume.compareTo(askOrder.getVolume()) <= 0) {
                                    //
                                    transaction.setVolume(pendingVolume);
                                    pendingVolume = BigInteger.valueOf(0);
                                    askOrder.setVolume(askOrder.getVolume().subtract(transaction.getVolume()));
                                    log.info("Transaction Done: {}", askOrder.toString());
                                }
                            }
                        } catch (Exception e) {
                            log.error(e.toString());
                        }
                    }
                } //end of loop through all current ASKs
                if (pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    //theres still some volume that has not been met
                    //so create an ASK for the remaining volume
                    bidTreeMap = assetTreeMap.get(OrderBook.BID);
                    orderItemModel.setVolume(pendingVolume);
                    if (bidTreeMap.containsKey(orderItemModel.getAmount())) {
                        PriorityQueue<OrderItemModel> priorityQueue = bidTreeMap.get(orderItemModel.getAmount());
                        priorityQueue.add(orderItemModel);
                    } else {
                        PriorityQueue<OrderItemModel> priorityQueue = new PriorityQueue<OrderItemModel>(new AskComparatorOrderTime());
                        priorityQueue.add(orderItemModel);
                        bidTreeMap.put(orderItemModel.getAmount(), priorityQueue);
                    }
                }


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
            //getHighBid();
            //log.info("ME: Scheduled runMatchingEngine Executed");
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
