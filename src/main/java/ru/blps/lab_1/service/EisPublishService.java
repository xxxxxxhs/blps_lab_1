package ru.blps.lab_1.service;

import jakarta.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.blps.lab_1.eis.OnlyOfficeConnection;
import ru.blps.lab_1.eis.OnlyOfficeConnectionFactory;
import ru.blps.lab_1.eis.OnlyOfficeConnectionSpec;
import ru.blps.lab_1.eis.PublishedDocument;
import ru.blps.lab_1.entity.EisReport;
import ru.blps.lab_1.repository.EisReportRepository;

@Service
public class EisPublishService {

    private static final Logger log = LoggerFactory.getLogger(EisPublishService.class);

    private final OnlyOfficeConnectionFactory connectionFactory;
    private final EisReportRepository reportRepository;
    private final String instanceNum;

    public EisPublishService(OnlyOfficeConnectionFactory connectionFactory,
                             EisReportRepository reportRepository,
                             @Value("${spring.application.instance-num:0}") String instanceNum) {
        this.connectionFactory = connectionFactory;
        this.reportRepository = reportRepository;
        this.instanceNum = instanceNum;
    }

    public EisReport publish(String reportType, String fileName, byte[] body) {
        OnlyOfficeConnection conn = null;
        try {
            conn = connectionFactory.getConnection(new OnlyOfficeConnectionSpec(reportType));
            PublishedDocument pub = conn.publishDocument(fileName, body);
            EisReport report = new EisReport(reportType, pub.getFileName(), pub.getStorageUrl(), pub.getSizeBytes(), instanceNum);
            return reportRepository.save(report);
        } catch (ResourceException e) {
            log.error("Failed to publish to EIS: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (ResourceException ignored) {}
        }
    }
}
