# Credit Decision Engine (Credit Decision Platform)

Специализированный FinTech-сервис принятия кредитных решений на Java 21 и Spring Boot 4.

Сервис вызывается доверенным основным бэкендом (Loan Origination System / Core Banking), принимает нормализованный финансовый и демографический профиль заёмщика с данными из кредитного бюро (БКИ), выполняет 5-ступенчатый конвейер оценки (Decision Pipeline) по дифференцированному кредиту и возвращает объяснимое решение (`APPROVED`, `COUNTER_OFFER` с матрицей альтернативных офферов или `REJECTED`) с сохранением полного аудиторского снимка в PostgreSQL.

---

## Целевая архитектура конвейера решений (Decision Pipeline)

```text
Основной бэкенд (LOS)
   │  POST /api/v1/decisions (профиль заёмщика + данные БКИ + параметры кредита)
   ▼
[Idempotency Guard] ──(повтор requestId)──► Возврат сохранённого решения
   │
   ▼
[Decision Pipeline]
   ├── 1. Hard Stop Filter        → Возраст (21–65), активные просрочки, просрочки > 30 дн., балл < 520
   ├── 2. Risk-Based Pricing      → Грейд A/B/C/D по creditScore, индивидуальная ставка (APR) и лимит риска
   ├── 3. Affordability (КДН)     → Бюджет = min(Доход × 50% − Платежи, Доход − Прожиточный минимум − Платежи)
   ├── 4. Differentiated Calc     → Помесячный дифференцированный график, шаг 10 000 ₸, проверка 1-го платежа
   └── 5. Smart Offer Generator   → APPROVED / COUNTER_OFFER (снижение суммы ИЛИ удлинение срока) / REJECTED
   │
   ▼
[PostgreSQL 16] ──► Неизменяемый снимок входов, расчётов, причин и офферов
```

Подробная математическая модель, таблицы грейдов и контракты описаны в [техническом дизайне](docs/design.md), а пошаговый порядок разработки — в [плане реализации](docs/development-plan.md).

---

## Что уже реализовано в коде

- Базовый синхронный REST API (`/api/decisions`): расчёт и сохранение решения в PostgreSQL, чтение по UUID, пагинированная история (`createdAt DESC, id DESC`).
- Отделение модели расчёта (`ScoringInput`, `ScoringResult`, `ScoringRuleResult`, `ScoringRuleCode`) от HTTP DTO.
- Валидация входных данных и централизованная обработка ошибок (`GlobalExceptionHandler`, `ApiError`).
- Управление схемой БД через Flyway (`V1`–`V3`).
- Изолированные unit-тесты и интеграционный тест контекста через Testcontainers (`postgres:16-alpine`).
- CI-пайплайн в GitHub Actions (`./mvnw verify`).

---

## Стек

Java 21 · Spring Boot 4.1.1 · Spring Web MVC · Bean Validation · Spring Data JPA / Hibernate · PostgreSQL 16 · Flyway · Maven Wrapper · JUnit 5 / Mockito · Testcontainers · Docker Compose · GitHub Actions.

---

## Локальный запуск

Потребуются JDK 21 и Docker с Docker Compose. Maven отдельно устанавливать не нужно: в репозитории есть Wrapper (`./mvnw`).

1. Создайте локальный файл настроек:
   ```bash
   cp .env.example .env
   ```
   Задайте `DB_PASSWORD` в `.env` (строка из букв и цифр без спецсимволов). Файл `.env` исключён из Git.

2. Запустите PostgreSQL и дождитесь готовности:
   ```bash
   docker compose up -d --wait postgres
   ```

3. Запустите приложение:
   ```bash
   ./mvnw spring-boot:run
   ```
   API доступен по `http://localhost:8080`. При запуске Flyway применяет миграции, а Hibernate проверяет соответствие схемы (`ddl-auto=validate`).

### Переменные окружения

| Переменная | Значение по умолчанию / назначение |
|---|---|
| `DB_NAME` | `credit_decision` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | Обязательна для обычного запуска приложения (задаётся в `.env` или окружении) |
| `DB_PORT` | `5432` (порт PostgreSQL на `127.0.0.1`) |
| `DB_URL` | Необязательно: полный JDBC URL вместо локального адреса |

Остановка БД с сохранением данных в volume `postgres_data`:
```bash
docker compose down
```

---

## Существующий Legacy API (`/api/decisions`)

На период поэтапной разработки нового конвейера (`/api/v1/decisions`) в проекте сохранён исходный эндпоинт `/api/decisions`:

| Метод | Путь | Результат |
|---|---|---|
| `POST` | `/api/decisions` | Рассчитать и сохранить решение (`200 OK`) |
| `GET` | `/api/decisions/{uuid}` | Получить решение по ID (`200 OK` / `404 Not Found`) |
| `GET` | `/api/decisions?page=0&size=20` | Получить страницу истории (`200 OK`) |

Пример запроса:
```bash
curl -X POST http://localhost:8080/api/decisions \
  -H 'Content-Type: application/json' \
  -d '{"income":1000000,"requestedAmount":3000000,"termMonths":24}'
```

Готовые примеры запросов доступны в [requests.http](requests.http).

---

## Тесты и сборка

Запуск быстрых unit-тестов (без Docker и БД):
```bash
./mvnw -Dtest=LoanRiskCalculatorTest,LoanDecisionServiceTest test
```

Полная проверка (включая интеграционный тест с PostgreSQL 16 в Testcontainers) и сборка JAR (требуется запущенный Docker):
```bash
./mvnw verify
```

---

## Документация проекта

- [Технический дизайн (v3.0)](docs/design.md) — 5-стадийный конвейер принятия решений, Risk-Based Pricing, формулы КДН и прожиточного минимума, дифференцированный график, матрица контр-офферов, схема БД и контракты `/api/v1/decisions`.
- [План реализации](docs/development-plan.md) — разбивка разработки на 6 последовательных этапов с критериями готовности.
