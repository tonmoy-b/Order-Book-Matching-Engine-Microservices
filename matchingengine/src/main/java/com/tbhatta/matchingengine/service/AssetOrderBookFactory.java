package com.tbhatta.matchingengine.service;

import org.springframework.stereotype.Component;
import com.tbhatta.matchingengine.model.OrderItemModel;
import com.tbhatta.matchingengine.model.comparator.AskComparatorPriceBigDecimal;
import com.tbhatta.matchingengine.model.comparator.BidComparatorPriceBigDecimal;
import com.tbhatta.matchingengine.model.comparator.AskComparatorOrderTime;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.TreeMap;

@Component
public class AssetOrderBookFactory {
    public PriorityQueue<OrderItemModel> createPriceLevel() {
        return new PriorityQueue<>(new AskComparatorOrderTime());
    }

    public HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> createAssetBook() {
        TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTree =
                new TreeMap<>(new BidComparatorPriceBigDecimal());
        TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTree =
                new TreeMap<>(new AskComparatorPriceBigDecimal());
        HashMap<String, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>>> book = new HashMap<>();
        book.put(OrderBook.BID, bidTree);
        book.put(OrderBook.ASK, askTree);
        return book;
    }
}
