package ru.blps.lab_1.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blps.lab_1.dto.CreateOrderRequest;
import ru.blps.lab_1.dto.OrderDto;
import ru.blps.lab_1.security.OrderPrivileges;
import ru.blps.lab_1.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_CREATE + "')")
    public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PostMapping("/{orderId}/accept")
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_ACCEPT + "')")
    public OrderDto acceptOrder(@PathVariable Long orderId) {
        return orderService.acceptOrder(orderId);
    }

    @PostMapping("/{orderId}/reject")
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_REJECT + "')")
    public OrderDto rejectOrder(@PathVariable Long orderId) {
        return orderService.rejectOrder(orderId);
    }

    @PostMapping("/{orderId}/cook")
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_COOK + "')")
    public OrderDto cookOrder(@PathVariable Long orderId) {
        return orderService.cookOrder(orderId);
    }

    @PostMapping("/{orderId}/pickup")
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_PICKUP + "')")
    public OrderDto pickupOrder(@PathVariable Long orderId) {
        return orderService.pickupOrder(orderId);
    }

    @PostMapping("/{orderId}/complete")
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_COMPLETE + "')")
    public OrderDto completeOrder(@PathVariable Long orderId) {
        return orderService.completeOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_CANCEL + "')")
    public OrderDto cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('" + OrderPrivileges.ORDER_READ + "')")
    public OrderDto getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }
}

