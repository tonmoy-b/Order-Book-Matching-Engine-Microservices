package com.tbhatta.matchingengine.service.matching;

import com.tbhatta.matchingengine.model.OrderItemModel;
import org.springframework.data.mongodb.core.aggregation.BooleanOperators;

import java.math.BigDecimal;
import java.math.BigInteger;

public class MatchResult {
    private final OrderItemModel incomingOrder;
    private final OrderItemModel matchedOrder;
    private final BigInteger fillVolume;
    private final BigDecimal spread;
    private final FillType fillType;

    public MatchResult(OrderItemModel incomingOrder, OrderItemModel matchedOrder,
                       BigInteger fillVolume, BigDecimal spread, FillType fillType) {
        this.incomingOrder = incomingOrder;
        this.matchedOrder = matchedOrder;
        this.fillVolume = fillVolume;
        this.spread = spread;
        this.fillType = fillType;
    }

    public MatchResult mirrored() {
        return new MatchResult(this.incomingOrder,
                this.matchedOrder, this.fillVolume, this.spread.negate(),
                mirrorFillType(this.fillType));
    }

    public FillType mirrorFillType(FillType originalFillType) {
        // return the FillType for the counter matching fill
        return switch (originalFillType) {
            case INCOMING_PARTIAL -> FillType.COUNTERPARTY_PARTIAL;
            case COUNTERPARTY_PARTIAL -> FillType.INCOMING_PARTIAL;
            case EXACT_FULL -> FillType.EXACT_FULL; //EXACT_FULL mirrors EXACT_FULL
        };
    }

    public OrderItemModel getIncomingOrder() {
        return incomingOrder;
    }

    public OrderItemModel getMatchedOrder() {
        return matchedOrder;
    }

    public BigInteger getFillVolume() {
        return fillVolume;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public FillType getFillType() {
        return fillType;
    }

    @Override
    public String toString() {
        return "MatchResult{" +
                "incomingOrder=" + incomingOrder +
                ", matchedOrder=" + matchedOrder +
                ", fillVolume=" + fillVolume +
                ", spread=" + spread +
                ", fillType=" + fillType +
                '}';
    }
}
