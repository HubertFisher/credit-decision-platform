# Credit Decision Platform

Учебный backend на Java и Spring Boot для расчёта кредитного риска и хранения решений. Проект развивается небольшими шагами: от синхронного REST API к системе обработки кредитных заявок.

## Что уже работает

- Расчёт риска и сохранение решения в PostgreSQL.
- Получение решения по UUID.
- История решений с пагинацией и сортировкой по времени создания и UUID.
- Валидация входных данных и обработка ошибок.
- Версионирование схемы БД через Flyway.
- Unit-тесты правил риска и сервиса, тест запуска Spring-контекста с PostgreSQL.

Текущая модель — упрощённые учебные правила. Авторизация, отдельный жизненный цикл заявки, Kafka и outbox относятся к дальнейшему развитию. API предназначен для локального запуска с тестовыми данными.

## Стек

Java 21 · Spring Boot 4.1.1 · Spring Web MVC · Bean Validation · Spring Data JPA / Hibernate · PostgreSQL 16 · Flyway · Maven Wrapper · JUnit / Mockito · Docker Compose.

## Локальный запуск

Потребуются JDK 21 и Docker с Docker Compose. Maven отдельно устанавливать не нужно: в репозитории есть Wrapper. Проверьте, что `java -version` и `./mvnw -version` используют нужный JDK.

Все команды выполняются из корня репозитория.

1. Создайте локальный файл настроек:

   ```bash
   cp .env.example .env
   ```

   Задайте `DB_PASSWORD` в `.env`. Для простого локального запуска используйте значение без кавычек, пробелов, обратных слешей и знака `$`, например случайную строку из букв и цифр. Файл читает и Docker Compose, и приложение. Он исключён из Git; `.env.example` содержит только пример.

2. Запустите PostgreSQL и дождитесь готовности:

   ```bash
   docker compose up -d --wait postgres
   ```

3. Запустите приложение:

   ```bash
   ./mvnw spring-boot:run
   ```

   API доступен по `http://localhost:8080`. При запуске Flyway применяет миграции, Hibernate проверяет соответствие схемы сущностям.

На Windows используйте `Copy-Item .env.example .env` и `mvnw.cmd` вместо `./mvnw`. В IntelliJ IDEA выберите Java 21 для проекта и Maven Runner; рабочий каталог конфигурации запуска — корень репозитория.

### Настройки

| Переменная | Значение по умолчанию / назначение |
|---|---|
| `DB_NAME` | `credit_decision` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | Обязательна; задаётся в `.env` или окружении |
| `DB_PORT` | `5432`, порт PostgreSQL на локальном компьютере |
| `DB_URL` | Необязательно: полный JDBC URL вместо локального адреса |

Переменные окружения имеют приоритет над `.env`. Для подключения к существующей БД задайте её JDBC URL, пользователя и пароль. PostgreSQL в Compose публикует порт только на `127.0.0.1`.

**Если БД уже использовалась:** сохраните в `.env` её прежние имя, пользователя и пароль. `POSTGRES_*` инициализируют только новый volume; изменение `.env` не меняет пароль в уже созданной БД. Не удаляйте volume ради исправления пароля. Если порт 5432 занят, измените `DB_PORT` в `.env` и пересоздайте контейнер через `docker compose up -d --wait postgres`.

Остановка БД с сохранением данных:

```bash
docker compose down
```

Данные находятся в named volume `postgres_data` и сохраняются при обычной остановке контейнеров.

## API

| Метод | Путь | Результат |
|---|---|---|
| `POST` | `/api/decisions` | Рассчитать и сохранить решение; `200 OK` |
| `GET` | `/api/decisions/{uuid}` | Получить решение; `200 OK` или `404 Not Found` |
| `GET` | `/api/decisions?page=0&size=20` | Получить страницу истории; `200 OK` |

### Создать решение

```bash
curl -X POST http://localhost:8080/api/decisions \
  -H 'Content-Type: application/json' \
  -d '{"income":1000000,"requestedAmount":3000000,"termMonths":24}'
```

Пример ответа (UUID и время будут другими):

```json
{
  "id": "9e22ac19-8449-49fa-afd8-f6832baad0a5",
  "approved": true,
  "income": 1000000,
  "requestedAmount": 3000000,
  "termMonths": 24,
  "riskScore": 0,
  "createdAt": "2026-10-15T10:00:00Z"
}
```

Время возвращается в UTC. Денежные значения обрабатываются через `BigDecimal` и хранятся с двумя дробными знаками. Валюта пока не является полем API.

| Поле | Ограничения |
|---|---|
| `income` | Обязательно, больше 0, до 17 целых и 2 дробных знаков |
| `requestedAmount` | Обязательно, больше 0, до 17 целых и 2 дробных знаков |
| `termMonths` | Обязательно, целое число от 1 до 360 |

### Получить решение и историю

Подставьте UUID из ответа POST:

```bash
curl http://localhost:8080/api/decisions/9e22ac19-8449-49fa-afd8-f6832baad0a5
curl 'http://localhost:8080/api/decisions?page=0&size=20'
```

Ответ списка содержит `items`, `page`, `size`, `totalElements`, `totalPages`. Нумерация страниц начинается с 0; `size` — от 1 до 100, по умолчанию 20. Порядок фиксирован: `createdAt DESC, id DESC`. Пустая страница возвращает `items: []`. При добавлении новых решений между запросами страницы могут смещаться.

В [requests.http](requests.http) есть готовые примеры для HTTP-клиента IntelliJ IDEA: создание, чтение, история и ошибки.

### Ошибки

Валидация тела и параметров, ошибки преобразования параметров и отсутствие решения обрабатываются через `ApiError`. Например, для `page=abc`:

```json
{
  "status": 400,
  "message": "Invalid parameter",
  "errors": {
    "page": "Invalid value type"
  }
}
```

Некорректные значения дают `400`, отсутствующее решение — `404`. Не все ошибки HTTP пока приведены к этому формату; например, обработка синтаксически некорректного JSON ещё не унифицирована.

## Правила скоринга

Баллы складываются. Чем выше итоговый балл, тем выше риск по учебной модели.

| Условие | Баллы |
|---|---:|
| Запрошенная сумма больше `income × 10` | +40 |
| Срок больше 36 месяцев | +20 |
| Доход меньше 300000 | +30 |

Решение одобряется при `riskScore < 50`; при 50 и выше — отклоняется. Равенство порогу конкретного правила не добавляет баллы: ровно 36 месяцев, доход 300000 и сумма ровно в 10 доходов не включают соответствующий фактор.

## Как устроен проект

```text
HTTP request → LoanDecisionController → LoanDecisionService
                                         ├── LoanRiskCalculator
                                         └── LoanDecisionRepository → PostgreSQL
```

Калькулятор вызывается сервисом; он не обращается к репозиторию. Исключения API обрабатывает `GlobalExceptionHandler`. DTO отделены от JPA-сущности.

```text
src/main/java/kz/alitulegenov/creditdecisionengine/
├── controller/      REST API
├── dto/             запросы, ответы и ошибки
├── entity/          сущность кредитного решения
├── exception/       исключения и обработчики
├── repository/      Spring Data JPA
├── risk/            расчёт риска
└── service/         сценарии приложения
src/main/resources/db/migration/  миграции Flyway
src/test/java/                    тесты
```

Применённые миграции Flyway не редактируются. Изменения схемы оформляются новой миграцией после V3.

## Тесты и сборка

Только unit-тесты — без PostgreSQL:

```bash
./mvnw -Dtest=LoanRiskCalculatorTest,LoanDecisionServiceTest test
```

Полная проверка и сборка JAR — с доступной БД из локальных настроек:

```bash
docker compose up -d --wait postgres
./mvnw verify
```

Сейчас есть 8 тестов калькулятора, 5 тестов сервиса с Mockito и 1 тест запуска контекста. Последний подключается к PostgreSQL и запускает Flyway, поэтому для полного прогона нужна БД. Используйте локальную учебную или отдельную тестовую БД.

После сборки приложение можно запустить из корня репозитория:

```bash
java -jar target/credit-decision-engine-0.0.1-SNAPSHOT.jar
```

GitHub Actions запускает `./mvnw --batch-mode --no-transfer-progress verify` на Java 21 с отдельным PostgreSQL 16. Публикация и развёртывание workflow не выполняются.

## Дальнейшее развитие

Следующие учебные направления: отдельная кредитная заявка и её состояния, версии правил и объяснения решения, идемпотентность, фоновая обработка, авторизация, transactional outbox и Kafka. Они ещё не реализованы в текущем API и будут добавляться поэтапно после согласования дизайна.

Справочные материалы по настройке: [Spring Boot external configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html), [Docker Compose environment variables](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/), [GitHub Actions для Maven](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven).
