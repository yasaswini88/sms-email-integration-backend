package com.example.sms_email_integration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
     @Column(name = "direction", nullable = true)
    private String direction; // e.g. "INCOMING" or "OUTGOING"

    // Default Constructor
    public IncomingMessage() {}

    // Parameterized Constructor

     public IncomingMessage(String fromNumber, String toNumber, String body, LocalDateTime receivedAt) {
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
        this.body = body;
        this.receivedAt = receivedAt;
    }



    public IncomingMessage(String fromNumber, String toNumber, String body, LocalDateTime receivedAt, String direction) {
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
        this.body = body;
        this.receivedAt = receivedAt;
        this.direction = direction;
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

    public String getDirection() { return direction; }

    public void setDirection(String direction) { this.direction = direction; }



    @Override
    public String toString() {
        return "IncomingMessage{" +
                "msg_id=" + msg_id +
                ", fromNumber='" + fromNumber + '\'' +
                ", toNumber='" + toNumber + '\'' +
                ", body='" + body + '\'' +
                ", receivedAt=" + receivedAt +
                ", direction='" + direction + '\'' +
                '}';
    }
}
