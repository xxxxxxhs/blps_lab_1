package ru.blps.lab_1.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.blps.lab_1.entity.EisReport;
import ru.blps.lab_1.service.EisPublishService;
import ru.blps.lab_1.service.ReportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@DisallowConcurrentExecution
public class HourlyActiveOrdersJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(HourlyActiveOrdersJob.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    @Autowired private ReportService reportService;
    @Autowired private EisPublishService eisPublishService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Quartz: running HourlyActiveOrdersJob");
        byte[] body = reportService.buildActiveOrdersSnapshot();
        String fileName = "active-" + LocalDateTime.now().format(FMT) + ".csv";
        EisReport report = eisPublishService.publish("HOURLY_ACTIVE", fileName, body);
        log.info("Quartz: hourly active orders published id={} url={}", report.getId(), report.getStorageUrl());
    }
}
