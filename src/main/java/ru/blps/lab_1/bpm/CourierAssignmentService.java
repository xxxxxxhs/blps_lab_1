package ru.blps.lab_1.bpm;

import org.springframework.stereotype.Component;
import ru.blps.lab_1.entity.Courier;
import ru.blps.lab_1.entity.CourierDecision;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderStatus;
import ru.blps.lab_1.repository.CourierRepository;
import ru.blps.lab_1.repository.OrderCourierDecisionRepository;
import ru.blps.lab_1.repository.OrderRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CourierAssignmentService {

    private static final Set<OrderStatus> FREE_COURIER_LAST_ORDER_STATUSES =
        Set.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED);

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final OrderCourierDecisionRepository orderCourierDecisionRepository;
    private final Random random = new Random();

    public CourierAssignmentService(
        CourierRepository courierRepository,
        OrderRepository orderRepository,
        OrderCourierDecisionRepository orderCourierDecisionRepository
    ) {
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
        this.orderCourierDecisionRepository = orderCourierDecisionRepository;
    }

    public Optional<Courier> selectRandomFreeCourier(Set<Long> excludedCourierIds) {
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

    public Set<Long> getRejectedCourierIds(Long orderId) {
        return new HashSet<>(
            orderCourierDecisionRepository.findByOrder_IdAndDecision(orderId, CourierDecision.REJECTED)
                .stream()
                .map(decision -> decision.getCourier().getId())
                .collect(Collectors.toSet())
        );
    }

    private boolean isCourierFree(Courier courier) {
        Optional<Order> lastOrder = orderRepository.findTopByCourier_IdOrderByIdDesc(courier.getId());
        if (lastOrder.isEmpty()) {
            return true;
        }
        return FREE_COURIER_LAST_ORDER_STATUSES.contains(lastOrder.get().getStatus());
    }
}
