package ru.blps.lab_1.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.blps.lab_1.dto.CreateOrderRequest;
import ru.blps.lab_1.entity.Courier;
import ru.blps.lab_1.entity.CourierDecision;
import ru.blps.lab_1.entity.AppUser;
import ru.blps.lab_1.entity.Restaurant;
import ru.blps.lab_1.dto.OrderDto;
import ru.blps.lab_1.dto.OrderItemDto;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderCourierDecision;
import ru.blps.lab_1.entity.OrderItem;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.repository.AppUserRepository;
import ru.blps.lab_1.repository.CourierRepository;
import ru.blps.lab_1.repository.OrderCourierDecisionRepository;
import ru.blps.lab_1.repository.OrderRepository;
import ru.blps.lab_1.repository.RestaurantRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Set<OrderStatus> FREE_COURIER_LAST_ORDER_STATUSES = Set.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final AppUserRepository appUserRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderCourierDecisionRepository orderCourierDecisionRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final Random random = new Random();

    @Value("${telegram.chatId:${telegram.chat-id:}}")
    private String telegramChatId;

    public OrderService(
        OrderRepository orderRepository,
        CourierRepository courierRepository,
        AppUserRepository appUserRepository,
        RestaurantRepository restaurantRepository,
        OrderCourierDecisionRepository orderCourierDecisionRepository,
        PlatformTransactionManager transactionManager,
        NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.courierRepository = courierRepository;
        this.appUserRepository = appUserRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderCourierDecisionRepository = orderCourierDecisionRepository;
        this.notificationService = notificationService;
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
            Courier courier = selectRandomFreeCourier(Set.of())
                .orElseThrow(() -> new IllegalStateException("No free couriers available"));
            Order order = new Order(
                client,
                courier,
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
            return toDto(saved);
        });
    }

    public OrderDto acceptOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            if (order.getStatus() != OrderStatus.NEW) {
                throw new IllegalStateException("Cannot accept order in status: " + order.getStatus());
            }
            Courier courier = order.getCourier();
            if (courier == null) {
                throw new IllegalStateException("Cannot accept order without assigned courier");
            }
            orderCourierDecisionRepository.save(
                new OrderCourierDecision(order, courier, CourierDecision.ACCEPTED, LocalDateTime.now())
            );
            order.setStatus(OrderStatus.ACCEPTED);
            Order saved = orderRepository.save(order);
            if (telegramChatId != null && !telegramChatId.isBlank()) {
                notificationService.send(
                    Recipient.CLIENT,
                    saved.getClientId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " — курьер принял заказ."
                );
                String itemsText = saved.getItems().stream()
                    .map(i -> i.getName() + " — " + i.getQuantity() + " шт.")
                    .collect(Collectors.joining("\n"));
                notificationService.send(
                    Recipient.RESTAURANT,
                    saved.getRestaurantId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " для приготовления:\n" + itemsText
                );
            }
            return toDto(saved);
        });
    }

    public OrderDto rejectOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            if (order.getStatus() != OrderStatus.NEW) {
                throw new IllegalStateException("Cannot reject order in status: " + order.getStatus());
            }
            Courier rejectedCourier = order.getCourier();
            if (rejectedCourier == null) {
                throw new IllegalStateException("Cannot reject order without assigned courier");
            }
            orderCourierDecisionRepository.save(
                new OrderCourierDecision(order, rejectedCourier, CourierDecision.REJECTED, LocalDateTime.now())
            );
            Set<Long> excludedCourierIds = getRejectedCourierIds(orderId);
            Courier nextCourier = selectRandomFreeCourier(excludedCourierIds)
                .orElseThrow(() -> new IllegalStateException("No free couriers available for reassignment"));
            order.setCourier(nextCourier);
            Order saved = orderRepository.save(order);
            return toDto(saved);
        });
    }

    public OrderDto cookOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertRestaurantOfOrder(order);
            if (order.getStatus() != OrderStatus.ACCEPTED) {
                throw new IllegalStateException("Cannot cook order in status: " + order.getStatus());
            }
            order.setStatus(OrderStatus.COOKED);
            Order saved = orderRepository.save(order);
            if (telegramChatId != null && !telegramChatId.isBlank()) {
                notificationService.send(
                    Recipient.COURIER,
                    saved.getCourierId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " готов к выдаче в ресторане."
                );
                notificationService.send(
                    Recipient.CLIENT,
                    saved.getClientId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " приготовлен, курьер скоро заберёт его."
                );
            }
            return toDto(saved);
        });
    }

    public OrderDto pickupOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            if (order.getStatus() != OrderStatus.COOKED) {
                throw new IllegalStateException("Cannot pickup order in status: " + order.getStatus());
            }
            order.setStatus(OrderStatus.PICKED_UP);
            Order saved = orderRepository.save(order);
            if (telegramChatId != null && !telegramChatId.isBlank()) {
                notificationService.send(
                    Recipient.CLIENT,
                    saved.getClientId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " курьер забрал, едет к вам."
                );
            }
            return toDto(saved);
        });
    }

    public OrderDto completeOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertAssignedCourier(order);
            if (order.getStatus() != OrderStatus.PICKED_UP) {
                throw new IllegalStateException("Cannot complete order in status: " + order.getStatus());
            }
            order.setStatus(OrderStatus.DELIVERED);
            Order saved = orderRepository.save(order);
            if (telegramChatId != null && !telegramChatId.isBlank()) {
                notificationService.send(
                    Recipient.CLIENT,
                    saved.getClientId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " доставлен."
                );
                notificationService.send(
                    Recipient.COURIER,
                    saved.getCourierId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " доставлен."
                );
            }
            return toDto(saved);
        });
    }

    public OrderDto cancelOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertClientOwnsOrder(order);
            Set<OrderStatus> cancellable = Set.of(
                OrderStatus.NEW,
                OrderStatus.ACCEPTED,
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
                notificationService.send(
                    Recipient.CLIENT,
                    saved.getClientId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " отменён."
                );
                notificationService.send(
                    Recipient.COURIER,
                    saved.getCourierId(),
                    telegramChatId,
                    "Заказ #" + saved.getId() + " отменён."
                );
                if (notifyRestaurant) {
                    notificationService.send(
                        Recipient.RESTAURANT,
                        saved.getRestaurantId(),
                        telegramChatId,
                        "Заказ #" + saved.getId() + " отменён."
                    );
                }
            }
            return toDto(saved);
        });
    }

    public OrderDto getOrder(Long orderId) {
        return inTransaction(() -> {
            Order order = findOrderOrThrow(orderId);
            assertOrderVisible(order);
            return toDto(order);
        });
    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    private Optional<Courier> selectRandomFreeCourier(Set<Long> excludedCourierIds) {
        List<Courier> freeCouriers = courierRepository.findAll()
            .stream()
            .filter(c -> c.getLogin() != null && !c.getLogin().isBlank())
            .filter(courier -> !excludedCourierIds.contains(courier.getId()))
            .filter(this::isCourierFree)
            .collect(Collectors.toList());
        if (freeCouriers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(freeCouriers.get(random.nextInt(freeCouriers.size())));
    }

    private boolean isCourierFree(Courier courier) {
        Optional<Order> lastOrder = orderRepository.findTopByCourier_IdOrderByIdDesc(courier.getId());
        if (lastOrder.isEmpty()) {
            return true;
        }
        return FREE_COURIER_LAST_ORDER_STATUSES.contains(lastOrder.get().getStatus());
    }

    private Set<Long> getRejectedCourierIds(Long orderId) {
        return new HashSet<>(
            orderCourierDecisionRepository.findByOrder_IdAndDecision(orderId, CourierDecision.REJECTED)
                .stream()
                .map(decision -> decision.getCourier().getId())
                .collect(Collectors.toSet())
        );
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

