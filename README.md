# BLPS Lab 4 — Food Delivery with Camunda BPM

Spring Boot 3.4.5 + Camunda 7.23 + JTA/Narayana + PostgreSQL

## Запуск

### 1. PostgreSQL в Docker

```bash
docker run -d \
  --name lab1-postgres \
  -e POSTGRES_DB=lab_1 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

### 2. Сборка и запуск

```bash
./gradlew bootJar
java -jar build/libs/lab_1-0.0.1-SNAPSHOT.jar
```

Приложение поднимается на `http://localhost:8080`.

Тестовые данные (пользователи, курьеры, рестораны) загружаются автоматически из `data.sql`.

---

## Пользователи

| Логин | Пароль | Роль |
|-------|--------|------|
| client1..5 | client1..5 | Клиент |
| courier1..5 | courier1..5 | Курьер |
| restaurant1..5 | restaurant1..5 | Ресторан |
| admin | admin | Camunda Admin |

---

## Сценарий: полный жизненный цикл заказа

### Шаг 1. Клиент создаёт заказ

```bash
curl -u client1:client1 -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "restaurantId": 1,
    "city": "Москва",
    "deliveryAddress": "Тверская 1",
    "phone": "79001234567",
    "items": [
      {"name": "Пицца Маргарита", "quantity": 1, "price": 650},
      {"name": "Кола 0.5", "quantity": 2, "price": 120}
    ]
  }'
```

В ответе вернётся JSON с `id` заказа. Запомните его — он понадобится для отмены.

После этого Camunda автоматически:
- Запускает процесс `orderDeliveryProcess`
- Случайно выбирает свободного курьера
- Создаёт задачу «Принять или отклонить заказ», назначенную на этого курьера

### Шаг 2. Курьер принимает заказ — Camunda Tasklist

1. Открыть [http://localhost:8080/camunda](http://localhost:8080/camunda)
2. Войти под логином назначенного курьера (например, `courier3` / `courier3`)
3. В списке задач появится **«Принять или отклонить заказ»**
4. Нажать **Claim** → в поле «Решение курьера» выбрать **Принять заказ** → нажать **Complete**

> Если курьер выбирает «Отклонить заказ» — процесс автоматически назначает другого свободного курьера и создаёт задачу заново.

### Шаг 3. Ресторан готовит заказ — Camunda Tasklist

1. Войти в Tasklist под логином ресторана (например, `restaurant1` / `restaurant1`)
2. Появится задача **«Приготовить заказ»**
3. **Claim** → отметить чекбокс → **Complete**

### Шаг 4. Курьер забирает заказ — Camunda Tasklist

1. Войти в Tasklist под тем же курьером (например, `courier3`)
2. Задача **«Забрать заказ из ресторана»**
3. **Claim** → отметить чекбокс → **Complete**

### Шаг 5. Курьер доставляет заказ — Camunda Tasklist

1. Тот же курьер в Tasklist
2. Задача **«Доставить заказ клиенту»**
3. **Claim** → отметить чекбокс → **Complete**

Процесс завершён. Статус заказа: `DELIVERED`.

---

## Отмена заказа

Клиент может отменить заказ до момента доставки (статусы NEW, ACCEPTED, COOKED, PICKED_UP).

```bash
curl -u client1:client1 -X POST http://localhost:8080/api/orders/{id}/cancel
```

Замените `{id}` на ID заказа из ответа шага 1.

---

## Мониторинг процессов

**Camunda Cockpit** — [http://localhost:8080/camunda/app/cockpit](http://localhost:8080/camunda/app/cockpit)

Войти под `admin` / `admin`. Там видно все активные и завершённые экземпляры процесса, текущий шаг, переменные.
