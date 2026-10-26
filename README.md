# Digital Banking Application

[![CI](https://github.com/VitaliyProgrammer/digital-banking-application/actions/workflows/ci.yml/badge.svg)](https://github.com/VitaliyProgrammer/digital-banking-application/actions/workflows/ci.yml)

Back-end of a digital bank: double-entry ledger, idempotent payments, bill payments, credit engine.

**Stack:** Java 25 · Spring Boot 4.1 · PostgreSQL 17 · Flyway · Spring Modulith · Testcontainers

> Work in progress. The architecture overview and benchmarks will be added as the project grows.

## Run locally

Requirements: JDK 25, Docker.

```bash
docker compose up -d          # start PostgreSQL
./mvnw spring-boot:run        # start the application
```

Health check: <http://localhost:8080/actuator/health>

## Tests

```bash
./mvnw verify                 # unit + integration tests (needs Docker)
```
