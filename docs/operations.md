# 📈 Operations

![docker](https://img.shields.io/badge/image-multi--stage%20·%20layered%20·%20non--root-2496ED?logo=docker&logoColor=white)
![prometheus](https://img.shields.io/badge/metrics-Prometheus-E6522C?logo=prometheus&logoColor=white)
![grafana](https://img.shields.io/badge/dashboard-Grafana-F46800?logo=grafana&logoColor=white)

> **In one sentence:** one command runs the whole bank with its infrastructure and monitoring, and the dashboard shows not only "is the server alive" but "is the bank working".

← [Back to README](../README.md)

---

## 🐳 Running it

```bash
docker compose up -d                                   # infrastructure only: run the bank from the IDE
docker compose --profile app up -d --build             # + the bank, Prometheus, Grafana
docker compose --profile load run --rm k6 run /scripts/load.js   # + a load test
docker compose --profile app down                      # stop (the database volume is kept)
```

| Service | Port | Notes |
|---|---|---|
| 🏦 app | 8080 | the API; health and metrics on 8081, **not published** |
| 🐘 postgres | 5432 | data in the `postgres-data` volume |
| ⚡ redis | 6379 | attempt counters |
| 🐇 rabbitmq | 5672, UI 15672 | `guest` / `guest` locally |
| 📧 mailpit | SMTP 1025, UI 8025 | catches every email the bank sends |
| 📈 prometheus | 9090 | scrapes the app every 5 s |
| 📊 grafana | 3000 | anonymous viewer locally, dashboard provisioned |

## 📦 The image

| Decision | Why |
|---|---|
| 🔹 Multi-stage: build on `eclipse-temurin:25-jdk`, run on `25-jre-alpine` | no JDK, Maven or source code in the final image: smaller, fewer vulnerabilities |
| 🔹 Layered jar, one `COPY` per layer | a code change rebuilds only the thin `application` layer; dependencies come from the cache |
| 🔹 `--mount=type=cache` for `~/.m2` | Maven does not download the internet on every build |
| 🔹 Non-root user `bank` | whoever breaks into the application is not root in the container |
| 🔹 `-XX:MaxRAMPercentage=75` | the heap follows the container's memory limit, not the whole machine's |
| 🔹 `HEALTHCHECK` on `/actuator/health` | "running" is not "ready"; compose and orchestrators ask the application itself |
| 🔹 `exec java …` as PID 1 | the JVM gets SIGTERM directly and Spring shuts down gracefully |

### 🚚 Publishing

| What | When | Where |
|---|---|---|
| 🐳 Docker image for `amd64` and `arm64` | every merge into `main` ([`docker.yml`](../.github/workflows/docker.yml)) | `ghcr.io/vitaliyprogrammer/digital-banking-application:latest` and `:sha-…` |

The jar inside is built once, natively; only the thin runtime layer is per architecture, so the arm64 image costs no emulated Maven build. [`docker-compose.demo.yml`](../docker-compose.demo.yml) runs the published image with its infrastructure and needs no file from the repository.

## ⚙️ Configuration

Everything that differs between environments is an environment variable with a local default. Defaults are for local development only; secrets **must** be set elsewhere.

| Variable | Default | What |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `localhost`, `5432`, `digital_bank`, `bank`, `bank` | PostgreSQL |
| `REDIS_HOST`, `RABBITMQ_HOST`, `MAIL_HOST` | `localhost` | the other servers |
| `JWT_SECRET` | a local-only value | **must** be set: 32+ characters |
| `CARD_ENCRYPTION_KEY`, `CARD_LOOKUP_KEY`, `CARD_CVV_KEY`, `CARD_PIN_KEY` | local-only keys | **must** be set: four separate 256-bit keys |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | empty | the first admin; without them there is none |
| `MANAGEMENT_PORT` | `8081` | health and metrics |
| `OUTBOX_RELAY_ENABLED`, `NOTIFICATIONS_*_ENABLED`, `SAVINGS_JOBS_ENABLED`, `CASHBACK_JOBS_ENABLED`, `CREDIT_LINE_JOBS_ENABLED`, `EXCHANGE_RATES_SYNC_ENABLED` | `true` | background jobs; tests switch them off and run them by hand |

Business numbers (fees, rates, credit rules, limits, retry delays) live in [`application.yml`](../src/main/resources/application.yml) under `banking.*`, are validated at startup, and are never hard-coded.

### ⏰ Scheduled jobs (Kyiv time)

| When | Job |
|---|---|
| every hour at :05 | NBU rates (a day already stored is skipped) |
| 00:10 daily | savings interest accrual |
| 1st, 01:00 | savings interest payout |
| 1st, 01:30 | cashback payout |
| 1st, 02:00 | credit statements: settle due ones, close last month |
| every 1 s | outbox relay |
| every 2 s / 10 s | bill payments: send / reconcile |
| every 10 s | notification emails |

Every job is safe to run twice or on two instances: unique keys, row locks or `SKIP LOCKED` make sure nothing happens twice.

## 📊 Metrics and the dashboard

`/actuator/prometheus` on the management port. Besides the JVM, HTTP and HikariCP metrics, the bank publishes its own:

| Metric | Type | Labels |
|---|---|---|
| `bank_payments_total` | counter, **after commit** | `type`, `result` (completed / failed) |
| `bank_credit_applications_total` | counter | `outcome` of scoring |
| `bank_credit_decisions_total` | counter | `status`, `source` (auto / manual) |
| `bank_support_tickets_total` | counter | `event` (opened / resolved) |
| `bank_outbox_unpublished`, `bank_outbox_oldest_unpublished_age_seconds` | gauges | |
| `bank_notifications_emails_pending`, `bank_notifications_dead_letters` | gauges | |

Counters move **only after the transaction commits**: a payment that rolled back did not happen, so it is not on the graph either. Labels hold only small fixed sets (types, statuses); a user id as a label would create a time series per client.

**Grafana → Digital Bank — overview** (provisioned, nothing to click):

| Row | Panels |
|---|---|
| 🌐 API | requests/s by endpoint, **p95 / p99** latency, % of 5xx, p95 by endpoint, 4xx by status |
| 💸 Business | payments per minute by type, failed bill payments, credit decisions, tickets |
| 📨 Events | outbox backlog and age, pending emails, dead letters, with green / yellow / red thresholds |
| ⚙️ JVM and database | heap, CPU, HikariCP active / pending / max, threads |
| 🧪 k6 | virtual users, k6 requests/s, latency as the client sees it |

💡 The HikariCP **pending** panel is what showed the [hot-account bottleneck](../load-tests/README.md): 0% errors, yet up to 23 requests waiting for a connection.
