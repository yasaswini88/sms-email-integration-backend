package com.example.sms_email_integration.dto;

public class CaseTypeMetricsDTO {
    private long count;
    private String caseType;

    public CaseTypeMetricsDTO(long count, String caseType) {
        this.count = count;
        this.caseType = caseType;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }
}