package com.tbhatta.matchingengine.service.matching;
import com.tbhatta.matchingengine.model.OrderItemModel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.UUID;

import com.tbhatta.matchingengine.model.comparator.AskComparatorOrderTime;
import com.tbhatta.matchingengine.model.comparator.AskComparatorPriceBigDecimal;
import com.tbhatta.matchingengine.model.comparator.BidComparatorOrderTime;
import com.tbhatta.matchingengine.model.comparator.BidComparatorPriceBigDecimal;
public class OrderBookTestFixtures {
    public static final String ASSET  = "btc";
    public static final String CLIENT_A = "client-alice";
    public static final String CLIENT_B = "client-bob";
    public static final String CLIENT_C = "client-charlie";

    // Factory section for Orders
    public static OrderItemModel ask(String clientId, String price, int volume) {
        return order(clientId, "ask", price, volume, LocalDateTime.now());
    }

    public static OrderItemModel ask(String clientId, String price, int volume, LocalDateTime time) {
        return order(clientId, "ask", price, volume, time);
    }

    public static OrderItemModel bid(String clientId, String price, int volume) {
        return order(clientId, "bid", price, volume, LocalDateTime.now());
    }

    public static OrderItemModel bid(String clientId, String price, int volume, LocalDateTime time) {
        return order(clientId, "bid", price, volume, time);
    }

    private static OrderItemModel order(String clientId, String type, String price, int volume, LocalDateTime time) {
        return new OrderItemModel(
                UUID.randomUUID(),
                clientId,
                ASSET,
                time,
                type,
                new BigDecimal(price),
                BigInteger.valueOf(volume)
        );
    }

    //Factory Section for Trees
    public static TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> emptyBidTree() {
        return new TreeMap<>(new BidComparatorPriceBigDecimal());
    }

    public static TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> emptyAskTree() {
        return new TreeMap<>(new AskComparatorPriceBigDecimal());
    }

    public static void addToBidTree(
            TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> tree,
            OrderItemModel order
    ) {
        tree.computeIfAbsent(order.getAmount(), k -> new PriorityQueue<>(new BidComparatorOrderTime()))
                .add(order);
    }

    public static void addToAskTree(
            TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> tree,
            OrderItemModel order
    ) {
        tree.computeIfAbsent(order.getAmount(), k -> new PriorityQueue<>(new AskComparatorOrderTime()))
                .add(order);
    }

    // Helper
    public static int totalOrdersInTree(TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> tree) {
        return tree.values().stream().mapToInt(PriorityQueue::size).sum();
    }

}
