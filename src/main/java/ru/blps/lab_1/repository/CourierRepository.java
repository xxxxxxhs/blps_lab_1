package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.blps.lab_1.entity.Courier;

import java.util.Optional;

public interface CourierRepository extends JpaRepository<Courier, Long> {
    Optional<Courier> findByLogin(String login);

    boolean existsByLogin(String login);
}
