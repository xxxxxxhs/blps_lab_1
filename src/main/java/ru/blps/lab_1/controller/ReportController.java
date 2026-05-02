package ru.blps.lab_1.controller;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blps.lab_1.entity.EisReport;
import ru.blps.lab_1.repository.EisReportRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final EisReportRepository repository;
    private final Scheduler scheduler;

    public ReportController(EisReportRepository repository, Scheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    @GetMapping
    public List<EisReport> list() {
        return repository.findAllByOrderByGeneratedAtDesc();
    }

    @PostMapping("/run/{name}")
    public Map<String, Object> runNow(@PathVariable String name) throws SchedulerException {
        JobKey key = switch (name) {
            case "daily" -> JobKey.jobKey("dailyReportJob");
            case "hourly" -> JobKey.jobKey("hourlyActiveOrdersJob");
            case "stuck" -> JobKey.jobKey("stuckOrdersJob");
            default -> throw new IllegalArgumentException("Unknown job: " + name);
        };
        scheduler.triggerJob(key);
        return Map.of("triggered", name);
    }
}
