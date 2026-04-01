# Observability Walkthrough — Metrics Verification

This guide allows you to verify that the matching engine's custom metrics
are emitting correctly, end to end from order submission to Grafana.

---

## Step 1 — Start the stack

```bash
git clone https://github.com/tonmoy-b/Order-Book-Matching-Engine-Microservices.git
cd Order-Book-Matching-Engine-Microservices
cp .env.example .env          # Mac/Linux
copy .env.example .env      # Windows
docker compose up -d
````

Wait ~20 seconds for Kafka and Postgres to pass their healthchecks.
Watch Docker Desktop to confirm all containers show healthy before proceeding.

---

## Step 2 — Verify zero-baselines before any orders

Open a browser and go to:

```
http://localhost:4001/actuator/prometheus
```

Search the page for `orderbook_orders_received`. You should see:

```
orderbook_orders_received_total{application="matching-engine",side="ask"} 0.0
orderbook_orders_received_total{application="matching-engine",side="bid"} 0.0
```

These zeros confirm pre-registration is working — the metrics exist before
any order has been submitted. 
Which helps grafana since without this pre-registration, Grafana dashboards
show "no data" gaps rather than a clean zero baseline.

---

## Step 3 — Submit orders via the frontend

```bash
cd frontend
npm install          # first time only
npm run dev
```

Open `http://localhost:3001` and submit at least one BID and one ASK for
the same asset at the same price so a match occurs. Submit a second pair
to generate multiple fills.

---

## Step 4 — Verify metrics are non-zero

Refresh `http://localhost:4001/actuator/prometheus` and search for each metric:

| Metric | What to expect |
|---|---|
| `orderbook_orders_received_total` | 1.0+ per side, tagged `side="bid"` / `side="ask"` |
| `orderbook_match_duration_seconds` | p50/p95/p99 latency buckets populated |
| `orderbook_fills_total` | Incremented per fill, tagged by `fill_type` |
| `orderbook_fill_volume_total` | Distribution summary of fill volumes |
| `orderbook_persist_duration_seconds` | MongoDB write latency buckets populated |
| `orderbook_persist_failures_total` | Should be `0` — confirms MongoDB is healthy |

---

## Step 5 — View in Grafana

```
http://localhost:3000
```

Login: `admin` / `admin`

Navigate to **Explore**, select **Prometheus** as the data source, and run:

```promql
# Orders received per side
rate(orderbook_orders_received_total[1m])

# p99 matching latency in milliseconds
histogram_quantile(0.99,
rate(orderbook_match_duration_seconds_bucket[1m])
) * 1000

# p99 MongoDB persist latency in milliseconds
histogram_quantile(0.99,
rate(orderbook_persist_duration_seconds_bucket[1m])
) * 1000

# Fill rate by type
rate(orderbook_fills_total[1m])
```

---

## Step 6 — Trigger a persist failure (optional)

To verify the failure counter works, stop MongoDB while the app is running:

```bash
docker stop mongodb
```

Submit an order that produces a match. Refresh the Prometheus endpoint
and confirm `orderbook_persist_failures_total` has incremented above zero.
Restore MongoDB:

```bash
docker start mongodb
```

This confirms the failure path is instrumented correctly and the circuit
breaker and persistence failure metrics work independently.