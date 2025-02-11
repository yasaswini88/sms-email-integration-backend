package com.example.sms_email_integration.dto;

public class CustomerDto {

    private Long custiId;
    private String custMail;
    private String custName;
    private String twilioNumber;
    private String EnabledAssignedLawyer;

    public CustomerDto() {
    }

    public CustomerDto(Long custiId, String custMail, String custName, String twilioNumber, String EnabledAssignedLawyer) {
        this.custiId = custiId;
        this.custMail = custMail;
        this.custName = custName;
        this.twilioNumber = twilioNumber;
        this.EnabledAssignedLawyer = EnabledAssignedLawyer;
    }

    public Long getCustiId() {
        return custiId;
    }

    public void setCustiId(Long custiId) {
        this.custiId = custiId;
    }

    public String getCustMail() {
        return custMail;
    }

    public void setCustMail(String custMail) {
        this.custMail = custMail;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getTwilioNumber() {
        return twilioNumber;
    }

    public void setTwilioNumber(String twilioNumber) {
        this.twilioNumber = twilioNumber;
    }

    public String getEnabledAssignedLawyer() {
        return EnabledAssignedLawyer;
    }   

    public void setEnabledAssignedLawyer(String EnabledAssignedLawyer) {
        this.EnabledAssignedLawyer = EnabledAssignedLawyer;
    }

    
}
