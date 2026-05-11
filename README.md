# BLPS Lab 1 — Food Delivery System

Распределённая система управления доставкой еды. Два инстанса Spring Boot за nginx, PostgreSQL, ActiveMQ (STOMP), Quartz (кластер), JTA/Narayana, JCA-адаптер для Nextcloud/OnlyOffice.

## Запуск

```bash
docker-compose up --build -d
./nextcloud-setup.sh   # дождаться "Done." (≈ 2-3 мин)
```

API доступно через nginx: `http://localhost/api/...`

### Учётные записи

| Логин       | Пароль      | Роль       |
|-------------|-------------|------------|
| client1-5   | client1-5   | CLIENT     |
| courier1-5  | courier1-5  | COURIER    |
| restaurant1-5 | restaurant1-5 | RESTAURANT |

---

## Flows

> Во всех curl-командах используется `http://localhost/api` — запросы идут через nginx и балансируются между app-1 и app-2.

---

### Flow 1 — Успешная доставка

**Шаг 1. Клиент создаёт заказ**

```bash
curl -s -u client1:client1 -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "city": "Москва",
    "deliveryAddress": "ул. Тестовая, д. 1",
    "phone": "79991234567",
    "comment": "Без лука",
    "items": [
      {"name": "Бургер", "quantity": 2, "price": 350},
      {"name": "Картофель фри", "quantity": 1, "price": 120}
    ]
  }'
```

Ответ содержит `"id"` — запомнить как `ORDER_ID`. Статус: `NEW`, поле `courierId` — назначенный курьер.

**Шаг 2. Проверить заказ**

```bash
curl -s -u client1:client1 http://localhost/api/orders/1
```

**Шаг 3. Курьер принимает заказ**

Если назначен `courierId: 1`:

```bash
curl -s -u courier1:courier1 -X POST http://localhost/api/orders/1/accept
```

Статус → `ACCEPTED`.

**Шаг 4. Ресторан готовит заказ**

```bash
curl -s -u restaurant1:restaurant1 -X POST http://localhost/api/orders/1/cook
```

Статус → `COOKED`.

**Шаг 5. Курьер забирает заказ**

```bash
curl -s -u courier1:courier1 -X POST http://localhost/api/orders/1/pickup
```

Статус → `PICKED_UP`.

**Шаг 6. Курьер завершает доставку**

```bash
curl -s -u courier1:courier1 -X POST http://localhost/api/orders/1/complete
```

Статус → `DELIVERED`. Заказ закрыт.

---

### Flow 2 — Отмена заказа клиентом

Клиент может отменить заказ в статусах `NEW`, `ACCEPTED`, `COOKED`, `PICKED_UP`.

**Создать заказ:**

```bash
curl -s -u client2:client2 -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 2,
    "city": "Москва",
    "deliveryAddress": "ул. Ленина, д. 10",
    "phone": "79997654321",
    "items": [{"name": "Пицца", "quantity": 1, "price": 600}]
  }'
```

**Отменить:**

```bash
curl -s -u client2:client2 -X POST http://localhost/api/orders/2/cancel
```

Статус → `CANCELLED`. Курьер освобождается.

---

### Flow 3 — Курьер отклоняет заказ (переназначение)

**Создать заказ:**

```bash
curl -s -u client3:client3 -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "city": "Москва",
    "deliveryAddress": "пр. Мира, д. 5",
    "phone": "79990001122",
    "items": [{"name": "Роллы", "quantity": 3, "price": 450}]
  }'
```

**Курьер отклоняет** (вместо `courierN` — тот, кто назначен):

```bash
curl -s -u courier1:courier1 -X POST http://localhost/api/orders/3/reject
```

Система автоматически назначает другого свободного курьера (исключая уже отклонивших). Статус остаётся `NEW`, в ответе новый `courierId`.

---

### Flow 4 — Все курьеры заняты

**Проблема:** `OrderService.selectRandomFreeCourier()` ищет курьеров, чей последний заказ имеет статус `DELIVERED` или `CANCELLED`. Если все 5 курьеров имеют активные заказы — выбросится `IllegalStateException("No free couriers available")`.

**Симптом:** `POST /api/orders` возвращает 500.

**Диагностика через БД:**

```bash
docker exec -it postgres psql -U postgres -d lab1 -c "
SELECT c.id, c.full_name, o.id as order_id, o.status
FROM couriers c
LEFT JOIN orders o ON o.id = (
  SELECT id FROM orders WHERE courier_id = c.id ORDER BY id DESC LIMIT 1
)
ORDER BY c.id;"
```

**Исправление на горячую** — пометить застрявший заказ как доставленный, чтобы освободить курьера:

```bash
# Найти активный заказ нужного курьера
docker exec -it postgres psql -U postgres -d lab1 -c "
  SELECT id, status, courier_id FROM orders
  WHERE status NOT IN ('DELIVERED','CANCELLED')
  ORDER BY id DESC LIMIT 10;"

# Освободить курьера (например courier_id=1, order_id=X)
docker exec -it postgres psql -U postgres -d lab1 -c "
  UPDATE orders SET status='DELIVERED' WHERE id=X;"
```

После этого новые заказы снова будут назначаться.

---

### Flow 5 — Отказоустойчивость: падение одного инстанса

Система запускает два инстанса `app-1` и `app-2` за nginx (round-robin).

**Проверить, какой инстанс отвечает:**

```bash
for i in $(seq 1 6); do
  curl -s -u client1:client1 http://localhost/api/util/info
  echo
done
```

Ответы будут чередоваться между `Instancenumber: 1` и `Instancenumber: 2`.

**Уронить app-1:**

```bash
docker stop app-1
```

**Убедиться, что всё работает:**

```bash
# Все запросы теперь идут только на app-2
curl -s -u client1:client1 http://localhost/api/orders/1

curl -s -u client1:client1 -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 3,
    "city": "Москва",
    "deliveryAddress": "ул. Победы, д. 7",
    "phone": "79993334455",
    "items": [{"name": "Суши", "quantity": 2, "price": 800}]
  }'

# Quartz-джобы тоже работают (кластер переключился на app-2)
curl -s -u client1:client1 -X POST http://localhost/api/reports/run/hourly
```

**Поднять обратно:**

```bash
docker start app-1
```

---

### Flow 6 — Отчёты через Quartz (ручной запуск)

Отчёты автоматически генерируются по расписанию, но можно запустить вручную через API.

**Запустить почасовой снапшот активных заказов:**

```bash
curl -s -u client1:client1 -X POST http://localhost/api/reports/run/hourly
```

**Запустить отчёт по зависшим заказам (> 30 мин без смены статуса):**

```bash
curl -s -u client1:client1 -X POST http://localhost/api/reports/run/stuck
```

**Запустить ежедневный отчёт (выручка ресторанов + статистика курьеров):**

```bash
curl -s -u client1:client1 -X POST http://localhost/api/reports/run/daily
```

**Посмотреть список сгенерированных отчётов:**

```bash
curl -s -u client1:client1 http://localhost/api/reports | python3 -m json.tool
```

Каждый отчёт содержит `storageUrl` — путь в Nextcloud WebDAV. Поле `generatedByInstance` показывает, на каком инстансе был сгенерирован отчёт.

**Скачать CSV-отчёт:**

```bash
# subDir = тип отчёта (daily/hourly/stuck), fileName — из поля fileName в ответе
curl -s http://localhost/api/documents/hourly/active_orders_YYYYMMDD_HHmmss.csv -o report.csv
```

**Открыть отчёт в браузере через OnlyOffice:**

```
http://localhost/api/documents/hourly/active_orders_YYYYMMDD_HHmmss.csv/view
```

---

## Расписание Quartz-джобов

| Джоб              | Cron              | Описание                                          |
|-------------------|-------------------|---------------------------------------------------|
| DailyReportJob    | `0 55 23 * * ?`   | Ежедневный: выручка ресторанов, статистика курьеров |
| HourlyActiveOrdersJob | `0 0 * * * ?` | Снапшот активных заказов каждый час               |
| StuckOrdersJob    | `0 0/15 * * * ?`  | Зависшие заказы (> 30 мин) каждые 15 мин         |

---

## Статусная машина заказа

```
NEW ──[accept]──→ ACCEPTED ──[cook]──→ COOKED ──[pickup]──→ PICKED_UP ──[complete]──→ DELIVERED
 │                  │                    │             │
 │                  └────────────────────┴─────────────┴──[cancel]──→ CANCELLED
 └──[reject]──→ NEW (другой курьер)
 └──[cancel]──→ CANCELLED
```

---

## JCA-адаптер (Nextcloud/OnlyOffice)

### Файлы и их назначение

| Файл | Роль в JCA |
|------|-----------|
| `EisConfig.java` | Spring `@Configuration` — создаёт бины `ManagedConnectionFactory` и `ConnectionFactory` из `application.yaml` |
| `OnlyOfficeManagedConnectionFactory.java` | Фабрика физических соединений. Хранит URL/логин/пароль, создаёт `ManagedConnection` по запросу `ConnectionManager`-а |
| `OnlyOfficeManagedConnection.java` | Физическое соединение — настоящий `HttpClient`. Делает HTTP PUT (WebDAV) в Nextcloud, MKCOL для создания директорий. Хранит список `ConnectionEventListener`-ов |
| `DefaultConnectionManager.java` | Упрощённый `ConnectionManager` без пула: каждый вызов создаёт новое физическое соединение |
| `OnlyOfficeConnectionFactoryImpl.java` | CCI `ConnectionFactory` — то, что инжектится в бизнес-код. `getConnection()` → `ConnectionManager.allocateConnection()` |
| `OnlyOfficeConnectionImpl.java` | Handle-обёртка над `ManagedConnection`, которую держит бизнес-код. При `close()` — сигнализирует MC |
| `OnlyOfficeConnection.java` | Интерфейс handle-а: один метод `publishDocument()` |
| `OnlyOfficeConnectionFactory.java` | Интерфейс фабрики: расширяет стандартный `ConnectionFactory`, добавляет `getConnection(OnlyOfficeConnectionSpec)` |
| `OnlyOfficeConnectionSpec.java` | `ConnectionSpec` — параметры конкретного запроса (тип отчёта = subDir в WebDAV-пути) |
| `PublishedDocument.java` | DTO результата: имя файла, WebDAV URL, размер |

---

### Как это выглядело бы на WildFly

В нашем проекте всё живёт внутри одного Spring Boot JAR — `DefaultConnectionManager` не умеет пулить соединения, а `ManagedConnectionFactory` создаётся как обычный Spring Bean.

На настоящем Jakarta EE сервере (WildFly, GlassFish) адаптер упаковывается отдельно и разворачивается сервером:

**Структура RAR-архива** (Resource Adapter Archive, аналог WAR для коннекторов):

```
onlyoffice-adapter.rar
├── META-INF/
│   └── ra.xml                          ← дескриптор: описывает классы адаптера,
│                                          типы транзакций, config-property для URL/логина
└── onlyoffice-adapter.jar
    └── ru/blps/lab_1/eis/
        ├── OnlyOfficeManagedConnectionFactory.class
        ├── OnlyOfficeManagedConnection.class
        ├── OnlyOfficeConnectionFactoryImpl.class
        ├── OnlyOfficeConnectionImpl.class
        └── ...
```

`ra.xml` — это XML-манифест адаптера. В нём объявлено:
- какой класс является `ManagedConnectionFactory`
- какие config-property (webdavUrl, user, password) настраиваются через консоль сервера
- тип транзакций: `NoTransaction` / `LocalTransaction` / `XATransaction`

**Жизненный цикл на сервере:**

```
Деплой RAR
    │
    ▼
WildFly читает ra.xml
    │  создаёт и конфигурирует
    ▼
ManagedConnectionFactory (singleton)
    │
    │  при первом getConnection()
    ▼
ConnectionManager (встроенный в сервер — умеет пулить!)
    │  вызывает mcf.createManagedConnection()
    ▼
ManagedConnection (из пула или новый)
    │  возвращает
    ▼
ConnectionImpl (handle) ── инжектится в EJB/@Resource
```

Приложение (WAR/EAR) обращается к адаптеру через JNDI-lookup или `@Resource`:

```java
// В нашем коде (Spring) — через @Autowired:
@Autowired OnlyOfficeConnectionFactory cf;

// На WildFly — через JNDI:
@Resource(lookup = "java:/eis/OnlyOfficeConnectionFactory")
OnlyOfficeConnectionFactory cf;
```

**Что сервер берёт на себя вместо нашего `DefaultConnectionManager`:**
- пул физических соединений с min/max размером
- проверка живости соединений (validation)
- участие в XA-транзакциях (если адаптер объявляет `XATransaction` в `ra.xml`)
- статистика и мониторинг через консоль

---

### Схема вызова — от бизнес-кода до Nextcloud

```
EisPublishService
  │  connectionFactory.getConnection(spec)
  ▼
OnlyOfficeConnectionFactoryImpl
  │  cm.allocateConnection(mcf, null)
  ▼
DefaultConnectionManager                    ← на WildFly здесь был бы пул сервера
  │  mcf.createManagedConnection()
  ▼
OnlyOfficeManagedConnectionFactory
  │  new OnlyOfficeManagedConnection(url, user, password)
  ▼
OnlyOfficeManagedConnection
  │  mc.getConnection()  →  new OnlyOfficeConnectionImpl(this)
  ▼
OnlyOfficeConnectionImpl  ──────────────────  возвращается в EisPublishService
  │
  │  conn.publishDocument("daily", "report.csv", bytes)
  │       └── mc.publishDocument()
  ▼
HttpClient.send( PUT http://nginx/cloud/remote.php/dav/files/admin/reports/daily/report.csv )
  │
  ▼
Nextcloud WebDAV  →  файл сохранён
  │
  ▼
EisReport сохраняется в PostgreSQL (storageUrl, fileName, sizeBytes, ...)
```

---

## Архитектура

```
Browser / curl
      │
    nginx :80
    ├── /api/*      → app-1 :8080  ╮  round-robin
    │               → app-2 :8080  ╯
    ├── /cloud/     → nextcloud (fpm)
    └── /office/    → onlyoffice :8080

app-1 / app-2
  ├── PostgreSQL :5432  (JTA/XA через Narayana)
  ├── ActiveMQ :61613   (STOMP, order.notifications queue)
  └── Nextcloud WebDAV  (JCA-адаптер, публикация CSV-отчётов)
```
