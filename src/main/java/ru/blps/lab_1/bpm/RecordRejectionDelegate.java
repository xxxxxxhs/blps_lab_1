package ru.blps.lab_1.bpm;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.blps.lab_1.entity.Courier;
import ru.blps.lab_1.entity.CourierDecision;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderCourierDecision;
import ru.blps.lab_1.repository.OrderCourierDecisionRepository;
import ru.blps.lab_1.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Set;

@Component("recordRejectionDelegate")
public class RecordRejectionDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final OrderCourierDecisionRepository orderCourierDecisionRepository;
    private final CourierAssignmentService courierAssignmentService;

    public RecordRejectionDelegate(
        OrderRepository orderRepository,
        OrderCourierDecisionRepository orderCourierDecisionRepository,
        CourierAssignmentService courierAssignmentService
    ) {
        this.orderRepository = orderRepository;
        this.orderCourierDecisionRepository = orderCourierDecisionRepository;
        this.courierAssignmentService = courierAssignmentService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = ((Number) execution.getVariable("orderId")).longValue();
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        Courier rejectedCourier = order.getCourier();
        if (rejectedCourier == null) {
            throw new IllegalStateException("Cannot reject order without assigned courier");
        }
        orderCourierDecisionRepository.save(
            new OrderCourierDecision(order, rejectedCourier, CourierDecision.REJECTED, LocalDateTime.now())
        );
        Set<Long> excludedCourierIds = courierAssignmentService.getRejectedCourierIds(orderId);
        Courier nextCourier = courierAssignmentService.selectRandomFreeCourier(excludedCourierIds)
            .orElseThrow(() -> new IllegalStateException("No free couriers available for reassignment"));
        order.setCourier(nextCourier);
        orderRepository.save(order);
        execution.setVariable("assignedCourierLogin", nextCourier.getLogin());
    }
}
