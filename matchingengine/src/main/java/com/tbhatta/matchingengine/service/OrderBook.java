package com.tbhatta.matchingengine.service;

import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.model.comparator.*;
import com.tbhatta.matchingengine.model.comparator.AskComparatorOrderTime;
import com.tbhatta.matchingengine.model.comparator.BidComparatorOrderTime;
import com.tbhatta.matchingengine.service.matching.AskMatchingStrategy;
import com.tbhatta.matchingengine.service.matching.BidMatchingStrategy;
import com.tbhatta.matchingengine.service.matching.MatchResult;
import com.tbhatta.matchingengine.service.matching.MatchingStrategy;
import com.tbhatta.matchingengine.service.metrics.OrderBookMetrics;
import com.tbhatta.matchingengine.service.persistence.TransactionPersistenceService;
import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.aggregation.BooleanOperators;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.tbhatta.matchingengine.model.OrderItemModel;

@Service
public class OrderBook {
    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);
    public static final String ASK = "ask";
    public static final String BID = "bid";
    //public PriorityQueue<OrderItemModel> askPQ, bidPQ;
    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;
    public HashMap<String, HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>>> rootAssetHashMap;
    private final ConcurrentHashMap<String, AssetOrderBook> assetBooks = new ConcurrentHashMap<>();
    private final OrderBookMetrics orderBookMetrics;
    private final AssetOrderBookFactory assetOrderBookFactory;
    private final AskMatchingStrategy askMatchingStrategy;
    private final BidMatchingStrategy bidMatchingStrategy;
    private final TransactionPersistenceService transactionPersistenceService;

    public OrderBook(
            AssetOrderBookFactory assetOrderBookFactory,
            AskMatchingStrategy askMatchingStrategy,
            BidMatchingStrategy bidMatchingStrategy,
            TransactionPersistenceService transactionPersistenceService,
            OrderBookMetrics orderBookMetrics
    ) {
        this.assetOrderBookFactory = assetOrderBookFactory;
        this.askMatchingStrategy = askMatchingStrategy;
        this.bidMatchingStrategy = bidMatchingStrategy;
        this.transactionPersistenceService = transactionPersistenceService;
        this.orderBookMetrics = orderBookMetrics;
        rootAssetHashMap = new HashMap<>();
    }

    public void enterOrderItem_(OrderItemModel orderItemModel) {
        try {
            String asset     = normalise(orderItemModel.getAsset());
            String orderType = normalise(orderItemModel.getOrderType());
            orderBookMetrics.recordOrderReceived(orderType);
            AssetOrderBook orderBook = assetBooks.computeIfAbsent(
                    asset, k -> assetOrderBookFactory.createAssetBook()
            );
            orderBook.acquireWriteLock();
            try {
                //processOrder(orderItemModel, orderBook, orderType);
                orderBookMetrics.matchTimer(orderType).record(() -> {
                    processOrder(orderItemModel, orderBook, orderType); // your existing matching call
                });
            } catch (Exception e) {
                log.error("Error in enterOrderItem_ for order {}: {}",
                        orderItemModel.getOrderId(), e.getMessage(), e);
            } finally {
                orderBook.releaseWriteLock();
            }
        } catch (Exception e) {
            log.error("Error in enterOrderItem_ for order {}: {}", orderItemModel.getOrderId(), e.getMessage(), e);
        }
    }

    private void ensureAssetBookExists(String normalisedAsset, String originalAsset) {
        if (!rootAssetHashMap.containsKey(normalisedAsset)) {
            log.info("OrderBook: initialising new asset book for '{}'", normalisedAsset);
            var assetOrderBook = assetOrderBookFactory.createAssetBook();
            HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetTrees = new HashMap<>();
            assetTrees.put(ASK, assetOrderBook.getAskTree());
            assetTrees.put(BID, assetOrderBook.getBidTree());
            rootAssetHashMap.put(normalisedAsset, assetTrees);
        }
    }

    /// new helpers
    private String normalise(String s) {
        return s.strip().toLowerCase();
    }

    private void processOrder(
            OrderItemModel order,
            AssetOrderBook orderBook,
            String orderType
    ) {
        List<MatchResult> fills;
        if (orderType.equals(ASK)) {
            fills = askMatchingStrategy.match(order, orderBook.getBidTree());
        } else if (orderType.equals(BID)) {
            fills = bidMatchingStrategy.match(order, orderBook.getAskTree());
        } else {
            throw new IllegalArgumentException(
                    "OrderType must be 'bid' or 'ask', got: " + order.getOrderType()
            );
        }
        for (MatchResult fill : fills) {
            orderBookMetrics.recordFill(
                    orderType,
                    fill.getFillType().name(),
                    fill.getFillVolume().doubleValue()
            );
        }
        transactionPersistenceService.persistAll(fills);
        requeueResidual(order, orderBook, orderType);
    }

    private void requeueResidual(
            OrderItemModel order,
            AssetOrderBook orderBook,
            String orderType
    ) {
        orderType = normalise(orderType);
        if (order.getVolume().compareTo(BigInteger.ZERO) <= 0) {
            return; // fully filled, nothing left to insert
        }
        TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> tree;
        if (orderType.equals(ASK)) {
            tree = orderBook.getAskTree();
        } else if (orderType.equals(BID)) {
            tree = orderBook.getBidTree();
        } else {
            throw new IllegalArgumentException(
                    "OrderType must be 'bid' or 'ask', got: " + order.getOrderType()
            );
        }
        BigDecimal orderAmount = order.getAmount();
        if (!tree.containsKey(orderAmount)) {
            tree.put(orderAmount, assetOrderBookFactory.createPriceLevel());
        }
        tree.get(orderAmount).add(order);
        log.info("OrderBook: parked residual volume={} for order {} on {} side at price {}",
                order.getVolume(), order.getOrderId(), orderType, orderAmount);
    }


    private String iteratorToString(Iterator<BigDecimal> iterator) {
        String s = "";
        for (Iterator<BigDecimal> it = iterator; it.hasNext(); ) {
            BigDecimal bigDecimal = it.next();
            s += bigDecimal.toString() + "; ";
        }
        return s + ".";

    }

    //keep lean to release lock asap
    public List<OrderItemModel> getPendingOrdersByClientId(String strClientId) {
        List<OrderItemModel> pendingOrders = new ArrayList<>();
        String normClient = normalise(strClientId);
        for (Map.Entry<String, AssetOrderBook> entry : assetBooks.entrySet()) {
            AssetOrderBook orderBook = entry.getValue();
            orderBook.acquireReadLock();
            try {
                collectPendingFromTree(orderBook.getBidTree(), normClient, pendingOrders);
                collectPendingFromTree(orderBook.getAskTree(), normClient, pendingOrders);
            } catch (Exception e) {
                log.error("Error in getPendingOrdersByClientId with clientId == {}.\n\t Exception is {}", strClientId, e.getStackTrace());
            }
            finally {
                orderBook.releaseReadLock();
            }
        }
        return pendingOrders;
    }

    private void collectPendingFromTree(TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> tree,
                                        String normClient,
                                        List<OrderItemModel> listPendingOrders) {
        //loop through priority queues
        for (PriorityQueue<OrderItemModel> queue : tree.values()) {
            if (queue == null) {
                continue;
            }
            queue.stream()
                    .filter(Objects::nonNull)
                    .filter(orderItemModel -> normalise(orderItemModel.getClientId()).equals(normClient))
                    .map(OrderItemModel::makeCopy)
                    .forEach(listPendingOrders::add);
        }
    }


    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getBidTreeByAsset(String assetTicker) {
        AssetOrderBook assetOrderBook = assetBooks.get(normalise(assetTicker));
        if (assetOrderBook != null) {
            return assetOrderBook.getBidTree();
        } else {
            return null;
        }
    }

    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getAskTreeByAsset(String assetTicker) {
        AssetOrderBook assetOrderBook = assetBooks.get(normalise(assetTicker));
        if (assetOrderBook != null) {
            return assetOrderBook.getAskTree();
        } else {
            return null;
        }
    }

    public long getResidualVolume(String assetTicker, String side) {
        AssetOrderBook book = assetBooks.get(normalise(assetTicker));
        if (book == null) return 0;
        // locking taken care of in AssetOrderBook func. code
        return book.getTotalVolume(side);
    }

    public List<OrderItemModel> getPendingOrdersByClientIdNoLock(String strClientId) {
        try {
            List<OrderItemModel> pendingTransaction = new ArrayList<>();
            //List<String> orderTypes = Arrays.asList(OrderBook.BID.strip(), OrderBook.ASK.strip());
            for (String strAssetTicker : rootAssetHashMap.keySet()) {
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidHashMap = rootAssetHashMap.get(strAssetTicker).get(OrderBook.BID);
                var bidHashMapKeys = bidHashMap.keySet();
                for (BigDecimal bidKey : bidHashMapKeys) {
                    if (bidHashMap.get(bidKey) != null) {
                        pendingTransaction.addAll(bidHashMap.get(bidKey)
                                .stream()
                                .filter(Objects::nonNull)
                                .filter(obj -> obj.getClientId().strip().equalsIgnoreCase(strClientId.strip()))
                                .<OrderItemModel>map(OrderItemModel::makeCopy)
                                .toList());
                    }
                }
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askHashMap = rootAssetHashMap.get(strAssetTicker).get(OrderBook.ASK);
                var askHashMapKeys = askHashMap.keySet();
                for (BigDecimal askKey : askHashMapKeys) {
                    if (askHashMap.get(askKey) != null) {
                        pendingTransaction.addAll(askHashMap.get(askKey)
                                .stream()
                                .filter(Objects::nonNull)
                                .filter(obj -> obj.getClientId().strip().equalsIgnoreCase(strClientId.strip()))
                                .<OrderItemModel>map(OrderItemModel::makeCopy)
                                .toList());
                    }
                }
            }
            return pendingTransaction;
        } catch (Exception e) {
            log.error("error in getPendingTransactionsByClientId with \n{}", e.toString());
            return null;
        }
    }


    /// /////////////////////////////
    //@Scheduled(fixedRate = 3000)
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
