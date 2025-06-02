package com.tbhatta.matchingengine.model.comparator;

import java.math.BigDecimal;
import java.util.Comparator;

public class BidComparatorPriceBigDecimal implements Comparator<BigDecimal> {
    @Override
    public int compare(BigDecimal o1, BigDecimal o2) {
        return o1.compareTo(o2);
    }


}
