package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.blps.lab_1.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}