package com.example.sms_email_integration.dto;

import com.example.sms_email_integration.entity.Customer;

public class FirmLawyerDto {

    private Long lawyerId;
    private String lawyerName;
    private String lawyerMail;
    private Customer firm; // Include the entire Customer object

    // Constructor
    public FirmLawyerDto(Long lawyerId, String lawyerName, String lawyerMail, Customer firm) {
        this.lawyerId = lawyerId;
        this.lawyerName = lawyerName;
        this.lawyerMail = lawyerMail;
        this.firm = firm;
    }

    // Getters and Setters
    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
    }

    public String getLawyerName() {
        return lawyerName;
    }

    public void setLawyerName(String lawyerName) {
        this.lawyerName = lawyerName;
    }

    public String getLawyerMail() {
        return lawyerMail;
    }

    public void setLawyerMail(String lawyerMail) {
        this.lawyerMail = lawyerMail;
    }

    public Customer getFirm() {
        return firm;
    }

    public void setFirm(Customer firm) {
        this.firm = firm;
    }
}
