package com.tbhatta.matchingengine.model.comparator;

import com.tbhatta.matchingengine.model.OrderItemModel;

import java.util.Comparator;

public class AskComparatorOrderTime implements Comparator<OrderItemModel> {
    @Override
    public int compare(OrderItemModel o1, OrderItemModel o2) {
        return o1.getOrderTime().compareTo(o2.getOrderTime());
    }
}
