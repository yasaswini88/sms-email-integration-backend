package com.example.sms_email_integration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key

    // Link to the new ConversationThread table
    @ManyToOne
    @JoinColumn(name = "conversation_thread_id", nullable = false)
    private ConversationThread conversationThread;

    // Fields that are per-message (not per-thread)
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "direction")
    private String direction; // "INCOMING" or "OUTGOING"

    @Column(name = "channel")
    private String channel; // "SMS" or "EMAIL"

    @Column(name = "subject")
    private String subject; // for Email subjects; can be null for SMS

    @Column(name = "timestamp")
    private LocalDateTime timestamp; // when the message is sent/received

    @Column(name = "message_id", unique = true)
    private String messageId; // unique ID for each message from Twilio or SendGrid





    // Constructors
    public Conversation() {}

    public Conversation(ConversationThread conversationThread,
                        String message,
                        String direction,
                        String channel,
                        String subject,
                        LocalDateTime timestamp,
                        String messageId
                        
                       
                        )
                         {
        this.conversationThread = conversationThread;
        this.message = message;
        this.direction = direction;
        this.channel = channel;
        this.subject = subject;
        this.timestamp = timestamp;
        this.messageId = messageId;
       
       
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConversationThread getConversationThread() {
        return conversationThread;
    }

    public void setConversationThread(ConversationThread conversationThread) {
        this.conversationThread = conversationThread;
    }

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

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }


    
}
