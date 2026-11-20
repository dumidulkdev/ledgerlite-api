# LedgerLite API

LedgerLite is a small digital wallet backend that keeps money movement safe under retries, concurrent requests, and strict accounting rules. Transfers are idempotent, wallet balances cannot go negative, every movement is recorded in a balanced double-entry ledger, and amounts stay exact with two decimal places.

## Features

- Wallet creation and paginated listing with current balance
- Deposits and withdrawals with optional references
- Same-currency wallet transfers in a single atomic transaction
- Required `Idempotency-Key` on every money-moving POST
- Double-entry ledger with a system cash account for deposits and withdrawals
- Optimistic locking and configurable retries on concurrent updates
- Paginated ledger history and date-range statements
- Ledger integrity verification endpoint
- After-commit async transfer notifications

## Tech Stack

- Java 21
- Spring Boot 3.4
- Spring Web, Spring Data JPA, Bean Validation
- PostgreSQL with Flyway (H2 in PostgreSQL mode for local runs and tests)
- springdoc-openapi (Swagger UI)
- Spring Retry and Spring async application events
- JUnit 5, Mockito, Spring Boot Test, MockMvc
- Docker and Docker Compose

## Running Locally

### Maven (default in-memory H2 profile)

```bash
mvn spring-boot:run
```

The API listens on port `8080`. Run the test suite with:

```bash
mvn verify
```

### Docker Compose (PostgreSQL)

```bash
mvn -DskipTests package
docker compose up --build
```

### Environment Variables (Docker profile)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Activates PostgreSQL datasource settings |
| `DB_HOST` | `postgres` | Database host |
| `DB_PORT` | `5432` | Database port |
| `DB_NAME` | `ledgerlite` | Database name |
| `DB_USER` | `ledgerlite` | Database user |
| `DB_PASSWORD` | `ledgerlite` | Database password |

Interactive API docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/wallets` | Create a wallet |
| GET | `/api/wallets/{id}` | Get wallet with balance |
| GET | `/api/wallets` | List wallets (paged, sortable) |
| POST | `/api/wallets/{id}/deposits` | Deposit funds (`Idempotency-Key` required) |
| POST | `/api/wallets/{id}/withdrawals` | Withdraw funds (`Idempotency-Key` required) |
| POST | `/api/transfers` | Transfer between wallets (`Idempotency-Key` required) |
| GET | `/api/transfers/{id}` | Get transfer |
| GET | `/api/wallets/{id}/ledger` | Ledger history, newest first |
| GET | `/api/wallets/{id}/statement` | Statement for `from` / `to` instants |
| GET | `/api/admin/ledger/verify` | Integrity check |
| GET | `/api/notifications` | Async notification log |

## Idempotency

Every deposit, withdrawal, and transfer requires an `Idempotency-Key` header.

- Missing header → `400 Bad Request`
- Same key and same JSON body → original response replayed; money is not moved again
- Same key and different body → `409 Conflict`
- Keys are stored with a hash of the request body and the serialized response

## Double-Entry Ledger

Each operation creates one ledger transaction with balanced `DEBIT` and `CREDIT` entries:

- **Deposit** — credit the wallet, debit system cash
- **Withdrawal** — debit the wallet, credit system cash
- **Transfer** — debit source wallet, credit target wallet

Wallet balances are updated in the same database transaction as the ledger writes. The integrity endpoint confirms that each wallet balance matches its ledger entries and that every transaction nets to zero.

## Concurrency

Wallets use JPA `@Version` optimistic locking. `OptimisticLockingFailureException` is retried with Spring Retry (`ledgerlite.retry.max-attempts`, `initial-backoff-ms`, `multiplier` in `application.yml`). Parallel transfers are covered by an automated test that checks conservation of total funds and non-negative balances.

## Example Requests

Create two wallets:

```bash
curl -s -X POST http://localhost:8080/api/wallets \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Alex Kim","currency":"USD"}'

curl -s -X POST http://localhost:8080/api/wallets \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Jordan Lee","currency":"USD"}'
```

Deposit into the first wallet:

```bash
curl -s -X POST http://localhost:8080/api/wallets/{walletId}/deposits \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: deposit-demo-001' \
  -d '{"amount":100.00,"reference":"Opening balance"}'
```

Transfer with idempotency (repeat the same call to get the same response):

```bash
curl -s -X POST http://localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-demo-001' \
  -d '{"sourceWalletId":"...","targetWalletId":"...","amount":25.00,"reference":"Rent"}'
```

Reuse the key with a different body (expect `409`):

```bash
curl -s -X POST http://localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-demo-001' \
  -d '{"sourceWalletId":"...","targetWalletId":"...","amount":30.00,"reference":"Rent"}'
```

Overdraft (expect `422`):

```bash
curl -s -X POST http://localhost:8080/api/wallets/{walletId}/withdrawals \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: withdraw-demo-001' \
  -d '{"amount":9999.00,"reference":"Too much"}'
```

Ledger, statement, and integrity:

```bash
curl -s "http://localhost:8080/api/wallets/{walletId}/ledger?page=0&size=20"
curl -s "http://localhost:8080/api/wallets/{walletId}/statement?from=2020-01-01T00:00:00Z&to=2030-01-01T00:00:00Z"
curl -s http://localhost:8080/api/admin/ledger/verify
```

## Configuration

| Property | Default | Purpose |
|----------|---------|---------|
| `ledgerlite.retry.max-attempts` | `5` | Optimistic lock retry attempts |
| `ledgerlite.retry.initial-backoff-ms` | `50` | Initial backoff between retries |
| `ledgerlite.retry.multiplier` | `2.0` | Exponential backoff multiplier |
| `spring.datasource.url` | in-memory H2 | Local database |
| `server.port` | `8080` | HTTP port |

## Project Structure

```
src/main/java/com/ledgerlite/
  config/          Spring configuration (clock, async, retry, OpenAPI, JPA auditing)
  controller/      REST controllers
  domain/          JPA entities and enums
  dto/             Request/response records
  event/           Domain events and async listeners
  exception/       API exceptions and global handler
  mapper/          Entity-to-DTO mappers
  repository/      Spring Data repositories
  service/         Business logic (wallets, ledger, idempotency, statements)
src/main/resources/db/migration/   Flyway SQL migrations
src/test/java/                     Unit, web, integration, and concurrency tests
```

## What I Learned

- **Idempotency design** — reservation rows, request hashing, and stored responses make retries safe without double-spend.
- **Transaction boundaries** — balance updates and ledger rows commit together so partial failures cannot corrupt state.
- **Optimistic locking with retries** — version columns plus bounded retries handle concurrent transfers without long-lived locks.
- **Double-entry bookkeeping** — paired debits and credits keep every transaction balanced and auditable.
- **BigDecimal money handling** — scale-2 `HALF_EVEN` rounding avoids float drift and rejects invalid precisions.
- **After-commit async events** — `@TransactionalEventListener(AFTER_COMMIT)` with `@Async` decouples notifications from request latency.

## License

[MIT License](LICENSE). Developed by Dumidu Lakshan.