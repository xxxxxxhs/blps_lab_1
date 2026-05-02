package ru.blps.lab_1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderItem;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.repository.CourierRepository;
import ru.blps.lab_1.repository.OrderRepository;
import ru.blps.lab_1.repository.RestaurantRepository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
        OrderStatus.NEW, OrderStatus.ACCEPTED, OrderStatus.COOKED, OrderStatus.PICKED_UP
    );

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final RestaurantRepository restaurantRepository;

    public ReportService(OrderRepository orderRepository,
                         CourierRepository courierRepository,
                         RestaurantRepository restaurantRepository) {
        this.orderRepository = orderRepository;
        this.courierRepository = courierRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public byte[] buildDailyReport(LocalDate day) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        List<Order> orders = orderRepository.findCreatedBetween(from, to);

        Map<Long, long[]> byRest = new HashMap<>();
        Map<Long, long[]> byCourier = new HashMap<>();
        Map<Long, Double> revByRest = new HashMap<>();

        for (Order o : orders) {
            long[] r = byRest.computeIfAbsent(o.getRestaurantId(), k -> new long[2]);
            r[0]++;
            if (o.getStatus() == OrderStatus.CANCELLED) r[1]++;

            if (o.getCourierId() != null) {
                long[] c = byCourier.computeIfAbsent(o.getCourierId(), k -> new long[2]);
                c[0]++;
                if (o.getStatus() == OrderStatus.DELIVERED) c[1]++;
            }

            double sum = 0;
            for (OrderItem i : o.getItems()) {
                sum += (i.getPrice() == null ? 0 : i.getPrice()) * (i.getQuantity() == null ? 0 : i.getQuantity());
            }
            revByRest.merge(o.getRestaurantId(), sum, Double::sum);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Отчёт за ").append(day).append("\n\n");
        sb.append("=== Рестораны ===\n");
        sb.append("id;name;orders_total;orders_cancelled;revenue\n");
        restaurantRepository.findAllById(byRest.keySet()).forEach(r -> {
            long[] s = byRest.get(r.getId());
            double rev = revByRest.getOrDefault(r.getId(), 0.0);
            sb.append(r.getId()).append(';').append(escape(r.getName())).append(';')
              .append(s[0]).append(';').append(s[1]).append(';')
              .append(String.format("%.2f", rev)).append('\n');
        });
        sb.append("\n=== Курьеры ===\n");
        sb.append("id;name;orders_total;orders_delivered\n");
        courierRepository.findAllById(byCourier.keySet()).forEach(c -> {
            long[] s = byCourier.get(c.getId());
            sb.append(c.getId()).append(';').append(escape(c.getFullName())).append(';')
              .append(s[0]).append(';').append(s[1]).append('\n');
        });
        sb.append("\nИтого заказов: ").append(orders.size()).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] buildActiveOrdersSnapshot() {
        List<Order> orders = orderRepository.findAllByStatusIn(ACTIVE_STATUSES);
        StringBuilder sb = new StringBuilder();
        sb.append("Активные заказы на ").append(LocalDateTime.now().format(TS)).append("\n\n");
        sb.append("order_id;status;restaurant_id;courier_id;city;age_minutes\n");
        LocalDateTime now = LocalDateTime.now();
        for (Order o : orders) {
            long age = o.getCreatedAt() == null ? 0 : Duration.between(o.getCreatedAt(), now).toMinutes();
            sb.append(o.getId()).append(';')
              .append(o.getStatus()).append(';')
              .append(o.getRestaurantId()).append(';')
              .append(o.getCourierId() == null ? "" : o.getCourierId()).append(';')
              .append(escape(o.getCity())).append(';')
              .append(age).append('\n');
        }
        sb.append("\nВсего активных: ").append(orders.size()).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public StuckReport buildStuckOrdersReport(int thresholdMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(thresholdMinutes);
        List<Order> stuck = orderRepository.findStuckOrders(ACTIVE_STATUSES, threshold);
        if (stuck.isEmpty()) return new StuckReport(null, 0);

        StringBuilder sb = new StringBuilder();
        sb.append("Зависшие заказы (без изменений > ").append(thresholdMinutes).append(" мин) на ")
          .append(LocalDateTime.now().format(TS)).append("\n\n");
        sb.append("order_id;status;minutes_in_status;restaurant_id;courier_id;phone\n");
        LocalDateTime now = LocalDateTime.now();
        for (Order o : stuck) {
            long mins = o.getStatusChangedAt() == null ? 0 : Duration.between(o.getStatusChangedAt(), now).toMinutes();
            sb.append(o.getId()).append(';')
              .append(o.getStatus()).append(';')
              .append(mins).append(';')
              .append(o.getRestaurantId()).append(';')
              .append(o.getCourierId() == null ? "" : o.getCourierId()).append(';')
              .append(escape(o.getPhone())).append('\n');
        }
        return new StuckReport(sb.toString().getBytes(StandardCharsets.UTF_8), stuck.size());
    }

    private String escape(String v) {
        if (v == null) return "";
        return v.replace(';', ',').replace('\n', ' ');
    }

    public static class StuckReport {
        private final byte[] body;
        private final int count;
        public StuckReport(byte[] body, int count) { this.body = body; this.count = count; }
        public byte[] getBody() { return body; }
        public int getCount() { return count; }
    }
}
