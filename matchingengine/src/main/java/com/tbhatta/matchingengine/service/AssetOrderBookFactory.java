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

    public AssetOrderBook createAssetBook() {
        TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTree =
                new TreeMap<>(new BidComparatorPriceBigDecimal());
        TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTree =
                new TreeMap<>(new AskComparatorPriceBigDecimal());
        var orderBook = new AssetOrderBook(bidTree, askTree);
        return orderBook;
    }
}
