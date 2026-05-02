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

import java.time.LocalDate;

@DisallowConcurrentExecution
public class DailyReportJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DailyReportJob.class);

    @Autowired private ReportService reportService;
    @Autowired private EisPublishService eisPublishService;

    @Override
    public void execute(JobExecutionContext context) {
        LocalDate day = LocalDate.now();
        log.info("Quartz: running DailyReportJob for {}", day);
        byte[] body = reportService.buildDailyReport(day);
        String fileName = "daily-" + day + ".csv";
        EisReport report = eisPublishService.publish("DAILY", fileName, body);
        log.info("Quartz: daily report published id={} url={}", report.getId(), report.getStorageUrl());
    }
}
