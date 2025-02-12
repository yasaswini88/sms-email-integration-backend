package com.example.sms_email_integration.dto;

import java.time.LocalDateTime;

public class ConversationDto {

    private Long id;
    private String phoneNumber;
    private String toNumber;
    private String email;
    private String message;
    private String direction;
    private String channel;
    private String subject;
    private LocalDateTime timestamp;
    private String threadId;
    private String messageId;
    private Long assignedLawyerId;
private String assignedLawyerName;
private String status; 

private Long conversationThreadId;

private String caseType;

    // Default Constructor
    public ConversationDto() {
    }

    // Parameterized Constructor
    public ConversationDto(Long id, String phoneNumber, String toNumber,String email, String message,
                           String direction, String channel, String subject,
                           LocalDateTime timestamp, String threadId, String messageId,String assignedLawyerName,Long assignedLawyerId,Long conversationThreadId,String status,String caseType) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.toNumber = toNumber;
        this.email = email;
        this.message = message;
        this.direction = direction;
        this.channel = channel;
        this.subject = subject;
        this.timestamp = timestamp;
        this.threadId = threadId;
        this.messageId = messageId;
        this.assignedLawyerName = assignedLawyerName;
        this.assignedLawyerId = assignedLawyerId;
        this.conversationThreadId = conversationThreadId;
        this.status = status;
        this.caseType = caseType;

    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getAssignedLawyerId() {
        return assignedLawyerId;
    }

    public void setAssignedLawyerId(Long assignedLawyerId) {
        this.assignedLawyerId = assignedLawyerId;
    }

    public String getAssignedLawyerName() {
        return assignedLawyerName;
    }

    public void setAssignedLawyerName(String assignedLawyerName) {
        this.assignedLawyerName = assignedLawyerName;
    }

 public Long getConversationThreadId() {
        return conversationThreadId;
    }

    public void setConversationThreadId(Long conversationThreadId) {
        this.conversationThreadId = conversationThreadId;
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
    


    @Override
    public String toString() {
        return "ConversationDto{" +
                "id=" + id +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", toNumber='" + toNumber + '\'' +
                ", email='" + email + '\'' +
                ", message='" + message + '\'' +
                ", direction='" + direction + '\'' +
                ", channel='" + channel + '\'' +
                ", subject='" + subject + '\'' +
                ", timestamp=" + timestamp +
                ", threadId='" + threadId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", assignedLawyerId=" + assignedLawyerId +
                ", assignedLawyerName='" + assignedLawyerName + '\'' +
                ", conversationThreadId=" + conversationThreadId +
                ", status='" + status + '\'' +
                
                '}';
    }
}
