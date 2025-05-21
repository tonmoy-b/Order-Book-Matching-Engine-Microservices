package com.tbhatta.matchingengine.model.comparator;

import java.util.Comparator;
import com.tbhatta.matchingengine.model.OrderItemModel;

public class BidComparator implements Comparator<OrderItemModel> {
    @Override
    public int compare(OrderItemModel o1, OrderItemModel o2) {
        return o1.getAmount().compareTo(o2.getAmount());
    }
}
