package ru.blps.lab_1.eis;

import jakarta.resource.cci.ConnectionSpec;

public class OnlyOfficeConnectionSpec implements ConnectionSpec {

    private final String reportType;

    public OnlyOfficeConnectionSpec(String reportType) {
        this.reportType = reportType;
    }

    public String getReportType() {
        return reportType;
    }
}
