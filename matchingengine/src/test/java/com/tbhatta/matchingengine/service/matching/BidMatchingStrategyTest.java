package com.tbhatta.matchingengine.service.matching;

import static org.junit.jupiter.api.Assertions.*;

import com.tbhatta.matchingengine.model.OrderItemModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

import static com.tbhatta.matchingengine.service.matching.OrderBookTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class BidMatchingStrategyTest {
    private BidMatchingStrategy strategy;
    private TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTree;

    @BeforeEach
    void setup() {
        strategy = new BidMatchingStrategy();
        askTree = emptyAskTree();
    }

    //Scenarios: No Match
    @Nested
    @DisplayName("No fills")
    class NoFills {
        @Test
        @DisplayName("Empty ask tree → no fills, full volume remains")
        void emptyAskTree_noFills() {
            var bidOrder = bid(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).isEmpty();
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.valueOf(10));
        }

        @Test
        @DisplayName("Ask price above bid price → no match")
        void askPriceAboveBid_noFills() {
            addToAskTree(askTree, ask(CLIENT_B, "101.00", 10)); // ask=101, bid=100 → no match
            var bidOrder = bid(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).isEmpty();
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.valueOf(10));
            assertThat(totalOrdersInTree(askTree)).isEqualTo(1); // ask untouched
        }

        @Test
        @DisplayName("Same client ask and bid → self-match prevented")
        void sameClient_selfMatchPrevented() {
            addToAskTree(askTree, ask(CLIENT_A, "100.00", 10)); // same client as bid
            var bidOrder = bid(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).isEmpty();
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.valueOf(10));
            assertThat(totalOrdersInTree(askTree)).isEqualTo(1);
        }
    }

    //Scenarios: Exact match for full fills
    @Nested
    @DisplayName("Exact fills")
    class ExactFills {
        @Test
        @DisplayName("Bid volume == ask volume → EXACT fill, both consumed")
        void exactVolumeMatch() {
            addToAskTree(askTree, ask(CLIENT_B, "100.00", 10));
            var bidOrder = bid(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).hasSize(1);
            MatchResult fill = results.get(0);
            assertThat(fill.getFillType()).isEqualTo(FillType.EXACT_FULL);
            assertThat(fill.getFillVolume()).isEqualTo(BigInteger.valueOf(10));
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.ZERO);
            assertThat(totalOrdersInTree(askTree)).isEqualTo(0);
        }

        @Test
        @DisplayName("Ask price below bid → match succeeds, spread is positive (buyer saved money)")
        void askBelowBid_positiveSpread() {
            addToAskTree(askTree, ask(CLIENT_B, "95.00", 5));
            var bidOrder = bid(CLIENT_A, "100.00", 5);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).hasSize(1);
            // Spread from BID perspective = bid.amount - ask.amount = 100 - 95 = 5
            assertThat(results.get(0).getSpread()).isEqualByComparingTo(new BigDecimal("5.00"));
        }
    }

    //Scenarios: partial fills - incoming smaller
    @Nested
    @DisplayName("Incoming BID partially fills (bid volume < ask volume)")
    class IncomingPartial {

        @Test
        @DisplayName("Bid volume < ask volume → INCOMING_PARTIAL, ask partially remains in tree")
        void bidSmallerThanAsk_incomingPartial() {
            var existingAsk = ask(CLIENT_B, "100.00", 20);
            addToAskTree(askTree, existingAsk);
            var bidOrder = bid(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFillType()).isEqualTo(FillType.INCOMING_PARTIAL);
            assertThat(results.get(0).getFillVolume()).isEqualTo(BigInteger.valueOf(10));
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.ZERO);
            assertThat(totalOrdersInTree(askTree)).isEqualTo(1);
            assertThat(existingAsk.getVolume()).isEqualTo(BigInteger.valueOf(10));
        }
    }

    //Scenarios: partial fills - counterparty smaller
    @Nested
    @DisplayName("Counterparty ASK partially fills (bid volume > ask volume)")
    class CounterpartyPartial {

        @Test
        @DisplayName("Bid volume > ask volume → COUNTERPARTY_PARTIAL, residual remains on bid")
        void bidLargerThanAsk_counterpartyPartial() {
            addToAskTree(askTree, ask(CLIENT_B, "100.00", 5));
            var bidOrder = bid(CLIENT_A, "100.00", 20);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFillType()).isEqualTo(FillType.COUNTERPARTY_PARTIAL);
            assertThat(results.get(0).getFillVolume()).isEqualTo(BigInteger.valueOf(5));
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.valueOf(15));
            assertThat(totalOrdersInTree(askTree)).isEqualTo(0);
        }

        @Test
        @DisplayName("Bug regression: pendingVolume positive after counterparty partial fill")
        void pendingVolumeCalculation_isCorrect() {
            addToAskTree(askTree, ask(CLIENT_B, "100.00", 5));
            var bidOrder = bid(CLIENT_A, "100.00", 20);
            strategy.match(bidOrder, askTree);
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.valueOf(15));
            assertThat(bidOrder.getVolume().compareTo(BigInteger.ZERO)).isGreaterThan(0);
        }
    }

    //Scenarios: across multiple levels and orders
    @Nested
    @DisplayName("Multi-order and multi-price-level matching")
    class MultiOrder {

        @Test
        @DisplayName("Bid drains multiple asks at same price level in time order")
        void multipleOrdersSamePriceLevel_drainsInOrder() {
            LocalDateTime t1 = LocalDateTime.now().minusMinutes(2);
            LocalDateTime t2 = LocalDateTime.now().minusMinutes(1);
            var ask1 = ask(CLIENT_B, "100.00", 5, t1); // older — served first
            var ask2 = ask(CLIENT_C, "100.00", 5, t2);
            addToAskTree(askTree, ask1);
            addToAskTree(askTree, ask2);
            var bidOrder = bid(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMatchedOrder().getOrderId()).isEqualTo(ask1.getOrderId());
            assertThat(results.get(1).getMatchedOrder().getOrderId()).isEqualTo(ask2.getOrderId());
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.ZERO);
        }

        @Test
        @DisplayName("Bid matches across price levels, best ask (i.e. lowest) first")
        void multiplePriceLevels_bestAskFirst() {
            // ASK tree is lowest-first — 95 should be matched before 100
            addToAskTree(askTree, ask(CLIENT_B, "95.00", 5));
            addToAskTree(askTree, ask(CLIENT_C, "100.00", 5));
            var bidOrder = bid(CLIENT_A, "105.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMatchedOrder().getAmount())
                    .isEqualByComparingTo(new BigDecimal("95.00")); // lowest matched first
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.ZERO);
        }

        @Test
        @DisplayName("Bid stops matching when ask price exceeds bid price")
        void stopsMatchingAtPriceBoundary() {
            addToAskTree(askTree, ask(CLIENT_B, "95.00", 3)); // matches (95 <= 100)
            addToAskTree(askTree, ask(CLIENT_C, "105.00", 3)); // does NOT match (105 > 100)
            var bidOrder = bid(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(bidOrder, askTree);
            assertThat(results).hasSize(1);
            assertThat(bidOrder.getVolume()).isEqualTo(BigInteger.valueOf(7));
            assertThat(totalOrdersInTree(askTree)).isEqualTo(1); // 105.00 ask untouched
        }
    }


}