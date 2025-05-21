package com.tbhatta.matchingengine.model.comparator;

import com.tbhatta.matchingengine.model.OrderItemModel;

import java.util.Comparator;

public class AskComparator implements Comparator<OrderItemModel> {
    @Override
    public int compare(OrderItemModel o1, OrderItemModel o2) {
        return o2.getAmount().compareTo(o1.getAmount());
    }
}
