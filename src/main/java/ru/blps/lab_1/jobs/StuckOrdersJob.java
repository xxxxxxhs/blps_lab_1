package ru.blps.lab_1.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.blps.lab_1.entity.EisReport;
import ru.blps.lab_1.service.EisPublishService;
import ru.blps.lab_1.service.ReportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@DisallowConcurrentExecution
public class StuckOrdersJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(StuckOrdersJob.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    @Autowired private ReportService reportService;
    @Autowired private EisPublishService eisPublishService;
    @Value("${eis.reports.stuck-threshold-minutes:30}") private int thresholdMinutes;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Quartz: running StuckOrdersJob (threshold={}min)", thresholdMinutes);
        ReportService.StuckReport rep = reportService.buildStuckOrdersReport(thresholdMinutes);
        if (rep.getCount() == 0) {
            log.info("Quartz: no stuck orders, skipping publish");
            return;
        }
        String fileName = "stuck-" + LocalDateTime.now().format(FMT) + ".csv";
        EisReport report = eisPublishService.publish("STUCK", fileName, rep.getBody());
        log.warn("Quartz: stuck orders found ({}), report id={} url={}", rep.getCount(), report.getId(), report.getStorageUrl());
    }
}
