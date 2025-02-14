package com.example.sms_email_integration.dto;

import java.time.LocalDateTime;

public class EmailIncomingDto {

    private Long id;
    private String clientPhoneNumber;
    private Long custiId;
    private LocalDateTime receivedAt;

    // We also include the Lawyer's email (not just the ID)
    private String lawyerEmail;

    private String direction;

    public EmailIncomingDto() {
    }

    public EmailIncomingDto(Long id, String clientPhoneNumber, Long custiId, 
                            LocalDateTime receivedAt, String lawyerEmail, String direction) {
        this.id = id;
        this.clientPhoneNumber = clientPhoneNumber;
        this.custiId = custiId;
        this.receivedAt = receivedAt;
        this.lawyerEmail = lawyerEmail;
        this.direction = direction;
    }

    // --- Getters/Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientPhoneNumber() {
        return clientPhoneNumber;
    }

    public void setClientPhoneNumber(String clientPhoneNumber) {
        this.clientPhoneNumber = clientPhoneNumber;
    }

    public Long getCustiId() {
        return custiId;
    }

    public void setCustiId(Long custiId) {
        this.custiId = custiId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getLawyerEmail() {
        return lawyerEmail;
    }

    public void setLawyerEmail(String lawyerEmail) {
        this.lawyerEmail = lawyerEmail;
    }


    public String getDirection() {
        return direction;
    }


    public void setDirection(String direction) {
        this.direction = direction;
    }

    
}
