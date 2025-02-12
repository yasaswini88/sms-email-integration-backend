package com.example.sms_email_integration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "verify_code")
public class VerifyCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The Lawyer who requested verification
    @Column(name = "lawyer_id", nullable = false)
    private Long lawyerId;

    // The 4-digit code
    @Column(name = "code", nullable = false)
    private String code;

    // When this code was generated
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public VerifyCode() {}

    public VerifyCode(Long lawyerId, String code, LocalDateTime createdAt) {
        this.lawyerId = lawyerId;
        this.code = code;
        this.createdAt = createdAt;
    }

    // Getters and setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLawyerId() { return lawyerId; }
    public void setLawyerId(Long lawyerId) { this.lawyerId = lawyerId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
