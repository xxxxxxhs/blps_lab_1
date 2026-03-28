package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.blps.lab_1.entity.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
