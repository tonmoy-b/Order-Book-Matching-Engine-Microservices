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

class AskMatchingStrategyTest {
    private AskMatchingStrategy strategy;
    private TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTree;

    @BeforeEach
    void setup() {
        strategy = new AskMatchingStrategy();
        bidTree = emptyBidTree();
    }

    //Scenarios: No Match
    @Nested
    @DisplayName("No fills")
    class NoFills {
        @Test
        @DisplayName("Empty bid tree → no fills, full volume remains")
        void emptyBidTree_noFills() {
            var ask = ask(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).isEmpty();
            assertThat(ask.getVolume()).isEqualTo(BigInteger.valueOf(10));
        }

        @Test
        @DisplayName("Bid price below ask price → no match")
        void bidPriceBelowAsk_noFills() {
            addToBidTree(bidTree, bid(CLIENT_B, "99.00", 10));  // bid=99, ask=100 → no match
            var ask = ask(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).isEmpty();
            assertThat(ask.getVolume()).isEqualTo(BigInteger.valueOf(10));
            assertThat(totalOrdersInTree(bidTree)).isEqualTo(1); // bid untouched
        }

        @Test
        @DisplayName("Same client bid and ask → self-match prevented")
        void sameClient_selfMatchPrevented() {
            addToBidTree(bidTree, bid(CLIENT_A, "100.00", 10)); // same client as ask
            var ask = ask(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).isEmpty();
            assertThat(ask.getVolume()).isEqualTo(BigInteger.valueOf(10));
            assertThat(totalOrdersInTree(bidTree)).isEqualTo(1); // self-bid still in tree
        }
    }

    //Scenarios: Exact match for full fills
    @Nested
    @DisplayName("Exact fills")
    class ExactFills {
        @Test
        @DisplayName("Ask volume == bid volume → EXACT fill, both consumed")
        void exactVolumeMatch() {
            addToBidTree(bidTree, bid(CLIENT_B, "100.00", 10));
            var ask = ask(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(1);
            MatchResult fill = results.get(0);
            assertThat(fill.getFillType()).isEqualTo(FillType.EXACT_FULL);
            assertThat(fill.getFillVolume()).isEqualTo(BigInteger.valueOf(10));
            assertThat(fill.getSpread()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(ask.getVolume()).isEqualTo(BigInteger.ZERO);
            assertThat(totalOrdersInTree(bidTree)).isEqualTo(0);
        }

        @Test
        @DisplayName("Bid price above ask → match succeeds, spread is positive")
        void bidAboveAsk_positiveSpread() {
            addToBidTree(bidTree, bid(CLIENT_B, "105.00", 5));
            var ask = ask(CLIENT_A, "100.00", 5);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSpread()).isEqualByComparingTo(new BigDecimal("5.00"));
        }
    }

    //Scenarios: partial fills - incoming smaller
    @Nested
    @DisplayName("Incoming ASK partially fills (ask volume < bid volume)")
    class IncomingPartial {
        @Test
        @DisplayName("Ask volume < bid volume → INCOMING_PARTIAL, bid partially remains in tree")
        void askSmallerThanBid_incomingPartial() {
            var existingBid = bid(CLIENT_B, "100.00", 20);
            addToBidTree(bidTree, existingBid);
            var ask = ask(CLIENT_A, "100.00", 10); // only needs 10, bid has 20
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(1);
            MatchResult fill = results.get(0);
            assertThat(fill.getFillType()).isEqualTo(FillType.INCOMING_PARTIAL);
            assertThat(fill.getFillVolume()).isEqualTo(BigInteger.valueOf(10));
            // Ask fully consumed
            assertThat(ask.getVolume()).isEqualTo(BigInteger.ZERO);
            // Bid partially remains — 10 still in tree
            assertThat(totalOrdersInTree(bidTree)).isEqualTo(1);
            assertThat(existingBid.getVolume()).isEqualTo(BigInteger.valueOf(10));
        }
    }

    //Scenarios: partial fills - counterparty smaller
    @Nested
    @DisplayName("Counterparty BID partially fills (ask volume > bid volume)")
    class CounterpartyPartial {
        @Test
        @DisplayName("Ask volume > bid volume → COUNTERPARTY_PARTIAL, residual volume remains on ask")
        void askLargerThanBid_counterpartyPartial() {
            addToBidTree(bidTree, bid(CLIENT_B, "100.00", 5));
            var ask = ask(CLIENT_A, "100.00", 20); // needs 20, bid only has 5
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFillType()).isEqualTo(FillType.COUNTERPARTY_PARTIAL);
            assertThat(results.get(0).getFillVolume()).isEqualTo(BigInteger.valueOf(5));
            // Ask has 15 remaining
            assertThat(ask.getVolume()).isEqualTo(BigInteger.valueOf(15));
            // Bid fully consumed
            assertThat(totalOrdersInTree(bidTree)).isEqualTo(0);
        }

        @Test
        @DisplayName("Bug regression: pendingVolume calculation correct (was subtract(pendingVolume) in original)")
        void pendingVolumeCalculation_isCorrect() {
            // ask=20, bid=5 → after fill, pending should be 20-5=15, NOT 5-20=-15
            addToBidTree(bidTree, bid(CLIENT_B, "100.00", 5));
            var ask = ask(CLIENT_A, "100.00", 20);
            strategy.match(ask, bidTree);
            assertThat(ask.getVolume()).isEqualTo(BigInteger.valueOf(15));
            assertThat(ask.getVolume().compareTo(BigInteger.ZERO)).isGreaterThan(0); // must be positive
        }
    }

    //Scenarios: across multiple levels and orders
    @Nested
    @DisplayName("Multi-order and multi-price-level matching")
    class MultiOrder {
        @Test
        @DisplayName("Ask drains multiple bids at same price level in time order")
        void multipleOrdersSamePriceLevel_drainsInOrder() {
            LocalDateTime t1 = LocalDateTime.now().minusMinutes(2);
            LocalDateTime t2 = LocalDateTime.now().minusMinutes(1);
            var bid1 = bid(CLIENT_B, "100.00", 5, t1); // older — served first
            var bid2 = bid(CLIENT_C, "100.00", 5, t2);
            addToBidTree(bidTree, bid1);
            addToBidTree(bidTree, bid2);
            var ask = ask(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMatchedOrder().getOrderId()).isEqualTo(bid1.getOrderId());
            assertThat(results.get(1).getMatchedOrder().getOrderId()).isEqualTo(bid2.getOrderId());
            assertThat(ask.getVolume()).isEqualTo(BigInteger.ZERO);
        }

        @Test
        @DisplayName("Ask matches across multiple price levels, best bid first")
        void multiplePriceLevels_bestBidFirst() {
            // BID tree is highest-first — 105 should be matched before 100
            addToBidTree(bidTree, bid(CLIENT_B, "105.00", 5));
            addToBidTree(bidTree, bid(CLIENT_C, "100.00", 5));
            var ask = ask(CLIENT_A, "99.00", 10);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMatchedOrder().getAmount())
                    .isEqualByComparingTo(new BigDecimal("105.00")); // highest matched first
            assertThat(ask.getVolume()).isEqualTo(BigInteger.ZERO);
        }

        @Test
        @DisplayName("Ask stops matching when bid price goes below ask price")
        void stopsMatchingAtPriceBoundary() {
            addToBidTree(bidTree, bid(CLIENT_B, "105.00", 3)); // matches (105 >= 100)
            addToBidTree(bidTree, bid(CLIENT_C, "95.00", 3)); // does NOT match (95 < 100)
            var ask = ask(CLIENT_A, "100.00", 10);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(1);
            assertThat(ask.getVolume()).isEqualTo(BigInteger.valueOf(7)); // only 3 filled
            assertThat(totalOrdersInTree(bidTree)).isEqualTo(1); // 95.00 bid untouched
        }

        @Test
        @DisplayName("Self-match skipped, other client is at same price level still matches")
        void selfMatchSkipped_otherClientMatches() {
            addToBidTree(bidTree, bid(CLIENT_A, "100.00", 5)); // same client — skip
            addToBidTree(bidTree, bid(CLIENT_B, "100.00", 5)); // different — match
            var ask = ask(CLIENT_A, "100.00", 5);
            List<MatchResult> results = strategy.match(ask, bidTree);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMatchedOrder().getClientId()).isEqualTo(CLIENT_B);
        }
    }


}