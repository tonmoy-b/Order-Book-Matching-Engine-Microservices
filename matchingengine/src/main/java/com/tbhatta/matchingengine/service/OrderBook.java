package com.tbhatta.matchingengine.service;

import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.model.comparator.*;
import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public HashMap<String, HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>>> rootAssetHashMap;
    @Autowired
    public TransactionItemRepository transactionItemRepository;

    public OrderBook() {
        //askPQ = new PriorityQueue<>(new AskComparatorPrice());
        //bidPQ = new PriorityQueue<>(new BidComparatorPrice());
        // //
        //bidTreeMap = new TreeMap<>(new BidComparatorPriceBigDecimal());
        //askTreeMap = new TreeMap<>(new AskComparatorPriceBigDecimal());
        rootAssetHashMap = new HashMap<>();
    }

    public void enterOrderItem_(OrderItemModel orderItemModel) {
        try {
            String orderAsset = orderItemModel.getAsset().strip().toLowerCase();
            //check if ask-bid trees exist for that asset
            //if they do not exist, create hashmap
            if (!rootAssetHashMap.containsKey(orderAsset)) {
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;
                bidTreeMap = new TreeMap<>(new BidComparatorPriceBigDecimal());
                askTreeMap = new TreeMap<>(new AskComparatorPriceBigDecimal());
                HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> treeMaps = new HashMap<>();
                treeMaps.put(OrderBook.BID, bidTreeMap);
                treeMaps.put(OrderBook.ASK, askTreeMap);
                rootAssetHashMap.put(orderItemModel.getAsset(), treeMaps);
            }
            HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetHashMapForTreeMaps = rootAssetHashMap.get(orderAsset.toLowerCase().strip());
            //if they exist, check if bid-tree exists in cse of ask order or vice versa
            BigDecimal orderPrice = orderItemModel.getAmount();
            List<OrderItemModel> transactions = new ArrayList<>();
            if (orderItemModel.getOrderType().strip().equalsIgnoreCase(OrderBook.ASK)) {
                //Order coming in from the Kafka queue is of an Ask-type
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap = assetHashMapForTreeMaps.get(OrderBook.BID);
                Iterator<BigDecimal> keyPricesBidTreeMapIterator = bidTreeMap.keySet().iterator();
                log.info("ME: Iterator of Bid Prices is {}", iteratorToString(keyPricesBidTreeMapIterator));//iterator runs till end here
                keyPricesBidTreeMapIterator = bidTreeMap.keySet().iterator();//refetch the iterator
                log.info("ME: Iterator.hasNext() == {}, and pendvol == {}", keyPricesBidTreeMapIterator.hasNext(), orderItemModel.getVolume().compareTo(BigInteger.ZERO) > 0);
                BigInteger pendingVolume = orderItemModel.getVolume();//Volume to be sold to asker
                while (keyPricesBidTreeMapIterator.hasNext() && pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    BigDecimal keyPrice = keyPricesBidTreeMapIterator.next();
                    if (keyPrice.compareTo(orderPrice) >= 0) {
                        //Bid-Amount >= Ask-Amount, thus transaction is possible
                        try {
                            //OrderItemModel bidOrder = bidTreeMap.get(keyPrice).peek();
                            //bidOrder = bidTreeMap.get(keyPrice).poll();
                            //break;
                            PriorityQueue<OrderItemModel> priorityQueue = bidTreeMap.get(keyPrice);
                            //iterate through OrderItemModels in the PriorityQueue at this Price point
                            ArrayList<OrderItemModel> itemsRemovedFromPriorityQueue = new ArrayList<>();//for later re-insertion
                            int numItemsInPriorityQueue = priorityQueue.size();
                            for (int i = 0; i < numItemsInPriorityQueue; i++) {
                                if (priorityQueue.peek().getClientId().strip().equalsIgnoreCase(orderItemModel.getClientId().strip())) {
                                    //Don't match between bid and ask requests of the same client
                                    continue;
                                }
                                OrderItemModel bidOrder = priorityQueue.poll();//bidTreeMap.get(keyPrice).poll();
                                if (bidOrder == null) {
                                    //shouldn't be necessary as PriorityQueues don't accept null elements
                                    continue;
                                }
                                if (pendingVolume.compareTo(bidOrder.getVolume()) < 0) {
                                    BigInteger transactionVolume = new BigInteger(String.valueOf(pendingVolume));
                                    bidOrder.setVolume(bidOrder.getVolume().subtract(pendingVolume));
                                    pendingVolume = BigInteger.valueOf(0);
                                    //main-party is from kafka, so orderItemModel
                                    //counter-party is from bidTree, so bidOrder
                                    //make transaction record (for mongodb insertion) for the order coming in from kafka
                                    TransactionItemModel mainPartyTransaction = new TransactionItemModel();
                                    mainPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    mainPartyTransaction.setMainClientID(orderItemModel.getClientId());
                                    mainPartyTransaction.setCounterPartyID(bidOrder.getClientId());
                                    mainPartyTransaction.setMainclientOrderId(orderItemModel.getOrderId().toString());
                                    mainPartyTransaction.setCounterPartOrderId(bidOrder.getOrderId().toString());
                                    mainPartyTransaction.setMainClientOrderType(orderItemModel.getOrderType());//
                                    mainPartyTransaction.setMainClientTransactionAmount(orderItemModel.getAmount());
                                    mainPartyTransaction.setSpreadAmount(bidOrder.getAmount().subtract(orderItemModel.getAmount()));
                                    //make transaction record (for mongodb insertion) for the bid order from bid-tree in memory
                                    TransactionItemModel counterPartyTransaction = new TransactionItemModel();
                                    counterPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    counterPartyTransaction.setMainClientID(bidOrder.getClientId());
                                    counterPartyTransaction.setCounterPartyID(orderItemModel.getClientId());
                                    counterPartyTransaction.setMainclientOrderId(bidOrder.getOrderId().toString());
                                    counterPartyTransaction.setCounterPartOrderId(orderItemModel.getOrderId().toString());
                                    counterPartyTransaction.setMainClientOrderType(bidOrder.getOrderType());
                                    counterPartyTransaction.setMainClientTransactionAmount(bidOrder.getAmount());
                                    counterPartyTransaction.setSpreadAmount(orderItemModel.getAmount().subtract(bidOrder.getAmount()));
                                    //save in mongodb
                                    transactionItemRepository.insert(mainPartyTransaction);
                                    transactionItemRepository.insert(counterPartyTransaction);
                                    //since there's still some volume left in bidOrder put iit in ArrayList for re-insertion
                                    itemsRemovedFromPriorityQueue.add(bidOrder);

                                } else if (pendingVolume.compareTo(bidOrder.getVolume()) > 0) {
                                    BigInteger transactionVolume = new BigInteger(String.valueOf(pendingVolume));
                                    pendingVolume = new BigInteger(bidOrder.getVolume().subtract(pendingVolume).toString());
                                    bidOrder.setVolume(BigInteger.valueOf(0));
                                    //main-party is from kafka, so orderItemModel
                                    //counter-party is from bidTree, so bidOrder
                                    //make transaction record (for mongodb insertion) for the order coming in from kafka
                                    TransactionItemModel mainPartyTransaction = new TransactionItemModel();
                                    mainPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    mainPartyTransaction.setMainClientID(orderItemModel.getClientId());
                                    mainPartyTransaction.setCounterPartyID(bidOrder.getClientId());
                                    mainPartyTransaction.setMainclientOrderId(orderItemModel.getOrderId().toString());
                                    mainPartyTransaction.setCounterPartOrderId(bidOrder.getOrderId().toString());
                                    mainPartyTransaction.setMainClientOrderType(orderItemModel.getOrderType());//
                                    mainPartyTransaction.setMainClientTransactionAmount(orderItemModel.getAmount());
                                    mainPartyTransaction.setSpreadAmount(bidOrder.getAmount().subtract(orderItemModel.getAmount()));
                                    //make transaction record (for mongodb insertion) for the bid order from bid-tree in memory
                                    TransactionItemModel counterPartyTransaction = new TransactionItemModel();
                                    counterPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    counterPartyTransaction.setMainClientID(bidOrder.getClientId());
                                    counterPartyTransaction.setCounterPartyID(orderItemModel.getClientId());
                                    counterPartyTransaction.setMainclientOrderId(bidOrder.getOrderId().toString());
                                    counterPartyTransaction.setCounterPartOrderId(orderItemModel.getOrderId().toString());
                                    counterPartyTransaction.setMainClientOrderType(bidOrder.getOrderType());
                                    counterPartyTransaction.setMainClientTransactionAmount(bidOrder.getAmount());
                                    counterPartyTransaction.setSpreadAmount(orderItemModel.getAmount().subtract(bidOrder.getAmount()));
                                    //save in mongodb
                                    transactionItemRepository.insert(mainPartyTransaction);
                                    transactionItemRepository.insert(counterPartyTransaction);
                                } else {
                                    //pendingVolume == bibOrderVolue
                                    BigInteger transactionVolume = new BigInteger(String.valueOf(pendingVolume));
                                    pendingVolume = BigInteger.valueOf(0);
                                    bidOrder.setVolume(BigInteger.valueOf(0));
                                    //main-party is from kafka, so orderItemModel
                                    //counter-party is from bidTree, so bidOrder
                                    //make transaction record (for mongodb insertion) for the order coming in from kafka
                                    TransactionItemModel mainPartyTransaction = new TransactionItemModel();
                                    mainPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    mainPartyTransaction.setMainClientID(orderItemModel.getClientId());
                                    mainPartyTransaction.setCounterPartyID(bidOrder.getClientId());
                                    mainPartyTransaction.setMainclientOrderId(orderItemModel.getOrderId().toString());
                                    mainPartyTransaction.setCounterPartOrderId(bidOrder.getOrderId().toString());
                                    mainPartyTransaction.setMainClientOrderType(orderItemModel.getOrderType());//
                                    mainPartyTransaction.setMainClientTransactionAmount(orderItemModel.getAmount());
                                    mainPartyTransaction.setSpreadAmount(bidOrder.getAmount().subtract(orderItemModel.getAmount()));
                                    //make transaction record (for mongodb insertion) for the bid order from bid-tree in memory
                                    TransactionItemModel counterPartyTransaction = new TransactionItemModel();
                                    counterPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    counterPartyTransaction.setMainClientID(bidOrder.getClientId());
                                    counterPartyTransaction.setCounterPartyID(orderItemModel.getClientId());
                                    counterPartyTransaction.setMainclientOrderId(bidOrder.getOrderId().toString());
                                    counterPartyTransaction.setCounterPartOrderId(orderItemModel.getOrderId().toString());
                                    counterPartyTransaction.setMainClientOrderType(bidOrder.getOrderType());
                                    counterPartyTransaction.setMainClientTransactionAmount(bidOrder.getAmount());
                                    counterPartyTransaction.setSpreadAmount(orderItemModel.getAmount().subtract(bidOrder.getAmount()));
                                    //save in mongodb
                                    transactionItemRepository.insert(mainPartyTransaction);
                                    transactionItemRepository.insert(counterPartyTransaction);
                                }
                            }//end of iteration through PriorityQueue
                            priorityQueue.addAll(itemsRemovedFromPriorityQueue);//
                            itemsRemovedFromPriorityQueue = null;

                        } catch (Exception e) {
                            log.info("ME Enginge Exception in fetching PQ {}", e.toString());
                        }
                    }
                } //end of iteration through each BigDecial-key of Bid-TreeMap
                if (pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    //theres still some volume that has not been met
                    //so create an ASK for the remaining volume
                    TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTreeMap = assetHashMapForTreeMaps.get(OrderBook.ASK);
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
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTreeMap = assetHashMapForTreeMaps.get(OrderBook.ASK);
                Iterator<BigDecimal> keyPricesAskTreeMapIterator = askTreeMap.keySet().iterator();//keys are the Ask prices
                log.info("ME: Iterator of Ask Prices is {}", iteratorToString(keyPricesAskTreeMapIterator));//iterator runs till the end here
                keyPricesAskTreeMapIterator = askTreeMap.keySet().iterator();//refetch the iterator
                log.info("ME: Iterator.hasNext() == {}, and pendvol == {}", keyPricesAskTreeMapIterator.hasNext(), orderItemModel.getVolume().compareTo(BigInteger.ZERO) > 0);
                BigInteger pendingVolume = orderItemModel.getVolume();//volume of sale required in the Bid-Order
                while (keyPricesAskTreeMapIterator.hasNext() && pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    BigDecimal keyPrice = keyPricesAskTreeMapIterator.next();
                    if (keyPrice.compareTo(orderPrice) <= 0) {
                        try {
                            PriorityQueue<OrderItemModel> priorityQueue = askTreeMap.get(keyPrice);
                            //iterate through OrderItemModels in the PriorityQueue at this Price point
                            ArrayList<OrderItemModel> itemsRemovedFromPriorityQueue = new ArrayList<>();//for later re-insertion
                            int numItemsInPriorityQueue = priorityQueue.size();
                            for (int i = 0; i < numItemsInPriorityQueue; i++) {
                                if (priorityQueue.peek().getClientId().strip().equalsIgnoreCase(orderItemModel.getClientId().strip())) {
                                    //Don't match between bid and ask requests of the same client
                                    continue;
                                }
                                OrderItemModel askOrder = priorityQueue.poll();//bidTreeMap.get(keyPrice).poll();
                                if (askOrder == null) {
                                    //shouldn't be necessary as PriorityQueues don't accept null elements
                                    continue;
                                }
                                if (pendingVolume.compareTo(askOrder.getVolume()) < 0) {
                                    BigInteger transactionVolume = new BigInteger(String.valueOf(pendingVolume));
                                    askOrder.setVolume(askOrder.getVolume().subtract(pendingVolume));
                                    pendingVolume = BigInteger.valueOf(0);
                                    //main-party is from kafka, so orderItemModel
                                    //counter-party is from bidTree, so bidOrder
                                    //make transaction record (for mongodb insertion) for the order coming in from kafka
                                    TransactionItemModel mainPartyTransaction = new TransactionItemModel();
                                    mainPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    mainPartyTransaction.setMainClientID(orderItemModel.getClientId());
                                    mainPartyTransaction.setCounterPartyID(askOrder.getClientId());
                                    mainPartyTransaction.setMainclientOrderId(orderItemModel.getOrderId().toString());
                                    mainPartyTransaction.setCounterPartOrderId(askOrder.getOrderId().toString());
                                    mainPartyTransaction.setMainClientOrderType(orderItemModel.getOrderType());//
                                    mainPartyTransaction.setMainClientTransactionAmount(orderItemModel.getAmount());
                                    mainPartyTransaction.setSpreadAmount(askOrder.getAmount().subtract(orderItemModel.getAmount()));
                                    //make transaction record (for mongodb insertion) for the bid order from bid-tree in memory
                                    TransactionItemModel counterPartyTransaction = new TransactionItemModel();
                                    counterPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    counterPartyTransaction.setMainClientID(askOrder.getClientId());
                                    counterPartyTransaction.setCounterPartyID(orderItemModel.getClientId());
                                    counterPartyTransaction.setMainclientOrderId(askOrder.getOrderId().toString());
                                    counterPartyTransaction.setCounterPartOrderId(orderItemModel.getOrderId().toString());
                                    counterPartyTransaction.setMainClientOrderType(askOrder.getOrderType());
                                    counterPartyTransaction.setMainClientTransactionAmount(askOrder.getAmount());
                                    counterPartyTransaction.setSpreadAmount(orderItemModel.getAmount().subtract(askOrder.getAmount()));
                                    //save in mongodb
                                    transactionItemRepository.insert(mainPartyTransaction);
                                    transactionItemRepository.insert(counterPartyTransaction);
                                    //since there's still some volume left in bidOrder put iit in ArrayList for re-insertion
                                    itemsRemovedFromPriorityQueue.add(askOrder);
                                } else if (pendingVolume.compareTo(askOrder.getVolume()) > 0) {
                                    BigInteger transactionVolume = new BigInteger(String.valueOf(pendingVolume));
                                    pendingVolume = new BigInteger(askOrder.getVolume().subtract(pendingVolume).toString());
                                    askOrder.setVolume(BigInteger.valueOf(0));
                                    //main-party is from kafka, so orderItemModel
                                    //counter-party is from bidTree, so bidOrder
                                    //make transaction record (for mongodb insertion) for the order coming in from kafka
                                    TransactionItemModel mainPartyTransaction = new TransactionItemModel();
                                    mainPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    mainPartyTransaction.setMainClientID(orderItemModel.getClientId());
                                    mainPartyTransaction.setCounterPartyID(askOrder.getClientId());
                                    mainPartyTransaction.setMainclientOrderId(orderItemModel.getOrderId().toString());
                                    mainPartyTransaction.setCounterPartOrderId(askOrder.getOrderId().toString());
                                    mainPartyTransaction.setMainClientOrderType(orderItemModel.getOrderType());//
                                    mainPartyTransaction.setMainClientTransactionAmount(orderItemModel.getAmount());
                                    mainPartyTransaction.setSpreadAmount(askOrder.getAmount().subtract(orderItemModel.getAmount()));
                                    //make transaction record (for mongodb insertion) for the bid order from bid-tree in memory
                                    TransactionItemModel counterPartyTransaction = new TransactionItemModel();
                                    counterPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    counterPartyTransaction.setMainClientID(askOrder.getClientId());
                                    counterPartyTransaction.setCounterPartyID(orderItemModel.getClientId());
                                    counterPartyTransaction.setMainclientOrderId(askOrder.getOrderId().toString());
                                    counterPartyTransaction.setCounterPartOrderId(orderItemModel.getOrderId().toString());
                                    counterPartyTransaction.setMainClientOrderType(askOrder.getOrderType());
                                    counterPartyTransaction.setMainClientTransactionAmount(askOrder.getAmount());
                                    counterPartyTransaction.setSpreadAmount(orderItemModel.getAmount().subtract(askOrder.getAmount()));
                                    //save in mongodb
                                    transactionItemRepository.insert(mainPartyTransaction);
                                    transactionItemRepository.insert(counterPartyTransaction);

                                } else { //pendingVolume == askOrder.getVolume())
                                    BigInteger transactionVolume = new BigInteger(String.valueOf(pendingVolume));
                                    pendingVolume = BigInteger.valueOf(0);
                                    askOrder.setVolume(BigInteger.valueOf(0));
                                    //main-party is from kafka, so orderItemModel
                                    //counter-party is from bidTree, so bidOrder
                                    //make transaction record (for mongodb insertion) for the order coming in from kafka
                                    TransactionItemModel mainPartyTransaction = new TransactionItemModel();
                                    mainPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    mainPartyTransaction.setMainClientID(orderItemModel.getClientId());
                                    mainPartyTransaction.setCounterPartyID(askOrder.getClientId());
                                    mainPartyTransaction.setMainclientOrderId(orderItemModel.getOrderId().toString());
                                    mainPartyTransaction.setCounterPartOrderId(askOrder.getOrderId().toString());
                                    mainPartyTransaction.setMainClientOrderType(orderItemModel.getOrderType());//
                                    mainPartyTransaction.setMainClientTransactionAmount(orderItemModel.getAmount());
                                    mainPartyTransaction.setSpreadAmount(askOrder.getAmount().subtract(orderItemModel.getAmount()));
                                    //make transaction record (for mongodb insertion) for the bid order from bid-tree in memory
                                    TransactionItemModel counterPartyTransaction = new TransactionItemModel();
                                    counterPartyTransaction.setTransactionID(String.valueOf(UUID.randomUUID()));
                                    counterPartyTransaction.setMainClientID(askOrder.getClientId());
                                    counterPartyTransaction.setCounterPartyID(orderItemModel.getClientId());
                                    counterPartyTransaction.setMainclientOrderId(askOrder.getOrderId().toString());
                                    counterPartyTransaction.setCounterPartOrderId(orderItemModel.getOrderId().toString());
                                    counterPartyTransaction.setMainClientOrderType(askOrder.getOrderType());
                                    counterPartyTransaction.setMainClientTransactionAmount(askOrder.getAmount());
                                    counterPartyTransaction.setSpreadAmount(orderItemModel.getAmount().subtract(askOrder.getAmount()));
                                    //save in mongodb
                                    transactionItemRepository.insert(mainPartyTransaction);
                                    transactionItemRepository.insert(counterPartyTransaction);
                                }
                            }
                            priorityQueue.addAll(itemsRemovedFromPriorityQueue);//
                            itemsRemovedFromPriorityQueue = null;

                        } catch (Exception e) {
                            log.error(e.toString());
                        }
                    }
                } //end of loop through all current ASKs
                if (pendingVolume.compareTo(BigInteger.ZERO) > 0) {
                    //theres still some volume that has not been met
                    //so create an ASK for the remaining volume
                    bidTreeMap = assetHashMapForTreeMaps.get(OrderBook.BID);
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


    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getBidTreeByAsset(String assetTicker) {
        HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetMaps = rootAssetHashMap.get(assetTicker);
        return assetMaps.get(OrderBook.BID);
    }

    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getAskTreeByAsset(String assetTicker) {
        HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetMaps = rootAssetHashMap.get(assetTicker);
        return assetMaps.get(OrderBook.ASK);
    }





    /// //////////////////////////////////
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
        } else if (orderItemModel.getOrderType().toLowerCase().strip().equals("ask")) {
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
