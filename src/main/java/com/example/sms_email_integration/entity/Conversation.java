package com.example.sms_email_integration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key

    @Column(name = "phone_number")
    private String phoneNumber; // Client's phone number

     @Column(name = "to_number")
    private String toNumber; 

    @Column(name = "email")
    private String email; // Lawyer's email address

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // The content of the SMS or Email

    @Column(name = "direction")
    private String direction; // "INCOMING" or "OUTGOING"

    @Column(name = "channel")
    private String channel; // "SMS" or "EMAIL"

    @Column(name = "subject")
    private String subject; // For Email subjects; SMS can be null

    @Column(name = "timestamp")
    private LocalDateTime timestamp; // When the message is sent/received

    @Column(name = "thread_id")
    private String threadId; // Groups related messages (client-lawyer conversation)

    @Column(name = "message_id", unique = true)
    private String messageId; // Unique ID for each message

    // Constructors
    public Conversation() {}

    public Conversation(
            String phoneNumber,
             String toNumber,
            String email,
            String message,
            String direction,
            String channel,
            String subject,
            LocalDateTime timestamp,
            String threadId,
            String messageId
    ) {
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
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
     public String getToNumber() { return toNumber; }
    public void setToNumber(String toNumber) { this.toNumber = toNumber; }


    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
}
