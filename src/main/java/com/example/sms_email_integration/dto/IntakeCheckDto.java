package com.example.sms_email_integration.dto;

public class IntakeCheckDto {
    private boolean complete;
    private String caseType;

    public IntakeCheckDto() {}
    public IntakeCheckDto(boolean complete, String caseType) {
        this.complete = complete;
        this.caseType = caseType;
    }

    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }

    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
}
