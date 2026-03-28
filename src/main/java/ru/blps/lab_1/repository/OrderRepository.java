package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.blps.lab_1.entity.Order;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findTopByCourier_IdOrderByIdDesc(Long courierId);
}