package ru.blps.lab_1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blps.lab_1.dto.OrderDto;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder() {
        return orderService.createOrder();
    }

    @PostMapping("/{orderId}/assign")
    public Order assignOrder(@PathVariable Long orderId) {
        return orderService.assignOrder(orderId);
    }

    @PostMapping("/{orderId}/accept")
    public Order acceptOrder(@PathVariable Long orderId) {
        return orderService.acceptOrder(orderId);
    }

    @PostMapping("/{orderId}/reject")
    public Order rejectOrder(@PathVariable Long orderId) {
        return orderService.rejectOrder(orderId);
    }

    @PostMapping("/{orderId}/pickup")
    public Order pickupOrder(@PathVariable Long orderId) {
        return orderService.pickupOrder(orderId);
    }

    @PostMapping("/{orderId}/complete")
    public Order completeOrder(@PathVariable Long orderId) {
        return orderService.completeOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public Order cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/{orderId}")
    public OrderDto getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }
}

