package com.tbhatta.matchingengine.service.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class OrderBookMetrics {
    private final MeterRegistry registry;

    public OrderBookMetrics(MeterRegistry registry) {
        this.registry = registry;

        String[] sides = new String[]{"bid", "ask"};
        for (String side : sides) {
            Counter.builder("orderbook.orders.received")
                    .description("Total orders received by the matching engine")
                    .tag("side", side)
                    .register(registry);

            Counter.builder("orderbook.fills.total")
                    .description("Total fills executed")
                    .tag("side", side)
                    .tag("fill_type", "EXACT")
                    .register(registry);

            Counter.builder("orderbook.fills.total")
                    .tag("side", side)
                    .tag("fill_type", "INCOMING_PARTIAL")
                    .register(registry);

            Counter.builder("orderbook.fills.total")
                    .tag("side", side)
                    .tag("fill_type", "COUNTERPARTY_PARTIAL")
                    .register(registry);

            Counter.builder("orderbook.persist.failures")
                    .description("MongoDB persistence failures")
                    .tag("side", side)
                    .register(registry);

            Timer.builder("orderbook.match.duration")
                    .description("Time to execute matching algorithm")
                    .tag("side", side)
                    .publishPercentiles(0.50, 0.95, 0.99)
                    .register(registry);

            Timer.builder("orderbook.persist.duration")
                    .description("Time to persist transaction to MongoDB")
                    .tag("side", side)
                    .publishPercentiles(0.50, 0.95, 0.99)
                    .register(registry);

            DistributionSummary.builder("orderbook.fill.volume.total")
                    .description("Volume of each fill")
                    .tag("side", side)
                    .register(registry);
        }
    }

    public void recordOrderReceived(String side) {
        registry.counter("orderbook.orders.received", "side", side.strip().toLowerCase()).increment();
    }

    public void recordFill(String side, String fillType, double volume) {
        registry.counter("orderbook.fills.total",
                "side", side, "fill_type", fillType).increment();
        registry.summary("orderbook.fill.volume.total",
                "side", side).record(volume);
    }

    public void recordPersistFailure(String side) {
        registry.counter("orderbook.persist.failures", "side", side).increment();
    }

    public Timer matchTimer(String side) {
        return registry.timer("orderbook.match.duration", "side", side.strip().toLowerCase());
    }

    public Timer persistTimer(String side) {
        return registry.timer("orderbook.persist.duration", "side", side);
    }
}
