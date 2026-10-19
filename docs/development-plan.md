# Credit Decision Platform — план реализации

## 1. Цель и подход

План определяет пошаговую реализацию коммерческого конвейера принятия решений (**Decision Pipeline**) в соответствии с [техническим дизайном v3.0](design.md).

Каждый этап даёт завершённый, изолированно тестируемый функциональный блок:
`Политика и модели` $\to$ `Стоп-факторы и Risk-Based Pricing` $\to$ `Платёжеспособность и Калькулятор дифференцированного графика` $\to$ `Генератор матрицы офферов и Оркестратор` $\to$ `Персистентность (Flyway V4)` $\to$ `REST API и Идемпотентность`.

Применённые миграции Flyway `V1`–`V3` сохраняются для валидации контрольных сумм; миграция `V4` заменяет устаревшую таблицу на новую схему `credit_decision`.

---

## 2. Дорожная карта этапов

| Этап | Название | Основной результат | Критерий готовности |
|---|---|---|---|
| **0** | **Проектирование конвейера (Design v3.0)** | Архитектура 5-стадийного пайплайна, формулы, контракты API и схема БД | Актуализированы `docs/design.md`, `docs/development-plan.md`, `README.md`, удалён устаревший код |
| **1** | **Доменное ядро: Политика, Стоп-факторы и Risk-Based Pricing** | Классы `CreditPolicy`, `RiskGrade`, `HardStopEvaluator`, `RiskGradeResolver` | Unit-тесты проверяют все стоп-факторы (возраст, просрочки, мин. балл) и назначение грейдов `A–D` со ставками и лимитами |
| **2** | **Расчёт платёжеспособности (Affordability)** | `AffordabilityCalculator` (двойной контроль: КДН 50% + прожиточный минимум) | Unit-тесты покрывают расчёт бюджета при низком и высоком доходе, граничные случаи КДН и отказы по нехватке бюджета |
| **3** | **Калькулятор дифференцированного кредита** | `DifferentiatedLoanCalculator` (помесячный график, первый/последний платёж, обратный расчёт с шагом 10 000 ₸) | Unit-тесты проверяют точность `HALF_EVEN`, закрытие последнего месяца в 0.00 ₸ и верификацию обратного расчёта реальным графиком |
| **4** | **Генератор матрицы офферов и Конвейер решений** | `OfferMatrixGenerator` и `DecisionPipeline` (`APPROVED`, `COUNTER_OFFER` по сумме и по сроку, `REJECTED`) | Полный набор unit-тестов всех бизнес-сценариев: прямое одобрение, контр-оффер с уменьшением суммы, контр-оффер с удлинением срока, отказ |
| **5** | **Хранение и идемпотентность (PostgreSQL + Flyway V4)** | Миграция `V4__replace_loan_decision_with_credit_decision.sql`, JPA-сущность, репозиторий и сервис с защитой `requestId` | Интеграционные тесты с Testcontainers (PostgreSQL 16) подтверждают сохранение JSONB-офферов, снимка и идемпотентность при повторе `requestId` |
| **6** | **REST API и валидация** | `CreditDecisionController` (`POST /api/decisions`, `GET /api/decisions/{id}`, пагинация), DTO, `requests.http` | Сквозные MockMvc / SpringBootTest проверки HTTP-контрактов, ошибок `400`/`404` и корректной сериализации ответа |

---

## 3. Детализация этапов разработки

### Этап 1. Доменное ядро: Политика, Стоп-факторы и Risk-Based Pricing
- Пакет: `kz.alitulegenov.creditdecisionengine.domain`
- Создание неизменяемой конфигурации `CreditPolicy` (с фабричным методом `commercialV1()`), содержащей все пороги (возраст 21–65, просрочка до 30 дней, мин. балл 520, КДН 50%, прожиточный минимум 85 000 ₸, шаг суммы 10 000 ₸, сетка сроков).
- Enum `DecisionStatus` (`APPROVED`, `COUNTER_OFFER`, `REJECTED`) и `DecisionReasonCode`.
- Enum `RiskGrade` (`GRADE_A`, `GRADE_B`, `GRADE_C`, `GRADE_D`, `REJECT`).
- Компонент `HardStopEvaluator`: проверяет входной профиль на стоп-факторы и возвращает список сработавших кодов отказа.
- Компонент `RiskGradeResolver`: по `creditScore` возвращает грейд, индивидуальную годовую ставку (`nominalAnnualRate`) и потолок риска (`riskExposureCap`).

### Этап 2. Расчёт платёжеспособности (Affordability)
- Модель `AffordabilityResult`: текущий КДН, бюджет по КДН, бюджет по прожиточному минимуму, итоговый доступный платёж, признак достаточности бюджета и коды причин при отказе.
- Компонент `AffordabilityCalculator`: вычисляет обе границы и выбирает минимальную из них.

### Этап 3. Калькулятор дифференцированного кредита
- Модели `ScheduleInstallment` (один месяц графика) и `DifferentiatedScheduleSummary` (первый платёж, последний платёж, сумма процентов, полная сумма к возврату, список платежей).
- Компонент `DifferentiatedLoanCalculator`:
  1. `calculateSchedule(principal, termMonths, annualRate)` — точный расчёт графика с закрытием последнего платежа без копеечного остатка.
  2. `calculateMaxAffordablePrincipal(availableBudget, termMonths, annualRate, maxCap, step)` — аналитическая оценка сверху $\to$ ограничение лимитами $\to$ округление вниз до шага `10 000 ₸` $\to$ контрольная проверка реальным графиком.

### Этап 4. Генератор матрицы офферов и Оркестратор конвейера
- Модели `OfferType` (`REQUESTED_TERMS`, `REDUCED_AMOUNT_SAME_TERM`, `EXTENDED_TERM_FOR_AMOUNT`) и `LoanOffer`.
- Компонент `OfferMatrixGenerator`:
  - Проверяет проходимость запрошенной суммы и срока.
  - Если не проходит — строит Вариант А (снижение суммы на том же сроке) и Вариант Б (минимальный более длинный срок из сетки `[6, 12, 18, 24, 36, 48, 60]` с учётом возраста погашения $\le 65$ лет).
- Фасад `DecisionPipeline`: объединяет Стадии 1–5 в единый чистый процесс.

### Этап 5. Персистентность и идемпотентность (Flyway V4)
- Миграция `V4__replace_loan_decision_with_credit_decision.sql`.
- Сущность `CreditDecision`, репозиторий `CreditDecisionRepository`.
- Сервис `CreditDecisionService`: проверяет наличие решения по `requestId` (идемпотентный возврат), вызывает `DecisionPipeline`, сохраняет результат в БД.

### Этап 6. REST API
- DTO: `CreditDecisionRequest`, `CreditDecisionResponse`, вложенные блоки `RiskAssessmentDto`, `AffordabilityDto`, `LoanOfferDto`.
- Контроллер `CreditDecisionController` (`/api/decisions`).
- Обновление `GlobalExceptionHandler` и примеров в `requests.http`.
