# 🧪 Load tests (k6)

![k6](https://img.shields.io/badge/k6-1.1-7D64FF?logo=k6&logoColor=white)
![scenarios](https://img.shields.io/badge/scenarios-3-blue)
![money check](https://img.shields.io/badge/ledger%20under%20load-exact%20to%20the%20kopiyka-success)

Three scenarios, each with its own question. k6 runs as a container in the same Docker network as the bank, so nothing has to be installed.

| Scenario | Load | Question it answers |
|---|---|---|
| 💨 [`smoke.js`](smoke.js) | 1 virtual user, 30 s | Does the main path work at all? Run it before anything bigger |
| 📈 [`load.js`](load.js) | 0 → 40 users in 1 min, 3 min at 40, ramp-down | How does a normal busy day feel: p95, errors, database connections |
| 🔥 [`contention.js`](contention.js) | 30 users on **one** account for 1 min, every request sent twice | Under the worst race: does the balance stay ≥ 0, does a retry never pay twice, does every kopiyka add up |

## ▶️ Run

```bash
docker compose --profile app up -d --build                     # the bank, Prometheus and Grafana
docker compose --profile load run --rm k6 run /scripts/smoke.js
docker compose --profile load run --rm k6 run --summary-export=/results/load.json /scripts/load.js
docker compose --profile load run --rm k6 run /scripts/contention.js
```

While a test runs, watch **Grafana → Digital Bank — overview** at http://localhost:3000: the 🧪 k6 row shows the virtual users, the rows above show how the bank reacts. A failed threshold makes k6 exit with code 99, so the same scripts can gate a release in CI.

## 📊 Results

Laptop: 4 CPUs for Docker, shared by k6, the bank, PostgreSQL and RabbitMQ. Numbers are for a **warmed-up** JVM (the first run after a start also measures JIT compilation).

### 🔍 A bottleneck found by k6: the hot fee account

The first `load.js` runs failed their thresholds with **0 errors**, but slow transfers and up to 23 requests waiting for a database connection. PostgreSQL showed why:

```sql
-- which account rows are transactions queuing for, during the load
FEE_INCOME  BANK-FEE-INCOME-UAH   ← 3–6 transactions waiting at any moment
```

Every transfer with a fee locks the bank's **one** fee income row: 40 clients with different accounts still queued behind it, each holding a connection, so even plain reads waited for the pool. Fix ([`V22`](../src/main/resources/db/migration/V22__shard_hot_bank_accounts.sql)): the hot bank accounts (fee income, settlement, hold) are split into **16 shards** each, and a posting takes a random one. After it, no transaction waits for an account row.

| `load.js`, 40 users | Before | After | |
|---|---|---|---|
| p95, all requests | 439 ms | **270 ms** | −38% |
| p95, transfers | 890 ms ❌ | **483 ms** ✅ | −46% |
| p99, all requests | 1.08 s | **591 ms** | −45% |
| Throughput | 67 req/s | **72 req/s** | +8% |
| Failed requests | 0% | 0% | |
| Thresholds | ❌ | ✅ all green | |

### 🔥 Contention: 30 users, one account

| Check | Result |
|---|---|
| Transfers that went through | **1 000**, then every next one: `422 Insufficient funds` |
| Sender: 15 000.00 − 1 000 × (10.00 + 5.00 fee) | **0.00** ✅ |
| Recipient | **10 000.00** ✅ |
| A retry with the same `Idempotency-Key` returns the same payment | **100%** ✅ |
| Failed requests (anything but 201 / 422) | **0** ✅ |

## 💡 Lessons

- **Measure again after a fix.** The first run after the change looked *worse*: it was a cold JVM. Compare warm with warm.
- **Errors 0% is not "fine".** The bank never failed, it was just queuing: only percentiles and the connection pool showed it.
- **More connections would not have helped.** A bigger pool only makes a longer queue behind the same row; the cure was to remove the shared row.
- **The load generator competes with the system** when both run on one laptop. Past this point the limit is the hardware, not the code.
