package ru.blps.lab_1.service;

import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.blps.lab_1.dto.CreateOrderRequest;
import ru.blps.lab_1.entity.AppUser;
import ru.blps.lab_1.entity.Courier;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderItem;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.entity.Restaurant;
import ru.blps.lab_1.dto.OrderDto;
import ru.blps.lab_1.dto.OrderItemDto;
import ru.blps.lab_1.repository.AppUserRepository;
import ru.blps.lab_1.repository.CourierRepository;
import ru.blps.lab_1.repository.OrderRepository;
import ru.blps.lab_1.repository.RestaurantRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String PROCESS_KEY = "orderDeliveryProcess";
    private static final String CANCEL_MESSAGE = "cancelOrder";

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final AppUserRepository appUserRepository;
    private final RestaurantRepository restaurantRepository;
    private final NotificationService notificationService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final TransactionTemplate transactionTemplate;

    @Value("${telegram.chatId:${telegram.chat-id:}}")
    private String telegramChatId;

    public OrderService(
        OrderRepository orderRepository,
        CourierRepository courierRepository,
        AppUserRepository appUserRepository,
        RestaurantRepository restaurantRepository,
        PlatformTransactionManager transactionManager,
        NotificationService notificationService,
        RuntimeService runtimeService,
        TaskService taskService
    ) {
        this.orderRepository = orderRepository;
        this.courierRepository = courierRepository;
        this.appUserRepository = appUserRepository;
        this.restaurantRepository = restaurantRepository;
        this.notificationService = notificationService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    public void logTelegramConfig() {
        boolean enabled = telegramChatId != null && !telegramChatId.isBlank();
        log.info("Telegram notifications: {}", enabled ? "enabled (chat-id set)" : "disabled (chat-id empty)");
    }

    public OrderDto createOrder(CreateOrderRequest request) {
        return inTransaction(() -> {
            String login = currentUsername();
            AppUser client = appUserRepository.findByUsername(login)
                .orElseThrow(() -> new NoSuchElementException("No client profile for login: " + login));
            Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found: " + request.getRestaurantId()));
            Order order = new Order(
                client,
                null,
                restaurant,
                restaurant.getAddress(),
                request.getCity(),
                request.getDeliveryAddress(),
                request.getPhone(),
                request.getComment(),
                OrderStatus.NEW
            );
            request.getItems().forEach(itemData -> {
                double price = itemData.getPrice() == null ? 0.0 : itemData.getPrice();
                OrderItem item = new OrderItem(
                    order,
                    itemData.getName(),
                    itemData.getQuantity(),
                    price
                );
                order.addItem(item);
            });
            Order saved = orderRepository.save(order);
            if (telegramChatId != null && !telegramChatId.isBlank()) {
                notificationService.send(
                    Recipient.CLIENT,
                    saved.getClientId(),
                    telegramChatId,
                    "Ваш заказ создан!\n" + saved.toString()
                );
            }
            runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                saved.getId().toString(),
                Map.of("orderId", saved.getId(), "restaurantLogin", restaurant.getLogin())
            );
            return toDto(orderRepository.findById(saved.getId()).orElse(saved));
        });
    }

    public OrderDto acceptOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            completeTask(orderId, "task_courierDecision", Map.of("courierDecision", "ACCEPTED"));
            return toDto(findOrderOrThrow(orderId));
        });
    }

    public OrderDto rejectOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            completeTask(orderId, "task_courierDecision", Map.of("courierDecision", "REJECTED"));
            return toDto(findOrderOrThrow(orderId));
        });
    }

    public OrderDto cookOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertRestaurantOfOrder(order);
            completeTask(orderId, "task_restaurantCook", Map.of());
            return toDto(findOrderOrThrow(orderId));
        });
    }

    public OrderDto pickupOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            completeTask(orderId, "task_courierPickup", Map.of());
            return toDto(findOrderOrThrow(orderId));
        });
    }

    public OrderDto completeOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            completeTask(orderId, "task_courierDeliver", Map.of());
            return toDto(findOrderOrThrow(orderId));
        });
    }

    public OrderDto cancelOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertClientOwnsOrder(order);
            long affected = runtimeService.createMessageCorrelation(CANCEL_MESSAGE)
                .processInstanceBusinessKey(orderId.toString())
                .correlateAllWithResult()
                .size();
            if (affected == 0) {
                throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
            }
            return toDto(findOrderOrThrow(orderId));
        });
    }

    public OrderDto getOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertOrderVisible(order);
            return toDto(order);
        });
    }

    private void completeTask(Long orderId, String taskDefinitionKey, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
            .processInstanceBusinessKey(orderId.toString())
            .taskDefinitionKey(taskDefinitionKey)
            .singleResult();
        if (task == null) {
            throw new IllegalStateException("No active task '" + taskDefinitionKey + "' for order: " + orderId);
        }
        taskService.complete(task.getId(), variables);
    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    private <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return name;
    }

    private void assertOrderVisible(Order order) {
        String login = currentUsername();
        if (appUserRepository.findByUsername(login).filter(u -> u.getId().equals(order.getClientId())).isPresent()) {
            return;
        }
        if (courierRepository.findByLogin(login).filter(c -> c.getId().equals(order.getCourierId())).isPresent()) {
            return;
        }
        if (restaurantRepository.findByLogin(login).filter(r -> r.getId().equals(order.getRestaurantId())).isPresent()) {
            return;
        }
        throw new AccessDeniedException("Order not accessible for this user");
    }

    private void assertClientOwnsOrder(Order order) {
        String login = currentUsername();
        AppUser client = appUserRepository.findByUsername(login)
            .orElseThrow(() -> new AccessDeniedException("Only the client can cancel this order"));
        if (!client.getId().equals(order.getClientId())) {
            throw new AccessDeniedException("Not your order");
        }
    }

    private void assertAssignedCourier(Order order) {
        String login = currentUsername();
        Courier courier = courierRepository.findByLogin(login)
            .orElseThrow(() -> new AccessDeniedException("Only the assigned courier can perform this action"));
        Long assignedId = order.getCourierId();
        if (assignedId == null || !assignedId.equals(courier.getId())) {
            throw new AccessDeniedException("Not your assigned order");
        }
    }

    private void assertRestaurantOfOrder(Order order) {
        String login = currentUsername();
        Restaurant restaurant = restaurantRepository.findByLogin(login)
            .orElseThrow(() -> new AccessDeniedException("Only the restaurant can perform this action"));
        if (!restaurant.getId().equals(order.getRestaurantId())) {
            throw new AccessDeniedException("Not your restaurant order");
        }
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
