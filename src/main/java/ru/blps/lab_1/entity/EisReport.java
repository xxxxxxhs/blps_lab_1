package ru.blps.lab_1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eis_reports")
public class EisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String reportType;

    @Column(nullable = false, length = 256)
    private String fileName;

    @Column(nullable = false, length = 512)
    private String storageUrl;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false, length = 64)
    private String generatedByInstance;

    public EisReport() {}

    public EisReport(String reportType, String fileName, String storageUrl, Long sizeBytes, String generatedByInstance) {
        this.reportType = reportType;
        this.fileName = fileName;
        this.storageUrl = storageUrl;
        this.sizeBytes = sizeBytes;
        this.generatedByInstance = generatedByInstance;
    }

    @PrePersist
    void onCreate() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getReportType() { return reportType; }
    public String getFileName() { return fileName; }
    public String getStorageUrl() { return storageUrl; }
    public Long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getGeneratedByInstance() { return generatedByInstance; }
}
