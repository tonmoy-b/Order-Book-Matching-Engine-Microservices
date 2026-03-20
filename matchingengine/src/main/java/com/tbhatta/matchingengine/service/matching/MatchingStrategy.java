package com.tbhatta.matchingengine.service.matching;

import com.tbhatta.matchingengine.model.OrderItemModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

public interface MatchingStrategy {
    List<MatchResult> match (OrderItemModel incomingOrder,
                             TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> oppositeTree);
}
