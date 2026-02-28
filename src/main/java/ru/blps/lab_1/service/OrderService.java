package ru.blps.lab_1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.blps.lab_1.dto.OrderDto;
import ru.blps.lab_1.dto.OrderItemDto;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderItem;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.repository.OrderRepository;
import ru.blps.lab_1.util.RandomOrderDataGenerator;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Long COURIER_ID = 1L;

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder() {
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
            OrderItem item = new OrderItem(order, itemData.getName(), itemData.getQuantity());
            order.addItem(item);
        }
        return orderRepository.save(order);
    }

    public Order assignOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new IllegalStateException("Cannot assign order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.ASSIGNED);
        return orderRepository.save(order);
    }

    public Order acceptOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot accept order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.ACCEPTED);
        return orderRepository.save(order);
    }

    public Order rejectOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot reject order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.REJECTED);
        return orderRepository.save(order);
    }

    public Order pickupOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot pickup order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.PICKED_UP);
        return orderRepository.save(order);
    }

    public Order completeOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new IllegalStateException("Cannot complete order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.DELIVERED);
        return orderRepository.save(order);
    }

    public Order cancelOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        Set<OrderStatus> cancellable = Set.of(
            OrderStatus.NEW,
            OrderStatus.ASSIGNED,
            OrderStatus.ACCEPTED,
            OrderStatus.REJECTED,
            OrderStatus.PICKED_UP
        );
        if (!cancellable.contains(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
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

