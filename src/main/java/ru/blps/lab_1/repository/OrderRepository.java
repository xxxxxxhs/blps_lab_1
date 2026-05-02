package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.blps.lab_1.entity.Order;
import ru.blps.lab_1.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findTopByCourier_IdOrderByIdDesc(Long courierId);

    List<Order> findAllByStatusIn(List<OrderStatus> statuses);

    @Query("select o from Order o where o.status in :statuses and o.statusChangedAt < :threshold")
    List<Order> findStuckOrders(@Param("statuses") List<OrderStatus> statuses,
                                @Param("threshold") LocalDateTime threshold);

    @Query("select distinct o from Order o left join fetch o.items where o.createdAt >= :from and o.createdAt < :to")
    List<Order> findCreatedBetween(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
