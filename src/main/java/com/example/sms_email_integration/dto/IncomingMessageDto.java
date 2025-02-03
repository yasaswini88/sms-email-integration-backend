package com.example.sms_email_integration.dto;

import java.time.LocalDateTime;

public class IncomingMessageDto {

    private Long msgId;
    private String fromNumber;
    private String toNumber;
    private String body;
    private LocalDateTime receivedAt;

    public IncomingMessageDto() {
    }

    public IncomingMessageDto(Long msgId, String fromNumber, String toNumber, String body, LocalDateTime receivedAt) {
        this.msgId = msgId;
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
        this.body = body;
        this.receivedAt = receivedAt;
    }

    public Long getMsgId() {
        return msgId;
    }

    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
