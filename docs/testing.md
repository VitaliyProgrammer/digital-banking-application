# 🧪 Testing

![tests](https://img.shields.io/badge/tests-123%20passing-success)
![real infrastructure](https://img.shields.io/badge/integration-real%20PostgreSQL%20·%20Redis%20·%20RabbitMQ%20·%20SMTP-291A3D)
![ArchUnit](https://img.shields.io/badge/architecture-23%20rules-blueviolet)
![k6](https://img.shields.io/badge/load-k6-7D64FF?logo=k6&logoColor=white)

> **In one sentence:** the tests run against the same kinds of servers as production, they prove the dangerous cases (races, retries, outages), and the architecture itself is a test that fails the build.

← [Back to README](../README.md)

---

## 🔺 What runs where

| Level | Count | Speed | What it covers |
|---|---|---|---|
| 🧮 Unit | 5 classes | milliseconds | pure rules: scoring, credit terms, grace period, backoff, notification texts |
| 🏛️ Architecture | 23 rules | seconds | layers and table ownership ([ArchUnit](../src/test/java/io/github/vitaliyprogrammer/banking/ArchitectureTests.java)) |
| 🐘 Integration | 21 classes | ~3 minutes in all | services and HTTP on real PostgreSQL, Redis, RabbitMQ, an SMTP server and HTTP fakes |
| 🔥 Load | 3 scenarios | minutes, on demand | [k6](../load-tests/README.md) against the running stack |

```bash
./mvnw verify                          # everything except load tests; needs Docker
./mvnw test -Dtest=CreditScoringTests  # one class
```

## 🐳 Real infrastructure, one shared context

| Decision | Why |
|---|---|
| 🔹 Testcontainers: PostgreSQL 17, Redis 8, RabbitMQ 4.1, Mailpit | an H2 database or a mocked broker would not have `FOR UPDATE SKIP LOCKED`, partial indexes, triggers or publisher confirms, which is exactly what we test |
| 🔹 WireMock for bill providers and the NBU | a real HTTP server that can be slow, refuse, or never answer |
| 🔹 **One** Spring context for all integration tests | Spring caches a context by its configuration; anything a class adds (its own properties, port, container) starts a whole second application. So MockMvc, WireMock and the mail server live in the base class [`IntegrationTest`](../src/test/java/io/github/vitaliyprogrammer/banking/IntegrationTest.java) |
| 🔹 Static singleton containers | one set for the whole run instead of one per context (it used to be ~20 containers, which overloaded Docker) |
| 🔹 A mail outage is a Mailpit "chaos" switch (451, try again later) | the same server serves "mail works" and "mail is down", so no second context is needed |
| 🔹 Every test creates its own users and accounts | tests share one database without stepping on each other |

## ⚖️ The ledger is checked after **every** test

The base class runs two invariants after each integration test, whatever it did:

1. every account's balance equals the sum of its ledger entries;
2. the sum of all balances in each currency is **zero**: money was neither created nor lost.

A bug that leaks one kopiyka anywhere fails the next test that runs.

## 🎯 What the tests prove

| Danger | Test |
|---|---|
| Parallel debits overdraw an account | `parallelDebitsNeverOverdrawAnAccount` |
| A → B and B → A deadlock | `oppositeTransfersDoNotDeadlock` |
| Somebody edits ledger history | `databaseRejectsAnyChangeToLedgerHistory` |
| "Pay" pressed twice | `parallelRequestsWithSameKeyCreateOnePayment` |
| A provider times out after taking the money | `timeoutBecomesUnknownAndReconciliationFinishesTheJob` |
| Five workers send the same bill | `acceptedPaymentIsSentOnceEvenWithFiveWorkers` |
| A quote spent twice | `aQuoteIsSpentOnceEvenByFiveParallelRequestsWithDifferentKeys` |
| A credit limit overspent in parallel | `parallelSpendingNeverGoesPastTheLimit` |
| Interest charged twice by a repeated job | `theMinimumPaymentAvoidsTheLateFeeAndInterestIsChargedOnlyOnceOnTheRest` |
| An event about a payment that rolled back | `anEventIsWrittenOnlyTogetherWithItsPayment` |
| A duplicate message makes two notifications | `theSameEventDeliveredTwiceMakesOneNotification` |
| Mail down loses an email | `aMailServerThatIsDownDelaysTheEmailButNeverLosesTheNotification` |
| Two employees take one ticket | `ofSeveralEmployeesTakingTheSameTicketAtOnceExactlyOneWins` |
| A stolen refresh token is replayed | [`AuthenticationFlowIntegrationTests`](../src/test/java/io/github/vitaliyprogrammer/banking/identity/AuthenticationFlowIntegrationTests.java) |
| The audit log is rewritten | `theAuditLogCannotBeRewrittenEvenWithPlainSql` |

Races are tested for real: tasks wait at a start gate (`CountDownLatch`) and are released at the same instant on virtual threads, so they really compete for the same rows.

## 🏛️ Architecture as a test

[`ArchitectureTests`](../src/test/java/io/github/vitaliyprogrammer/banking/ArchitectureTests.java) runs 23 rules on every build. A broken rule fails with the exact class and line:

| Rule | Example |
|---|---|
| Layers only depend downwards | a controller never touches a repository |
| Controllers never return entities | a password hash cannot leak into JSON |
| Only the ledger changes balances | `onlyTheLedgerChangesBalances` |
| Each domain owns its tables | only the payment domain uses `PaymentRepository`; others go through its service |
| Only the outbox relay talks to the broker | no `rabbitTemplate.send()` next to a database change |
| Only the HTTP clients make HTTP calls | the rest of the code sees business outcomes, not `RestClient` |
| Only the credit module opens credit lines | lent money exists only for an approved application |
