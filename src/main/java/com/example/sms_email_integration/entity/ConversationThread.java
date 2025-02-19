package com.example.sms_email_integration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_thread")
public class ConversationThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_thread_id")
    private Long conversationThreadId;

    @Column(name = "thread_id")
    private String threadId;   

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "to_number")           
    private String toNumber;  

     @Column(name = "status")
    private String status; 

    @Column(name = "case_type")
    private String caseType;

    @Column(name = "custi_id", nullable = true)
    private Long custiId;

    

    // Constructors
    public ConversationThread() {
    }

    public ConversationThread(String threadId, String phoneNumber, String email, LocalDateTime createdAt,
     String toNumber,String CaseType, Long custiId) {
        this.threadId = threadId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.createdAt = createdAt;
        this.toNumber = toNumber;
        this.caseType = caseType;
        this.custiId = custiId;

    }

    // Getters and Setters
    public Long getConversationThreadId() {
        return conversationThreadId;
    }

    public void setConversationThreadId(Long conversationThreadId) {
        this.conversationThreadId = conversationThreadId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

   
    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }




    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public Long getCustiId() {
        return custiId;
    }

    public void setCustiId(Long custiId) {
        this.custiId = custiId;
    }

    
}
