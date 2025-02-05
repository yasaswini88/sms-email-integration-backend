package com.example.sms_email_integration.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "firm_client_lawyer")
public class FirmClientMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long firmClientMappingId;                   

    @ManyToOne
    @JoinColumn(name = "lawyer_id", nullable = false)  
    private FirmLawyer firmLawyer;

    private String clientPhoneNumber;

    @ManyToOne
    @JoinColumn(name = "custi_id", nullable = false)  // Foreign key linking to Customer (Firm)
    private Customer firm;

    // Default constructor
    public FirmClientMapping() {}

    // Parameterized constructor

    public FirmClientMapping(FirmLawyer firmLawyer, String clientPhoneNumber, Customer firm) {
        this.firmLawyer = firmLawyer;
        this.clientPhoneNumber = clientPhoneNumber;
        this.firm = firm;
    }

    // Getters and Setters

    public Long getFirmClientMappingId() {
        return firmClientMappingId;
    }

    public void setFirmClientMappingId(Long firmClientMappingId) {
        this.firmClientMappingId = firmClientMappingId;
    }

    public FirmLawyer getFirmLawyer() {
        return firmLawyer;
    }

    public void setFirmLawyer(FirmLawyer firmLawyer) {
        this.firmLawyer = firmLawyer;
    }

    public String getClientPhoneNumber() {
        return clientPhoneNumber;
    }

    public void setClientPhoneNumber(String clientPhoneNumber) {
        this.clientPhoneNumber = clientPhoneNumber;
    }

    public Customer getFirm() {
        return firm;
    }

    public void setFirm(Customer firm) {
        this.firm = firm;
    }

    @Override
    public String toString() {
        return "FirmClientMapping{" +
                "firmClientMappingId=" + firmClientMappingId +
                ", firmLawyer=" + firmLawyer +
                ", clientPhoneNumber='" + clientPhoneNumber + '\'' +
                ", firm=" + firm +
                '}';
    }
    

}
