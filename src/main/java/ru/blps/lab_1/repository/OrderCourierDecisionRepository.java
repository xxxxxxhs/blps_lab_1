package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.blps.lab_1.entity.CourierDecision;
import ru.blps.lab_1.entity.OrderCourierDecision;

import java.util.List;

public interface OrderCourierDecisionRepository extends JpaRepository<OrderCourierDecision, Long> {
    List<OrderCourierDecision> findByOrder_IdAndDecision(Long orderId, CourierDecision decision);
}
