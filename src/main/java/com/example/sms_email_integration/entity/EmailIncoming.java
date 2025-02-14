package com.example.sms_email_integration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_incoming")
public class EmailIncoming {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The client's phone number that is extracted from the "To" address
    @Column(name = "client_phone_number", nullable = false)
    private String clientPhoneNumber;

    // Relationship to the Lawyer (we store lawyer_id as a foreign key)
    @ManyToOne
    @JoinColumn(name = "lawyer_id") 
    private FirmLawyer lawyer; 

    // The firm ID (custi_id)
    @Column(name = "custi_id", nullable = false)
    private Long custiId;

    // The date/time we received the incoming email (from SendGrid)
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

     @Column(name = "direction", nullable = false)
   private String direction;  // e.g. "INCOMING" or "OUTGOING"

    // --- Constructors ---
    public EmailIncoming() {
    }

 public EmailIncoming(String clientPhoneNumber, FirmLawyer lawyer, Long custiId, 
                        LocalDateTime receivedAt, String direction) {
       this.clientPhoneNumber = clientPhoneNumber;
       this.lawyer = lawyer;
       this.custiId = custiId;
       this.receivedAt = receivedAt;
       this.direction = direction;
   }


 public EmailIncoming(String clientPhoneNumber, FirmLawyer lawyer, Long custiId, 
                        LocalDateTime receivedAt) {
       this.clientPhoneNumber = clientPhoneNumber;
       this.lawyer = lawyer;
       this.custiId = custiId;
       this.receivedAt = receivedAt;
       
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

    public FirmLawyer getLawyer() {
        return lawyer;
    }

    public void setLawyer(FirmLawyer lawyer) {
        this.lawyer = lawyer;
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

    public String getDirection() {
        return direction;
    }


    public void setDirection(String direction) {
        this.direction = direction;
    }

    

    @Override
    public String toString() {
        return "EmailIncoming{" +
                "id=" + id +
                ", clientPhoneNumber='" + clientPhoneNumber + '\'' +
                ", lawyer=" + (lawyer != null ? lawyer.getLawyerId() : null) +
                ", custiId=" + custiId +
                ", receivedAt=" + receivedAt +
                ", direction='" + direction + '\'' +
                '}';
    }
}
