package ru.blps.lab_1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blps.lab_1.dto.OrderDto;
import ru.blps.lab_1.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderDto createOrder() {
        return orderService.createOrder();
    }

    @PostMapping("/{orderId}/assign")
    public OrderDto assignOrder(@PathVariable Long orderId) {
        return orderService.assignOrder(orderId);
    }

    @PostMapping("/{orderId}/accept")
    public OrderDto acceptOrder(@PathVariable Long orderId) {
        return orderService.acceptOrder(orderId);
    }

    @PostMapping("/{orderId}/reject")
    public OrderDto rejectOrder(@PathVariable Long orderId) {
        return orderService.rejectOrder(orderId);
    }

    @PostMapping("/{orderId}/cook")
    public OrderDto cookOrder(@PathVariable Long orderId) {
        return orderService.cookOrder(orderId);
    }

    @PostMapping("/{orderId}/pickup")
    public OrderDto pickupOrder(@PathVariable Long orderId) {
        return orderService.pickupOrder(orderId);
    }

    @PostMapping("/{orderId}/complete")
    public OrderDto completeOrder(@PathVariable Long orderId) {
        return orderService.completeOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderDto cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/{orderId}")
    public OrderDto getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }
}

