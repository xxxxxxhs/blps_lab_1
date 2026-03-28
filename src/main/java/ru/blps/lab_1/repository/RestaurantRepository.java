package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.blps.lab_1.entity.Restaurant;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByLogin(String login);

    boolean existsByLogin(String login);
}
