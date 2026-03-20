package com.tbhatta.matchingengine.service.matching;

import com.tbhatta.matchingengine.model.OrderItemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;

@Component
public class AskMatchingStrategy implements MatchingStrategy{

    private static final Logger log = LoggerFactory.getLogger(AskMatchingStrategy.class);

    @Override
    public List<MatchResult> match(OrderItemModel incomingAsk, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTree) {
        List<MatchResult> matchingResults = new ArrayList<>();
        BigDecimal askPrice       = incomingAsk.getAmount();
        BigInteger pendingVolume  = incomingAsk.getVolume();
        Iterator<BigDecimal> priceIterator = bidTree.keySet().iterator();
        while (priceIterator.hasNext() && pendingVolume.compareTo(BigInteger.ZERO) > 0) {
            BigDecimal bidPrice = priceIterator.next();
            if (bidPrice.compareTo(askPrice) < 0) {
                // bid must >= ask
                // since bidTree has highest price first, no further keys can match
                break;
            }
            PriorityQueue<OrderItemModel> priceLevel = bidTree.get(bidPrice);
            if (priceLevel == null || priceLevel.isEmpty()) {
                continue;
            }
            List<OrderItemModel> toRequeue = new ArrayList<>();
            int numOrdersAtLevel = priceLevel.size();
            for (int i = 0; i < numOrdersAtLevel && pendingVolume.compareTo(BigInteger.ZERO) > 0; i++) {
                OrderItemModel bidOrder = priceLevel.peek();
                if (bidOrder == null) {
                    continue;
                }
                // make sure there's no self-matching
                if (bidOrder.getClientId().strip().equalsIgnoreCase(incomingAsk.getClientId().strip())) {
                    toRequeue.add(priceLevel.poll());
                    continue;
                }
                priceLevel.poll(); // remove from queue
                BigDecimal spread = bidOrder.getAmount().subtract(incomingAsk.getAmount());
                int volumeComparison = pendingVolume.compareTo(bidOrder.getVolume());
                if (volumeComparison < 0) {
                    // incoming ask is smaller so incoming is fully filled, bid order is partially filled
                    BigInteger fillVolume = pendingVolume;
                    bidOrder.setVolume(bidOrder.getVolume().subtract(pendingVolume));
                    pendingVolume = BigInteger.ZERO;
                    matchingResults.add(new MatchResult(incomingAsk, bidOrder, fillVolume, spread, FillType.INCOMING_PARTIAL));
                    toRequeue.add(bidOrder); // still has remaining volume
                } else if (volumeComparison > 0) {
                    // incoming ASK is bigger so bid order is fully consumed, incoming continues
                    BigInteger fillVolume = bidOrder.getVolume();
                    pendingVolume = pendingVolume.subtract(bidOrder.getVolume());
                    bidOrder.setVolume(BigInteger.ZERO);
                    matchingResults.add(new MatchResult(incomingAsk, bidOrder, fillVolume, spread, FillType.COUNTERPARTY_PARTIAL));
                    // bidOrder is fully used, no need to requeue
                } else {
                    // Exact fill
                    BigInteger fillVolume = pendingVolume;
                    pendingVolume = BigInteger.ZERO;
                    bidOrder.setVolume(BigInteger.ZERO);
                    matchingResults.add(new MatchResult(incomingAsk, bidOrder, fillVolume, spread, FillType.EXACT_FULL));
                    // bidOrder is fully consumed, no need to requeue
                }
            } //eof
            priceLevel.addAll(toRequeue);
        }
        // update the incomingOrder's remaining volume
        incomingAsk.setVolume(pendingVolume);
        log.info("AskMatchingStrategy: {} fills for order {}, residual volume={}",
                matchingResults.size(), incomingAsk.getOrderId(), pendingVolume);
        return matchingResults;
    }
}
