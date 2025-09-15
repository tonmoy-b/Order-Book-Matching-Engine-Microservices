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
import java.util.stream.Collectors;

import com.tbhatta.matchingengine.model.OrderItemModel;

@Service
public class OrderBookMultiThreaded {
    private static final Logger log = LoggerFactory.getLogger(OrderBookMultiThreaded.class);
    public static final String ASK = "ask";
    public static final String BID = "bid";
    public HashMap<String, HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>>> rootAssetHashMap;
    @Autowired
    public TransactionItemRepository transactionItemRepository;

    public OrderBookMultiThreaded() {
        rootAssetHashMap = new HashMap<>();//assetRootHasMap is built at app startup
    }

    public void enterOrderItem(OrderItemModel orderItemModel) {
        try {
            String orderAsset = orderItemModel.getAsset().strip().toLowerCase();
            if (!rootAssetHashMap.containsKey(orderAsset)) {
                //There's no order for this ticker, so create it
                TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTreeMap, askTreeMap;
                bidTreeMap = new TreeMap<>(new BidComparatorPriceBigDecimal());
                askTreeMap = new TreeMap<>(new AskComparatorPriceBigDecimal());
                HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> treeMaps = new HashMap<>();
                treeMaps.put(OrderBook.BID, bidTreeMap);
                treeMaps.put(OrderBook.ASK, askTreeMap);
                rootAssetHashMap.put(orderAsset, treeMaps);
            }
            HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> assetHashMapForTreeMaps = rootAssetHashMap.get(orderAsset);

            //if they exist, check if bid-tree exists in cse of ask order or vice versa
            BigDecimal orderPrice = orderItemModel.getAmount();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
