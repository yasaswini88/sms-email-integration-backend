package com.example.sms_email_integration.dto;

public class CustomerDto {

    private Long custiId;
    private String custMail;
    private String custName;
    private String twilioNumber;
    private String EnabledAssignedLawyer;
     private String firmAddress;
    private String city;
    private String state;
    private String zipCode;


    public CustomerDto() {
    }

    public CustomerDto(Long custiId, String custMail, String custName, String twilioNumber, String EnabledAssignedLawyer, String firmAddress, String city, String state, String zipCode) {
        this.custiId = custiId;
        this.custMail = custMail;
        this.custName = custName;
        this.twilioNumber = twilioNumber;
        this.EnabledAssignedLawyer = EnabledAssignedLawyer;
        this.firmAddress = firmAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;

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


    public String getFirmAddress() {
        return firmAddress;
    }

    public void setFirmAddress(String firmAddress) {
        this.firmAddress = firmAddress;
    }


    public String getCity() {
        return city;
    }


    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }


    public void setState(String state) {
        this.state = state;
    }


    public String getZipCode() {
        return zipCode;
    }



    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }


    

    
}
