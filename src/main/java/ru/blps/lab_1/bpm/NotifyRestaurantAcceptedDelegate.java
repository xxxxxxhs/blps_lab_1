package ru.blps.lab_1.bpm;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.blps.lab_1.entity.Courier;
import ru.blps.lab_1.entity.CourierDecision;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderCourierDecision;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.repository.OrderCourierDecisionRepository;
import ru.blps.lab_1.repository.OrderRepository;
import ru.blps.lab_1.service.Recipient;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Component("notifyRestaurantAcceptedDelegate")
public class NotifyRestaurantAcceptedDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final OrderCourierDecisionRepository orderCourierDecisionRepository;
    private final OrderNotifier orderNotifier;

    public NotifyRestaurantAcceptedDelegate(
        OrderRepository orderRepository,
        OrderCourierDecisionRepository orderCourierDecisionRepository,
        OrderNotifier orderNotifier
    ) {
        this.orderRepository = orderRepository;
        this.orderCourierDecisionRepository = orderCourierDecisionRepository;
        this.orderNotifier = orderNotifier;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = ((Number) execution.getVariable("orderId")).longValue();
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        Courier courier = order.getCourier();
        if (courier == null) {
            throw new IllegalStateException("Cannot accept order without assigned courier");
        }
        orderCourierDecisionRepository.save(
            new OrderCourierDecision(order, courier, CourierDecision.ACCEPTED, LocalDateTime.now())
        );
        order.setStatus(OrderStatus.ACCEPTED);
        Order saved = orderRepository.save(order);
        orderNotifier.send(
            Recipient.CLIENT,
            saved.getClientId(),
            "Заказ #" + saved.getId() + " — курьер принял заказ."
        );
        String itemsText = saved.getItems().stream()
            .map(i -> i.getName() + " — " + i.getQuantity() + " шт.")
            .collect(Collectors.joining("\n"));
        orderNotifier.send(
            Recipient.RESTAURANT,
            saved.getRestaurantId(),
            "Заказ #" + saved.getId() + " для приготовления:\n" + itemsText
        );
    }
}
