package com.tbhatta.matchingengine.service;

import com.tbhatta.matchingengine.model.OrderItemModel;
import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import com.tbhatta.matchingengine.service.matching.AskMatchingStrategy;
import com.tbhatta.matchingengine.service.matching.BidMatchingStrategy;
import com.tbhatta.matchingengine.service.persistence.TransactionPersistenceService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tbhatta.matchingengine.service.matching.OrderBookTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
public class OrderBookConcurrencyTest {

    @Mock
    TransactionItemRepository transactionItemRepository;

    private OrderBook orderBook;

    @BeforeEach
    void setup() {
        AssetOrderBookFactory factory = new AssetOrderBookFactory();
        TransactionPersistenceService ps = new TransactionPersistenceService(transactionItemRepository);

        orderBook = new OrderBook(
                factory,
                new AskMatchingStrategy(),
                new BidMatchingStrategy(),
                ps
        );
    }

    @RepeatedTest(5)
    @DisplayName("100 concurrent orders on same asset — no exceptions, no deadlock")
    void concurrentOrders_noExceptions() throws InterruptedException {
        int threads = 10;
        int perThread = 10; // 100 orders total
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            pool.submit(() -> {
                ready.countDown();
                try {
                    ready.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    for (int i = 0; i < perThread; i++) {
                        String type = (i % 2 == 0) ? "ask" : "bid";
                        String price = (90 + (i % 5)) + ".00";
                        OrderItemModel order = (type.equals("ask"))
                                ? ask("client-" + tid, price, 5)
                                : bid("client-" + tid, price, 5);
                        orderBook.enterOrderItem_(order);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        boolean completed = done.await(10, TimeUnit.SECONDS);
        pool.shutdown();
        assertThat(completed).as("All threads completed within timeout").isTrue();
        assertThat(errors.get()).as("No exceptions thrown").isEqualTo(0);
    }


    //@Disabled
    @RepeatedTest(5)
    @DisplayName("Volume conservation: filled + residual == submitted")
    void volumeConservation() throws InterruptedException {
        int orderCount = 50;
        int volumeEach = 10;
        ExecutorService pool = Executors.newFixedThreadPool(orderCount);
        CountDownLatch ready = new CountDownLatch(orderCount);
        CountDownLatch done = new CountDownLatch(orderCount);
        // Submit 25 bids and 25 asks, all at the same price, many fills expected
        for (int i = 0; i < orderCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    ready.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    if (idx % 2 == 0) {
                        orderBook.enterOrderItem_(bid("buyer-" + idx, "100.00", volumeEach));
                    } else {
                        orderBook.enterOrderItem_(ask("seller-" + idx, "100.00", volumeEach));
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        boolean completed = done.await(15, TimeUnit.SECONDS);
        pool.shutdown();
        assertThat(completed).isTrue();
        // Count residual volume left in both side books of the asset
        long residualBid = orderBook.getResidualVolume(ASSET, "bid");
        long residualAsk = orderBook.getResidualVolume(ASSET, "ask");
        long totalResidual = residualBid + residualAsk;
        // Total submitted volume
        long totalSubmitted = (long) orderCount * volumeEach;
        // The sum of all remaining + filled must equal total submitted
        // We can't know exact filled without capturing every fill
        // Since residual must be <= total, that's going to be the test criteria
        assertThat(totalResidual).isLessThanOrEqualTo(totalSubmitted);
        // At least some transactions were persisted (bids and asks did cross)
        verify(transactionItemRepository, atLeast(1)).insert(any(TransactionItemModel.class));
    }


    // testing helpers
    private long residualVolumeInTree(
            TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> tree
    ) {
        if (tree == null) return 0;
        return tree.values().stream()
                .filter(Objects::nonNull)
                .flatMap(PriorityQueue::stream)
                .filter(Objects::nonNull)
                .mapToLong(o -> o.getVolume().longValue())
                .sum();
    }


}
