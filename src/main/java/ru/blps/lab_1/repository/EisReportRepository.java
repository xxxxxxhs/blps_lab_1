package ru.blps.lab_1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.blps.lab_1.entity.EisReport;

import java.util.List;
import java.util.Optional;

public interface EisReportRepository extends JpaRepository<EisReport, Long> {
    List<EisReport> findAllByOrderByGeneratedAtDesc();
    Optional<EisReport> findByFileName(String fileName);
}
