package com.tbhatta.matchingengine.service.matching;

import com.tbhatta.matchingengine.model.OrderItemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Component
public class BidMatchingStrategy implements MatchingStrategy{

    private static final Logger log = LoggerFactory.getLogger(BidMatchingStrategy.class);

    @Override
    public List<MatchResult> match(OrderItemModel incomingBid, TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTree) {
        List<MatchResult> matchingResults = new ArrayList<>();
        BigDecimal bidPrice = incomingBid.getAmount();
        BigInteger pendingVolume = incomingBid.getVolume();
        Iterator<BigDecimal> priceIterator = askTree.keySet().iterator();
        while (priceIterator.hasNext() && pendingVolume.compareTo(BigInteger.ZERO) > 0) {
            BigDecimal askPrice = priceIterator.next();
            if (askPrice.compareTo(bidPrice) > 0) {
                // ask must <= bid
                // since askTree has lowest price first,
                // if there's a askPrice > bidPrice the following items will have the same condition
                break;
            }
            PriorityQueue<OrderItemModel> priceLevel = askTree.get(askPrice);
            if (priceLevel == null || priceLevel.isEmpty()) {
                continue; //no items present
            }

            List<OrderItemModel> toRequeue = new ArrayList<>();
            int numOrdersAtLevel = priceLevel.size();
            for (int i = 0; i < numOrdersAtLevel && pendingVolume.compareTo(BigInteger.ZERO) > 0; i++) {
                OrderItemModel askOrder = priceLevel.peek();
                if (askOrder == null) {
                    continue;
                }
                // make sure there's no self-matching
                if (askOrder.getClientId().strip().equalsIgnoreCase(incomingBid.getClientId().strip())) {
                    toRequeue.add(priceLevel.poll());
                    continue;
                }
                priceLevel.poll(); // remove from queue since processing done
                // spread from the BID side --> bid.amount - ask.amount, the buyer's saving
                BigDecimal spread = incomingBid.getAmount().subtract(askOrder.getAmount());
                int volumeComparison = pendingVolume.compareTo(askOrder.getVolume());
                if (volumeComparison < 0) {
                    // since bidVol < askVol
                    // bid is filled but ask has remaining
                    BigInteger fillVolume = pendingVolume;
                    askOrder.setVolume(askOrder.getVolume().subtract(pendingVolume));
                    pendingVolume = BigInteger.ZERO;
                    matchingResults.add(new MatchResult(incomingBid, askOrder, fillVolume, spread,
                            FillType.INCOMING_PARTIAL));
                    toRequeue.add(askOrder); // still has remaining volume
                } else if (volumeComparison > 0) {
                    //incomingOrder bid > ask, so fill more till there's pendingVolume
                    BigInteger fillVolume = askOrder.getVolume();
                    pendingVolume = pendingVolume.subtract(askOrder.getVolume());
                    askOrder.setVolume(BigInteger.ZERO);
                    matchingResults.add(new MatchResult(incomingBid, askOrder, fillVolume, spread, FillType.COUNTERPARTY_PARTIAL));
                    // askOrder is fully consumed, no need to requeue
                } else {
                    BigInteger fillVolume = pendingVolume;
                    pendingVolume = BigInteger.ZERO;
                    askOrder.setVolume(BigInteger.ZERO);
                    matchingResults.add(new MatchResult(incomingBid, askOrder, fillVolume, spread, FillType.EXACT_FULL));
                }
            } //eof
            priceLevel.addAll(toRequeue);
        }
        // update the incomingOrder's remaining volume
        incomingBid.setVolume(pendingVolume);
        log.info("BidMatchingStrategy: {} fills for order {}, residual volume={}",
                matchingResults.size(), incomingBid.getOrderId(), pendingVolume);
        return matchingResults;
    }
}
