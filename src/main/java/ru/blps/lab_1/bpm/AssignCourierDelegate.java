package ru.blps.lab_1.bpm;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.blps.lab_1.entity.Courier;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.repository.OrderRepository;

import java.util.NoSuchElementException;
import java.util.Set;

@Component("assignCourierDelegate")
public class AssignCourierDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final CourierAssignmentService courierAssignmentService;

    public AssignCourierDelegate(OrderRepository orderRepository, CourierAssignmentService courierAssignmentService) {
        this.orderRepository = orderRepository;
        this.courierAssignmentService = courierAssignmentService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = ((Number) execution.getVariable("orderId")).longValue();
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        Courier courier = courierAssignmentService.selectRandomFreeCourier(Set.of())
            .orElseThrow(() -> new IllegalStateException("No free couriers available"));
        order.setCourier(courier);
        orderRepository.save(order);
        execution.setVariable("assignedCourierLogin", courier.getLogin());
    }
}
