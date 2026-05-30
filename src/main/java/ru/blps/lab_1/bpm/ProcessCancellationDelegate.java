package ru.blps.lab_1.bpm;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.repository.OrderRepository;
import ru.blps.lab_1.service.Recipient;

import java.util.NoSuchElementException;

@Component("processCancellationDelegate")
public class ProcessCancellationDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final OrderNotifier orderNotifier;

    public ProcessCancellationDelegate(OrderRepository orderRepository, OrderNotifier orderNotifier) {
        this.orderRepository = orderRepository;
        this.orderNotifier = orderNotifier;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = ((Number) execution.getVariable("orderId")).longValue();
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        boolean notifyRestaurant = order.getStatus() != OrderStatus.COOKED
            && order.getStatus() != OrderStatus.PICKED_UP;
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        orderNotifier.send(
            Recipient.CLIENT,
            saved.getClientId(),
            "Заказ #" + saved.getId() + " отменён."
        );
        orderNotifier.send(
            Recipient.COURIER,
            saved.getCourierId(),
            "Заказ #" + saved.getId() + " отменён."
        );
        if (notifyRestaurant) {
            orderNotifier.send(
                Recipient.RESTAURANT,
                saved.getRestaurantId(),
                "Заказ #" + saved.getId() + " отменён."
            );
        }
    }
}
