# Credit Decision Engine

Specialized FinTech microservice for automated consumer credit decisioning built with Java 21 and Spring Boot 4.

The service operates as an internal **Decision Engine** invoked by an upstream Loan Origination System (LOS / Core Banking). It accepts a normalized borrower financial and credit bureau profile, executes a deterministic 5-stage evaluation pipeline for an annuity consumer loan, and returns an explainable verdict (`APPROVED`, `COUNTER_OFFER` with an alternative offer matrix, or `REJECTED`) while recording an immutable audit snapshot in its own dedicated PostgreSQL database (`Database per Service`).

---

## Decision Pipeline Architecture

```text
Upstream Core Backend (LOS)
   │  POST /api/decisions (borrower profile + credit bureau metrics + requested loan terms)
   ▼
[Idempotency Guard] ──(duplicate requestId)──► Return previously recorded decision snapshot
   │
   ▼
[Decision Pipeline]
   ├── 1. HardStopEvaluator       → Age boundaries (21–65), active delinquency, 12m delinquency > 30d, score < 520
   ├── 2. RiskGradeResolver       → Risk Grade A/B/C/D by creditScore, individualized APR (16%–32%) & exposure cap
   ├── 3. AffordabilityCalculator → Budget = min(Income × 50% − ExistingDebt, Income − SubsistenceMin − ExistingDebt)
   ├── 4. AnnuityLoanCalculator   → Exact annuity schedule (DECIMAL128 / HALF_EVEN) & reverse capacity (10,000 KZT step)
   └── 5. Smart Offer Matrix      → APPROVED / COUNTER_OFFER (reduced amount OR extended term) / REJECTED
   │
   ▼
[PostgreSQL 16] ──► Immutable audit snapshot of inputs, pricing, solvency metrics, reasons (JSONB), and offers (JSONB)
```

Detailed mathematical formulas, risk-grade pricing tiers, and API contracts are documented in the [Technical Design](docs/design.md), and the engineering stages are outlined in the [Development Plan](docs/development-plan.md).

---

## Tech Stack

Java 21 · Spring Boot 4.1.1 · Spring Web MVC · Jakarta Bean Validation · Spring Data JPA / Hibernate 7 · PostgreSQL 16 (`JSONB`) · Flyway · Maven Wrapper · JUnit 5 / Mockito · Testcontainers · Docker / Docker Compose · GitHub Actions.

---

## Quick Start: Full Stack in Docker (`app + postgres`)

You can build and run both PostgreSQL and the Spring Boot application using Docker Compose (no local JDK installation required):

1. Create a local `.env` file from the template and set `DB_PASSWORD`:
   ```bash
   cp .env.example .env
   ```

2. Build and start the containers:
   ```bash
   docker compose up -d --build
   ```

   Docker Compose will automatically:
   - start `postgres:16-alpine` and wait for its `pg_isready` healthcheck;
   - build the multi-stage application image (`eclipse-temurin:21-jre-alpine`, running as non-root `appuser`);
   - apply Flyway migrations (`V1`–`V4`) and expose the REST API at `http://localhost:8080`.

Stop the containers while preserving database data in the `postgres_data` volume:
```bash
docker compose down
```

---

## Local Development (IDE / Maven)

To run the application directly on your host machine while keeping PostgreSQL in Docker:

1. Start only the PostgreSQL container:
   ```bash
   docker compose up -d --wait postgres
   ```

2. Run the application with the Maven Wrapper (requires JDK 21):
   ```bash
   ./mvnw spring-boot:run
   ```

### Environment Variables (`.env`)

| Variable | Default / Description |
|---|---|
| `DB_NAME` | `credit_decision` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | Database password (set in `.env` or environment) |
| `DB_PORT` | `5432` (PostgreSQL host port bound to `127.0.0.1`) |
| `APP_PORT` | `8080` (Application host port bound to `127.0.0.1` in Docker Compose) |
| `DB_URL` | Optional full JDBC URL override |

---

## REST API (`/api/decisions`)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/decisions` | Evaluate borrower profile and idempotently persist the credit decision (`200 OK`) |
| `GET` | `/api/decisions/{uuid}` | Retrieve a recorded credit decision by internal UUID (`200 OK` / `404 Not Found`) |
| `GET` | `/api/decisions?page=0&size=20` | Retrieve paginated decision history ordered by `createdAt DESC, id DESC` (`200 OK`) |

Ready-to-run HTTP requests covering direct approval, smart counter-offer matrices, hard-stop rejections, idempotency replay, pagination, and validation errors are available in [requests.http](requests.http).

---

## Testing & Verification

Run fast domain and service unit tests (no Docker required):
```bash
./mvnw -Dtest='*EvaluatorTest,*ResolverTest,*CalculatorTest,*PipelineTest,*ServiceTest' test
```

Run the full verification suite (36 tests, including the Spring Boot context integration test and Flyway `V1`–`V4` migrations against an ephemeral PostgreSQL 16 container via Testcontainers):
```bash
./mvnw clean verify
```

---

## Documentation

- [Technical Design](docs/design.md) — pipeline architecture, risk-grade pricing tiers, annuity and DTI math, database schema, and JSON payloads.
- [Development Plan](docs/development-plan.md) — implementation roadmap and verification criteria.

---

## License

This project is licensed under the [MIT License](LICENSE).
