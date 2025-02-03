package com.example.sms_email_integration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incoming_messages")
public class IncomingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long msg_id; // Primary Key

    @Column(name = "from_number", nullable = false)
    private String fromNumber;

    @Column(name = "to_number", nullable = false)
    private String toNumber;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    // Default Constructor
    public IncomingMessage() {}

    // Parameterized Constructor
    public IncomingMessage(String fromNumber, String toNumber, String body, LocalDateTime receivedAt) {
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
        this.body = body;
        this.receivedAt = receivedAt;
    }

    // Getters and Setters
    public Long getMsgId() { return msg_id; }
    public void setMsgId(Long msg_id) { this.msg_id = msg_id; }

    public String getFromNumber() { return fromNumber; }
    public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }

    public String getToNumber() { return toNumber; }
    public void setToNumber(String toNumber) { this.toNumber = toNumber; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    @Override
    public String toString() {
        return "IncomingMessage{" +
                "msg_id=" + msg_id +
                ", fromNumber='" + fromNumber + '\'' +
                ", toNumber='" + toNumber + '\'' +
                ", body='" + body + '\'' +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
