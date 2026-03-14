package ru.blps.lab_1.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.blps.lab_1.dto.OrderDto;
import ru.blps.lab_1.dto.OrderItemDto;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderItem;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.repository.OrderRepository;
import ru.blps.lab_1.util.RandomOrderDataGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Long COURIER_ID = 1L;

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Value("${telegram.chat-id:}")
    private String telegramChatId;

    public OrderService(OrderRepository orderRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void logTelegramConfig() {
        boolean enabled = telegramChatId != null && !telegramChatId.isBlank();
        log.info("Telegram notifications: {}", enabled ? "enabled (chat-id set)" : "disabled (chat-id empty)");
    }

    public OrderDto createOrder() {
        Order order = new Order(
            RandomOrderDataGenerator.randomClientId(),
            COURIER_ID,
            RandomOrderDataGenerator.randomRestaurantId(),
            RandomOrderDataGenerator.randomRestaurantAddress(),
            RandomOrderDataGenerator.randomCity(),
            RandomOrderDataGenerator.randomDeliveryAddress(),
            RandomOrderDataGenerator.randomPhone(),
            RandomOrderDataGenerator.randomComment(),
            OrderStatus.NEW
        );
        for (RandomOrderDataGenerator.Item itemData : RandomOrderDataGenerator.randomItems()) {
            OrderItem item = new OrderItem(
                order,
                itemData.getName(),
                itemData.getQuantity(),
                itemData.getPrice()
            );
            order.addItem(item);
        }
        Order saved = orderRepository.save(order);
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            notificationService.send(Recipient.CLIENT, telegramChatId, "Ваш заказ создан!\n" + saved.toString());
        }
        return toDto(saved);
    }

    public OrderDto assignOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new IllegalStateException("Cannot assign order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.ASSIGNED);
        Order saved = orderRepository.save(order);
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            notificationService.send(
                Recipient.COURIER,
                telegramChatId,
                "Вам назначен заказ #" + saved.getId() + "\n" +
                "Адрес ресторана: " + saved.getRestaurantAddress() + "\n" +
                "Телефон клиента: " + saved.getPhone() + "\n" +
                "Адрес доставки: " + saved.getDeliveryAddress() + "\n" +
                "Комментарий: " + saved.getComment() + "\n"
            );
        }
        return toDto(saved);
    }

    public OrderDto acceptOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot accept order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.ACCEPTED);
        Order saved = orderRepository.save(order);
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            notificationService.send(
                Recipient.CLIENT,
                telegramChatId,
                "Заказ #" + saved.getId() + " — курьер принял заказ."
            );
            String itemsText = saved.getItems().stream()
                .map(i -> i.getName() + " — " + i.getQuantity() + " шт.")
                .collect(Collectors.joining("\n"));
            notificationService.send(
                Recipient.RESTAURANT,
                telegramChatId,
                "Заказ #" + saved.getId() + " для приготовления:\n" + itemsText
            );
        }
        return toDto(saved);
    }

    public OrderDto rejectOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot reject order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.REJECTED);
        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    public OrderDto cookOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot cook order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.COOKED);
        Order saved = orderRepository.save(order);
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            notificationService.send(
                Recipient.COURIER,
                telegramChatId,
                "Заказ #" + saved.getId() + " готов к выдаче в ресторане."
            );
            notificationService.send(
                Recipient.CLIENT,
                telegramChatId,
                "Заказ #" + saved.getId() + " приготовлен, курьер скоро заберёт его."
            );
        }
        return toDto(saved);
    }

    public OrderDto pickupOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.COOKED) {
            throw new IllegalStateException("Cannot pickup order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.PICKED_UP);
        Order saved = orderRepository.save(order);
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            notificationService.send(
                Recipient.CLIENT,
                telegramChatId,
                "Заказ #" + saved.getId() + " курьер забрал, едет к вам."
            );
        }
        return toDto(saved);
    }

    public OrderDto completeOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new IllegalStateException("Cannot complete order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.DELIVERED);
        Order saved = orderRepository.save(order);
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            notificationService.send(
                Recipient.CLIENT,
                telegramChatId,
                "Заказ #" + saved.getId() + " доставлен."
            );
            notificationService.send(
                Recipient.COURIER,
                telegramChatId,
                "Заказ #" + saved.getId() + " доставлен."
            );
        }
        return toDto(saved);
    }

    public OrderDto cancelOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        Set<OrderStatus> cancellable = Set.of(
            OrderStatus.NEW,
            OrderStatus.ASSIGNED,
            OrderStatus.ACCEPTED,
            OrderStatus.REJECTED,
            OrderStatus.COOKED,
            OrderStatus.PICKED_UP
        );
        if (!cancellable.contains(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }
        boolean notifyRestaurant = (order.getStatus() != OrderStatus.COOKED && order.getStatus() != OrderStatus.PICKED_UP);
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            notificationService.send(Recipient.CLIENT, telegramChatId, "Заказ #" + saved.getId() + " отменён.");
            notificationService.send(Recipient.COURIER, telegramChatId, "Заказ #" + saved.getId() + " отменён.");
            if (notifyRestaurant) {
                notificationService.send(Recipient.RESTAURANT, telegramChatId, "Заказ #" + saved.getId() + " отменён.");
            }
        }
        return toDto(saved);
    }

    public OrderDto getOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        return toDto(order);
    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems()
            .stream()
            .map(this::toItemDto)
            .collect(Collectors.toList());

        return new OrderDto(
            order.getId(),
            order.getStatus().name(),
            order.getRestaurantAddress(),
            order.getCity(),
            order.getDeliveryAddress(),
            order.getPhone(),
            order.getComment(),
            itemDtos
        );
    }

    private OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
            item.getName(),
            item.getQuantity()
        );
    }
}

