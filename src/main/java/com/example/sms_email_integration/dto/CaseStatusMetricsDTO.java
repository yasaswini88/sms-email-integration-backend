package com.example.sms_email_integration.dto;

public class CaseStatusMetricsDTO {
    private long count;
    private String status;

    public CaseStatusMetricsDTO(long count, String status) {
        this.count = count;
        this.status = status;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}